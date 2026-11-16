package com.exiledradio.trailsandtalesbackport.presentation1205.client;

import com.exiledradio.backportcore.presentation.client.WorldBackups;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiConfirmOpenLink;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 1.20.5's {@code RecoverWorldDataScreen} (24w05a), as 1.20.6 has it: "Failed to load world", the folder's issues
 * (the state of level.dat and of level.dat_old: their time and what reading them threw), Report a Bug, Attempt to
 * Restore (only when level.dat fails and level.dat_old reads; otherwise its tooltip says "No state to recover from
 * available") and Back, in one centered column with 8 pixels between rows.
 *
 * <p>Attempt to Restore makes a backup first (Backport Core's {@link WorldBackups#backup}, 1.13's Make Backup) and
 * shows the backup toast, then moves level.dat aside as {@code level.dat_corrupted_<date>} and level.dat_old into its
 * place: "Recovery done" / "Recovery was successful!" (Continue opens the world, Back goes back), or "Failed to recover
 * from previous state.".
 */
@SideOnly(Side.CLIENT)
public class RecoverWorldDataScreen extends GuiScreen implements GuiYesNoCallback {

    /** 1.20.6's bug tracker link. */
    public static final String BUG_TRACKER = "https://aka.ms/snapshotbugs?ref=game";
    /** 1.20.6's {@code WorldSelectionList.DATE_FORMAT}. */
    static final DateTimeFormatter DATE = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault());

    private static final int SPACING = 8;
    private static final int BUG = 0;
    private static final int RESTORE = 1;
    private static final int BACK = 2;

    private final Screens1206.Callback callback;
    public final String folderName;
    private final File folder;
    public final String message;
    public final String issues;
    public final boolean canRestore;
    private List<String> messageLines = Collections.emptyList();
    private List<String> issueLines = Collections.emptyList();
    private int issueWidth;
    private int top;
    private GuiButton restore;

    public RecoverWorldDataScreen(Screens1206.Callback callback, String folderName, File folder) {
        this.callback = callback;
        this.folderName = folderName;
        this.folder = folder;
        this.message = Screens1206.text("recover_world.message", TextFormatting.GRAY + folderName + TextFormatting.RESET);
        Exception level = LevelData.issue(folder, false);
        Exception old = LevelData.issue(folder, true);
        this.issues = info(false, level) + "\n" + info(true, old);
        this.canRestore = level != null && old == null;
    }

    public static String title() {
        return TextFormatting.BOLD + I18n.format("recover_world.title");
    }

    /** 1.20.6's {@code buildInfo}: one file's line. */
    private String info(boolean old, @Nullable Exception issue) {
        if (old && issue instanceof FileNotFoundException) {
            return "";
        }
        Instant time = LevelData.modified(this.folder, old);
        String when = time != null ? DATE.format(time) : I18n.format("recover_world.state_entry.unknown");
        StringBuilder b = new StringBuilder(I18n.format("recover_world.state_entry", TextFormatting.GRAY + when + TextFormatting.RESET));
        if (issue == null) {
            b.append(TextFormatting.GREEN).append(I18n.format("recover_world.issue.none"));
        } else if (issue instanceof FileNotFoundException) {
            b.append(TextFormatting.RED).append(I18n.format("recover_world.issue.missing_file"));
        } else {
            b.append(TextFormatting.RED).append(issue.toString());
        }
        return b.append(TextFormatting.RESET).toString();
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        int max = this.width - 50;
        this.messageLines = Screens1206.wrap(this, this.message, max);
        this.issueLines = Screens1206.wrap(this, this.issues, max);
        this.issueWidth = 0;
        for (String line : this.issueLines) {
            this.issueWidth = Math.max(this.issueWidth, this.fontRenderer.getStringWidth(line));
        }
        int height = Screens1206.LINE + SPACING + this.messageLines.size() * Screens1206.LINE + SPACING
                + this.issueLines.size() * Screens1206.LINE + SPACING + 20 + SPACING + 20;
        this.top = (this.height - height) / 2;
        int rowY = this.top + Screens1206.LINE + SPACING + this.messageLines.size() * Screens1206.LINE + SPACING
                + this.issueLines.size() * Screens1206.LINE + SPACING;
        int rowX = (this.width - (120 + 5 + 120)) / 2;
        this.buttonList.add(new GuiButton(BUG, rowX, rowY, 120, 20, I18n.format("recover_world.bug_tracker")));
        this.restore = new GuiButton(RESTORE, rowX + 125, rowY, 120, 20, I18n.format("recover_world.restore"));
        this.restore.enabled = this.canRestore;
        this.buttonList.add(this.restore);
        this.buttonList.add(new GuiButton(BACK, (this.width - 120) / 2, rowY + 20 + SPACING, 120, 20, I18n.format("gui.back")));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == BUG) {
            this.mc.displayGuiScreen(new GuiConfirmOpenLink(this, BUG_TRACKER, 0, true));
        } else if (button.id == RESTORE) {
            attemptRestore();
        } else if (button.id == BACK) {
            onClose();
        }
    }

    /** The link screen's answer: Open opens the bug tracker (Copy to Clipboard copied it already). */
    @Override
    public void confirmClicked(boolean result, int id) {
        if (result) {
            openLink(BUG_TRACKER);
        }
        this.mc.displayGuiScreen(this);
    }

    private static void openLink(String url) {
        try {
            Class<?> desktop = Class.forName("java.awt.Desktop");
            Object instance = desktop.getMethod("getDesktop").invoke(null);
            desktop.getMethod("browse", URI.class).invoke(instance, new URI(url));
        } catch (Throwable t) {
            TrailsAndTalesBackport.LOGGER.error("Couldn't open link", t);
        }
    }

    public void onClose() {
        this.callback.accept(false);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            onClose();
        }
    }

    /** 1.20.6's {@code attemptRestore}. */
    public void attemptRestore() {
        final Minecraft mc = this.mc;
        Exception level = LevelData.issue(this.folder, false);
        Exception old = LevelData.issue(this.folder, true);
        final String doneTitle = I18n.format("recover_world.done.title");
        if (level != null && old == null) {
            mc.displayGuiScreen(new Screens1206.Message(I18n.format("recover_world.restoring"), () -> {
                backupWithToast(mc, this.folder.getParentFile(), this.folderName);
                if (LevelData.restoreFromOld(this.folder)) {
                    mc.displayGuiScreen(new Screens1206.Confirm(this.callback, doneTitle, Screens1206.text("recover_world.done.success"),
                            I18n.format("gui.continue"), I18n.format("gui.back")));
                } else {
                    mc.displayGuiScreen(new Screens1206.Alert(() -> this.callback.accept(false), doneTitle, Screens1206.text("recover_world.done.failed")));
                }
            }));
        } else {
            TrailsAndTalesBackport.LOGGER.error("Failed to recover world, files not as expected. level.dat: {}, level.dat_old: {}",
                    level != null ? level.getMessage() : "no issues", old != null ? old.getMessage() : "no issues");
            mc.displayGuiScreen(new Screens1206.Alert(() -> this.callback.accept(false), doneTitle, Screens1206.text("recover_world.done.failed")));
        }
    }

    /** 1.20.6's {@code EditWorldScreen.makeBackupAndShowToast}. */
    static boolean backupWithToast(Minecraft mc, File saves, String folderName) {
        try {
            long size = WorldBackups.backup(saves, folderName, WorldBackups.backupsDir());
            mc.getToastGui().add(new SystemToast1205(SystemToast1205.Id.WORLD_BACKUP, I18n.format("selectWorld.edit.backupCreated", folderName),
                    I18n.format("selectWorld.edit.backupSize", (int) Math.ceil(size / 1048576.0))));
            return true;
        } catch (IOException e) {
            mc.getToastGui().add(new SystemToast1205(SystemToast1205.Id.WORLD_BACKUP, I18n.format("selectWorld.edit.backupFailed"),
                    String.valueOf(e.getMessage())));
            return false;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        int y = this.top;
        this.drawCenteredString(this.fontRenderer, title(), this.width / 2, y, 0xFFFFFF);
        y += Screens1206.LINE + SPACING;
        Screens1206.drawCentered(this, this.messageLines, y);
        y += this.messageLines.size() * Screens1206.LINE + SPACING;
        int x = (this.width - this.issueWidth) / 2;
        for (String line : this.issueLines) {
            this.drawString(this.fontRenderer, line, x, y, 0xFFFFFF);
            y += Screens1206.LINE;
        }
        if (!this.canRestore && this.restore != null && mouseX >= this.restore.x && mouseY >= this.restore.y
                && mouseX < this.restore.x + this.restore.width && mouseY < this.restore.y + this.restore.height) {
            List<String> tip = new ArrayList<String>(this.fontRenderer.listFormattedStringToWidth(I18n.format("recover_world.no_fallback"), 170));
            this.drawHoveringText(tip, mouseX, mouseY);
        }
    }
}
