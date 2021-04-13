package eu.codedsakura.fabricsmputils.modules.tpa;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import eu.codedsakura.common.TeleportUtils;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static eu.codedsakura.fabricsmputils.FabricSMPUtils.config;
import static net.minecraft.command.argument.EntityArgumentType.getPlayer;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class TPA {
    private final ArrayList<TPARequest> activeTPA = new ArrayList<>();
    private final HashMap<UUID, Long> recentRequests = new HashMap<>();

    public void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(literal("tpa")
                    .requires(source -> config.teleportation != null && config.teleportation.tpa != null)
                    .requires(Permissions.require("fabricspmutils.teleportation.tpa", true))
                    .then(argument("target", EntityArgumentType.player()).suggests(this::getTPAInitSuggestions)
                            .executes(ctx -> tpaInit(ctx, getPlayer(ctx, "target")))));

            dispatcher.register(literal("tpahere")
                    .requires(source -> config.teleportation != null && config.teleportation.tpa != null)
                    .requires(Permissions.require("fabricspmutils.teleportation.tpa", true))
                    .then(argument("target", EntityArgumentType.player()).suggests(this::getTPAInitSuggestions)
                            .executes(ctx -> tpaHere(ctx, getPlayer(ctx, "target")))));

            dispatcher.register(literal("tpaaccept")
                    .requires(source -> config.teleportation != null && config.teleportation.tpa != null)
                    .requires(Permissions.require("fabricspmutils.teleportation.tpa", true))
                    .then(argument("target", EntityArgumentType.player()).suggests(this::getTPATargetSuggestions)
                            .executes(ctx -> tpaAccept(ctx, getPlayer(ctx, "target"))))
                    .executes(ctx -> tpaAccept(ctx, null)));

            dispatcher.register(literal("tpadeny")
                    .requires(source -> config.teleportation != null && config.teleportation.tpa != null)
                    .requires(Permissions.require("fabricspmutils.teleportation.tpa", true))
                    .then(argument("target", EntityArgumentType.player()).suggests(this::getTPATargetSuggestions)
                            .executes(ctx -> tpaDeny(ctx, getPlayer(ctx, "target"))))
                    .executes(ctx -> tpaDeny(ctx, null)));

            dispatcher.register(literal("tpacancel")
                    .requires(source -> config.teleportation != null && config.teleportation.tpa != null)
                    .requires(Permissions.require("fabricspmutils.teleportation.tpa", true))
                    .then(argument("target", EntityArgumentType.player()).suggests(this::getTPASenderSuggestions)
                            .executes(ctx -> tpaCancel(ctx, getPlayer(ctx, "target"))))
                    .executes(ctx -> tpaCancel(ctx, null)));
        });
    }

    @Nullable
    private static CompletableFuture<Suggestions> filterSuggestionsByInput(SuggestionsBuilder builder, List<String> values) {
        String start = builder.getRemaining().toLowerCase();
        values.stream().filter(s -> s.toLowerCase().startsWith(start)).forEach(builder::suggest);
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> getTPAInitSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        ServerCommandSource scs = context.getSource();

        List<String> activeTargets = Stream.concat(
                activeTPA.stream().map(tpaRequest -> tpaRequest.rTo.getName().asString()),
                activeTPA.stream().map(tpaRequest -> tpaRequest.rFrom.getName().asString())
        ).collect(Collectors.toList());
        List<String> others = Arrays.stream(scs.getMinecraftServer().getPlayerNames())
                .filter(s -> !s.equals(scs.getName()) && !activeTargets.contains(s))
                .collect(Collectors.toList());
        return filterSuggestionsByInput(builder, others);
    }

    private CompletableFuture<Suggestions> getTPATargetSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        List<String> activeTargets = activeTPA.stream().map(tpaRequest -> tpaRequest.rFrom.getName().asString()).collect(Collectors.toList());
        return filterSuggestionsByInput(builder, activeTargets);
    }

    private CompletableFuture<Suggestions> getTPASenderSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        List<String> activeTargets = activeTPA.stream().map(tpaRequest -> tpaRequest.rTo.getName().asString()).collect(Collectors.toList());
        return filterSuggestionsByInput(builder, activeTargets);
    }

    public int tpaInit(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity tTo) throws CommandSyntaxException {
        final ServerPlayerEntity tFrom = ctx.getSource().getPlayer();

        if (tFrom.equals(tTo)) {
            tFrom.sendMessage(new LiteralText("You cannot request to teleport to yourself!").formatted(Formatting.RED), false);
            return 1;
        }

        if (checkCooldown(tFrom)) return 1;

        TPARequest tr = new TPARequest(tFrom, tTo, false, config.teleportation.tpa.timeout * 1000);
        if (activeTPA.stream().anyMatch(tpaRequest -> tpaRequest.equals(tr))) {
            tFrom.sendMessage(new LiteralText("There is already an ongoing request like this!").formatted(Formatting.RED), false);
            return 1;
        }
        tr.setTimeoutCallback(() -> {
            activeTPA.remove(tr);
            tFrom.sendMessage(new LiteralText("Your teleport request to " + tTo.getName().asString() + " has timed out!").formatted(Formatting.RED), false);
            tTo.sendMessage(new LiteralText("Teleport request from " + tFrom.getName().asString() + " has timed out!").formatted(Formatting.RED), false);
        });
        activeTPA.add(tr);

        tFrom.sendMessage(
                new LiteralText("You have requested to teleport to ").formatted(Formatting.LIGHT_PURPLE)
                        .append(new LiteralText(tTo.getName().asString()).formatted(Formatting.AQUA))
                        .append(new LiteralText("\nTo cancel type ").formatted(Formatting.LIGHT_PURPLE))
                        .append(new LiteralText("/tpacancel [<player>]").styled(s ->
                                s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpacancel " + tTo.getName().asString()))
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText("/tpacancel " + tTo.getName().asString())))
                                        .withColor(Formatting.GOLD)))
                        .append(new LiteralText("\nThis request will timeout in " + config.teleportation.tpa.timeout + " seconds.").formatted(Formatting.LIGHT_PURPLE)),
                false);

        tTo.sendMessage(
                new LiteralText(tFrom.getName().asString()).formatted(Formatting.AQUA)
                        .append(new LiteralText(" has requested to teleport to you!").formatted(Formatting.LIGHT_PURPLE))
                        .append(new LiteralText("\nTo accept type ").formatted(Formatting.LIGHT_PURPLE))
                        .append(new LiteralText("/tpaaccept [<player>]").styled(s ->
                                s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaaccept " + tFrom.getName().asString()))
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText("/tpaaccept " + tFrom.getName().asString())))
                                        .withColor(Formatting.GOLD)))
                        .append(new LiteralText("\nTo deny type ").formatted(Formatting.LIGHT_PURPLE))
                        .append(new LiteralText("/tpadeny [<player>]").styled(s ->
                                s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpadeny " + tFrom.getName().asString()))
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText("/tpadeny " + tFrom.getName().asString())))
                                        .withColor(Formatting.GOLD)))
                        .append(new LiteralText("\nThis request will timeout in " + config.teleportation.tpa.timeout + " seconds.").formatted(Formatting.LIGHT_PURPLE)),
                false);
        return 1;
    }

    public int tpaHere(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity tFrom) throws CommandSyntaxException {
        final ServerPlayerEntity tTo = ctx.getSource().getPlayer();

        if (tTo.equals(tFrom)) {
            tTo.sendMessage(new LiteralText("You cannot request for you to teleport to yourself!").formatted(Formatting.RED), false);
            return 1;
        }

        if (checkCooldown(tFrom)) return 1;

        TPARequest tr = new TPARequest(tFrom, tTo, true, config.teleportation.tpa.timeout * 1000);
        if (activeTPA.stream().anyMatch(tpaRequest -> tpaRequest.equals(tr))) {
            tTo.sendMessage(new LiteralText("There is already an ongoing request like this!").formatted(Formatting.RED), false);
            return 1;
        }
        tr.setTimeoutCallback(() -> {
            activeTPA.remove(tr);
            tTo.sendMessage(new LiteralText("Your teleport request for " + tFrom.getName().asString() + " to you has timed out!").formatted(Formatting.RED), false);
            tFrom.sendMessage(new LiteralText("Teleport request for you to " + tTo.getName().asString() + " has timed out!").formatted(Formatting.RED), false);
        });
        activeTPA.add(tr);

        tTo.sendMessage(
                new LiteralText("You have requested for ").formatted(Formatting.LIGHT_PURPLE)
                        .append(new LiteralText(tFrom.getName().asString()).formatted(Formatting.AQUA))
                        .append(new LiteralText(" to teleport to you!\nTo cancel type ").formatted(Formatting.LIGHT_PURPLE))
                        .append(new LiteralText("/tpacancel [<player>]").styled(s ->
                                s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpacancel " + tFrom.getName().asString()))
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText("/tpacancel " + tFrom.getName().asString())))
                                        .withColor(Formatting.GOLD)))
                        .append(new LiteralText("\nThis request will timeout in " + config.teleportation.tpa.timeout + " seconds.").formatted(Formatting.LIGHT_PURPLE)),
                false);

        tFrom.sendMessage(
                new LiteralText(tTo.getName().asString()).formatted(Formatting.AQUA)
                        .append(new LiteralText(" has requested for you to teleport to them!").formatted(Formatting.LIGHT_PURPLE))
                        .append(new LiteralText("\nTo accept type ").formatted(Formatting.LIGHT_PURPLE))
                        .append(new LiteralText("/tpaaccept [<player>]").styled(s ->
                                s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaaccept " + tTo.getName().asString()))
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText("/tpaaccept " + tTo.getName().asString())))
                                        .withColor(Formatting.GOLD)))
                        .append(new LiteralText("\nTo deny type ").formatted(Formatting.LIGHT_PURPLE))
                        .append(new LiteralText("/tpadeny [<player>]").styled(s ->
                                s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpadeny " + tTo.getName().asString()))
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText("/tpadeny " + tTo.getName().asString())))
                                        .withColor(Formatting.GOLD)))
                        .append(new LiteralText("\nThis request will timeout in " + config.teleportation.tpa.timeout + " seconds.").formatted(Formatting.LIGHT_PURPLE)),
                false);
        return 1;
    }

    private boolean checkCooldown(ServerPlayerEntity tFrom) {
        if (recentRequests.containsKey(tFrom.getUuid())) {
            long diff = Instant.now().getEpochSecond() - recentRequests.get(tFrom.getUuid());
            if (diff < config.teleportation.tpa.cooldown) {
                tFrom.sendMessage(new LiteralText("You cannot make a request for ").append(String.valueOf(config.teleportation.tpa.cooldown - diff))
                        .append(" more seconds!").formatted(Formatting.RED), false);
                return true;
            }
        }
        return false;
    }

    private enum TPAAction {
        ACCEPT, DENY, CANCEL
    }

    private TPARequest getTPARequest(ServerPlayerEntity rFrom, ServerPlayerEntity rTo, TPAAction action) {
        Optional<TPARequest> otr = activeTPA.stream()
                .filter(tpaRequest -> tpaRequest.rFrom.equals(rFrom) && tpaRequest.rTo.equals(rTo)).findFirst();

        if (!otr.isPresent()) {
            if (action == TPAAction.CANCEL) {
                rFrom.sendMessage(new LiteralText("No ongoing request!").formatted(Formatting.RED), false);
            } else {
                rTo.sendMessage(new LiteralText("No ongoing request!").formatted(Formatting.RED), false);
            }
            return null;
        }

        return otr.get();
    }

    public int tpaAccept(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity rFrom) throws CommandSyntaxException {
        final ServerPlayerEntity rTo = ctx.getSource().getPlayer();

        if (rFrom == null) {
            TPARequest[] candidates;
            candidates = activeTPA.stream().filter(tpaRequest -> tpaRequest.rTo.equals(rTo)).toArray(TPARequest[]::new);
            if (candidates.length > 1) {
                MutableText text = new LiteralText("You currently have multiple active teleport requests! Please specify whose request to accept.\n").formatted(Formatting.LIGHT_PURPLE);
                Arrays.stream(candidates).map(tpaRequest -> tpaRequest.rFrom.getName().asString()).forEach(name ->
                        text.append(new LiteralText(name).styled(s ->
                                s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaaccept " + name))
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText("/tpaaccept " + name)))
                                        .withColor(Formatting.GOLD))).append(" "));
                rTo.sendMessage(text, false);
                return 1;
            }
            if (candidates.length < 1) {
                rTo.sendMessage(new LiteralText("You currently don't have any teleport requests!").formatted(Formatting.RED), false);
                return 1;
            }
            rFrom = candidates[0].rFrom;
        }

        TPARequest tr = getTPARequest(rFrom, rTo, TPAAction.ACCEPT);
        if (tr == null) return 1;
        TeleportUtils.genericTeleport(
                config.teleportation.tpa.bossBar, config.teleportation.tpa.actionBar, config.teleportation.tpa.standStill,
                rFrom, () -> {
                    if (tr.tFrom.removed || tr.tTo.removed) tr.refreshPlayers();
                    tr.tFrom.teleport(tr.tTo.getServerWorld(), tr.tTo.getX(), tr.tTo.getY(), tr.tTo.getZ(), tr.tTo.yaw, tr.tTo.pitch);
                    switch (config.teleportation.tpa.cooldownMode) {
                        case BothUsers:
                            recentRequests.put(tr.tFrom.getUuid(), Instant.now().getEpochSecond());
                            recentRequests.put(tr.tTo.getUuid(), Instant.now().getEpochSecond());
                            break;
                        case WhoInitiated:
                            recentRequests.put(tr.rFrom.getUuid(), Instant.now().getEpochSecond());
                            break;
                        case WhoTeleported:
                            recentRequests.put(tr.tFrom.getUuid(), Instant.now().getEpochSecond());
                            break;
                    }
                });

        tr.cancelTimeout();
        activeTPA.remove(tr);
        tr.rTo.sendMessage(new LiteralText("You have accepted the teleport request!"), false);
        tr.rFrom.sendMessage(new LiteralText(tr.rTo.getName().asString()).formatted(Formatting.AQUA)
                .append(new LiteralText(" has accepted the teleportation request!").formatted(Formatting.LIGHT_PURPLE)), false);
        return 1;
    }


    public int tpaDeny(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity rFrom) throws CommandSyntaxException {
        final ServerPlayerEntity rTo = ctx.getSource().getPlayer();

        if (rFrom == null) {
            TPARequest[] candidates;
            candidates = activeTPA.stream().filter(tpaRequest -> tpaRequest.rTo.equals(rTo)).toArray(TPARequest[]::new);
            if (candidates.length > 1) {
                MutableText text = new LiteralText("You currently have multiple active teleport requests! Please specify whose request to deny.\n").formatted(Formatting.LIGHT_PURPLE);
                Arrays.stream(candidates).map(tpaRequest -> tpaRequest.rFrom.getName().asString()).forEach(name ->
                        text.append(new LiteralText(name).styled(s ->
                                s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpadeny " + name))
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText("/tpadeny " + name)))
                                        .withColor(Formatting.GOLD))).append(" "));
                rTo.sendMessage(text, false);
                return 1;
            }
            if (candidates.length < 1) {
                rTo.sendMessage(new LiteralText("You currently don't have any teleport requests!").formatted(Formatting.RED), false);
                return 1;
            }
            rFrom = candidates[0].rFrom;
        }

        TPARequest tr = getTPARequest(rFrom, rTo, TPAAction.DENY);
        if (tr == null) return 1;
        tr.cancelTimeout();
        activeTPA.remove(tr);
        tr.rTo.sendMessage(new LiteralText("You have cancelled the teleport request!"), false);
        tr.rFrom.sendMessage(new LiteralText(tr.rTo.getName().asString()).formatted(Formatting.AQUA)
                .append(new LiteralText(" has cancelled the teleportation request!").formatted(Formatting.RED)), false);
        return 1;
    }

    public int tpaCancel(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity rTo) throws CommandSyntaxException {
        final ServerPlayerEntity rFrom = ctx.getSource().getPlayer();

        if (rTo == null) {
            TPARequest[] candidates;
            candidates = activeTPA.stream().filter(tpaRequest -> tpaRequest.rFrom.equals(rFrom)).toArray(TPARequest[]::new);
            if (candidates.length > 1) {
                MutableText text = new LiteralText("You currently have multiple active teleport requests! Please specify which request to cancel.\n").formatted(Formatting.LIGHT_PURPLE);
                Arrays.stream(candidates).map(tpaRequest -> tpaRequest.rFrom.getName().asString()).forEach(name ->
                        text.append(new LiteralText(name).styled(s ->
                                s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpacancel " + name))
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText("/tpacancel " + name)))
                                        .withColor(Formatting.GOLD))).append(" "));
                rFrom.sendMessage(text, false);
                return 1;
            }
            if (candidates.length < 1) {
                rFrom.sendMessage(new LiteralText("You currently don't have any teleport requests!").formatted(Formatting.RED), false);
                return 1;
            }
            rTo = candidates[0].rFrom;
        }

        TPARequest tr = getTPARequest(rFrom, rTo, TPAAction.CANCEL);
        if (tr == null) return 1;
        tr.cancelTimeout();
        activeTPA.remove(tr);
        tr.rFrom.sendMessage(new LiteralText("You have cancelled the teleport request!"), false);
        tr.rTo.sendMessage(new LiteralText(tr.rFrom.getName().asString()).formatted(Formatting.AQUA)
                .append(new LiteralText(" has cancelled the teleportation request!").formatted(Formatting.RED)), false);
        return 1;
    }
}
