package com.exiledradio.trailsandtalesbackport.mechanics;

import com.exiledradio.trailsandtalesbackport.ModConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * 1.20.2's mob melee reach (23w31a), patch P7. 1.12.2's {@code EntityAIAttackMelee} hits when the squared distance from
 * the attacker's feet to the target's feet is at most {@code getAttackReachSqr} ({@code (2 * width)^2 + target width}):
 * a wide mob reaches far (an iron golem nearly three blocks, through a wall), and the height of either mob never counts.
 * 1.20.2's {@code Mob.isWithinMeleeAttackRange} instead asks whether the attacker's box, widened by
 * {@code sqrt(2.04) - 0.6} to each side and not at all up or down (joined with its mount's box when riding), touches
 * the target's hitbox (its box, cut off below the point it sits at when riding: {@code LivingEntity.getHitbox}), and
 * {@code MeleeAttackGoal.canPerformAttack} also needs the attacker to see the target.
 *
 * <p>The patch hands every {@code checkAndPerformAttack} (vanilla's and each override in a class that extends
 * {@code EntityAIAttackMelee} or {@code EntityAIZombieAttack}) a distance that answers 1.20.2's question against the
 * method's own reach: well inside the reach when the target is in range and seen, just beyond it otherwise. Every
 * comparison the method makes then follows 1.20.2, and overrides that use the reach for something else (the polar
 * bear's warning, within twice the reach) keep the real distance when it is larger.
 */
public final class MeleeReach {

    /** 1.20.2's {@code Mob.DEFAULT_ATTACK_REACH}. */
    public static final double REACH = Math.sqrt(2.04F) - 0.6F;

    private MeleeReach() {
    }

    public static boolean enabled() {
        return ModConfig.mechanics.meleeReach;
    }

    /**
     * Called first in each patched {@code checkAndPerformAttack(target, distSq)}; the result replaces {@code distSq}.
     *
     * @param reach the method's own {@code getAttackReachSqr(target)}
     */
    public static double distance(EntityLiving attacker, EntityLivingBase target, double distSq, double reach) {
        if (!enabled() || attacker == null || target == null) {
            return distSq;
        }
        if (canHit(attacker, target)) {
            double inside = reach > 0.0D ? reach * 0.5D : reach - 1.0D;
            return Math.min(distSq, inside);
        }
        return Math.max(distSq, Math.nextUp(Math.max(reach, 0.0D)));
    }

    /** 1.20.2's {@code isWithinMeleeAttackRange} and line of sight. */
    public static boolean canHit(EntityLiving attacker, EntityLivingBase target) {
        return withinRange(attacker, target) && attacker.getEntitySenses().canSee(target);
    }

    /** {@code Mob.isWithinMeleeAttackRange}: the attack box touches the target's hitbox. */
    public static boolean withinRange(EntityLivingBase attacker, EntityLivingBase target) {
        AxisAlignedBB hitbox = hitbox(target);
        return hitbox != null && attackBox(attacker).intersects(hitbox);
    }

    /** {@code Mob.getAttackBoundingBox}. */
    public static AxisAlignedBB attackBox(EntityLivingBase attacker) {
        AxisAlignedBB own = attacker.getEntityBoundingBox();
        Entity vehicle = attacker.getRidingEntity();
        AxisAlignedBB box = own;
        if (vehicle != null) {
            AxisAlignedBB v = vehicle.getEntityBoundingBox();
            box = new AxisAlignedBB(Math.min(own.minX, v.minX), own.minY, Math.min(own.minZ, v.minZ),
                    Math.max(own.maxX, v.maxX), own.maxY, Math.max(own.maxZ, v.maxZ));
        }
        return box.grow(REACH, 0.0D, REACH);
    }

    /**
     * {@code LivingEntity.getHitbox}: a rider's box starts no lower than the point it sits at (1.12.2's
     * {@code posY + getMountedYOffset()} of the mount, where 1.20.2 has {@code getPassengerRidingPosition}).
     *
     * @return null when nothing of the box is left above that point
     */
    public static AxisAlignedBB hitbox(EntityLivingBase target) {
        AxisAlignedBB box = target.getEntityBoundingBox();
        Entity vehicle = target.getRidingEntity();
        if (vehicle == null) {
            return box;
        }
        double seat = vehicle.posY + vehicle.getMountedYOffset();
        if (seat <= box.minY) {
            return box;
        }
        if (seat >= box.maxY) {
            return null;
        }
        return new AxisAlignedBB(box.minX, seat, box.minZ, box.maxX, box.maxY, box.maxZ);
    }
}
