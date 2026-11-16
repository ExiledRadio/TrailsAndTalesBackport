// Copied from CavesAndCliffsBackport/src/main/java/com/exiledradio/cavesandcliffsbackport/terrain/core/DoubleNoise.java by tools/smithing/copy_from_owner.py; edit the owner's file, not this one.
package com.exiledradio.trailsandtalesbackport.smithing.copy.noise;

/** 1.18's NormalNoise: two octave stacks, the second sampled slightly scaled. */
public final class DoubleNoise {
    private static final double INPUT_FACTOR = 1.0181268882175227D;

    private final OctaveNoise first;
    private final OctaveNoise second;
    private final double valueFactor;
    private final double maxValue;

    /** NormalNoise.create over a LegacyRandomSource (GeodeFeature). */
    public DoubleNoise(LegacyRandom random, int firstOctave, double... amplitudes) {
        this(OctaveNoise.create(random, firstOctave, amplitudes), OctaveNoise.create(random, firstOctave, amplitudes), amplitudes);
    }

    private DoubleNoise(OctaveNoise first, OctaveNoise second, double[] amplitudes) {
        this.first = first;
        this.second = second;
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < amplitudes.length; i++) {
            if (amplitudes[i] != 0.0D) {
                min = Math.min(min, i);
                max = Math.max(max, i);
            }
        }
        valueFactor = 0.16666666666666666D / (0.1D * (1.0D + 1.0D / (double) (max - min + 1)));
        maxValue = (first.maxValue() + second.maxValue()) * valueFactor;
    }

    public double getValue(double x, double y, double z) {
        double x2 = x * INPUT_FACTOR;
        double y2 = y * INPUT_FACTOR;
        double z2 = z * INPUT_FACTOR;
        return (first.getValue(x, y, z) + second.getValue(x2, y2, z2)) * valueFactor;
    }

    public double maxValue() {
        return maxValue;
    }
}
