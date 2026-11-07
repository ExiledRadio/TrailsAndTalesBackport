package com.exiledradio.trailsandtalesbackport.cherry.client;

import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Random;

/**
 * Client parts of the cherry stream: 1.20's falling cherry petal particle ({@code cherry_leaves}).
 *
 * <p>The twelve petal sprites ({@code particle/cherry_0..11}) are stitched into the block atlas, which is
 * the atlas 1.12.2's particle layer 1 draws from.
 */
public final class CherryClient {

    private static final TextureAtlasSprite[] SPRITES = new TextureAtlasSprite[12];

    private CherryClient() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new CherryClient.Events());
    }

    /** Called from the leaves' random display tick: one petal just under the block. */
    public static void petal(World world, BlockPos pos, Random rand) {
        if (SPRITES[0] == null) {
            return;
        }
        Minecraft.getMinecraft().effectRenderer.addEffect(new Petal(world,
                pos.getX() + rand.nextDouble(), pos.getY() - 0.05D, pos.getZ() + rand.nextDouble(), rand));
    }

    public static final class Events {
        @SubscribeEvent
        public void stitch(TextureStitchEvent.Pre event) {
            for (int i = 0; i < SPRITES.length; i++) {
                SPRITES[i] = event.getMap().registerSprite(
                        new ResourceLocation(TrailsAndTalesBackport.MOD_ID, "particle/cherry_" + i));
            }
        }
    }

    /**
     * 1.20's {@code CherryParticle}: lives 300 ticks, falls very slowly (gravity 0.00075), drifts sideways
     * more and more in one direction, spins, and is gone when it lands or stops against something.
     */
    static final class Petal extends Particle {
        private final float drift;
        private float spin;
        private final float spinAcceleration;

        Petal(World world, double x, double y, double z, Random rand) {
            super(world, x, y, z);
            setParticleTexture(SPRITES[rand.nextInt(SPRITES.length)]);
            this.spin = (float) Math.toRadians(rand.nextBoolean() ? -30.0D : 30.0D);
            this.drift = rand.nextFloat();
            this.spinAcceleration = (float) Math.toRadians(rand.nextBoolean() ? -5.0D : 5.0D);
            this.particleMaxAge = 300;
            this.particleGravity = 7.5E-4F;
            float size = rand.nextBoolean() ? 0.05F : 0.075F;
            // 1.12.2 draws a quad 0.1 * particleScale across; 1.20 gives the size directly.
            this.particleScale = size * 10.0F;
            setSize(size, size);
            this.motionX = 0;
            this.motionY = 0;
            this.motionZ = 0;
            this.particleRed = this.particleGreen = this.particleBlue = 1.0F;
        }

        @Override
        public int getFXLayer() {
            return 1;
        }

        @Override
        public void onUpdate() {
            this.prevPosX = this.posX;
            this.prevPosY = this.posY;
            this.prevPosZ = this.posZ;
            if (this.particleMaxAge-- <= 0) {
                setExpired();
                return;
            }
            float age = Math.min((300 - this.particleMaxAge) / 300.0F, 1.0F);
            double push = 2.0D * Math.pow(age, 1.25D);
            this.motionX += Math.cos(Math.toRadians(this.drift * 60.0F)) * push * 0.0025D;
            this.motionZ += Math.sin(Math.toRadians(this.drift * 60.0F)) * push * 0.0025D;
            this.motionY -= this.particleGravity;
            this.spin += this.spinAcceleration / 20.0F;
            this.prevParticleAngle = this.particleAngle;
            this.particleAngle += this.spin / 20.0F;
            move(this.motionX, this.motionY, this.motionZ);
            if (this.onGround || this.particleMaxAge < 299 && (this.motionX == 0.0D || this.motionZ == 0.0D)) {
                setExpired();
            }
        }
    }
}
