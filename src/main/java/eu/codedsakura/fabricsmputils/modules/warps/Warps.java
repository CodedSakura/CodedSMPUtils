package eu.codedsakura.fabricsmputils.modules.warps;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import eu.codedsakura.common.TeleportUtils;
import eu.codedsakura.common.TextUtils;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.RotationArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static eu.codedsakura.fabricsmputils.FabricSMPUtils.config;
import static eu.codedsakura.fabricsmputils.SMPUtilCardinalComponents.WARP_LIST;
import static net.minecraft.command.argument.RotationArgumentType.getRotation;
import static net.minecraft.command.argument.Vec3ArgumentType.getPosArgument;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class Warps {
    private final HashMap<UUID, Long> recentRequests = new HashMap<>();

    private List<Pair<ServerWorld, Warp>> getAllWarps(MinecraftServer server) {
        List<Pair<ServerWorld, Warp>> out = new ArrayList<>();
        server.getWorlds().forEach(serverWorld ->
                WARP_LIST.get(serverWorld).getWarps().forEach(warp ->
                        out.add(new Pair<>(serverWorld, warp))));
        out.sort((o1, o2) -> o1.getLeft().toString().compareToIgnoreCase(o2.getLeft().toString()));
        return out;
    }

    public void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(literal("warp")
                    .requires(source -> config.teleportation != null && config.teleportation.tpa != null)
                    .requires(Permissions.require("fabricspmutils.teleportation.warp", true))
                    .then(argument("name", StringArgumentType.string()).suggests(this::getWarpSuggestions)
                            .executes(ctx -> warpTo(ctx, getString(ctx, "name")))));

            dispatcher.register(literal("warps")
                    .requires(source -> config.teleportation != null && config.teleportation.tpa != null)
                    .requires(Permissions.require("fabricspmutils.teleportation.warp", true))
                    .executes(this::warpList)
                    .then(literal("list")
                            .executes(this::warpList)
                            .then(argument("dimension", DimensionArgumentType.dimension())
                                    .executes(ctx -> warpList(ctx, DimensionArgumentType.getDimensionArgument(ctx, "dimension")))))
                    .then(literal("add")
                            .requires(Permissions.require("fabricspmutils.teleportation.warp.modify", 2))
                            .executes(ctx -> {throw new SimpleCommandExceptionType(new LiteralText("Provide a warp name!")).create();})
                            .then(argument("name", StringArgumentType.string())
                                    .executes(ctx -> warpAdd(ctx, getString(ctx, "name")))
                                    .then(argument("position", Vec3ArgumentType.vec3(true))
                                            .executes(ctx -> warpAdd(ctx, getString(ctx, "name"), getPosArgument(ctx, "position").toAbsolutePos(ctx.getSource())))
                                            .then(argument("rotation", RotationArgumentType.rotation())
                                                    .executes(ctx -> warpAdd(ctx, getString(ctx, "name"), getPosArgument(ctx, "position").toAbsolutePos(ctx.getSource()), getRotation(ctx, "rotation").toAbsoluteRotation(ctx.getSource())))
                                                    .then(argument("dimension", DimensionArgumentType.dimension())
                                                            .executes(ctx -> warpAdd(ctx, getString(ctx, "name"), getPosArgument(ctx, "position").toAbsolutePos(ctx.getSource()), getRotation(ctx, "rotation").toAbsoluteRotation(ctx.getSource()), DimensionArgumentType.getDimensionArgument(ctx, "dimension"))))))))
                    .then(literal("remove")
                            .requires(Permissions.require("fabricspmutils.teleportation.warp.modify", 2))
                            .executes(ctx -> {throw new SimpleCommandExceptionType(new LiteralText("Provide a warp name!")).create();})
                            .then(argument("name", StringArgumentType.string()).suggests(this::getWarpSuggestions)
                                    .executes(ctx -> warpRemove(ctx, getString(ctx, "name")))))
                    .then(literal("warp_player")
                            .requires(Permissions.require("fabricspmutils.teleportation.warp.warp_player", 2))
                            .then(argument("player", EntityArgumentType.player())
                                    .then(argument("warp_name", StringArgumentType.string()).suggests(this::getWarpSuggestions)
                                            .executes(ctx -> warpTo(ctx, EntityArgumentType.getPlayer(ctx, "player"), getString(ctx, "warp_name")))))));
        });
    }

    private int warpRemove(CommandContext<ServerCommandSource> ctx, String name) throws CommandSyntaxException {
        Pair<ServerWorld, Warp> warp = getAllWarps(ctx.getSource().getMinecraftServer()).stream()
                .filter(v -> v.getRight().name.equals(name)).findFirst()
                .orElseThrow(() -> new SimpleCommandExceptionType(new LiteralText("Warp with this name not found!")).create());
        if (!WARP_LIST.get(warp.getLeft()).removeWarp(warp.getRight().name))
            throw new SimpleCommandExceptionType(new LiteralText("Failed to remove warp!")).create();
        ctx.getSource().sendFeedback(new TranslatableText("Warp %s successfully removed!", new LiteralText(name).formatted(Formatting.GOLD)), true);
        return 1;
    }

    private int warpAdd(CommandContext<ServerCommandSource> ctx, String name) throws CommandSyntaxException {
        return warpAdd(ctx, name, ctx.getSource().getPosition(), ctx.getSource().getRotation(), ctx.getSource().getWorld());
    }

    private int warpAdd(CommandContext<ServerCommandSource> ctx, String name, Vec3d position) throws CommandSyntaxException {
        return warpAdd(ctx, name, position, ctx.getSource().getRotation(), ctx.getSource().getWorld());
    }

    private int warpAdd(CommandContext<ServerCommandSource> ctx, String name, Vec3d position, Vec2f rotation) throws CommandSyntaxException {
        return warpAdd(ctx, name, position, rotation, ctx.getSource().getWorld());
    }

    private int warpAdd(CommandContext<ServerCommandSource> ctx, String name, Vec3d position, Vec2f rotation, ServerWorld dimension) throws CommandSyntaxException {
        if (!name.matches("^[!-~]+$")) throw new SimpleCommandExceptionType(new LiteralText("Invalid warp name!")).create();
        if (getAllWarps(ctx.getSource().getMinecraftServer()).stream().anyMatch(w -> w.getRight().name.equalsIgnoreCase(name)))
            throw new SimpleCommandExceptionType(new LiteralText("Warp with this name already exists!")).create();
        Warp newWarp = new Warp(position, rotation, name, ctx.getSource().getPlayer().getUuid());
        if (!WARP_LIST.get(dimension).addWarp(newWarp))
            throw new SimpleCommandExceptionType(new LiteralText("Failed to add warp!")).create();
        ctx.getSource().sendFeedback(new TranslatableText("Warp %s successfully added!",
                new LiteralText(name).styled(s -> s.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, newWarp.toText(ctx.getSource().getWorld()))).withColor(Formatting.GOLD))), true);
        return 1;
    }

    private MutableText warpListForDimension(ServerWorld dimension) {
        List<Warp> warps = WARP_LIST.get(dimension).getWarps();
        MutableText list = new LiteralText("In ").formatted(Formatting.LIGHT_PURPLE)
                .append(new LiteralText(dimension.getRegistryKey().getValue().toString()).formatted(Formatting.AQUA))
                .append(new LiteralText(":").formatted(Formatting.LIGHT_PURPLE));
        warps.stream().sorted((o1, o2) -> o1.name.compareToIgnoreCase(o2.name)).forEach((v) ->
                list.append(new LiteralText("\n  ").append(new LiteralText(v.name).styled(s ->
                        s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/warp \"" + v.name + "\""))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, LiteralText.EMPTY.copy().append(new LiteralText("Click to teleport.\n").formatted(Formatting.ITALIC)).append(v.toText(dimension))))
                                .withColor(Formatting.GOLD)))));
        return list;
    }

    private int warpList(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ctx.getSource().getPlayer().sendMessage(TextUtils.join(StreamSupport.stream(ctx.getSource().getMinecraftServer().getWorlds().spliterator(), false)
                .map(this::warpListForDimension).collect(Collectors.toList()), new LiteralText("\n")), false);//.reduce(LiteralText.EMPTY.copy(), (buff, elem) -> buff.append(elem).append("\n")), false);
        return 1;
    }

    private int warpList(CommandContext<ServerCommandSource> ctx, ServerWorld dimension) throws CommandSyntaxException {
        ctx.getSource().getPlayer().sendMessage(warpListForDimension(dimension), false);
        return 1;
    }

    private boolean checkCooldown(ServerPlayerEntity tFrom) {
        if (recentRequests.containsKey(tFrom.getUuid())) {
            long diff = Instant.now().getEpochSecond() - recentRequests.get(tFrom.getUuid());
            if (diff < config.teleportation.warps.cooldown) {
                tFrom.sendMessage(new TranslatableText("You cannot make a request for %s more seconds!",
                        String.valueOf(config.teleportation.warps.cooldown - diff)).formatted(Formatting.RED), false);
                return true;
            }
        }
        return false;
    }

    private int warpTo(CommandContext<ServerCommandSource> ctx, String name) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (checkCooldown(player)) return 1;
        return warpTo(ctx, player, name);
    }

    private int warpTo(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player, String name) throws CommandSyntaxException {
        Pair<ServerWorld, Warp> warp = getAllWarps(ctx.getSource().getMinecraftServer()).stream()
                .filter(v -> v.getRight().name.equals(name)).findFirst()
                .orElseThrow(() -> new SimpleCommandExceptionType(new LiteralText("Invalid warp")).create());

        TeleportUtils.genericTeleport(
                config.teleportation.warps.bossBar, config.teleportation.warps.actionBar, config.teleportation.warps.standStill,
                player, () -> {
                    player.teleport(warp.getLeft(), warp.getRight().x, warp.getRight().y, warp.getRight().z, warp.getRight().yaw, warp.getRight().pitch);
                    recentRequests.put(player.getUuid(), Instant.now().getEpochSecond());
                });
        return 1;
    }

    private CompletableFuture<Suggestions> getWarpSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        String start = builder.getRemaining().toLowerCase();
        getAllWarps(context.getSource().getMinecraftServer()).stream()
                .map(v -> v.getRight().name)
                .sorted(String::compareToIgnoreCase)
                .filter(pair -> pair.toLowerCase().startsWith(start))
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

}
