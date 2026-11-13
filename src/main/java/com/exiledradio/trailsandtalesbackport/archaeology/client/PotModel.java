package com.exiledradio.trailsandtalesbackport.archaeology.client;

import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.PositionTextureVertex;
import net.minecraft.client.model.TexturedQuad;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * The decorated pot's model, 1.20's {@code DecoratedPotRenderer} layers part for part: the neck, top and
 * bottom on the 32x32 base texture, and four sides on 16x16 pattern textures, each side a single
 * outward face.
 */
final class PotModel {

    private static final float PI = (float) Math.PI;
    private static final ResourceLocation BASE = tex("decorated_pot_base");
    private static final ResourceLocation SIDE = tex("decorated_pot_side");
    private static final Map<Item, ResourceLocation> PATTERNS = new HashMap<Item, ResourceLocation>();

    final ModelRenderer neck;
    final ModelRenderer top;
    final ModelRenderer bottom;
    /** Back, left, right, front, as the faces are stored. */
    final ModelRenderer[] sides = new ModelRenderer[4];

    PotModel() {
        ModelBase base = new ModelBase() {
        };
        base.textureWidth = 32;
        base.textureHeight = 32;
        this.neck = new ModelRenderer(base, 0, 0);
        this.neck.addBox(4.0F, 17.0F, 4.0F, 8, 3, 8, -0.1F);
        this.neck.setTextureOffset(0, 5);
        this.neck.addBox(5.0F, 20.0F, 5.0F, 6, 1, 6, 0.2F);
        this.neck.setRotationPoint(0.0F, 37.0F, 16.0F);
        this.neck.rotateAngleX = PI;
        this.top = new ModelRenderer(base, -14, 13);
        this.top.addBox(0.0F, 0.0F, 0.0F, 14, 0, 14);
        this.top.setRotationPoint(1.0F, 16.0F, 1.0F);
        this.bottom = new ModelRenderer(base, -14, 13);
        this.bottom.addBox(0.0F, 0.0F, 0.0F, 14, 0, 14);
        this.bottom.setRotationPoint(1.0F, 0.0F, 1.0F);

        ModelBase sideBase = new ModelBase() {
        };
        sideBase.textureWidth = 16;
        sideBase.textureHeight = 16;
        this.sides[0] = side(sideBase, 15.0F, 16.0F, 1.0F, 0.0F, 0.0F, PI);
        this.sides[1] = side(sideBase, 1.0F, 16.0F, 1.0F, 0.0F, -PI / 2.0F, PI);
        this.sides[2] = side(sideBase, 15.0F, 16.0F, 15.0F, 0.0F, PI / 2.0F, PI);
        this.sides[3] = side(sideBase, 1.0F, 16.0F, 15.0F, PI, 0.0F, 0.0F);
    }

    private static ModelRenderer side(ModelBase base, float x, float y, float z, float rx, float ry, float rz) {
        ModelRenderer r = new ModelRenderer(base, 1, 0);
        r.cubeList.add(new NorthFace(r, 1, 0, 14, 16));
        r.setRotationPoint(x, y, z);
        r.rotateAngleX = rx;
        r.rotateAngleY = ry;
        r.rotateAngleZ = rz;
        return r;
    }

    /** Draws the whole pot in block space (0-1) with these faces; the caller has set up the transform. */
    void render(Item[] sherds) {
        float scale = 1.0F / 16.0F;
        Minecraft mc = Minecraft.getMinecraft();
        mc.getTextureManager().bindTexture(BASE);
        this.neck.render(scale);
        this.top.render(scale);
        this.bottom.render(scale);
        for (int i = 0; i < 4; i++) {
            mc.getTextureManager().bindTexture(pattern(sherds[i]));
            this.sides[i].render(scale);
        }
    }

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(TrailsAndTalesBackport.MOD_ID, "textures/entity/decorated_pot/" + name + ".png");
    }

    /**
     * The pattern for a face: a sherd {@code <ns>:<x>_pottery_sherd} draws
     * {@code <ns>:textures/entity/decorated_pot/<x>_pottery_pattern.png} when that texture exists (so another
     * mod's sherds bring their own); a brick, or a sherd without one, the plain side.
     */
    static ResourceLocation pattern(Item item) {
        ResourceLocation cached = PATTERNS.get(item);
        if (cached != null) {
            return cached;
        }
        ResourceLocation out = SIDE;
        ResourceLocation id = item.getRegistryName();
        if (item != Items.BRICK && id != null && id.getPath().endsWith("_pottery_sherd")) {
            String name = id.getPath().substring(0, id.getPath().length() - "_sherd".length()) + "_pattern";
            ResourceLocation candidate = new ResourceLocation(id.getNamespace(),
                    "textures/entity/decorated_pot/" + name + ".png");
            try {
                Minecraft.getMinecraft().getResourceManager().getResource(candidate).close();
                out = candidate;
            } catch (Exception missing) {
                out = SIDE;
            }
        }
        PATTERNS.put(item, out);
        return out;
    }

    /** A zero-depth box that draws only its north face, as 1.20's {@code EnumSet.of(Direction.NORTH)}. */
    private static final class NorthFace extends ModelBox {
        private final TexturedQuad quad;

        NorthFace(ModelRenderer renderer, int texU, int texV, int dx, int dy) {
            super(renderer, texU, texV, 0.0F, 0.0F, 0.0F, dx, dy, 0, 0.0F);
            PositionTextureVertex v0 = new PositionTextureVertex(0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
            PositionTextureVertex v1 = new PositionTextureVertex(dx, 0.0F, 0.0F, 0.0F, 8.0F);
            PositionTextureVertex v2 = new PositionTextureVertex(dx, dy, 0.0F, 8.0F, 8.0F);
            PositionTextureVertex v3 = new PositionTextureVertex(0.0F, dy, 0.0F, 8.0F, 0.0F);
            this.quad = new TexturedQuad(new PositionTextureVertex[]{v1, v0, v3, v2}, texU, texV, texU + dx, texV + dy,
                    renderer.textureWidth, renderer.textureHeight);
        }

        @Override
        public void render(BufferBuilder buffer, float scale) {
            this.quad.draw(buffer, scale);
        }
    }
}
