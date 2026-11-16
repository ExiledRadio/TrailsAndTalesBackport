package com.exiledradio.trailsandtalesbackport.sculk;

import com.exiledradio.backportcore.gameevent.GameEvent;
import com.exiledradio.backportcore.gameevent.GameEventContext;
import com.exiledradio.backportcore.gameevent.GameEventTags;
import com.exiledradio.backportcore.gameevent.GameEvents;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * 1.20's amethyst resonance ({@code SculkSensorBlock.tryResonateVibration}), for every backport's sculk sensors
 * through Backport Core's {@code SensorActivation}: each amethyst block ({@code #vibration_resonators}) beside an
 * activating sensor posts {@code resonate_<frequency>} from itself, carrying the vibration's frequency on to
 * sensors further off, and plays {@code block.amethyst_block.resonate} at the note for that frequency.
 */
public final class Resonance {

    /** 1.20's {@code RESONANCE_PITCH_BEND}: frequency 0-15 to a note block note, as a pitch. */
    static final float[] PITCH = new float[16];

    static {
        int[] notes = {0, 0, 2, 4, 6, 7, 9, 10, 12, 14, 15, 18, 19, 21, 22, 24};
        for (int i = 0; i < 16; i++) {
            // NoteBlock.getPitchFromNote
            PITCH[i] = (float) Math.pow(2.0D, (notes[i] - 12) / 12.0D);
        }
    }

    private Resonance() {
    }

    static void resonate(@Nullable Entity source, World world, BlockPos pos, int frequency) {
        for (EnumFacing dir : EnumFacing.values()) {
            BlockPos at = pos.offset(dir);
            IBlockState state = world.getBlockState(at);
            if (GameEventTags.resonates(state)) {
                GameEvents.emit(world, GameEvent.resonance(frequency), at, GameEventContext.of(source, state));
                world.playSound(null, at, SensorContent.RESONATE, SoundCategory.BLOCKS, 1.0F, PITCH[frequency]);
            }
        }
    }
}
