package com.exiledradio.trailsandtalesbackport.sniffer;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFarmland;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/** 1.20's {@code CropBlock} numbers that both sniffer crops share. */
final class CropGrowth {

    private CropGrowth() {
    }

    /**
     * 1.20's {@code CropBlock.getGrowthSpeed}: 1, plus 1 for the farmland under the crop (3 if moist) and a
     * quarter of that for each of the eight around it, halved when the same crop stands in rows on both
     * axes or on a diagonal.
     */
    static float speed(Block block, IBlockAccess world, BlockPos pos) {
        float speed = 1.0F;
        BlockPos below = pos.down();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                float f = 0.0F;
                IBlockState soil = world.getBlockState(below.add(dx, 0, dz));
                if (soil.getBlock() == Blocks.FARMLAND) {
                    f = 1.0F;
                    if (soil.getValue(BlockFarmland.MOISTURE) > 0) {
                        f = 3.0F;
                    }
                }
                if (dx != 0 || dz != 0) {
                    f /= 4.0F;
                }
                speed += f;
            }
        }
        BlockPos north = pos.north();
        BlockPos south = pos.south();
        BlockPos west = pos.west();
        BlockPos east = pos.east();
        boolean rowX = world.getBlockState(west).getBlock() == block || world.getBlockState(east).getBlock() == block;
        boolean rowZ = world.getBlockState(north).getBlock() == block || world.getBlockState(south).getBlock() == block;
        if (rowX && rowZ) {
            speed /= 2.0F;
        } else if (world.getBlockState(west.north()).getBlock() == block || world.getBlockState(east.north()).getBlock() == block
                || world.getBlockState(east.south()).getBlock() == block || world.getBlockState(west.south()).getBlock() == block) {
            speed /= 2.0F;
        }
        return speed;
    }

    /** 1.20's {@code getRawBrightness(pos, 0)}: the brighter of sky and block light at the crop itself. */
    static int light(World world, BlockPos pos) {
        return world.getLightFromNeighbors(pos);
    }

    /** 1.20's {@code CropBlock.canSurvive} light rule: light 8 or the open sky. */
    static boolean enoughLight(World world, BlockPos pos) {
        return light(world, pos) >= 8 || world.canSeeSky(pos);
    }
}
