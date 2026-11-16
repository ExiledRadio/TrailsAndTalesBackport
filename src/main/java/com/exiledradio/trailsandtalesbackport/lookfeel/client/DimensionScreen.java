package com.exiledradio.trailsandtalesbackport.lookfeel.client;

import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.20.5's dimension-change screen (24w03a, {@code ReceivingLevelScreen.renderBackground}): going to or from the
 * Nether, the nether portal's animated texture fills the screen; to or from the End, the end portal's effect; any
 * other change (or a respawn after death, {@code ClientPacketListener.determineLevelLoadingReason}) keeps the menu
 * background. "Loading terrain..." is drawn over it as before.
 *
 * <p>1.12.2 opens {@code GuiDownloadTerrain} from {@code handleRespawn} after setting the player's new dimension, so the
 * old one is the dimension seen on the last client tick. The screen's draw is replaced only for the two portal
 * reasons; the other case is left to whatever draws the screen's background (Backport Core's 1.20.5 menu background).
 *
 * <p>The end portal is 1.20.6's {@code rendertype_end_portal} shader in fixed function: {@code end_sky.png} at the
 * screen position times its first color, then 15 layers of {@code end_portal.png} added, each layer's texture
 * coordinates the shader's {@code end_portal_layer} matrix applied to the screen position (the shader's math is affine,
 * so transforming a quad's corners is exact), times its color. The textures are 1.20.6's.
 */
@SideOnly(Side.CLIENT)
public final class DimensionScreen {

    public enum Reason {
        NETHER_PORTAL, END_PORTAL, OTHER
    }

    private static final ResourceLocation END_SKY = new ResourceLocation(TrailsAndTalesBackport.MOD_ID, "textures/environment/end_sky.png");
    private static final ResourceLocation END_PORTAL = new ResourceLocation(TrailsAndTalesBackport.MOD_ID, "textures/entity/end_portal.png");
    /** 1.20.6's {@code EndPortalLayers} for {@code RenderType.endPortal()}. */
    static final int LAYERS = 15;
    private static final float[][] COLORS = {
            {0.022087F, 0.098399F, 0.110818F}, {0.011892F, 0.095924F, 0.089485F}, {0.027636F, 0.101689F, 0.100326F},
            {0.046564F, 0.109883F, 0.114838F}, {0.064901F, 0.117696F, 0.097189F}, {0.063761F, 0.086895F, 0.123646F},
            {0.084817F, 0.111994F, 0.166380F}, {0.097489F, 0.154120F, 0.091064F}, {0.106152F, 0.131144F, 0.195191F},
            {0.097721F, 0.110188F, 0.187229F}, {0.133516F, 0.138278F, 0.148582F}, {0.070006F, 0.243332F, 0.235792F},
            {0.196766F, 0.142899F, 0.214696F}, {0.047281F, 0.315338F, 0.321970F}, {0.204675F, 0.390010F, 0.302066F},
            {0.080955F, 0.314821F, 0.661491F}};

    /** The dimension of the last client tick: the one being left when the screen opens. */
    private static Integer lastDimension;
    /** For the open screen. */
    public static volatile Reason reason = Reason.OTHER;
    /** For the client self-test: the reason of the last screen opened, and how many frames this class drew. */
    public static volatile Reason lastReason;
    public static volatile int framesDrawn;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase == TickEvent.Phase.END && mc.player != null && !(mc.currentScreen instanceof GuiDownloadTerrain)) {
            lastDimension = mc.player.dimension;
        }
    }

    @SubscribeEvent
    public void onOpen(GuiOpenEvent event) {
        if (!(event.getGui() instanceof GuiDownloadTerrain)) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        Integer from = lastDimension;
        int to = mc.player != null ? mc.player.dimension : 0;
        boolean dead = mc.player != null && (mc.player.isDead || mc.player.getHealth() <= 0.0F);
        reason = reasonFor(dead, from, to);
        lastReason = reason;
    }

    /** 1.20.6's {@code determineLevelLoadingReason}, with 1.12.2's dimension ids (-1 the Nether, 1 the End). */
    public static Reason reasonFor(boolean dead, Integer from, int to) {
        if (dead || from == null || from == to) {
            return Reason.OTHER;
        }
        if (to == -1 || from == -1) {
            return Reason.NETHER_PORTAL;
        }
        if (to == 1 || from == 1) {
            return Reason.END_PORTAL;
        }
        return Reason.OTHER;
    }

    static boolean active() {
        return Presentation.isAtLeast("1.20.5");
    }

    @SubscribeEvent
    public void onDraw(GuiScreenEvent.DrawScreenEvent.Pre event) {
        GuiScreen gui = event.getGui();
        if (!(gui instanceof GuiDownloadTerrain) || reason == Reason.OTHER || !active()) {
            return;
        }
        event.setCanceled(true);
        Minecraft mc = Minecraft.getMinecraft();
        if (reason == Reason.NETHER_PORTAL) {
            drawNetherPortal(mc, gui.width, gui.height);
        } else {
            drawEndPortal(mc, gui.width, gui.height, event.getRenderPartialTicks());
        }
        gui.drawCenteredString(mc.fontRenderer, I18n.format("multiplayer.downloadingTerrain"), gui.width / 2, gui.height / 2 - 50, 0xFFFFFF);
        framesDrawn++;
    }

    /** {@code blit(0, 0, -90, width, height, sprite)}: the portal's particle sprite stretched over the screen. */
    static void drawNetherPortal(Minecraft mc, int width, int height) {
        TextureAtlasSprite sprite = mc.getBlockRendererDispatcher().getBlockModelShapes().getTexture(Blocks.PORTAL.getDefaultState());
        mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
        Tessellator t = Tessellator.getInstance();
        BufferBuilder b = t.getBuffer();
        b.begin(7, DefaultVertexFormats.POSITION_TEX);
        b.pos(0, height, -90).tex(sprite.getMinU(), sprite.getMaxV()).endVertex();
        b.pos(width, height, -90).tex(sprite.getMaxU(), sprite.getMaxV()).endVertex();
        b.pos(width, 0, -90).tex(sprite.getMaxU(), sprite.getMinV()).endVertex();
        b.pos(0, 0, -90).tex(sprite.getMinU(), sprite.getMinV()).endVertex();
        t.draw();
    }

    /** 1.20.6's {@code GameRenderer} game time for shaders: the day's tick over 24000. */
    static float gameTime(Minecraft mc, float partialTicks) {
        long time = mc.world != null ? mc.world.getTotalWorldTime() : 0L;
        return ((float) (time % 24000L) + partialTicks) / 24000.0F;
    }

    /**
     * The layer's texture coordinates for a screen position (u, v in 0..1, v up), as the shader's
     * {@code texProj0 * end_portal_layer(layer)}: scale and rotate, translate, then {@code SCALE_TRANSLATE}.
     */
    static float[] layerUv(int layer, float time, float u, float v) {
        float l = layer;
        double angle = Math.toRadians((l * l * 4321.0F + l * 9.0F) * 2.0F);
        float c = (float) Math.cos(angle);
        float s = (float) Math.sin(angle);
        float k = (4.5F - l / 4.0F) * 2.0F;
        float x = k * (c * u - s * v);
        float y = k * (s * u + c * v);
        x += 17.0F / l;
        y += (2.0F + l / 1.5F) * (time * 1.5F);
        return new float[] {0.5F * x + 0.25F, 0.5F * y + 0.25F};
    }

    static void drawEndPortal(Minecraft mc, int width, int height, float partialTicks) {
        float time = gameTime(mc, partialTicks);
        GlStateManager.disableAlpha();
        GlStateManager.disableBlend();
        mc.getTextureManager().bindTexture(END_SKY);
        quad(width, height, 0, time, COLORS[0]);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        mc.getTextureManager().bindTexture(END_PORTAL);
        for (int i = 0; i < LAYERS; i++) {
            quad(width, height, i + 1, time, COLORS[i]);
        }
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** One full-screen quad; layer 0 is the sky at the screen position itself. Screen v runs up, GUI y down. */
    private static void quad(int width, int height, int layer, float time, float[] color) {
        float[][] corners = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};
        double[][] at = {{0, height}, {width, height}, {width, 0}, {0, 0}};
        Tessellator t = Tessellator.getInstance();
        BufferBuilder b = t.getBuffer();
        b.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        for (int i = 0; i < 4; i++) {
            float[] uv = layer == 0 ? corners[i] : layerUv(layer, time, corners[i][0], corners[i][1]);
            b.pos(at[i][0], at[i][1], -90).tex(uv[0], uv[1]).color(color[0], color[1], color[2], 1.0F).endVertex();
        }
        t.draw();
    }
}
