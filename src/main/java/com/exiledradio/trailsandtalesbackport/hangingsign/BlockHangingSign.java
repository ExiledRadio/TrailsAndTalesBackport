package com.exiledradio.trailsandtalesbackport.hangingsign;

import com.exiledradio.backportcore.waterlog.Waterloggable;

import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.BlockSign;
import net.minecraft.block.SoundType;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Hanging signs: {@link Ceiling} hangs under a block, {@link Wall} hangs from a bracket on a wall.
 *
 * <p>Both extend {@link BlockSign}, so editing, commands in sign text and every {@code instanceof} check
 * work as for any sign; they are not {@code BlockStandingSign}/{@code BlockWallSign}, whose support rules
 * (a block below, a block behind) are not these.
 *
 * <p>Both are {@link Waterloggable}, as 1.20's {@code CeilingHangingSignBlock} and {@code WallHangingSignBlock}
 * are (with Fluidlogged API; dry without it). One that loses its support drops and leaves its water.
 */
public abstract class BlockHangingSign extends BlockSign implements Waterloggable {

    final HangingWood wood;
    Item item;

    BlockHangingSign(HangingWood wood, SoundType sound) {
        this.wood = wood;
        setHardness(1.0F);
        setSoundType(sound);
        disableStats();
    }

    public HangingWood wood() {
        return this.wood;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileHangingSign();
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return this.item;
    }

    @Override
    public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
        return new ItemStack(this.item);
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    /** @return true if this state may stay where it is */
    public abstract boolean canStay(World world, BlockPos pos, IBlockState state);

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(IBlockState state, World world, BlockPos pos, net.minecraft.block.Block block, BlockPos from) {
        if (!canStay(world, pos, state)) {
            dropBlockAsItem(world, pos, state, 0);
            world.setBlockToAir(pos);
        }
        super.neighborChanged(state, world, pos, block, from);
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return world.getBlockState(pos).getBlock().isReplaceable(world, pos);
    }

    private static AxisAlignedBB box(double x1, double y1, double z1, double x2, double y2, double z2) {
        return new AxisAlignedBB(x1 / 16, y1 / 16, z1 / 16, x2 / 16, y2 / 16, z2 / 16);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Under a block. {@code rotation} 0-15 is saved; {@code attached} (hung by a V of chains, used when the
     * block above has no full bottom or the player sneaks) is kept by the tile, since metadata is full.
     * 1.20's rule for support: the block above holds up the middle of its bottom face (a full block, a fence,
     * a wall, iron bars, a chain).
     */
    public static class Ceiling extends BlockHangingSign {
        public static final PropertyInteger ROTATION = PropertyInteger.create("rotation", 0, 15);
        public static final PropertyBool ATTACHED = PropertyBool.create("attached");

        private static final AxisAlignedBB ANY = box(3, 0, 3, 13, 16, 13);
        private static final AxisAlignedBB ALONG_X = box(1, 0, 7, 15, 10, 9);
        private static final AxisAlignedBB ALONG_Z = box(7, 0, 1, 9, 10, 15);

        public Ceiling(HangingWood wood, SoundType sound) {
            super(wood, sound);
            setDefaultState(this.blockState.getBaseState().withProperty(ROTATION, 0).withProperty(ATTACHED, false));
        }

        @Override
        protected BlockStateContainer createBlockState() {
            return new BlockStateContainer(this, ROTATION, ATTACHED);
        }

        @Override
        @SuppressWarnings("deprecation")
        public IBlockState getStateFromMeta(int meta) {
            return getDefaultState().withProperty(ROTATION, meta & 15);
        }

        @Override
        public int getMetaFromState(IBlockState state) {
            return state.getValue(ROTATION);
        }

        @Override
        @SuppressWarnings("deprecation")
        public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
            TileEntity tile = world.getTileEntity(pos);
            return state.withProperty(ATTACHED, tile instanceof TileHangingSign && ((TileHangingSign) tile).attached());
        }

        @Override
        @SuppressWarnings("deprecation")
        public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
            int r = state.getValue(ROTATION);
            if (r == 0 || r == 8) {
                return ALONG_X;
            }
            if (r == 4 || r == 12) {
                return ALONG_Z;
            }
            return ANY;
        }

        @Override
        @SuppressWarnings("deprecation")
        public IBlockState withRotation(IBlockState state, Rotation rotation) {
            return state.withProperty(ROTATION, rotation.rotate(state.getValue(ROTATION), 16));
        }

        @Override
        @SuppressWarnings("deprecation")
        public IBlockState withMirror(IBlockState state, Mirror mirror) {
            return state.withProperty(ROTATION, mirror.mirrorRotation(state.getValue(ROTATION), 16));
        }

        @Override
        public boolean canStay(World world, BlockPos pos, IBlockState state) {
            return holdsCenter(world, pos.up());
        }

        /** @return true if the block at {@code above} holds up the middle of its bottom face */
        @SuppressWarnings("deprecation")
        public static boolean holdsCenter(World world, BlockPos above) {
            IBlockState s = world.getBlockState(above);
            if (s.isSideSolid(world, above, EnumFacing.DOWN)) {
                return true;
            }
            if (s.getBlock() instanceof BlockHangingSign) {
                return false;
            }
            AxisAlignedBB box = s.getCollisionBoundingBox(world, above);
            if (box == null || box == net.minecraft.block.Block.NULL_AABB) {
                return false;
            }
            return box.minY <= 0.0D && box.minX <= 7 / 16.0 && box.maxX >= 9 / 16.0
                    && box.minZ <= 7 / 16.0 && box.maxZ >= 9 / 16.0;
        }

        /** @return true if the block above has a full bottom face, so the sign hangs by straight chains */
        public static boolean fullBelow(World world, BlockPos above) {
            return world.getBlockState(above).getBlockFaceShape(world, above, EnumFacing.DOWN) == BlockFaceShape.SOLID;
        }
    }

    /**
     * On a wall: the bracket goes into the block on the sign's left or right (its board faces along
     * {@code facing}, so the wall is beside it), or into another wall hanging sign along the same line.
     */
    public static class Wall extends BlockHangingSign {
        public static final PropertyDirection FACING = BlockHorizontal.FACING;

        private static final AxisAlignedBB NORTH_SOUTH = box(0, 0, 6, 16, 16, 10);
        private static final AxisAlignedBB EAST_WEST = box(6, 0, 0, 10, 16, 16);

        public Wall(HangingWood wood, SoundType sound) {
            super(wood, sound);
            setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
        }

        @Override
        protected BlockStateContainer createBlockState() {
            return new BlockStateContainer(this, FACING);
        }

        @Override
        @SuppressWarnings("deprecation")
        public IBlockState getStateFromMeta(int meta) {
            EnumFacing f = EnumFacing.byIndex(meta);
            return getDefaultState().withProperty(FACING, f.getAxis() == EnumFacing.Axis.Y ? EnumFacing.NORTH : f);
        }

        @Override
        public int getMetaFromState(IBlockState state) {
            return state.getValue(FACING).getIndex();
        }

        @Override
        @SuppressWarnings("deprecation")
        public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
            return state.getValue(FACING).getAxis() == EnumFacing.Axis.Z ? NORTH_SOUTH : EAST_WEST;
        }

        @Override
        @SuppressWarnings("deprecation")
        public IBlockState withRotation(IBlockState state, Rotation rotation) {
            return state.withProperty(FACING, rotation.rotate(state.getValue(FACING)));
        }

        @Override
        @SuppressWarnings("deprecation")
        public IBlockState withMirror(IBlockState state, Mirror mirror) {
            return state.withRotation(mirror.toRotation(state.getValue(FACING)));
        }

        @Override
        public boolean canStay(World world, BlockPos pos, IBlockState state) {
            EnumFacing f = state.getValue(FACING);
            return attachesTo(world, state, pos.offset(f.rotateY()), f.rotateYCCW())
                    || attachesTo(world, state, pos.offset(f.rotateYCCW()), f.rotateY());
        }

        /** 1.20's {@code canAttachTo}: another wall hanging sign on the same line, or a full face. */
        private static boolean attachesTo(World world, IBlockState sign, BlockPos at, EnumFacing face) {
            IBlockState s = world.getBlockState(at);
            if (s.getBlock() instanceof Wall) {
                return s.getValue(FACING).getAxis() == sign.getValue(FACING).getAxis();
            }
            return s.getBlockFaceShape(world, at, face) == BlockFaceShape.SOLID;
        }
    }
}
