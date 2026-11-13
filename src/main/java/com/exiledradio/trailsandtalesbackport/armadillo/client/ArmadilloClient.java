package com.exiledradio.trailsandtalesbackport.armadillo.client;

import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.armadillo.EntityArmadillo;

import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.RenderingRegistry;

/** Client parts of the armadillo stream: the renderer. */
public final class ArmadilloClient {

    static final ResourceLocation TEXTURE = new ResourceLocation(TrailsAndTalesBackport.MOD_ID, "textures/entity/armadillo.png");

    private ArmadilloClient() {
    }

    public static void init() {
        RenderingRegistry.registerEntityRenderingHandler(EntityArmadillo.class, Render::new);
    }

    /** 1.20.5's {@code ArmadilloRenderer}: shadow 0.4 (halved for a baby, as 1.12.2 does), one texture. */
    static final class Render extends RenderLiving<EntityArmadillo> {
        Render(RenderManager manager) {
            super(manager, new ModelArmadillo(), 0.4F);
        }

        @Override
        protected ResourceLocation getEntityTexture(EntityArmadillo entity) {
            return TEXTURE;
        }
    }
}
