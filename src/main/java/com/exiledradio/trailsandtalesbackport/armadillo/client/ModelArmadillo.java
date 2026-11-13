package com.exiledradio.trailsandtalesbackport.armadillo.client;

import com.exiledradio.trailsandtalesbackport.armadillo.AnimationClock;
import com.exiledradio.trailsandtalesbackport.armadillo.EntityArmadillo;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * 1.20.5's {@code ArmadilloModel}: the same parts, cubes, texture offsets (64 x 64), pivots and rotations.
 *
 * <p>Rolled up ({@code shouldHideInShell}) the body's own cubes, the hind legs and the tail are hidden and the
 * ball cube shown; otherwise the head follows the look, clamped to 25 degrees down, 22.5 up and 32.5 aside.
 * Then 1.20.5's keyframe animations: the walk by the limb swing (speed 16.5, strength x 2.5), and rolling out,
 * rolling up and peeking by their clocks. A baby is drawn at 0.6 size, 16.02 pixels down
 * ({@code AgeableHierarchicalModel(0.6, 16.02)}).
 */
public class ModelArmadillo extends ModelBase {

    private static final float BABY_SCALE = 0.6F;
    private static final float BABY_Y_OFFSET = 16.02F;
    private static final float MAX_DOWN_HEAD_ROTATION_EXTENT = 25.0F;
    private static final float MAX_UP_HEAD_ROTATION_EXTENT = 22.5F;
    private static final float MAX_WALK_ANIMATION_SPEED = 16.5F;
    private static final float WALK_ANIMATION_SCALE_FACTOR = 2.5F;

    private final AnimatedPart root;
    private final AnimatedPart body;
    private final AnimatedPart rightHindLeg;
    private final AnimatedPart leftHindLeg;
    private final AnimatedPart cube;
    private final AnimatedPart head;
    private final AnimatedPart tail;
    private final Map<String, AnimatedPart> parts = new HashMap<String, AnimatedPart>();

    public ModelArmadillo() {
        this.textureWidth = 64;
        this.textureHeight = 64;
        this.root = part("root", null, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        this.body = part("body", this.root, 0.0F, 21.0F, 4.0F, 0.0F, 0.0F, 0.0F);
        box(this.body, 0, 20, -4.0F, -7.0F, -10.0F, 8, 8, 12, 0.3F);
        box(this.body, 0, 40, -4.0F, -7.0F, -10.0F, 8, 8, 12, 0.0F);
        this.tail = part("tail", this.body, 0.0F, -3.0F, 1.0F, 0.5061F, 0.0F, 0.0F);
        box(this.tail, 44, 53, -0.5F, -0.0865F, 0.0933F, 1, 6, 1, 0.0F);
        this.head = part("head", this.body, 0.0F, -2.0F, -11.0F, 0.0F, 0.0F, 0.0F);
        box(part("head_cube", this.head, 0.0F, 0.0F, 0.0F, -0.3927F, 0.0F, 0.0F), 43, 15, -1.5F, -1.0F, -1.0F, 3, 5, 2, 0.0F);
        AnimatedPart rightEar = part("right_ear", this.head, -1.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        box(part("right_ear_cube", rightEar, -0.5F, 0.0F, -0.6F, 0.1886F, -0.3864F, -0.0718F), 43, 10, -2.0F, -3.0F, 0.0F, 2, 5, 0, 0.0F);
        AnimatedPart leftEar = part("left_ear", this.head, 1.0F, -2.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        box(part("left_ear_cube", leftEar, 0.5F, 1.0F, -0.6F, 0.1886F, 0.3864F, 0.0718F), 47, 10, 0.0F, -3.0F, 0.0F, 2, 5, 0, 0.0F);
        this.rightHindLeg = part("right_hind_leg", this.root, -2.0F, 21.0F, 4.0F, 0.0F, 0.0F, 0.0F);
        box(this.rightHindLeg, 51, 31, -1.0F, 0.0F, -1.0F, 2, 3, 2, 0.0F);
        this.leftHindLeg = part("left_hind_leg", this.root, 2.0F, 21.0F, 4.0F, 0.0F, 0.0F, 0.0F);
        box(this.leftHindLeg, 42, 31, -1.0F, 0.0F, -1.0F, 2, 3, 2, 0.0F);
        box(part("right_front_leg", this.root, -2.0F, 21.0F, -4.0F, 0.0F, 0.0F, 0.0F), 51, 43, -1.0F, 0.0F, -1.0F, 2, 3, 2, 0.0F);
        box(part("left_front_leg", this.root, 2.0F, 21.0F, -4.0F, 0.0F, 0.0F, 0.0F), 42, 43, -1.0F, 0.0F, -1.0F, 2, 3, 2, 0.0F);
        this.cube = part("cube", this.root, 0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        box(this.cube, 0, 0, -5.0F, -10.0F, -6.0F, 10, 10, 10, 0.0F);
    }

    private AnimatedPart part(String name, AnimatedPart parent, float x, float y, float z, float xRot, float yRot, float zRot) {
        AnimatedPart p = new AnimatedPart(this, name).pose(x, y, z, xRot, yRot, zRot);
        if (parent != null) {
            parent.addChild(p);
        }
        this.parts.put(name, p);
        return p;
    }

    /** 1.20's {@code texOffs(u, v).addBox(x, y, z, w, h, d, new CubeDeformation(grow))}. */
    private static void box(AnimatedPart part, int u, int v, float x, float y, float z, int w, int h, int d, float grow) {
        part.cubeList.add(new ModelBox(part, u, v, x, y, z, w, h, d, grow));
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                                  float headPitch, float scale, Entity entity) {
        for (AnimatedPart p : this.parts.values()) {
            p.reset();
        }
        EntityArmadillo armadillo = (EntityArmadillo) entity;
        if (armadillo.shouldHideInShell()) {
            this.body.skipDraw = true;
            this.leftHindLeg.showModel = false;
            this.rightHindLeg.showModel = false;
            this.tail.showModel = false;
            this.cube.showModel = true;
        } else {
            this.body.skipDraw = false;
            this.leftHindLeg.showModel = true;
            this.rightHindLeg.showModel = true;
            this.tail.showModel = true;
            this.cube.showModel = false;
            this.head.rotateAngleX = MathHelper.clamp(headPitch, -MAX_UP_HEAD_ROTATION_EXTENT, MAX_DOWN_HEAD_ROTATION_EXTENT)
                    * (float) (Math.PI / 180.0D);
            this.head.rotateAngleY = MathHelper.clamp(netHeadYaw, -EntityArmadillo.MAX_HEAD_ROTATION_EXTENT,
                    EntityArmadillo.MAX_HEAD_ROTATION_EXTENT) * (float) (Math.PI / 180.0D);
        }
        // 1.20's animateWalk: the walk's time from the limb swing, its strength from the swing amount.
        long walkTime = (long) (limbSwing * 50.0F * MAX_WALK_ANIMATION_SPEED);
        float walkScale = Math.min(limbSwingAmount * WALK_ANIMATION_SCALE_FACTOR, 1.0F);
        ArmadilloAnimations.ARMADILLO_WALK.apply(this.parts, walkTime, walkScale);
        animate(armadillo.rollOutAnimation, ArmadilloAnimations.ARMADILLO_ROLL_OUT, ageInTicks);
        animate(armadillo.rollUpAnimation, ArmadilloAnimations.ARMADILLO_ROLL_UP, ageInTicks);
        animate(armadillo.peekAnimation, ArmadilloAnimations.ARMADILLO_PEEK, ageInTicks);
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
        if (this.isChild) {
            GlStateManager.pushMatrix();
            GlStateManager.scale(BABY_SCALE, BABY_SCALE, BABY_SCALE);
            GlStateManager.translate(0.0F, BABY_Y_OFFSET / 16.0F, 0.0F);
            this.root.render(scale);
            GlStateManager.popMatrix();
        } else {
            this.root.render(scale);
        }
    }
}
