package eu.codedsakura.codedsmputils.modules.teleportation.back;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import eu.codedsakura.codedsmputils.modules.teleportation.CooldownManager;
import eu.codedsakura.codedsmputils.requirements.Relation;
import eu.codedsakura.codedsmputils.requirements.RequirementManager;
import eu.codedsakura.common.TeleportUtils;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static eu.codedsakura.codedsmputils.CodedSMPUtils.*;
import static net.minecraft.server.command.CommandManager.literal;

public class Back {
    private static final ArrayList<TeleportLocation> teleports = new ArrayList<>();

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
        teleports.removeIf(location1 -> location1.player == location.player);
        teleports.add(location);
    }

    public Back(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("back")
                .requires(source -> CONFIG.teleportation != null && CONFIG.teleportation.back != null)
                .requires(Permissions.require("fabricspmutils.teleportation.back", true))
                .executes(this::back));
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

    private int back(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        List<TeleportLocation> locations = teleports.stream().filter(location -> location.player == player.getUuid()).collect(Collectors.toList());
        if (locations.size() == 0) {
            ctx.getSource().sendFeedback(L.get("teleportation.back.no-location"), false);
            return 0;
        } else if (locations.size() > 1) {
            ctx.getSource().sendFeedback(L.get("teleportation.back.error"), true);
            return 0;
        }

        if (checkCooldown(player)) return 1;

        TeleportLocation loc = locations.get(0);
        boolean cost;
        Map<String, ?> args = loc.toArguments(ctx.getSource().getMinecraftServer());
        cost = CONFIG.teleportation.back.cost.getValue(args);

        MutableText message = new LiteralText("");

        if (cost) {
            message.append(L.get("teleportation.back.requirements.cost"));

            RequirementManager rm = new RequirementManager(
                    CONFIG.teleportation.back, player, CONFIG.teleportation.back.requirementRelation, args);
            if (rm.hasEnough()) {
                if (rm.consume()) {
                    message.append(L.get("teleportation.back.requirements.consumed"));
                    teleport(player, loc);
                } else {
                    // TODO: L
                }
            } else {
                if (CONFIG.teleportation.back.requirementRelation == Relation.AND) {
                    message.append(L.get("teleportation.back.requirements.missing.and"));
                } else {
                    message.append(L.get("teleportation.back.requirements.missing.or"));
                }

                rm.getAll().forEach(item -> message.append(L.get(item.getMissingLocale(), item.asArguments())));
            }
            logger.info(!rm.hasEnough() ? "false" : "true");
        } else {
            teleport(player, loc);
        }
        ctx.getSource().sendFeedback(message, false);
        return 1;
    }

    private void teleport(ServerPlayerEntity player, TeleportLocation loc) {
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
}
