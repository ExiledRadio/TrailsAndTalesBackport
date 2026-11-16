// Copied from CavesAndCliffsBackport/src/main/java/com/exiledradio/cavesandcliffsbackport/terrain/core/LegacyRandom.java by tools/smithing/copy_from_owner.py; edit the owner's file, not this one.
package com.exiledradio.trailsandtalesbackport.smithing.copy.noise;

/**
 * java.util.Random's 48-bit LCG and its methods, bit for bit, without the synchronization: what 1.18.2's
 * LegacyRandomSource and a WorldgenRandom over it compute (WorldgenRandom extends Random and only overrides next),
 * plus WorldgenRandom's seeding helpers.
 */
public final class LegacyRandom {
    private static final long MULTIPLIER = 0x5DEECE66DL;
    private static final long MASK = (1L << 48) - 1;

    private long seed;

    public LegacyRandom(long seed) {
        setSeed(seed);
    }

    public void setSeed(long seed) {
        this.seed = (seed ^ MULTIPLIER) & MASK;
    }

    public int next(int bits) {
        seed = seed * MULTIPLIER + 0xBL & MASK;
        return (int) (seed >>> (48 - bits));
    }

    public int nextInt() {
        return next(32);
    }

    public int nextInt(int bound) {
        int r = next(31);
        int m = bound - 1;
        if ((bound & m) == 0) {
            return (int) ((long) bound * (long) r >> 31);
        }
        for (int u = r; u - (r = u % bound) + m < 0; u = next(31)) {
        }
        return r;
    }

    public long nextLong() {
        return ((long) next(32) << 32) + (long) next(32);
    }

    public boolean nextBoolean() {
        return next(1) != 0;
    }

    public float nextFloat() {
        return (float) next(24) / (float) (1 << 24);
    }

    public double nextDouble() {
        return (double) (((long) next(26) << 27) + (long) next(27)) * 0x1.0p-53;
    }

    /** Mth.randomBetweenInclusive. */
    public int between(int min, int max) {
        return nextInt(max - min + 1) + min;
    }

    /** Mth.randomBetween (UniformFloat.sample). */
    public float between(float min, float max) {
        return nextFloat() * (max - min) + min;
    }

    /** WorldgenRandom.setLargeFeatureSeed. */
    public void setLargeFeatureSeed(long worldSeed, int x, int z) {
        setSeed(worldSeed);
        long i = nextLong();
        long j = nextLong();
        setSeed((long) x * i ^ (long) z * j ^ worldSeed);
    }

    /** WorldgenRandom.setDecorationSeed: returns the seed set. */
    public long setDecorationSeed(long worldSeed, int x, int z) {
        setSeed(worldSeed);
        long i = nextLong() | 1L;
        long j = nextLong() | 1L;
        long k = (long) x * i + (long) z * j ^ worldSeed;
        setSeed(k);
        return k;
    }

    /** WorldgenRandom.setFeatureSeed. */
    public void setFeatureSeed(long decorationSeed, int index, int step) {
        setSeed(decorationSeed + (long) index + (long) (10000 * step));
    }

    /** WorldgenRandom.setLargeFeatureWithSalt. */
    public void setLargeFeatureWithSalt(long worldSeed, int x, int z, int salt) {
        setSeed((long) x * 341873128712L + (long) z * 132897987541L + worldSeed + (long) salt);
    }
}
