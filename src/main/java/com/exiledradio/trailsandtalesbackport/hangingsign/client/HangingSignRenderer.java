package com.exiledradio.trailsandtalesbackport.hangingsign.client;

import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.hangingsign.BlockHangingSign;
import com.exiledradio.trailsandtalesbackport.hangingsign.TileHangingSign;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiUtilRenderComponents;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Draws hanging signs with 1.20's model ({@code HangingSignRenderer}): a 14x10x2 board, two pairs of
 * crossed chains (or one V of chains when attached), and for wall signs a 16x2x4 plank bracket; the wood's
 * {@code entity/signs/hanging/<wood>.png} (64x32). Text: four lines 9 apart, 0.9 of a sign's scale, up to
 * 60 pixels wide, in the board's middle.
 */
public class HangingSignRenderer extends TileEntitySpecialRenderer<TileHangingSign> {

    private final Model model = new Model();
    private final Map<com.exiledradio.trailsandtalesbackport.hangingsign.HangingWood, ResourceLocation> textures =
            new EnumMap<com.exiledradio.trailsandtalesbackport.hangingsign.HangingWood, ResourceLocation>(
                    com.exiledradio.trailsandtalesbackport.hangingsign.HangingWood.class);

    @Override
    public void render(TileHangingSign te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        Block block = te.getBlockType();
        if (!(block instanceof BlockHangingSign)) {
            return;
        }
        boolean wall = block instanceof BlockHangingSign.Wall;
        float yaw;
        if (wall) {
            IBlockState state = te.getWorld().getBlockState(te.getPos());
            EnumFacing f = state.getBlock() == block ? state.getValue(BlockHangingSign.Wall.FACING) : EnumFacing.NORTH;
            yaw = f.getHorizontalAngle();
        } else {
            yaw = te.getBlockMetadata() * 360 / 16.0F;
        }
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x + 0.5F, (float) y + 0.9375F, (float) z + 0.5F);
        GlStateManager.rotate(-yaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.translate(0.0F, -0.3125F, 0.0F);

        this.model.plank.showModel = wall;
        this.model.normalChains.showModel = wall || !te.attached();
        this.model.vChains.showModel = !wall && te.attached();

        if (destroyStage >= 0) {
            bindTexture(DESTROY_STAGES[destroyStage]);
            GlStateManager.matrixMode(5890);
            GlStateManager.pushMatrix();
            GlStateManager.scale(4.0F, 2.0F, 1.0F);
            GlStateManager.translate(0.0625F, 0.0625F, 0.0625F);
            GlStateManager.matrixMode(5888);
        } else {
            bindTexture(texture(((BlockHangingSign) block).wood()));
        }
        GlStateManager.enableRescaleNormal();
        GlStateManager.pushMatrix();
        GlStateManager.scale(1.0F, -1.0F, -1.0F);
        this.model.render();
        GlStateManager.popMatrix();

        if (destroyStage < 0) {
            FontRenderer font = getFontRenderer();
            float scale = 0.015625F * 0.9F;
            GlStateManager.translate(0.0F, -0.32F, 0.073F);
            GlStateManager.scale(scale, -scale, scale);
            GlStateManager.glNormal3f(0.0F, 0.0F, -scale);
            GlStateManager.depthMask(false);
            for (int line = 0; line < te.signText.length; line++) {
                if (te.signText[line] == null) {
                    continue;
                }
                List<ITextComponent> split = GuiUtilRenderComponents.splitText(te.signText[line], 60, font, false, true);
                String s = split != null && !split.isEmpty() ? split.get(0).getFormattedText() : "";
                if (line == te.lineBeingEdited) {
                    s = "> " + s + " <";
                }
                font.drawString(s, -font.getStringWidth(s) / 2, line * 9 - 18, 0);
            }
            GlStateManager.depthMask(true);
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
        if (destroyStage >= 0) {
            GlStateManager.matrixMode(5890);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(5888);
        }
    }

    private ResourceLocation texture(com.exiledradio.trailsandtalesbackport.hangingsign.HangingWood wood) {
        ResourceLocation t = this.textures.get(wood);
        if (t == null) {
            t = new ResourceLocation(TrailsAndTalesBackport.MOD_ID, "textures/entity/signs/hanging/" + wood.name + ".png");
            this.textures.put(wood, t);
        }
        return t;
    }

    /** 1.20's hanging sign layer, in 1.12.2's model parts. */
    static final class Model extends ModelBase {
        final ModelRenderer board;
        final ModelRenderer plank;
        final ModelRenderer normalChains;
        final ModelRenderer vChains;

        Model() {
            this.textureWidth = 64;
            this.textureHeight = 32;
            this.board = new ModelRenderer(this, 0, 12);
            this.board.addBox(-7.0F, 0.0F, -1.0F, 14, 10, 2);
            this.plank = new ModelRenderer(this, 0, 0);
            this.plank.addBox(-8.0F, -6.0F, -2.0F, 16, 2, 4);
            this.normalChains = new ModelRenderer(this, 0, 0);
            this.normalChains.addChild(chain(0, 6, -5.0F, (float) -Math.PI / 4));
            this.normalChains.addChild(chain(6, 6, -5.0F, (float) Math.PI / 4));
            this.normalChains.addChild(chain(0, 6, 5.0F, (float) -Math.PI / 4));
            this.normalChains.addChild(chain(6, 6, 5.0F, (float) Math.PI / 4));
            this.vChains = new ModelRenderer(this, 14, 6);
            this.vChains.addBox(-6.0F, -6.0F, 0.0F, 12, 6, 0);
        }

        private ModelRenderer chain(int u, int v, float x, float yaw) {
            ModelRenderer r = new ModelRenderer(this, u, v);
            r.addBox(-1.5F, 0.0F, 0.0F, 3, 6, 0);
            r.setRotationPoint(x, -6.0F, 0.0F);
            r.rotateAngleY = yaw;
            return r;
        }

        void render() {
            float s = 0.0625F;
            this.board.render(s);
            this.plank.render(s);
            this.normalChains.render(s);
            this.vChains.render(s);
        }
    }
}
