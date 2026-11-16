package com.exiledradio.trailsandtalesbackport.presentation1205;

import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import net.minecraft.util.ResourceLocation;

/**
 * Stream presentation1205: 1.20.5's world warnings and its new language, each on its own config option and only while
 * Backport Core's newest installed version is 1.20.5 or later.
 *
 * <ul>
 *   <li>Chunk toasts (24w03a): in singleplayer a red "Failed to load chunk at [x, z]" or "Failed to save chunk at
 *       [x, z]" toast, "See log for more details" ({@link ChunkFailures}, which watches the log lines 1.12.2 writes when
 *       it catches those failures, and patch P4 for the chunk the server thread is handling);</li>
 *   <li>low disk space (24w03a): opening a singleplayer world with under 64 MB free on its drive shows "Warning! Low
 *       disk space!" (Continue / Back), and the game's saves and chunk failures show "Low disk space!" while that
 *       lasts ({@link DiskSpace}, {@link LowDiskSpaceTicker});</li>
 *   <li>the Failed to Load World screen (24w05a, {@code RecoverWorldDataScreen}): a world whose {@code level.dat}
 *       cannot be read is listed as 1.20.5 lists it and opens that screen, with Attempt to Restore from
 *       {@code level.dat_old} (client side);</li>
 *   <li>Viossa (24w06a), {@code vp_vl}, in the language list, with 1.20.6's text (client side).</li>
 * </ul>
 * The new keys' text, in the family's languages and Viossa, is in {@code vanilla_text_1205/}, which Core composes into
 * vanilla's language files ({@code tools/presentation1205/generate.py}).
 */
public final class Presentation1205 {

    public static final String VERSION = "1.20.5";
    /** The vanilla text folder of this stream's keys, and of 1.12.2's keys in Viossa. */
    public static final ResourceLocation VANILLA_TEXT = new ResourceLocation(TrailsAndTalesBackport.MOD_ID, "vanilla_text_1205");

    private static boolean registered;

    private Presentation1205() {
    }

    private static ModConfig.Presentation1205 config() {
        return ModConfig.presentation1205;
    }

    private static boolean on(boolean option) {
        return option && Presentation.isAtLeast(VERSION);
    }

    public static boolean chunkToasts() {
        return on(config().chunkToasts);
    }

    public static boolean lowDiskSpace() {
        return on(config().lowDiskSpace);
    }

    public static boolean recoverWorld() {
        return on(config().recoverWorld);
    }

    public static boolean viossa() {
        return on(config().viossa);
    }

    /** Pre-init, both sides: 1.20.5 joins Core's versions, with this stream's text. */
    public static void preInit() {
        ModConfig.Presentation1205 c = config();
        if (registered || !(c.chunkToasts || c.lowDiskSpace || c.recoverWorld || c.viossa)) {
            return;
        }
        registered = true;
        Presentation.version(VERSION).vanillaText(VANILLA_TEXT);
    }
}
