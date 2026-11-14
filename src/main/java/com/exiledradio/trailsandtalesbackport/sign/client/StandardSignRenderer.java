package com.exiledradio.trailsandtalesbackport.sign.client;

import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.common.Owners;
import com.exiledradio.trailsandtalesbackport.common.wood.WoodKind;
import com.exiledradio.trailsandtalesbackport.common.wood.WoodSign;

import net.minecraft.block.Block;
import net.minecraft.block.BlockStandingSign;
import net.minecraft.client.model.ModelSign;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Standing and wall signs with both faces, in place of vanilla's renderer, this mod's wood sign renderer
 * and Village & Pillage's: vanilla's model and placement, the wood's texture, and each face's text
 * ({@link SignTextRenderer}) - the back turned half a turn about the board's middle, as 1.20's
 * {@code SignRenderer.translateSignText}.
 *
 * <p>Textures: vanilla's {@code textures/entity/sign.png}; this mod's woods' {@code entity/signs/<wood>};
 * Village & Pillage's five woods' {@code villagerbackport:textures/entity/signs/<wood>.png}, the wood
 * read from the block's name ({@code spruce_sign}, {@code spruce_wall_sign}). A sign block of any other mod
 * that uses vanilla's tile draws with vanilla's texture, as vanilla's renderer drew it.
 */
public class StandardSignRenderer extends TileEntitySpecialRenderer<TileEntitySign> {

    private static final ResourceLocation VANILLA = new ResourceLocation("textures/entity/sign.png");
    private static final Map<Block, ResourceLocation> TEXTURES = new HashMap<Block, ResourceLocation>();

    private final ModelSign model = new ModelSign();

    @Override
    public void render(TileEntitySign te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        Block block = te.getBlockType();
        boolean standing = block instanceof BlockStandingSign;
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x + 0.5F, (float) y + 0.5F, (float) z + 0.5F);
        if (standing) {
            GlStateManager.rotate(-(te.getBlockMetadata() * 360) / 16.0F, 0.0F, 1.0F, 0.0F);
            this.model.signStick.showModel = true;
        } else {
            int facing = te.getBlockMetadata();
            float angle = facing == 2 ? 180.0F : facing == 4 ? 90.0F : facing == 5 ? -90.0F : 0.0F;
            GlStateManager.rotate(-angle, 0.0F, 1.0F, 0.0F);
            GlStateManager.translate(0.0F, -0.3125F, -0.4375F);
            this.model.signStick.showModel = false;
        }

        if (destroyStage >= 0) {
            bindTexture(DESTROY_STAGES[destroyStage]);
            GlStateManager.matrixMode(5890);
            GlStateManager.pushMatrix();
            GlStateManager.scale(4.0F, 2.0F, 1.0F);
            GlStateManager.translate(0.0625F, 0.0625F, 0.0625F);
            GlStateManager.matrixMode(5888);
        } else {
            bindTexture(texture(block));
        }

        GlStateManager.enableRescaleNormal();
        GlStateManager.pushMatrix();
        GlStateManager.scale(0.6666667F, -0.6666667F, -0.6666667F);
        this.model.renderSign();
        GlStateManager.popMatrix();

        if (destroyStage < 0) {
            for (int face = 0; face < 2; face++) {
                GlStateManager.pushMatrix();
                if (face == 1) {
                    GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
                }
                GlStateManager.translate(0.0F, 0.33333334F, 0.046666667F);
                GlStateManager.scale(0.010416667F, -0.010416667F, 0.010416667F);
                GlStateManager.glNormal3f(0.0F, 0.0F, -0.010416667F);
                GlStateManager.depthMask(false);
                SignTextRenderer.draw(getFontRenderer(), te, face == 0, 10, 90);
                GlStateManager.depthMask(true);
                GlStateManager.popMatrix();
            }
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();

        if (destroyStage >= 0) {
            GlStateManager.matrixMode(5890);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(5888);
        }
    }

    /** The board texture for a sign block (also the edit screen's). */
    public static ResourceLocation texture(Block block) {
        ResourceLocation t = TEXTURES.get(block);
        if (t == null) {
            t = VANILLA;
            WoodKind kind = WoodSign.kindOf(block);
            ResourceLocation id = block.getRegistryName();
            if (kind != null) {
                t = new ResourceLocation(TrailsAndTalesBackport.MOD_ID, "textures/entity/signs/" + kind.name + ".png");
            } else if (id != null && Owners.VILLAGE_AND_PILLAGE.equals(id.getNamespace())) {
                String path = id.getPath();
                String wood = path.endsWith("_wall_sign") ? path.substring(0, path.length() - "_wall_sign".length())
                        : path.endsWith("_sign") ? path.substring(0, path.length() - "_sign".length()) : null;
                if (wood != null) {
                    t = new ResourceLocation(Owners.VILLAGE_AND_PILLAGE, "textures/entity/signs/" + wood + ".png");
                }
            }
            TEXTURES.put(block, t);
        }
        return t;
    }
}
