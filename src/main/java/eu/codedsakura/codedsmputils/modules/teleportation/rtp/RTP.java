package eu.codedsakura.codedsmputils.modules.teleportation.rtp;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import eu.codedsakura.codedsmputils.modules.teleportation.CooldownManager;
import eu.codedsakura.codedsmputils.modules.teleportation.back.Back;
import eu.codedsakura.common.TeleportUtils;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.block.BlockState;
import net.minecraft.block.Material;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

import static eu.codedsakura.codedsmputils.CodedSMPUtils.*;
import static net.minecraft.server.command.CommandManager.literal;

public class RTP {
    private static final int MAX_ATTEMPTS = 100;

    public RTP(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("rtp")
                .requires(source -> CONFIG.teleportation != null && CONFIG.teleportation.rtp != null)
                .requires(Permissions.require("codedsmputils.teleportation.rtp", true))
                .requires(this::checkIfDimAllowed)
                .executes(this::rtpInit));
    }

    private boolean checkIfDimAllowed(ServerCommandSource source) {
        try {
            String playerDim = source.getPlayer().getServerWorld().getRegistryKey().getValue().toString();
            if (CONFIG.teleportation.rtp.blacklistDims != null) {
                if (Arrays.stream(CONFIG.teleportation.rtp.blacklistDims.split(",")).anyMatch(s -> s.equalsIgnoreCase(playerDim))) {
                    return false;
                }
            }
            if (CONFIG.teleportation.rtp.whitelistDims != null) {
                return Arrays.stream(CONFIG.teleportation.rtp.whitelistDims.split(",")).anyMatch(s -> s.equalsIgnoreCase(playerDim));
            }
            return true;
        } catch (CommandSyntaxException e) {
            return true;
        }
    }

    private boolean checkCooldown(ServerPlayerEntity tFrom) {
        long remaining = CooldownManager.getCooldownTimeRemaining(RTP.class, tFrom.getUuid());
        if (remaining > 0) {
            tFrom.sendMessage(L.get("teleportation.rtp.cooldown",
                    new HashMap<String, Long>() {{ put("remaining", remaining); }}), false);
            return true;
        }
        return false;
    }

    private int rtpInit(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        Random r = new Random();
        ServerPlayerEntity player = ctx.getSource().getPlayer();

        if (TeleportUtils.cantTeleport(player)) return 1;
        if (checkCooldown(player)) return 1;
        CooldownManager.addCooldown(RTP.class, player.getUuid(), CONFIG.teleportation.rtp.cooldown);

        Thread wait = new Thread(() -> {
            try {
                Thread.sleep(250);
            } catch (InterruptedException ignored) {
                return;
            }
            player.sendMessage(L.get("teleportation.rtp.wait"), false);
        });
        wait.start();

        ServerWorld world = ctx.getSource().getWorld();
        final long timeStart = System.currentTimeMillis();
        Thread runner = new Thread(() -> {
            int attempts = 0;
            while (attempts < MAX_ATTEMPTS) {
                BlockPos.Mutable blockPos = getNewCoords(r, world.getLogicalHeight(), player);

                boolean[] airHistory = new boolean[3];
                BlockState blockState;
                while (blockPos.getY() > world.getBottomY()) {
                    blockState = world.getBlockState(blockPos);
                    airHistory[2] = airHistory[1];
                    airHistory[1] = airHistory[0];
                    airHistory[0] = blockState.isAir();

                    if (!airHistory[0] && airHistory[1] && airHistory[2]) {
                        if (checkAndTP(player, blockPos, blockState)) {
                            wait.interrupt();
                            logger.info("[CSMPU] RTP took {} attempt(s), {} millisecond(s)",
                                    attempts, System.currentTimeMillis() - timeStart);
                            return;
                        }
                        break;
                    }

                    blockPos.move(Direction.DOWN);
                }
                attempts++;
            }

            ctx.getSource().sendFeedback(L.get("teleportation.rtp.failed"), false);
        });
        runner.start();
        return 1;
    }

    private BlockPos.Mutable getNewCoords(Random r, double height, ServerPlayerEntity player) {
        double x, z;

        eu.codedsakura.codedsmputils.config.elements.teleportation.RTP conf = CONFIG.teleportation.rtp;

        switch (conf.areaShape) {
            case Square:
                do {
                    x = MathHelper.nextDouble(r, -conf.maxRange, conf.minRange);
                } while (Math.abs(x) < conf.minRange);
                do {
                    z = MathHelper.nextDouble(r, -conf.maxRange, conf.maxRange);
                } while (Math.abs(z) < conf.minRange);
                break;
            case Circle:
                double a = MathHelper.nextDouble(r, 0, Math.PI);
                double l = MathHelper.nextDouble(r, conf.minRange, conf.maxRange);
                x = Math.cos(a) * l;
                z = Math.sin(a) * l;
                break;
            default:
                x = 0;
                z = 0;
        }

        switch (conf.areaCenter) {
            case Zero:
                return new BlockPos.Mutable(x, height, z);
            case Player:
                return new BlockPos.Mutable(
                        player.getX() + x,
                        height,
                        player.getZ() + z
                );
            default:
                return new BlockPos.Mutable(0, height, 0);
        }

    }

    private boolean checkAndTP(ServerPlayerEntity player, BlockPos blockPos, BlockState blockState) {
        Material material = blockState.getMaterial();
        if (!material.isLiquid() && material != Material.FIRE) {
            TeleportUtils.genericTeleport(
                    "teleportation.rtp",
                    CONFIG.teleportation.rtp.bossBar, CONFIG.teleportation.rtp.actionBar, CONFIG.teleportation.rtp.standStill,
                    player, () -> {
                        if (CONFIG.teleportation.rtp.allowBack)
                            Back.addNewTeleport(new Back.TeleportLocation(player.getUuid(), Instant.now().getEpochSecond(),
                                    (ServerWorld) player.world, player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch()));
                        player.teleport(blockPos.getX() + .5, blockPos.getY() + 1, blockPos.getZ() + .5);
                    });
            return true;
        }
        return false;
    }
}
