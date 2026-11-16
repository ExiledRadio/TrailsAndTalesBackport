package com.exiledradio.trailsandtalesbackport.sculk;

import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The calibrated sculk sensor: 1.20.6's {@code CalibratedSculkSensorBlock}.
 *
 * <ul>
 *   <li>{@code facing}: the way the placing player looked. The side toward the player (the back,
 *       {@code facing.getOpposite()}) is the input, with its own texture: a redstone signal there of strength
 *       1-15 makes the sensor hear only vibrations of that frequency (none: everything).</li>
 *   <li>It gives no power out of its back; to every other side, and strongly into the block below, as the
 *       sculk sensor.</li>
 *   <li>Listens 16 blocks; active 10 ticks; 10 ticks of cooldown.</li>
 * </ul>
 * Metadata: the phase in bits 0-1, the facing's horizontal index in bits 2-3.
 */
public class BlockCalibratedSculkSensor extends BlockSculkSensor {

    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    public static final int ACTIVE_TICKS = 10;

    @Override
    protected IBlockState defaultState() {
        return super.defaultState().withProperty(FACING, EnumFacing.NORTH);
    }

    @Override
    public int activeTicks() {
        return ACTIVE_TICKS;
    }

    /** 1.20's {@code getStateForPlacement}: {@code context.getHorizontalDirection()}, where the player looks. */
    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, int meta,
                                            EntityLivingBase placer) {
        return this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing());
    }

    /** No power out of the back: 1.20's {@code getSignal}, {@code direction != FACING}. */
    @Override
    @SuppressWarnings("deprecation")
    public int getWeakPower(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        return side != state.getValue(FACING) ? super.getWeakPower(state, world, pos, side) : 0;
    }

    /** The frequency the back's signal asks for, 0 for none: 1.20's {@code getBackSignal}. */
    public static int backSignal(World world, BlockPos pos, IBlockState state) {
        EnumFacing back = state.getValue(FACING).getOpposite();
        return world.getRedstonePower(pos.offset(back), back);
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
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, PHASE, FACING);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return phase(state).ordinal() | state.getValue(FACING).getHorizontalIndex() << 2;
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        return super.getStateFromMeta(meta).withProperty(FACING, EnumFacing.byHorizontalIndex(meta >> 2));
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileCalibratedSculkSensor();
    }
}
