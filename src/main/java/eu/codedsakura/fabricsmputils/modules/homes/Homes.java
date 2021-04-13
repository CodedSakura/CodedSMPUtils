package eu.codedsakura.fabricsmputils.modules.homes;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import eu.codedsakura.fabricsmputils.FabricSMPUtils;
import eu.codedsakura.common.ConfigUtils;
import eu.codedsakura.common.TeleportUtils;
import eu.codedsakura.common.TextUtils;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static eu.codedsakura.fabricsmputils.FabricSMPUtils.config;
import static eu.codedsakura.fabricsmputils.SMPUtilCardinalComponents.HOME_DATA;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class Homes {

    private final HashMap<UUID, Long> recentRequests = new HashMap<>();

    public void initialize() {

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(literal("home")
                    .requires(source -> config.teleportation != null && config.teleportation.homes != null)
                    .requires(Permissions.require("fabricspmutils.teleportation.homes", true))
                    .executes(ctx -> homeInit(ctx, null))
                    .then(argument("name", StringArgumentType.greedyString()).suggests(this::getHomeSuggestions)
                            .executes(ctx -> homeInit(ctx, StringArgumentType.getString(ctx, "name")))));

            dispatcher.register(literal("sethome")
                    .requires(source -> config.teleportation != null && config.teleportation.homes != null)
                    .requires(Permissions.require("fabricspmutils.teleportation.homes.edit", true))
                    .executes(ctx -> homeSet(ctx, null))
                    .then(argument("name", StringArgumentType.greedyString())
                            .executes(ctx -> homeSet(ctx, StringArgumentType.getString(ctx, "name")))));

            dispatcher.register(literal("delhome")
                    .requires(source -> config.teleportation != null && config.teleportation.homes != null)
                    .requires(Permissions.require("fabricspmutils.teleportation.homes.edit", true))
                            .then(argument("name", StringArgumentType.greedyString()).suggests(this::getHomeSuggestions)
                                    .executes(ctx -> homeDel(ctx, StringArgumentType.getString(ctx, "name")))));

            dispatcher.register(literal("homes")
                    .requires(source -> config.teleportation != null && config.teleportation.homes != null)
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
        });
    }

    private boolean checkCooldown(ServerPlayerEntity tFrom) {
        if (recentRequests.containsKey(tFrom.getUuid())) {
            long diff = Instant.now().getEpochSecond() - recentRequests.get(tFrom.getUuid());
            if (diff < config.teleportation.homes.cooldown) {
                tFrom.sendMessage(new TranslatableText("You cannot make teleport home for %s more seconds!", String.valueOf(config.teleportation.homes.cooldown - diff)).formatted(Formatting.RED), false);
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
            ctx.getSource().sendFeedback(new LiteralText("This home does not exist").formatted(Formatting.RED), false);
            return 0;
        }

        if (checkCooldown(player)) return 1;

        TeleportUtils.genericTeleport(
                config.teleportation.homes.bossBar, config.teleportation.homes.actionBar, config.teleportation.homes.standStill,
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

        if (HOME_DATA.get(ctx.getSource().getPlayer()).getHomes().size() >= config.teleportation.homes.starting) {
            ctx.getSource().sendFeedback(new LiteralText("Home limit reached!").formatted(Formatting.RED), false);
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
                ctx.getSource().sendFeedback(new LiteralText("Something went wrong adding the home!").formatted(Formatting.RED), true);
                return 1;
            }

            ctx.getSource().sendFeedback(new TranslatableText("Home %s added successfully!",
                    new LiteralText(name).styled(s -> s.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, home.get().toText(ctx.getSource().getMinecraftServer())))
                            .withColor(Formatting.GOLD))).formatted(Formatting.LIGHT_PURPLE), false);
        } else {
            ctx.getSource().sendFeedback(new LiteralText("Couldn't add the home (probably already exists)!").formatted(Formatting.RED), false);
            return 1;
        }
        return 1;
    }

    int homeDel(CommandContext<ServerCommandSource> ctx, String name) throws CommandSyntaxException {
        if (HOME_DATA.get(ctx.getSource().getPlayer()).removeHome(name)) {
            Optional<HomeComponent> home = HOME_DATA.get(ctx.getSource().getPlayer()).getHomes()
                    .stream().filter(v -> v.getName().equals(name)).findFirst();

            if (home.isPresent()) {
                ctx.getSource().sendFeedback(new LiteralText("Something went wrong removing the home!").formatted(Formatting.RED), true);
                return 1;
            }

            ctx.getSource().sendFeedback(new TranslatableText("Home %s deleted successfully!",
                    new LiteralText(name).formatted(Formatting.GOLD)).formatted(Formatting.LIGHT_PURPLE), false);
        } else {
            ctx.getSource().sendFeedback(new LiteralText("Couldn't remove the home!").formatted(Formatting.RED), false);
            return 1;
        }
        return 1;
    }


    int homeList(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        return homeList(ctx, ctx.getSource().getPlayer());
    }
    int homeList(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player) {
        List<HomeComponent> homes = HOME_DATA.get(player).getHomes();
        List<Text> list = new ArrayList<>();
        homes.stream().sorted((h1, h2) -> h1.getName().compareToIgnoreCase(h2.getName())).forEach(h ->
                list.add(new LiteralText(h.getName()).styled(s ->
                        s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/home " + h.getName()))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        LiteralText.EMPTY.copy().append(new LiteralText("Click to teleport.\n").formatted(Formatting.ITALIC))
                                                .append(h.toText(ctx.getSource().getMinecraftServer()))))
                                .withColor(Formatting.GOLD))));
        ctx.getSource().sendFeedback(new TranslatableText("%s/%s:\n", homes.size(), config.teleportation.homes.starting).append(TextUtils.join(list, new LiteralText(", "))), false);
        return 1;
    }
}
