package com.exiledradio.trailsandtalesbackport.selftest;

import com.exiledradio.backportcore.structure.Structures;
import com.exiledradio.backportcore.worldgen.FlatWorlds;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.biome.CherryGroveContent;
import com.exiledradio.trailsandtalesbackport.camel.EntityCamel;
import com.exiledradio.trailsandtalesbackport.cherry.CherryContent;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Biomes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.gen.FlatGeneratorInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Superflat worlds generate only what vanilla 1.12 does for their preset (Backport Core's {@link FlatWorlds}).
 *
 * <p>In the suite's own (Default) world this only checks the gates are open. In a real superflat world
 * ({@code sh tools/flat/selftest.sh <preset>}) it scans the chunks around spawn: no block of this mod unless the
 * preset has {@code decoration} and its biome is the cherry grove (then trees and petals); and a camel at a
 * desert village only when the preset has villages, as 1.12's flat villages get their villagers. Nothing here
 * places a structure, so there are none to look for: the gates are the decorator (only with {@code decoration})
 * and the camel's village.
 */
final class FlatSelfTest implements SelfTest {

    /** Chunks around spawn (and around the village) that are generated and scanned. */
    private static final int RADIUS = 5;

    @Override
    public String name() {
        return "flat";
    }

    @Override
    public void run(Check c) {
        WorldServer world = c.world;
        if (!FlatWorlds.isFlat(world)) {
            c.ok(FlatWorlds.decoration(world) && FlatWorlds.structure(world, Structures.VILLAGE),
                    "not superflat: decoration and villages (and so village camels) are on");
            return;
        }
        FlatGeneratorInfo info = FlatWorlds.info(world);
        String options = world.getWorldInfo().getGeneratorOptions();
        boolean decoration = FlatWorlds.decoration(world);
        boolean villages = FlatWorlds.structure(world, Structures.VILLAGE);
        c.eq(options.contains("decoration"), decoration, "decoration as the preset says (" + options + ")");
        c.eq(options.contains("village"), villages, "villages as the preset says");

        BlockPos spawn = world.getSpawnPoint();
        Biome biome = world.getBiome(spawn);
        Biome grove = CherryGroveContent.biome;
        boolean isGrove = grove != null && biome == grove;
        TrailsAndTalesBackport.LOGGER.info("SELFTEST flat biome {} (id {}), the grove's id {}", biome.getRegistryName(),
                Biome.getIdForBiome(biome), grove == null ? "none" : Biome.getIdForBiome(grove));
        c.eq(info.getBiome(), Biome.getIdForBiome(biome), "spawn stands in the preset's biome");

        // The mod's blocks around spawn.
        int[] counts = scan(c, spawn);
        int ours = counts[0];
        int logs = counts[1];
        int petals = counts[2];
        TrailsAndTalesBackport.LOGGER.info("SELFTEST flat blocks of this mod: {} ({} cherry logs, {} pink petals)", ours, logs, petals);
        if (isGrove && decoration) {
            c.ok(logs > 0, "decorated grove: cherry trees (" + logs + " logs)");
            c.ok(petals > 0, "decorated grove: pink petals (" + petals + ")");
        } else {
            c.eq(0, ours, "no block of this mod (" + (isGrove ? "grove without decoration" : biome.getRegistryName()) + ")");
        }

        // Camels: a desert village's own, so only with the preset's villages.
        BlockPos village = world.findNearestStructure("Village", spawn, false);
        TrailsAndTalesBackport.LOGGER.info("SELFTEST flat nearest village {}", village);
        c.ok(villages || village == null, "no village without the village option");
        if (village != null && village.distanceSq(spawn) < 2048 * 2048) {
            scan(c, village);
            int villagers = count(world, village, EntityVillager.class);
            int camels = count(world, village, EntityCamel.class);
            TrailsAndTalesBackport.LOGGER.info("SELFTEST flat village at {}: {} villagers, {} camels", village, villagers, camels);
            c.ok(villagers > 0, "the village was built (" + villagers + " villagers)");
            if (biome == Biomes.DESERT) {
                c.eq(1, camels, "a desert village has its camel");
            } else {
                c.eq(0, camels, "no camel at a village that is not a desert one");
            }
        }

        // Nothing else of this mod's lives in the world at generation.
        List<String> entities = new ArrayList<String>();
        for (Entity e : world.loadedEntityList) {
            ResourceLocation id = EntityList.getKey(e);
            if (id != null && TrailsAndTalesBackport.MOD_ID.equals(id.getNamespace()) && !(e instanceof EntityCamel)) {
                entities.add(id + " at " + new BlockPos(e));
            }
        }
        c.ok(entities.isEmpty(), "no other entity of this mod at generation: " + entities);
        if (!villages) {
            c.eq(0, count(world, null, EntityCamel.class), "no camels without villages");
        }
    }

    /** Generates and scans the chunks around {@code center}: {all blocks of this mod, cherry logs, pink petals}. */
    private static int[] scan(Check c, BlockPos center) {
        WorldServer world = c.world;
        int cx = center.getX() >> 4;
        int cz = center.getZ() >> 4;
        // One further out so each scanned chunk is populated.
        for (int x = cx - RADIUS - 1; x <= cx + RADIUS + 1; x++) {
            for (int z = cz - RADIUS - 1; z <= cz + RADIUS + 1; z++) {
                world.getChunkProvider().provideChunk(x, z);
            }
        }
        Block log = CherryContent.set == null ? null : CherryContent.set.log;
        Block petals = CherryContent.petals;
        int[] counts = new int[3];
        for (int x = cx - RADIUS; x <= cx + RADIUS; x++) {
            for (int z = cz - RADIUS; z <= cz + RADIUS; z++) {
                Chunk chunk = world.getChunk(x, z);
                for (ExtendedBlockStorage storage : chunk.getBlockStorageArray()) {
                    if (storage == null || storage.isEmpty()) {
                        continue;
                    }
                    for (int i = 0; i < 4096; i++) {
                        Block b = storage.get(i & 15, i >> 8, (i >> 4) & 15).getBlock();
                        ResourceLocation id = b.getRegistryName();
                        if (id != null && TrailsAndTalesBackport.MOD_ID.equals(id.getNamespace())) {
                            counts[0]++;
                            counts[1] += b == log ? 1 : 0;
                            counts[2] += b == petals ? 1 : 0;
                        }
                    }
                }
            }
        }
        return counts;
    }

    /** Entities of a class within 96 blocks of {@code center}, or anywhere when it is null. */
    private static int count(WorldServer world, BlockPos center, Class<? extends Entity> type) {
        int n = 0;
        for (Entity e : world.loadedEntityList) {
            if (type.isInstance(e) && (center == null || e.getDistanceSq(center) < 96 * 96)) {
                n++;
            }
        }
        return n;
    }
}
