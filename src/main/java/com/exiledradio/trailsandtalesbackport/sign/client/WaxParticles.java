package com.exiledradio.trailsandtalesbackport.sign.client;

import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * Level event 3003's particles (1.20.6 {@code LevelRenderer.levelEvent}: {@code ParticleUtils.spawnParticlesOnBlockFaces(
 * level, pos, ParticleTypes.WAX_ON, UniformInt.of(3, 5))}): three to five on each of the six faces, 0.55 from the
 * block's center, spread over the face, their speed along the face random in -0.5..0.5. The particle is
 * {@code GlowParticle.WaxOnProvider}: color (0.91, 0.55, 0.08), speed times 0.005 sideways and 0.01 up, 10 to 39
 * ticks, on the {@code glow} sprite, three quarters size, drag 0.96, no collision, brightening to full block light
 * over its life.
 */
@SideOnly(Side.CLIENT)
public final class WaxParticles {

    static TextureAtlasSprite glow;
    /** For the client self-test. */
    public static volatile int spawned;

    @SubscribeEvent
    public void stitch(TextureStitchEvent.Pre event) {
        glow = event.getMap().registerSprite(new ResourceLocation(TrailsAndTalesBackport.MOD_ID, "particle/glow"));
    }

    /** From the packet: on the client thread. */
    public static void receive(final BlockPos pos) {
        final Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> {
            if (mc.world != null) {
                spawn(mc.world, pos);
            }
        });
    }

    public static void spawn(World world, BlockPos pos) {
        Random r = world.rand;
        for (EnumFacing face : EnumFacing.values()) {
            int count = 3 + r.nextInt(3);
            for (int i = 0; i < count; i++) {
                int sx = face.getXOffset();
                int sy = face.getYOffset();
                int sz = face.getZOffset();
                double vx = MathHelper.nextDouble(r, -0.5D, 0.5D);
                double vy = MathHelper.nextDouble(r, -0.5D, 0.5D);
                double vz = MathHelper.nextDouble(r, -0.5D, 0.5D);
                double x = pos.getX() + 0.5D + (sx == 0 ? MathHelper.nextDouble(r, -0.5D, 0.5D) : sx * 0.55D);
                double y = pos.getY() + 0.5D + (sy == 0 ? MathHelper.nextDouble(r, -0.5D, 0.5D) : sy * 0.55D);
                double z = pos.getZ() + 0.5D + (sz == 0 ? MathHelper.nextDouble(r, -0.5D, 0.5D) : sz * 0.55D);
                Minecraft.getMinecraft().effectRenderer.addEffect(new WaxOn(world, x, y, z,
                        sx == 0 ? vx : 0.0D, sy == 0 ? vy : 0.0D, sz == 0 ? vz : 0.0D));
                spawned++;
            }
        }
    }

    /** 1.20.6's {@code GlowParticle} as {@code WaxOnProvider} makes it. */
    static final class WaxOn extends Particle {
        WaxOn(World world, double x, double y, double z, double xd, double yd, double zd) {
            super(world, x, y, z);
            if (glow != null) {
                setParticleTexture(glow);
            }
            setRBGColorF(0.91F, 0.55F, 0.08F);
            this.motionX = xd * 0.01D / 2.0D;
            this.motionY = yd * 0.01D;
            this.motionZ = zd * 0.01D / 2.0D;
            this.particleScale *= 0.75F;
            this.canCollide = false;
            this.particleGravity = 0.0F;
            this.particleMaxAge = this.rand.nextInt(30) + 10;
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
            if (this.particleAge++ >= this.particleMaxAge) {
                setExpired();
                return;
            }
            move(this.motionX, this.motionY, this.motionZ);
            if (this.posY == this.prevPosY) {
                // speedUpWhenYMotionIsBlocked
                this.motionX *= 1.1D;
                this.motionZ *= 1.1D;
            }
            this.motionX *= 0.96D;
            this.motionY *= 0.96D;
            this.motionZ *= 0.96D;
            if (this.onGround) {
                this.motionX *= 0.7D;
                this.motionZ *= 0.7D;
            }
        }

        @Override
        public int getBrightnessForRender(float partialTicks) {
            float f = MathHelper.clamp((this.particleAge + partialTicks) / this.particleMaxAge, 0.0F, 1.0F);
            int light = super.getBrightnessForRender(partialTicks);
            int block = (light & 255) + (int) (f * 15.0F * 16.0F);
            if (block > 240) {
                block = 240;
            }
            return block | (light >> 16 & 255) << 16;
        }
    }
}
