package com.exiledradio.trailsandtalesbackport.network;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;

/**
 * The mod's one network channel.
 *
 * <h2>Packet ids</h2>
 * Each stream owns a block of five ids (see {@code docs/PLAN.md}) and registers its packets here, inside
 * its own block, so two streams can never pick the same number and merges stay mechanical:
 *
 * <pre>
 *   0-4   common          30-34 signs              60-64 smithing
 *   5-9   cherry          35-39 chiseledBookshelf  65-69 armadillos
 *   10-14 bamboo          40-44 archaeology        70-74 wolves
 *   15-19 boats           45-49 structures         75-79 calibratedSculkSensor
 *   20-24 cherryGrove     50-54 sniffer            80-84 heads
 *   25-29 hangingSigns    55-59 camels             85-89 misc
 * </pre>
 *
 * <p>The channel name is short because Forge caps channel names at 20 characters, one fewer than the
 * mod id.
 */
public final class NetworkHandler {

    public static final String CHANNEL = "ttbackport";

    public static SimpleNetworkWrapper channel;

    private NetworkHandler() {
    }

    public static void init() {
        channel = NetworkRegistry.INSTANCE.newSimpleChannel(CHANNEL);
        // Streams register their packets below, in id order.
    }
}
