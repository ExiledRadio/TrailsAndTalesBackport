package com.exiledradio.trailsandtalesbackport.heads.client;

import com.exiledradio.trailsandtalesbackport.heads.HeadsContent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityZombieVillager;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

/**
 * A worn piglin head, drawn where 1.12.2's {@code LayerCustomHead} draws a worn skull (its skull branch, line
 * for line), with the wearer's walk animation driving the ears as 1.20's {@code CustomHeadLayer} does.
 *
 * <p>Added beside every {@code LayerCustomHead}, on the same head part; the item renderer draws nothing
 * for the worn stack, so the generic worn-item path that layer takes for a non-skull item shows nothing.
 */
public class LayerPiglinHead implements LayerRenderer<EntityLivingBase> {

    private final ModelRenderer head;

    public LayerPiglinHead(ModelRenderer head) {
        this.head = head;
    }

    @Override
    public void doRenderLayer(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks,
                              float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        ItemStack stack = entity.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
        if (stack.isEmpty() || HeadsContent.headItem == null || stack.getItem() != HeadsContent.headItem) {
            return;
        }
        GlStateManager.pushMatrix();
        if (entity.isSneaking()) {
            GlStateManager.translate(0.0F, 0.2F, 0.0F);
        }
        boolean villager = entity instanceof EntityVillager || entity instanceof EntityZombieVillager;
        if (entity.isChild() && !(entity instanceof EntityVillager)) {
            GlStateManager.translate(0.0F, 0.5F * scale, 0.0F);
            GlStateManager.scale(0.7F, 0.7F, 0.7F);
            GlStateManager.translate(0.0F, 16.0F * scale, 0.0F);
        }
        this.head.postRender(0.0625F);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.scale(1.1875F, -1.1875F, -1.1875F);
        if (villager) {
            GlStateManager.translate(0.0F, 0.0625F, 0.0F);
        }
        Minecraft.getMinecraft().getTextureManager().bindTexture(RenderPiglinHead.TEXTURE);
        RenderPiglinHead.renderHead(-0.5F, 0.0F, -0.5F, EnumFacing.UP, 180.0F, limbSwing);
        GlStateManager.popMatrix();
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}
