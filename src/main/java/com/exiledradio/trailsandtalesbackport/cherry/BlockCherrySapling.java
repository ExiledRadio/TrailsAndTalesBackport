package com.exiledradio.trailsandtalesbackport.cherry;

import net.minecraft.block.BlockBush;
import net.minecraft.block.IGrowable;
import net.minecraft.block.SoundType;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.event.terraingen.TerrainGen;

import java.util.Random;

/**
 * The cherry sapling: vanilla's sapling rules (grows in light 9 or more, one random tick in seven, in two
 * stages; bone meal works 45% of the time) growing a {@link CherryTree}.
 */
public class BlockCherrySapling extends BlockBush implements IGrowable {

    public static final PropertyInteger STAGE = PropertyInteger.create("stage", 0, 1);
    private static final AxisAlignedBB AABB = new AxisAlignedBB(0.1D, 0.0D, 0.1D, 0.9D, 0.8D, 0.9D);

    public BlockCherrySapling(SoundType sound) {
        setDefaultState(this.blockState.getBaseState().withProperty(STAGE, 0));
        setSoundType(sound);
        setHardness(0.0F);
        setTickRandomly(true);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, STAGE);
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(STAGE, meta & 1);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(STAGE);
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return AABB;
    }

    @Override
    public int damageDropped(IBlockState state) {
        return 0;
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        if (world.isRemote) {
            return;
        }
        super.updateTick(world, pos, state, rand);
        if (!world.isAreaLoaded(pos, 1)) {
            return;
        }
        if (world.getLightFromNeighbors(pos.up()) >= 9 && rand.nextInt(7) == 0) {
            grow(world, rand, pos, state);
        }
    }

    @Override
    public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean isClient) {
        return true;
    }

    @Override
    public boolean canUseBonemeal(World world, Random rand, BlockPos pos, IBlockState state) {
        return world.rand.nextFloat() < 0.45D;
    }

    @Override
    public void grow(World world, Random rand, BlockPos pos, IBlockState state) {
        if (state.getValue(STAGE) == 0) {
            world.setBlockState(pos, state.withProperty(STAGE, 1), 4);
            return;
        }
        if (!TerrainGen.saplingGrowTree(world, rand, pos)) {
            return;
        }
        world.setBlockState(pos, net.minecraft.init.Blocks.AIR.getDefaultState(), 4);
        if (!new CherryTree(true).generate(world, rand, pos)) {
            world.setBlockState(pos, state, 4);
        }
    }
}
