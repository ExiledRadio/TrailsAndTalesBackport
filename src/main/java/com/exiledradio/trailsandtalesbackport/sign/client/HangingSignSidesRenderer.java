package com.exiledradio.trailsandtalesbackport.sign.client;

import com.exiledradio.trailsandtalesbackport.hangingsign.TileHangingSign;
import com.exiledradio.trailsandtalesbackport.hangingsign.client.HangingSignRenderer;

import net.minecraft.client.renderer.GlStateManager;

/**
 * Stream H's hanging sign renderer with both faces' text, colors and glow ({@link SignTextRenderer}); the
 * back turned half a turn about the board's middle, as 1.20 draws it.
 */
public class HangingSignSidesRenderer extends HangingSignRenderer {

    @Override
    protected void renderText(TileHangingSign te) {
        for (int face = 0; face < 2; face++) {
            GlStateManager.pushMatrix();
            if (face == 1) {
                GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
            }
            textOrigin();
            GlStateManager.depthMask(false);
            SignTextRenderer.draw(getFontRenderer(), te, face == 0, LINE_HEIGHT, MAX_WIDTH);
            GlStateManager.depthMask(true);
            GlStateManager.popMatrix();
        }
    }
}
