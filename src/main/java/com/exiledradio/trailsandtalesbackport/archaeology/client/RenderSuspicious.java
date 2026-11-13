package com.exiledradio.trailsandtalesbackport.archaeology.client;

import com.exiledradio.trailsandtalesbackport.archaeology.BlockSuspicious;
import com.exiledradio.trailsandtalesbackport.archaeology.TileSuspicious;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

/**
 * 1.20's {@code BrushableBlockRenderer}: once a suspicious block is dusted, its item pokes out of the
 * brushed face, further out at each stage, lit by the light in front of that face.
 */
public class RenderSuspicious extends TileEntitySpecialRenderer<TileSuspicious> {

    @Override
    public void render(TileSuspicious te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        if (te.getWorld() == null) {
            return;
        }
        IBlockState state = te.getWorld().getBlockState(te.getPos());
        if (!(state.getBlock() instanceof BlockSuspicious)) {
            return;
        }
        int dusted = state.getValue(BlockSuspicious.DUSTED);
        EnumFacing face = te.hitDirection();
        ItemStack item = te.item();
        if (dusted <= 0 || face == null || item.isEmpty()) {
            return;
        }
        float[] t = translations(face, dusted);
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y + 0.5D, z);
        GlStateManager.translate(t[0], t[1], t[2]);
        GlStateManager.rotate(75.0F, 0.0F, 1.0F, 0.0F);
        boolean eastWest = face == EnumFacing.EAST || face == EnumFacing.WEST;
        GlStateManager.rotate((float) ((eastWest ? 90 : 0) + 11), 0.0F, 1.0F, 0.0F);
        GlStateManager.scale(0.5F, 0.5F, 0.5F);
        int light = te.getWorld().getCombinedLight(te.getPos().offset(face), 0);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) (light % 65536), (float) (light / 65536));
        RenderHelper.enableStandardItemLighting();
        Minecraft.getMinecraft().getRenderItem().renderItem(item, ItemCameraTransforms.TransformType.FIXED);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.popMatrix();
    }

    private static float[] translations(EnumFacing face, int dusted) {
        float[] t = {0.5F, 0.0F, 0.5F};
        float out = (float) dusted / 10.0F * 0.75F;
        switch (face) {
            case EAST:
                t[0] = 0.73F + out;
                break;
            case WEST:
                t[0] = 0.25F - out;
                break;
            case UP:
                t[1] = 0.25F + out;
                break;
            case DOWN:
                t[1] = -0.23F - out;
                break;
            case NORTH:
                t[2] = 0.25F - out;
                break;
            case SOUTH:
                t[2] = 0.73F + out;
                break;
            default:
                break;
        }
        return t;
    }
}
