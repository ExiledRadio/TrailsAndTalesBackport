package com.exiledradio.trailsandtalesbackport.lookfeel.client;

import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.lookfeel.LookAndFeel;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Stream Lf's client side: A Familiar Room in the game music ({@link GameMusicPack}) and the brushing arm pose
 * ({@link BrushPose}). The rest of 1.20's look that is drawn or pressed (the F3 chart keys, Ctrl + scroll GUI Scale,
 * the window icon, 1.20.5's loading and dimension-change screens and shield arm) is Backport Core's since Core 1.4.7
 * ({@code presentation.versions.client.Look120Client}).
 */
@SideOnly(Side.CLIENT)
public final class LookClient {

    private LookClient() {
    }

    /** From {@code ClientSetup}, in pre-init: before the sound handler first reads the sound lists. */
    public static void init() {
        if (!LookAndFeel.enabled()) {
            return;
        }
        if (ModConfig.lookAndFeel.gameMusic) {
            GameMusicPack.install();
        }
        // `runClient -Pselftest=client`: the client check, reached by name (a release jar has no test classes).
        String selftest = System.getProperty(TrailsAndTalesBackport.MOD_ID + ".selftest");
        if (selftest != null && selftest.contains("client")) {
            try {
                Class.forName("com.exiledradio.trailsandtalesbackport.selftest.ClientLookFeelTest").getMethod("register").invoke(null);
            } catch (ReflectiveOperationException e) {
                TrailsAndTalesBackport.LOGGER.error("CLIENT SELFTEST DONE pass=0 fail=1: the test classes are not in this build", e);
            }
        }
    }
}
