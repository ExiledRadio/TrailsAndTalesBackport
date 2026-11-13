package com.exiledradio.trailsandtalesbackport.camel.client;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.BufferBuilder;

/**
 * A model part as 1.20's {@code ModelPart} has it: a pose it returns to each frame, and a scale, which
 * 1.12.2's {@link ModelRenderer} lacks. Drawn the same way - moved to its pivot, turned Z then Y then X,
 * then scaled - with its cubes in one display list and its children inside its transform.
 */
public final class AnimatedPart extends ModelRenderer {

    public float xScale = 1.0F;
    public float yScale = 1.0F;
    public float zScale = 1.0F;

    private float initialX;
    private float initialY;
    private float initialZ;
    private int list = -1;

    public AnimatedPart(ModelBase model, String name) {
        super(model, name);
    }

    /** Sets the part's pivot and remembers it as the pose to reset to. */
    public AnimatedPart pose(float x, float y, float z) {
        setRotationPoint(x, y, z);
        this.initialX = x;
        this.initialY = y;
        this.initialZ = z;
        return this;
    }

    /** 1.20's {@code resetPose}. */
    public void reset() {
        this.rotationPointX = this.initialX;
        this.rotationPointY = this.initialY;
        this.rotationPointZ = this.initialZ;
        this.rotateAngleX = 0.0F;
        this.rotateAngleY = 0.0F;
        this.rotateAngleZ = 0.0F;
        this.xScale = 1.0F;
        this.yScale = 1.0F;
        this.zScale = 1.0F;
    }

    @Override
    public void render(float scale) {
        if (this.isHidden || !this.showModel) {
            return;
        }
        if (this.list < 0) {
            this.list = GLAllocation.generateDisplayLists(1);
            GlStateManager.glNewList(this.list, 4864);
            BufferBuilder buffer = Tessellator.getInstance().getBuffer();
            for (ModelBox box : this.cubeList) {
                box.render(buffer, scale);
            }
            GlStateManager.glEndList();
        }
        GlStateManager.pushMatrix();
        GlStateManager.translate(this.rotationPointX * scale, this.rotationPointY * scale, this.rotationPointZ * scale);
        float deg = (float) (180.0D / Math.PI);
        if (this.rotateAngleZ != 0.0F) {
            GlStateManager.rotate(this.rotateAngleZ * deg, 0.0F, 0.0F, 1.0F);
        }
        if (this.rotateAngleY != 0.0F) {
            GlStateManager.rotate(this.rotateAngleY * deg, 0.0F, 1.0F, 0.0F);
        }
        if (this.rotateAngleX != 0.0F) {
            GlStateManager.rotate(this.rotateAngleX * deg, 1.0F, 0.0F, 0.0F);
        }
        if (this.xScale != 1.0F || this.yScale != 1.0F || this.zScale != 1.0F) {
            GlStateManager.scale(this.xScale, this.yScale, this.zScale);
        }
        GlStateManager.callList(this.list);
        if (this.childModels != null) {
            for (ModelRenderer child : this.childModels) {
                child.render(scale);
            }
        }
        GlStateManager.popMatrix();
    }
}
