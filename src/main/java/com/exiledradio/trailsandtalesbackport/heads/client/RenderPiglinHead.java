package com.exiledradio.trailsandtalesbackport.heads.client;

import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.heads.BlockPiglinHead;
import com.exiledradio.trailsandtalesbackport.heads.TilePiglinHead;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

/**
 * The piglin head drawn as 1.20's {@code SkullBlockRenderer.renderSkull} draws it, in 1.12.2's skull
 * renderer's GL setup: standing at the block's center, or on a wall a quarter block out from it, the
 * powered animation passed to the ears.
 */
public class RenderPiglinHead extends TileEntitySpecialRenderer<TilePiglinHead> {

    public static final ResourceLocation TEXTURE =
            new ResourceLocation(TrailsAndTalesBackport.MOD_ID, "textures/entity/piglin/piglin.png");

    static final ModelPiglinHead MODEL = new ModelPiglinHead();

    /** The living entity being drawn now (set around each entity render), so the item renderer can skip a worn head. */
    @Nullable
    static EntityLivingBase drawing;

    @Override
    public void render(TilePiglinHead te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        EnumFacing facing = EnumFacing.UP;
        if (te.hasWorld()) {
            IBlockState state = te.getWorld().getBlockState(te.getPos());
            if (state.getPropertyKeys().contains(BlockPiglinHead.FACING)) {
                facing = state.getValue(BlockPiglinHead.FACING);
            }
        }
        float rotation = te.getRotation() * 360 / 16.0F;
        if (destroyStage >= 0) {
            bindTexture(DESTROY_STAGES[destroyStage]);
            GlStateManager.matrixMode(5890);
            GlStateManager.pushMatrix();
            GlStateManager.scale(4.0F, 4.0F, 1.0F);
            GlStateManager.translate(0.0625F, 0.0625F, 0.0625F);
            GlStateManager.matrixMode(5888);
        } else {
            bindTexture(TEXTURE);
        }
        renderHead((float) x, (float) y, (float) z, facing, rotation, te.getAnimation(partialTicks));
        if (destroyStage >= 0) {
            GlStateManager.matrixMode(5890);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(5888);
        }
    }

    /**
     * Draws the head with its texture already bound.
     *
     * @param facing   {@code UP} for a standing head, else the direction a wall head looks
     * @param rotation degrees, for a standing head
     */
    static void renderHead(float x, float y, float z, EnumFacing facing, float rotation, float animation) {
        GlStateManager.pushMatrix();
        GlStateManager.disableCull();
        if (facing == EnumFacing.UP) {
            GlStateManager.translate(x + 0.5F, y, z + 0.5F);
        } else {
            GlStateManager.translate(x + 0.5F - facing.getXOffset() * 0.25F, y + 0.25F, z + 0.5F - facing.getZOffset() * 0.25F);
            rotation = wallRotation(facing);
        }
        GlStateManager.enableRescaleNormal();
        GlStateManager.scale(-1.0F, -1.0F, 1.0F);
        GlStateManager.enableAlpha();
        MODEL.setupAnim(animation, rotation, 0.0F);
        MODEL.render(0.0625F);
        GlStateManager.popMatrix();
    }

    /** 1.20's {@code RotationSegment.convertToDegrees(convertToSegment(facing.getOpposite()))}. */
    static float wallRotation(EnumFacing facing) {
        return facing.getOpposite().getHorizontalAngle();
    }

    /** The item: a standing head turned to face the viewer, as 1.12.2 draws its skull items. */
    public static final class ItemRenderer extends TileEntityItemStackRenderer {
        @Override
        public void renderByItem(ItemStack stack, float partialTicks) {
            EntityLivingBase wearer = drawing;
            if (wearer != null && wearer.getItemStackFromSlot(EntityEquipmentSlot.HEAD) == stack) {
                // Worn: LayerPiglinHead draws it where a skull goes; the generic worn-item path draws nothing.
                return;
            }
            Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);
            GlStateManager.pushMatrix();
            GlStateManager.disableCull();
            renderHead(0.0F, 0.0F, 0.0F, EnumFacing.UP, 180.0F, 0.0F);
            GlStateManager.enableCull();
            GlStateManager.popMatrix();
        }
    }
}
