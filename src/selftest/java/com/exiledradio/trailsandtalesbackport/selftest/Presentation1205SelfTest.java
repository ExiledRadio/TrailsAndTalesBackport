package com.exiledradio.trailsandtalesbackport.selftest;

import com.exiledradio.backportcore.presentation.Contribution;
import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.asm.Asm;
import com.exiledradio.trailsandtalesbackport.presentation1205.ChunkFailures;
import com.exiledradio.trailsandtalesbackport.presentation1205.DiskSpace;
import com.exiledradio.trailsandtalesbackport.presentation1205.LowDiskSpaceTicker;
import com.exiledradio.trailsandtalesbackport.presentation1205.Presentation1205;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraft.world.chunk.storage.RegionFileCache;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.chunkio.ChunkIOExecutor;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stream presentation1205 on a dedicated server: the 1.20.5 registration and text, patch P4, and the chunk failure
 * watcher against real failures (a chunk that cannot become NBT, a region write that throws on the file IO thread,
 * a chunk whose stored data is not NBT, read both by the server thread and by Forge's loading threads).
 */
public final class Presentation1205SelfTest implements SelfTest {

    private static final String ID = TrailsAndTalesBackport.MOD_ID;
    static final String[] CODES = {"de_de", "en_us", "es_ar", "es_es", "es_mx", "es_uy", "es_ve", "fr_fr", "it_it", "ja_jp",
            "ko_kr", "nl_nl", "pl_pl", "pt_br", "ru_ru", "tr_tr", "uk_ua", "zh_cn", "zh_tw", "vp_vl"};
    static final String[] KEYS = {"chunk.toast.checkLog", "chunk.toast.loadFailure", "chunk.toast.lowDiskSpace",
            "chunk.toast.lowDiskSpace.description", "chunk.toast.saveFailure", "gui.continue", "recover_world.bug_tracker",
            "recover_world.button", "recover_world.done.failed", "recover_world.done.success", "recover_world.done.title",
            "recover_world.issue.missing_file", "recover_world.issue.none", "recover_world.message", "recover_world.no_fallback",
            "recover_world.restore", "recover_world.restoring", "recover_world.state_entry", "recover_world.state_entry.unknown",
            "recover_world.title", "recover_world.warning", "selectWorld.warning.lowDiskSpace.description",
            "selectWorld.warning.lowDiskSpace.title"};
    private static final Pattern ARG = Pattern.compile("%(?:(\\d+)\\$)?([sd])");

    @Override
    public String name() {
        return "presentation1205";
    }

    @Override
    public void run(Check c) throws Exception {
        registration(c);
        text(c);
        patch(c);
        failures(c);
        disk(c);
    }

    private static void registration(Check c) {
        Contribution v = Presentation.contribution(Presentation1205.VERSION);
        c.ok(v != null && v.owners().contains(ID), "1.20.5 is registered by " + ID + ": " + v);
        c.ok(Presentation.vanillaTexts().contains(Presentation1205.VANILLA_TEXT), "Core composes vanilla_text_1205: " + Presentation.vanillaTexts());
        c.ok(Presentation1205.chunkToasts() && Presentation1205.lowDiskSpace() && Presentation1205.recoverWorld() && Presentation1205.viossa(),
                "every option is on with 1.20.5 installed");
    }

    static Map<String, String> lang(String path) throws Exception {
        Map<String, String> out = new LinkedHashMap<String, String>();
        InputStream in = Presentation1205SelfTest.class.getClassLoader().getResourceAsStream(path);
        if (in == null) {
            return null;
        }
        BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        while ((line = r.readLine()) != null) {
            int eq = line.indexOf('=');
            if (!line.isEmpty() && !line.startsWith("#") && eq > 0) {
                out.put(line.substring(0, eq), line.substring(eq + 1));
            }
        }
        r.close();
        return out;
    }

    static List<String> args(String text) {
        List<String> out = new ArrayList<String>();
        Matcher m = ARG.matcher(text.replace("%%", ""));
        int seq = 0;
        while (m.find()) {
            String index = m.group(1) != null ? m.group(1) : String.valueOf(++seq);
            String a = index + m.group(2);
            if (!out.contains(a)) {
                out.add(a);
            }
        }
        java.util.Collections.sort(out);
        return out;
    }

    private static void text(Check c) throws Exception {
        String folder = "assets/" + ID + "/vanilla_text_1205/";
        Map<String, String> en = lang(folder + "en_us.lang");
        c.ok(en != null, "vanilla_text_1205/en_us.lang is in the jar");
        if (en == null) {
            return;
        }
        c.eq("Failed to load chunk at %s", en.get("chunk.toast.loadFailure"), "en_us chunk.toast.loadFailure");
        c.eq("Warning! Low disk space!", en.get("selectWorld.warning.lowDiskSpace.title"), "en_us lowDiskSpace.title");
        c.eq("Attempt to Restore", en.get("recover_world.restore"), "en_us recover_world.restore");
        for (String code : CODES) {
            Map<String, String> l = lang(folder + code + ".lang");
            if (!c.ok(l != null, code + ".lang exists")) {
                continue;
            }
            int missing = 0;
            int badArgs = 0;
            for (String k : KEYS) {
                if (!l.containsKey(k) || l.get(k).isEmpty()) {
                    missing++;
                } else if (!args(l.get(k)).equals(args(en.get(k)))) {
                    badArgs++;
                }
            }
            c.eq(0, missing, code + ": keys missing");
            c.eq(0, badArgs, code + ": keys whose arguments differ from English");
        }
        Map<String, String> vp = lang(folder + "vp_vl.lang");
        c.ok(vp != null && vp.size() > 2000, "vp_vl has 1.12.2's vanilla keys: " + (vp == null ? 0 : vp.size()));
        if (vp != null) {
            c.eq("Spel namono", vp.get("menu.singleplayer"), "vp_vl menu.singleplayer");
            c.eq("Humba lesa velttelnen long %s", vp.get("chunk.toast.loadFailure"), "vp_vl chunk.toast.loadFailure");
            Map<String, String> old = lang("assets/minecraft/lang/en_us.lang");
            if (old != null) {
                int bad = 0;
                for (Map.Entry<String, String> e : vp.entrySet()) {
                    String english = old.get(e.getKey());
                    if (english != null && !args(english).equals(args(e.getValue()))) {
                        bad++;
                        TrailsAndTalesBackport.LOGGER.warn("SELFTEST vp_vl argument mismatch {}: {} / {}", e.getKey(), english, e.getValue());
                    }
                }
                // A handful of keys 1.20.6 rewords (Lf's vanilla_text) take 1.20.6's arguments, not 1.12.2's.
                c.ok(bad <= 3, "vp_vl's arguments match 1.12.2's English: " + bad + " differ");
            } else {
                TrailsAndTalesBackport.LOGGER.warn("SELFTEST note: no 1.12.2 en_us.lang on the class path; argument check skipped");
            }
        }
        Map<String, String> ours = lang("assets/" + ID + "/lang/vp_vl.lang");
        Map<String, String> ourEn = lang("assets/" + ID + "/lang/en_us.lang");
        c.ok(ours != null && ours.size() > 100, "this mod's vp_vl.lang: " + (ours == null ? 0 : ours.size()) + " keys");
        if (ours != null && ourEn != null) {
            int stray = 0;
            for (String k : ours.keySet()) {
                if (!ourEn.containsKey(k)) {
                    stray++;
                }
            }
            c.eq(0, stray, "vp_vl.lang keys en_us.lang lacks");
        }
    }

    private static void patch(Check c) {
        c.ok(Asm.APPLIED.contains("P4 ChunkProviderServer.saveChunkData"), "P4 patched saveChunkData: " + Asm.APPLIED);
        c.ok(Asm.APPLIED.contains("P4 ChunkProviderServer.loadChunkFromFile"), "P4 patched loadChunkFromFile");
    }

    private static boolean waitFor(int reportsBefore, long millis) throws InterruptedException {
        long end = System.currentTimeMillis() + millis;
        while (ChunkFailures.reports == reportsBefore && System.currentTimeMillis() < end) {
            Thread.sleep(10L);
        }
        return ChunkFailures.reports != reportsBefore;
    }

    private static void failures(Check c) throws Exception {
        c.ok(ChunkFailures.install(), "the chunk failure appender is in place");
        c.ok(ChunkFailures.install(), "and installing again keeps it");
        final List<String> heard = new ArrayList<String>();
        ChunkFailures.setListener((load, pos) -> {
            synchronized (heard) {
                heard.add((load ? "load " : "save ") + pos);
            }
        });
        try {
            WorldServer world = c.world;
            ChunkProviderServer provider = world.getChunkProvider();
            AnvilChunkLoader loader = (AnvilChunkLoader) provider.chunkLoader;
            Method saveChunkData = ReflectionHelper.findMethod(ChunkProviderServer.class, "saveChunkData", "func_73242_b", Chunk.class);
            Method loadChunkFromFile = ReflectionHelper.findMethod(ChunkProviderServer.class, "loadChunkFromFile", "func_73239_e", int.class, int.class);

            // 1. The server thread cannot turn a chunk into NBT (an entity list missing): AnvilChunkLoader.saveChunk's catch.
            int before = ChunkFailures.reports;
            saveChunkData.invoke(provider, brokenChunk(world, 31000, 31000));
            c.eq("save [31000, 31000]", ChunkFailures.lastReport, "a chunk that cannot be saved is reported with its position");
            c.ok(ChunkFailures.reports == before + 1, "once");

            // 2. The file IO thread cannot write it (a string tag without a string): writeNextIO's catch.
            before = ChunkFailures.reports;
            NBTTagCompound root = new NBTTagCompound();
            NBTTagString bad = new NBTTagString("x");
            ReflectionHelper.findField(NBTTagString.class, "data", "field_74751_a").set(bad, null);
            root.setTag("Level", bad);
            Method pending = ReflectionHelper.findMethod(AnvilChunkLoader.class, "addChunkToPending", "func_75824_a", ChunkPos.class, NBTTagCompound.class);
            pending.invoke(loader, new ChunkPos(31001, 31001), root);
            c.ok(waitFor(before, 5000L), "the file IO thread's failure is heard");
            c.eq("save [31001, 31001]", ChunkFailures.lastReport, "with the chunk it was writing");

            // 3. Stored data that is not NBT, read by the server thread (loadChunkFromFile's catch).
            File dir = world.getSaveHandler().getWorldDirectory();
            DataOutputStream out = RegionFileCache.getChunkOutputStream(dir, 31002, 31002);
            out.write(new byte[] {10, 0x7F, (byte) 0xFF});
            out.close();
            before = ChunkFailures.reports;
            Object loaded = loadChunkFromFile.invoke(provider, 31002, 31002);
            c.ok(loaded == null, "the unreadable chunk does not load");
            c.eq("load [31002, 31002]", ChunkFailures.lastReport, "and is reported with its position");

            // 4. The same chunk read by Forge's chunk loading threads, as chunks load while playing.
            before = ChunkFailures.reports;
            ChunkIOExecutor.queueChunkLoad(world, loader, provider, 31002, 31002, () -> { });
            c.ok(waitFor(before, 5000L), "a chunk loading thread's failure is heard");
            c.eq("load [31002, 31002]", ChunkFailures.lastReport, "with the QueuedChunk's position");

            synchronized (heard) {
                c.ok(heard.contains("save [31000, 31000]") && heard.contains("save [31001, 31001]") && heard.contains("load [31002, 31002]"),
                        "the listener heard each: " + heard);
            }

            // 5. With the option off nothing is reported.
            ModConfig.presentation1205.chunkToasts = false;
            before = ChunkFailures.reports;
            saveChunkData.invoke(provider, brokenChunk(world, 31003, 31003));
            c.eq(before, ChunkFailures.reports, "with chunkToasts off, no report");
        } finally {
            ModConfig.presentation1205.chunkToasts = true;
            ChunkFailures.setListener(null);
        }
    }

    private static Chunk brokenChunk(WorldServer world, int x, int z) throws Exception {
        Chunk chunk = new Chunk(world, x, z);
        Object[] lists = (Object[]) ReflectionHelper.findField(Chunk.class, "entityLists", "field_76645_j").get(chunk);
        lists[0] = null;
        return chunk;
    }

    private static void disk(Check c) {
        File dir = c.world.getSaveHandler().getWorldDirectory();
        c.ok(DiskSpace.estimate(dir) > 0L, "the world's drive has a usable size: " + DiskSpace.estimate(dir));
        c.eq(67108864L, DiskSpace.WARNING_THRESHOLD, "1.20.6's threshold, 64 MB");
        try {
            DiskSpace.testUsableSpace = DiskSpace.WARNING_THRESHOLD - 1;
            c.ok(DiskSpace.isLow(dir), "one byte under 64 MB is low");
            DiskSpace.testUsableSpace = DiskSpace.WARNING_THRESHOLD;
            c.ok(!DiskSpace.isLow(dir), "64 MB is not");
            // A dedicated server never warns (1.20.6's MinecraftServer reports nothing).
            final int[] warned = {0};
            LowDiskSpaceTicker.setListener(() -> warned[0]++);
            DiskSpace.testUsableSpace = 1L;
            new LowDiskSpaceTicker().advance(LowDiskSpaceTicker.AUTOSAVE_INTERVAL);
            LowDiskSpaceTicker.check();
            c.eq(0, warned[0], "no low disk warning on a dedicated server");
            c.ok(LowDiskSpaceTicker.worldFolder() == null, "the dedicated server has no singleplayer world folder");
        } finally {
            DiskSpace.testUsableSpace = null;
            LowDiskSpaceTicker.setListener(null);
        }
    }
}
