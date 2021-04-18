package eu.codedsakura.fabricsmputils.modules.teleportation.homes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import eu.codedsakura.common.TeleportUtils;
import eu.codedsakura.fabricsmputils.config.FabricSMPUtilsConfig;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.*;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static eu.codedsakura.fabricsmputils.FabricSMPUtils.*;
import static eu.codedsakura.fabricsmputils.SMPUtilCardinalComponents.HOME_DATA;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class Homes {

    private final HashMap<UUID, Long> recentRequests = new HashMap<>();

    public Homes(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("home")
                .requires(source -> CONFIG.teleportation != null && CONFIG.teleportation.homes != null)
                .requires(Permissions.require("fabricspmutils.teleportation.homes", true))
                .executes(ctx -> homeInit(ctx, null))
                .then(argument("name", StringArgumentType.greedyString()).suggests(this::getHomeSuggestions)
                        .executes(ctx -> homeInit(ctx, StringArgumentType.getString(ctx, "name")))));

        dispatcher.register(literal("sethome")
                .requires(source -> CONFIG.teleportation != null && CONFIG.teleportation.homes != null)
                .requires(Permissions.require("fabricspmutils.teleportation.homes.edit", true))
                .executes(ctx -> homeSet(ctx, null))
                .then(argument("name", StringArgumentType.greedyString())
                        .executes(ctx -> homeSet(ctx, StringArgumentType.getString(ctx, "name")))));

        dispatcher.register(literal("delhome")
                .requires(source -> CONFIG.teleportation != null && CONFIG.teleportation.homes != null)
                .requires(Permissions.require("fabricspmutils.teleportation.homes.edit", true))
                        .then(argument("name", StringArgumentType.greedyString()).suggests(this::getHomeSuggestions)
                                .executes(ctx -> homeDel(ctx, StringArgumentType.getString(ctx, "name")))));

        dispatcher.register(literal("homes")
                .requires(source -> CONFIG.teleportation != null && CONFIG.teleportation.homes != null)
                .requires(Permissions.require("fabricspmutils.teleportation.homes", true))
                .executes(this::homeList)
                .then(literal("list")
                        .executes(this::homeList)
                        .then(argument("player", EntityArgumentType.player())
                                .requires(Permissions.require("fabricspmutils.teleportation.homes.list-player", 2))
                                .executes(ctx -> homeList(ctx, EntityArgumentType.getPlayer(ctx, "player")))))
                .then(literal("gui").requires(req -> false)
                        .executes(ctx -> 0)) // TODO
                .then(literal("delete")
                        .requires(Permissions.require("fabricspmutils.teleportation.homes.edit", true))
                        .then(argument("name", StringArgumentType.greedyString()).suggests(this::getHomeSuggestions)
                                .executes(ctx -> homeDel(ctx, StringArgumentType.getString(ctx, "name"))))));

        CONFIG_RELOAD_EVENT.register(this::handleConfigReload);
    }

    private void handleConfigReload(FabricSMPUtilsConfig oldConfig, FabricSMPUtilsConfig newConfig) {
        // TODO
    }

    private boolean checkCooldown(ServerPlayerEntity tFrom) {
        if (recentRequests.containsKey(tFrom.getUuid())) {
            long diff = Instant.now().getEpochSecond() - recentRequests.get(tFrom.getUuid());
            if (diff < CONFIG.teleportation.homes.cooldown) {
                tFrom.sendMessage(L.get("teleportation.homes.cooldown",
                        new HashMap<String, Object>() {{ put("remaining", CONFIG.teleportation.homes.cooldown - diff); }}), false);
                return true;
            }
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
        if (name == null) name = "main";

        String finalName = name;
        Optional<HomeComponent> home = HOME_DATA.get(player).getHomes()
                .stream().filter(v -> v.getName().equals(finalName)).findFirst();

        if (!home.isPresent()) {
            ctx.getSource().sendFeedback(L.get("teleportation.homes.not-exists"), false);
            return 0;
        }

        if (checkCooldown(player)) return 1;

        TeleportUtils.genericTeleport(
                "teleportation.homes", CONFIG.teleportation.homes.bossBar, CONFIG.teleportation.homes.actionBar, CONFIG.teleportation.homes.standStill,
                player, () -> {
                    player.teleport(
                            ctx.getSource().getMinecraftServer().getWorld(RegistryKey.of(Registry.DIMENSION, home.get().getDimID())),
                            home.get().getX(), home.get().getY(), home.get().geyZ(),
                            home.get().getYaw(), home.get().getPitch());
                    recentRequests.put(player.getUuid(), Instant.now().getEpochSecond());
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
                ctx.getSource().getPlayer().pitch,
                ctx.getSource().getPlayer().yaw,
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
                    put("count", homes.size());
                    put("max", CONFIG.teleportation.homes.starting);
        }}).shallowCopy();
        homes.stream().sorted((h1, h2) -> h1.getName().compareToIgnoreCase(h2.getName())).forEach(h ->
                list.append(L.get("teleportation.homes.list.entry", h.asArguments())));
        ctx.getSource().sendFeedback(list, false);
        return 1;
    }
}
