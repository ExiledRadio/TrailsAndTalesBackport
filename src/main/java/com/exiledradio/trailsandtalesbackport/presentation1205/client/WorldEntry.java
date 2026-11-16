package com.exiledradio.trailsandtalesbackport.presentation1205.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiListWorldSelection;
import net.minecraft.client.gui.GuiListWorldSelectionEntry;
import net.minecraft.client.gui.GuiWorldSelection;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.GameType;
import net.minecraft.world.storage.WorldInfo;
import net.minecraft.world.storage.WorldSummary;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A world of the World Selection list, opened through 1.20.6's checks ({@link WorldOpening}) before 1.12.2's own
 * ({@link #joinVanilla}).
 *
 * <p>A corrupted entry is 1.20.6's {@code CorruptedLevelSummary}: a folder whose level.dat is missing or cannot be
 * read. It shows the folder's name, the folder and level.dat's time, and "Failed to load world summary" in red, and it
 * opens the Failed to Load World screen; Edit and Re-Create are off for it ({@link WorldListHandler}).
 */
@SideOnly(Side.CLIENT)
public final class WorldEntry extends GuiListWorldSelectionEntry {

    private static final ResourceLocation ICON_MISSING = new ResourceLocation("textures/misc/unknown_server.png");
    private static final ResourceLocation ICON_OVERLAY = new ResourceLocation("textures/gui/world_selection.png");
    /** 1.12.2's world list date format, as its other entries show it. */
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat();
    private static Field iconField;

    final GuiListWorldSelection list;
    final WorldSummary summary;
    public final boolean corrupted;
    private final ResourceLocation iconLocation;

    public WorldEntry(GuiListWorldSelection list, WorldSummary summary, boolean corrupted) {
        super(list, summary, Minecraft.getMinecraft().getSaveLoader());
        this.list = list;
        this.summary = summary;
        this.corrupted = corrupted;
        this.iconLocation = new ResourceLocation("worlds/" + summary.getFileName() + "/icon");
    }

    public String folder() {
        return this.summary.getFileName();
    }

    public GuiWorldSelection screen() {
        return this.list.getGuiWorldSelection();
    }

    /** A corrupted folder's summary: its name, level.dat's time, nothing else known. */
    static WorldSummary corruptedSummary(String folder, final long lastPlayed) {
        WorldInfo info = new WorldInfo() {
            @Override
            public long getLastTimePlayed() {
                return lastPlayed;
            }

            @Override
            public GameType getGameType() {
                return GameType.NOT_SET;
            }
        };
        return new WorldSummary(info, folder, folder, 0L, false);
    }

    @Override
    public void joinWorld() {
        WorldOpening.open(this);
    }

    /** 1.12.2's own opening: the version question, Forge's checks, the integrated server. */
    void joinVanilla() {
        super.joinWorld();
    }

    @Override
    public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected, float partialTicks) {
        if (!this.corrupted) {
            super.drawEntry(slotIndex, x, y, listWidth, slotHeight, mouseX, mouseY, isSelected, partialTicks);
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        String name = this.summary.getFileName();
        long last = this.summary.getLastTimePlayed();
        String line2 = last != -1L ? name + " (" + DATE_FORMAT.format(new Date(last)) + ")" : name;
        mc.fontRenderer.drawString(name, x + 32 + 3, y + 1, 0xFFFFFF);
        mc.fontRenderer.drawString(line2, x + 32 + 3, y + mc.fontRenderer.FONT_HEIGHT + 3, 0x808080);
        mc.fontRenderer.drawString(TextFormatting.RED + I18n.format("recover_world.warning"), x + 32 + 3,
                y + mc.fontRenderer.FONT_HEIGHT + mc.fontRenderer.FONT_HEIGHT + 3, 0x808080);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(hasIcon() ? this.iconLocation : ICON_MISSING);
        GlStateManager.enableBlend();
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0.0F, 0.0F, 32, 32, 32.0F, 32.0F);
        GlStateManager.disableBlend();
        if (mc.gameSettings.touchscreen || isSelected) {
            mc.getTextureManager().bindTexture(ICON_OVERLAY);
            Gui.drawRect(x, y, x + 32, y + 32, -1601138544);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            Gui.drawModalRectWithCustomSizedTexture(x, y, 0.0F, mouseX - x < 32 ? 32.0F : 0.0F, 32, 32, 256.0F, 256.0F);
        }
    }

    private boolean hasIcon() {
        try {
            if (iconField == null) {
                iconField = ReflectionHelper.findField(GuiListWorldSelectionEntry.class, "icon", "field_186790_k");
            }
            return iconField.get(this) != null;
        } catch (Exception e) {
            return false;
        }
    }
}
