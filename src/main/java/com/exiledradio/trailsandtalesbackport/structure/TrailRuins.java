package com.exiledradio.trailsandtalesbackport.structure;

import com.exiledradio.backportcore.structure.ScatteredPlacement;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Trail ruins' settings, read from 1.20.1's own files in this jar: {@code worldgen/structure/trail_ruins.json} (start
 * pool, size, start height, projection, max distance, terrain adaptation, biome tag) and
 * {@code worldgen/structure_set/trail_ruins.json} (the {@code random_spread} grid). Nothing is typed in by hand; the
 * self-test pins the values 1.20.1 ships (34, 8, 83469867; size 7; y -15 on the surface; 80; bury).
 */
public final class TrailRuins {

    private static TrailRuins instance;

    public final ResourceLocation startPool;
    public final int maxDepth;
    public final int startHeight;
    public final boolean projectToSurface;
    public final int maxDistance;
    public final String terrainAdaptation;
    public final ScatteredPlacement grid;
    public final boolean linear;
    /** {@code #has_structure/trail_ruins}: today's biome ids. */
    public final Set<String> biomes;

    private TrailRuins() {
        JsonObject structure = file("/assets/" + TrailsAndTalesBackport.MOD_ID + "/worldgen/structure/trail_ruins.json");
        JsonObject set = file("/assets/" + TrailsAndTalesBackport.MOD_ID + "/worldgen/structure_set/trail_ruins.json");
        this.startPool = new ResourceLocation(structure.get("start_pool").getAsString());
        this.maxDepth = structure.get("size").getAsInt();
        JsonObject height = structure.getAsJsonObject("start_height");
        this.startHeight = height.has("absolute") ? height.get("absolute").getAsInt() : 0;
        this.projectToSurface = structure.has("project_start_to_heightmap");
        this.maxDistance = structure.has("max_distance_from_center") ? structure.get("max_distance_from_center").getAsInt() : 80;
        this.terrainAdaptation = structure.has("terrain_adaptation") ? structure.get("terrain_adaptation").getAsString() : "none";
        JsonObject placement = set.getAsJsonObject("placement");
        this.linear = !placement.has("spread_type") || "linear".equals(placement.get("spread_type").getAsString());
        this.grid = new ScatteredPlacement(placement.get("spacing").getAsInt(), placement.get("separation").getAsInt(),
                placement.get("salt").getAsInt(), !this.linear);
        Set<String> b = new LinkedHashSet<String>();
        String tag = structure.get("biomes").getAsString();
        JsonObject tagFile = file("/assets/" + TrailsAndTalesBackport.MOD_ID + "/tags/worldgen/biome/"
                + new ResourceLocation(tag.substring(1)).getPath() + ".json");
        for (JsonElement e : tagFile.getAsJsonArray("values")) {
            b.add(VanillaTemplate.full(e.getAsString()));
        }
        this.biomes = Collections.unmodifiableSet(b);
    }

    public static synchronized TrailRuins get() {
        if (instance == null) {
            instance = new TrailRuins();
        }
        return instance;
    }

    private static JsonObject file(String path) {
        JsonElement e = Archaeology.json(path);
        if (e == null) {
            throw new IllegalStateException("Trails & Tales Backport's jar is missing " + path);
        }
        return e.getAsJsonObject();
    }
}
