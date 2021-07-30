package eu.codedsakura.codedsmputils.modules.teleportation.back;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import eu.codedsakura.codedsmputils.modules.teleportation.CooldownManager;
import eu.codedsakura.codedsmputils.modules.teleportation.lastdeath.LastDeath;
import eu.codedsakura.codedsmputils.requirements.Relation;
import eu.codedsakura.codedsmputils.requirements.RequirementManager;
import eu.codedsakura.codedsmputils.requirements.fulfillables.FAdvancement;
import eu.codedsakura.codedsmputils.requirements.fulfillables.Fulfillable;
import eu.codedsakura.codedsmputils.requirements.fulfillables.StaticItem;
import eu.codedsakura.codedsmputils.requirements.fulfillables.StaticXP;
import eu.codedsakura.common.TeleportUtils;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static eu.codedsakura.codedsmputils.CodedSMPUtils.*;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class Back {
    private static final ArrayList<TeleportLocation> teleports = new ArrayList<>();
    private final ArrayList<FulfillmentRequest> fulfillmentRequests = new ArrayList<>();


    public static class TeleportLocation {
        UUID player;
        long time;
        ServerWorld world;
        double x, y, z;
        float yaw, pitch;

        public TeleportLocation(UUID player, long time, ServerWorld world, double x, double y, double z, float yaw, float pitch) {
            this.player = player;
            this.time = time;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        protected Map<String, ?> toArguments(MinecraftServer server) {
            return new HashMap<String, Object>() {{
                put("player", Objects.requireNonNull(server.getPlayerManager().getPlayer(player)).getName());
                put("uuid", player.toString());
                put("time_of_request", time);
                put("seconds_since_teleport", Instant.now().getEpochSecond() - time);
                put("dimension", world.getRegistryKey().getValue().toString());
            }};
        }
    }

    public static void addNewTeleport(TeleportLocation location) {
        if (CONFIG.teleportation == null || CONFIG.teleportation.back == null || !CONFIG.teleportation.allowBack) return;
        teleports.removeIf(location1 -> location1.player.compareTo(location.player) == 0);
        teleports.add(location);
    }


    public Back(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("back")
                .requires(source -> CONFIG.teleportation != null && CONFIG.teleportation.back != null)
                .requires(Permissions.require("codedsmputils.teleportation.back", true))
                .executes(this::back)
                .then(literal("fulfill")
                        .requires(this::needsToFulfill)
                        .then(argument("type", StringArgumentType.string())
                                .then(argument("value", StringArgumentType.greedyString())
                                        .executes(this::fulfill)))));
    }

    private boolean needsToFulfill(ServerCommandSource source) {
        return fulfillmentRequests.stream().anyMatch(fr -> {
            try {
                return source.getPlayer().getUuid().compareTo(fr.uuid) == 0;
            } catch (CommandSyntaxException ignored) {
                return false;
            }
        });
    }

    private int back(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayer();

        if (TeleportUtils.cantTeleport(player)) return 1;

        if (CONFIG.teleportation.back.allowDeathPoint) {
            Optional<Long> maxBack = teleports.stream()
                    .filter(loc -> loc.player.compareTo(player.getUuid()) == 0)
                    .map(loc -> loc.time).max(Long::compareTo);
            if (!maxBack.isPresent()) {
                if (LastDeath.deaths.containsKey(player.getUuid())) {
                    return LastDeath.run(ctx);
                } // else continue
            } else {
                if (LastDeath.deaths.containsKey(player.getUuid())) {
                    if (maxBack.get() < LastDeath.deaths.get(player.getUuid()).time) {
                        return LastDeath.run(ctx);
                    } // else continue
                } // else continue
            }
        }

        List<TeleportLocation> locations = teleports.stream().filter(location -> location.player == player.getUuid()).collect(Collectors.toList());
        if (locations.size() == 0) {
            ctx.getSource().sendFeedback(L.get("teleportation.back.no-location"), false);
            return 0;
        } else if (locations.size() > 1) {
            ctx.getSource().sendFeedback(L.get("teleportation.back.error"), true);
            logger.error("[CSPMU] Player has more than 1 location in their back cache! This should never happen under normal circumstances!");
            teleports.removeIf(other -> other.player.compareTo(player.getUuid()) == 0);
            return 0;
        }

        if (checkCooldown(player)) return 1;

        TeleportLocation loc = locations.get(0);
        boolean cost;
        Map<String, ?> args = loc.toArguments(ctx.getSource().getServer());
        cost = CONFIG.teleportation.back.cost.getValue(args);

        MutableText message = new LiteralText("");

        if (cost) {
            message.append(L.get("teleportation.back.requirements.cost"));

            RequirementManager rm = new RequirementManager(
                    CONFIG.teleportation.back, player, CONFIG.teleportation.back.requirementRelation, args);
            if (rm.hasEnough()) {

                if (rm.consume()) {

                    message.append(L.get("teleportation.back.requirements.consumed"));
                    rm.getAll().forEach(item -> message.append(L.get(item.getMissingLocale(), item.asArguments())));

                    teleport(player, loc);

                } else {

                    fulfillmentRequests.add(new FulfillmentRequest(player.getUuid()));

                    message.append(L.get("teleportation.back.requirements.choice.or"));
                    rm.getFulfilled().forEach(item -> message.append(FulfillmentRequest.getCommandFromFulfillable(item)));

                    message.append(L.get("teleportation.back.requirements.choice.alt"));
                    rm.getUnfulfilled().forEach(item -> message.append(L.get(item.getMissingLocale(), item.asArguments())));

                }

            } else {

                if (CONFIG.teleportation.back.requirementRelation == Relation.AND) {
                    message.append(L.get("teleportation.back.requirements.missing.and"));
                } else {
                    message.append(L.get("teleportation.back.requirements.missing.or"));
                }

                rm.getAll().forEach(item -> message.append(L.get(item.getMissingLocale(), item.asArguments())));
            }
        } else {
            teleport(player, loc);
        }
        if (message.asTruncatedString(1).length() > 0) {
            ctx.getSource().sendFeedback(message, false);
        }
        return 1;
    }

    private boolean checkCooldown(ServerPlayerEntity tFrom) {
        long remaining = CooldownManager.getCooldownTimeRemaining(Back.class, tFrom.getUuid());
        if (remaining > 0) {
            tFrom.sendMessage(L.get("teleportation.back.cooldown",
                    new HashMap<String, Long>() {{ put("remaining", remaining); }}), false);
            return true;
        }
        return false;
    }

    private int fulfill(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        String type = StringArgumentType.getString(ctx, "type");
        String value = StringArgumentType.getString(ctx, "value");

        List<TeleportLocation> locations = teleports.stream().filter(location -> location.player == player.getUuid()).collect(Collectors.toList());

        TeleportLocation loc = locations.get(0);
        Map<String, ?> args = loc.toArguments(ctx.getSource().getServer());
        RequirementManager rm = new RequirementManager(
                CONFIG.teleportation.back, player, CONFIG.teleportation.back.requirementRelation, args);

        if (rm.hasEnough()) {
            if (rm.consumeSpecific(type, value)) {

                MutableText message = new LiteralText("");
                message.append(L.get("teleportation.back.requirements.consumed"));
                rm.getConsumed().forEach(item -> message.append(L.get(item.getMissingLocale(), item.asArguments())));
                ctx.getSource().sendFeedback(message, false);
                teleport(player, loc);
            } else {
                ctx.getSource().sendFeedback(L.get("teleportation.back.requirements.inventory-missing"), false);
            }
        } else {
            ctx.getSource().sendFeedback(L.get("teleportation.back.requirements.inventory-changed"), false);
        }

        return 1;
    }

    private void teleport(ServerPlayerEntity player, TeleportLocation loc) {
        teleports.removeIf(location -> location.player.compareTo(player.getUuid()) == 0);
        TeleportUtils.genericTeleport(
                "teleportation.back", CONFIG.teleportation.back.bossBar, CONFIG.teleportation.back.actionBar, CONFIG.teleportation.back.standStill,
                player, () -> {
                    if (CONFIG.teleportation.back.allowBack)
                        Back.addNewTeleport(new TeleportLocation(player.getUuid(), Instant.now().getEpochSecond(),
                                (ServerWorld) player.world, player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch()));
                    player.teleport(loc.world, loc.x, loc.y, loc.z, loc.yaw, loc.pitch);
                    CooldownManager.addCooldown(Back.class, player.getUuid(), CONFIG.teleportation.back.cooldown);
                });
    }

    private static class FulfillmentRequest {
        protected UUID uuid;
        protected long time;

        public FulfillmentRequest(UUID uuid) {
            this.uuid = uuid;
//            this.time =
        }

        static Text getCommandFromFulfillable(Fulfillable fulfillable) {
            String type = "";
            if (fulfillable instanceof FAdvancement) {
                logger.warn("[CSMPU] consumable advancements???");
                return new LiteralText("");
            } else if (fulfillable instanceof StaticItem) {
                type = "item";
            } else if (fulfillable instanceof StaticXP) {
                type = "xp";
            }

            final String command = "/back fulfill " + type + " " + fulfillable.getOriginalValue();
            return L.get(fulfillable.getChoiceLocale(), fulfillable.asArguments()).shallowCopy()
                    .styled(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command)));
        }
    }
}
