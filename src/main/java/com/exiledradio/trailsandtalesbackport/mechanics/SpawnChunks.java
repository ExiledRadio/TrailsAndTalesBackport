package com.exiledradio.trailsandtalesbackport.mechanics;

import com.exiledradio.trailsandtalesbackport.ModConfig;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 1.20.5's spawn chunks (24w03a's {@code spawnChunkRadius}, default 2), patch P10. 1.12.2 keeps every chunk whose
 * center is within 128 blocks of the world spawn loaded (17 by 17 chunks, {@code World.isSpawnChunk}, asked by
 * {@code WorldProviderSurface.canDropChunk}) and prepares 25 by 25 chunks when the server starts
 * ({@code MinecraftServer.initialWorldChunkLoad}). 1.20.5 keeps the chunks within {@link #radius} chunks of the
 * spawn's chunk (5 by 5) and prepares that same area.
 *
 * <p>{@link #radius} is the one value: 1.21.9 removed spawn chunks entirely, and Tricky Trials Backport, when it is
 * installed, sets it to 0, which keeps none and prepares none.
 */
public final class SpawnChunks {

    /** In chunks around the spawn's chunk; 0 or less keeps none. Another backport may change it. */
    public static volatile int radius = 2;

    private SpawnChunks() {
    }

    public static boolean enabled() {
        return ModConfig.mechanics.spawnChunks;
    }

    /**
     * {@link #radius}, or, while it is above 0, the overworld's {@code spawnChunkRadius} game rule (1.20.5, added by
     * {@link NewGameRules}). A radius of 0 set by another backport wins over the rule.
     */
    public static int effectiveRadius() {
        int r = radius;
        return r > 0 ? NewGameRules.spawnChunkRadius(r) : r;
    }

    /** Replaces the body of {@code World.isSpawnChunk(x, z)}. */
    public static boolean isSpawnChunk(World world, int x, int z) {
        BlockPos spawn = world.getSpawnPoint();
        if (!enabled()) {
            int dx = x * 16 + 8 - spawn.getX();
            int dz = z * 16 + 8 - spawn.getZ();
            return dx >= -128 && dx <= 128 && dz >= -128 && dz <= 128;
        }
        int r = effectiveRadius();
        return r > 0 && Math.abs(x - (spawn.getX() >> 4)) <= r && Math.abs(z - (spawn.getZ() >> 4)) <= r;
    }

    /** {@code initialWorldChunkLoad}'s 192: how far, in blocks, the start area reaches (below the start: none). */
    public static int prepareBlocks() {
        if (!enabled()) {
            return 192;
        }
        int r = effectiveRadius();
        return r > 0 ? r * 16 : -16;
    }

    /** {@code initialWorldChunkLoad}'s -192, where the loops start. */
    public static int prepareStart() {
        return -prepareBlocks();
    }

    /** {@code initialWorldChunkLoad}'s 625, the chunk count its progress is out of. */
    public static int prepareCount() {
        if (!enabled()) {
            return 625;
        }
        int side = 2 * Math.max(effectiveRadius(), 0) + 1;
        return side * side;
    }
}
