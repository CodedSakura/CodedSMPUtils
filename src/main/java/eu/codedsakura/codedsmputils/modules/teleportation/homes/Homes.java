package eu.codedsakura.codedsmputils.modules.teleportation.homes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import eu.codedsakura.codedsmputils.config.CodedSMPUtilsConfig;
import eu.codedsakura.codedsmputils.modules.teleportation.CooldownManager;
import eu.codedsakura.codedsmputils.modules.teleportation.back.Back;
import eu.codedsakura.common.TeleportUtils;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static eu.codedsakura.codedsmputils.CodedSMPUtils.*;
import static eu.codedsakura.codedsmputils.SMPUtilCardinalComponents.HOME_DATA;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class Homes {
    public Homes(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("home")
                .requires(Permissions.require("codedsmputils.teleportation.homes", true)
                        .and(source -> CONFIG.teleportation != null && CONFIG.teleportation.homes != null))
                .executes(ctx -> homeInit(ctx, null))
                .then(argument("name", StringArgumentType.greedyString()).suggests(this::getHomeSuggestions)
                        .executes(ctx -> homeInit(ctx, StringArgumentType.getString(ctx, "name")))));

        dispatcher.register(literal("sethome")
                .requires(Permissions.require("codedsmputils.teleportation.homes.edit", true)
                        .and(source -> CONFIG.teleportation != null && CONFIG.teleportation.homes != null))
                .executes(ctx -> homeSet(ctx, null))
                .then(argument("name", StringArgumentType.greedyString())
                        .executes(ctx -> homeSet(ctx, StringArgumentType.getString(ctx, "name")))));

        dispatcher.register(literal("delhome")
                .requires(Permissions.require("codedsmputils.teleportation.homes.edit", true)
                        .and(source -> CONFIG.teleportation != null && CONFIG.teleportation.homes != null))
                        .then(argument("name", StringArgumentType.greedyString()).suggests(this::getHomeSuggestions)
                                .executes(ctx -> homeDel(ctx, StringArgumentType.getString(ctx, "name")))));

        dispatcher.register(literal("homes")
                .requires(Permissions.require("codedsmputils.teleportation.homes", true)
                        .and(source -> CONFIG.teleportation != null && CONFIG.teleportation.homes != null))
                .executes(this::homeList)
                .then(literal("list")
                        .executes(this::homeList)
                        .then(argument("player", EntityArgumentType.player())
                                .requires(Permissions.require("codedsmputils.teleportation.homes.list-player", 2))
                                .executes(ctx -> homeList(ctx, EntityArgumentType.getPlayer(ctx, "player")))))
                .then(literal("gui").requires(req -> false)
                        .executes(ctx -> 0)) // TODO
                .then(literal("delete")
                        .requires(Permissions.require("codedsmputils.teleportation.homes.edit", true))
                        .then(argument("name", StringArgumentType.greedyString()).suggests(this::getHomeSuggestions)
                                .executes(ctx -> homeDel(ctx, StringArgumentType.getString(ctx, "name"))))));

        CONFIG_RELOAD_EVENT.register(this::handleConfigReload);
    }

    private void handleConfigReload(CodedSMPUtilsConfig oldConfig, CodedSMPUtilsConfig newConfig) {
        // TODO
    }

    private boolean checkCooldown(ServerPlayerEntity tFrom) {
        long remaining = CooldownManager.getCooldownTimeRemaining(Homes.class, tFrom.getUuid());
        if (remaining > 0) {
            tFrom.sendMessage(L.get("teleportation.homes.cooldown",
                    new HashMap<String, Long>() {{ put("remaining", remaining); }}), false);
            return true;
        }
        return false;
    }

    private CompletableFuture<Suggestions> getHomeSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        String start = builder.getRemaining().toLowerCase();
        HOME_DATA.get(context.getSource().getPlayer()).getHomes().stream()
                .map(HomeComponent::getName)
                .sorted(String::compareToIgnoreCase)
                .filter(v -> v.toLowerCase().startsWith(start))
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

    int homeInit(CommandContext<ServerCommandSource> ctx, String name) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayer();

        if (TeleportUtils.cantTeleport(player)) return 1;

        if (name == null) name = "main";

        String finalName = name;
        Optional<HomeComponent> home = HOME_DATA.get(player).getHomes()
                .stream().filter(v -> v.getName().equals(finalName)).findFirst();

        if (!home.isPresent()) {
            ctx.getSource().sendFeedback(L.get("teleportation.homes.not-exists"), false);
            return 0;
        }

        if (checkCooldown(player)) return 1;

        boolean allowBack;
        allowBack = CONFIG.teleportation.homes.allowBack.getValue(new HashMap<String, Object>() {{
            put("home_count", HOME_DATA.get(player).getHomes().size());
            put("max", CONFIG.teleportation.homes.starting);
        }});

        TeleportUtils.genericTeleport(
                "teleportation.homes", CONFIG.teleportation.homes.bossBar, CONFIG.teleportation.homes.actionBar, CONFIG.teleportation.homes.standStill,
                player, () -> {
                    if (allowBack)
                        Back.addNewTeleport(new Back.TeleportLocation(player.getUuid(), Instant.now().getEpochSecond(),
                                (ServerWorld) player.world, player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch()));

                    ServerWorld world = ctx.getSource().getServer().getWorld(RegistryKey.of(Registry.WORLD_KEY, home.get().getDimID()));
                    player.teleport(world, home.get().getX(), home.get().getY(), home.get().geyZ(), home.get().getYaw(), home.get().getPitch());
                    CooldownManager.addCooldown(Homes.class, player.getUuid(), CONFIG.teleportation.homes.cooldown);
                });

        return 1;
    }

    int homeSet(CommandContext<ServerCommandSource> ctx, String name) throws CommandSyntaxException {
        if (name == null) name = "main";

        if (HOME_DATA.get(ctx.getSource().getPlayer()).getHomes().size() >= CONFIG.teleportation.homes.starting) {
            ctx.getSource().sendFeedback(L.get("teleportation.homes.limit"), false);
            return 1;
        }

        if (HOME_DATA.get(ctx.getSource().getPlayer()).addHome(new HomeComponent(
                ctx.getSource().getPosition(),
                ctx.getSource().getPlayer().getPitch(),
                ctx.getSource().getPlayer().getYaw(),
                ctx.getSource().getWorld().getRegistryKey().getValue(),
                name))) {

            String finalName = name;
            Optional<HomeComponent> home = HOME_DATA.get(ctx.getSource().getPlayer()).getHomes()
                    .stream().filter(v -> v.getName().equals(finalName)).findFirst();

            if (!home.isPresent()) {
                ctx.getSource().sendFeedback(L.get("teleportation.homes.add.error"), true);
                return 1;
            }

            ctx.getSource().sendFeedback(L.get("teleportation.homes.add.success",
                    new HashMap<String, Object>() {{ put("name", finalName); }}), false);
        } else {
            ctx.getSource().sendFeedback(L.get("teleportation.homes.add.could-not"), false);
            return 1;
        }
        return 1;
    }

    int homeDel(CommandContext<ServerCommandSource> ctx, String name) throws CommandSyntaxException {
        if (HOME_DATA.get(ctx.getSource().getPlayer()).removeHome(name)) {
            Optional<HomeComponent> home = HOME_DATA.get(ctx.getSource().getPlayer()).getHomes()
                    .stream().filter(v -> v.getName().equals(name)).findFirst();

            if (home.isPresent()) {
                ctx.getSource().sendFeedback(L.get("teleportation.homes.remove.error"), true);
                return 1;
            }

            ctx.getSource().sendFeedback(L.get("teleportation.homes.remove.success",
                    new HashMap<String, Object>() {{ put("name", name); }}), false);
        } else {
            ctx.getSource().sendFeedback(L.get("teleportation.homes.remove.could-not"), false);
            return 1;
        }
        return 1;
    }


    int homeList(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        return homeList(ctx, ctx.getSource().getPlayer());
    }
    int homeList(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player) {
        List<HomeComponent> homes = HOME_DATA.get(player).getHomes();
        MutableText list = L.get("teleportation.homes.list.header",
                new HashMap<String, Object>() {{
                    put("home_count", homes.size());
                    put("max", CONFIG.teleportation.homes.starting);
        }}).shallowCopy();
        homes.stream().sorted((h1, h2) -> h1.getName().compareToIgnoreCase(h2.getName())).forEach(h ->
                list.append(L.get("teleportation.homes.list.entry", h.asArguments())));
        ctx.getSource().sendFeedback(list, false);
        return 1;
    }
}
