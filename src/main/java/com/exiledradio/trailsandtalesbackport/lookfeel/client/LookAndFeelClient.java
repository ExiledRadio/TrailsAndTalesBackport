package com.exiledradio.trailsandtalesbackport.lookfeel.client;

import com.exiledradio.backportcore.presentation.Contribution;
import com.exiledradio.backportcore.presentation.LoadingRevision;
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
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.GuiOpenEvent;
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
 *   <li>1.20's "Java Edition" under the title logo (1.20.6's {@code LogoRenderer}: 128 by 14 at width / 2 - 64,
 *       37 below the logo's top), and its Minceraft logo, while this mod's logo is the one Core serves;</li>
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

    static final ResourceLocation TITLE_LOGO = new ResourceLocation("textures/gui/title/minecraft.png");
    static final ResourceLocation EDITION = new ResourceLocation("textures/gui/title/edition.png");

    /** Counters for the client self-test. */
    public static volatile int editionFrames;
    public static volatile int splashFrames;
    public static volatile int backButtons;
    public static volatile int guiScaleScrolls;
    public static volatile boolean iconSet;

    /** 1.20.2's chart switches, which F3 alone no longer changes. */
    static boolean profilerChart;
    static boolean fpsCharts;

    private static Field minceraftRoll;
    /** True while minecraft.png is bound to 1.20's Minceraft logo. */
    private static boolean minceraft;

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

    /** @return true while Core serves this mod's title logo (no newer installed version replaced it) */
    public static boolean ourLogo() {
        return LookAndFeel.LOGO.equals(Presentation.asset("textures/gui/title/minecraft.png"));
    }

    /**
     * A new title screen: 1.12.2 shows its Minceraft as shuffled slices of minecraft.png, one time in 10,000. 1.20 has
     * a Minceraft logo of its own, so the roll is taken here, and minecraft.png is bound to 1.20's Minceraft logo
     * for that screen instead.
     */
    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (!(event.getGui() instanceof GuiMainMenu)) {
            return;
        }
        boolean rolled = false;
        if (ourLogo()) {
            try {
                if (minceraftRoll == null) {
                    minceraftRoll = ReflectionHelper.findField(GuiMainMenu.class, "minceraftRoll", "field_73974_b");
                }
                if (minceraftRoll.getFloat(event.getGui()) < 1.0E-4F) {
                    minceraftRoll.setFloat(event.getGui(), 1.0F);
                    rolled = true;
                }
            } catch (Exception e) {
                TrailsAndTalesBackport.LOGGER.debug("Look and feel: Minceraft roll unreadable ({})", e.toString());
            }
        }
        showMinceraft(rolled);
    }

    /** Binds minecraft.png to 1.20's Minceraft logo, or back to the normal one. */
    public static void showMinceraft(boolean show) {
        if (show == minceraft) {
            return;
        }
        minceraft = show;
        Minecraft mc = Minecraft.getMinecraft();
        mc.getTextureManager().deleteTexture(TITLE_LOGO);
        if (show) {
            mc.getTextureManager().loadTexture(TITLE_LOGO, new SimpleTexture(LookAndFeel.MINCERAFT));
        }
    }

    /** @return true while the title screen shows 1.20's Minceraft logo */
    public static boolean showingMinceraft() {
        return minceraft;
    }

    /**
     * 1.12.2 draws its splash before anything this mod can draw after it, and where 1.12.2 puts it (width / 2 + 90,
     * 70), which "Java Edition" now covers. While this mod's logo is shown, the splash is taken out of the title
     * screen for its own drawing and drawn after "Java Edition", where 1.20 draws it.
     */
    @SubscribeEvent
    public void onDrawPre(GuiScreenEvent.DrawScreenEvent.Pre event) {
        if (!(event.getGui() instanceof GuiMainMenu) || !ourLogo()) {
            return;
        }
        String splash = splash(event.getGui());
        heldSplash = splash;
        if (splash != null && !splash.isEmpty()) {
            setSplash(event.getGui(), "");
        }
    }

    /**
     * 1.20.6 {@code LogoRenderer.renderLogo}: "Java Edition", 128 by 14, overlapping the logo's bottom by 7; then
     * {@code SplashRenderer.render}: the splash at width / 2 + 123, 69, turned 20 degrees, pulsing as 1.12.2's.
     */
    @SubscribeEvent
    public void onDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.getGui() instanceof GuiMainMenu) || !ourLogo()) {
            return;
        }
        GuiScreen gui = event.getGui();
        String splash = heldSplash;
        heldSplash = null;
        if (splash != null && !splash.isEmpty()) {
            setSplash(gui, splash);
        }
        Minecraft mc = Minecraft.getMinecraft();
        mc.getTextureManager().bindTexture(EDITION);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        Gui.drawModalRectWithCustomSizedTexture(gui.width / 2 - 64, 30 + 44 - 7, 0.0F, 0.0F, 128, 14, 128.0F, 16.0F);
        GlStateManager.disableBlend();
        editionFrames++;
        if (splash != null && !splash.isEmpty()) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(gui.width / 2.0F + 123.0F, 69.0F, 0.0F);
            GlStateManager.rotate(-20.0F, 0.0F, 0.0F, 1.0F);
            float size = 1.8F - MathHelper.abs(MathHelper.sin((float) (Minecraft.getSystemTime() % 1000L) / 1000.0F * ((float) Math.PI * 2F)) * 0.1F);
            size = size * 100.0F / (float) (mc.fontRenderer.getStringWidth(splash) + 32);
            GlStateManager.scale(size, size, size);
            gui.drawCenteredString(mc.fontRenderer, splash, 0, -8, 0xFFFFFF00);
            GlStateManager.popMatrix();
            splashFrames++;
        }
    }

    private static String heldSplash;
    private static Field splashText;

    @javax.annotation.Nullable
    static String splash(GuiScreen menu) {
        try {
            if (splashText == null) {
                splashText = ReflectionHelper.findField(GuiMainMenu.class, "splashText", "field_73975_c");
            }
            return (String) splashText.get(menu);
        } catch (Exception e) {
            return null;
        }
    }

    private static void setSplash(GuiScreen menu, String text) {
        try {
            splashText.set(menu, text);
        } catch (Exception ignored) {
        }
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
        if (Keyboard.getEventKey() == Keyboard.KEY_F3 && !Keyboard.getEventKeyState()) {
            applyCharts(Minecraft.getMinecraft());
        }
    }

    // ---- Menus ----

    /** 1.20.2: Multiplayer's and World Selection's Cancel reads Back. */
    @SubscribeEvent
    public void onInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.getGui() instanceof GuiMultiplayer) && !(event.getGui() instanceof GuiWorldSelection)) {
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
        if (!(event.getGui() instanceof GuiVideoSettings) || !GuiScreen.isCtrlKeyDown()) {
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

    /** 1.20's window icon, 16 and 32 pixels, as 1.12.2's {@code Minecraft.setWindowIcon} sets it (not on macOS). */
    static void setIcon() {
        if (Util.getOSType() == Util.EnumOS.OSX) {
            return;
        }
        try {
            ByteBuffer small = icon("icon_16x16.png");
            ByteBuffer large = icon("icon_32x32.png");
            if (small != null && large != null) {
                Display.setIcon(new ByteBuffer[] {small, large});
                iconSet = true;
            }
        } catch (Exception e) {
            TrailsAndTalesBackport.LOGGER.warn("Look and feel: the window icon was not set ({})", e.toString());
        }
    }

    private static ByteBuffer icon(String name) throws Exception {
        InputStream in = LookAndFeelClient.class.getResourceAsStream("/assets/" + TrailsAndTalesBackport.MOD_ID + "/textures/gui/icon/" + name);
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
