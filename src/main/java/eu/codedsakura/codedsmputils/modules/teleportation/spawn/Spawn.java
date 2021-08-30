package eu.codedsakura.codedsmputils.modules.teleportation.spawn;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import eu.codedsakura.codedsmputils.modules.teleportation.CooldownManager;
import eu.codedsakura.codedsmputils.modules.teleportation.back.Back;
import eu.codedsakura.common.BlockUtils;
import eu.codedsakura.common.TeleportUtils;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Random;

import static eu.codedsakura.codedsmputils.CodedSMPUtils.*;
import static net.minecraft.server.command.CommandManager.literal;

public class Spawn {
    public Spawn(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("spawn")
                .requires(Permissions.require("codedsmputils.teleportation.spawn", true))
                .executes(this::run));
    }

    private BlockPos findSuitableSpawnPos(ServerWorld world, boolean useRadius, boolean searchUp, boolean searchDown) {
        BlockPos spawnPos = world.getSpawnPos();
        int spawnRadius = Math.max(0, world.getServer().getSpawnRadius(world));

        if (useRadius) {
            int worldBorderDistance = MathHelper.floor(world.getWorldBorder().getDistanceInsideBorder(spawnPos.getX(), spawnPos.getZ()));

            if (worldBorderDistance < spawnRadius) spawnRadius = worldBorderDistance;
            if (worldBorderDistance <= 1) spawnRadius = 1;
        } else {
            spawnRadius = 0;
        }


        int spawnDiameter = spawnRadius * 2 + 1;
        int spawnArea = spawnDiameter > 46340 /* Math.sqrt(Integer.MAX_VALUE) */
                ? Integer.MAX_VALUE
                : spawnDiameter * spawnDiameter;
        int offset = spawnArea <= 16 ? spawnArea - 1 : 17;
        int randomPosBase = (new Random()).nextInt(spawnArea);
        int y = spawnPos.getY();

        for (int attempts = 0; attempts < spawnArea; attempts++) {
            int randomPos = (randomPosBase + offset * attempts) % spawnArea;
            int x = spawnPos.getX() + (randomPos % spawnDiameter) - spawnRadius;
            int z = spawnPos.getZ() + (randomPos / spawnDiameter) - spawnRadius;
            BlockPos.Mutable newPos = new BlockPos.Mutable(x, spawnPos.getY(), z);

            WorldChunk worldChunk = world.getChunk(ChunkSectionPos.getSectionCoord(x), ChunkSectionPos.getSectionCoord(z));
            int maxHeight = world.getDimension().hasCeiling()
                    ? world.getChunkManager().getChunkGenerator().getSpawnHeight(world)
                    : worldChunk.sampleHeightmap(Heightmap.Type.MOTION_BLOCKING, x & 15, z & 15);
            int minHeight = world.getBottomY();

            int verticalOffset = -2;
            boolean searchingUp = searchUp, searchingDown = searchDown;
            int upHistory = 0, downHistory = 0; // to record if block was air or not
            while (searchingUp || searchingDown) {
                if (searchingUp) {
                    searchingUp = y + verticalOffset < Math.min(world.getTopY(), maxHeight + 3);

                    newPos.setY(y + verticalOffset);
                    BlockState stateUp = world.getBlockState(newPos);
                    upHistory = ((upHistory << 1) & 0b111) | (stateUp.isAir() ? 1 : 0);
                    if (upHistory == 0b011) {
                        if (BlockUtils.isSafe(newPos.setY(y + verticalOffset - 2), world)) {
                            return newPos.setY(y + verticalOffset - 1).toImmutable();
                        }
                    }
                }

                if (searchingDown) {
                    searchingDown = y - verticalOffset > minHeight;

                    newPos.setY(y - verticalOffset);
                    BlockState stateDown = world.getBlockState(newPos);
                    downHistory = ((downHistory << 1) & 0b111) | (stateDown.isAir() ? 1 : 0);
                    if (downHistory == 0b110 && BlockUtils.isSafe(stateDown)) {
                        return newPos.setY(y - verticalOffset + 1).toImmutable();
                    }
                }

                verticalOffset++;
            }
        }

        return null;
    }

    private int run(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayer();

        if (TeleportUtils.cantTeleport(player)) return 1;
        if (CooldownManager.check(player, Spawn.class, "spawn")) return 1;

        MinecraftServer server = ctx.getSource().getServer();
        ServerWorld world = server.getWorld(RegistryKey.of(Registry.WORLD_KEY,
                Identifier.tryParse(CONFIG.teleportation.spawn.world)));
        if (world == null) {
            logger.error("[CSMPU] [/spawn] failed to find a world with id '{}'", CONFIG.teleportation.spawn.world);
            ctx.getSource().sendFeedback(L.get("teleportation.spawn.no-world"), false);
            return 1;
        }

        BlockPos spawnPos = this.findSuitableSpawnPos(world, CONFIG.teleportation.spawn.useRadius,
                CONFIG.teleportation.spawn.searchUp, CONFIG.teleportation.spawn.searchDown);
        if (spawnPos == null) {
            logger.error("[CSMPU] [/spawn] failed to find a spawn position???");
            ctx.getSource().sendFeedback(L.get("teleportation.spawn.not-found"), false);
            return 1;
        }

        TeleportUtils.genericTeleport(
                "teleportation.spawn",
                CONFIG.teleportation.spawn.bossBar, CONFIG.teleportation.spawn.actionBar, CONFIG.teleportation.spawn.standStill,
                player, () -> {
                    if (CONFIG.teleportation.spawn.allowBack) Back.addNewTeleport(player);
                    player.teleport(world, spawnPos.getX() + .5, spawnPos.getY(), spawnPos.getZ() + .5, player.getYaw(), player.getPitch());
                    CooldownManager.addCooldown(Spawn.class, player.getUuid(), CONFIG.teleportation.spawn.cooldown);
                });
        return 0;
    }
}
