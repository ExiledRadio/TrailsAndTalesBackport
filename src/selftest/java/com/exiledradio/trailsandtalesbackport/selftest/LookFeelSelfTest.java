package com.exiledradio.trailsandtalesbackport.selftest;

import com.exiledradio.backportcore.presentation.Contribution;
import com.exiledradio.backportcore.presentation.GameVersion;
import com.exiledradio.backportcore.presentation.LoadingRevision;
import com.exiledradio.backportcore.presentation.MenuMusic;
import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.backportcore.presentation.Splashes;
import com.exiledradio.backportcore.presentation.option.OptionScreen;
import com.exiledradio.backportcore.presentation.option.PresentationOption;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.backportcore.presentation.versions.Look120;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Stream Lf: what this mod hands Backport Core's presentation module, and the files behind it; and Core's own 1.20
 * pieces (this mod's until 1.0.4) applying with this mod installed.
 */
public final class LookFeelSelfTest implements SelfTest {

    private static final String ID = TrailsAndTalesBackport.MOD_ID;
    /** Core's 1.20 vanilla text (Core 1.4.7). */
    private static final ResourceLocation CORE_TEXT = new ResourceLocation("backportcore", "look/1.20/vanilla_text");

    @Override
    public String name() {
        return "lookfeel";
    }

    @Override
    public void run(Check c) throws Exception {
        registration(c);
        panorama(c);
        title(c);
        splashes(c);
        music(c);
        options(c);
        vanillaText(c);
        lang(c);
    }

    private static void registration(Check c) {
        for (String v : new String[] {"1.20", "1.20.2", "1.20.3", "1.20.5"}) {
            Contribution contribution = Presentation.contribution(v);
            c.ok(contribution != null && contribution.owners().contains(ID), v + " is registered by " + ID + ": " + contribution);
        }
        // On its own (this dev server) 1.20.5 is the newest version, so every single slot is 1.20's.
        c.eq(GameVersion.parse("1.20.5"), Presentation.newest(), "the newest installed version");
        c.eq(LoadingRevision.PANORAMA_1_20_5, Presentation.loadingRevision(), "the world-loading screen revision");
        c.ok(Presentation.vanillaTexts().contains(CORE_TEXT), "Core's 1.20 vanilla text is registered");
        c.ok(!Presentation.vanillaTexts().contains(new ResourceLocation(ID, "vanilla_text")), "this mod has no 1.20 vanilla text of its own any more");
        c.ok(Presentation.HIDE_SPLASH_TEXTS.isShown(), "Core's Hide Splash Texts shows with 1.20.3 installed");
    }

    private static void panorama(Check c) throws IOException {
        List<Presentation.Panorama> all = Presentation.panoramas();
        Presentation.Panorama last = all.isEmpty() ? null : all.get(all.size() - 1);
        if (!c.ok(last != null && last.version.equals(GameVersion.parse("1.20")), "the newest panorama is 1.20's: " + all.size() + " panoramas")) {
            return;
        }
        for (int i = 0; i < 6; i++) {
            ResourceLocation face = last.faces[i];
            c.eq(new ResourceLocation(ID, "textures/gui/title/background/panorama_" + i + ".png"), face, "panorama face " + i);
            int[] size = pngSize(face);
            c.ok(size != null && size[0] == 1024 && size[1] == 1024, "face " + i + " is 1.20.6's 1024x1024 image: " + Arrays.toString(size));
        }
        ResourceLocation overlay = Presentation.panoramaOverlay();
        c.eq(new ResourceLocation(ID, "textures/gui/title/background/panorama_overlay.png"), overlay, "the panorama overlay");
        c.ok(overlay != null && pngSize(overlay) != null, "1.20.6's overlay is in the jar");
    }

    private static void title(Check c) throws IOException {
        String[][] assets = {
                {"textures/gui/title/minecraft.png", "look/1.20/textures/gui/title/minecraft.png", "1024x256"},
                {"textures/gui/title/minceraft.png", "look/1.20/textures/gui/title/minceraft.png", "1024x256"},
                {"textures/gui/title/minceraft.png.mcmeta", "look/1.20/textures/gui/title/smooth.png.mcmeta", null},
                {"textures/gui/title/minecraft.png.mcmeta", "look/1.20/textures/gui/title/smooth.png.mcmeta", null},
                {"textures/gui/title/edition.png", "look/1.20/textures/gui/title/edition.png", "512x64"},
                {"textures/gui/title/edition.png.mcmeta", "look/1.20/textures/gui/title/smooth.png.mcmeta", null},
                {"textures/gui/menu_background.png", "look/1.20.5/textures/gui/menu_background.png", "16x16"},
        };
        for (String[] a : assets) {
            ResourceLocation src = Presentation.asset(a[0]);
            c.eq(new ResourceLocation("backportcore", a[1]), src, "Core serves vanilla's " + a[0] + " from its own 1.20 pieces");
            if (a[2] != null) {
                int[] size = src == null ? null : pngSize(src);
                c.ok(size != null && a[2].equals(size[0] + "x" + size[1]), a[1] + " is " + a[2] + ": " + Arrays.toString(size));
            } else {
                c.ok(read(src).contains("\"blur\": true"), a[1] + " asks for smooth scaling");
            }
        }
        for (String gone : new String[] {"textures/gui/title/minecraft.png", "textures/gui/menu_background.png", "textures/gui/icon/icon_16x16.png",
                "shaders/post/menu_blur.json", "texts/splashes_added.txt", "vanilla_text/en_us.lang"}) {
            c.ok(open(new ResourceLocation(ID, gone)) == null, "moved to Core: " + gone);
        }
    }

    private static void splashes(Check c) throws IOException {
        List<String> added = lines(new ResourceLocation("backportcore", "look/1.20/texts/splashes_added.txt"));
        List<String> removed = lines(new ResourceLocation("backportcore", "look/1.20/texts/splashes_removed.txt"));
        c.eq(8, added.size(), "1.20.6 adds this many splashes to 1.19.4's");
        c.eq(2, removed.size(), "1.20.6 removes this many");
        List<String> base = linesOf(LookFeelSelfTest.class.getResourceAsStream("/assets/minecraft/texts/splashes.txt"));
        c.ok(base.size() > 300, "1.12.2's splashes.txt is readable: " + base.size());
        List<String> pool = Splashes.pool(base, Presentation.installed(), LookFeelSelfTest::lines);
        for (String s : new String[] {"This sand is sus", "Sniff sniff...", "Joule is neat too!", "Place hanging sign here",
                "Has working bookshelves!", "Remember to brush your... ...teeth", "Verlet integration!"}) {
            c.ok(pool.contains(s), "the splash pool has 1.20's \"" + s + "\"");
        }
        c.ok(!pool.contains("Java 16 + 1 + 4 = 21!"), "1.20.5's Java splash is left out (false on 1.12.2)");
        c.ok(!pool.contains("Verlet intregration!") || !base.contains("Verlet intregration!"), "the misspelled splash is gone");
        Set<String> expected = new LinkedHashSet<String>(base);
        for (String s : removed) {
            if (!Splashes.isAboutTheRuntime(s)) {
                expected.remove(s);
            }
        }
        for (String s : added) {
            if (!Splashes.isAboutTheRuntime(s)) {
                expected.add(s);
            }
        }
        // Other backports add their own splashes to the same pool (Caves & Cliffs' 1.17-1.18 ones), so the exact size
        // only holds when this mod is the only one adding; with another installed, every expected line must still be there.
        // Core's own 1.19 pieces (Core 1.4.7) apply too, under 1.20: 1.19 to 1.19.3's splashes are in the pool as well.
        boolean others = true || net.minecraftforge.fml.common.Loader.isModLoaded("cavesandcliffsbackport")
                || net.minecraftforge.fml.common.Loader.isModLoaded("wildupdatebackport")
                || net.minecraftforge.fml.common.Loader.isModLoaded("trickytrialsbackport");
        if (others) {
            // Their versions add and remove lines of their own; this mod's part is its additions in, its removals out.
            boolean ours = true;
            for (String s : added) {
                ours &= Splashes.isAboutTheRuntime(s) || pool.contains(s);
            }
            for (String s : removed) {
                ours &= Splashes.isAboutTheRuntime(s) || !pool.contains(s);
            }
            c.ok(ours, "1.20's additions are in the pool and its removals are out (other backports change it too)");
        } else {
            c.eq(expected.size(), pool.size(), "pool size: 1.12.2's set less 1.20's removals plus its additions");
        }
    }

    private static void music(Check c) throws IOException {
        List<MenuMusic.Track> base = new ArrayList<MenuMusic.Track>();
        for (String s : new String[] {"minecraft:music/menu/menu1", "minecraft:music/menu/menu2", "minecraft:music/menu/menu3",
                "minecraft:music/menu/menu4", "minecraft:music/game/swamp/aerie", "wildupdatebackport:music/game/swamp/firebugs"}) {
            base.add(new MenuMusic.Track(s, 1));
        }
        c.ok(MenuMusic.changed(Presentation.installed()), "the menu pool is changed, so Core picks its tracks");
        List<String> pool = new ArrayList<String>();
        for (MenuMusic.Track t : MenuMusic.pool(base, Presentation.installed())) {
            pool.add(t.soundName);
        }
        for (String track : new String[] {"a_familiar_room", "bromeliad", "crescent_dunes", "echo_in_the_wind"}) {
            String name = ID + ":music/game/" + track;
            c.ok(pool.contains(name), "the menu pool has 1.20's " + track + ": " + pool);
            c.ok(LookFeelSelfTest.class.getResource("/assets/" + ID + "/sounds/music/game/" + track + ".ogg") != null, track + ".ogg is in the jar");
        }
        c.ok(pool.contains("minecraft:music/menu/menu1"), "1.12.2's menu tracks stay");
        c.ok(!pool.contains("minecraft:music/game/swamp/aerie") && !pool.contains("wildupdatebackport:music/game/swamp/firebugs"),
                "1.19's swamp tracks leave the menu pool: " + pool);
        String pack = readStream(LookFeelSelfTest.class.getResourceAsStream("/" + ID + "_packs/game_music/assets/minecraft/sounds.json"));
        JsonObject game = new JsonParser().parse(pack).getAsJsonObject().getAsJsonObject("music.game");
        c.ok(game != null && !game.has("replace") && game.toString().contains(ID + ":music/game/a_familiar_room"),
                "the game music pack adds A Familiar Room to music.game without replacing it: " + game);
    }

    private static void options(Check c) {
        PresentationOption blur = Presentation.option(Look120.MENU_BLUR.id());
        c.ok(blur == Look120.MENU_BLUR && blur.isCoreOwned(), "Menu Background Blur is Core's, under this mod's id");
        c.eq(ID + ":menu_background_blurriness", blur.id(), "its id is still this mod's (saved values carry over)");
        c.eq(OptionScreen.ACCESSIBILITY, blur.screen(), "Menu Background Blur's screen");
        c.ok(blur.alsoOn().contains(OptionScreen.VIDEO), "and Video Settings too");
        c.eq(GameVersion.parse("1.20.5"), blur.since(), "since 1.20.5");
        c.eq("AUTO_JUMP", blur.after(), "after Auto-Jump");
        c.ok(blur.isShown(), "it shows with 1.20.5 installed");
        c.eq(0.5, Look120.MENU_BLUR.defaultValue(), "50% by default");
    }

    private static final String[] LANGS = {"de_de", "en_us", "es_ar", "es_es", "es_mx", "es_uy", "es_ve", "fr_fr", "it_it",
            "ja_jp", "ko_kr", "nl_nl", "pl_pl", "pt_br", "ru_ru", "tr_tr", "uk_ua", "zh_cn", "zh_tw"};

    private static void vanillaText(Check c) throws IOException {
        List<String> en = lines(new ResourceLocation("backportcore", "look/1.20/vanilla_text/en_us.lang"));
        c.ok(en.contains("death.attack.thorns=%1$s was killed while trying to hurt %2$s"), "1.20's \"while\" in death messages");
        c.ok(en.contains("deathScreen.title.hardcore=Game Over!"), "1.20's Game Over!");
        c.ok(en.contains("key.mouse.left=Left Button"), "1.20's Left Button");
        c.ok(en.contains("debug.creative_spectator.help=F3 + N = Cycle previous game mode <-> spectator"), "1.20's \"game mode\"");
        for (String code : LANGS) {
            List<String> l = lines(new ResourceLocation("backportcore", "look/1.20/vanilla_text/" + code + ".lang"));
            boolean sameKeys = l.size() == en.size();
            for (int i = 0; sameKeys && i < l.size(); i++) {
                sameKeys = l.get(i).split("=", 2)[0].equals(en.get(i).split("=", 2)[0]);
            }
            c.ok(sameKeys, code + "'s vanilla text has en_us's " + en.size() + " keys: " + l.size());
        }
        c.ok(lines(new ResourceLocation("backportcore", "look/1.20/vanilla_text/de_de.lang")).contains("deathScreen.title.hardcore=Game over!") ||
                        !lines(new ResourceLocation("backportcore", "look/1.20/vanilla_text/de_de.lang")).contains("deathScreen.title.hardcore=Game Over!"),
                "German is Mojang's German, not the English");
    }

    private static void lang(Check c) throws IOException {
        List<String> en = lines(new ResourceLocation(ID, "lang/en_us.lang"));
        for (String key : new String[] {"config.trailsandtalesbackport.lookAndFeel",
                "config.trailsandtalesbackport.lookAndFeel.enabled", "config.trailsandtalesbackport.lookAndFeel.gameMusic"}) {
            boolean found = false;
            for (String line : en) {
                found |= line.startsWith(key + "=");
            }
            c.ok(found, "en_us.lang has " + key);
        }
        List<String> core = lines(new ResourceLocation("backportcore", "lang/en_us.lang"));
        for (String key : new String[] {"options.accessibility.menu_background_blurriness", "options.accessibility.menu_background_blurriness.tooltip",
                "selectWorld.data_read", "debug.profiler_chart.help", "debug.fps_charts.help"}) {
            boolean ours = false;
            boolean cores = false;
            for (String line : en) {
                ours |= line.startsWith(key + "=");
            }
            for (String line : core) {
                cores |= line.startsWith(key + "=");
            }
            c.ok(cores && !ours, key + " is Core's (en_us), no longer this mod's");
        }
        // Rule 4: the 18 languages each have exactly en_us's keys (written by tools/lang/build_langs.py).
        java.util.Set<String> enKeys = keys(en);
        java.util.Map<String, String> enMap = map(en);
        for (String code : LANGS) {
            if (code.equals("en_us")) {
                continue;
            }
            List<String> l = lines(new ResourceLocation(ID, "lang/" + code + ".lang"));
            java.util.Set<String> k = keys(l);
            c.ok(k.equals(enKeys), code + ".lang has en_us's " + enKeys.size() + " keys: " + k.size());
            c.ok(!String.valueOf(map(l).get("config.trailsandtalesbackport.cherry.enabled.tooltip")).equals(enMap.get("config.trailsandtalesbackport.cherry.enabled.tooltip")),
                    code + ".lang's own text is translated");
        }
    }

    private static java.util.Set<String> keys(List<String> lines) {
        return map(lines).keySet();
    }

    private static java.util.Map<String, String> map(List<String> lines) {
        java.util.Map<String, String> out = new java.util.LinkedHashMap<>();
        for (String line : lines) {
            if (!line.isEmpty() && !line.startsWith("#") && line.contains("=")) {
                String[] kv = line.split("=", 2);
                out.put(kv[0], kv[1]);
            }
        }
        return out;
    }

    // ------------------------------------------------------------------------------------------

    private static InputStream open(ResourceLocation rl) {
        return LookFeelSelfTest.class.getResourceAsStream("/assets/" + rl.getNamespace() + "/" + rl.getPath());
    }

    static String read(ResourceLocation rl) throws IOException {
        InputStream in = rl == null ? null : open(rl);
        return in == null ? "" : readStream(in);
    }

    static String readStream(InputStream in) throws IOException {
        if (in == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            for (String line; (line = r.readLine()) != null; ) {
                out.append(line).append('\n');
            }
        }
        return out.toString();
    }

    static List<String> lines(ResourceLocation rl) throws IOException {
        return linesOf(open(rl));
    }

    static List<String> linesOf(InputStream in) throws IOException {
        List<String> out = new ArrayList<String>();
        for (String line : readStream(in).split("\n")) {
            if (!line.trim().isEmpty() && !line.startsWith("#")) {
                out.add(line);
            }
        }
        return out;
    }

    static BufferedImage image(ResourceLocation rl) throws IOException {
        InputStream in = open(rl);
        if (in == null) {
            return null;
        }
        try {
            return ImageIO.read(in);
        } finally {
            in.close();
        }
    }

    static int[] pngSize(ResourceLocation rl) throws IOException {
        BufferedImage img = image(rl);
        return img == null ? null : new int[] {img.getWidth(), img.getHeight()};
    }

    /** @return true when every pixel in columns [x0, x1) of rows [y0, y1) is clear */
    private static boolean columnEmpty(BufferedImage img, int x0, int y0, int x1, int y1) {
        for (int x = x0; x < x1; x++) {
            for (int y = y0; y < y1; y++) {
                if ((img.getRGB(x, y) >>> 24) != 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean rowsEmpty(BufferedImage img, int y0, int y1) {
        return columnEmpty(img, 0, y0, img.getWidth(), y1);
    }
}
