package com.exiledradio.trailsandtalesbackport.sign.client;

import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.hangingsign.TileHangingSign;
import com.exiledradio.trailsandtalesbackport.sign.SignSides;
import net.minecraft.block.Block;
import net.minecraft.block.BlockStandingSign;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Other mods' signs with a renderer of their own (Decided 6's limit, closed): their renderer still draws the board and
 * the front text; this wraps it and adds what it cannot know about, in vanilla's sign layout (where 1.12.2's renderer
 * and every mod copying it puts the text): the back face's text ({@link SignTextRenderer}, turned half a turn as 1.20's
 * {@code translateSignText}), and, on a glowing front, the front text again glowing over theirs (1.20's outline and
 * full-bright text). A sign whose own renderer lays its board out differently gets the text in vanilla's place.
 *
 * <p>Wrapped once, on the first client tick (after every mod's own binding): every {@code TileEntitySign} subclass
 * bound to a renderer that is not this mod's.
 */
@SideOnly(Side.CLIENT)
public final class OtherSignRenderer extends TileEntitySpecialRenderer<TileEntitySign> {

    private final TileEntitySpecialRenderer<TileEntitySign> theirs;

    /** For the client self-test: the tile classes wrapped. */
    public static final List<String> WRAPPED = new ArrayList<String>();

    OtherSignRenderer(TileEntitySpecialRenderer<TileEntitySign> theirs) {
        this.theirs = theirs;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static void wrapAll() {
        TileEntityRendererDispatcher dispatcher = TileEntityRendererDispatcher.instance;
        for (Map.Entry<Class<? extends TileEntity>, TileEntitySpecialRenderer<? extends TileEntity>> e
                : new ArrayList<>(dispatcher.renderers.entrySet())) {
            Class<? extends TileEntity> type = e.getKey();
            TileEntitySpecialRenderer<?> renderer = e.getValue();
            if (!TileEntitySign.class.isAssignableFrom(type) || TileHangingSign.class.isAssignableFrom(type)
                    || renderer == null || ours(renderer)) {
                continue;
            }
            OtherSignRenderer wrapper = new OtherSignRenderer((TileEntitySpecialRenderer) renderer);
            wrapper.setRendererDispatcher(dispatcher);
            dispatcher.renderers.put(type, (TileEntitySpecialRenderer) wrapper);
            WRAPPED.add(type.getName());
            TrailsAndTalesBackport.LOGGER.info("Signs: {}'s own renderer ({}) gets 1.20's back text and glow drawn over it",
                    type.getName(), renderer.getClass().getName());
        }
    }

    private static boolean ours(TileEntitySpecialRenderer<?> renderer) {
        return renderer instanceof StandardSignRenderer || renderer instanceof HangingSignSidesRenderer
                || renderer instanceof OtherSignRenderer;
    }

    @Override
    public void render(TileEntitySign te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        this.theirs.render(te, x, y, z, partialTicks, destroyStage, alpha);
        if (destroyStage >= 0) {
            return;
        }
        boolean back = SignSides.hasMessage(te, false);
        boolean frontGlow = SignSides.glowing(te, true) && SignSides.hasMessage(te, true);
        if (!back && !frontGlow) {
            return;
        }
        Block block = te.getBlockType();
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x + 0.5F, (float) y + 0.5F, (float) z + 0.5F);
        if (block instanceof BlockStandingSign) {
            GlStateManager.rotate(-(te.getBlockMetadata() * 360) / 16.0F, 0.0F, 1.0F, 0.0F);
        } else {
            int facing = te.getBlockMetadata();
            float angle = facing == 2 ? 180.0F : facing == 4 ? 90.0F : facing == 5 ? -90.0F : 0.0F;
            GlStateManager.rotate(-angle, 0.0F, 1.0F, 0.0F);
            GlStateManager.translate(0.0F, -0.3125F, -0.4375F);
        }
        for (int face = 0; face < 2; face++) {
            if (face == 0 ? !frontGlow : !back) {
                continue;
            }
            GlStateManager.pushMatrix();
            if (face == 1) {
                GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
            }
            // A hair in front of their text, so the glowing copy covers it.
            GlStateManager.translate(0.0F, 0.33333334F, face == 0 ? 0.046666667F + 0.0005F : 0.046666667F);
            GlStateManager.scale(0.010416667F, -0.010416667F, 0.010416667F);
            GlStateManager.glNormal3f(0.0F, 0.0F, -0.010416667F);
            GlStateManager.depthMask(false);
            SignTextRenderer.draw(getFontRenderer(), te, face == 0, 10, 90);
            GlStateManager.depthMask(true);
            GlStateManager.popMatrix();
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    @Override
    public boolean isGlobalRenderer(TileEntitySign te) {
        return this.theirs.isGlobalRenderer(te);
    }
}
