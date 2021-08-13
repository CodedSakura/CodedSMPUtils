package eu.codedsakura.common;

import net.minecraft.block.BlockState;
import net.minecraft.block.Material;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class BlockUtils {
    public static boolean isSafe(Material material) {
        return !material.isLiquid() && material != Material.FIRE;
    }

    public static boolean isSafe(BlockState blockState) {
        return BlockUtils.isSafe(blockState.getMaterial());
    }

    public static boolean isSafe(BlockPos.Mutable blockPos, ServerWorld world) {
        return BlockUtils.isSafe(world.getBlockState(blockPos));
    }
}
