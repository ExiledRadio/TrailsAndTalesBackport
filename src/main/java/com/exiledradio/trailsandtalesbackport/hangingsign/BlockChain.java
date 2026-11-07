package com.exiledradio.trailsandtalesbackport.hangingsign;

import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.16's chain, Nether Update's block (family Rule 1): registered only when no installed mod provides one,
 * for the hanging sign recipe. Its states are 1.16's without waterlogging: {@code axis} x, y, z, in 1.12.2's
 * pillar metadata (y 0, x 4, z 8). Nether Update's copy must match, so the owner can rename ours in place.
 *
 * <p>Numbers are 1.16's: hardness 5, blast resistance 6, metal map color, 3/16 thick, chain sounds.
 */
public class BlockChain extends BlockRotatedPillar {

    private static final double LO = 6.5D / 16;
    private static final double HI = 9.5D / 16;
    private static final AxisAlignedBB Y = new AxisAlignedBB(LO, 0, LO, HI, 1, HI);
    private static final AxisAlignedBB X = new AxisAlignedBB(0, LO, LO, 1, HI, HI);
    private static final AxisAlignedBB Z = new AxisAlignedBB(LO, LO, 0, HI, HI, 1);

    public BlockChain(SoundType sound) {
        super(Material.IRON, MapColor.AIR);
        setHardness(5.0F);
        setResistance(2.0F);
        setSoundType(sound);
        // BlockRotatedPillar leaves the first axis value (x) as the default; upright is the default here.
        setDefaultState(this.blockState.getBaseState().withProperty(AXIS, net.minecraft.util.EnumFacing.Axis.Y));
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        switch (state.getValue(AXIS)) {
            case X:
                return X;
            case Z:
                return Z;
            default:
                return Y;
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return face.getAxis() == state.getValue(AXIS) ? BlockFaceShape.CENTER_SMALL : BlockFaceShape.UNDEFINED;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }
}
