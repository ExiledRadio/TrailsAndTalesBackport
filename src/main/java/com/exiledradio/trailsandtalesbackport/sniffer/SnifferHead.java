package com.exiledradio.trailsandtalesbackport.sniffer;

import net.minecraft.entity.EntityBodyHelper;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityLookHelper;
import net.minecraft.util.math.MathHelper;

/**
 * 1.20's {@code Sniffer.getMaxHeadYRot()}, 50 degrees: how far the head turns from the body. 1.12.2 fixes it at 75 in
 * both places 1.20 reads it, so both get a sniffer version: the look helper ({@code LookControl.clampHeadRotationToBody},
 * while it has a path) and the body helper ({@code BodyRotationControl}: the head kept within it while walking, the
 * body following a head turned further, and the head easing to the front after standing still).
 */
public final class SnifferHead {

    /** 1.20's {@code Sniffer.getMaxHeadYRot}. */
    public static final float MAX_HEAD_YAW = 50.0F;

    private SnifferHead() {
    }

    /** {@code Mth.rotateIfNecessary}: {@code to} kept within {@code max} degrees of {@code from}. */
    static float within(float from, float to, float max) {
        float f = MathHelper.wrapDegrees(from - to);
        if (f < -max) {
            f = -max;
        }
        if (f >= max) {
            f = max;
        }
        return from - f;
    }

    /** The look helper, with 1.20's limit while the sniffer has a path. */
    static final class Look extends EntityLookHelper {
        private final EntityLiving sniffer;

        Look(EntityLiving sniffer) {
            super(sniffer);
            this.sniffer = sniffer;
        }

        @Override
        public void onUpdateLook() {
            super.onUpdateLook();
            if (!this.sniffer.getNavigator().noPath()) {
                this.sniffer.rotationYawHead = within(this.sniffer.renderYawOffset, this.sniffer.rotationYawHead, MAX_HEAD_YAW);
            }
        }
    }

    /** 1.12.2's {@code EntityBodyHelper} (1.20's {@code BodyRotationControl}) with 50 in place of 75. */
    static final class Body extends EntityBodyHelper {
        private final EntityLivingBase living;
        private int stableTicks;
        private float lastStableHead;

        Body(EntityLivingBase living) {
            super(living);
            this.living = living;
        }

        @Override
        public void updateRenderAngles() {
            double dx = this.living.posX - this.living.prevPosX;
            double dz = this.living.posZ - this.living.prevPosZ;
            if (dx * dx + dz * dz > 2.500000277905201E-7D) {
                this.living.renderYawOffset = this.living.rotationYaw;
                this.living.rotationYawHead = within(this.living.renderYawOffset, this.living.rotationYawHead, MAX_HEAD_YAW);
                this.lastStableHead = this.living.rotationYawHead;
                this.stableTicks = 0;
                return;
            }
            if (this.living.getPassengers().isEmpty() || !(this.living.getPassengers().get(0) instanceof EntityLiving)) {
                float max = MAX_HEAD_YAW;
                if (Math.abs(this.living.rotationYawHead - this.lastStableHead) > 15.0F) {
                    this.stableTicks = 0;
                    this.lastStableHead = this.living.rotationYawHead;
                } else if (++this.stableTicks > 10) {
                    max = Math.max(1.0F - (this.stableTicks - 10) / 10.0F, 0.0F) * MAX_HEAD_YAW;
                }
                this.living.renderYawOffset = within(this.living.rotationYawHead, this.living.renderYawOffset, max);
            }
        }
    }
}
