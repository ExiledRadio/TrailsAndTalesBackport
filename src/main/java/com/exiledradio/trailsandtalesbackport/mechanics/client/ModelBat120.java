package com.exiledradio.trailsandtalesbackport.mechanics.client;

import com.exiledradio.trailsandtalesbackport.armadillo.AnimationClock;
import com.exiledradio.trailsandtalesbackport.armadillo.client.AnimatedPart;
import com.exiledradio.trailsandtalesbackport.armadillo.client.KeyframeAnimation;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBox;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityBat;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 1.20.3's {@code BatModel} (23w43a, reshaped to Bedrock's in 23w44a): the same parts, cubes, texture offsets (32 x 32)
 * and pivots, posed by 1.20.3's keyframe animations, flying or resting. A resting bat's head follows its look
 * ({@code applyHeadRotation}); the animations turn it and the body upside down.
 *
 * <p>1.20.3's {@code Bat.setupAnimationStates} switches the two animations' clocks each tick by
 * {@code isResting}; 1.12.2's bat has no clocks, so this keeps them per bat on the client.
 */
public class ModelBat120 extends ModelBase {

    /** A bat's two animation clocks, 1.20.3's {@code flyAnimationState} and {@code restAnimationState}. */
    static final class Clocks {
        final AnimationClock fly = new AnimationClock();
        final AnimationClock rest = new AnimationClock();
    }

    private static final Map<EntityBat, Clocks> CLOCKS = new WeakHashMap<EntityBat, Clocks>();

    private final AnimatedPart body;
    private final AnimatedPart head;
    private final Map<String, AnimatedPart> parts = new HashMap<String, AnimatedPart>();

    public ModelBat120() {
        this.textureWidth = 32;
        this.textureHeight = 32;
        this.body = part("body", null, 0.0F, 17.0F, 0.0F);
        box(this.body, 0, 0, -1.5F, 0.0F, -1.0F, 3, 5, 2);
        this.head = part("head", null, 0.0F, 17.0F, 0.0F);
        box(this.head, 0, 7, -2.0F, -3.0F, -1.0F, 4, 3, 2);
        box(part("right_ear", this.head, -1.5F, -2.0F, 0.0F), 1, 15, -2.5F, -4.0F, 0.0F, 3, 5, 0);
        box(part("left_ear", this.head, 1.1F, -3.0F, 0.0F), 8, 15, -0.1F, -3.0F, 0.0F, 3, 5, 0);
        AnimatedPart rightWing = part("right_wing", this.body, -1.5F, 0.0F, 0.0F);
        box(rightWing, 12, 0, -2.0F, -2.0F, 0.0F, 2, 7, 0);
        box(part("right_wing_tip", rightWing, -2.0F, 0.0F, 0.0F), 16, 0, -6.0F, -2.0F, 0.0F, 6, 8, 0);
        AnimatedPart leftWing = part("left_wing", this.body, 1.5F, 0.0F, 0.0F);
        box(leftWing, 12, 7, 0.0F, -2.0F, 0.0F, 2, 7, 0);
        box(part("left_wing_tip", leftWing, 2.0F, 0.0F, 0.0F), 16, 8, 0.0F, -2.0F, 0.0F, 6, 8, 0);
        box(part("feet", this.body, 0.0F, 5.0F, 0.0F), 16, 16, -1.5F, 0.0F, 0.0F, 3, 2, 0);
    }

    private AnimatedPart part(String name, AnimatedPart parent, float x, float y, float z) {
        AnimatedPart p = new AnimatedPart(this, name).pose(x, y, z, 0.0F, 0.0F, 0.0F);
        if (parent != null) {
            parent.addChild(p);
        }
        this.parts.put(name, p);
        return p;
    }

    private static void box(AnimatedPart part, int u, int v, float x, float y, float z, int w, int h, int d) {
        part.cubeList.add(new ModelBox(part, u, v, x, y, z, w, h, d, 0.0F));
    }

    static Clocks clocks(EntityBat bat) {
        Clocks clocks = CLOCKS.get(bat);
        if (clocks == null) {
            clocks = new Clocks();
            CLOCKS.put(bat, clocks);
        }
        return clocks;
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                                  float headPitch, float scale, Entity entity) {
        for (AnimatedPart p : this.parts.values()) {
            p.reset();
        }
        EntityBat bat = (EntityBat) entity;
        Clocks clocks = clocks(bat);
        boolean resting = bat.getIsBatHanging();
        if (resting) {
            clocks.fly.stop();
            clocks.rest.startIfStopped(bat.ticksExisted);
            this.head.rotateAngleY = netHeadYaw * (float) (Math.PI / 180.0D);
        } else {
            clocks.rest.stop();
            clocks.fly.startIfStopped(bat.ticksExisted);
        }
        animate(clocks.fly, BatAnimations.BAT_FLYING, ageInTicks);
        animate(clocks.rest, BatAnimations.BAT_RESTING, ageInTicks);
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
        this.body.render(scale);
        this.head.render(scale);
    }
}
