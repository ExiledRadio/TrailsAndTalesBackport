package com.exiledradio.trailsandtalesbackport.biome;

import com.exiledradio.trailsandtalesbackport.cherry.BlockPinkPetals;
import com.exiledradio.trailsandtalesbackport.cherry.CherryContent;
import com.exiledradio.trailsandtalesbackport.cherry.CherryTree;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockSilverfish;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.state.pattern.BlockMatcher;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeDecorator;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.feature.WorldGenLiquids;
import net.minecraft.world.gen.feature.WorldGenMinable;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent;
import net.minecraftforge.event.terraingen.OreGenEvent;
import net.minecraftforge.event.terraingen.TerrainGen;

import java.util.Random;

/**
 * The cherry grove's decoration in 1.12.2's terrain: 1.20.6's placed features for the biome, in its step order and
 * with its numbers, on 1.12.2's decoration area (the chunk offset by 8, so nothing reaches an ungenerated chunk).
 *
 * <table>
 *   <tr><th>1.20.6 step</th><th>Placed features</th><th>Here</th></tr>
 *   <tr><td>ores</td><td>the default ores, {@code disk_sand} 3, {@code disk_clay} 1, {@code disk_gravel} 1,
 *       {@code ore_emerald}</td><td>1.12.2's ores and disks (the same counts), and 1.12.2's mountain emeralds
 *       (3-8 at y 4-31, as its extreme hills); tuff and copper are Caves &amp; Cliffs'</td></tr>
 *   <tr><td>underground decoration</td><td>{@code ore_infested}</td><td>1.12.2's mountain silverfish stone (7 veins
 *       of 9, y 0-63)</td></tr>
 *   <tr><td>fluid springs</td><td>the default springs</td><td>1.12.2's (50 water, 20 lava)</td></tr>
 *   <tr><td rowspan="4">vegetation</td><td>{@code patch_tall_grass_2}: 7 where the biome noise is at least -0.8, else
 *       0; then 1 in 32; motion-blocking surface</td><td>96 tries, 7 across, 3 up or down: tall grass</td></tr>
 *   <tr><td>{@code patch_grass_plain}: 10 where the noise is at least -0.8, else 5; worldgen surface</td><td>32 tries,
 *       7 across, 3 up or down: grass</td></tr>
 *   <tr><td>{@code flower_cherry}: 10, else 5; motion-blocking surface</td><td>96 tries, 6 across, 2 up or down: pink
 *       petals, any facing, 1 to 4 flowers</td></tr>
 *   <tr><td>{@code trees_cherry}: 10 (9 in 10) or 11; no water above (worldgen heightmaps); ocean floor; where a
 *       cherry sapling would stand</td><td>{@code cherry_bees_005}: {@link CherryTree}; the 5% bee nest is Buzzy Bees Backport's
 *       ({@link CherryGroveContent#bees})</td></tr>
 * </table>
 * Each position is also checked as 1.20's {@code biome} filter does: it must be in a cherry grove. Amethyst geodes,
 * glow lichen, monster rooms and lakes come from the generator and the other backports, as for every biome.
 *
 * <p>The heightmaps are 1.20's: <i>world surface</i> is the first block above the highest one that is not air,
 * <i>motion blocking</i> above the highest one that blocks movement or holds a fluid, <i>ocean floor</i> above the
 * highest one that blocks movement. The <i>worldgen</i> ones ({@code WORLD_SURFACE_WG}, {@code OCEAN_FLOOR_WG}: the
 * grass patches' surface and the trees' water-depth check) stop being updated in 1.20 once features start, so here
 * they skip plants, leaves and logs. A tree also needs its trunk's own block to be air or
 * {@code #replaceable_by_trees}, as 1.20's {@code TreeFeature} checks; pink petals are not in that tag. The biome noise
 * is read at the chunk's corner, as 1.20 reads it at the placement origin. Blocks are set without neighbor or observer
 * updates (flags 2 and 16), as 1.20's worldgen region sets them, so nothing reaches past the loaded chunks.
 */
public class CherryGroveDecorator extends BiomeDecorator {

    /** 1.20.6's numbers. */
    public static final double NOISE_LEVEL = -0.8D;
    public static final int TALL_GRASS_ABOVE = 7;
    public static final int TALL_GRASS_BELOW = 0;
    public static final int TALL_GRASS_RARITY = 32;
    public static final int TALL_GRASS_TRIES = 96;
    public static final int GRASS_ABOVE = 10;
    public static final int GRASS_BELOW = 5;
    public static final int GRASS_TRIES = 32;
    public static final int PATCH_SPREAD = 7;
    public static final int PATCH_Y_SPREAD = 3;
    public static final int PETALS_ABOVE = 10;
    public static final int PETALS_BELOW = 5;
    public static final int PETALS_TRIES = 96;
    public static final int PETALS_SPREAD = 6;
    public static final int PETALS_Y_SPREAD = 2;
    /** {@code weighted_list}: 10 at weight 9, 11 at weight 1. */
    public static final int TREES = 10;
    public static final int TREES_EXTRA = 11;
    public static final int TREES_EXTRA_WEIGHT = 1;
    public static final int TREES_TOTAL_WEIGHT = 10;

    /** 1.12.2's mountain extras. */
    public static final int EMERALD_MIN = 3;
    public static final int EMERALD_EXTRA = 6;
    public static final int SILVERFISH_VEINS = 7;

    private final WorldGenMinable silverfish = new WorldGenMinable(
            Blocks.MONSTER_EGG.getDefaultState().withProperty(BlockSilverfish.VARIANT, BlockSilverfish.EnumType.STONE), 9);

    /** For tests: the positions trees were tried at in the last decoration (null: not recording). */
    static java.util.List<BlockPos> treeTries;
    /** For tests: chunks, grass patches in the grove, grass placed, petal patches in the grove, petals placed, petal patches on a canopy. */
    static long[] stats;

    @Override
    protected void genDecorations(Biome biome, World world, Random random) {
        ChunkPos chunk = new ChunkPos(this.chunkPos);
        MinecraftForge.EVENT_BUS.post(new DecorateBiomeEvent.Pre(world, random, chunk));

        // Ores: 1.12.2's ores, then the disks.
        generateOres(world, random);
        disks(world, random, chunk);
        emeralds(world, random, chunk);
        // Underground decoration: infested stone.
        if (TerrainGen.generateOre(world, random, this.silverfish, this.chunkPos, OreGenEvent.GenerateMinable.EventType.SILVERFISH)) {
            for (int i = 0; i < SILVERFISH_VEINS; i++) {
                this.silverfish.generate(world, random, this.chunkPos.add(random.nextInt(16), random.nextInt(64), random.nextInt(16)));
            }
        }
        springs(world, random, chunk);

        if (stats != null) {
            stats[0]++;
        }
        // Vegetation, 1.20.6's order.
        boolean above = BiomeCherryGrove.infoNoise(this.chunkPos.getX() / 200.0D, this.chunkPos.getZ() / 200.0D) >= NOISE_LEVEL;
        if (!CherryGroveContent.generatorPlaces(world, "patch_tall_grass_2")
                && TerrainGen.decorate(world, random, chunk, DecorateBiomeEvent.Decorate.EventType.GRASS)) {
            int count = above ? TALL_GRASS_ABOVE : TALL_GRASS_BELOW;
            for (int i = 0; i < count; i++) {
                if (random.nextFloat() >= 1.0F / TALL_GRASS_RARITY) {
                    continue;
                }
                BlockPos at = motionBlocking(world, inSquare(random));
                if (inGrove(world, at)) {
                    tallGrassPatch(world, random, at);
                }
            }
        }
        if (!CherryGroveContent.generatorPlaces(world, "patch_grass_plain")
                && TerrainGen.decorate(world, random, chunk, DecorateBiomeEvent.Decorate.EventType.GRASS)) {
            IBlockState grass = Blocks.TALLGRASS.getDefaultState().withProperty(BlockTallGrass.TYPE, BlockTallGrass.EnumType.GRASS);
            int count = above ? GRASS_ABOVE : GRASS_BELOW;
            for (int i = 0; i < count; i++) {
                BlockPos at = worldSurfaceWG(world, inSquare(random));
                if (inGrove(world, at)) {
                    int n = patch(world, random, at, GRASS_TRIES, PATCH_SPREAD, PATCH_Y_SPREAD, grass, null);
                    if (stats != null) {
                        stats[1]++;
                        stats[2] += n;
                    }
                }
            }
        }
        BlockPinkPetals petals = CherryContent.petals;
        if (petals != null && !CherryGroveContent.generatorPlaces(world, "flower_cherry")
                && TerrainGen.decorate(world, random, chunk, DecorateBiomeEvent.Decorate.EventType.FLOWERS)) {
            int count = above ? PETALS_ABOVE : PETALS_BELOW;
            for (int i = 0; i < count; i++) {
                BlockPos at = motionBlocking(world, inSquare(random));
                if (inGrove(world, at)) {
                    int n = patch(world, random, at, PETALS_TRIES, PETALS_SPREAD, PETALS_Y_SPREAD, null, petals);
                    if (stats != null) {
                        stats[3]++;
                        stats[4] += n;
                        stats[5] += at.getY() > worldSurfaceWG(world, at).getY() ? 1 : 0;
                    }
                }
            }
        }
        if (CherryContent.sapling != null && !CherryGroveContent.generatorPlaces(world, "trees_cherry")
                && TerrainGen.decorate(world, random, chunk, DecorateBiomeEvent.Decorate.EventType.TREE)) {
            int count = random.nextInt(TREES_TOTAL_WEIGHT) < TREES_TOTAL_WEIGHT - TREES_EXTRA_WEIGHT ? TREES : TREES_EXTRA;
            for (int i = 0; i < count; i++) {
                BlockPos column = inSquare(random);
                BlockPos floor = oceanFloor(world, column);
                if (worldSurfaceWG(world, column).getY() - oceanFloorWG(world, column).getY() > 0 || !inGrove(world, floor) || !saplingSurvives(world, floor)) {
                    continue;
                }
                // 1.20's TreeFeature checks the trunk's own block too (CherryTree leaves it to the sapling's
                // grower, which has just cleared it): pink petals there, as often as not, stop the tree.
                if (!validTreePos(world, floor)) {
                    continue;
                }
                if (treeTries != null) {
                    treeTries.add(floor);
                }
                new CherryTree(false).generate(world, random, floor);
            }
        }

        MinecraftForge.EVENT_BUS.post(new DecorateBiomeEvent.Post(world, random, chunk));
    }

    /** {@code InSquarePlacement}: a random column of the decoration area. */
    private BlockPos inSquare(Random random) {
        return this.chunkPos.add(random.nextInt(16) + 8, 0, random.nextInt(16) + 8);
    }

    /** 1.20's {@code BiomeFilter}: the feature belongs to the biome at the position. */
    static boolean inGrove(World world, BlockPos pos) {
        return world.getBiome(pos) == CherryGroveContent.biome;
    }

    /** Block ids of other backports' plants in 1.20's {@code #replaceable_by_trees}. */
    private static final java.util.Set<String> REPLACEABLE_BY_TREES = new java.util.HashSet<String>(java.util.Arrays.asList(
            "glow_lichen", "hanging_roots", "seagrass", "tall_seagrass", "pitcher_plant", "warped_roots", "nether_sprouts",
            "crimson_roots", "short_grass"));

    /**
     * 1.20's {@code TreeFeature.validTreePos}: air or {@code #replaceable_by_trees} (leaves, grass, fern, dead bush,
     * vines, the tall flowers and grasses, water, and the other backports' glow lichen, hanging roots, seagrass,
     * pitcher plant and nether roots). Pink petals are not in it.
     */
    static boolean validTreePos(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (block.isAir(state, world, pos) || state.getMaterial() == Material.LEAVES || state.getMaterial() == Material.WATER
                || block instanceof net.minecraft.block.BlockTallGrass || block instanceof net.minecraft.block.BlockDeadBush
                || block instanceof net.minecraft.block.BlockVine || block instanceof BlockDoublePlant) {
            return true;
        }
        return block.getRegistryName() != null && REPLACEABLE_BY_TREES.contains(block.getRegistryName().getPath());
    }

    static boolean saplingSurvives(World world, BlockPos pos) {
        return CherryContent.sapling.canBlockStay(world, pos, CherryContent.sapling.getDefaultState());
    }

    // --- 1.12.2's ore-step extras -------------------------------------------------------------------------------

    /** 1.12.2's decorator disks: 3 sand, 1 clay, 1 gravel (1.20's disk counts), each on the top solid or liquid. */
    private void disks(World world, Random random, ChunkPos chunk) {
        if (TerrainGen.decorate(world, random, chunk, DecorateBiomeEvent.Decorate.EventType.SAND)) {
            for (int i = 0; i < this.sandPatchesPerChunk; ++i) {
                this.sandGen.generate(world, random, world.getTopSolidOrLiquidBlock(this.chunkPos.add(random.nextInt(16) + 8, 0, random.nextInt(16) + 8)));
            }
        }
        if (TerrainGen.decorate(world, random, chunk, DecorateBiomeEvent.Decorate.EventType.CLAY)) {
            for (int i = 0; i < this.clayPerChunk; ++i) {
                this.clayGen.generate(world, random, world.getTopSolidOrLiquidBlock(this.chunkPos.add(random.nextInt(16) + 8, 0, random.nextInt(16) + 8)));
            }
        }
        if (TerrainGen.decorate(world, random, chunk, DecorateBiomeEvent.Decorate.EventType.SAND_PASS2)) {
            for (int i = 0; i < this.gravelPatchesPerChunk; ++i) {
                this.gravelGen.generate(world, random, world.getTopSolidOrLiquidBlock(this.chunkPos.add(random.nextInt(16) + 8, 0, random.nextInt(16) + 8)));
            }
        }
    }

    /** 1.12.2's {@code BiomeHills.decorate}: 3-8 single emerald ores in stone, y 4-31. */
    private void emeralds(World world, Random random, ChunkPos chunk) {
        net.minecraft.world.gen.feature.WorldGenerator none = new net.minecraft.world.gen.feature.WorldGenerator() {
            @Override
            public boolean generate(World w, Random r, BlockPos p) {
                return false;
            }
        };
        if (!TerrainGen.generateOre(world, random, none, this.chunkPos, OreGenEvent.GenerateMinable.EventType.EMERALD)) {
            return;
        }
        int n = EMERALD_MIN + random.nextInt(EMERALD_EXTRA);
        for (int i = 0; i < n; ++i) {
            BlockPos at = this.chunkPos.add(random.nextInt(16), random.nextInt(28) + 4, random.nextInt(16));
            IBlockState state = world.getBlockState(at);
            if (state.getBlock().isReplaceableOreGen(state, world, at, BlockMatcher.forBlock(Blocks.STONE))) {
                // Flag 16: no observer updates, which would reach the chunk to the north or west (1.12.2's own emeralds do).
                world.setBlockState(at, Blocks.EMERALD_ORE.getDefaultState(), 2 | 16);
            }
        }
    }

    /** As 1.12.2's own springs (1.20's are the same features). */
    private void springs(World world, Random random, ChunkPos chunk) {
        if (!this.generateFalls) {
            return;
        }
        if (TerrainGen.decorate(world, random, chunk, DecorateBiomeEvent.Decorate.EventType.LAKE_WATER)) {
            for (int i = 0; i < 50; ++i) {
                int x = random.nextInt(16) + 8;
                int z = random.nextInt(16) + 8;
                int y = random.nextInt(248) + 8;
                if (y > 0) {
                    new WorldGenLiquids(Blocks.FLOWING_WATER).generate(world, random, this.chunkPos.add(x, random.nextInt(y), z));
                }
            }
        }
        if (TerrainGen.decorate(world, random, chunk, DecorateBiomeEvent.Decorate.EventType.LAKE_LAVA)) {
            for (int i = 0; i < 20; ++i) {
                int x = random.nextInt(16) + 8;
                int z = random.nextInt(16) + 8;
                int y = random.nextInt(random.nextInt(random.nextInt(240) + 8) + 8);
                new WorldGenLiquids(Blocks.FLOWING_LAVA).generate(world, random, this.chunkPos.add(x, y, z));
            }
        }
    }

    // --- 1.20's random patch ------------------------------------------------------------------------------------

    /**
     * {@code RandomPatchFeature} with a {@code simple_block} on air: each try is offset by {@code nextInt(spread + 1)
     * - nextInt(spread + 1)} across and the same with {@code ySpread} up or down; the block goes where there is air and
     * it can stand. Pink petals (when {@code petals} is given) are one of 1.20's sixteen weighted states: four
     * facings, 1 to 4 flowers.
     *
     * @return how many were placed
     */
    static int patch(World world, Random random, BlockPos origin, int tries, int spread, int ySpread,
                     IBlockState state, BlockPinkPetals petals) {
        int placed = 0;
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int i = 0; i < tries; i++) {
            at.setPos(origin.getX() + random.nextInt(spread + 1) - random.nextInt(spread + 1),
                    origin.getY() + random.nextInt(ySpread + 1) - random.nextInt(ySpread + 1),
                    origin.getZ() + random.nextInt(spread + 1) - random.nextInt(spread + 1));
            // The air filter comes first; the state is drawn only for a try that passes it.
            if (!world.isAirBlock(at)) {
                continue;
            }
            IBlockState place = state;
            if (petals != null) {
                // weighted_state_provider: 16 entries of weight 1, in the data file's order (amount, then facing).
                int pick = random.nextInt(16);
                place = petals.getDefaultState().withProperty(BlockPinkPetals.FACING, FACINGS[pick & 3])
                        .withProperty(BlockPinkPetals.AMOUNT, 1 + (pick >> 2));
            }
            if (canStand(world, at, place)) {
                world.setBlockState(at.toImmutable(), place, 2 | 16);
                placed++;
            }
        }
        return placed;
    }

    /** The data file's facing order: north, east, south, west. */
    private static final EnumFacing[] FACINGS = {EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.WEST};

    /** {@code patch_tall_grass}: 96 tries of tall grass, both halves, where the upper half has air too. */
    static int tallGrassPatch(World world, Random random, BlockPos origin) {
        int placed = 0;
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int i = 0; i < TALL_GRASS_TRIES; i++) {
            at.setPos(origin.getX() + random.nextInt(PATCH_SPREAD + 1) - random.nextInt(PATCH_SPREAD + 1),
                    origin.getY() + random.nextInt(PATCH_Y_SPREAD + 1) - random.nextInt(PATCH_Y_SPREAD + 1),
                    origin.getZ() + random.nextInt(PATCH_SPREAD + 1) - random.nextInt(PATCH_SPREAD + 1));
            BlockPos pos = at.toImmutable();
            // 1.20: the lower half's soil, then air above it for the upper half.
            if (world.isAirBlock(pos) && Blocks.DOUBLE_PLANT.canPlaceBlockAt(world, pos)) {
                Blocks.DOUBLE_PLANT.placeAt(world, pos, BlockDoublePlant.EnumPlantType.GRASS, 2 | 16);
                placed++;
            }
        }
        return placed;
    }

    private static boolean canStand(World world, BlockPos pos, IBlockState state) {
        Block block = state.getBlock();
        if (block instanceof BlockBush) {
            return ((BlockBush) block).canBlockStay(world, pos, state);
        }
        return block.canPlaceBlockAt(world, pos);
    }

    // --- 1.20's heightmaps --------------------------------------------------------------------------------------

    /** WORLD_SURFACE: the position above the highest block that is not air. */
    public static BlockPos worldSurface(World world, BlockPos column) {
        return scan(world, column, 0);
    }

    /** MOTION_BLOCKING: above the highest block that blocks movement or holds a fluid. */
    public static BlockPos motionBlocking(World world, BlockPos column) {
        return scan(world, column, 1);
    }

    /** OCEAN_FLOOR: above the highest block that blocks movement. */
    public static BlockPos oceanFloor(World world, BlockPos column) {
        return scan(world, column, 2);
    }

    /**
     * WORLD_SURFACE_WG: as {@link #worldSurface}, of the terrain alone. 1.20's worldgen heightmaps stop being updated
     * once features start, so trees, leaves and plants (this chunk's or a neighbor's) are not in them.
     */
    public static BlockPos worldSurfaceWG(World world, BlockPos column) {
        return scan(world, column, 3);
    }

    /** OCEAN_FLOOR_WG: as {@link #oceanFloor}, of the terrain alone. */
    public static BlockPos oceanFloorWG(World world, BlockPos column) {
        return scan(world, column, 4);
    }

    /** A block that features place, which the worldgen heightmaps do not see: plants, leaves, logs. */
    static boolean feature(IBlockState state, Material m) {
        return m == Material.PLANTS || m == Material.VINE || m == Material.LEAVES || m == Material.WEB
                || state.getBlock() instanceof BlockBush || state.getBlock() instanceof net.minecraft.block.BlockLog;
    }

    private static BlockPos scan(World world, BlockPos column, int kind) {
        Chunk chunk = world.getChunk(column);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(column.getX(), chunk.getTopFilledSegment() + 16, column.getZ());
        while (pos.getY() > 0) {
            pos.setY(pos.getY() - 1);
            IBlockState state = chunk.getBlockState(pos);
            Material m = state.getMaterial();
            boolean hit;
            if (kind == 0) {
                hit = m != Material.AIR;
            } else if (kind == 1) {
                hit = m.blocksMovement() || m.isLiquid();
            } else if (kind == 2) {
                hit = m.blocksMovement();
            } else if (kind == 3) {
                hit = m != Material.AIR && !feature(state, m);
            } else {
                hit = m.blocksMovement() && !feature(state, m);
            }
            if (hit) {
                return pos.up().toImmutable();
            }
        }
        return new BlockPos(column.getX(), 0, column.getZ());
    }
}
