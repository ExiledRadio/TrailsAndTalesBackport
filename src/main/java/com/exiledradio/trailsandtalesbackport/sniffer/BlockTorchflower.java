package com.exiledradio.trailsandtalesbackport.sniffer;

import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.common.BlockPotted;
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
 *
 * <p>Not vanilla, on request: with {@code sniffer.torchflowerLight} on (the default) the torchflower and its pot
 * give off block light {@link #LIGHT}. The level comes from the block state ({@code getLightValue(IBlockState)}),
 * so the lighting engine, Phosphor, OptiFine and Celeritas all see it.
 */
public class BlockTorchflower extends BlockBush {

    /** The light a torchflower gives off with {@code sniffer.torchflowerLight} on: between a soul torch and a redstone torch. */
    public static final int LIGHT = 10;

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

    /** @return {@link #LIGHT} with {@code sniffer.torchflowerLight} on, else 0 */
    public static int light() {
        return ModConfig.sniffer.torchflowerLight ? LIGHT : 0;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getLightValue(IBlockState state) {
        return light();
    }

    /** The potted torchflower: the same light as the flower. */
    public static final class Potted extends BlockPotted {
        @Override
        @SuppressWarnings("deprecation")
        public int getLightValue(IBlockState state) {
            return light();
        }
    }
}
