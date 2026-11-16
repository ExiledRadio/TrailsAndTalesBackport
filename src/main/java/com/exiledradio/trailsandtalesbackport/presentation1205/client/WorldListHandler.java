package com.exiledradio.trailsandtalesbackport.presentation1205.client;

import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.presentation1205.Presentation1205;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiListWorldSelection;
import net.minecraft.client.gui.GuiListWorldSelectionEntry;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiWorldSelection;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.WorldSummary;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The World Selection screen as 1.20.6's checks need it: every entry 1.12.2 built becomes a {@link WorldEntry}, which
 * opens through {@link WorldOpening}; with the recovery screen on, a world whose level.dat is missing or unreadable
 * is listed as 1.20.6's {@code CorruptedLevelSummary} (also when 1.12.2 left it out, or listed it from
 * level.dat_old), sorted by level.dat's time as 1.20.6 sorts it. For a corrupted world the Play Selected World button
 * reads "Attempt to Recover", and Edit and Re-Create are off. Entries of another mod's own class are left as they are.
 */
@SideOnly(Side.CLIENT)
public final class WorldListHandler {

    private static Field selectionList;
    private static Field entries;
    private static Field entrySummary;

    private static Field field(Class<?> owner, String mcp, String srg) {
        return ReflectionHelper.findField(owner, mcp, srg);
    }

    static boolean active() {
        return Presentation1205.recoverWorld() || Presentation1205.lowDiskSpace();
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.getGui() instanceof GuiWorldSelection && active()) {
            try {
                rebuild((GuiWorldSelection) event.getGui());
            } catch (Exception e) {
                TrailsAndTalesBackport.LOGGER.error("World list: could not add 1.20.5's checks; worlds open as in 1.12.2", e);
            }
        }
    }

    @SubscribeEvent
    public void onDraw(GuiScreenEvent.DrawScreenEvent.Pre event) {
        if (event.getGui() instanceof GuiWorldSelection && active()) {
            buttons((GuiWorldSelection) event.getGui());
        }
    }

    static GuiListWorldSelection list(GuiWorldSelection screen) throws Exception {
        if (selectionList == null) {
            selectionList = field(GuiWorldSelection.class, "selectionList", "field_184866_u");
        }
        return (GuiListWorldSelection) selectionList.get(screen);
    }

    @SuppressWarnings("unchecked")
    static List<GuiListWorldSelectionEntry> entries(GuiListWorldSelection list) throws Exception {
        if (entries == null) {
            entries = field(GuiListWorldSelection.class, "entries", "field_186799_w");
        }
        return (List<GuiListWorldSelectionEntry>) entries.get(list);
    }

    static WorldSummary summary(GuiListWorldSelectionEntry entry) throws Exception {
        if (entrySummary == null) {
            entrySummary = field(GuiListWorldSelectionEntry.class, "worldSummary", "field_186786_g");
        }
        return (WorldSummary) entrySummary.get(entry);
    }

    static File savesDir() {
        return new File(Minecraft.getMinecraft().gameDir, "saves");
    }

    /** A new entry for this folder: its icon texture is the old entry's, so that one is released first. */
    static WorldEntry entry(GuiListWorldSelection list, WorldSummary summary, boolean corrupted) {
        Minecraft.getMinecraft().getTextureManager().deleteTexture(new ResourceLocation("worlds/" + summary.getFileName() + "/icon"));
        return new WorldEntry(list, summary, corrupted);
    }

    static void rebuild(GuiWorldSelection screen) throws Exception {
        GuiListWorldSelection list = list(screen);
        if (list == null) {
            return;
        }
        List<GuiListWorldSelectionEntry> current = entries(list);
        boolean recover = Presentation1205.recoverWorld();
        File saves = savesDir();
        List<GuiListWorldSelectionEntry> out = new ArrayList<GuiListWorldSelectionEntry>();
        Set<String> seen = new HashSet<String>();
        for (GuiListWorldSelectionEntry e : current) {
            WorldSummary s = summary(e);
            seen.add(s.getFileName());
            if (e.getClass() != GuiListWorldSelectionEntry.class) {
                out.add(e);
                continue;
            }
            File folder = new File(saves, s.getFileName());
            if (recover && LevelData.cachedIssue(folder) != null) {
                out.add(entry(list, WorldEntry.corruptedSummary(s.getFileName(), LevelData.lastPlayed(folder)), true));
            } else {
                out.add(entry(list, s, false));
            }
        }
        if (recover) {
            // Folders 1.12.2 left out because neither file reads; 1.20.6 lists every folder with either file.
            File[] folders = saves.listFiles();
            if (folders != null) {
                for (File folder : folders) {
                    if (folder.isDirectory() && !seen.contains(folder.getName()) && LevelData.listed(folder)
                            && LevelData.cachedIssue(folder) != null) {
                        out.add(entry(list, WorldEntry.corruptedSummary(folder.getName(), LevelData.lastPlayed(folder)), true));
                    }
                }
            }
            // 1.20.6's LevelSummary order: last played first, then by folder.
            final List<WorldSummary> keys = new ArrayList<WorldSummary>();
            for (GuiListWorldSelectionEntry e : out) {
                keys.add(summary(e));
            }
            List<Integer> order = new ArrayList<Integer>();
            for (int i = 0; i < out.size(); i++) {
                order.add(i);
            }
            Collections.sort(order, new Comparator<Integer>() {
                @Override
                public int compare(Integer a, Integer b) {
                    WorldSummary x = keys.get(a);
                    WorldSummary y = keys.get(b);
                    if (x.getLastTimePlayed() != y.getLastTimePlayed()) {
                        return x.getLastTimePlayed() < y.getLastTimePlayed() ? 1 : -1;
                    }
                    return x.getFileName().compareTo(y.getFileName());
                }
            });
            List<GuiListWorldSelectionEntry> sorted = new ArrayList<GuiListWorldSelectionEntry>();
            for (int i : order) {
                sorted.add(out.get(i));
            }
            out = sorted;
        }
        current.clear();
        current.addAll(out);
    }

    /** Play Selected World / Edit / Re-Create for the selected entry (ids 1, 4 and 5 of GuiWorldSelection). */
    static void buttons(GuiWorldSelection screen) {
        GuiListWorldSelectionEntry selected;
        try {
            GuiListWorldSelection list = list(screen);
            selected = list == null ? null : list.getSelectedWorld();
        } catch (Exception e) {
            return;
        }
        boolean corrupted = selected instanceof WorldEntry && ((WorldEntry) selected).corrupted;
        for (GuiButton b : buttonList(screen)) {
            if (b.id == 1) {
                b.displayString = I18n.format(corrupted ? "recover_world.button" : "selectWorld.select");
            } else if ((b.id == 4 || b.id == 5) && corrupted) {
                b.enabled = false;
            }
        }
    }

    private static Field buttonList;

    @SuppressWarnings("unchecked")
    static List<GuiButton> buttonList(GuiScreen screen) {
        try {
            if (buttonList == null) {
                buttonList = field(GuiScreen.class, "buttonList", "field_146292_n");
            }
            return (List<GuiButton>) buttonList.get(screen);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
