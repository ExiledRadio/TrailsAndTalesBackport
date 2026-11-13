package com.exiledradio.trailsandtalesbackport.sniffer;

import net.minecraft.block.BlockBush;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * 1.20's torchflower: a small flower (1.20's {@code FlowerBlock}) on grass, dirt or farmland, offset on the
 * ground like vanilla's flowers, broken instantly. Grown from a torchflower crop.
 */
public class BlockTorchflower extends BlockBush {

    private static final AxisAlignedBB AABB = new AxisAlignedBB(0.3125D, 0.0D, 0.3125D, 0.6875D, 0.625D, 0.6875D);

    public BlockTorchflower() {
        setSoundType(SoundType.PLANT);
        setHardness(0.0F);
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return AABB.offset(state.getOffset(source, pos));
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
}
