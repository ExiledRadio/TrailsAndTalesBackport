// Copied from CavesAndCliffsBackport/src/main/java/com/exiledradio/cavesandcliffsbackport/amethyst/GeodeGenerator.java by tools/smithing/copy_from_owner.py; edit the owner's file, not this one.
package com.exiledradio.trailsandtalesbackport.smithing.copy;

import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.common.Owners;
import com.exiledradio.trailsandtalesbackport.smithing.copy.noise.DoubleNoise;
import com.exiledradio.trailsandtalesbackport.smithing.copy.noise.LegacyRandom;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.IWorldGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Amethyst geodes in 1.12 worlds, 1.17's settings within 1.12's height: one in {@code geodeRarity} chunks (24),
 * between {@code geodeMinY} and {@code geodeMaxY} (6-46).
 *
 * <h2>Shape: 1.18.2's GeodeFeature with AMETHYST_GEODE's configuration</h2>
 * Three or four points 4-6 blocks from a corner on each axis, each with an offset of 1-2. For every block in
 * reach, {@code s} is the sum over the points of {@code fastInvSqrt(distance^2 + offset) + n}, where {@code n} is
 * the geode noise there (a NormalNoise on the world seed, first octave -4) times 0.05, so a geode swells or
 * shrinks with the noise around it. Thresholds from the layer sizes (filling 1.7, inner 2.2, middle 3.2, outer
 * 4.2, each as {@code 1 / sqrt(size)}, the last three widened by points / 6) pick air, amethyst (budding 8.3% of
 * the time), calcite or smooth basalt. 95% of geodes get a crack: three points (7, 5 and 1 over the corner) out
 * one side, at the far corner or at the corner itself, whose own sum opens the shell to the air inside. Then 35%
 * of the budding blocks get a random bud or cluster on their first open face, in Direction order. This is the
 * code of the 1.18 terrain's geode ({@code terrain/core/Geodes}) on java.util.Random.
 *
 * <p>A geode aborts if more than one of its points lies in air, bedrock, water, lava or ice. It never replaces
 * bedrock, spawners, chests, end portal frames or tile entities, and it stays inside the population window, so
 * it never loads another chunk. Calcite and smooth basalt are stream D's; without them (stream off) no geodes
 * generate.
 */
public final class GeodeGenerator implements IWorldGenerator {

    private static final double FILLING = 1.7D, INNER = 2.2D, MIDDLE = 3.2D, OUTER = 4.2D;
    private static final double CRACK_CHANCE = 0.95D, BASE_CRACK = 2.0D, NOISE_MULTIPLIER = 0.05D;
    private static final int CRACK_OFFSET = 2;
    private static final double POTENTIAL_PLACEMENTS = 0.35D, ALTERNATE_LAYER0 = 0.083D;

    /** The geode noise of the last world seed asked for (it depends on the seed alone). */
    private static final class SeedNoise {
        final long seed;
        final DoubleNoise noise;
        /** A bound on |noise * 0.05|, with a margin, for skipping blocks that cannot reach the outer layer. */
        final double bound;

        SeedNoise(long seed) {
            this.seed = seed;
            this.noise = new DoubleNoise(new LegacyRandom(seed), -4, 1.0D);
            this.bound = noise.maxValue() * NOISE_MULTIPLIER * 1.000001D + 1.0E-9D;
        }
    }

    private static volatile SeedNoise last;

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator generator, IChunkProvider provider) {
        if (false) {
            return; // the 1.18 terrain places its own
        }
        if (!ModConfig.smithing.geodes || world.provider.getDimension() != 0
                || !com.exiledradio.backportcore.worldgen.FlatWorlds.decoration(world) // superflat: only with decoration, as 1.17's geodes
                || 24 <= 0 || random.nextInt(24) != 0) {
            return;
        }
        int lo = Math.max(1, 6);
        int hi = Math.max(lo + 1, 46);
        // The points sit 4-6 blocks past the corner and the shell reaches about 7 past them, so a geode spans
        // about corner-2 to corner+13: keep it inside the 32x32 population window [chunk*16, chunk*16+32).
        BlockPos corner = new BlockPos(chunkX * 16 + 7 + random.nextInt(8), lo + random.nextInt(hi - lo), chunkZ * 16 + 7 + random.nextInt(8));
        place(world, random, corner, chunkX * 16, chunkZ * 16);
    }

    private static SeedNoise noise(long seed) {
        SeedNoise n = last;
        if (n == null || n.seed != seed) {
            n = new SeedNoise(seed);
            last = n;
        }
        return n;
    }

    /** Mth.fastInvSqrt(double). */
    private static double fastInvSqrt(double x) {
        double half = 0.5D * x;
        long i = Double.doubleToRawLongBits(x);
        i = 6910469410427058090L - (i >> 1);
        x = Double.longBitsToDouble(i);
        return x * (1.5D - half * x * x);
    }

    private static double distSqr(int x, int y, int z, int ox, int oy, int oz) {
        double dx = (double) x - ox, dy = (double) y - oy, dz = (double) z - oz;
        return dx * dx + dy * dy + dz * dz;
    }

    /** UniformInt.of(min, max).sample(random). */
    private static int between(Random random, int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }

    /**
     * Places a geode at a corner, writing only inside {@code [minX, minX+32) x [minZ, minZ+32)}.
     *
     * @return true if it was placed
     */
    public static boolean place(World world, Random random, BlockPos corner, int minX, int minZ) {
        Block calcite = Owners.ourBlock("calcite");
        Block basalt = Owners.ourBlock("smooth_basalt");
        if (calcite == null || basalt == null || AmethystCopies.amethyst == null) {
            return false;
        }
        SeedNoise sn = noise(world.getSeed());
        DoubleNoise noise = sn.noise;
        int x = corner.getX(), y = corner.getY(), z = corner.getZ();
        int k = between(random, 3, 4);
        double d0 = (double) k / 6.0D;
        double d1 = 1.0D / Math.sqrt(FILLING);
        double d2 = 1.0D / Math.sqrt(INNER + d0);
        double d3 = 1.0D / Math.sqrt(MIDDLE + d0);
        double d4 = 1.0D / Math.sqrt(OUTER + d0);
        double d5 = 1.0D / Math.sqrt(BASE_CRACK + random.nextDouble() / 2.0D + (k > 3 ? d0 : 0.0D));
        boolean cracked = (double) random.nextFloat() < CRACK_CHANCE;
        int[] px = new int[k], py = new int[k], pz = new int[k], po = new int[k];
        int invalid = 0;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int i = 0; i < k; i++) {
            int ox = between(random, 4, 6), oy = between(random, 4, 6), oz = between(random, 4, 6);
            IBlockState s = world.getBlockState(m.setPos(x + ox, y + oy, z + oz));
            if ((s.getBlock().isAir(s, world, m) || invalidHere(s)) && ++invalid > 1) {
                return false;
            }
            px[i] = x + ox;
            py[i] = y + oy;
            pz[i] = z + oz;
            po[i] = between(random, 1, 2);
        }
        int cracks = 0;
        int[] cx = new int[3], cy = new int[3], cz = new int[3];
        if (cracked) {
            int side = random.nextInt(4), reach = k * 2 + 1;
            int ox = side == 0 || side == 2 ? reach : 0, oz = side == 1 || side == 2 ? reach : 0;
            int[] ys = {7, 5, 1};
            for (int c = 0; c < 3; c++) {
                cx[c] = x + ox;
                cy[c] = y + ys[c];
                cz[c] = z + oz;
            }
            cracks = 3;
        }
        IBlockState air = Blocks.AIR.getDefaultState();
        IBlockState amethyst = AmethystCopies.amethyst.getDefaultState();
        IBlockState budding = AmethystCopies.budding.getDefaultState();
        List<BlockPos> placements = new ArrayList<BlockPos>();
        // Vanilla's order (z, then y, then x): the budding and placement draws follow it.
        for (int bz = z - 16; bz <= z + 16; bz++) {
            if (bz < minZ || bz >= minZ + 32) {
                continue;
            }
            for (int by = Math.max(1, y - 16); by <= Math.min(254, y + 16); by++) {
                for (int bx = x - 16; bx <= x + 16; bx++) {
                    if (bx < minX || bx >= minX + 32) {
                        continue;
                    }
                    double base = 0.0D;
                    for (int p = 0; p < k; p++) {
                        base += fastInvSqrt(distSqr(bx, by, bz, px[p], py[p], pz[p]) + (double) po[p]);
                    }
                    if (base + k * sn.bound < d4) {
                        continue; // cannot reach the outer layer whatever the noise
                    }
                    double d8 = noise.getValue((double) bx, (double) by, (double) bz) * NOISE_MULTIPLIER;
                    double d6 = 0.0D, d7 = 0.0D;
                    for (int p = 0; p < k; p++) {
                        d6 += fastInvSqrt(distSqr(bx, by, bz, px[p], py[p], pz[p]) + (double) po[p]) + d8;
                    }
                    if (d6 < d4) {
                        continue;
                    }
                    for (int c = 0; c < cracks; c++) {
                        d7 += fastInvSqrt(distSqr(bx, by, bz, cx[c], cy[c], cz[c]) + (double) CRACK_OFFSET) + d8;
                    }
                    m.setPos(bx, by, bz);
                    if (cracked && d7 >= d5 && d6 < d1) {
                        safeSet(world, m, air);
                    } else if (d6 >= d1) {
                        safeSet(world, m, air);
                    } else if (d6 >= d2) {
                        boolean bud = (double) random.nextFloat() < ALTERNATE_LAYER0;
                        safeSet(world, m, bud ? budding : amethyst);
                        if (bud && (double) random.nextFloat() < POTENTIAL_PLACEMENTS) {
                            placements.add(m.toImmutable());
                        }
                    } else if (d6 >= d3) {
                        safeSet(world, m, calcite.getDefaultState());
                    } else {
                        safeSet(world, m, basalt.getDefaultState());
                    }
                }
            }
        }
        BlockAmethystCluster[] buds = AmethystCopies.stages;
        for (BlockPos b : placements) {
            BlockAmethystCluster bud = buds[random.nextInt(buds.length)];
            for (EnumFacing f : EnumFacing.values()) {
                BlockPos at = b.offset(f);
                if (at.getX() < minX || at.getX() >= minX + 32 || at.getZ() < minZ || at.getZ() >= minZ + 32) {
                    continue;
                }
                if (BlockAmethyst.Budding.canGrowInto(world.getBlockState(at))) {
                    safeSet(world, at, bud.getDefaultState().withProperty(BlockAmethystCluster.FACING, f));
                    break;
                }
            }
        }
        return true;
    }

    private static void safeSet(World world, BlockPos pos, IBlockState state) {
        if (!cannotReplace(world.getBlockState(pos))) {
            world.setBlockState(pos, state, 2);
        }
    }

    /** BlockTags.GEODE_INVALID_BLOCKS (the feature counts air with them). */
    private static boolean invalidHere(IBlockState s) {
        Block b = s.getBlock();
        return b == Blocks.BEDROCK || s.getMaterial() == Material.WATER || s.getMaterial() == Material.LAVA
                || b == Blocks.ICE || b == Blocks.PACKED_ICE;
    }

    /** BlockTags.FEATURES_CANNOT_REPLACE, and any tile entity. */
    private static boolean cannotReplace(IBlockState s) {
        Block b = s.getBlock();
        return b == Blocks.BEDROCK || b == Blocks.MOB_SPAWNER || b == Blocks.CHEST || b == Blocks.END_PORTAL_FRAME
                || b.hasTileEntity(s);
    }
}
