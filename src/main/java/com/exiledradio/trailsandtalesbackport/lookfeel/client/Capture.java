package com.exiledradio.trailsandtalesbackport.lookfeel.client;

import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;

/** Saves what the bound framebuffer holds as a PNG: the client self-test's look at screens it cannot see. */
@SideOnly(Side.CLIENT)
final class Capture {

    private Capture() {
    }

    static void save(Minecraft mc, File file) {
        int w = mc.displayWidth;
        int h = mc.displayHeight;
        try {
            ByteBuffer pixels = BufferUtils.createByteBuffer(w * h * 4);
            GlStateManager.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
            GL11.glReadPixels(0, 0, w, h, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
            BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int i = ((h - 1 - y) * w + x) * 4;
                    image.setRGB(x, y, (pixels.get(i) & 255) << 16 | (pixels.get(i + 1) & 255) << 8 | pixels.get(i + 2) & 255);
                }
            }
            file.getParentFile().mkdirs();
            ImageIO.write(image, "png", file);
        } catch (Exception e) {
            TrailsAndTalesBackport.LOGGER.warn("Look and feel: capture to {} failed ({})", file, e.toString());
        }
    }
}
