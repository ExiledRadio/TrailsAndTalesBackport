package com.exiledradio.trailsandtalesbackport.mechanics.client;

import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderBat;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.fml.client.registry.RenderingRegistry;

/** Client parts of the mechanics stream: 1.20.3's bat, and 1.20.3's names of looked-at entities (P20). */
public final class MechanicsClient {

    static final ResourceLocation BAT_TEXTURE = new ResourceLocation(TrailsAndTalesBackport.MOD_ID, "textures/entity/bat.png");

    private MechanicsClient() {
    }

    public static void init() {
        RenderingRegistry.registerEntityRenderingHandler(EntityBat.class, BatRender::new);
    }

    /**
     * 1.20.3's {@code BatRenderer}: the new model at full size (shadow 0.25), no bobbing of its own (the animations
     * move it). With the setting off it hands the bat to 1.12.2's {@link RenderBat}.
     */
    public static final class BatRender extends RenderLiving<EntityBat> {

        private final RenderBat old;

        BatRender(RenderManager manager) {
            super(manager, new ModelBat120(), 0.25F);
            this.old = new RenderBat(manager);
        }

        @Override
        public void doRender(EntityBat entity, double x, double y, double z, float entityYaw, float partialTicks) {
            if (!ModConfig.mechanics.batModel) {
                this.old.doRender(entity, x, y, z, entityYaw, partialTicks);
                return;
            }
            super.doRender(entity, x, y, z, entityYaw, partialTicks);
        }

        @Override
        protected ResourceLocation getEntityTexture(EntityBat entity) {
            return BAT_TEXTURE;
        }
    }

    /**
     * P20, the body of {@code Render.canRenderName}, for entities whose renderer does not override it (not mobs):
     * 1.12.2 shows a name only with {@code CustomNameVisible}; 1.20.3's {@code EntityRenderer.shouldShowName} also
     * shows a custom name while the entity is under the crosshair. 1.20.3's crosshair entity is any entity the
     * crosshair hits; 1.12.2 keeps that in {@code objectMouseOver}, since its {@code pointedEntity} is set only for
     * living entities and item frames (whose renderers do not come here).
     */
    public static boolean canRenderName(Entity entity) {
        if (entity.getAlwaysRenderNameTagForRender() && entity.hasCustomName()) {
            return true;
        }
        return ModConfig.mechanics.entityNames && entity.hasCustomName() && entity == crosshairEntity();
    }

    /** 1.20.3's {@code Minecraft.crosshairPickEntity}: the entity the crosshair hits, of any kind. */
    static Entity crosshairEntity() {
        RayTraceResult hit = Minecraft.getMinecraft().objectMouseOver;
        return hit != null && hit.typeOfHit == RayTraceResult.Type.ENTITY ? hit.entityHit : null;
    }
}
