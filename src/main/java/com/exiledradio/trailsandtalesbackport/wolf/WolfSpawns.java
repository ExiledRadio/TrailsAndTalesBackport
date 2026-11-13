package com.exiledradio.trailsandtalesbackport.wolf;

import net.minecraft.block.BlockDirt;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Where wolves spawn: 1.20.5's biome builders ({@code OverworldBiomes}) and spawn rule.
 *
 * <ul>
 *   <li>1.12.2 already has 1.20's wolves in every taiga (8, 4-4) and the forest (5, 4-4). 1.20 adds the savanna
 *       plateau (8, 4-8), the sparse jungle - 1.12.2's jungle edge (8, 2-4) - and the wooded badlands - 1.12.2's
 *       mesa plateau F (2, 4-8); the grove (1, 1-1) is Caves &amp; Cliffs' biome and gets its wolves when it has
 *       none. A biome with 1.18's name gets 1.20's entry too when it has no wolves, so 1.18-style terrain needs no
 *       code of its own. Others call {@link #addSpawns(Biome)} or read {@link #forBiomePath(String)}.</li>
 *   <li>The spawn rule is 1.20.5's {@code checkWolfSpawnRules}: on {@code #wolves_spawnable_on} (grass, snow,
 *       snow blocks, coarse dirt, podzol) with a raw light above 8.</li>
 * </ul>
 */
public final class WolfSpawns {

    /** {weight, min, max} */
    public static final int[] TAIGA = {8, 4, 4};
    public static final int[] FOREST = {5, 4, 4};
    public static final int[] SAVANNA_PLATEAU = {8, 4, 8};
    public static final int[] SPARSE_JUNGLE = {8, 2, 4};
    public static final int[] WOODED_BADLANDS = {2, 4, 8};
    public static final int[] GROVE = {1, 1, 1};

    private static final Map<String, int[]> BY_PATH = new HashMap<String, int[]>();

    static {
        for (String p : new String[] {"taiga", "snowy_taiga", "old_growth_pine_taiga", "old_growth_spruce_taiga"}) {
            BY_PATH.put(p, TAIGA);
        }
        BY_PATH.put("forest", FOREST);
        for (String p : new String[] {"savanna_rock", "savanna_plateau"}) {
            BY_PATH.put(p, SAVANNA_PLATEAU);
        }
        for (String p : new String[] {"jungle_edge", "mutated_jungle_edge", "sparse_jungle"}) {
            BY_PATH.put(p, SPARSE_JUNGLE);
        }
        for (String p : new String[] {"mesa_rock", "mutated_mesa_rock", "wooded_badlands"}) {
            BY_PATH.put(p, WOODED_BADLANDS);
        }
        BY_PATH.put("grove", GROVE);
    }

    private WolfSpawns() {
    }

    /** @return {weight, min, max} for a biome path, or null where 1.20 adds none to 1.12.2's */
    @Nullable
    public static int[] forBiomePath(String path) {
        return BY_PATH.get(path);
    }

    public static void addAll() {
        for (Biome biome : ForgeRegistries.BIOMES) {
            addSpawns(biome);
        }
    }

    /** Adds 1.20's wolves to this biome when it is one of 1.20's wolf biomes and has no wolves yet. */
    public static void addSpawns(Biome biome) {
        if (!WolfContent.enabled() || biome.getRegistryName() == null) {
            return;
        }
        int[] spawn = forBiomePath(biome.getRegistryName().getPath());
        if (spawn == null) {
            return;
        }
        for (Biome.SpawnListEntry e : biome.getSpawnableList(EnumCreatureType.CREATURE)) {
            if (e.entityClass == EntityWolf.class) {
                return;
            }
        }
        biome.getSpawnableList(EnumCreatureType.CREATURE).add(new Biome.SpawnListEntry(EntityWolf.class, spawn[0], spawn[1], spawn[2]));
    }

    /** 1.20.5's {@code #wolves_spawnable_on}. */
    public static boolean isSpawnableOn(IBlockState state) {
        if (state.getBlock() == Blocks.GRASS || state.getBlock() == Blocks.SNOW_LAYER || state.getBlock() == Blocks.SNOW) {
            return true;
        }
        if (state.getBlock() == Blocks.DIRT) {
            BlockDirt.DirtType t = state.getValue(BlockDirt.VARIANT);
            return t == BlockDirt.DirtType.COARSE_DIRT || t == BlockDirt.DirtType.PODZOL;
        }
        return false;
    }

    /** 1.20.5's {@code checkWolfSpawnRules}. */
    public static boolean canSpawnAt(World world, BlockPos pos) {
        return isSpawnableOn(world.getBlockState(pos.down())) && world.getLight(pos) > 8;
    }
}
