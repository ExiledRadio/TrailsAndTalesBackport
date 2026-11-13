package com.exiledradio.trailsandtalesbackport.armadillo;

import net.minecraft.entity.EnumCreatureType;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Where armadillos spawn: 1.20.5's biome files, the one place this stream decides it.
 *
 * <ul>
 *   <li>Savanna, savanna plateau and windswept savanna: weight 10, groups of 2-3 (with the other animals).</li>
 *   <li>Badlands, wooded badlands and eroded badlands: weight 6, groups of 1-2 (the only animal there).</li>
 * </ul>
 *
 * Biomes are matched by registry path in any namespace, under 1.12.2's names (savanna_rock is the savanna
 * plateau, mutated_* the shattered savannas and the bryce and plateau badlands, all of which 1.18 merged into
 * those six) and under 1.18's, so 1.18-style terrain from another mod gets them with no code of its own. A
 * terrain that registers biomes after init, or under other names, calls {@link #addSpawns(Biome)} or reads
 * {@link #forBiomePath(String)}.
 */
public final class ArmadilloSpawns {

    /** {weight, min, max} */
    public static final int[] SAVANNA = {10, 2, 3};
    public static final int[] BADLANDS = {6, 1, 2};

    private static final Map<String, int[]> BY_PATH = new HashMap<String, int[]>();

    static {
        for (String path : new String[] {"savanna", "savanna_rock", "mutated_savanna", "mutated_savanna_rock",
                "savanna_plateau", "windswept_savanna"}) {
            BY_PATH.put(path, SAVANNA);
        }
        for (String path : new String[] {"mesa", "mesa_rock", "mesa_clear_rock", "mutated_mesa", "mutated_mesa_rock",
                "mutated_mesa_clear_rock", "badlands", "wooded_badlands", "eroded_badlands"}) {
            BY_PATH.put(path, BADLANDS);
        }
    }

    private ArmadilloSpawns() {
    }

    /** @return {weight, min, max} for a biome registry path (1.12.2 or 1.18 name), or null where none spawn */
    @Nullable
    public static int[] forBiomePath(String path) {
        return BY_PATH.get(path);
    }

    /** Adds armadillos to every registered biome they spawn in; called from init. */
    public static void addAll() {
        for (Biome biome : ForgeRegistries.BIOMES) {
            addSpawns(biome);
        }
    }

    /** Adds armadillos to this biome's creature list if it is one they spawn in, once. */
    public static void addSpawns(Biome biome) {
        if (!ArmadilloContent.enabled() || biome.getRegistryName() == null) {
            return;
        }
        int[] spawn = forBiomePath(biome.getRegistryName().getPath());
        if (spawn == null) {
            return;
        }
        for (Biome.SpawnListEntry e : biome.getSpawnableList(EnumCreatureType.CREATURE)) {
            if (e.entityClass == EntityArmadillo.class) {
                return;
            }
        }
        biome.getSpawnableList(EnumCreatureType.CREATURE).add(new Biome.SpawnListEntry(EntityArmadillo.class, spawn[0], spawn[1], spawn[2]));
    }
}
