package com.exiledradio.trailsandtalesbackport.camel.client;

import com.exiledradio.trailsandtalesbackport.camel.AnimationClock;
import com.exiledradio.trailsandtalesbackport.camel.EntityCamel;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * 1.20's {@code CamelModel}: the same parts, cubes, texture offsets (128 x 128), pivots and 0.05 grown saddle
 * and bridle, with 1.20's keyframe animations (walk by the limb swing at 2x, strength 2.5x; then sit, sit pose,
 * stand up, idle and dash by their clocks), the head turned within 30 degrees and tipped back as the dash cools
 * down, the saddle and bridle shown when saddled and the reins when also ridden, and a baby at 0.45 size moved
 * down 29.35 pixels ({@code 1.834375} blocks after scaling).
 *
 * <p>Built with {@code baby}, 26.1's {@code BabyCamelModel} instead: its own parts (64 x 64, for
 * {@code camel_baby.png}) and animations ({@code CamelBabyAnimation}), drawn at full size and never saddled.
 */
public class ModelCamel extends ModelBase {

    private static final float MAX_WALK_ANIMATION_SPEED = 2.0F;
    private static final float WALK_ANIMATION_SCALE_FACTOR = 2.5F;
    private static final float BABY_SCALE = 0.45F;
    private static final float BABY_Y_OFFSET = 1.834375F;
    private static final float GROW = 0.05F;

    private final AnimatedPart root;
    private final AnimatedPart head;
    private final AnimatedPart[] saddleParts;
    private final AnimatedPart[] ridingParts;
    private final Map<String, AnimatedPart> parts = new HashMap<String, AnimatedPart>();
    /** 26.1's baby model: no baby scale, the baby animations. */
    private final boolean baby;

    public ModelCamel() {
        this(false);
    }

    public ModelCamel(boolean baby) {
        this.baby = baby;
        if (baby) {
            // 26.3's BabyCamelModel.createBodyLayer.
            this.textureWidth = 64;
            this.textureHeight = 64;
            this.root = part("root", null, 0.0F, 0.0F, 0.0F);
            AnimatedPart body = part("body", this.root, 0.0F, 7.0F, 0.0F);
            box(body, 0, 14, -4.5F, -4.0F, -8.0F, 9, 8, 16, 0.0F, false);
            box(part("tail", body, 0.0F, -1.5F, 8.05F), 50, 38, -1.5F, -0.5F, 0.0F, 3, 9, 0, 0.0F, false);
            this.head = part("head", body, 0.0F, 1.0F, -7.5F);
            box(this.head, 20, 0, -2.5F, -3.0F, -7.5F, 5, 5, 7, 0.0F, false);
            box(this.head, 0, 0, -2.5F, -12.0F, -7.5F, 5, 9, 5, 0.0F, false);
            box(this.head, 0, 14, -2.5F, -12.0F, -10.5F, 5, 4, 3, 0.0F, false);
            box(part("right_ear", this.head, -2.5F, -11.0F, -4.0F), 37, 0, -3.0F, -0.5F, -1.0F, 3, 1, 2, 0.0F, false);
            box(part("left_ear", this.head, 2.5F, -11.0F, -4.0F), 47, 0, 0.0F, -0.5F, -1.0F, 3, 1, 2, 0.0F, false);
            box(part("right_front_leg", this.root, -3.0F, 11.5F, -5.5F), 36, 14, -1.5F, -0.5F, -1.5F, 3, 13, 3, 0.0F, false);
            box(part("left_front_leg", this.root, 3.0F, 11.5F, -5.5F), 48, 14, -1.5F, -0.5F, -1.5F, 3, 13, 3, 0.0F, false);
            box(part("left_hind_leg", this.root, 3.0F, 11.5F, 5.5F), 12, 38, -1.5F, -0.5F, -1.5F, 3, 13, 3, 0.0F, false);
            box(part("right_hind_leg", this.root, -3.0F, 11.5F, 5.5F), 0, 38, -1.5F, -0.5F, -1.5F, 3, 13, 3, 0.0F, false);
            this.saddleParts = new AnimatedPart[0];
            this.ridingParts = new AnimatedPart[0];
            return;
        }
        this.textureWidth = 128;
        this.textureHeight = 128;
        this.root = part("root", null, 0.0F, 0.0F, 0.0F);
        AnimatedPart body = part("body", this.root, 0.0F, 4.0F, 9.5F);
        box(body, 0, 25, -7.5F, -12.0F, -23.5F, 15, 12, 27, 0.0F, false);
        box(part("hump", body, 0.0F, -12.0F, -10.0F), 74, 0, -4.5F, -5.0F, -5.5F, 9, 5, 11, 0.0F, false);
        box(part("tail", body, 0.0F, -9.0F, 3.5F), 122, 0, -1.5F, 0.0F, 0.0F, 3, 14, 0, 0.0F, false);
        this.head = part("head", body, 0.0F, -3.0F, -19.5F);
        box(this.head, 60, 24, -3.5F, -7.0F, -15.0F, 7, 8, 19, 0.0F, false);
        box(this.head, 21, 0, -3.5F, -21.0F, -15.0F, 7, 14, 7, 0.0F, false);
        box(this.head, 50, 0, -2.5F, -21.0F, -21.0F, 5, 5, 6, 0.0F, false);
        box(part("left_ear", this.head, 2.5F, -21.0F, -9.5F), 45, 0, -0.5F, 0.5F, -1.0F, 3, 1, 2, 0.0F, false);
        box(part("right_ear", this.head, -2.5F, -21.0F, -9.5F), 67, 0, -2.5F, 0.5F, -1.0F, 3, 1, 2, 0.0F, false);
        box(part("left_hind_leg", this.root, 4.9F, 1.0F, 9.5F), 58, 16, -2.5F, 2.0F, -2.5F, 5, 21, 5, 0.0F, false);
        box(part("right_hind_leg", this.root, -4.9F, 1.0F, 9.5F), 94, 16, -2.5F, 2.0F, -2.5F, 5, 21, 5, 0.0F, false);
        box(part("left_front_leg", this.root, 4.9F, 1.0F, -10.5F), 0, 0, -2.5F, 2.0F, -2.5F, 5, 21, 5, 0.0F, false);
        box(part("right_front_leg", this.root, -4.9F, 1.0F, -10.5F), 0, 26, -2.5F, 2.0F, -2.5F, 5, 21, 5, 0.0F, false);
        AnimatedPart saddle = part("saddle", body, 0.0F, 0.0F, 0.0F);
        box(saddle, 74, 64, -4.5F, -17.0F, -15.5F, 9, 5, 11, GROW, false);
        box(saddle, 92, 114, -3.5F, -20.0F, -15.5F, 7, 3, 11, GROW, false);
        box(saddle, 0, 89, -7.5F, -12.0F, -23.5F, 15, 12, 27, GROW, false);
        AnimatedPart reins = part("reins", this.head, 0.0F, 0.0F, 0.0F);
        box(reins, 98, 42, 3.51F, -18.0F, -17.0F, 0, 7, 15, 0.0F, false);
        box(reins, 84, 57, -3.5F, -18.0F, -2.0F, 7, 7, 0, 0.0F, false);
        box(reins, 98, 42, -3.51F, -18.0F, -17.0F, 0, 7, 15, 0.0F, false);
        AnimatedPart bridle = part("bridle", this.head, 0.0F, 0.0F, 0.0F);
        box(bridle, 60, 87, -3.5F, -7.0F, -15.0F, 7, 8, 19, GROW, false);
        box(bridle, 21, 64, -3.5F, -21.0F, -15.0F, 7, 14, 7, GROW, false);
        box(bridle, 50, 64, -2.5F, -21.0F, -21.0F, 5, 5, 6, GROW, false);
        box(bridle, 74, 70, 2.5F, -19.0F, -18.0F, 1, 2, 2, 0.0F, false);
        box(bridle, 74, 70, -3.5F, -19.0F, -18.0F, 1, 2, 2, 0.0F, true);
        this.saddleParts = new AnimatedPart[] {saddle, bridle};
        this.ridingParts = new AnimatedPart[] {reins};
    }

    private AnimatedPart part(String name, AnimatedPart parent, float x, float y, float z) {
        AnimatedPart p = new AnimatedPart(this, name).pose(x, y, z);
        if (parent != null) {
            parent.addChild(p);
        }
        this.parts.put(name, p);
        return p;
    }

    /** 1.20's {@code texOffs(u, v)[.mirror()].addBox(x, y, z, w, h, d, new CubeDeformation(grow))}. */
    private static void box(AnimatedPart part, int u, int v, float x, float y, float z, int w, int h, int d, float grow,
                            boolean mirror) {
        part.cubeList.add(new ModelBox(part, u, v, x, y, z, w, h, d, grow, mirror));
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                                  float headPitch, float scale, Entity entity) {
        for (AnimatedPart p : this.parts.values()) {
            p.reset();
        }
        EntityCamel camel = (EntityCamel) entity;
        applyHeadRotation(camel, netHeadYaw, headPitch, ageInTicks);
        boolean saddled = camel.isHorseSaddled();
        boolean ridden = camel.isBeingRidden();
        for (AnimatedPart p : this.saddleParts) {
            p.showModel = saddled;
        }
        for (AnimatedPart p : this.ridingParts) {
            p.showModel = ridden && saddled;
        }
        // 1.20's animateWalk: the walk's time from the limb swing, its strength from the swing amount.
        long walkTime = (long) (limbSwing * 50.0F * MAX_WALK_ANIMATION_SPEED);
        float walkScale = Math.min(limbSwingAmount * WALK_ANIMATION_SCALE_FACTOR, 1.0F);
        if (this.baby) {
            CamelBabyAnimations.CAMEL_BABY_WALK.apply(this.parts, walkTime, walkScale);
            animate(camel.sitAnimation, CamelBabyAnimations.CAMEL_BABY_SIT, ageInTicks);
            animate(camel.sitPoseAnimation, CamelBabyAnimations.CAMEL_BABY_SIT_POSE, ageInTicks);
            animate(camel.sitUpAnimation, CamelBabyAnimations.CAMEL_BABY_STANDUP, ageInTicks);
            animate(camel.idleAnimation, CamelBabyAnimations.CAMEL_BABY_IDLE, ageInTicks);
            animate(camel.dashAnimation, CamelBabyAnimations.CAMEL_BABY_DASH, ageInTicks);
            return;
        }
        CamelAnimations.CAMEL_WALK.apply(this.parts, walkTime, walkScale);
        animate(camel.sitAnimation, CamelAnimations.CAMEL_SIT, ageInTicks);
        animate(camel.sitPoseAnimation, CamelAnimations.CAMEL_SIT_POSE, ageInTicks);
        animate(camel.sitUpAnimation, CamelAnimations.CAMEL_STANDUP, ageInTicks);
        animate(camel.idleAnimation, CamelAnimations.CAMEL_IDLE, ageInTicks);
        animate(camel.dashAnimation, CamelAnimations.CAMEL_DASH, ageInTicks);
    }

    /** 1.20: within 30 degrees aside and -25 to 45 up and down; tipped up to 45 degrees back by the dash cooldown. */
    private void applyHeadRotation(EntityCamel camel, float yaw, float pitch, float ageInTicks) {
        yaw = MathHelper.clamp(yaw, -30.0F, 30.0F);
        pitch = MathHelper.clamp(pitch, -25.0F, 45.0F);
        if (camel.getJumpCooldown() > 0) {
            float partial = ageInTicks - camel.ticksExisted;
            float tip = 45.0F * (camel.getJumpCooldown() - partial) / EntityCamel.DASH_COOLDOWN_TICKS;
            pitch = MathHelper.clamp(pitch + tip, -25.0F, 70.0F);
        }
        this.head.rotateAngleY = yaw * (float) (Math.PI / 180.0D);
        this.head.rotateAngleX = pitch * (float) (Math.PI / 180.0D);
    }

    private void animate(AnimationClock clock, KeyframeAnimation animation, float ageInTicks) {
        if (clock.isStarted()) {
            animation.apply(this.parts, clock.timeInMillis(ageInTicks), 1.0F);
        }
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                       float headPitch, float scale) {
        setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        if (this.isChild && !this.baby) {
            GlStateManager.pushMatrix();
            GlStateManager.scale(BABY_SCALE, BABY_SCALE, BABY_SCALE);
            GlStateManager.translate(0.0F, BABY_Y_OFFSET, 0.0F);
            this.root.render(scale);
            GlStateManager.popMatrix();
        } else {
            this.root.render(scale);
        }
    }
}
