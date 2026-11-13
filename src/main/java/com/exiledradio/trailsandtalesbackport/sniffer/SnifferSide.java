package com.exiledradio.trailsandtalesbackport.sniffer;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * What the sniffer stream does only on a client. The server keeps this no-op one; the client's
 * {@code sniffer.client.SnifferClient} replaces it, so common code never names a client class.
 */
public class SnifferSide {

    /** 1.20's {@code SnifferSoundInstance}: the digging sound, following the sniffer while it digs or searches. */
    public void diggingSound(EntitySniffer sniffer) {
    }

    /** 1.20's level event 3009: sparkles on every face of an egg on moss. */
    public void eggBoostParticles(World world, BlockPos pos) {
    }
}
