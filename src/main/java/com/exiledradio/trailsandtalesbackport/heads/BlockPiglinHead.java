package com.exiledradio.trailsandtalesbackport.heads;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The piglin head, standing and on a wall, as one block the way 1.12.2's skull is one block: {@link #FACING}
 * is {@code up} for a standing head (its 16-step rotation is in {@link TilePiglinHead}) or the direction a
 * wall head looks.
 *
 * <p>1.20's {@code SkullBlock}/{@code PiglinWallSkullBlock}: strength 1, stone sounds, destroyed by pistons,
 * the piglin's wider shape (10 x 8 x 10 standing, 10 x 8 x 8 on a wall), no occlusion.
 */
public class BlockPiglinHead extends BlockContainer {

    public static final PropertyDirection FACING = PropertyDirection.create("facing", f -> f != EnumFacing.DOWN);

    static final AxisAlignedBB STANDING = box(3, 0, 3, 13, 8, 13);
    static final AxisAlignedBB NORTH = box(3, 4, 8, 13, 12, 16);
    static final AxisAlignedBB SOUTH = box(3, 4, 0, 13, 12, 8);
    static final AxisAlignedBB EAST = box(0, 4, 3, 8, 12, 13);
    static final AxisAlignedBB WEST = box(8, 4, 3, 16, 12, 13);

    public BlockPiglinHead() {
        super(Material.CIRCUITS);
        setHardness(1.0F);
        setSoundType(SoundType.STONE);
        setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.UP));
    }

    private static AxisAlignedBB box(double x1, double y1, double z1, double x2, double y2, double z2) {
        return new AxisAlignedBB(x1 / 16, y1 / 16, z1 / 16, x2 / 16, y2 / 16, z2 / 16);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        switch (state.getValue(FACING)) {
            case NORTH:
                return NORTH;
            case SOUTH:
                return SOUTH;
            case EAST:
                return EAST;
            case WEST:
                return WEST;
            default:
                return STANDING;
        }
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TilePiglinHead();
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    public EnumPushReaction getPushReaction(IBlockState state) {
        return EnumPushReaction.DESTROY;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        EnumFacing facing = EnumFacing.byIndex(meta & 7);
        return getDefaultState().withProperty(FACING, facing == EnumFacing.DOWN ? EnumFacing.UP : facing);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getIndex();
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        EnumFacing facing = state.getValue(FACING);
        return facing == EnumFacing.UP ? state : state.withProperty(FACING, rot.rotate(facing));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirror) {
        return state.withRotation(mirror.toRotation(state.getValue(FACING)));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }
}
