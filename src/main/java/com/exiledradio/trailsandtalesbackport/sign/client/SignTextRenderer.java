package com.exiledradio.trailsandtalesbackport.sign.client;

import com.exiledradio.trailsandtalesbackport.sign.SignSides;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiUtilRenderComponents;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.Entity;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;

import java.util.List;

/**
 * Draws one face's four lines in the world, as 1.20's {@code SignRenderer.renderSignText}: the face's
 * color at 40% ({@code getDarkColor}); glowing text in the full color at full brightness, with an outline
 * in the dark color (black glowing text: a cream outline) when the camera is within 16 blocks, or always
 * for black.
 *
 * <p>Called with the matrix already at the face's text origin and scale.
 */
public final class SignTextRenderer {

    /** 1.20's {@code SignRenderer.OUTLINE_RENDER_DISTANCE}: 16 blocks, squared. */
    private static final double OUTLINE_DISTANCE_SQ = 16.0D * 16.0D;

    private SignTextRenderer() {
    }

    public static void draw(FontRenderer font, TileEntitySign sign, boolean front, int lineHeight, int maxWidth) {
        ITextComponent[] lines = SignSides.lines(sign, front);
        EnumDyeColor color = SignSides.color(sign, front);
        boolean glowing = SignSides.glowing(sign, front);
        int dark = SignSides.darkColor(color, glowing);
        int textColor;
        boolean outline;
        float lastX = OpenGlHelper.lastBrightnessX;
        float lastY = OpenGlHelper.lastBrightnessY;
        if (glowing) {
            textColor = SignSides.textColor(color);
            outline = outlineVisible(sign.getPos(), textColor);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
        } else {
            textColor = dark;
            outline = false;
        }
        int top = lines.length * lineHeight / 2;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i] == null) {
                continue;
            }
            List<ITextComponent> split = GuiUtilRenderComponents.splitText(lines[i], maxWidth, font, false, true);
            String s = split != null && !split.isEmpty() ? split.get(0).getFormattedText() : "";
            if (front && i == sign.lineBeingEdited) {
                // Vanilla's own editor, if some other screen still draws through it.
                s = "> " + s + " <";
            }
            int x = -font.getStringWidth(s) / 2;
            int y = i * lineHeight - top;
            if (outline) {
                String plain = TextFormatting.getTextWithoutFormattingCodes(s);
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        if (dx != 0 || dy != 0) {
                            font.drawString(plain, x + dx, y + dy, dark);
                        }
                    }
                }
            }
            font.drawString(s, x, y, textColor);
        }
        if (glowing) {
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastX, lastY);
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** 1.20's {@code isOutlineVisible}. */
    static boolean outlineVisible(BlockPos pos, int textColor) {
        if (textColor == SignSides.textColor(EnumDyeColor.BLACK)) {
            return true;
        }
        Entity camera = Minecraft.getMinecraft().getRenderViewEntity();
        return camera != null && camera.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) < OUTLINE_DISTANCE_SQ;
    }
}
