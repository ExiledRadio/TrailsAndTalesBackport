// Copied from CavesAndCliffsBackport/src/main/java/com/exiledradio/cavesandcliffsbackport/terrain/core/OctaveNoise.java by tools/smithing/copy_from_owner.py; edit the owner's file, not this one.
package com.exiledradio.trailsandtalesbackport.smithing.copy.noise;

/** 1.18's PerlinNoise: a stack of octaves. */
public final class OctaveNoise {
    private static final double ROUND_OFF = 3.3554432E7D;
    /** Below this, wrap() is the identity (the floor it takes is 0), with a wide safety margin. */
    private static final double NO_WRAP = 1.0E7D;

    /** By octave slot; null where the amplitude is zero. */
    private final ImprovedNoise[] levels;
    private final double[] amplitudes;
    private final double lowestFreqInputFactor;
    private final double lowestFreqValueFactor;
    private final double maxValue;

    // The non-null octaves, packed, for getValue.
    private final ImprovedNoise[] used;
    private final double[] usedAmplitude;
    private final double[] usedInputFactor;
    private final double[] usedValueFactor;

    private OctaveNoise(ImprovedNoise[] levels, double[] amplitudes, int firstOctave) {
        this.levels = levels;
        this.amplitudes = amplitudes;
        int count = amplitudes.length;
        int j = -firstOctave;
        lowestFreqInputFactor = Math.pow(2.0D, (double) (-j));
        lowestFreqValueFactor = Math.pow(2.0D, (double) (count - 1)) / (Math.pow(2.0D, (double) count) - 1.0D);
        int n = 0;
        for (ImprovedNoise level : levels) {
            if (level != null) {
                n++;
            }
        }
        used = new ImprovedNoise[n];
        usedAmplitude = new double[n];
        usedInputFactor = new double[n];
        usedValueFactor = new double[n];
        double d1 = lowestFreqInputFactor;
        double d2 = lowestFreqValueFactor;
        n = 0;
        for (int i = 0; i < count; i++) {
            if (levels[i] != null) {
                used[n] = levels[i];
                usedAmplitude[n] = amplitudes[i];
                usedInputFactor[n] = d1;
                usedValueFactor[n] = d2;
                n++;
            }
            d1 *= 2.0D;
            d2 /= 2.0D;
        }
        maxValue = edgeValue(2.0D);
    }

    /**
     * PerlinNoise.create over a LegacyRandomSource: its positional factory is seeded by one nextLong, each octave
     * by ("octave_" + octave).hashCode() ^ that seed.
     */
    public static OctaveNoise create(LegacyRandom random, int firstOctave, double[] amplitudes) {
        ImprovedNoise[] levels = new ImprovedNoise[amplitudes.length];
        long seed = random.nextLong();
        for (int k = 0; k < amplitudes.length; k++) {
            if (amplitudes[k] != 0.0D) {
                levels[k] = new ImprovedNoise(new LegacyRandom((long) ("octave_" + (firstOctave + k)).hashCode() ^ seed));
            }
        }
        return new OctaveNoise(levels, amplitudes, firstOctave);
    }

    public static double wrap(double v) {
        if (v < NO_WRAP && v > -NO_WRAP) {
            return v;
        }
        double d = v / ROUND_OFF + 0.5D;
        long i = (long) d;
        long floor = d < (double) i ? i - 1L : i;
        return v - (double) floor * ROUND_OFF;
    }

    public double getValue(double x, double y, double z) {
        double sum = 0.0D;
        ImprovedNoise[] used = this.used;
        for (int i = 0; i < used.length; i++) {
            double f = usedInputFactor[i];
            double v = used[i].noise(wrap(x * f), wrap(y * f), wrap(z * f));
            sum += usedAmplitude[i] * v * usedValueFactor[i];
        }
        return sum;
    }

    /** PerlinNoise.getOctaveNoise: octaves counted from the highest frequency down. */
    public ImprovedNoise octave(int fromTop) {
        return levels[levels.length - 1 - fromTop];
    }

    public double maxValue() {
        return maxValue;
    }

    public double maxBrokenValue(double yScale) {
        return edgeValue(yScale + 2.0D);
    }

    private double edgeValue(double v) {
        double sum = 0.0D;
        double d1 = lowestFreqValueFactor;
        for (int i = 0; i < levels.length; i++) {
            if (levels[i] != null) {
                sum += amplitudes[i] * v * d1;
            }
            d1 /= 2.0D;
        }
        return sum;
    }
}
