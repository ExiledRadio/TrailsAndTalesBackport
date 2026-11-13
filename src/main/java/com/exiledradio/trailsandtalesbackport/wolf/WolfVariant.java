package com.exiledradio.trailsandtalesbackport.wolf;

import net.minecraft.world.biome.Biome;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 1.20.5's nine wolf variants ({@code WolfVariants}) and the biomes that choose them.
 *
 * <p>Vanilla picks the variant whose biome set holds the biome a wolf spawns in, else pale. The sets are 1.20's
 * (taiga; savannas; grove; old growth pine taiga; snowy taiga; jungles; forest; old growth spruce taiga;
 * badlands), matched here by registry path in any namespace, under 1.12.2's names and 1.18's: 1.12.2's hills and
 * "M" biomes are the ones 1.18 folded into each. The grove belongs to Caves &amp; Cliffs and is found by its path
 * only, so nothing links against that mod.
 */
public enum WolfVariant {
    PALE("wolf", "taiga", "taiga_hills", "mutated_taiga"),
    SPOTTED("wolf_spotted", "savanna", "savanna_rock", "mutated_savanna", "mutated_savanna_rock", "savanna_plateau",
            "windswept_savanna"),
    SNOWY("wolf_snowy", "grove"),
    BLACK("wolf_black", "redwood_taiga", "redwood_taiga_hills", "old_growth_pine_taiga"),
    ASHEN("wolf_ashen", "taiga_cold", "taiga_cold_hills", "mutated_taiga_cold", "snowy_taiga"),
    RUSTY("wolf_rusty", "jungle", "jungle_hills", "jungle_edge", "mutated_jungle", "mutated_jungle_edge", "sparse_jungle",
            "bamboo_jungle", "bamboo_jungle_hills"),
    WOODS("wolf_woods", "forest", "forest_hills"),
    CHESTNUT("wolf_chestnut", "mutated_redwood_taiga", "mutated_redwood_taiga_hills", "old_growth_spruce_taiga"),
    STRIPED("wolf_striped", "mesa", "mesa_rock", "mesa_clear_rock", "mutated_mesa", "mutated_mesa_rock",
            "mutated_mesa_clear_rock", "badlands", "wooded_badlands", "eroded_badlands");

    private static final Map<String, WolfVariant> BY_BIOME = new HashMap<String, WolfVariant>();
    private static final Map<String, WolfVariant> BY_ID = new HashMap<String, WolfVariant>();

    static {
        for (WolfVariant v : values()) {
            for (String b : v.biomes) {
                BY_BIOME.put(b, v);
            }
            BY_ID.put(v.id(), v);
        }
    }

    /** The texture stem under {@code textures/entity/wolf/}: 1.20's {@code wolf}, {@code wolf_ashen}, ... */
    public final String texture;
    private final String[] biomes;

    WolfVariant(String texture, String... biomes) {
        this.texture = texture;
        this.biomes = biomes;
    }

    /** @return 1.20's id, {@code minecraft:pale} and so on, as its {@code variant} tag stores it */
    public String id() {
        return "minecraft:" + name().toLowerCase(Locale.ROOT);
    }

    /** @return the variant for a saved id, or null for an unknown one */
    @Nullable
    public static WolfVariant byId(String id) {
        return BY_ID.get(id);
    }

    /** @return the variant by ordinal (the packet's form), pale when out of range */
    public static WolfVariant byOrdinal(int i) {
        WolfVariant[] all = values();
        return i >= 0 && i < all.length ? all[i] : PALE;
    }

    /** @return the variant a biome registry path chooses, or null (vanilla then gives pale) */
    @Nullable
    public static WolfVariant forBiomePath(String path) {
        return BY_BIOME.get(path);
    }

    /** 1.20.5's {@code WolfVariants.getSpawnVariant}: the biome's variant, else pale. */
    public static WolfVariant forBiome(@Nullable Biome biome) {
        if (biome == null || biome.getRegistryName() == null) {
            return PALE;
        }
        WolfVariant v = forBiomePath(biome.getRegistryName().getPath());
        return v == null ? PALE : v;
    }
}
