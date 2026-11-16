package com.exiledradio.trailsandtalesbackport.music;

import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.biome.CherryGroveContent;
import com.exiledradio.trailsandtalesbackport.common.Content;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 1.20's biome music (as 1.20.6 has it): Aaron Cherof's Bromeliad, Crescent Dunes and Echo in the Wind, with Left to
 * Bloom and the older tracks, by biome. 1.20.6's {@code Minecraft.getSituationalMusic} plays a biome's
 * {@code music.overworld.<biome>} wherever the game music would play.
 *
 * <p>The table is 1.20.6's {@code sounds.json}, track for track (weights and volumes included). The tracks come from
 * the mods that ship them: 1.12.2's own (by their old file names), this mod's, Caves &amp; Cliffs Backport's 1.18
 * tracks and Wild Update Backport's swamp tracks; a track whose mod is absent is left out of the list
 * ({@code music.client.BiomeMusicPack} writes the sound list at load).
 *
 * <p>1.20 also rewrote the lists of 1.18's eight music biomes (meadow, grove, the peaks, the slopes, lush and
 * dripstone caves). Caves &amp; Cliffs owns those regions and chooses where they play; this mod's "1.20" music step,
 * which runs after its "1.18" one, swaps each for 1.20's list ({@code music.client.BiomeMusicClient}). Swamp and deep
 * dark music are 1.19's, Wild Update's.
 *
 * <p>No client types: the self-test reads the table and the biome mapping on a server.
 */
@Mod.EventBusSubscriber(modid = TrailsAndTalesBackport.MOD_ID)
public final class BiomeMusic {

    /** One entry of a {@code music.overworld.*} list: a 1.20.6 track name, its weight and volume. */
    public static final class Entry {
        public final String track;
        public final int weight;
        public final float volume;

        Entry(String track, int weight, float volume) {
            this.track = track;
            this.weight = weight;
            this.volume = volume;
        }
    }

    /** 1.20.6's music biomes (the part of each event name after {@code music.overworld.}), in sounds.json order. */
    static final Map<String, List<Entry>> LISTS = new LinkedHashMap<String, List<Entry>>();
    private static final Map<String, SoundEvent> EVENTS = new LinkedHashMap<String, SoundEvent>();

    /** 1.18's music biomes, whose lists 1.20 rewrote; Caves &amp; Cliffs chooses where they play. */
    public static final String[] REGIONS_1_18 = {"lush_caves", "dripstone_caves", "meadow", "grove", "snowy_slopes",
            "jagged_peaks", "frozen_peaks", "stony_peaks"};

    static {
        // "track:w:v": weight w (1 when empty) and volume v (1 when empty).
        list("badlands", "crescent_dunes:2:0.4", "danny", "dry_hands", "echo_in_the_wind::0.4", "haggstrom", "key",
                "living_mice", "mice_on_venus", "one_more_day::0.4", "oxygene", "subwoofer_lullaby", "wet_hands");
        list("bamboo_jungle", "bromeliad:2:0.4", "danny", "dry_hands", "haggstrom", "key", "left_to_bloom::0.4",
                "living_mice", "mice_on_venus", "oxygene", "subwoofer_lullaby", "wet_hands");
        list("cherry_grove", "bromeliad:3:0.4", "clark", "echo_in_the_wind:3:0.4", "left_to_bloom::0.4", "minecraft",
                "sweden");
        list("desert", "crescent_dunes:3:0.4", "danny", "haggstrom", "key", "living_mice", "one_more_day::0.4",
                "oxygene", "subwoofer_lullaby");
        list("dripstone_caves", "an_ordinary_day", "danny", "infinite_amethyst::0.4", "key", "oxygene",
                "subwoofer_lullaby", "wending:2:0.4");
        list("flower_forest", "bromeliad::0.4", "danny", "echo_in_the_wind::0.4", "haggstrom", "key",
                "left_to_bloom::0.4", "living_mice", "oxygene", "subwoofer_lullaby");
        list("forest", "bromeliad::0.4", "clark", "comforting_memories::0.4", "danny", "dry_hands",
                "floating_dream::0.4", "haggstrom", "key", "left_to_bloom::0.4", "living_mice", "mice_on_venus",
                "minecraft", "one_more_day::0.4", "oxygene", "subwoofer_lullaby", "swamp/aerie::0.4",
                "swamp/firebugs::0.4", "swamp/labyrinthine::0.4", "sweden", "wet_hands");
        list("frozen_peaks", "danny", "dry_hands", "haggstrom", "living_mice", "mice_on_venus", "stand_tall:2:0.4",
                "subwoofer_lullaby", "wet_hands");
        list("grove", "clark", "comforting_memories::0.4", "infinite_amethyst::0.4", "key", "mice_on_venus",
                "minecraft", "oxygene", "sweden", "wending::0.4");
        list("jagged_peaks", "dry_hands", "floating_dream::0.4", "key", "living_mice", "mice_on_venus", "oxygene",
                "stand_tall::0.4", "subwoofer_lullaby", "wending::0.4", "wet_hands");
        list("jungle", "bromeliad:3:0.4", "danny", "dry_hands", "haggstrom", "key", "left_to_bloom::0.4",
                "living_mice", "mice_on_venus", "oxygene", "subwoofer_lullaby", "wet_hands");
        list("lush_caves", "an_ordinary_day:2", "clark:2", "echo_in_the_wind:2:0.4", "floating_dream:2",
                "left_to_bloom:4:0.4", "mice_on_venus:2", "minecraft:2", "one_more_day:2:0.4", "swamp/aerie::0.4",
                "swamp/firebugs::0.4", "swamp/labyrinthine::0.4", "sweden:2");
        list("meadow", "danny", "haggstrom", "left_to_bloom::0.4", "living_mice", "one_more_day:2:0.4",
                "subwoofer_lullaby");
        list("old_growth_taiga", "clark:3", "comforting_memories:3:0.4", "danny:3", "dry_hands:3",
                "floating_dream:3:0.4", "haggstrom:3", "key:3", "left_to_bloom:3:0.4", "living_mice:3",
                "mice_on_venus:3", "minecraft:3", "one_more_day:3:0.4", "oxygene:3", "subwoofer_lullaby:3",
                "swamp/aerie::0.4", "swamp/firebugs::0.4", "swamp/labyrinthine::0.4", "sweden:3", "wet_hands:3");
        list("snowy_slopes", "an_ordinary_day::0.4", "haggstrom", "living_mice", "one_more_day::0.4",
                "stand_tall::0.4", "subwoofer_lullaby");
        list("sparse_jungle", "bromeliad:2:0.4", "danny", "dry_hands", "haggstrom", "key", "left_to_bloom::0.4",
                "living_mice", "mice_on_venus", "oxygene", "subwoofer_lullaby", "wet_hands");
        list("stony_peaks", "danny", "dry_hands", "haggstrom", "living_mice", "mice_on_venus", "stand_tall:2:0.4",
                "subwoofer_lullaby", "wending::0.4", "wet_hands");
    }

    /**
     * 1.12.2's biomes by registry path, to 1.20.6's music biome (OverworldBiomes: forest() gives birch and old growth
     * birch forests the forest's music, darkForest() too; badlands() all three badlands; oldGrowthTaiga() both old
     * growth taigas).
     */
    private static final Map<String, String> VANILLA = new LinkedHashMap<String, String>();
    /** Modern ids other mods' biomes may carry as their path, to their music biome. */
    private static final Map<String, String> MODERN = new LinkedHashMap<String, String>();

    static {
        map("jungle", "jungle", "jungle_hills", "mutated_jungle");
        map("sparse_jungle", "jungle_edge", "mutated_jungle_edge");
        map("desert", "desert", "desert_hills", "mutated_desert");
        map("badlands", "mesa", "mesa_rock", "mesa_clear_rock", "mutated_mesa", "mutated_mesa_rock",
                "mutated_mesa_clear_rock");
        map("forest", "forest", "forest_hills", "birch_forest", "birch_forest_hills", "mutated_birch_forest",
                "mutated_birch_forest_hills", "roofed_forest", "mutated_roofed_forest");
        map("flower_forest", "mutated_forest");
        map("old_growth_taiga", "redwood_taiga", "redwood_taiga_hills", "mutated_redwood_taiga",
                "mutated_redwood_taiga_hills");

        for (String id : LISTS.keySet()) {
            MODERN.put(id, id);
        }
        MODERN.put("bamboo_jungle_hills", "bamboo_jungle");
        MODERN.put("eroded_badlands", "badlands");
        MODERN.put("wooded_badlands", "badlands");
        MODERN.put("birch_forest", "forest");
        MODERN.put("old_growth_birch_forest", "forest");
        MODERN.put("dark_forest", "forest");
        MODERN.put("old_growth_pine_taiga", "old_growth_taiga");
        MODERN.put("old_growth_spruce_taiga", "old_growth_taiga");
    }

    private BiomeMusic() {
    }

    private static void list(String biome, String... entries) {
        List<Entry> out = new ArrayList<Entry>();
        for (String e : entries) {
            String[] p = e.split(":", -1);
            int weight = p.length > 1 && !p[1].isEmpty() ? Integer.parseInt(p[1]) : 1;
            float volume = p.length > 2 && !p[2].isEmpty() ? Float.parseFloat(p[2]) : 1.0F;
            out.add(new Entry(p[0], weight, volume));
        }
        LISTS.put(biome, Collections.unmodifiableList(out));
    }

    private static void map(String music, String... paths) {
        for (String p : paths) {
            VANILLA.put(p, music);
        }
    }

    /** @return true while the feature is on */
    public static boolean enabled() {
        return ModConfig.lookAndFeel.biomeMusic;
    }

    /** @return the music biomes, in sounds.json order */
    public static Map<String, List<Entry>> lists() {
        return Collections.unmodifiableMap(LISTS);
    }

    /** @return the event of a music biome, or null (feature off, or the grove's while its stream is off) */
    @Nullable
    public static SoundEvent event(String biome) {
        if ("cherry_grove".equals(biome)) {
            return enabled() ? CherryGroveContent.music : null;
        }
        return EVENTS.get(biome);
    }

    /** @return every registered biome music event */
    public static List<SoundEvent> events() {
        List<SoundEvent> out = new ArrayList<SoundEvent>(EVENTS.values());
        SoundEvent grove = event("cherry_grove");
        if (grove != null) {
            out.add(grove);
        }
        return out;
    }

    /** @return the music biome of a biome, or null for one with only the game music */
    @Nullable
    public static String musicOf(@Nullable Biome biome) {
        if (biome == null || biome.getRegistryName() == null) {
            return null;
        }
        if (CherryGroveContent.biome != null && biome == CherryGroveContent.biome) {
            return "cherry_grove";
        }
        ResourceLocation id = biome.getRegistryName();
        if ("minecraft".equals(id.getNamespace())) {
            return VANILLA.get(id.getPath());
        }
        return MODERN.get(id.getPath());
    }

    /** 1.12.2's tracks, by the names 1.20 gave their files (23w14a's rename). */
    private static final Map<String, String> OLD_NAMES = new LinkedHashMap<String, String>();
    /** The tracks this mod ships. */
    private static final String[] OURS = {"a_familiar_room", "bromeliad", "crescent_dunes", "echo_in_the_wind", "left_to_bloom"};

    static {
        String[] pairs = {"minecraft", "calm1", "clark", "calm2", "sweden", "calm3", "subwoofer_lullaby", "hal1",
                "living_mice", "hal2", "haggstrom", "hal3", "danny", "hal4", "key", "nuance1", "oxygene", "nuance2",
                "dry_hands", "piano1", "wet_hands", "piano2", "mice_on_venus", "piano3"};
        for (int i = 0; i < pairs.length; i += 2) {
            OLD_NAMES.put(pairs[i], pairs[i + 1]);
        }
    }

    /**
     * @return the sound file of a 1.20.6 track name ({@code namespace:music/game/...}) from the mod that ships it, or
     *         null when that mod is not installed: 1.12.2's own, this mod's, Caves &amp; Cliffs' 1.18 tracks, Wild
     *         Update's swamp tracks
     */
    @Nullable
    public static String file(String track) {
        String old = OLD_NAMES.get(track);
        if (old != null) {
            return "minecraft:music/game/" + old;
        }
        for (String ours : OURS) {
            if (ours.equals(track)) {
                return TrailsAndTalesBackport.MOD_ID + ":music/game/" + track;
            }
        }
        String owner = track.startsWith("swamp/") ? "wildupdatebackport" : "cavesandcliffsbackport";
        return shipped(owner, "music/game/" + track) ? owner + ":music/game/" + track : null;
    }

    private static boolean shipped(String namespace, String path) {
        return BiomeMusic.class.getResource("/assets/" + namespace + "/sounds/" + path + ".ogg") != null;
    }

    /** @return this mod's {@code sounds.json} part with every music biome's list, of the tracks installed */
    public static String soundsJson() {
        StringBuilder out = new StringBuilder("{\n");
        boolean firstEvent = true;
        for (Map.Entry<String, List<Entry>> list : LISTS.entrySet()) {
            if (!firstEvent) {
                out.append(",\n");
            }
            firstEvent = false;
            out.append("  \"music.overworld.").append(list.getKey()).append("\": {\"sounds\": [");
            boolean first = true;
            for (Entry e : list.getValue()) {
                String file = file(e.track);
                if (file == null) {
                    continue;
                }
                out.append(first ? "\n" : ",\n");
                first = false;
                // Every track streams (1.20.6's jungle forgets "stream" on Left to Bloom: a whole track in memory).
                out.append("    {\"name\": \"").append(file).append("\", \"stream\": true");
                if (e.weight != 1) {
                    out.append(", \"weight\": ").append(e.weight);
                }
                if (e.volume != 1.0F) {
                    out.append(", \"volume\": ").append(e.volume);
                }
                out.append('}');
            }
            out.append(first ? "]}" : "\n  ]}");
        }
        return out.append("\n}\n").toString();
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        if (!enabled()) {
            return;
        }
        for (String biome : LISTS.keySet()) {
            if ("cherry_grove".equals(biome)) {
                continue; // CherryGroveContent's, registered with the biome
            }
            ResourceLocation id = Content.id("music.overworld." + biome);
            SoundEvent e = new SoundEvent(id).setRegistryName(id);
            event.getRegistry().register(e);
            EVENTS.put(biome, e);
        }
    }
}
