package com.exiledradio.trailsandtalesbackport.lookfeel.client;

import com.exiledradio.backportcore.presentation.Contribution;
import com.exiledradio.backportcore.presentation.LoadingRevision;
import com.exiledradio.backportcore.presentation.LookArea;
import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.backportcore.presentation.client.LoadingScreens;
import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.lookfeel.LookAndFeel;
import com.exiledradio.trailsandtalesbackport.lookfeel.LookRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiVideoSettings;
import net.minecraft.client.gui.GuiWorldSelection;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;

/**
 * Stream Lf's client side: what 1.20 changed that is drawn or pressed rather than registered with Core.
 * <ul>
 *   <li>1.20.2's F3 + 1 (profiler chart) and F3 + 2 (FPS and TPS charts), which also keep the charts when F3 is
 *       pressed with Shift or Alt (1.20.2 took those away);</li>
 *   <li>1.20.2's "Back" on the Multiplayer and World Selection screens (23w31a; was "Cancel");</li>
 *   <li>1.20 (23w13a): Ctrl + scroll on Video Settings changes GUI Scale;</li>
 *   <li>1.20's application icon (a grass block, was a crafting table);</li>
 *   <li>A Familiar Room in the game music, and the 1.20.5 loading screen ({@link LoadingScreen1205}).</li>
 * </ul>
 */
@SideOnly(Side.CLIENT)
public final class LookAndFeelClient {

    /** Counters for the client self-test. */
    public static volatile int backButtons;
    public static volatile int guiScaleScrolls;
    public static volatile boolean iconSet;

    /** 1.20.2's chart switches, which F3 alone no longer changes. */
    static boolean profilerChart;
    static boolean fpsCharts;

    private LookAndFeelClient() {
    }

    /** From {@code ClientSetup}, in pre-init: before the sound handler first reads the sound lists. */
    public static void init() {
        if (!LookAndFeel.enabled()) {
            return;
        }
        if (ModConfig.lookAndFeel.gameMusic) {
            GameMusicPack.install();
        }
        Contribution v1202 = Presentation.contribution("1.20.2");
        if (v1202 != null) {
            v1202.debugKey(Keyboard.KEY_1, "debug.profiler_chart.help", LookAndFeelClient::toggleProfilerChart);
            v1202.debugKey(Keyboard.KEY_2, "debug.fps_charts.help", LookAndFeelClient::toggleFpsCharts);
        }
        LoadingScreens.register(LoadingRevision.PANORAMA_1_20_5, new LoadingScreen1205());
        MinecraftForge.EVENT_BUS.register(new LookAndFeelClient());
        MinecraftForge.EVENT_BUS.register(new DimensionScreen());
        setIcon();
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

    // ---- Title logo ----

    /**
     * @return true while Core serves this mod's title logo (no newer installed version replaced it). Core's patch
     *         P29 draws it in 1.20's layout from 1.20.6's own files: the logo, Minceraft, "Java Edition" and the splash.
     */
    public static boolean ourLogo() {
        return LookAndFeel.LOGO.equals(Presentation.asset("textures/gui/title/minecraft.png"));
    }

    // ---- F3 charts (1.20.2) ----

    /** F3 + 1: the profiler chart. */
    static boolean toggleProfilerChart() {
        profilerChart = !profilerChart;
        applyCharts(Minecraft.getMinecraft());
        return true;
    }

    /** F3 + 2: the FPS chart (and, in singleplayer, the server's tick chart: 1.12.2's lagometer). */
    static boolean toggleFpsCharts() {
        fpsCharts = !fpsCharts;
        applyCharts(Minecraft.getMinecraft());
        return true;
    }

    static void applyCharts(Minecraft mc) {
        mc.gameSettings.showDebugProfilerChart = profilerChart;
        mc.gameSettings.showLagometer = fpsCharts;
    }

    /**
     * F3 released on its own: 1.12.2 sets the profiler chart to Shift and the lagometer to Alt. 1.20.2 shows only the
     * charts F3 + 1 and F3 + 2 chose, so they are put back.
     */
    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        if (Keyboard.getEventKey() == Keyboard.KEY_F3 && !Keyboard.getEventKeyState() && Presentation.isAtLeast(LookArea.DEBUG, "1.20.2")) {
            applyCharts(Minecraft.getMinecraft());
        }
    }

    // ---- Menus ----

    /** 1.20.2: Multiplayer's and World Selection's Cancel reads Back. */
    @SubscribeEvent
    public void onInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.getGui() instanceof GuiMultiplayer) && !(event.getGui() instanceof GuiWorldSelection)
                || !Presentation.isAtLeast(LookArea.MENUS, "1.20.2")) {
            return;
        }
        String cancel = I18n.format("gui.cancel");
        for (GuiButton button : event.getButtonList()) {
            if (button.id == 0 && cancel.equals(button.displayString)) {
                button.displayString = I18n.format("gui.back");
                backButtons++;
            }
        }
    }

    /** 1.20.6 {@code VideoSettingsScreen.mouseScrolled}: Ctrl + scroll steps GUI Scale (Auto counts as one above the most). */
    @SubscribeEvent
    public void onMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!(event.getGui() instanceof GuiVideoSettings) || !GuiScreen.isCtrlKeyDown() || !Presentation.isAtLeast(LookArea.VIDEO, "1.20")) {
            return;
        }
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) {
            return;
        }
        event.setCanceled(true);
        Minecraft mc = Minecraft.getMinecraft();
        int next = LookRules.nextGuiScale(mc.gameSettings.guiScale, Integer.signum(wheel), maxGuiScale(mc));
        if (next != mc.gameSettings.guiScale) {
            mc.gameSettings.guiScale = next;
            mc.gameSettings.saveOptions();
            ScaledResolution resolution = new ScaledResolution(mc);
            event.getGui().setWorldAndResolution(mc, resolution.getScaledWidth(), resolution.getScaledHeight());
            guiScaleScrolls++;
        }
    }

    /** @return the GUI scale Auto picks for this window (1.12.2's {@code ScaledResolution} with no limit) */
    static int maxGuiScale(Minecraft mc) {
        int scale = 1;
        while (scale < 1000 && mc.displayWidth / (scale + 1) >= 320 && mc.displayHeight / (scale + 1) >= 240) {
            scale++;
        }
        if (mc.isUnicode() && scale % 2 != 0 && scale != 1) {
            scale--;
        }
        return scale;
    }

    // ---- Application icon ----

    /**
     * 1.20's window icon, 16 and 32 pixels, as 1.12.2's {@code Minecraft.setWindowIcon} sets it (not on macOS), while
     * the menus look like 1.20 or later (Core's Look &amp; Feel page); otherwise 1.12.2's own, from vanilla's jar.
     */
    static void setIcon() {
        if (Util.getOSType() == Util.EnumOS.OSX) {
            return;
        }
        boolean ours = Presentation.isAtLeast(LookArea.MENUS, "1.20");
        if (ours == iconSet && iconChecked) {
            return;
        }
        iconChecked = true;
        try {
            ByteBuffer small = icon("icon_16x16.png", ours);
            ByteBuffer large = icon("icon_32x32.png", ours);
            if (small != null && large != null) {
                Display.setIcon(new ByteBuffer[] {small, large});
                iconSet = ours;
            }
        } catch (Exception e) {
            TrailsAndTalesBackport.LOGGER.warn("Look and feel: the window icon was not set ({})", e.toString());
        }
    }

    private static boolean iconChecked;

    /** Core's Look &amp; Feel page changed: the icon follows it. */
    @SubscribeEvent
    public void onConfigChanged(net.minecraftforge.fml.client.event.ConfigChangedEvent.PostConfigChangedEvent event) {
        if ("backportcore".equals(event.getModID())) {
            setIcon();
        }
    }

    private static ByteBuffer icon(String name, boolean ours) throws Exception {
        InputStream in = ours ? LookAndFeelClient.class.getResourceAsStream("/assets/" + TrailsAndTalesBackport.MOD_ID + "/textures/gui/icon/" + name)
                : Minecraft.getMinecraft().getResourcePackRepository().rprDefaultResourcePack.getInputStream(new ResourceLocation("icons/" + name));
        if (in == null) {
            return null;
        }
        try {
            BufferedImage image = ImageIO.read(in);
            int[] argb = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
            ByteBuffer buffer = ByteBuffer.allocate(4 * argb.length);
            for (int p : argb) {
                buffer.putInt(p << 8 | p >> 24 & 255);
            }
            buffer.flip();
            return buffer;
        } finally {
            in.close();
        }
    }
}
