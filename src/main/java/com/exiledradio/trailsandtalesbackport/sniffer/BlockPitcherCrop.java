package com.exiledradio.trailsandtalesbackport.sniffer;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.IGrowable;
import net.minecraft.block.SoundType;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
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

import javax.annotation.Nullable;
import java.util.Random;

/**
 * 1.20's pitcher crop: five ages on farmland, two blocks tall from age 3.
 *
 * <p>As 1.20's {@code PitcherCropBlock}: only the lower half ticks; a random tick grows it one age with the
 * crop chance (25 / growth speed, no light minimum) when it has light 8 or the sky, and from age 3 only when
 * the block above is free, which then becomes the upper half. Bone meal on either half adds one age. A half
 * that loses its other half breaks; only the lower half drops anything - a pod until age 4, then the pitcher
 * plant - and a creative player breaking the upper half takes the lower one without drops.
 */
public class BlockPitcherCrop extends BlockBush implements IGrowable {

    public static final PropertyInteger AGE = PropertyInteger.create("age", 0, 4);
    public static final int MAX_AGE = 4;

    private static final AxisAlignedBB FULL_UPPER = box(3, 0, 3, 13, 15, 13);
    private static final AxisAlignedBB FULL_LOWER = box(3, -1, 3, 13, 16, 13);
    private static final AxisAlignedBB COLLISION_BULB = box(5, -1, 5, 11, 3, 11);
    private static final AxisAlignedBB COLLISION_CROP = box(3, -1, 3, 13, 5, 13);
    private static final AxisAlignedBB[] UPPER_BY_AGE = {box(3, 0, 3, 13, 11, 13), FULL_UPPER};
    private static final AxisAlignedBB[] LOWER_BY_AGE = {COLLISION_BULB, box(3, -1, 3, 13, 14, 13), FULL_LOWER, FULL_LOWER, FULL_LOWER};

    private Item pod;
    private Item plant;

    public BlockPitcherCrop() {
        setTickRandomly(true);
        setSoundType(SoundType.PLANT);
        setHardness(0.0F);
        setCreativeTab(null);
        setDefaultState(this.blockState.getBaseState().withProperty(AGE, 0).withProperty(Half.PROPERTY, Half.LOWER));
    }

    static AxisAlignedBB box(double x1, double y1, double z1, double x2, double y2, double z2) {
        return new AxisAlignedBB(x1 / 16.0D, y1 / 16.0D, z1 / 16.0D, x2 / 16.0D, y2 / 16.0D, z2 / 16.0D);
    }

    void link(Item pod, Item plant) {
        this.pod = pod;
        this.plant = plant;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, AGE, Half.PROPERTY);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(AGE, Math.min(meta & 7, MAX_AGE))
                .withProperty(Half.PROPERTY, (meta & 8) != 0 ? Half.UPPER : Half.LOWER);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(AGE) | (state.getValue(Half.PROPERTY) == Half.UPPER ? 8 : 0);
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        int age = state.getValue(AGE);
        return state.getValue(Half.PROPERTY) == Half.UPPER
                ? UPPER_BY_AGE[Math.min(Math.abs(MAX_AGE - (age + 1)), UPPER_BY_AGE.length - 1)]
                : LOWER_BY_AGE[age];
    }

    @Override
    @Nullable
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        if (state.getValue(AGE) == 0) {
            return COLLISION_BULB;
        }
        return state.getValue(Half.PROPERTY) == Half.LOWER ? COLLISION_CROP : NULL_AABB;
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
    public boolean isReplaceable(IBlockAccess world, BlockPos pos) {
        return false;
    }

    private boolean isLower(IBlockState state) {
        return state.getBlock() == this && state.getValue(Half.PROPERTY) == Half.LOWER;
    }

    private boolean isUpper(IBlockState state) {
        return state.getBlock() == this && state.getValue(Half.PROPERTY) == Half.UPPER;
    }

    @Override
    public boolean canBlockStay(World world, BlockPos pos, IBlockState state) {
        if (state.getBlock() != this) {
            return super.canBlockStay(world, pos, state);
        }
        if (state.getValue(Half.PROPERTY) == Half.UPPER) {
            return isLower(world.getBlockState(pos.down()));
        }
        return canSustainBush(world.getBlockState(pos.down())) && CropGrowth.enoughLight(world, pos)
                && (state.getValue(AGE) < 3 || isUpper(world.getBlockState(pos.up())));
    }

    /** 1.20's {@code updateShape} turning a half that cannot survive into air - destroyed, with its drops. */
    @Override
    protected void checkAndDropBlock(World world, BlockPos pos, IBlockState state) {
        if (!canBlockStay(world, pos, state)) {
            world.destroyBlock(pos, true);
        }
    }

    @Override
    public void onBlockHarvested(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        // 1.20's preventCreativeDropFromBottomPart.
        if (!world.isRemote && player.capabilities.isCreativeMode && state.getValue(Half.PROPERTY) == Half.UPPER) {
            BlockPos below = pos.down();
            IBlockState lower = world.getBlockState(below);
            if (isLower(lower)) {
                world.setBlockState(below, Blocks.AIR.getDefaultState(), 2);
                world.playEvent(player, 2001, below, Block.getStateId(lower));
            }
        }
        super.onBlockHarvested(world, pos, state, player);
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        checkAndDropBlock(world, pos, state);
        state = world.getBlockState(pos);
        if (!isLower(state) || state.getValue(AGE) >= MAX_AGE || !world.isAreaLoaded(pos, 1)) {
            return;
        }
        float speed = CropGrowth.speed(this, world, pos);
        if (ForgeHooks.onCropsGrowPre(world, pos, state, rand.nextInt((int) (25.0F / speed) + 1) == 0)) {
            grow(world, state, pos, 1);
            ForgeHooks.onCropsGrowPost(world, pos, state, world.getBlockState(pos));
        }
    }

    /** 1.20's {@code PitcherCropBlock.grow}, on the lower half. */
    void grow(World world, IBlockState lower, BlockPos pos, int by) {
        int age = Math.min(lower.getValue(AGE) + by, MAX_AGE);
        if (canGrow(world, pos, lower, age)) {
            world.setBlockState(pos, lower.withProperty(AGE, age), 2);
            if (age >= 3) {
                world.setBlockState(pos.up(), getDefaultState().withProperty(AGE, age).withProperty(Half.PROPERTY, Half.UPPER), 3);
            }
        }
    }

    private boolean canGrow(World world, BlockPos pos, IBlockState lower, int age) {
        if (lower.getValue(AGE) >= MAX_AGE || !CropGrowth.enoughLight(world, pos)) {
            return false;
        }
        if (age < 3) {
            return true;
        }
        IBlockState above = world.getBlockState(pos.up());
        return above.getBlock().isAir(above, world, pos.up()) || above.getBlock() == this;
    }

    @Nullable
    private BlockPos lowerHalf(World world, BlockPos pos, IBlockState state) {
        if (isLower(state)) {
            return pos;
        }
        return isLower(world.getBlockState(pos.down())) ? pos.down() : null;
    }

    @Override
    public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean isClient) {
        BlockPos lower = lowerHalf(world, pos, state);
        if (lower == null) {
            return false;
        }
        IBlockState s = world.getBlockState(lower);
        return canGrow(world, lower, s, s.getValue(AGE) + 1);
    }

    @Override
    public boolean canUseBonemeal(World world, Random rand, BlockPos pos, IBlockState state) {
        return true;
    }

    @Override
    public void grow(World world, Random rand, BlockPos pos, IBlockState state) {
        BlockPos lower = lowerHalf(world, pos, state);
        if (lower != null) {
            grow(world, world.getBlockState(lower), lower, 1);
        }
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        if (state.getValue(Half.PROPERTY) == Half.LOWER) {
            drops.add(new ItemStack(state.getValue(AGE) >= MAX_AGE ? this.plant : this.pod));
        }
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return this.pod;
    }

    @Override
    public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
        return new ItemStack(this.pod);
    }
}
