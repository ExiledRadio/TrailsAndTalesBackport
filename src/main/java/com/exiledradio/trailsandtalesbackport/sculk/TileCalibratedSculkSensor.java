package com.exiledradio.trailsandtalesbackport.sculk;

import com.exiledradio.backportcore.gameevent.GameEvent;
import com.exiledradio.backportcore.gameevent.GameEventContext;
import com.exiledradio.backportcore.gameevent.VibrationSystem;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

/**
 * The calibrated sculk sensor's tile entity: 1.20.6's {@code CalibratedSculkSensorBlockEntity}. Listens 16
 * blocks; with a signal of 1-15 into its back it hears only vibrations of that frequency.
 */
public class TileCalibratedSculkSensor extends TileSculkSensor {

    public static final int LISTENER_RANGE = 16;

    protected class CalibratedUser extends User {
        protected CalibratedUser(BlockPos pos) {
            super(pos);
        }

        @Override
        public int listenerRadius() {
            return LISTENER_RANGE;
        }

        @Override
        public boolean canReceiveVibration(WorldServer world, BlockPos origin, GameEvent event, GameEventContext context) {
            IBlockState state = world.getBlockState(this.blockPos);
            if (state.getBlock() instanceof BlockCalibratedSculkSensor) {
                int back = BlockCalibratedSculkSensor.backSignal(world, this.blockPos, state);
                if (back != 0 && VibrationSystem.frequency(event) != back) {
                    return false;
                }
            }
            return super.canReceiveVibration(world, origin, event, context);
        }
    }

    @Override
    protected User createUser() {
        return new CalibratedUser(this.pos);
    }
}
