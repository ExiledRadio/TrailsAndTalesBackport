package com.exiledradio.trailsandtalesbackport.selftest;

import com.exiledradio.backportcore.presentation.client.WorldBackups;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.presentation1205.ChunkFailures;
import com.exiledradio.trailsandtalesbackport.presentation1205.DiskSpace;
import com.exiledradio.trailsandtalesbackport.presentation1205.LowDiskSpaceTicker;
import com.exiledradio.trailsandtalesbackport.presentation1205.client.LevelData;
import com.exiledradio.trailsandtalesbackport.presentation1205.client.Presentation1205Client;
import com.exiledradio.trailsandtalesbackport.presentation1205.client.RecoverWorldDataScreen;
import com.exiledradio.trailsandtalesbackport.presentation1205.client.Screens1206;
import com.exiledradio.trailsandtalesbackport.presentation1205.client.SystemToast1205;
import com.exiledradio.trailsandtalesbackport.presentation1205.client.ViossaPack;
import com.exiledradio.trailsandtalesbackport.presentation1205.client.WorldEntry;
import com.exiledradio.trailsandtalesbackport.presentation1205.client.WorldOpening;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiListWorldSelection;
import net.minecraft.client.gui.GuiListWorldSelectionEntry;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiWorldSelection;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.Language;
import net.minecraft.client.resources.LanguageManager;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * {@code runClient -Pselftest=screens1205}: stream presentation1205 on a real client, without touching the mouse or
 * keyboard. At the title screen: Viossa (in the list, its text, English fallback) and the three toasts (saved to
 * {@code eclipse/presentation1205/toasts.png}). In a new world: a real chunk save failure on the integrated server
 * becomes the toast, and pausing with too little disk space shows the low disk space toast. Back at the title screen:
 * the world list with a world whose level.dat is broken and one whose level.dat_old is broken too, the Failed to Load
 * World screen for both, a restore, and the low disk space warning when opening a world. Logged as "CLIENT SELFTEST
 * DONE pass= fail=".
 */
public final class ClientPresentation1205Test {

    private static final String WORLD = "p1205-world";
    private static final String BROKEN = "p1205-broken";
    private static final String LOST = "p1205-lost";

    private final Deque<Object[]> steps = new ArrayDeque<Object[]>();
    private int wait;
    private int idle;
    private int pass;
    private int fail;
    private boolean started;
    private boolean done;
    private File shots;

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new ClientPresentation1205Test());
        TrailsAndTalesBackport.LOGGER.info("CLIENT SELFTEST registered (presentation1205)");
    }

    private void ok(boolean condition, String what) {
        if (condition) {
            this.pass++;
            TrailsAndTalesBackport.LOGGER.info("CLIENT SELFTEST ok {}", what);
        } else {
            this.fail++;
            TrailsAndTalesBackport.LOGGER.error("CLIENT SELFTEST FAIL {}", what);
        }
    }

    private interface Step {
        /** @return false to run again next tick (waiting for something) */
        boolean run(Minecraft mc) throws Exception;
    }

    /** Runs a step after this many ticks. */
    private void then(int ticks, Step step) {
        this.steps.add(new Object[] {ticks, step});
    }

    private void shot(Minecraft mc, String name) {
        ScreenShotHelper.saveScreenshot(this.shots, name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        File f = new File(new File(this.shots, "screenshots"), name);
        ok(f.isFile() && f.length() > 1000, "saved " + f.getName());
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || this.done) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (!this.started) {
                if (mc.currentScreen instanceof GuiMainMenu && ++this.idle >= 100) {
                    this.started = true;
                    // A test window in the background would pause the game by itself.
                    mc.gameSettings.pauseOnLostFocus = false;
                    this.shots = new File(mc.gameDir, "presentation1205");
                    FileUtils.deleteQuietly(this.shots);
                    this.shots.mkdirs();
                    script();
                }
                return;
            }
            if (this.wait > 0) {
                this.wait--;
                return;
            }
            Object[] next = this.steps.peek();
            if (next == null) {
                finish(mc);
                return;
            }
            if (((Step) next[1]).run(mc)) {
                this.steps.poll();
                Object[] after = this.steps.peek();
                this.wait = after == null ? 0 : (Integer) after[0];
                this.idle = 0;
            } else if (++this.idle > 2400) {
                ok(false, "a step waited two minutes: " + this.steps.size() + " left");
                finish(mc);
            }
        } catch (Throwable t) {
            TrailsAndTalesBackport.LOGGER.error("CLIENT SELFTEST threw", t);
            this.fail++;
            finish(mc);
        }
    }

    private void finish(Minecraft mc) {
        this.done = true;
        DiskSpace.testUsableSpace = null;
        TrailsAndTalesBackport.LOGGER.info("CLIENT SELFTEST DONE pass={} fail={}", this.pass, this.fail);
        mc.shutdown();
    }

    private static File saves(Minecraft mc) {
        return new File(mc.gameDir, "saves");
    }

    private void script() {
        then(0, this::viossa);
        then(0, this::toasts);
        then(40, mc -> {
            shot(mc, "toasts.png");
            SystemToast1205.clear(mc);
            FileUtils.deleteQuietly(new File(saves(mc), WORLD));
            // The integrated server would start the server self-tests for this property, and stop itself.
            System.clearProperty(TrailsAndTalesBackport.MOD_ID + ".selftest");
            mc.launchIntegratedServer(WORLD, WORLD, new WorldSettings(5L, GameType.CREATIVE, true, false, WorldType.DEFAULT).enableCommands());
            return true;
        });
        then(0, mc -> mc.world != null && mc.player != null && mc.currentScreen == null);
        then(60, this::chunkFailureInGame);
        then(0, mc -> SystemToast1205.find(mc, SystemToast1205.Id.CHUNK_SAVE_FAILURE) != null || this.idle > 200);
        then(0, mc -> {
            com.exiledradio.trailsandtalesbackport.presentation1205.client.SystemToastView t = SystemToast1205.find(mc, SystemToast1205.Id.CHUNK_SAVE_FAILURE);
            ok(t != null && t.title().equals("§cFailed to save chunk at [31000, 31000]") && "See log for more details".equals(t.message()),
                    "the integrated server's chunk save failure shows 1.20.5's toast: " + (t == null ? null : t.title() + " / " + t.message()));
            ok(ChunkFailures.lastReport != null && ChunkFailures.lastReport.equals("save [31000, 31000]"), "reported " + ChunkFailures.lastReport);
            SystemToast1205.clear(mc);
            mc.displayGuiScreen(null);
            return true;
        });
        then(10, mc -> {
            ok(!mc.isGamePaused(), "the game runs");
            // Pausing saves everything, which ends with the disk check.
            DiskSpace.testUsableSpace = 1L << 20;
            this.checksBefore = LowDiskSpaceTicker.checks;
            mc.displayInGameMenu();
            return true;
        });
        then(20, mc -> {
            ok(mc.isGamePaused(), "the game is paused");
            ok(LowDiskSpaceTicker.checks > this.checksBefore, "pausing looked at the disk: " + this.checksBefore + " -> " + LowDiskSpaceTicker.checks);
            com.exiledradio.trailsandtalesbackport.presentation1205.client.SystemToastView t = SystemToast1205.find(mc, SystemToast1205.Id.LOW_DISK_SPACE);
            ok(t != null && "Low disk space!".equals(t.title()) && "Might not be able to save the world.".equals(t.message()),
                    "with 1 MB free, pausing shows the low disk space toast");
            return true;
        });
        then(30, mc -> {
            shot(mc, "lowdisk-toast.png");
            SystemToast1205.clear(mc);
            mc.displayGuiScreen(null);
            return true;
        });
        then(20, mc -> {
            // The autosave cadence: 6000 ticks of the game running (1.12.2's singleplayer has no /save-all).
            this.checksBefore = LowDiskSpaceTicker.checks;
            this.ticker.advance(LowDiskSpaceTicker.AUTOSAVE_INTERVAL - 1);
            return true;
        });
        then(5, mc -> {
            ok(LowDiskSpaceTicker.checks == this.checksBefore, "no check a tick before the autosave");
            mc.getIntegratedServer().addScheduledTask(() -> this.ticker.advance(1));
            return true;
        });
        then(5, mc -> {
            ok(LowDiskSpaceTicker.checks == this.checksBefore + 1, "the autosave tick looks at the disk");
            ok(SystemToast1205.find(mc, SystemToast1205.Id.LOW_DISK_SPACE) != null, "and shows the toast");
            DiskSpace.testUsableSpace = null;
            SystemToast1205.clear(mc);
            mc.world.sendQuittingDisconnectingPacket();
            mc.loadWorld(null);
            mc.displayGuiScreen(new GuiMainMenu());
            return true;
        });
        then(0, mc -> mc.getIntegratedServer() == null && mc.world == null);
        then(100, this::fixtures);
        then(5, this::worldList);
        then(5, mc -> {
            this.selection.selectWorld(indexOf(this.selection, BROKEN));
            return true;
        });
        then(5, this::listButtons);
        then(5, mc -> {
            shot(mc, "worldlist.png");
            entry(BROKEN).joinWorld();
            return true;
        });
        then(5, mc -> {
            GuiScreen s = mc.currentScreen;
            ok(s instanceof RecoverWorldDataScreen, "a broken level.dat opens the Failed to Load World screen: " + s);
            if (s instanceof RecoverWorldDataScreen) {
                RecoverWorldDataScreen r = (RecoverWorldDataScreen) s;
                ok(r.canRestore, "level.dat_old reads, so Attempt to Restore is on");
                ok(r.issues.contains("No issues"), "level.dat_old has no issues: " + r.issues);
                ok(r.message.contains(BROKEN) && r.message.contains("\n"), "the message names the folder, on two lines");
                ok(button(s, 1) != null && button(s, 1).enabled && "Attempt to Restore".equals(button(s, 1).displayString), "Attempt to Restore");
                ok(button(s, 0) != null && "Report a Bug".equals(button(s, 0).displayString), "Report a Bug");
                ok(button(s, 2) != null && "Back".equals(button(s, 2).displayString), "Back");
            }
            return true;
        });
        then(10, mc -> {
            shot(mc, "recover.png");
            ((RecoverWorldDataScreen) mc.currentScreen).onClose();
            return true;
        });
        then(5, mc -> {
            ok(mc.currentScreen instanceof GuiWorldSelection, "Back returns to the world list");
            this.selection = list((GuiWorldSelection) mc.currentScreen);
            entry(LOST).joinWorld();
            return true;
        });
        then(10, mc -> {
            GuiScreen s = mc.currentScreen;
            ok(s instanceof RecoverWorldDataScreen && !((RecoverWorldDataScreen) s).canRestore
                    && button(s, 1) != null && !button(s, 1).enabled, "with level.dat_old broken too, Attempt to Restore is off");
            return true;
        });
        then(5, mc -> {
            shot(mc, "recover-nofallback.png");
            ((RecoverWorldDataScreen) mc.currentScreen).onClose();
            return true;
        });
        then(5, mc -> {
            this.selection = list((GuiWorldSelection) mc.currentScreen);
            entry(BROKEN).joinWorld();
            return true;
        });
        then(5, mc -> {
            ((RecoverWorldDataScreen) mc.currentScreen).attemptRestore();
            ok(mc.currentScreen instanceof Screens1206.Message
                    && "Attempting to restore world...".equals(((Screens1206.Message) mc.currentScreen).title), "Attempting to restore world...");
            return true;
        });
        then(10, this::restored);
        then(30, mc -> {
            shot(mc, "restored.png");
            if (mc.currentScreen instanceof Screens1206.Confirm) {
                ((Screens1206.Confirm) mc.currentScreen).answer(false);
            } else if (mc.currentScreen instanceof Screens1206.Alert) {
                ((Screens1206.Alert) mc.currentScreen).ok();
            }
            return true;
        });
        then(5, mc -> {
            ok(mc.currentScreen instanceof GuiWorldSelection, "Back after the restore returns to the world list");
            this.selection = list((GuiWorldSelection) mc.currentScreen);
            WorldEntry broken = entry(BROKEN);
            ok(broken != null && !broken.corrupted, "the restored world lists as a world again");
            SystemToast1205.clear(mc);
            DiskSpace.testUsableSpace = 1L << 20;
            entry(WORLD).joinWorld();
            return true;
        });
        then(5, mc -> {
            GuiScreen s = mc.currentScreen;
            ok(s instanceof Screens1206.Confirm && ((Screens1206.Confirm) s).title.equals(WorldOpening.lowDiskTitle()),
                    "with 1 MB free, opening a world warns first: " + s);
            if (s instanceof Screens1206.Confirm) {
                ok(((Screens1206.Confirm) s).message.startsWith("There is not much space left on your device.\n"), "with 1.20.6's text");
                ok(button(s, 0) != null && "Continue".equals(button(s, 0).displayString) && "Back".equals(button(s, 1).displayString), "Continue / Back");
            }
            return true;
        });
        then(10, mc -> {
            shot(mc, "lowdisk-screen.png");
            ((Screens1206.Confirm) mc.currentScreen).answer(false);
            DiskSpace.testUsableSpace = null;
            return true;
        });
        then(5, mc -> {
            ok(mc.currentScreen instanceof GuiWorldSelection, "Back returns to the world list");
            return true;
        });
    }

    private int checksBefore;
    private final LowDiskSpaceTicker ticker = new LowDiskSpaceTicker();
    private GuiListWorldSelection selection;

    private boolean viossa(Minecraft mc) throws Exception {
        LanguageManager lm = mc.getLanguageManager();
        Language vp = null;
        for (Language l : lm.getLanguages()) {
            if (ViossaPack.CODE.equals(l.getLanguageCode())) {
                vp = l;
            }
        }
        ok(vp != null && "Viossa (Vilant)".equals(vp.toString()) && !vp.isBidirectional(), "Viossa is in the language list: " + vp);
        ok(ViossaPack.isInstalled(), "its pack is a default pack, so resource reloads keep it");
        if (vp == null) {
            return true;
        }
        String ourKey = null;
        String ourText = null;
        InputStream in = getClass().getClassLoader().getResourceAsStream("assets/" + TrailsAndTalesBackport.MOD_ID + "/lang/vp_vl.lang");
        if (in != null) {
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line = r.readLine();
            r.close();
            if (line != null && line.contains("=")) {
                ourKey = line.substring(0, line.indexOf('='));
                ourText = line.substring(line.indexOf('=') + 1);
            }
        }
        String before = mc.gameSettings.language;
        lm.setCurrentLanguage(vp);
        mc.gameSettings.language = vp.getLanguageCode();
        lm.onResourceManagerReload(mc.getResourceManager());
        try {
            ok("Spel namono".equals(I18n.format("menu.singleplayer")), "vanilla's 1.12.2 text in Viossa: " + I18n.format("menu.singleplayer"));
            ok("Humba lesa velttelnen long [1, 2]".equals(I18n.format("chunk.toast.loadFailure", new ChunkPos(1, 2))),
                    "1.20.5's keys in Viossa: " + I18n.format("chunk.toast.loadFailure", new ChunkPos(1, 2)));
            ok(ourKey != null && ourText.equals(I18n.format(ourKey)), "this mod's names in Viossa: " + ourKey + " = " + (ourKey == null ? null : I18n.format(ourKey)));
            ok("1.20.5 World Warnings and Viossa".equals(I18n.format("config.trailsandtalesbackport.presentation1205")),
                    "text Viossa lacks falls back to English");
        } finally {
            // The dev environment may lack vanilla's pack.mcmeta (FG2's asset download), so en_us may not be listed.
            lm.setCurrentLanguage(new Language(before, "", "", false));
            mc.gameSettings.language = before;
            lm.onResourceManagerReload(mc.getResourceManager());
        }
        ok("Singleplayer".equals(I18n.format("menu.singleplayer")) || !"en_us".equals(before), "and back to English");
        return true;
    }

    private boolean toasts(Minecraft mc) {
        Presentation1205Client.onChunkLoadFailure(mc, new ChunkPos(12, -34));
        Presentation1205Client.onChunkLoadFailure(mc, new ChunkPos(-1234, 5678));
        Presentation1205Client.onChunkSaveFailure(mc, new ChunkPos(7, 8));
        Presentation1205Client.onLowDiskSpace(mc);
        com.exiledradio.trailsandtalesbackport.presentation1205.client.SystemToastView load = SystemToast1205.find(mc, SystemToast1205.Id.CHUNK_LOAD_FAILURE);
        ok(load != null && load.title().equals("§cFailed to load chunk at [-1234, 5678]"), "a second load failure updates the first toast: "
                + (load == null ? null : load.title()));
        // The width is the first text's, as 1.20.6's (reset keeps it).
        int expected = Math.max(160, 30 + Math.max(mc.fontRenderer.getStringWidth("§c" + I18n.format("chunk.toast.loadFailure", new ChunkPos(12, -34))),
                mc.fontRenderer.getStringWidth(I18n.format("chunk.toast.checkLog"))));
        ok(load != null && load.width() == expected && load.width() > 160,
                "the toast is as wide as its text (1.20.6): " + (load == null ? 0 : load.width()) + " / " + expected);
        ok(load != null && load.height() == 32, "and one line high");
        com.exiledradio.trailsandtalesbackport.presentation1205.client.SystemToastView save = SystemToast1205.find(mc, SystemToast1205.Id.CHUNK_SAVE_FAILURE);
        ok(save != null && "§cFailed to save chunk at [7, 8]".equals(save.title()) && "See log for more details".equals(save.message()), "the save failure toast");
        com.exiledradio.trailsandtalesbackport.presentation1205.client.SystemToastView disk = SystemToast1205.find(mc, SystemToast1205.Id.LOW_DISK_SPACE);
        ok(disk != null && "Low disk space!".equals(disk.title()), "the low disk space toast");
        return true;
    }

    private boolean chunkFailureInGame(Minecraft mc) {
        ok(ChunkFailures.isActive(), "the chunk failure appender is in place on the client");
        final IntegratedServer server = mc.getIntegratedServer();
        server.addScheduledTask(() -> {
            try {
                WorldServer world = server.getWorld(0);
                ChunkProviderServer provider = world.getChunkProvider();
                Chunk chunk = new Chunk(world, 31000, 31000);
                Object[] lists = (Object[]) ReflectionHelper.findField(Chunk.class, "entityLists", "field_76645_j").get(chunk);
                lists[0] = null;
                Method save = ReflectionHelper.findMethod(ChunkProviderServer.class, "saveChunkData", "func_73242_b", Chunk.class);
                save.invoke(provider, chunk);
            } catch (Exception e) {
                TrailsAndTalesBackport.LOGGER.error("CLIENT SELFTEST could not break a chunk", e);
            }
        });
        return true;
    }

    private boolean fixtures(Minecraft mc) throws Exception {
        File world = new File(saves(mc), WORLD);
        ok(LevelData.issue(world, false) == null, "the test world's level.dat reads");
        byte[] good = Files.readAllBytes(new File(world, LevelData.LEVEL).toPath());
        byte[] garbage = "not a level".getBytes(StandardCharsets.UTF_8);
        File broken = new File(saves(mc), BROKEN);
        File lost = new File(saves(mc), LOST);
        FileUtils.deleteQuietly(broken);
        FileUtils.deleteQuietly(lost);
        broken.mkdirs();
        lost.mkdirs();
        Files.write(new File(broken, LevelData.OLD).toPath(), good);
        Files.write(new File(broken, LevelData.LEVEL).toPath(), garbage);
        Files.write(new File(lost, LevelData.OLD).toPath(), garbage);
        Files.write(new File(lost, LevelData.LEVEL).toPath(), garbage);
        long now = System.currentTimeMillis();
        new File(broken, LevelData.LEVEL).setLastModified(now - 2000L);
        new File(lost, LevelData.LEVEL).setLastModified(now - 1000L);
        // 1.12.2 lists the first from level.dat_old and leaves the second out.
        boolean listsBroken = false;
        boolean listsLost = false;
        for (net.minecraft.world.storage.WorldSummary s : mc.getSaveLoader().getSaveList()) {
            listsBroken |= s.getFileName().equals(BROKEN);
            listsLost |= s.getFileName().equals(LOST);
        }
        ok(listsBroken && !listsLost, "1.12.2's own list: the broken world (from level.dat_old) but not the lost one");
        mc.displayGuiScreen(new GuiWorldSelection(new GuiMainMenu()));
        return true;
    }

    private static GuiListWorldSelection list(GuiWorldSelection screen) throws Exception {
        return (GuiListWorldSelection) ReflectionHelper.findField(GuiWorldSelection.class, "selectionList", "field_184866_u").get(screen);
    }

    @SuppressWarnings("unchecked")
    private static List<GuiListWorldSelectionEntry> entries(GuiListWorldSelection list) throws Exception {
        return (List<GuiListWorldSelectionEntry>) ReflectionHelper.findField(GuiListWorldSelection.class, "entries", "field_186799_w").get(list);
    }

    private static int indexOf(GuiListWorldSelection list, String folder) throws Exception {
        List<GuiListWorldSelectionEntry> all = entries(list);
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i) instanceof WorldEntry && ((WorldEntry) all.get(i)).folder().equals(folder)) {
                return i;
            }
        }
        return -1;
    }

    private WorldEntry entry(String folder) throws Exception {
        int i = indexOf(this.selection, folder);
        return i < 0 ? null : (WorldEntry) entries(this.selection).get(i);
    }

    private static GuiButton button(GuiScreen screen, int id) {
        try {
            @SuppressWarnings("unchecked")
            List<GuiButton> buttons = (List<GuiButton>) ReflectionHelper.findField(GuiScreen.class, "buttonList", "field_146292_n").get(screen);
            for (GuiButton b : buttons) {
                if (b.id == id) {
                    return b;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private boolean worldList(Minecraft mc) throws Exception {
        ok(mc.currentScreen instanceof GuiWorldSelection, "the world list is open");
        this.selection = list((GuiWorldSelection) mc.currentScreen);
        WorldEntry world = entry(WORLD);
        WorldEntry broken = entry(BROKEN);
        WorldEntry lost = entry(LOST);
        ok(world != null && !world.corrupted, "the test world is a world");
        ok(broken != null && broken.corrupted, "the world with a broken level.dat lists as corrupted (1.20.6), not from level.dat_old");
        ok(lost != null && lost.corrupted, "the world with neither file readable lists too (1.20.6 lists it; 1.12.2 does not)");
        int iw = indexOf(this.selection, WORLD);
        int ib = indexOf(this.selection, BROKEN);
        int il = indexOf(this.selection, LOST);
        ok(il < ib && ib < iw, "sorted by level.dat's time, newest first: lost " + il + ", broken " + ib + ", world " + iw);
        return true;
    }

    private boolean listButtons(Minecraft mc) {
        GuiScreen s = mc.currentScreen;
        GuiButton play = button(s, 1);
        ok(play != null && "Attempt to Recover".equals(play.displayString) && play.enabled, "Play Selected World reads Attempt to Recover");
        ok(button(s, 4) != null && !button(s, 4).enabled && !button(s, 5).enabled, "Edit and Re-Create are off for it");
        ok(button(s, 2) != null && button(s, 2).enabled, "Delete stays on");
        return true;
    }

    private boolean restored(Minecraft mc) throws Exception {
        GuiScreen s = mc.currentScreen;
        ok(s instanceof Screens1206.Confirm && "Recovery done".equals(((Screens1206.Confirm) s).title)
                && "Recovery was successful!".equals(((Screens1206.Confirm) s).message), "Recovery done / Recovery was successful!: " + s);
        File broken = new File(saves(mc), BROKEN);
        ok(LevelData.issue(broken, false) == null, "level.dat reads again");
        ok(!new File(broken, LevelData.OLD).exists(), "level.dat_old became level.dat");
        File[] aside = broken.listFiles((dir, name) -> name.startsWith("level.dat_corrupted_"));
        ok(aside != null && aside.length == 1, "the broken level.dat is kept as level.dat_corrupted_<date>");
        File[] zips = WorldBackups.backupsDir().listFiles((dir, name) -> name.endsWith("_" + BROKEN + ".zip"));
        ok(zips != null && zips.length >= 1, "a backup was made first");
        com.exiledradio.trailsandtalesbackport.presentation1205.client.SystemToastView t = SystemToast1205.find(mc, SystemToast1205.Id.WORLD_BACKUP);
        ok(t != null && ("Backed up: " + BROKEN).equals(t.title()) && t.message() != null && t.message().startsWith("size: "),
                "with the backup toast: " + (t == null ? null : t.title() + " / " + t.message()));
        return true;
    }
}
