package com.exiledradio.trailsandtalesbackport.armadillo.client;

import com.exiledradio.backportcore.presentation.VanillaRevisions;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.armadillo.EntityArmadillo;

import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.RenderingRegistry;

/** Client parts of the armadillo stream: the renderer. */
public final class ArmadilloClient {

    static final ResourceLocation TEXTURE = new ResourceLocation(TrailsAndTalesBackport.MOD_ID, "textures/entity/armadillo.png");
    static final ResourceLocation BABY_TEXTURE = new ResourceLocation(TrailsAndTalesBackport.MOD_ID, "textures/entity/armadillo_baby.png");

    private ArmadilloClient() {
    }

    public static void init() {
        RenderingRegistry.registerEntityRenderingHandler(EntityArmadillo.class, Render::new);
    }

    /**
     * 1.20.5's {@code ArmadilloRenderer}: shadow 0.4 (halved for a baby, as 1.12.2 does), one texture. While Core's
     * {@code VanillaRevisions.babyModels()} (26.1 installed, as with Game Drops Backport), a baby is 26.3's:
     * {@code BabyArmadilloModel} and {@code armadillo_baby.png}.
     */
    static final class Render extends RenderLiving<EntityArmadillo> {
        private final ModelArmadillo adult;
        private final ModelArmadillo baby = new ModelArmadillo(true);

        Render(RenderManager manager) {
            super(manager, new ModelArmadillo(), 0.4F);
            this.adult = (ModelArmadillo) this.mainModel;
        }

        /** @return true when this one is drawn as 26.1's baby */
        static boolean modernBaby(EntityArmadillo entity) {
            return entity.isChild() && VanillaRevisions.babyModels();
        }

        @Override
        public void doRender(EntityArmadillo entity, double x, double y, double z, float entityYaw, float partialTicks) {
            this.mainModel = modernBaby(entity) ? this.baby : this.adult;
            super.doRender(entity, x, y, z, entityYaw, partialTicks);
        }

        @Override
        protected ResourceLocation getEntityTexture(EntityArmadillo entity) {
            return modernBaby(entity) ? BABY_TEXTURE : TEXTURE;
        }
    }
}
