package com.exiledradio.trailsandtalesbackport.biome;

import com.exiledradio.trailsandtalesbackport.cherry.BlockPinkPetals;

import net.minecraft.block.BlockTallGrass;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;

/** The self-test's way into this package's package-private pieces. Compiled in only for test runs. */
public final class GroveTestAccess {

    private GroveTestAccess() {
    }

    public static boolean takes(int id) {
        return CherryGroveLayer.takes(id);
    }

    public static BlockPos worldSurface(World world, BlockPos column) {
        return CherryGroveDecorator.worldSurface(world, column);
    }

    public static BlockPos motionBlocking(World world, BlockPos column) {
        return CherryGroveDecorator.motionBlocking(world, column);
    }

    public static BlockPos oceanFloor(World world, BlockPos column) {
        return CherryGroveDecorator.oceanFloor(world, column);
    }

    public static BlockPos worldSurfaceWG(World world, BlockPos column) {
        return CherryGroveDecorator.worldSurfaceWG(world, column);
    }

    public static BlockPos oceanFloorWG(World world, BlockPos column) {
        return CherryGroveDecorator.oceanFloorWG(world, column);
    }

    public static boolean validTreePos(World world, BlockPos pos) {
        return CherryGroveDecorator.validTreePos(world, pos);
    }

    public static boolean saplingSurvives(World world, BlockPos pos) {
        return CherryGroveDecorator.saplingSurvives(world, pos);
    }

    public static int petals(World world, Random rand, BlockPos origin, BlockPinkPetals petals) {
        return CherryGroveDecorator.patch(world, rand, origin, CherryGroveDecorator.PETALS_TRIES, CherryGroveDecorator.PETALS_SPREAD,
                CherryGroveDecorator.PETALS_Y_SPREAD, null, petals);
    }

    public static int grassPatch(World world, Random rand, BlockPos origin) {
        return CherryGroveDecorator.patch(world, rand, origin, CherryGroveDecorator.GRASS_TRIES, CherryGroveDecorator.PATCH_SPREAD,
                CherryGroveDecorator.PATCH_Y_SPREAD,
                Blocks.TALLGRASS.getDefaultState().withProperty(BlockTallGrass.TYPE, BlockTallGrass.EnumType.GRASS), null);
    }

    public static int tallGrassPatch(World world, Random rand, BlockPos origin) {
        return CherryGroveDecorator.tallGrassPatch(world, rand, origin);
    }

    public static void recordStats(long[] into) {
        CherryGroveDecorator.stats = into;
    }

    public static void recordTrees(List<BlockPos> into) {
        CherryGroveDecorator.treeTries = into;
    }
}
