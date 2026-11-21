package com.exiledradio.trailsandtalesbackport.sniffer.client;

import com.exiledradio.trailsandtalesbackport.sniffer.AnimationClock;
import com.exiledradio.trailsandtalesbackport.sniffer.EntitySniffer;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;

import java.util.HashMap;
import java.util.Map;

/**
 * 1.20's {@code SnifferModel}: the same parts, cubes, texture offsets (192 x 192) and pivots, with 1.20's
 * keyframe animations (walk or search walk by the limb swing, then digging, long sniff, standing up, happy and
 * scenting by their clocks) and, for a snifflet, the baby head and 1.20's half size 24 pixels down
 * ({@code AgeableHierarchicalModel(0.5, 24)}).
 *
 * <p>Built with {@code snifflet}, 26.1's snifflet instead: 26.3's {@code SniffletModel} parts (128 x 128, for
 * {@code snifflet.png}) drawn at full size with the adult's animations, as 26.3's {@code SnifferRenderer} draws it
 * (a {@code SnifferModel} of the {@code sniffer_baby} layer, so without {@code BABY_TRANSFORM}).
 */
public class ModelSniffer extends ModelBase {

    private static final float WALK_ANIMATION_SPEED_MAX = 9.0F;
    private static final float WALK_ANIMATION_SCALE_FACTOR = 100.0F;
    private static final float YOUNG_SCALE = 0.5F;
    private static final float BODY_Y_OFFSET = 24.0F;

    private final AnimatedPart root;
    private final AnimatedPart head;
    private final Map<String, AnimatedPart> parts = new HashMap<String, AnimatedPart>();
    /** 26.1's snifflet model: no baby scale or baby head. */
    private final boolean snifflet;

    public ModelSniffer() {
        this(false);
    }

    public ModelSniffer(boolean snifflet) {
        this.snifflet = snifflet;
        if (snifflet) {
            // 26.3's SniffletModel.createBodyLayer.
            this.textureWidth = 128;
            this.textureHeight = 128;
            this.root = part("root", null, 0.0F, 0.0F, 0.0F);
            AnimatedPart bone = part("bone", this.root, 0.0F, 24.0F, 0.0F);
            AnimatedPart body = part("body", bone, 6.0F, -3.0F, -9.5F);
            box(body, 0, 35, -13.0F, -14.0F, -0.5F, 14, 14, 20, 0.25F);
            box(body, 0, 0, -13.0F, -14.0F, -0.5F, 14, 15, 20, 0.0F);
            box(body, 68, 0, -13.0F, 0.0F, -0.5F, 14, 0, 20, 0.0F);
            this.head = part("head", body, -6.0F, -4.75F, 0.0F);
            box(this.head, 68, 20, -5.0F, -4.25F, -7.5F, 10, 9, 9, 0.0F);
            box(this.head, 88, 20, -5.0F, 3.75F, -7.5F, 10, 0, 9, 0.0F);
            box(part("left_ear", this.head, 5.0F, -4.25F, -1.5F), 104, 38, 0.0F, 0.0F, -2.0F, 1, 11, 3, 0.0F);
            box(part("right_ear", this.head, -5.0F, -4.25F, -1.5F), 96, 38, -1.0F, 0.0F, -2.0F, 1, 11, 3, 0.0F);
            box(part("nose", this.head, 0.0F, -1.25F, -9.5F), 68, 47, -5.0F, -3.0F, -2.0F, 10, 3, 4, 0.0F);
            box(part("lower_beak", this.head, 0.0F, 1.25F, -9.5F), 68, 38, -5.0F, -2.5F, -2.0F, 10, 5, 4, 0.0F);
            box(part("right_front_leg", bone, -4.0F, -4.0F, -7.0F), 0, 69, -2.0F, -1.0F, -2.0F, 4, 5, 4, 0.0F);
            box(part("right_mid_leg", bone, -4.0F, -4.0F, 0.0F), 0, 78, -2.0F, -1.0F, -2.0F, 4, 5, 4, 0.0F);
            box(part("right_hind_leg", bone, -4.0F, -4.0F, 7.0F), 0, 87, -2.0F, -1.0F, -2.0F, 4, 5, 4, 0.0F);
            box(part("left_front_leg", bone, 4.0F, -4.0F, -7.0F), 16, 69, -2.0F, -1.0F, -2.0F, 4, 5, 4, 0.0F);
            box(part("left_mid_leg", bone, 4.0F, -4.0F, 0.0F), 16, 78, -2.0F, -1.0F, -2.0F, 4, 5, 4, 0.0F);
            box(part("left_hind_leg", bone, 4.0F, -4.0F, 7.0F), 16, 87, -2.0F, -1.0F, -2.0F, 4, 5, 4, 0.0F);
            return;
        }
        this.textureWidth = 192;
        this.textureHeight = 192;
        this.root = part("root", null, 0.0F, 5.0F, 0.0F);
        AnimatedPart bone = part("bone", this.root, 0.0F, 0.0F, 0.0F);
        AnimatedPart body = part("body", bone, 0.0F, 0.0F, 0.0F);
        box(body, 62, 68, -12.5F, -14.0F, -20.0F, 25, 29, 40, 0.0F);
        box(body, 62, 0, -12.5F, -14.0F, -20.0F, 25, 24, 40, 0.5F);
        box(body, 87, 68, -12.5F, 12.0F, -20.0F, 25, 0, 40, 0.0F);
        box(part("right_front_leg", bone, -7.5F, 10.0F, -15.0F), 32, 87, -3.5F, -1.0F, -4.0F, 7, 10, 8, 0.0F);
        box(part("right_mid_leg", bone, -7.5F, 10.0F, 0.0F), 32, 105, -3.5F, -1.0F, -4.0F, 7, 10, 8, 0.0F);
        box(part("right_hind_leg", bone, -7.5F, 10.0F, 15.0F), 32, 123, -3.5F, -1.0F, -4.0F, 7, 10, 8, 0.0F);
        box(part("left_front_leg", bone, 7.5F, 10.0F, -15.0F), 0, 87, -3.5F, -1.0F, -4.0F, 7, 10, 8, 0.0F);
        box(part("left_mid_leg", bone, 7.5F, 10.0F, 0.0F), 0, 105, -3.5F, -1.0F, -4.0F, 7, 10, 8, 0.0F);
        box(part("left_hind_leg", bone, 7.5F, 10.0F, 15.0F), 0, 123, -3.5F, -1.0F, -4.0F, 7, 10, 8, 0.0F);
        this.head = part("head", body, 0.0F, 6.5F, -19.48F);
        box(this.head, 8, 15, -6.5F, -7.5F, -11.5F, 13, 18, 11, 0.0F);
        box(this.head, 8, 4, -6.5F, 7.5F, -11.5F, 13, 0, 11, 0.0F);
        box(part("left_ear", this.head, 6.51F, -7.5F, -4.51F), 2, 0, 0.0F, 0.0F, -3.0F, 1, 19, 7, 0.0F);
        box(part("right_ear", this.head, -6.51F, -7.5F, -4.51F), 48, 0, -1.0F, 0.0F, -3.0F, 1, 19, 7, 0.0F);
        box(part("nose", this.head, 0.0F, -4.5F, -11.5F), 10, 45, -6.5F, -2.0F, -9.0F, 13, 2, 9, 0.0F);
        box(part("lower_beak", this.head, 0.0F, 2.5F, -12.5F), 10, 57, -6.5F, -7.0F, -8.0F, 13, 12, 9, 0.0F);
    }

    private AnimatedPart part(String name, AnimatedPart parent, float x, float y, float z) {
        AnimatedPart p = new AnimatedPart(this, name).pose(x, y, z);
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
        EntitySniffer sniffer = (EntitySniffer) entity;
        this.head.rotateAngleX = headPitch * (float) (Math.PI / 180.0D);
        this.head.rotateAngleY = netHeadYaw * (float) (Math.PI / 180.0D);
        // 1.20's animateWalk: the walk's time from the limb swing, its strength from the swing amount.
        long walkTime = (long) (limbSwing * 50.0F * WALK_ANIMATION_SPEED_MAX);
        float walkScale = Math.min(limbSwingAmount * WALK_ANIMATION_SCALE_FACTOR, 1.0F);
        (sniffer.isSearching() ? SnifferAnimations.SNIFFER_SNIFF_SEARCH : SnifferAnimations.SNIFFER_WALK)
                .apply(this.parts, walkTime, walkScale);
        animate(sniffer.diggingAnimation, SnifferAnimations.SNIFFER_DIG, ageInTicks);
        animate(sniffer.sniffingAnimation, SnifferAnimations.SNIFFER_LONGSNIFF, ageInTicks);
        animate(sniffer.risingAnimation, SnifferAnimations.SNIFFER_STAND_UP, ageInTicks);
        animate(sniffer.feelingHappyAnimation, SnifferAnimations.SNIFFER_HAPPY, ageInTicks);
        animate(sniffer.scentingAnimation, SnifferAnimations.SNIFFER_SNIFFSNIFF, ageInTicks);
        if (this.isChild && !this.snifflet) {
            SnifferAnimations.BABY_TRANSFORM.apply(this.parts, 0L, 1.0F);
        }
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
        if (this.isChild && !this.snifflet) {
            GlStateManager.pushMatrix();
            GlStateManager.scale(YOUNG_SCALE, YOUNG_SCALE, YOUNG_SCALE);
            GlStateManager.translate(0.0F, BODY_Y_OFFSET / 16.0F, 0.0F);
            this.root.render(scale);
            GlStateManager.popMatrix();
        } else {
            this.root.render(scale);
        }
    }
}
