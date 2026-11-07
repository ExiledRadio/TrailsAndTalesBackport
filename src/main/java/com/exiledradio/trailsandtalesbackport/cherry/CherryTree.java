package com.exiledradio.trailsandtalesbackport.cherry;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;
import net.minecraftforge.common.IPlantable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * 1.20's cherry tree ({@code configured_feature/cherry} in the 1.20.6 jar): a trunk of 7-8 that splits into
 * one to three branches, each ending under a wide, flat blossom canopy with leaves hanging from its rim.
 *
 * <h2>Trunk ({@code cherry_trunk_placer})</h2>
 * Base height 7, plus 0-1. Branch count 1, 2 or 3 (equally likely); with 3 the trunk itself goes to the
 * top and carries the middle canopy. Each side branch starts 3-4 below the top, runs 2-4 out (one more if
 * it starts low), and ends 0-1 below the top, walking toward its end with sideways logs and upright
 * ones in the proportion the slope needs. Two side branches leave in opposite directions.
 *
 * <h2>Canopy ({@code cherry_foliage_placer}: radius 4, height 5, offset 0)</h2>
 * Rows around each branch end, from 3 above it down to 1 below: radii 1, 2, 3, 3, 2. Corners are left out,
 * the edge of a radius-3 row is thinned (25%), the bottom wide row loses edge leaves (25%), and the two
 * lowest rows grow hanging leaves from their rim (1 in 6), each sometimes one longer (1 in 3).
 *
 * <p>The numbers are the data file's; the placement walks are written from what the two placers produce.
 */
public class CherryTree extends WorldGenAbstractTree {

    private static final int RADIUS = 4;
    private static final int HEIGHT = 5;
    private static final float WIDE_BOTTOM_HOLE = 0.25F;
    private static final float CORNER_HOLE = 0.25F;
    private static final float HANGING = 0.16666667F;
    private static final float HANGING_EXTENSION = 0.33333334F;

    private final Set<BlockPos> leaves = new HashSet<BlockPos>();
    private IBlockState log;
    private IBlockState leaf;

    public CherryTree(boolean notify) {
        super(notify);
    }

    @Override
    public boolean generate(World world, Random rand, BlockPos pos) {
        Block logBlock = CherryContent.set == null ? null : CherryContent.set.log;
        if (logBlock == null || CherryContent.leaves == null) {
            return false;
        }
        this.log = logBlock.getDefaultState();
        this.leaf = CherryContent.leaves.getDefaultState().withProperty(BlockLeaves.CHECK_DECAY, false);
        this.leaves.clear();

        int height = 7 + rand.nextInt(2);
        if (pos.getY() < 1 || pos.getY() + height + 4 > world.getHeight()) {
            return false;
        }
        BlockPos below = pos.down();
        IBlockState soil = world.getBlockState(below);
        if (!soil.getBlock().canSustainPlant(soil, world, below, EnumFacing.UP, (IPlantable) Blocks.SAPLING)) {
            return false;
        }
        // two_layers_feature_size(limit 1, lower 0, upper 2): the trunk's own block at the base, a 5x5
        // column above it, all free.
        for (int y = 0; y <= height + 1; y++) {
            int r = y < 1 ? 0 : 2;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos at = pos.add(dx, y, dz);
                    if (!(y == 0 && dx == 0 && dz == 0) && !free(world, at)) {
                        return false;
                    }
                }
            }
        }
        soil.getBlock().onPlantGrow(soil, world, below, pos);

        for (BlockPos center : trunk(world, rand, height, pos)) {
            canopy(world, rand, center);
        }
        return true;
    }

    // ---------------------------------------------------------------------------------------------

    private List<BlockPos> trunk(World world, Random rand, int height, BlockPos pos) {
        int first = Math.max(0, height - 1 + startOffset(rand));
        int second = Math.max(0, height - 1 + startOffset(rand));
        if (second >= first) {
            second++;
        }
        int branches = 1 + rand.nextInt(3);
        boolean middle = branches == 3;
        boolean two = branches >= 2;
        int trunkHeight = middle ? height : two ? Math.max(first, second) + 1 : first + 1;

        for (int y = 0; y < trunkHeight; y++) {
            placeLog(world, pos.up(y), EnumFacing.Axis.Y);
        }
        List<BlockPos> canopies = new ArrayList<BlockPos>();
        if (middle) {
            canopies.add(pos.up(trunkHeight));
        }
        EnumFacing dir = EnumFacing.Plane.HORIZONTAL.random(rand);
        canopies.add(branch(world, rand, height, pos, dir, first, first < trunkHeight - 1));
        if (two) {
            canopies.add(branch(world, rand, height, pos, dir.getOpposite(), second, second < trunkHeight - 1));
        }
        return canopies;
    }

    /** branch_start_offset_from_top: -4 to -3 */
    private static int startOffset(Random rand) {
        return -4 + rand.nextInt(2);
    }

    /** @return where the branch's canopy is centered: one above its last log */
    private BlockPos branch(World world, Random rand, int height, BlockPos pos, EnumFacing dir, int start,
                            boolean trunkContinues) {
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(pos.up(start));
        int endY = height - 1 + (-1 + rand.nextInt(2));
        boolean longer = trunkContinues || endY < start;
        int length = 2 + rand.nextInt(3) + (longer ? 1 : 0);
        BlockPos end = pos.offset(dir, length).up(endY);
        int sideways = longer ? 2 : 1;
        for (int i = 0; i < sideways; i++) {
            at.move(dir);
            placeLog(world, at, dir.getAxis());
        }
        EnumFacing vertical = end.getY() > at.getY() ? EnumFacing.UP : EnumFacing.DOWN;
        while (true) {
            int distance = Math.abs(end.getX() - at.getX()) + Math.abs(end.getY() - at.getY()) + Math.abs(end.getZ() - at.getZ());
            if (distance == 0) {
                return end.up();
            }
            float chance = Math.abs(end.getY() - at.getY()) / (float) distance;
            boolean up = rand.nextFloat() < chance;
            at.move(up ? vertical : dir);
            placeLog(world, at, up ? EnumFacing.Axis.Y : dir.getAxis());
        }
    }

    private void placeLog(World world, BlockPos pos, EnumFacing.Axis axis) {
        if (free(world, pos)) {
            setBlockAndNotifyAdequately(world, pos.toImmutable(), this.log.withProperty(BlockRotatedPillar.AXIS, axis));
        }
    }

    // ---------------------------------------------------------------------------------------------

    private void canopy(World world, Random rand, BlockPos center) {
        int r = RADIUS - 1;
        row(world, rand, center, r - 2, HEIGHT - 3);
        row(world, rand, center, r - 1, HEIGHT - 4);
        for (int y = HEIGHT - 5; y >= 0; y--) {
            row(world, rand, center, r, y);
        }
        hangingRow(world, rand, center, r, -1);
        hangingRow(world, rand, center, r - 1, -2);
    }

    private void row(World world, Random rand, BlockPos center, int range, int y) {
        for (int dx = -range; dx <= range; dx++) {
            for (int dz = -range; dz <= range; dz++) {
                if (!skip(rand, Math.abs(dx), y, Math.abs(dz), range)) {
                    leaf(world, center.add(dx, y, dz));
                }
            }
        }
    }

    private static boolean skip(Random rand, int x, int y, int z, int range) {
        if (y == -1 && (x == range || z == range) && rand.nextFloat() < WIDE_BOTTOM_HOLE) {
            return true;
        }
        boolean corner = x == range && z == range;
        if (range > 2) {
            return corner || (x + z > range * 2 - 2 && rand.nextFloat() < CORNER_HOLE);
        }
        return corner && rand.nextFloat() < CORNER_HOLE;
    }

    /** A row, then leaves hanging from under its rim where a leaf of this tree is directly above. */
    private void hangingRow(World world, Random rand, BlockPos center, int range, int y) {
        row(world, rand, center, range, y);
        BlockPos origin = center.down();
        for (EnumFacing dir : EnumFacing.Plane.HORIZONTAL) {
            EnumFacing side = dir.rotateY();
            // One-wide trunks only, so the rim is the same distance out on every side.
            BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(center.up(y - 1).offset(side, range).offset(dir, -range));
            for (int k = -range; k < range; k++) {
                boolean above = this.leaves.contains(at.up());
                if (above && extension(world, rand, HANGING, origin, at)) {
                    BlockPos lower = at.down();
                    extension(world, rand, HANGING_EXTENSION, origin, lower);
                }
                at.move(dir);
            }
        }
    }

    private boolean extension(World world, Random rand, float chance, BlockPos origin, BlockPos at) {
        int manhattan = Math.abs(at.getX() - origin.getX()) + Math.abs(at.getY() - origin.getY()) + Math.abs(at.getZ() - origin.getZ());
        if (manhattan >= 7 || rand.nextFloat() > chance) {
            return false;
        }
        return leaf(world, at);
    }

    private boolean leaf(World world, BlockPos pos) {
        if (!free(world, pos)) {
            return false;
        }
        BlockPos at = pos.toImmutable();
        setBlockAndNotifyAdequately(world, at, this.leaf);
        this.leaves.add(at);
        return true;
    }

    /** 1.20's {@code TreeFeature.validTreePos}: air, leaves, or a replaceable plant. */
    private static boolean free(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        return block.isAir(state, world, pos) || block.isLeaves(state, world, pos)
                || (block.isReplaceable(world, pos) && state.getMaterial() != Material.WATER && state.getMaterial() != Material.LAVA);
    }
}
