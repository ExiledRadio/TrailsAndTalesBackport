package com.exiledradio.trailsandtalesbackport.wolf.client;

import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelWolf;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

/**
 * 1.20.5's {@code ModelLayers.WOLF_ARMOR}: the wolf model with every cube inflated by 0.2. It is 1.12.2's wolf
 * model (the one the wolf under it is drawn with, so the two move as one) with its cubes rebuilt inflated.
 */
public class ModelWolfArmor extends ModelWolf {

    private static final float INFLATE = 0.2F;

    public ModelWolfArmor() {
        ModelRenderer tail = ReflectionHelper.getPrivateValue(ModelWolf.class, this, "wolfTail", "field_78180_g");
        ModelRenderer mane = ReflectionHelper.getPrivateValue(ModelWolf.class, this, "wolfMane", "field_78186_h");
        box(this.wolfHeadMain, 0, 0, -2.0F, -3.0F, -2.0F, 6, 6, 4);
        this.wolfHeadMain.setTextureOffset(16, 14).addBox(-2.0F, -5.0F, 0.0F, 2, 2, 1, INFLATE);
        this.wolfHeadMain.setTextureOffset(16, 14).addBox(2.0F, -5.0F, 0.0F, 2, 2, 1, INFLATE);
        this.wolfHeadMain.setTextureOffset(0, 10).addBox(-0.5F, 0.0F, -5.0F, 3, 3, 4, INFLATE);
        box(this.wolfBody, 18, 14, -3.0F, -2.0F, -3.0F, 6, 9, 6);
        box(mane, 21, 0, -3.0F, -3.0F, -3.0F, 8, 6, 7);
        for (ModelRenderer leg : new ModelRenderer[] {this.wolfLeg1, this.wolfLeg2, this.wolfLeg3, this.wolfLeg4}) {
            box(leg, 0, 18, 0.0F, 0.0F, -1.0F, 2, 8, 2);
        }
        box(tail, 9, 18, 0.0F, 0.0F, -1.0F, 2, 8, 2);
    }

    private static void box(ModelRenderer part, int u, int v, float x, float y, float z, int w, int h, int d) {
        part.cubeList.clear();
        part.setTextureOffset(u, v).addBox(x, y, z, w, h, d, INFLATE);
    }
}
