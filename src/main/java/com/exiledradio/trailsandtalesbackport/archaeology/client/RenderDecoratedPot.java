package com.exiledradio.trailsandtalesbackport.archaeology.client;

import com.exiledradio.trailsandtalesbackport.archaeology.ItemDecoratedPot;
import com.exiledradio.trailsandtalesbackport.archaeology.TileDecoratedPot;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.MathHelper;

/**
 * Draws a placed pot turned to its facing, with 1.20.3's wobble: the positive wobble (an item went in)
 * rocks it on two axes for 7 ticks, the negative one (nothing went in) twists it for 10.
 */
public class RenderDecoratedPot extends TileEntitySpecialRenderer<TileDecoratedPot> {

    private static final float WOBBLE_AMPLITUDE = 0.125F;
    static final PotModel MODEL = new PotModel();

    @Override
    public void render(TileDecoratedPot pot, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        EnumFacing facing = pot.facing();
        GlStateManager.translate(0.5F, 0.0F, 0.5F);
        GlStateManager.rotate(180.0F - facing.getHorizontalAngle(), 0.0F, 1.0F, 0.0F);
        GlStateManager.translate(-0.5F, 0.0F, -0.5F);
        TileDecoratedPot.Wobble style = pot.lastWobble;
        if (style != null && pot.getWorld() != null) {
            float progress = ((float) (pot.getWorld().getTotalWorldTime() - pot.wobbleStartedAtTick) + partialTicks)
                    / (float) style.duration;
            if (progress >= 0.0F && progress <= 1.0F) {
                GlStateManager.translate(0.5F, 0.0F, 0.5F);
                if (style == TileDecoratedPot.Wobble.POSITIVE) {
                    float t = progress * ((float) Math.PI * 2F);
                    float tiltX = -1.5F * (MathHelper.cos(t) + 0.5F) * MathHelper.sin(t / 2.0F);
                    GlStateManager.rotate((float) Math.toDegrees(tiltX * 0.015625F), 1.0F, 0.0F, 0.0F);
                    float tiltZ = MathHelper.sin(t);
                    GlStateManager.rotate((float) Math.toDegrees(tiltZ * 0.015625F), 0.0F, 0.0F, 1.0F);
                } else {
                    float turn = MathHelper.sin(-progress * 3.0F * (float) Math.PI) * WOBBLE_AMPLITUDE;
                    float fade = 1.0F - progress;
                    GlStateManager.rotate((float) Math.toDegrees(turn * fade), 0.0F, 1.0F, 0.0F);
                }
                GlStateManager.translate(-0.5F, 0.0F, -0.5F);
            }
        }
        MODEL.render(pot.sherds());
        GlStateManager.popMatrix();
    }

    /** The pot's item, drawn by the same model (its item model is {@code builtin/entity}). */
    public static class ItemRenderer extends TileEntityItemStackRenderer {
        @Override
        public void renderByItem(ItemStack stack) {
            GlStateManager.pushMatrix();
            MODEL.render(ItemDecoratedPot.sherds(stack));
            GlStateManager.popMatrix();
        }
    }
}
