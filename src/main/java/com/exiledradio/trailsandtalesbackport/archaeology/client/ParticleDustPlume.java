package com.exiledradio.trailsandtalesbackport.archaeology.client;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/**
 * 1.20.3's {@code dust_plume} particle (a pot taking an item): an ash-smoke puff colored {@code #BAB1C2}
 * less up to 0.2, rising 0.15 a tick with half gravity, its gravity and drag easing off each tick, on the
 * generic smoke sprites 7 down to 0, growing to full size over its first thirty-second.
 */
public class ParticleDustPlume extends Particle {

    private static final int COLOR = 12235202;

    private float friction = 0.96F;
    private final float baseScale;

    public ParticleDustPlume(World world, double x, double y, double z, double xd, double yd, double zd) {
        super(world, x, y, z, 0.0D, 0.0D, 0.0D);
        this.motionX *= 0.7D;
        this.motionY *= 0.6D;
        this.motionZ *= 0.7D;
        this.motionX += xd;
        this.motionY += yd + 0.15D;
        this.motionZ += zd;
        float dim = this.rand.nextFloat() * 0.2F;
        this.particleRed = ((COLOR >> 16) & 255) / 255.0F - dim;
        this.particleGreen = ((COLOR >> 8) & 255) / 255.0F - dim;
        this.particleBlue = (COLOR & 255) / 255.0F - dim;
        this.particleScale *= 0.75F;
        this.baseScale = this.particleScale;
        this.particleMaxAge = Math.max(1, (int) (7.0D / (this.rand.nextFloat() * 0.8D + 0.2D)));
        this.particleGravity = 0.5F;
        this.canCollide = false;
        sprite();
    }

    private void sprite() {
        setParticleTextureIndex(7 - this.particleAge * 7 / this.particleMaxAge);
    }

    @Override
    public void onUpdate() {
        this.particleGravity *= 0.88F;
        this.friction *= 0.92F;
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        if (this.particleAge++ >= this.particleMaxAge) {
            setExpired();
            return;
        }
        this.motionY -= 0.04D * this.particleGravity;
        move(this.motionX, this.motionY, this.motionZ);
        if (this.posY == this.prevPosY) {
            this.motionX *= 1.1D;
            this.motionZ *= 1.1D;
        }
        this.motionX *= this.friction;
        this.motionY *= this.friction;
        this.motionZ *= this.friction;
        if (this.onGround) {
            this.motionX *= 0.7D;
            this.motionZ *= 0.7D;
        }
        if (this.particleAge <= this.particleMaxAge) {
            sprite();
        }
    }

    @Override
    public void renderParticle(BufferBuilder buffer, Entity entity, float partialTicks, float rotationX, float rotationZ,
                               float rotationYZ, float rotationXY, float rotationXZ) {
        this.particleScale = this.baseScale
                * MathHelper.clamp(((float) this.particleAge + partialTicks) / (float) this.particleMaxAge * 32.0F, 0.0F, 1.0F);
        super.renderParticle(buffer, entity, partialTicks, rotationX, rotationZ, rotationYZ, rotationXY, rotationXZ);
        this.particleScale = this.baseScale;
    }
}
