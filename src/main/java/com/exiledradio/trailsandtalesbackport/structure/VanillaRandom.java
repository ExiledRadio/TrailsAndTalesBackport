package com.exiledradio.trailsandtalesbackport.structure;

import net.minecraft.util.math.BlockPos;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 1.20's random helpers, draw for draw. {@code LegacyRandomSource} is {@link java.util.Random}'s generator (same seed
 * scrambling, {@code next}, {@code nextInt}, {@code nextLong}, {@code nextFloat}), so a plain {@code Random} stands in.
 */
public final class VanillaRandom {

    private VanillaRandom() {
    }

    /** {@code Mth.getSeed(x, y, z)}. */
    public static long seed(int x, int y, int z) {
        long l = (long) (x * 3129871) ^ (long) z * 116129781L ^ (long) y;
        l = l * l * 42317861L + l * 11L;
        return l >> 16;
    }

    public static long seed(BlockPos pos) {
        return seed(pos.getX(), pos.getY(), pos.getZ());
    }

    /** {@code RandomSource.create(Mth.getSeed(pos))}: a rule processor's random. */
    public static Random at(BlockPos pos) {
        return new Random(seed(pos));
    }

    /** {@code RandomSource.create(worldSeed).forkPositional().at(pos)}. */
    public static Random positional(long worldSeed, BlockPos pos) {
        long factory = new Random(worldSeed).nextLong();
        return new Random(seed(pos) ^ factory);
    }

    /** {@code Util.shuffle} (and {@code Util.toShuffledList}): n - 1 draws, the same as {@link Collections#shuffle}. */
    public static <T> void shuffle(List<T> list, Random random) {
        Collections.shuffle(list, random);
    }

    /** 1.20's {@code BlockPos.asLong}: x in the top 26 bits, then z (26), then y (12); 1.12.2's toLong orders them x, y, z. */
    public static long asLong(BlockPos pos) {
        return ((long) pos.getX() & 0x3FFFFFFL) << 38 | ((long) pos.getY() & 0xFFFL) | ((long) pos.getZ() & 0x3FFFFFFL) << 12;
    }

    /** {@code RandomSource.nextIntBetweenInclusive}. */
    public static int between(Random random, int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }
}
