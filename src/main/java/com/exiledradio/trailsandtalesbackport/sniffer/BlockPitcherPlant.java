package com.exiledradio.trailsandtalesbackport.sniffer;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

/**
 * 1.20's pitcher plant: a two-block flower (1.20's {@code DoublePlantBlock}, not a tall flower - bone meal
 * does not copy it) on grass, dirt or farmland, offset on the ground like vanilla's flowers.
 *
 * <p>Placed only with room for both halves; a half without its other half breaks. Only the lower half drops
 * the plant: breaking the upper half breaks the lower one with its drop, except in creative mode.
 */
public class BlockPitcherPlant extends BlockBush {

    private Item item;

    public BlockPitcherPlant() {
        setSoundType(SoundType.PLANT);
        setHardness(0.0F);
        setDefaultState(this.blockState.getBaseState().withProperty(Half.PROPERTY, Half.LOWER));
    }

    void link(Item item) {
        this.item = item;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, Half.PROPERTY);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(Half.PROPERTY, (meta & 8) != 0 ? Half.UPPER : Half.LOWER);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(Half.PROPERTY) == Half.UPPER ? 8 : 0;
    }

    @Override
    public EnumOffsetType getOffsetType() {
        return EnumOffsetType.XZ;
    }

    @Override
    @SuppressWarnings("deprecation")
    public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
        return MapColor.FOLIAGE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public net.minecraft.util.math.AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return FULL_BLOCK_AABB.offset(state.getOffset(source, pos));
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        // 1.20: placed only below the build limit and with a replaceable block above.
        return pos.getY() < world.getHeight() - 1 && super.canPlaceBlockAt(world, pos)
                && world.getBlockState(pos.up()).getBlock().isReplaceable(world, pos.up());
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        world.setBlockState(pos.up(), getDefaultState().withProperty(Half.PROPERTY, Half.UPPER), 3);
    }

    /** Places both halves, as 1.20's {@code DoublePlantBlock.placeAt}. */
    public void placeAt(World world, BlockPos pos, int flags) {
        world.setBlockState(pos, getDefaultState(), flags);
        world.setBlockState(pos.up(), getDefaultState().withProperty(Half.PROPERTY, Half.UPPER), flags);
    }

    @Override
    public boolean canBlockStay(World world, BlockPos pos, IBlockState state) {
        if (state.getBlock() != this) {
            return super.canBlockStay(world, pos, state);
        }
        if (state.getValue(Half.PROPERTY) == Half.UPPER) {
            IBlockState below = world.getBlockState(pos.down());
            return below.getBlock() == this && below.getValue(Half.PROPERTY) == Half.LOWER;
        }
        IBlockState above = world.getBlockState(pos.up());
        return super.canBlockStay(world, pos, state)
                && above.getBlock() == this && above.getValue(Half.PROPERTY) == Half.UPPER;
    }

    @Override
    protected void checkAndDropBlock(World world, BlockPos pos, IBlockState state) {
        if (!canBlockStay(world, pos, state)) {
            world.destroyBlock(pos, true);
        }
    }

    @Override
    public void onBlockHarvested(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        if (!world.isRemote && player.capabilities.isCreativeMode && state.getValue(Half.PROPERTY) == Half.UPPER) {
            BlockPos below = pos.down();
            IBlockState lower = world.getBlockState(below);
            if (lower.getBlock() == this && lower.getValue(Half.PROPERTY) == Half.LOWER) {
                world.setBlockState(below, Blocks.AIR.getDefaultState(), 2);
                world.playEvent(player, 2001, below, Block.getStateId(lower));
            }
        }
        super.onBlockHarvested(world, pos, state, player);
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        if (state.getValue(Half.PROPERTY) == Half.LOWER) {
            drops.add(new ItemStack(this.item));
        }
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return this.item;
    }

    @Override
    public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
        return new ItemStack(this.item);
    }
}
