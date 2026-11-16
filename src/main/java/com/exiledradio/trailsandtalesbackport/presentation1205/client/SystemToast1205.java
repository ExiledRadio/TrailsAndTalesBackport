package com.exiledradio.trailsandtalesbackport.presentation1205.client;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.toasts.GuiToast;
import net.minecraft.client.gui.toasts.IToast;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 1.20.6's {@code SystemToast} for this stream's toasts: as wide as its text (at least 160), the background drawn in
 * rows from the 160 by 32 system toast, yellow title (or the title's own color) and white message, shown 5 seconds (10
 * for low disk space), and {@code addOrUpdate} restarting a shown toast with new text.
 *
 * <p>1.12.2's toast list slides every toast in and out by 160 pixels; a wider toast moves itself the rest of the way,
 * so it slides by its own width as 1.20.6's {@code ToastComponent} does. 1.12.2 has no Notification Time option, so the
 * time is 1.20.6's default.
 */
@SideOnly(Side.CLIENT)
public final class SystemToast1205 implements IToast {

    /** 1.20.6's {@code SystemToast.SystemToastId}s this stream shows. */
    public enum Id {
        WORLD_BACKUP(5000L),
        LOW_DISK_SPACE(10000L),
        CHUNK_LOAD_FAILURE(5000L),
        CHUNK_SAVE_FAILURE(5000L);

        final long displayTime;

        Id(long displayTime) {
            this.displayTime = displayTime;
        }
    }

    private static final int SLIDE = 600;

    private final Id id;
    private String title;
    private List<String> messageLines;
    private final int width;
    private long lastChanged;
    private boolean changed;
    private long shownAt = -1L;
    private long hiddenAt = -1L;
    private boolean hiding;

    public SystemToast1205(Id id, String title, @Nullable String message) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        this.id = id;
        this.title = title;
        this.messageLines = lines(message);
        this.width = Math.max(160, 30 + Math.max(font.getStringWidth(title), message == null ? 0 : font.getStringWidth(message)));
    }

    private static List<String> lines(@Nullable String message) {
        return message == null ? ImmutableList.<String>of() : ImmutableList.of(message);
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return 20 + Math.max(this.messageLines.size(), 1) * 12;
    }

    public String title() {
        return this.title;
    }

    @Nullable
    public String message() {
        return this.messageLines.isEmpty() ? null : this.messageLines.get(0);
    }

    @Override
    public Visibility draw(GuiToast gui, long delta) {
        long now = Minecraft.getSystemTime();
        if (this.shownAt < 0L) {
            this.shownAt = now;
        }
        if (this.changed) {
            this.lastChanged = delta;
            this.changed = false;
        }
        // GuiToast's own slide (ToastInstance.getVisibility), so the extra offset follows it.
        float f = MathHelper.clamp((float) (now - (this.hiding ? this.hiddenAt : this.shownAt)) / SLIDE, 0.0F, 1.0F);
        float shown = this.hiding ? 1.0F - f * f : f * f;
        GlStateManager.translate(-(this.width - 160) * shown, 0.0F, 0.0F);

        gui.getMinecraft().getTextureManager().bindTexture(TEXTURE_TOASTS);
        GlStateManager.color(1.0F, 1.0F, 1.0F);
        int w = this.width;
        int h = this.height();
        if (w == 160 && this.messageLines.size() <= 1) {
            gui.drawTexturedModalRect(0, 0, 0, 64, 160, h);
        } else {
            int bottom = Math.min(4, h - 28);
            row(gui, w, 0, 0, 28);
            for (int y = 28; y < h - bottom; y += 10) {
                row(gui, w, 16, y, Math.min(16, h - y - bottom));
            }
            row(gui, w, 32 - bottom, h - bottom, bottom);
        }

        FontRenderer font = gui.getMinecraft().fontRenderer;
        if (this.messageLines.isEmpty()) {
            font.drawString(this.title, 18, 12, -256);
        } else {
            font.drawString(this.title, 18, 7, -256);
            for (int i = 0; i < this.messageLines.size(); i++) {
                font.drawString(this.messageLines.get(i), 18, 18 + i * 12, -1);
            }
        }

        Visibility visibility = delta - this.lastChanged < this.id.displayTime ? Visibility.SHOW : Visibility.HIDE;
        boolean hide = visibility == Visibility.HIDE;
        if (hide != this.hiding) {
            // GuiToast restarts its animation this frame: animationTime = now - (1 - visibility) * 600.
            long start = now - (long) (int) ((1.0F - shown) * SLIDE);
            if (hide) {
                this.hiddenAt = start;
            } else {
                this.shownAt = start;
            }
            this.hiding = hide;
        }
        return visibility;
    }

    /** 1.20.6's {@code renderBackgroundRow}: a row of the 160 by 32 sprite at v, stretched to w by 64-wide tiles. */
    private static void row(GuiToast gui, int w, int v, int y, int h) {
        int left = v == 0 ? 20 : 5;
        int right = Math.min(60, w - left);
        gui.drawTexturedModalRect(0, y, 0, 64 + v, left, h);
        for (int x = left; x < w - right; x += 64) {
            gui.drawTexturedModalRect(x, y, 32, 64 + v, Math.min(64, w - x - right), h);
        }
        gui.drawTexturedModalRect(w - right, y, 160 - right, 64 + v, right, h);
    }

    public void reset(String title, @Nullable String message) {
        this.title = title;
        this.messageLines = lines(message);
        this.changed = true;
    }

    @Override
    public Id getType() {
        return this.id;
    }

    /** 1.20.6's {@code SystemToast.addOrUpdate}. */
    public static void addOrUpdate(GuiToast toasts, Id id, String title, @Nullable String message) {
        SystemToast1205 toast = toasts.getToast(SystemToast1205.class, id);
        if (toast == null) {
            toasts.add(new SystemToast1205(id, title, message));
        } else {
            toast.reset(title, message);
        }
    }
}
