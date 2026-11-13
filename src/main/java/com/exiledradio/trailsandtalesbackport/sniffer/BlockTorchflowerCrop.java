package com.exiledradio.trailsandtalesbackport.sniffer;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.IGrowable;
import net.minecraft.block.SoundType;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.EnumPlantType;
import net.minecraftforge.common.ForgeHooks;

import java.util.Random;

/**
 * 1.20's torchflower crop: two ages on farmland, then a torchflower.
 *
 * <p>As 1.20's {@code TorchflowerCropBlock}: a crop whose third age is the torchflower block itself; it skips
 * two random ticks in three, and bone meal adds one age. Breaking it at any age gives back one seed.
 */
public class BlockTorchflowerCrop extends BlockBush implements IGrowable {

    public static final PropertyInteger AGE = PropertyInteger.create("age", 0, 1);
    private static final AxisAlignedBB[] AABB = {
            new AxisAlignedBB(0.3125D, 0.0D, 0.3125D, 0.6875D, 0.375D, 0.6875D),
            new AxisAlignedBB(0.3125D, 0.0D, 0.3125D, 0.6875D, 0.625D, 0.6875D)};

    private Item seeds;
    private Block flower;

    public BlockTorchflowerCrop() {
        setTickRandomly(true);
        setSoundType(SoundType.PLANT);
        setHardness(0.0F);
        setCreativeTab(null);
        setDefaultState(this.blockState.getBaseState().withProperty(AGE, 0));
    }

    void link(Item seeds, Block flower) {
        this.seeds = seeds;
        this.flower = flower;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, AGE);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(AGE, meta & 1);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(AGE);
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return AABB[state.getValue(AGE)];
    }

    @Override
    protected boolean canSustainBush(IBlockState state) {
        return state.getBlock() == Blocks.FARMLAND;
    }

    @Override
    public EnumPlantType getPlantType(IBlockAccess world, BlockPos pos) {
        return EnumPlantType.Crop;
    }

    @Override
    public boolean canBlockStay(World world, BlockPos pos, IBlockState state) {
        return CropGrowth.enoughLight(world, pos) && super.canBlockStay(world, pos, state);
    }

    /** The state one age on: age 1, then the torchflower. */
    IBlockState grown(IBlockState state) {
        return state.getValue(AGE) == 0 ? state.withProperty(AGE, 1) : this.flower.getDefaultState();
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        super.updateTick(world, pos, state, rand);
        // 1.20: two random ticks in three do nothing, the third is a crop's.
        if (!world.isAreaLoaded(pos, 1) || rand.nextInt(3) == 0) {
            return;
        }
        if (CropGrowth.light(world, pos) >= 9) {
            float speed = CropGrowth.speed(this, world, pos);
            if (ForgeHooks.onCropsGrowPre(world, pos, state, rand.nextInt((int) (25.0F / speed) + 1) == 0)) {
                world.setBlockState(pos, grown(state), 2);
                ForgeHooks.onCropsGrowPost(world, pos, state, world.getBlockState(pos));
            }
        }
    }

    @Override
    public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean isClient) {
        return true;
    }

    @Override
    public boolean canUseBonemeal(World world, Random rand, BlockPos pos, IBlockState state) {
        return true;
    }

    @Override
    public void grow(World world, Random rand, BlockPos pos, IBlockState state) {
        world.setBlockState(pos, grown(state), 2);
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        drops.add(new ItemStack(this.seeds));
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return this.seeds;
    }

    @Override
    public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
        return new ItemStack(this.seeds);
    }
}
