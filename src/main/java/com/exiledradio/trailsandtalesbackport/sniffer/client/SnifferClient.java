package com.exiledradio.trailsandtalesbackport.sniffer.client;

import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.sniffer.EntitySniffer;
import com.exiledradio.trailsandtalesbackport.sniffer.SnifferContent;
import com.exiledradio.trailsandtalesbackport.sniffer.SnifferSide;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.client.particle.ParticleSuspendedTown;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Random;

/**
 * Client parts of the sniffer stream: the renderer, the digging sound that follows a sniffer, and the
 * sparkles of an egg on moss.
 */
public final class SnifferClient extends SnifferSide {

    static final ResourceLocation TEXTURE = new ResourceLocation(TrailsAndTalesBackport.MOD_ID, "textures/entity/sniffer/sniffer.png");
    private static TextureAtlasSprite glint;

    private SnifferClient() {
    }

    public static void init() {
        SnifferContent.setSide(new SnifferClient());
        RenderingRegistry.registerEntityRenderingHandler(EntitySniffer.class, Render::new);
        MinecraftForge.EVENT_BUS.register(new Events());
    }

    @Override
    public void diggingSound(EntitySniffer sniffer) {
        Minecraft.getMinecraft().getSoundHandler().playSound(new DiggingSound(sniffer));
    }

    /**
     * 1.20's level event 3009 ({@code ParticleUtils.spawnParticlesOnBlockFaces} with the egg-crack particle,
     * 3 to 6 on each face): the happy-villager particle with 1.20's {@code glint} sprite, white.
     */
    @Override
    public void eggBoostParticles(World world, BlockPos pos) {
        if (glint == null) {
            return;
        }
        Random rand = world.rand;
        for (EnumFacing face : EnumFacing.values()) {
            int count = 3 + rand.nextInt(4);
            for (int i = 0; i < count; i++) {
                double x = pos.getX() + 0.5D + face.getXOffset() * 0.55D + (face.getXOffset() == 0 ? rand.nextDouble() - 0.5D : 0.0D);
                double y = pos.getY() + 0.5D + face.getYOffset() * 0.55D + (face.getYOffset() == 0 ? rand.nextDouble() - 0.5D : 0.0D);
                double z = pos.getZ() + 0.5D + face.getZOffset() * 0.55D + (face.getZOffset() == 0 ? rand.nextDouble() - 0.5D : 0.0D);
                Minecraft.getMinecraft().effectRenderer.addEffect(new EggCrack(world, x, y, z));
            }
        }
    }

    public static final class Events {
        @SubscribeEvent
        public void stitch(TextureStitchEvent.Pre event) {
            glint = event.getMap().registerSprite(new ResourceLocation(TrailsAndTalesBackport.MOD_ID, "particle/glint"));
        }
    }

    /** 1.20's {@code SuspendedTownParticle.EggCrackProvider}: white, the glint sprite. */
    static final class EggCrack extends ParticleSuspendedTown {
        EggCrack(World world, double x, double y, double z) {
            super(world, x, y, z, 0.0D, 0.0D, 0.0D);
            setParticleTexture(glint);
            setRBGColorF(1.0F, 1.0F, 1.0F);
        }

        @Override
        public int getFXLayer() {
            return 1;
        }
    }

    /** 1.20's {@code SnifferRenderer}: shadow 1.1, one texture. */
    static final class Render extends RenderLiving<EntitySniffer> {
        Render(RenderManager manager) {
            super(manager, new ModelSniffer(), 1.1F);
        }

        @Override
        protected ResourceLocation getEntityTexture(EntitySniffer entity) {
            return TEXTURE;
        }
    }

    /**
     * 1.20's {@code SnifferSoundInstance}: the digging sound, linear falloff, played once at the sniffer and
     * following it, stopped when it is gone, attacking, or neither digging nor searching.
     */
    static final class DiggingSound extends MovingSound {
        private final EntitySniffer sniffer;

        DiggingSound(EntitySniffer sniffer) {
            super(SnifferContent.DIGGING, SoundCategory.NEUTRAL);
            this.sniffer = sniffer;
            this.attenuationType = AttenuationType.LINEAR;
            this.repeat = false;
            this.repeatDelay = 0;
            this.xPosF = (float) sniffer.posX;
            this.yPosF = (float) sniffer.posY;
            this.zPosF = (float) sniffer.posZ;
        }

        @Override
        public void update() {
            if (!this.sniffer.isDead && this.sniffer.getAttackTarget() == null && this.sniffer.canPlayDiggingSound()
                    && !this.sniffer.isSilent()) {
                this.xPosF = (float) this.sniffer.posX;
                this.yPosF = (float) this.sniffer.posY;
                this.zPosF = (float) this.sniffer.posZ;
                this.volume = 1.0F;
                this.pitch = 1.0F;
            } else {
                this.donePlaying = true;
            }
        }
    }
}
