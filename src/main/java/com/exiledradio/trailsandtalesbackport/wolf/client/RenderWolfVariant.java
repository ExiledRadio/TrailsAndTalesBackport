package com.exiledradio.trailsandtalesbackport.wolf.client;

import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.wolf.WolfArmor;
import com.exiledradio.trailsandtalesbackport.wolf.WolfData;
import com.exiledradio.trailsandtalesbackport.wolf.WolfVariant;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderWolf;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import com.exiledradio.trailsandtalesbackport.wolf.ItemWolfArmor;

import java.util.EnumMap;
import java.util.Map;

/**
 * 1.20.5's {@code WolfRenderer} in place of 1.12.2's: the variant's wild, tame and angry textures, the tail
 * height from the share of health lost (1.20's {@code getTailAngle}, which 1.12.2 counts in health points), and
 * the armor layer drawn before the collar.
 */
public class RenderWolfVariant extends RenderWolf {

    private static final Map<WolfVariant, ResourceLocation[]> TEXTURES = new EnumMap<WolfVariant, ResourceLocation[]>(WolfVariant.class);

    static {
        for (WolfVariant v : WolfVariant.values()) {
            // Pale is 1.12.2's own wolf, drawn with vanilla's textures; the rest are this mod's.
            String ns = v == WolfVariant.PALE ? "minecraft" : TrailsAndTalesBackport.MOD_ID;
            String stem = "textures/entity/wolf/" + v.texture;
            TEXTURES.put(v, new ResourceLocation[] {new ResourceLocation(ns, stem + ".png"),
                    new ResourceLocation(ns, stem + "_tame.png"), new ResourceLocation(ns, stem + "_angry.png")});
        }
    }

    public RenderWolfVariant(RenderManager manager) {
        super(manager);
        this.layerRenderers.add(0, new ArmorLayer(this));
    }

    static WolfVariant variant(EntityWolf wolf) {
        WolfData data = WolfData.of(wolf);
        return data == null ? WolfVariant.PALE : data.variantOrPale();
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityWolf wolf) {
        ResourceLocation[] t = TEXTURES.get(variant(wolf));
        if (wolf.isTamed()) {
            return t[1];
        }
        return wolf.isAngry() ? t[2] : t[0];
    }

    @Override
    protected float handleRotationFloat(EntityWolf wolf, float partialTicks) {
        if (wolf.isAngry()) {
            return 1.5393804F;
        } else if (wolf.isTamed()) {
            float max = wolf.getMaxHealth();
            float lost = (max - wolf.getHealth()) / max;
            return (0.55F - lost * 0.4F) * (float) Math.PI;
        }
        return (float) Math.PI / 5.0F;
    }

    /** 1.20.5's {@code WolfArmorLayer}: the armor, its dyed overlay, and its cracks. */
    static final class ArmorLayer implements LayerRenderer<EntityWolf> {
        static final ResourceLocation ARMOR = tex("wolf_armor");
        static final ResourceLocation OVERLAY = tex("wolf_armor_overlay");
        static final Map<WolfArmor.Crack, ResourceLocation> CRACKS = new EnumMap<WolfArmor.Crack, ResourceLocation>(WolfArmor.Crack.class);

        static {
            CRACKS.put(WolfArmor.Crack.LOW, tex("wolf_armor_crackiness_low"));
            CRACKS.put(WolfArmor.Crack.MEDIUM, tex("wolf_armor_crackiness_medium"));
            CRACKS.put(WolfArmor.Crack.HIGH, tex("wolf_armor_crackiness_high"));
        }

        private final RenderLivingBase<EntityWolf> renderer;
        private final ModelWolfArmor model = new ModelWolfArmor();

        ArmorLayer(RenderLivingBase<EntityWolf> renderer) {
            this.renderer = renderer;
        }

        private static ResourceLocation tex(String name) {
            return new ResourceLocation(TrailsAndTalesBackport.MOD_ID, "textures/entity/wolf/" + name + ".png");
        }

        @Override
        public void doRenderLayer(EntityWolf wolf, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                                  float netHeadYaw, float headPitch, float scale) {
            WolfData data = WolfData.of(wolf);
            if (data == null || !WolfArmor.isWolfArmor(data.armor)) {
                return;
            }
            ItemStack armor = data.armor;
            this.model.setModelAttributes(this.renderer.getMainModel());
            this.model.setLivingAnimations(wolf, limbSwing, limbSwingAmount, partialTicks);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.renderer.bindTexture(ARMOR);
            this.model.render(wolf, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
            if (ItemWolfArmor.hasColor(armor)) {
                int rgb = ItemWolfArmor.getColor(armor);
                GlStateManager.color((rgb >> 16 & 255) / 255.0F, (rgb >> 8 & 255) / 255.0F, (rgb & 255) / 255.0F, 1.0F);
                this.renderer.bindTexture(OVERLAY);
                this.model.render(wolf, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            }
            ResourceLocation crack = CRACKS.get(WolfArmor.Crack.of(armor));
            if (crack != null) {
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                this.renderer.bindTexture(crack);
                this.model.render(wolf, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
                GlStateManager.disableBlend();
            }
        }

        @Override
        public boolean shouldCombineTextures() {
            return false;
        }
    }
}
