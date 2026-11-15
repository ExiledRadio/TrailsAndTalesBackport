package com.exiledradio.trailsandtalesbackport.selftest;

import com.exiledradio.backportcore.presentation.Contribution;
import com.exiledradio.backportcore.presentation.GameVersion;
import com.exiledradio.backportcore.presentation.LoadingRevision;
import com.exiledradio.backportcore.presentation.LoadingStatus;
import com.exiledradio.backportcore.presentation.MenuMusic;
import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.backportcore.presentation.Splashes;
import com.exiledradio.backportcore.presentation.option.OptionScreen;
import com.exiledradio.backportcore.presentation.option.PresentationOption;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.lookfeel.LookAndFeel;
import com.exiledradio.trailsandtalesbackport.lookfeel.LookRules;
import com.google.gson.JsonArray;
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

/** Stream Lf: what this mod hands Backport Core's presentation module, and the files behind it. */
public final class LookFeelSelfTest implements SelfTest {

    private static final String ID = TrailsAndTalesBackport.MOD_ID;

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
        rules(c);
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
        c.ok(!LoadingRevision.PANORAMA_1_20_5.coreDraws(), "Core leaves 1.20.5's loading screen to the backport (this mod draws it)");
        c.ok(Presentation.vanillaTexts().contains(new ResourceLocation(ID, "vanilla_text")), "1.20's vanilla text is registered");
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
                {"textures/gui/title/minecraft.png", "textures/gui/title/minecraft.png", "1024x256"},
                {"textures/gui/title/minceraft.png", "textures/gui/title/minceraft.png", "1024x256"},
                {"textures/gui/title/minceraft.png.mcmeta", "textures/gui/title/minceraft.png.mcmeta", null},
                {"textures/gui/title/minecraft.png.mcmeta", "textures/gui/title/minecraft.png.mcmeta", null},
                {"textures/gui/title/edition.png", "textures/gui/title/edition.png", "512x64"},
                {"textures/gui/title/edition.png.mcmeta", "textures/gui/title/edition.png.mcmeta", null},
                {"textures/gui/menu_background.png", "textures/gui/menu_background.png", "16x16"},
        };
        for (String[] a : assets) {
            ResourceLocation src = Presentation.asset(a[0]);
            c.eq(new ResourceLocation(ID, a[1]), src, "Core serves vanilla's " + a[0] + " from 1.20's");
            if (a[2] != null) {
                int[] size = src == null ? null : pngSize(src);
                c.ok(size != null && a[2].equals(size[0] + "x" + size[1]), a[1] + " is " + a[2] + ": " + Arrays.toString(size));
            } else {
                c.ok(read(src).contains("\"blur\": true"), a[1] + " asks for smooth scaling");
            }
        }
        for (int size : new int[] {16, 32}) {
            int[] icon = pngSize(new ResourceLocation(ID, "textures/gui/icon/icon_" + size + "x" + size + ".png"));
            c.ok(icon != null && icon[0] == size && icon[1] == size, "1.20's " + size + " pixel window icon: " + Arrays.toString(icon));
        }
        JsonObject blur = new JsonParser().parse(read(new ResourceLocation(ID, "shaders/post/menu_blur.json"))).getAsJsonObject();
        JsonArray passes = blur.getAsJsonArray("passes");
        c.eq(6, passes.size(), "the menu blur has 1.20.5's six passes");
        c.eq("blur", passes.get(0).getAsJsonObject().get("name").getAsString(), "each pass is 1.12.2's own blur program");
    }

    private static void splashes(Check c) throws IOException {
        List<String> added = lines(new ResourceLocation(ID, "texts/splashes_added.txt"));
        List<String> removed = lines(new ResourceLocation(ID, "texts/splashes_removed.txt"));
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
        c.eq(expected.size(), pool.size(), "pool size: 1.12.2's set less 1.20's removals plus its additions");
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
        PresentationOption blur = Presentation.option(LookAndFeel.MENU_BLUR.id());
        c.ok(blur == LookAndFeel.MENU_BLUR, "Menu Background Blur is registered");
        c.eq(OptionScreen.ACCESSIBILITY, blur.screen(), "Menu Background Blur's screen");
        c.ok(blur.alsoOn().contains(OptionScreen.VIDEO), "and Video Settings too");
        c.eq(GameVersion.parse("1.20.5"), blur.since(), "since 1.20.5");
        c.eq("AUTO_JUMP", blur.after(), "after Auto-Jump");
        c.eq(0.5, LookAndFeel.MENU_BLUR.defaultValue(), "50% by default");
        c.eq("options.off", LookAndFeel.MENU_BLUR.zeroKey(), "OFF at zero");
        c.eq(5.0F, LookAndFeel.blurRadius(), "the default blur radius is 5");
    }

    private static void rules(Check c) {
        // Ctrl + scroll on Video Settings (max 4): Auto steps down to 4, never up; 1 never goes to 0.
        c.eq(4, LookRules.nextGuiScale(0, -1, 4), "Auto, scroll down: 4");
        c.eq(0, LookRules.nextGuiScale(0, 1, 4), "Auto, scroll up: stays Auto");
        c.eq(3, LookRules.nextGuiScale(2, 1, 4), "2, up: 3");
        c.eq(4, LookRules.nextGuiScale(4, 1, 4), "4, up: stays 4");
        c.eq(1, LookRules.nextGuiScale(1, -1, 4), "1, down: stays 1");
        c.eq(1, LookRules.nextGuiScale(2, -1, 4), "2, down: 1");
        // 1.20's grid colors.
        c.eq(0x303572, LookRules.gridColor(LoadingStatus.LIQUID_CARVERS), "generated chunks: 1.20's carvers color");
        c.eq(0xFFE0A0, LookRules.gridColor(LoadingStatus.LIGHT), "lit chunks: 1.20's light color");
        c.eq(0x21C600, LookRules.gridColor(LoadingStatus.FEATURES), "populated chunks: features green");
        c.eq(0xFFFFFF, LookRules.gridColor(LoadingStatus.FULL), "full chunks: white");
        c.eq(0x000000, LookRules.gridColor(null), "untracked: black");
    }

    private static final String[] LANGS = {"de_de", "en_us", "es_ar", "es_es", "es_mx", "es_uy", "es_ve", "fr_fr", "it_it",
            "ja_jp", "ko_kr", "nl_nl", "pl_pl", "pt_br", "ru_ru", "tr_tr", "uk_ua", "zh_cn", "zh_tw"};

    private static void vanillaText(Check c) throws IOException {
        List<String> en = lines(new ResourceLocation(ID, "vanilla_text/en_us.lang"));
        c.ok(en.contains("death.attack.thorns=%1$s was killed while trying to hurt %2$s"), "1.20's \"while\" in death messages");
        c.ok(en.contains("deathScreen.title.hardcore=Game Over!"), "1.20's Game Over!");
        c.ok(en.contains("key.mouse.left=Left Button"), "1.20's Left Button");
        c.ok(en.contains("debug.creative_spectator.help=F3 + N = Cycle previous game mode <-> spectator"), "1.20's \"game mode\"");
        for (String code : LANGS) {
            List<String> l = lines(new ResourceLocation(ID, "vanilla_text/" + code + ".lang"));
            boolean sameKeys = l.size() == en.size();
            for (int i = 0; sameKeys && i < l.size(); i++) {
                sameKeys = l.get(i).split("=", 2)[0].equals(en.get(i).split("=", 2)[0]);
            }
            c.ok(sameKeys, code + "'s vanilla text has en_us's " + en.size() + " keys: " + l.size());
        }
        c.ok(lines(new ResourceLocation(ID, "vanilla_text/de_de.lang")).contains("deathScreen.title.hardcore=Game over!") ||
                        !lines(new ResourceLocation(ID, "vanilla_text/de_de.lang")).contains("deathScreen.title.hardcore=Game Over!"),
                "German is Mojang's German, not the English");
    }

    private static void lang(Check c) throws IOException {
        List<String> en = lines(new ResourceLocation(ID, "lang/en_us.lang"));
        for (String key : new String[] {"options.accessibility.menu_background_blurriness", "options.accessibility.menu_background_blurriness.tooltip",
                "selectWorld.data_read", "debug.profiler_chart.help", "debug.fps_charts.help", "config.trailsandtalesbackport.lookAndFeel",
                "config.trailsandtalesbackport.lookAndFeel.enabled", "config.trailsandtalesbackport.lookAndFeel.gameMusic"}) {
            boolean found = false;
            for (String line : en) {
                found |= line.startsWith(key + "=");
            }
            c.ok(found, "en_us.lang has " + key);
        }
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
