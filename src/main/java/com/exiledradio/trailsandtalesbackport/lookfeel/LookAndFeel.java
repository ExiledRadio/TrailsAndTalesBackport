package com.exiledradio.trailsandtalesbackport.lookfeel;

import com.exiledradio.backportcore.presentation.Contribution;
import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.backportcore.presentation.option.OptionScreen;
import com.exiledradio.backportcore.presentation.option.SliderOption;
import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import net.minecraft.util.ResourceLocation;

/**
 * Stream Lf: 1.20's look and feel, the changes 1.20 to 1.20.6 made beyond their content. Backport Core owns every
 * piece only one version can have at a time (the panorama, the splash list, the title logo, the loading screen, the
 * options screens, the F3 keys) and shows the newest installed version's; this class hands Core 1.20's pieces.
 *
 * <ul>
 *   <li>1.20 (23w14a): the cherry grove panorama and 1.20.6's {@code panorama_overlay.png}; the redrawn title logo
 *       and its own Minceraft logo, laid out the way 1.12.2's title screen draws {@code minecraft.png}
 *       ({@code tools/lookfeel/generate.py}), and the larger "Java Edition" under it (client side);</li>
 *   <li>the splash changes between 1.19.4's and 1.20.6's {@code splashes.txt} (8 added, 2 removed; Core drops the
 *       Java version one);</li>
 *   <li>1.20's menu music (23w17a): A Familiar Room, Bromeliad, Crescent Dunes and Echo in the Wind join the title
 *       screen's pool, and 1.19's Aerie, Firebugs and Labyrinthine leave it. A Familiar Room also joins the game
 *       music (client side, {@code ModConfig.lookAndFeel.gameMusic});</li>
 *   <li>vanilla's own text as 1.20.6 words it ({@code vanilla_text/}: "while" for "whilst" in death messages, "Game
 *       Over!", "Left Button" ...);</li>
 *   <li>1.20.2: F3 + 1 and F3 + 2 show the profiler and the FPS charts (Shift + F3 and Alt + F3 no longer do), and
 *       the Multiplayer and World Selection screens' Cancel reads Back (client side);</li>
 *   <li>1.20.3: Core's Hide Splash Texts option shows once 1.20.3 is installed;</li>
 *   <li>1.20.5: Menu Background Blur (Accessibility and Video Settings) and the world-loading screen over the
 *       blurred, darkened panorama (Core's {@code PANORAMA_1_20_5} revision; this mod draws it).</li>
 * </ul>
 * Also 1.20's application icon (a grass block) and Ctrl + scroll changing GUI Scale on Video Settings, client side.
 */
public final class LookAndFeel {

    private static final String ID = TrailsAndTalesBackport.MOD_ID;

    /** The title logo Core serves for {@code minecraft:textures/gui/title/minecraft.png}. */
    public static final ResourceLocation LOGO = new ResourceLocation(ID, "textures/gui/title/minecraft.png");
    /** 1.20's Minceraft logo, in the same layout. */
    public static final ResourceLocation MINCERAFT = new ResourceLocation(ID, "textures/gui/title/minceraft.png");

    /** 1.19.4's menu tracks that 1.20 took out of the title screen's pool, by their file under {@code sounds/}. */
    static final String[] REMOVED_MENU_TRACKS = {"music/game/swamp/aerie", "music/game/swamp/firebugs", "music/game/swamp/labyrinthine"};
    /** 1.20's four menu tracks, in this mod's sounds. */
    static final String[] MENU_TRACKS = {"a_familiar_room", "bromeliad", "crescent_dunes", "echo_in_the_wind"};
    /** The mods whose sounds may hold 1.19's tracks: vanilla's own name, and Wild Update's. */
    static final String[] TRACK_OWNERS = {"minecraft", "wildupdatebackport"};

    /**
     * 1.20.5's Menu Background Blur (Accessibility Settings and Video Settings), 0 to 100%, 50% by default: the box
     * blur radius of the panorama behind menus is ten times the value. OFF at zero.
     */
    public static final SliderOption MENU_BLUR = new SliderOption(ID + ":menu_background_blurriness",
            "options.accessibility.menu_background_blurriness", OptionScreen.ACCESSIBILITY, 0.0, 1.0, 0.0, 0.5);

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
                .panoramaOverlay(new ResourceLocation(ID, "textures/gui/title/background/panorama_overlay.png"))
                .addSplashes(new ResourceLocation(ID, "texts/splashes_added.txt"))
                .removeSplashes(new ResourceLocation(ID, "texts/splashes_removed.txt"))
                .titleLogo(LOGO)
                // 1.20.6's logo is drawn smoothed (its .mcmeta), as vanilla's at four times the size.
                .replaceAsset("textures/gui/title/minecraft.png.mcmeta", new ResourceLocation(ID, "textures/gui/title/minecraft.png.mcmeta"))
                .replaceAsset("textures/gui/title/edition.png", new ResourceLocation(ID, "textures/gui/title/edition.png"))
                .replaceAsset("textures/gui/title/edition.png.mcmeta", new ResourceLocation(ID, "textures/gui/title/edition.png.mcmeta"))
                .vanillaText(new ResourceLocation(ID, "vanilla_text"));
        for (String track : MENU_TRACKS) {
            v120.addMenuMusic(ID + ":music/game/" + track, 1);
        }
        for (String track : REMOVED_MENU_TRACKS) {
            for (String owner : TRACK_OWNERS) {
                v120.removeMenuMusic(owner + ":" + track);
            }
        }

        // 1.20.2: the F3 chart keys (client side) and Back on the world and server lists.
        Presentation.version("1.20.2");
        // 1.20.3: Hide Splash Texts, which is Core's (it owns the splash).
        Presentation.version("1.20.3");

        // 1.20.5 (24w09a): on Accessibility Settings after Auto-Jump, and last on Video Settings.
        MENU_BLUR.zeroKey("options.off");
        MENU_BLUR.tooltip("options.accessibility.menu_background_blurriness.tooltip");
        MENU_BLUR.alsoOn(OptionScreen.VIDEO);
        Presentation.version("1.20.5").option(MENU_BLUR.after("AUTO_JUMP"))
                // The tint over the loading screen's blurred panorama (renderMenuBackground).
                .replaceAsset("textures/gui/menu_background.png", new ResourceLocation(ID, "textures/gui/menu_background.png"));
    }

    /** @return the blur radius 1.20.5 uses behind menus: Menu Background Blur times ten */
    public static float blurRadius() {
        return (float) (MENU_BLUR.get() * 10.0);
    }
}
