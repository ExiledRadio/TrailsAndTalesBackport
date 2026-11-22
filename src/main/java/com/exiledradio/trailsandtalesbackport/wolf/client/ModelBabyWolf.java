package com.exiledradio.trailsandtalesbackport.wolf.client;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.util.math.MathHelper;

/**
 * 26.1's {@code BabyWolfModel} (26.3's {@code createBodyLayer}, 32 x 32, for the {@code *_baby} textures) with
 * {@code WolfModel.setupAnim}: tail wag, sitting, the begging head tilt and shaking dry. Drawn at full size: 1.12.2's
 * child shrinking is not applied, as 26.3 draws its baby models.
 *
 * <p>The tail's raise comes from the renderer's {@code handleRotationFloat} (1.20's tail angle, as for the adult),
 * the walk from 1.12.2's limb swing, which 1.12.2 already triples for children as 26.3 does.
 */
public class ModelBabyWolf extends ModelBase {

    /** 26.3's {@code getAgeScale()} for a baby, which the sitting pose scales by. */
    private static final float AGE_SCALE = 0.5F;
    private static final float RAD = (float) (Math.PI / 180.0D);

    final ModelRenderer head;
    final ModelRenderer body;
    final ModelRenderer rightHindLeg;
    final ModelRenderer leftHindLeg;
    final ModelRenderer rightFrontLeg;
    final ModelRenderer leftFrontLeg;
    final ModelRenderer tail;
    private final ModelRenderer[] roots;
    private float partialTicks;

    public ModelBabyWolf() {
        this.textureWidth = 32;
        this.textureHeight = 32;
        this.head = part(0.0F, 18.25F, -4.0F, 0.0F);
        box(this.head, 0, 12, -2.99F, -3.25F, -3.0F, 6, 5, 5, 0.025F);
        box(this.head, 17, 12, -1.5F, -0.24F, -5.0F, 3, 2, 2, 0.0F);
        ModelRenderer rightEar = part(-2.0F, -4.25F, -0.5F, 0.0F);
        box(rightEar, 0, 5, -1.0F, -1.0F, -0.5F, 2, 2, 1, 0.0F);
        this.head.addChild(rightEar);
        ModelRenderer leftEar = part(2.0F, -4.25F, -0.5F, 0.0F);
        box(leftEar, 20, 5, -1.0F, -1.0F, -0.5F, 2, 2, 1, 0.0F);
        this.head.addChild(leftEar);
        this.body = part(0.0F, 19.0F, 0.0F, 0.0F);
        box(this.body, 0, 0, -3.0F, -2.0F, -4.0F, 6, 4, 8, 0.0F);
        this.rightHindLeg = part(-1.5F, 21.0F, 3.0F, 0.0F);
        box(this.rightHindLeg, 0, 22, -1.0F, 0.0F, -1.0F, 2, 3, 2, 0.0F);
        this.leftHindLeg = part(1.5F, 21.0F, 3.0F, 0.0F);
        box(this.leftHindLeg, 8, 22, -1.0F, 0.0F, -1.0F, 2, 3, 2, 0.0F);
        this.rightFrontLeg = part(-1.5F, 21.0F, -3.0F, 0.0F);
        box(this.rightFrontLeg, 0, 0, -1.0F, 0.0F, -1.0F, 2, 3, 2, 0.0F);
        this.leftFrontLeg = part(1.5F, 21.0F, -3.0F, 0.0F);
        box(this.leftFrontLeg, 20, 0, -1.0F, 0.0F, -1.0F, 2, 3, 2, 0.0F);
        this.tail = part(0.0F, 19.0F, 3.0F, -0.5236F);
        ModelRenderer tailCube = part(0.0F, -0.6F, 0.2F, -3.1F);
        box(tailCube, 22, 16, -1.0F, -5.7F, -1.0F, 2, 6, 2, 0.0F);
        this.tail.addChild(tailCube);
        this.roots = new ModelRenderer[] {this.head, this.body, this.rightHindLeg, this.leftHindLeg, this.rightFrontLeg,
            this.leftFrontLeg, this.tail};
    }

    private ModelRenderer part(float x, float y, float z, float xRot) {
        ModelRenderer part = new ModelRenderer(this);
        part.setRotationPoint(x, y, z);
        part.rotateAngleX = xRot;
        return part;
    }

    private static void box(ModelRenderer part, int u, int v, float x, float y, float z, int w, int h, int d, float grow) {
        part.cubeList.add(new ModelBox(part, u, v, x, y, z, w, h, d, grow));
    }

    @Override
    public void setLivingAnimations(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks) {
        this.partialTicks = partialTicks;
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                       float headPitch, float scale) {
        setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        for (ModelRenderer part : this.roots) {
            part.render(scale);
        }
    }

    /** 26.3's {@code Model.resetPose} then {@code WolfModel.setupAnim} with {@code BabyWolfModel}'s overrides. */
    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                                  float headPitch, float scale, Entity entity) {
        reset(this.head, 0.0F, 18.25F, -4.0F, 0.0F);
        reset(this.body, 0.0F, 19.0F, 0.0F, 0.0F);
        reset(this.rightHindLeg, -1.5F, 21.0F, 3.0F, 0.0F);
        reset(this.leftHindLeg, 1.5F, 21.0F, 3.0F, 0.0F);
        reset(this.rightFrontLeg, -1.5F, 21.0F, -3.0F, 0.0F);
        reset(this.leftFrontLeg, 1.5F, 21.0F, -3.0F, 0.0F);
        reset(this.tail, 0.0F, 19.0F, 3.0F, -0.5236F);
        if (!(entity instanceof EntityWolf)) {
            return;
        }
        EntityWolf wolf = (EntityWolf) entity;
        float pos = limbSwing;
        float speed = limbSwingAmount;
        this.tail.rotateAngleY = wolf.isAngry() ? 0.0F : MathHelper.cos(pos * 0.6662F) * 1.4F * speed;
        if (wolf.isSitting()) {
            sit();
        } else {
            this.rightHindLeg.rotateAngleX = MathHelper.cos(pos * 0.6662F) * 1.4F * speed;
            this.leftHindLeg.rotateAngleX = MathHelper.cos(pos * 0.6662F + (float) Math.PI) * 1.4F * speed;
            this.rightFrontLeg.rotateAngleX = MathHelper.cos(pos * 0.6662F + (float) Math.PI) * 1.4F * speed;
            this.leftFrontLeg.rotateAngleX = MathHelper.cos(pos * 0.6662F) * 1.4F * speed;
        }
        // shakeOffWater: WolfModel's body roll, then BabyWolfModel's head and tail roll.
        this.body.rotateAngleZ = wolf.getShakeAngle(this.partialTicks, -0.16F);
        this.head.rotateAngleZ = wolf.getInterestedAngle(this.partialTicks) + wolf.getShakeAngle(this.partialTicks, 0.0F);
        this.tail.rotateAngleZ = wolf.getShakeAngle(this.partialTicks, -0.2F);
        this.head.rotateAngleX = headPitch * RAD;
        this.head.rotateAngleY = netHeadYaw * RAD;
        this.tail.rotateAngleX = ageInTicks;
    }

    private static void reset(ModelRenderer part, float x, float y, float z, float xRot) {
        part.setRotationPoint(x, y, z);
        part.rotateAngleX = xRot;
        part.rotateAngleY = 0.0F;
        part.rotateAngleZ = 0.0F;
    }

    /** {@code WolfModel.setSittingPose} at a baby's age scale, then {@code BabyWolfModel}'s one radian less body tilt. */
    private void sit() {
        this.body.rotationPointY += 4.0F * AGE_SCALE;
        this.body.rotationPointZ -= 2.0F * AGE_SCALE;
        this.body.rotateAngleX = (float) (Math.PI / 4);
        this.tail.rotationPointY += 9.0F * AGE_SCALE;
        this.tail.rotationPointZ -= 2.0F * AGE_SCALE;
        this.rightHindLeg.rotationPointY += 6.7F * AGE_SCALE;
        this.rightHindLeg.rotationPointZ -= 5.0F * AGE_SCALE;
        this.rightHindLeg.rotateAngleX = (float) (Math.PI * 3.0 / 2.0);
        this.leftHindLeg.rotationPointY += 6.7F * AGE_SCALE;
        this.leftHindLeg.rotationPointZ -= 5.0F * AGE_SCALE;
        this.leftHindLeg.rotateAngleX = (float) (Math.PI * 3.0 / 2.0);
        this.rightFrontLeg.rotateAngleX = 5.811947F;
        this.rightFrontLeg.rotationPointX += 0.01F * AGE_SCALE;
        this.rightFrontLeg.rotationPointY += 1.0F * AGE_SCALE;
        this.leftFrontLeg.rotateAngleX = 5.811947F;
        this.leftFrontLeg.rotationPointX -= 0.01F * AGE_SCALE;
        this.leftFrontLeg.rotationPointY += 1.0F * AGE_SCALE;
        this.body.rotateAngleX--;
    }
}
