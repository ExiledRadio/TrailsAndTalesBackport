package com.exiledradio.trailsandtalesbackport.presentation1205.client;

import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.presentation1205.DiskSpace;
import com.exiledradio.trailsandtalesbackport.presentation1205.Presentation1205;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.storage.WorldSummary;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.File;

/**
 * 1.20.6's {@code WorldOpenFlows.openWorld} for a world of the list, the parts 1.12.2 lacks, in 1.20.6's order:
 * <ol>
 *   <li>{@code openWorldLoadLevelData}: level.dat must read, or the Failed to Load World screen shows; after a
 *       successful restore, Continue opens the world again from the start;</li>
 *   <li>{@code openWorldCheckDiskSpace}: under 64 MB free, "Warning! Low disk space!" with Continue and Back;</li>
 * </ol>
 * then 1.12.2's own opening (its version question, Forge's checks). Back returns to a reloaded world list.
 */
@SideOnly(Side.CLIENT)
public final class WorldOpening {

    private WorldOpening() {
    }

    public static void open(WorldEntry entry) {
        Minecraft mc = Minecraft.getMinecraft();
        final File folder = new File(WorldListHandler.savesDir(), entry.folder());
        if (Presentation1205.recoverWorld() && LevelData.issue(folder, false) != null) {
            mc.displayGuiScreen(new RecoverWorldDataScreen(yes -> {
                if (yes) {
                    reopen(entry);
                } else {
                    back(entry);
                }
            }, entry.folder(), folder));
            return;
        }
        if (Presentation1205.lowDiskSpace() && DiskSpace.isLow(folder)) {
            mc.displayGuiScreen(new Screens1206.Confirm(yes -> {
                if (yes) {
                    entry.joinVanilla();
                } else {
                    back(entry);
                }
            }, lowDiskTitle(), Screens1206.text("selectWorld.warning.lowDiskSpace.description"),
                    I18n.format("gui.continue"), I18n.format("gui.back")));
            return;
        }
        entry.joinVanilla();
    }

    public static String lowDiskTitle() {
        return TextFormatting.RED + I18n.format("selectWorld.warning.lowDiskSpace.title");
    }

    static void back(WorldEntry entry) {
        Minecraft.getMinecraft().displayGuiScreen(entry.screen());
    }

    /** After a restore: the world as 1.12.2 now reads it, opened from the start. */
    private static void reopen(WorldEntry entry) {
        Minecraft mc = Minecraft.getMinecraft();
        WorldSummary found = null;
        try {
            mc.getSaveLoader().flushCache();
            for (WorldSummary s : mc.getSaveLoader().getSaveList()) {
                if (s.getFileName().equals(entry.folder())) {
                    found = s;
                }
            }
        } catch (Exception e) {
            TrailsAndTalesBackport.LOGGER.error("Couldn't load level list", e);
        }
        if (found == null) {
            back(entry);
            return;
        }
        open(WorldListHandler.entry(entry.list, found, false));
    }
}
