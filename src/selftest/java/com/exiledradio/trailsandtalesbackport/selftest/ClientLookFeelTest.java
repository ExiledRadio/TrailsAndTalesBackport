package com.exiledradio.trailsandtalesbackport.selftest;

import com.exiledradio.backportcore.presentation.DebugKeys;
import com.exiledradio.backportcore.presentation.LoadingRevision;
import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.backportcore.presentation.client.PanoramaRenderer;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.lookfeel.LookAndFeel;
import com.exiledradio.trailsandtalesbackport.lookfeel.client.GameMusicPack;
import com.exiledradio.trailsandtalesbackport.lookfeel.client.LoadingScreen1205;
import com.exiledradio.trailsandtalesbackport.lookfeel.client.LookAndFeelClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundEventAccessor;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiWorldSelection;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.apache.commons.io.FileUtils;
import org.lwjgl.input.Keyboard;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * {@code runClient -Pselftest=client}: stream Lf on a real client. On the title screen: the panorama, logo, "Java
 * Edition", icon, music, F3 keys, Back button and text; then it creates a world, which draws 1.20.5's loading screen,
 * saving one frame to {@code eclipse/lookfeel/loading.png}; then it quits. The result is logged as "CLIENT SELFTEST
 * DONE pass= fail=".
 */
public final class ClientLookFeelTest {

    private static final String WORLD = "lookfeel-client-test";

    private int ticks;
    private int pass;
    private int fail;
    private int phase;
    private File shots;
    private ClientLeftovers leftovers;
    private ClientReleaseChecks release;

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new ClientLookFeelTest());
        TrailsAndTalesBackport.LOGGER.info("CLIENT SELFTEST registered");
    }

    void ok(boolean condition, String what) {
        if (condition) {
            this.pass++;
            TrailsAndTalesBackport.LOGGER.info("CLIENT SELFTEST ok {}", what);
        } else {
            this.fail++;
            TrailsAndTalesBackport.LOGGER.error("CLIENT SELFTEST FAIL {}", what);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        try {
            if (this.phase == 0 && mc.currentScreen instanceof GuiMainMenu && ++this.ticks >= 300) {
                this.phase = 1;
                this.shots = new File(mc.gameDir, "lookfeel");
                this.shots.mkdirs();
                ScreenShotHelper.saveScreenshot(this.shots, "title.png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
                title(mc);
                screens(mc);
                File world = new File(new File(mc.gameDir, "saves"), WORLD);
                FileUtils.deleteQuietly(world);
                LoadingScreen1205.capture = new File(this.shots, "loading.png");
                // A window in the background must not pause the integrated server.
                mc.gameSettings.pauseOnLostFocus = false;
                this.ticks = 0;
                // The integrated server would run the server self-tests for "client" and stop itself.
                System.clearProperty(TrailsAndTalesBackport.MOD_ID + ".selftest");
                mc.launchIntegratedServer(WORLD, WORLD, new WorldSettings(1L, GameType.CREATIVE, true, false, WorldType.DEFAULT));
            } else if (this.phase == 1 && ++this.ticks > 2400) {
                ok(false, "the test world never opened");
                finish(mc);
            } else if (this.phase == 1 && mc.world != null && mc.player != null && this.ticks >= 40) {
                this.phase = 2;
                loading(mc);
                this.ticks = 0;
                this.leftovers = new ClientLeftovers(mc, this.shots, this::ok);
            } else if (this.phase == 2 && this.leftovers.tick(++this.ticks)) {
                this.phase = 4;
                this.release = new ClientReleaseChecks(mc, this.shots, this::ok);
            } else if (this.phase == 4 && this.release.tick()) {
                finish(mc);
            }
        } catch (Throwable t) {
            TrailsAndTalesBackport.LOGGER.error("CLIENT SELFTEST threw", t);
            this.fail++;
            finish(mc);
        }
    }

    private void finish(Minecraft mc) {
        this.phase = 3;
        TrailsAndTalesBackport.LOGGER.info("CLIENT SELFTEST DONE pass={} fail={}", this.pass, this.fail);
        mc.shutdown();
    }

    private void title(Minecraft mc) throws Exception {
        ok(Presentation.newest().isAtLeast("1.20.5"), "the newest version is 1.20.5: " + Presentation.newest());
        ResourceLocation[] faces = PanoramaRenderer.faces();
        ok(faces != null && faces[0].equals(new ResourceLocation(TrailsAndTalesBackport.MOD_ID, "textures/gui/title/background/panorama_0.png")),
                "the cherry grove panorama is shown: " + (faces == null ? null : faces[0]));
        ok(PanoramaRenderer.frames > 20, "the panorama was drawn: " + PanoramaRenderer.frames);
        ok(LookAndFeelClient.ourLogo(), "Core serves this mod's title logo");
        String pack = mc.getResourceManager().getResource(new ResourceLocation("textures/gui/title/minecraft.png")).getResourcePackName();
        ok("backportcore:vanilla_by_version".equals(pack), "the title logo comes from Core's vanilla-asset pack: " + pack);
        ok(mc.getResourceManager().getResource(new ResourceLocation("textures/gui/title/minecraft.png")).hasMetadata(),
                "the logo has its .mcmeta (smooth scaling)");
        ok(com.exiledradio.backportcore.presentation.client.TitleLogo.layout()
                == com.exiledradio.backportcore.presentation.VanillaRevisions.TitleLogo.LOGO_1_20,
                "Core lays the title out as 1.20 does: " + com.exiledradio.backportcore.presentation.client.TitleLogo.layout());
        ok(com.exiledradio.backportcore.presentation.client.TitleLogo.frames > 20,
                "Core drew the logo in 1.20's layout: " + com.exiledradio.backportcore.presentation.client.TitleLogo.frames);
        ok(LookAndFeelClient.iconSet, "1.20's window icon was set");
        ok(Presentation.loadingRevision() == LoadingRevision.PANORAMA_1_20_5, "the loading screen is 1.20.5's");

        // Minceraft: Core draws 1.20's own when 1.12.2 rolls it.
        ok(Presentation.asset("textures/gui/title/minceraft.png") != null, "Core has 1.20's Minceraft logo");

        // Music: A Familiar Room joined 1.12.2's game music.
        ok(GameMusicPack.installed, "the game music pack is installed");
        SoundEventAccessor game = mc.getSoundHandler().getAccessor(new ResourceLocation("music.game"));
        List<?> sounds = (List<?>) ReflectionHelper.findField(SoundEventAccessor.class, "accessorList", "field_188716_a").get(game);
        boolean familiar = false;
        for (Object s : sounds) {
            if (s instanceof net.minecraft.client.audio.Sound
                    && ((net.minecraft.client.audio.Sound) s).getSoundLocation().toString().contains("a_familiar_room")) {
                familiar = true;
            }
        }
        ok(familiar, "music.game has A Familiar Room");
        // FG2's dev asset cache often lacks vanilla's sounds.json (its downloader is refused): then vanilla's events
        // do not exist at all, and only the merge itself can be checked, not what it merged into.
        if (mc.getSoundHandler().getAccessor(new ResourceLocation("music.menu")) != null) {
            ok(sounds.size() > 12, "music.game keeps 1.12.2's tracks: " + sounds.size());
        } else {
            TrailsAndTalesBackport.LOGGER.warn("CLIENT SELFTEST note: no vanilla sounds.json in this dev environment; music.game has {} sound(s)", sounds.size());
        }

        // F3 + 1 and F3 + 2.
        Map<Integer, DebugKeys.Key> keys = DebugKeys.map(Presentation.installed());
        DebugKeys.Key one = keys.get(Keyboard.KEY_1);
        DebugKeys.Key two = keys.get(Keyboard.KEY_2);
        ok(one != null && one.action != null && two != null && two.action != null, "F3 + 1 and F3 + 2 are in the key map");
        if (one != null && two != null) {
            one.action.run();
            two.action.run();
            ok(mc.gameSettings.showDebugProfilerChart && mc.gameSettings.showLagometer, "F3 + 1 and F3 + 2 show the charts");
            one.action.run();
            two.action.run();
            ok(!mc.gameSettings.showDebugProfilerChart && !mc.gameSettings.showLagometer, "and hide them again");
        }

        // Text: 1.20's vanilla text and this mod's.
        String[][] lang = {
                {"deathScreen.title.hardcore", "Game Over!"},
                {"death.attack.thorns", "%1$s was killed while trying to hurt %2$s"},
                {"key.mouse.left", "Left Button"},
                {"options.accessibility.menu_background_blurriness", "Menu Background Blur"},
                {"selectWorld.data_read", "Reading world data..."},
        };
        for (String[] kv : lang) {
            String got = I18n.format(kv[0], "Alex", "Steve");
            String want = String.format(kv[1], "Alex", "Steve");
            ok(want.equals(got), "I18n " + kv[0] + " = \"" + got + "\"");
        }
        ok(Presentation.option(LookAndFeel.MENU_BLUR.id()) != null && LookAndFeel.MENU_BLUR.isShown(), "Menu Background Blur shows");
    }

    private void screens(Minecraft mc) throws Exception {
        GuiScreen menu = mc.currentScreen;
        GuiWorldSelection worlds = new GuiWorldSelection(menu);
        mc.displayGuiScreen(worlds);
        @SuppressWarnings("unchecked")
        List<GuiButton> buttons = (List<GuiButton>) ReflectionHelper.findField(GuiScreen.class, "buttonList", "field_146292_n").get(worlds);
        boolean back = false;
        for (GuiButton b : buttons) {
            back |= b.id == 0 && I18n.format("gui.back").equals(b.displayString);
        }
        ok(back, "World Selection's Cancel reads Back");
        mc.displayGuiScreen(menu);
    }

    private void loading(Minecraft mc) {
        ok(LoadingScreen1205.frames > 0, "1.20.5's loading screen was drawn: " + LoadingScreen1205.frames + " frames");
        ok(LoadingScreen1205.gridFrames > 0, "with the grid: " + LoadingScreen1205.gridFrames);
        ok(LoadingScreen1205.blurredFrames > 0, "over the blurred panorama: " + LoadingScreen1205.blurredFrames);
        File shot = new File(this.shots, "loading.png");
        ok(shot.isFile() && shot.length() > 1000, "a loading frame was saved: " + shot);
    }
}
