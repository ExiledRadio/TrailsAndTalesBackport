// Copied from CavesAndCliffsBackport/src/main/java/com/exiledradio/cavesandcliffsbackport/terrain/core/ImprovedNoise.java by tools/smithing/copy_from_owner.py; edit the owner's file, not this one.
package com.exiledradio.trailsandtalesbackport.smithing.copy.noise;

/**
 * 1.18's ImprovedNoise (one Perlin octave). Same arithmetic in the same order, so results are bit-identical;
 * what differs is how the work is laid out (a doubled permutation table, gradients by switch).
 */
public final class ImprovedNoise {
    public final double xo;
    public final double yo;
    public final double zo;
    /** The permutation, doubled plus one, so {@code p[p[i] + j]} needs no wrap for j in 0..256. */
    private final int[] p = new int[514];

    /** The same from a LegacyRandomSource (GeodeFeature's noise). */
    public ImprovedNoise(LegacyRandom random) {
        xo = random.nextDouble() * 256.0D;
        yo = random.nextDouble() * 256.0D;
        zo = random.nextDouble() * 256.0D;
        byte[] t = new byte[256];
        for (int i = 0; i < 256; i++) {
            t[i] = (byte) i;
        }
        for (int k = 0; k < 256; k++) {
            int j = random.nextInt(256 - k);
            byte b = t[k];
            t[k] = t[k + j];
            t[k + j] = b;
        }
        for (int i = 0; i < 514; i++) {
            p[i] = t[i & 255] & 255;
        }
    }

    private static int floor(double v) {
        int i = (int) v;
        return v < (double) i ? i - 1 : i;
    }

    /** noise(x, y, z, 0, 0). */
    public double noise(double x, double y, double z) {
        double d0 = x + xo;
        double d1 = y + yo;
        double d2 = z + zo;
        int i = floor(d0);
        int j = floor(d1);
        int k = floor(d2);
        double fx = d0 - (double) i;
        double fy = d1 - (double) j;
        double fz = d2 - (double) k;
        return sampleAndLerp(i, j, k, fx, fy, fz, fy);
    }

    /** The legacy form the blended terrain noise uses: y snapped to steps of yScale. */
    public double noise(double x, double y, double z, double yScale, double yMax) {
        double d0 = x + xo;
        double d1 = y + yo;
        double d2 = z + zo;
        int i = floor(d0);
        int j = floor(d1);
        int k = floor(d2);
        double fx = d0 - (double) i;
        double fy = d1 - (double) j;
        double fz = d2 - (double) k;
        double snap;
        if (yScale != 0.0D) {
            double d7 = yMax >= 0.0D && yMax < fy ? yMax : fy;
            snap = (double) floor(d7 / yScale + (double) 1.0E-7F) * yScale;
        } else {
            snap = 0.0D;
        }
        return sampleAndLerp(i, j, k, fx, fy - snap, fz, fy);
    }

    /** SimplexNoise.GRADIENT as doubles: x, y, z per hash. */
    private static final double[] GX = {1, -1, 1, -1, 1, -1, 1, -1, 0, 0, 0, 0, 1, 0, -1, 0};
    private static final double[] GY = {1, 1, -1, -1, 0, 0, 0, 0, 1, -1, 1, -1, 1, -1, 1, -1};
    private static final double[] GZ = {0, 0, 0, 0, 1, 1, -1, -1, 1, 1, -1, -1, 0, 1, 0, -1};

    private static double grad(int hash, double x, double y, double z) {
        int h = hash & 15;
        return GX[h] * x + GY[h] * y + GZ[h] * z;
    }

    private double sampleAndLerp(int x, int y, int z, double fx, double fy, double fz, double fySmooth) {
        int[] p = this.p;
        int xi = x & 255;
        int yi = y & 255;
        int zi = z & 255;
        int i = p[xi];
        int j = p[xi + 1];
        int k = p[i + yi];
        int l = p[i + yi + 1];
        int i1 = p[j + yi];
        int j1 = p[j + yi + 1];
        double d0 = grad(p[k + zi], fx, fy, fz);
        double d1 = grad(p[i1 + zi], fx - 1.0D, fy, fz);
        double d2 = grad(p[l + zi], fx, fy - 1.0D, fz);
        double d3 = grad(p[j1 + zi], fx - 1.0D, fy - 1.0D, fz);
        double d4 = grad(p[k + zi + 1], fx, fy, fz - 1.0D);
        double d5 = grad(p[i1 + zi + 1], fx - 1.0D, fy, fz - 1.0D);
        double d6 = grad(p[l + zi + 1], fx, fy - 1.0D, fz - 1.0D);
        double d7 = grad(p[j1 + zi + 1], fx - 1.0D, fy - 1.0D, fz - 1.0D);
        double sx = fx * fx * fx * (fx * (fx * 6.0D - 15.0D) + 10.0D);
        double sy = fySmooth * fySmooth * fySmooth * (fySmooth * (fySmooth * 6.0D - 15.0D) + 10.0D);
        double sz = fz * fz * fz * (fz * (fz * 6.0D - 15.0D) + 10.0D);
        // Mth.lerp3(sx, sy, sz, d0..d7)
        double a = d0 + sx * (d1 - d0);
        double b = d2 + sx * (d3 - d2);
        double c = d4 + sx * (d5 - d4);
        double d = d6 + sx * (d7 - d6);
        double ab = a + sy * (b - a);
        double cd = c + sy * (d - c);
        return ab + sz * (cd - ab);
    }
}
