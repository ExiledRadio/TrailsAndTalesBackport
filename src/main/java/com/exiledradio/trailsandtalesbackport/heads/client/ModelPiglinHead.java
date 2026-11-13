package com.exiledradio.trailsandtalesbackport.heads.client;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

/**
 * 1.20's {@code PiglinHeadModel}: the piglin's head from {@code PiglinModel.addHead} (64 x 64 texture), with
 * the ears flapping by the animation value.
 */
public class ModelPiglinHead extends ModelBase {

    private final ModelRenderer head;
    private final ModelRenderer leftEar;
    private final ModelRenderer rightEar;

    public ModelPiglinHead() {
        this.textureWidth = 64;
        this.textureHeight = 64;
        this.head = new ModelRenderer(this);
        this.head.setTextureOffset(0, 0).addBox(-5.0F, -8.0F, -4.0F, 10, 8, 8);
        this.head.setTextureOffset(31, 1).addBox(-2.0F, -4.0F, -5.0F, 4, 4, 1);
        this.head.setTextureOffset(2, 4).addBox(2.0F, -2.0F, -5.0F, 1, 2, 1);
        this.head.setTextureOffset(2, 0).addBox(-3.0F, -2.0F, -5.0F, 1, 2, 1);
        this.leftEar = new ModelRenderer(this);
        this.leftEar.setRotationPoint(4.5F, -6.0F, 0.0F);
        this.leftEar.setTextureOffset(51, 6).addBox(0.0F, 0.0F, -2.0F, 1, 5, 4);
        this.head.addChild(this.leftEar);
        this.rightEar = new ModelRenderer(this);
        this.rightEar.setRotationPoint(-4.5F, -6.0F, 0.0F);
        this.rightEar.setTextureOffset(39, 6).addBox(-1.0F, 0.0F, -2.0F, 1, 5, 4);
        this.head.addChild(this.rightEar);
    }

    /** 1.20's {@code setupAnim(animation, yRot, xRot)}, degrees in. */
    public void setupAnim(float animation, float yRot, float xRot) {
        this.head.rotateAngleY = yRot * ((float) Math.PI / 180.0F);
        this.head.rotateAngleX = xRot * ((float) Math.PI / 180.0F);
        this.leftEar.rotateAngleZ = (float) (-(Math.cos(animation * (float) Math.PI * 0.2F * 1.2F) + 2.5D)) * 0.2F;
        this.rightEar.rotateAngleZ = (float) (Math.cos(animation * (float) Math.PI * 0.2F) + 2.5D) * 0.2F;
    }

    public float leftEarAngle() {
        return this.leftEar.rotateAngleZ;
    }

    public void render(float scale) {
        this.head.render(scale);
    }
}
