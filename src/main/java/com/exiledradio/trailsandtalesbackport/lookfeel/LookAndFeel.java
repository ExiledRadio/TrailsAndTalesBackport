package com.exiledradio.trailsandtalesbackport.lookfeel;

import com.exiledradio.backportcore.presentation.Contribution;
import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import net.minecraft.util.ResourceLocation;

/**
 * Stream Lf: 1.20's look and feel, the changes 1.20 to 1.20.6 made beyond their content. Backport Core owns every
 * piece only one version can have at a time (the panorama, the splash list, the title logo, the loading screen, the
 * options screens, the F3 keys) and shows the newest installed version's; this class declares 1.20 to 1.20.5 installed
 * and hands Core what is this mod's own.
 *
 * <ul>
 *   <li>1.20 (23w14a): the cherry grove panorama and 1.20.6's {@code panorama_overlay.png};</li>
 *   <li>1.20's menu music (23w17a): A Familiar Room, Bromeliad, Crescent Dunes and Echo in the Wind join the title
 *       screen's pool, and 1.19's Aerie, Firebugs and Labyrinthine leave it. A Familiar Room also joins the game
 *       music (client side, {@code ModConfig.lookAndFeel.gameMusic});</li>
 *   <li>the brushing arm pose (client side, {@code lookfeel.client.BrushPose}).</li>
 * </ul>
 * The rest of 1.20.x's look and feel is Backport Core's own since Core 1.4.7 ({@code presentation.versions.Look120}:
 * the title logo, Minceraft and "Java Edition", the splashes, vanilla's text, the window icon, Ctrl + scroll GUI Scale,
 * F3 + 1 and F3 + 2, Menu Background Blur and {@code menu_background.png}, the 1.20.5 loading and dimension-change
 * screens, the shield arm and Ctrl + pick block), so that any 1.20 or later backport has it. With
 * {@code lookAndFeel.enabled} off this mod declares no 1.20 version, so those stay off too unless a newer backport
 * is installed.
 */
public final class LookAndFeel {

    private static final String ID = TrailsAndTalesBackport.MOD_ID;

    /** 1.19.4's menu tracks that 1.20 took out of the title screen's pool, by their file under {@code sounds/}. */
    static final String[] REMOVED_MENU_TRACKS = {"music/game/swamp/aerie", "music/game/swamp/firebugs", "music/game/swamp/labyrinthine"};
    /** 1.20's four menu tracks, in this mod's sounds. */
    static final String[] MENU_TRACKS = {"a_familiar_room", "bromeliad", "crescent_dunes", "echo_in_the_wind"};
    /** The mods whose sounds may hold 1.19's tracks: vanilla's own name, and Wild Update's. */
    static final String[] TRACK_OWNERS = {"minecraft", "wildupdatebackport"};

    private static boolean registered;

    private LookAndFeel() {
    }

    /** @return true when the stream is on */
    public static boolean enabled() {
        return ModConfig.lookAndFeel.enabled;
    }

    /** Pre-init, both sides: hands Core 1.20's pieces. */
    public static void preInit() {
        if (registered || !enabled()) {
            return;
        }
        registered = true;
        Contribution v120 = Presentation.version("1.20");
        v120.panorama(ID + ":textures/gui/title/background/panorama_")
                .panoramaOverlay(new ResourceLocation(ID, "textures/gui/title/background/panorama_overlay.png"));
        for (String track : MENU_TRACKS) {
            v120.addMenuMusic(ID + ":music/game/" + track, 1);
        }
        for (String track : REMOVED_MENU_TRACKS) {
            for (String owner : TRACK_OWNERS) {
                v120.removeMenuMusic(owner + ":" + track);
            }
        }

        // 1.20.2 (the F3 chart keys, Back on the world and server lists), 1.20.3 (Hide Splash Texts) and 1.20.5 (Menu
        // Background Blur, the loading and dimension screens, the shield arm, Ctrl + pick block): their pieces are
        // Backport Core's.
        Presentation.version("1.20.2");
        Presentation.version("1.20.3");
        Presentation.version("1.20.5");
    }
}
