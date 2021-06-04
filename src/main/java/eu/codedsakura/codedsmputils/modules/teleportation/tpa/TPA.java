package eu.codedsakura.codedsmputils.modules.teleportation.tpa;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import eu.codedsakura.codedsmputils.modules.teleportation.CooldownManager;
import eu.codedsakura.common.TeleportUtils;
import eu.codedsakura.codedsmputils.modules.teleportation.back.Back;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
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

import static eu.codedsakura.codedsmputils.CodedSMPUtils.CONFIG;
import static eu.codedsakura.codedsmputils.CodedSMPUtils.L;
import static net.minecraft.command.argument.EntityArgumentType.getPlayer;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class TPA {
    private final ArrayList<TPARequest> activeTPA = new ArrayList<>();

    public TPA(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("tpa")
                .requires(source -> CONFIG.teleportation != null && CONFIG.teleportation.tpa != null)
                .requires(Permissions.require("fabricspmutils.teleportation.tpa", true))
                .then(argument("target", EntityArgumentType.player()).suggests(this::getTPAInitSuggestions)
                        .executes(ctx -> tpaInit(ctx, getPlayer(ctx, "target")))));

        dispatcher.register(literal("tpahere")
                .requires(source -> CONFIG.teleportation != null && CONFIG.teleportation.tpa != null)
                .requires(Permissions.require("fabricspmutils.teleportation.tpa", true))
                .then(argument("target", EntityArgumentType.player()).suggests(this::getTPAInitSuggestions)
                        .executes(ctx -> tpaHere(ctx, getPlayer(ctx, "target")))));

        dispatcher.register(literal("tpaaccept")
                .requires(source -> CONFIG.teleportation != null && CONFIG.teleportation.tpa != null)
                .requires(Permissions.require("fabricspmutils.teleportation.tpa", true))
                .then(argument("target", EntityArgumentType.player()).suggests(this::getTPATargetSuggestions)
                        .executes(ctx -> tpaAccept(ctx, getPlayer(ctx, "target"))))
                .executes(ctx -> tpaAccept(ctx, null)));

        dispatcher.register(literal("tpadeny")
                .requires(source -> CONFIG.teleportation != null && CONFIG.teleportation.tpa != null)
                .requires(Permissions.require("fabricspmutils.teleportation.tpa", true))
                .then(argument("target", EntityArgumentType.player()).suggests(this::getTPATargetSuggestions)
                        .executes(ctx -> tpaDeny(ctx, getPlayer(ctx, "target"))))
                .executes(ctx -> tpaDeny(ctx, null)));

        dispatcher.register(literal("tpacancel")
                .requires(source -> CONFIG.teleportation != null && CONFIG.teleportation.tpa != null)
                .requires(Permissions.require("fabricspmutils.teleportation.tpa", true))
                .then(argument("target", EntityArgumentType.player()).suggests(this::getTPASenderSuggestions)
                        .executes(ctx -> tpaCancel(ctx, getPlayer(ctx, "target"))))
                .executes(ctx -> tpaCancel(ctx, null)));
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

    private boolean checkCooldown(ServerPlayerEntity tFrom) {
        long remaining = CooldownManager.getCooldownTimeRemaining(TPA.class, tFrom.getUuid());
        if (remaining > 0) {
            tFrom.sendMessage(L.get("teleportation.tpa.cooldown",
                    new HashMap<String, Long>() {{ put("remaining", remaining); }}), false);
            return true;
        }
        return false;
    }

    public int tpaInit(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity tTo) throws CommandSyntaxException {
        final ServerPlayerEntity tFrom = ctx.getSource().getPlayer();

        if (tFrom.equals(tTo)) {
            tFrom.sendMessage(L.get("teleportation.tpa.to-self"), false);
            return 1;
        }

        if (checkCooldown(tFrom)) return 1;

        TPARequest tr = new TPARequest(tFrom, tTo, false, CONFIG.teleportation.tpa.timeout * 1000);
        if (activeTPA.stream().anyMatch(tpaRequest -> tpaRequest.equals(tr))) {
            tFrom.sendMessage(L.get("teleportation.tpa.already-exists"), false);
            return 1;
        }

        Map<String, ?> arguments = tr.asArguments();
        tr.setTimeoutCallback(() -> {
            activeTPA.remove(tr);
            tTo.sendMessage(L.get("teleportation.tpa.init.timed-out.to", arguments), false);
            tFrom.sendMessage(L.get("teleportation.tpa.init.timed-out.from", arguments), false);
        });
        activeTPA.add(tr);

        tFrom.sendMessage(L.get("teleportation.tpa.init.request.from", arguments), false);
        tTo.sendMessage(L.get("teleportation.tpa.init.request.to", arguments), false);
        return 1;
    }

    public int tpaHere(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity tFrom) throws CommandSyntaxException {
        final ServerPlayerEntity tTo = ctx.getSource().getPlayer();

        if (tTo.equals(tFrom)) {
            tFrom.sendMessage(L.get("teleportation.tpa.to-self"), false);
            return 1;
        }

        if (checkCooldown(tFrom)) return 1;

        TPARequest tr = new TPARequest(tFrom, tTo, true, CONFIG.teleportation.tpa.timeout * 1000);
        if (activeTPA.stream().anyMatch(tpaRequest -> tpaRequest.equals(tr))) {
            tFrom.sendMessage(L.get("teleportation.tpa.already-exists"), false);
            return 1;
        }

        Map<String, ?> arguments = tr.asArguments();
        tr.setTimeoutCallback(() -> {
            activeTPA.remove(tr);
            tTo.sendMessage(L.get("teleportation.tpa.here.timed-out.to", arguments), false);
            tFrom.sendMessage(L.get("teleportation.tpa.here.timed-out.from", arguments), false);
        });
        activeTPA.add(tr);

        tFrom.sendMessage(L.get("teleportation.tpa.here.request.from", arguments), false);
        tTo.sendMessage(L.get("teleportation.tpa.here.request.to", arguments), false);
        return 1;
    }

    private enum TPAAction {
        ACCEPT, DENY, CANCEL
    }

    private TPARequest getTPARequest(ServerPlayerEntity rFrom, ServerPlayerEntity rTo, TPAAction action) {
        Optional<TPARequest> otr = activeTPA.stream()
                .filter(tpaRequest -> tpaRequest.rFrom.equals(rFrom) && tpaRequest.rTo.equals(rTo)).findFirst();

        if (!otr.isPresent()) {
            if (action == TPAAction.CANCEL) {
                rFrom.sendMessage(L.get("teleportation.tpa.no-ongoing"), false);
            } else {
                rTo.sendMessage(L.get("teleportation.tpa.no-ongoing"), false);
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
                MutableText text = L.get("teleportation.tpa.accept.multiple-ongoing").copy();
                Arrays.stream(candidates).map(tpaRequest -> tpaRequest.rFrom.getName().asString()).forEach(name ->
                        text.append(new LiteralText(name).styled(s ->
                                s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaaccept " + name))
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText("/tpaaccept " + name)))
                                        .withColor(Formatting.GOLD))).append(" "));
                rTo.sendMessage(text, false);
                return 1;
            }
            if (candidates.length < 1) {
                rTo.sendMessage(L.get("teleportation.tpa.no-ongoing"), false);
                return 1;
            }
            rFrom = candidates[0].rFrom;
        }

        TPARequest tr = getTPARequest(rFrom, rTo, TPAAction.ACCEPT);
        if (tr == null) return 1;
        TeleportUtils.genericTeleport(
                "teleportation.tpa", CONFIG.teleportation.tpa.bossBar, CONFIG.teleportation.tpa.actionBar, CONFIG.teleportation.tpa.standStill,
                rFrom, () -> {
                    if (tr.tFrom.isRemoved() || tr.tTo.isRemoved()) tr.refreshPlayers();
                    if (CONFIG.teleportation.warps.allowBack)
                        Back.addNewTeleport(new Back.TeleportLocation(tr.tFrom.getUuid(), Instant.now().getEpochSecond(),
                                (ServerWorld) tr.tFrom.world, tr.tFrom.getX(), tr.tFrom.getY(), tr.tFrom.getZ(), tr.tFrom.getYaw(), tr.tFrom.getPitch()));
                    tr.tFrom.teleport(tr.tTo.getServerWorld(), tr.tTo.getX(), tr.tTo.getY(), tr.tTo.getZ(), tr.tTo.getYaw(), tr.tTo.getPitch());
                    switch (CONFIG.teleportation.tpa.cooldownMode) {
                        case BothUsers:
                            CooldownManager.addCooldown(TPA.class, tr.tFrom.getUuid(), CONFIG.teleportation.tpa.cooldown);
                            CooldownManager.addCooldown(TPA.class, tr.tTo.getUuid(), CONFIG.teleportation.tpa.cooldown);
                            break;
                        case WhoInitiated:
                            CooldownManager.addCooldown(TPA.class, tr.rFrom.getUuid(), CONFIG.teleportation.tpa.cooldown);
                            break;
                        case WhoTeleported:
                            CooldownManager.addCooldown(TPA.class, tr.tFrom.getUuid(), CONFIG.teleportation.tpa.cooldown);
                            break;
                    }
                });

        tr.cancelTimeout();
        activeTPA.remove(tr);

        Map<String, ?> arguments = tr.asArguments();
        tr.rTo.sendMessage(L.get("teleportation.tpa.accept.to", arguments), false);
        tr.rFrom.sendMessage(L.get("teleportation.tpa.accept.from", arguments), false);
        return 1;
    }


    public int tpaDeny(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity rFrom) throws CommandSyntaxException {
        final ServerPlayerEntity rTo = ctx.getSource().getPlayer();

        if (rFrom == null) {
            TPARequest[] candidates;
            candidates = activeTPA.stream().filter(tpaRequest -> tpaRequest.rTo.equals(rTo)).toArray(TPARequest[]::new);
            if (candidates.length > 1) {
                MutableText text = L.get("teleportation.tpa.deny.multiple-ongoing").copy();
                Arrays.stream(candidates).map(tpaRequest -> tpaRequest.rFrom.getName().asString()).forEach(name ->
                        text.append(new LiteralText(name).styled(s ->
                                s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpadeny " + name))
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText("/tpadeny " + name)))
                                        .withColor(Formatting.GOLD))).append(" "));
                rTo.sendMessage(text, false);
                return 1;
            }
            if (candidates.length < 1) {
                rTo.sendMessage(L.get("teleportation.tpa.no-ongoing"), false);
                return 1;
            }
            rFrom = candidates[0].rFrom;
        }

        TPARequest tr = getTPARequest(rFrom, rTo, TPAAction.DENY);
        if (tr == null) return 1;
        tr.cancelTimeout();
        activeTPA.remove(tr);

        Map<String, ?> arguments = tr.asArguments();
        tr.rTo.sendMessage(L.get("teleportation.tpa.deny.to", arguments), false);
        tr.rFrom.sendMessage(L.get("teleportation.tpa.deny.from", arguments), false);
        return 1;
    }

    public int tpaCancel(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity rTo) throws CommandSyntaxException {
        final ServerPlayerEntity rFrom = ctx.getSource().getPlayer();

        if (rTo == null) {
            TPARequest[] candidates;
            candidates = activeTPA.stream().filter(tpaRequest -> tpaRequest.rFrom.equals(rFrom)).toArray(TPARequest[]::new);
            if (candidates.length > 1) {
                MutableText text = L.get("teleportation.tpa.cancel.multiple-ongoing").copy();
                Arrays.stream(candidates).map(tpaRequest -> tpaRequest.rTo.getName().asString()).forEach(name ->
                        text.append(new LiteralText(name).styled(s ->
                                s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpacancel " + name))
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText("/tpacancel " + name)))
                                        .withColor(Formatting.GOLD))).append(" "));
                rFrom.sendMessage(text, false);
                return 1;
            }
            if (candidates.length < 1) {
                rFrom.sendMessage(L.get("teleportation.tpa.no-ongoing"), false);
                return 1;
            }
            rTo = candidates[0].rTo;
        }

        TPARequest tr = getTPARequest(rFrom, rTo, TPAAction.CANCEL);
        if (tr == null) return 1;
        tr.cancelTimeout();
        activeTPA.remove(tr);

        Map<String, ?> arguments = tr.asArguments();
        tr.rTo.sendMessage(L.get("teleportation.tpa.cancel.to", arguments), false);
        tr.rFrom.sendMessage(L.get("teleportation.tpa.cancel.from", arguments), false);
        return 1;
    }
}
