package com.exiledradio.trailsandtalesbackport.lookfeel.client;

import com.exiledradio.backportcore.presentation.LoadingRevision;
import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.backportcore.presentation.SpawnProgress;
import com.exiledradio.backportcore.presentation.client.LoadingScreens;
import com.exiledradio.backportcore.presentation.client.PanoramaRenderer;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.lookfeel.LookAndFeel;
import com.exiledradio.trailsandtalesbackport.lookfeel.LookRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

/**
 * 1.20.5's world-loading screen (24w09a), the drawer of Core's {@link LoadingRevision#PANORAMA_1_20_5}: instead of
 * the dirt, the title screen's panorama, blurred by Menu Background Blur and darkened by {@code menu_background.png}
 * (1.20.6's {@code Screen.renderBackground}: panorama, {@code processBlurEffect}, {@code renderMenuBackground}).
 * Over it, while the save is read, "Reading world data..." in the middle ({@code GenericMessageScreen}); then the
 * chunk grid in the middle with the percentage just above it ({@code LevelLoadingScreen.render}), in 1.20's colors
 * (23w16a: carvers take the old liquid carvers' blue, light is pale yellow).
 *
 * <p>The grid keeps 1.12.2's spawn area: 1.20.5 shows fewer chunks because it prepares fewer (its spawn chunk
 * radius of 2), and this game still prepares 1.12.2's 25 by 25.
 */
@SideOnly(Side.CLIENT)
public final class LoadingScreen1205 implements LoadingScreens.Drawer {

    static final ResourceLocation MENU_BACKGROUND = new ResourceLocation("textures/gui/menu_background.png");
    static final ResourceLocation BLUR = new ResourceLocation(TrailsAndTalesBackport.MOD_ID, "shaders/post/menu_blur.json");
    /** {@code GL_FRAMEBUFFER_BINDING} (and its EXT twin). */
    private static final int FRAMEBUFFER_BINDING = 0x8CA6;
    /** 1.20.6's {@code box_blur} passes: the radius times 1, 1/2 and 1/4, each across then down. */
    private static final float[] PASS_SCALE = {1.0F, 1.0F, 0.5F, 0.5F, 0.25F, 0.25F};

    /** For the client self-test. */
    public static volatile int frames;
    public static volatile int blurredFrames;
    public static volatile int gridFrames;
    /** The client self-test sets this: the next grid frame is saved there as a PNG. */
    public static volatile File capture;

    /** Keeps the panorama's clock running across the loading screen's frames. */
    private static final GuiScreen CLOCK = new GuiScreen() {
    };

    private Framebuffer panorama;
    private ShaderGroup blur;
    private boolean blurFailed;
    private static Field listShaders;

    @Override
    public void draw(Minecraft mc, int width, int height, LoadingScreens.Frame frame) {
        background(mc, width, height);
        if (!frame.preparingSpawn) {
            String text = I18n.format("selectWorld.data_read");
            mc.fontRenderer.drawStringWithShadow(text, width / 2 - mc.fontRenderer.getStringWidth(text) / 2.0F, height / 2 - 9 / 2, 0xFFFFFF);
        } else {
            int x = width / 2;
            int y = height / 2;
            drawGrid(x, y, 2);
            String percent = MathHelper.clamp(frame.percent, 0, 100) + "%";
            int side = SpawnProgress.GRID_RADIUS * 2 + 1;
            mc.fontRenderer.drawStringWithShadow(percent, x - mc.fontRenderer.getStringWidth(percent) / 2.0F, y - (side + 9 + 2), 0xFFFFFF);
            gridFrames++;
            if (capture != null) {
                File file = capture;
                capture = null;
                Capture.save(mc, file);
            }
        }
        frames++;
    }

    /** The blurred, darkened panorama over the whole screen. */
    void background(Minecraft mc, int width, int height) {
        float radius = LookAndFeel.blurRadius();
        boolean blurred = false;
        if (radius >= 1.0F && OpenGlHelper.isFramebufferEnabled() && OpenGlHelper.shadersSupported && !this.blurFailed) {
            int target = GL11.glGetInteger(FRAMEBUFFER_BINDING);
            try {
                if (this.panorama == null || this.panorama.framebufferWidth != mc.displayWidth || this.panorama.framebufferHeight != mc.displayHeight) {
                    resize(mc);
                }
                this.panorama.framebufferClear();
                this.panorama.bindFramebuffer(true);
                panoramaAndOverlay(mc, width, height);
                setRadius(radius);
                this.blur.render(0.0F);
                blurred = true;
            } catch (Exception e) {
                this.blurFailed = true;
                TrailsAndTalesBackport.LOGGER.warn("Look and feel: the loading screen's blur is off ({})", e.toString());
            }
            OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, target);
            GlStateManager.viewport(0, 0, mc.displayWidth, mc.displayHeight);
            if (blurred) {
                drawPanoramaTexture(width, height);
                blurredFrames++;
            }
        }
        if (!blurred) {
            panoramaAndOverlay(mc, width, height);
        }
        // renderMenuBackground: menu_background.png tiled at 32 by 32.
        mc.getTextureManager().bindTexture(MENU_BACKGROUND);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        Gui.drawModalRectWithCustomSizedTexture(0, 0, 0.0F, 0.0F, width, height, 32.0F, 32.0F);
        GlStateManager.disableBlend();
    }

    /** 1.20.6's {@code PanoramaRenderer.render}: the cube, then the overlay stretched over the screen. */
    private static void panoramaAndOverlay(Minecraft mc, int width, int height) {
        GlStateManager.disableDepth();
        PanoramaRenderer.render(CLOCK);
        ResourceLocation overlay = Presentation.panoramaOverlay();
        if (overlay != null) {
            mc.getTextureManager().bindTexture(overlay);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            Gui.drawScaledCustomSizeModalRect(0, 0, 0.0F, 0.0F, 16, 128, width, height, 16.0F, 128.0F);
            GlStateManager.disableBlend();
        }
        GlStateManager.enableDepth();
    }

    private void resize(Minecraft mc) throws Exception {
        if (this.panorama == null) {
            this.panorama = new Framebuffer(mc.displayWidth, mc.displayHeight, false);
            this.panorama.setFramebufferFilter(GL11.GL_LINEAR);
        } else {
            this.panorama.createBindFramebuffer(mc.displayWidth, mc.displayHeight);
        }
        if (this.blur != null) {
            this.blur.deleteShaderGroup();
        }
        this.blur = new ShaderGroup(mc.getTextureManager(), mc.getResourceManager(), this.panorama, BLUR);
        this.blur.createBindFramebuffers(mc.displayWidth, mc.displayHeight);
    }

    @SuppressWarnings("unchecked")
    private void setRadius(float radius) throws Exception {
        if (listShaders == null) {
            listShaders = ReflectionHelper.findField(ShaderGroup.class, "listShaders", "field_148031_d");
        }
        List<Shader> passes = (List<Shader>) listShaders.get(this.blur);
        for (int i = 0; i < passes.size(); i++) {
            float scale = i < PASS_SCALE.length ? PASS_SCALE[i] : 1.0F;
            passes.get(i).getShaderManager().getShaderUniformOrDefault("Radius").set(radius * scale);
        }
    }

    /** The blurred panorama, drawn over the loading screen's own frame (a framebuffer's texture is upside down). */
    private void drawPanoramaTexture(int width, int height) {
        Framebuffer fb = this.panorama;
        float u = (float) fb.framebufferWidth / (float) fb.framebufferTextureWidth;
        float v = (float) fb.framebufferHeight / (float) fb.framebufferTextureHeight;
        GlStateManager.disableDepth();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        fb.bindFramebufferTexture();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(0.0D, height, 0.0D).tex(0.0D, 0.0D).endVertex();
        buffer.pos(width, height, 0.0D).tex(u, 0.0D).endVertex();
        buffer.pos(width, 0.0D, 0.0D).tex(u, v).endVertex();
        buffer.pos(0.0D, 0.0D, 0.0D).tex(0.0D, v).endVertex();
        tessellator.draw();
        fb.unbindFramebufferTexture();
        GlStateManager.enableDepth();
    }

    /**
     * 1.20.6's {@code LevelLoadingScreen.renderChunks} with no spacing: every cell of Core's grid, 2 by 2 pixels,
     * centered on the point.
     */
    static void drawGrid(int centerX, int centerY, int scale) {
        int side = SpawnProgress.GRID_RADIUS * 2 + 1;
        int total = side * scale;
        int x0 = centerX - total / 2;
        int y0 = centerY - total / 2;
        GlStateManager.disableTexture2D();
        GlStateManager.disableBlend();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        for (int gx = 0; gx < side; gx++) {
            for (int gz = 0; gz < side; gz++) {
                int rgb = LookRules.gridColor(SpawnProgress.status(gx - SpawnProgress.GRID_RADIUS, gz - SpawnProgress.GRID_RADIUS));
                int r = rgb >> 16 & 255;
                int g = rgb >> 8 & 255;
                int b = rgb & 255;
                int left = x0 + gx * scale;
                int top = y0 + gz * scale;
                buffer.pos(left, top + scale, 0.0D).color(r, g, b, 255).endVertex();
                buffer.pos(left + scale, top + scale, 0.0D).color(r, g, b, 255).endVertex();
                buffer.pos(left + scale, top, 0.0D).color(r, g, b, 255).endVertex();
                buffer.pos(left, top, 0.0D).color(r, g, b, 255).endVertex();
            }
        }
        tessellator.draw();
        GlStateManager.enableTexture2D();
    }
}
