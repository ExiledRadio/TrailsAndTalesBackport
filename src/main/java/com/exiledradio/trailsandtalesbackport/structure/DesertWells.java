package com.exiledradio.trailsandtalesbackport.structure;

import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import net.minecraft.block.BlockSand;
import net.minecraft.block.BlockStoneSlab;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.util.Random;

/**
 * 1.20's desert well ({@code DesertWellFeature}) in place of 1.12.2's: its basin is a block deeper, sand under the water
 * cross, and two suspicious sand with the desert well table, one under a random block of the cross and one under that.
 *
 * <p>1.12.2's desert decoration asks the {@code DESERT_WELL} event, then draws its 1-in-1000 chance and position from
 * the decoration random. This denies the event and makes the same draws itself, so every later draw of the chunk's
 * decoration (fossils) is what it was; the well is then 1.20's. The suspicious sand picks come from a random of the
 * well's own position, so they draw nothing from the decoration random either (1.20 draws them from its feature
 * random, which has no counterpart in 1.12.2's terrain). Caves &amp; Cliffs' 1.18 terrain places its own wells
 * ({@code docs/streams/structures.md}).
 */
public final class DesertWells {

    private DesertWells() {
    }

    public static final ResourceLocation TABLE = new ResourceLocation(TrailsAndTalesBackport.MOD_ID, "archaeology/desert_well");

    static final IBlockState SAND = Blocks.SAND.getDefaultState().withProperty(BlockSand.VARIANT, BlockSand.EnumType.SAND);
    static final IBlockState SANDSTONE = Blocks.SANDSTONE.getDefaultState();
    static final IBlockState SLAB = Blocks.STONE_SLAB.getDefaultState().withProperty(BlockStoneSlab.VARIANT, BlockStoneSlab.EnumType.SAND);
    static final IBlockState WATER = Blocks.WATER.getDefaultState();

    /** 1.12.2's {@code BiomeDesert.decorate} well step, with 1.20's well. */
    public static void decorate(DecorateBiomeEvent.Decorate event) {
        if (event.getType() != DecorateBiomeEvent.Decorate.EventType.DESERT_WELL || event.getResult() == Event.Result.DENY) {
            return;
        }
        World world = event.getWorld();
        event.setResult(Event.Result.DENY);
        Random rand = event.getRand();
        if (rand.nextInt(1000) == 0) {
            ChunkPos c = event.getChunkPos();
            BlockPos pos = new BlockPos(c.x << 4, 0, c.z << 4);
            int i = rand.nextInt(16) + 8;
            int j = rand.nextInt(16) + 8;
            BlockPos at = world.getHeight(pos.add(i, 0, j)).up();
            place(world, at);
        }
    }

    /** {@code DesertWellFeature.place}. */
    public static boolean place(World world, BlockPos origin) {
        BlockPos pos = origin.up();
        while (world.isAirBlock(pos) && pos.getY() > 2) {
            pos = pos.down();
        }
        if (world.getBlockState(pos) != SAND) {
            return false;
        }
        for (int i = -2; i <= 2; ++i) {
            for (int j = -2; j <= 2; ++j) {
                if (world.isAirBlock(pos.add(i, -1, j)) && world.isAirBlock(pos.add(i, -2, j))) {
                    return false;
                }
            }
        }
        for (int y = -2; y <= 0; ++y) {
            for (int x = -2; x <= 2; ++x) {
                for (int z = -2; z <= 2; ++z) {
                    world.setBlockState(pos.add(x, y, z), SANDSTONE, 2);
                }
            }
        }
        world.setBlockState(pos, WATER, 2);
        for (EnumFacing f : EnumFacing.Plane.HORIZONTAL) {
            world.setBlockState(pos.offset(f), WATER, 2);
        }
        BlockPos below = pos.down();
        world.setBlockState(below, SAND, 2);
        for (EnumFacing f : EnumFacing.Plane.HORIZONTAL) {
            world.setBlockState(below.offset(f), SAND, 2);
        }
        for (int x = -2; x <= 2; ++x) {
            for (int z = -2; z <= 2; ++z) {
                if (x == -2 || x == 2 || z == -2 || z == 2) {
                    world.setBlockState(pos.add(x, 1, z), SANDSTONE, 2);
                }
            }
        }
        world.setBlockState(pos.add(2, 1, 0), SLAB, 2);
        world.setBlockState(pos.add(-2, 1, 0), SLAB, 2);
        world.setBlockState(pos.add(0, 1, 2), SLAB, 2);
        world.setBlockState(pos.add(0, 1, -2), SLAB, 2);
        for (int x = -1; x <= 1; ++x) {
            for (int z = -1; z <= 1; ++z) {
                world.setBlockState(pos.add(x, 4, z), x == 0 && z == 0 ? SANDSTONE : SLAB, 2);
            }
        }
        for (int y = 1; y <= 3; ++y) {
            world.setBlockState(pos.add(-1, y, -1), SANDSTONE, 2);
            world.setBlockState(pos.add(-1, y, 1), SANDSTONE, 2);
            world.setBlockState(pos.add(1, y, -1), SANDSTONE, 2);
            world.setBlockState(pos.add(1, y, 1), SANDSTONE, 2);
        }
        // List.of(pos, east, south, west, north), Util.getRandom twice.
        BlockPos[] cross = {pos, pos.east(), pos.south(), pos.west(), pos.north()};
        Random random = VanillaRandom.at(pos);
        BlockPos first = cross[random.nextInt(cross.length)].down(1);
        BlockPos second = cross[random.nextInt(cross.length)].down(2);
        Archaeology.set(world, first, false, TABLE, VanillaRandom.asLong(first));
        Archaeology.set(world, second, false, TABLE, VanillaRandom.asLong(second));
        Archaeology.unscheduleFalling(world, new net.minecraft.world.gen.structure.StructureBoundingBox(pos.add(-2, -2, -2), pos.add(2, 4, 2)));
        return true;
    }
}
