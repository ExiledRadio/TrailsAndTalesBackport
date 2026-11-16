package com.exiledradio.trailsandtalesbackport.structure;

import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import net.minecraft.block.BlockColored;
import net.minecraft.block.BlockSandStone;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.ComponentScatteredFeaturePieces;
import net.minecraft.world.gen.structure.MapGenScatteredFeature;
import net.minecraft.world.gen.structure.MapGenStructure;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;

/**
 * 1.20's desert pyramid cellar ({@code DesertPyramidPiece.addCellar}) and its suspicious sand
 * ({@code DesertPyramidStructure.afterPlace}), added to the pyramid of whichever generator builds it: 1.12.2's own,
 * Backport Core's 1.13+ grid ({@code ModernTemples}) or Caves &amp; Cliffs' 1.18 one. All of them build 1.12.2's
 * {@code ComponentScatteredFeaturePieces.DesertPyramid}, which is block for block 1.20's pyramid body apart from one
 * sandstone (1.12.2 runs a wall to z 18, 1.20 to 17), so the cellar goes in by 1.20's own piece coordinates.
 *
 * <p>Run after each chunk's populate step has built its structures, inside that step's box, as 1.20's piece builds
 * per chunk. The 5-7 suspicious sand of the 83 cellar spots and the collapsed roof's one are picked as 1.20 picks
 * them ({@code forkPositional} randoms of the world seed). Not vanilla's: the roof's sandstone-or-sand and the stair
 * fill's {@code nextBoolean} come from a per-chunk random, where 1.20 draws its world-generation region's.
 */
public final class DesertPyramids {

    private DesertPyramids() {
    }

    public static final ResourceLocation TABLE = new ResourceLocation(TrailsAndTalesBackport.MOD_ID, "archaeology/desert_pyramid");

    private static Field structureMap;

    /** Every desert pyramid start of the world's generator whose box meets {@code box}. */
    public static List<StructureComponent> pyramidsIn(WorldServer world, StructureBoundingBox box) {
        List<StructureComponent> out = new ArrayList<StructureComponent>();
        for (MapGenStructure gen : templeGenerators(world)) {
            for (StructureStart start : starts(gen)) {
                if (!start.isSizeableStructure() || !start.getBoundingBox().intersectsWith(box)) {
                    continue;
                }
                for (StructureComponent c : start.getComponents()) {
                    if (c instanceof ComponentScatteredFeaturePieces.DesertPyramid && c.getBoundingBox().intersectsWith(box)) {
                        out.add(c);
                    }
                }
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    public static Iterable<StructureStart> starts(MapGenStructure gen) {
        try {
            if (structureMap == null) {
                structureMap = ReflectionHelper.findField(MapGenStructure.class, "structureMap", "field_75053_d");
            }
            Map<Long, StructureStart> map = (Map<Long, StructureStart>) structureMap.get(gen);
            return new ArrayList<StructureStart>(map.values());
        } catch (ReflectiveOperationException | RuntimeException e) {
            return new ArrayList<StructureStart>();
        }
    }

    private static final Map<Class<?>, List<Field>> FIELDS = new IdentityHashMap<Class<?>, List<Field>>();

    /** The scattered-feature (temple) generators a chunk generator holds, found by type through its fields. */
    public static synchronized List<MapGenStructure> templeGenerators(WorldServer world) {
        List<MapGenStructure> out = new ArrayList<MapGenStructure>();
        Object generator = world.getChunkProvider().chunkGenerator;
        if (generator == null) {
            return out;
        }
        List<Field> fields = FIELDS.get(generator.getClass());
        if (fields == null) {
            fields = new ArrayList<Field>();
            for (Class<?> c = generator.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    if (MapGenStructure.class.isAssignableFrom(f.getType()) || f.getType() == Object.class
                            || net.minecraft.world.gen.MapGenBase.class.isAssignableFrom(f.getType())) {
                        try {
                            f.setAccessible(true);
                            fields.add(f);
                        } catch (RuntimeException ignored) {
                            // a field we may not read
                        }
                    }
                }
            }
            FIELDS.put(generator.getClass(), fields);
        }
        for (Field f : fields) {
            try {
                Object v = f.get(generator);
                if (v instanceof MapGenScatteredFeature && !out.contains(v)) {
                    out.add((MapGenStructure) v);
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // skip
            }
        }
        return out;
    }

    // ---- the piece's frame: 1.12.2's StructureComponent offsets, which are 1.20's

    static final class Frame {
        final StructureBoundingBox b;
        @Nullable
        final EnumFacing facing;
        final Mirror mirror;
        final Rotation rotation;

        Frame(StructureComponent c) {
            this.b = c.getBoundingBox();
            this.facing = c.getCoordBaseMode();
            if (this.facing == EnumFacing.SOUTH) {
                this.mirror = Mirror.LEFT_RIGHT;
                this.rotation = Rotation.NONE;
            } else if (this.facing == EnumFacing.WEST) {
                this.mirror = Mirror.LEFT_RIGHT;
                this.rotation = Rotation.CLOCKWISE_90;
            } else if (this.facing == EnumFacing.EAST) {
                this.mirror = Mirror.NONE;
                this.rotation = Rotation.CLOCKWISE_90;
            } else {
                this.mirror = Mirror.NONE;
                this.rotation = Rotation.NONE;
            }
        }

        int x(int x, int z) {
            if (this.facing == null) {
                return x;
            }
            switch (this.facing) {
                case WEST:
                    return this.b.maxX - z;
                case EAST:
                    return this.b.minX + z;
                default:
                    return this.b.minX + x;
            }
        }

        int y(int y) {
            return this.facing == null ? y : y + this.b.minY;
        }

        int z(int x, int z) {
            if (this.facing == null) {
                return z;
            }
            switch (this.facing) {
                case NORTH:
                    return this.b.maxZ - z;
                case SOUTH:
                    return this.b.minZ + z;
                default:
                    return this.b.minZ + x;
            }
        }

        BlockPos pos(int x, int y, int z) {
            return new BlockPos(x(x, z), y(y), z(x, z));
        }

        /** {@code StructurePiece.placeBlock}. */
        void place(@Nullable World world, IBlockState state, int x, int y, int z, StructureBoundingBox box) {
            BlockPos p = pos(x, y, z);
            if (world != null && box.isVecInside(p)) {
                IBlockState s = state;
                if (this.mirror != Mirror.NONE) {
                    s = s.withMirror(this.mirror);
                }
                if (this.rotation != Rotation.NONE) {
                    s = s.withRotation(this.rotation);
                }
                world.setBlockState(p, s, 2);
            }
        }

        /** {@code StructurePiece.generateBox} with one state. */
        void box(@Nullable World world, StructureBoundingBox box, int x0, int y0, int z0, int x1, int y1, int z1, IBlockState state,
                 boolean excludeAir) {
            for (int y = y0; y <= y1; y++) {
                for (int x = x0; x <= x1; x++) {
                    for (int z = z0; z <= z1; z++) {
                        if (!excludeAir || !isAir(world, x, y, z, box)) {
                            place(world, state, x, y, z, box);
                        }
                    }
                }
            }
        }

        /** {@code getBlock(...).isAir()}: outside the box is air. */
        boolean isAir(@Nullable World world, int x, int y, int z, StructureBoundingBox box) {
            BlockPos p = pos(x, y, z);
            return world == null || !box.isVecInside(p) || world.isAirBlock(p);
        }
    }

    // ---- 1.20's cellar

    static final IBlockState SAND = Blocks.SAND.getDefaultState();
    static final IBlockState SANDSTONE = Blocks.SANDSTONE.getDefaultState();
    static final IBlockState CUT_SANDSTONE = Blocks.SANDSTONE.getDefaultState().withProperty(BlockSandStone.TYPE, BlockSandStone.EnumType.SMOOTH);
    static final IBlockState CHISELED_SANDSTONE = Blocks.SANDSTONE.getDefaultState().withProperty(BlockSandStone.TYPE, BlockSandStone.EnumType.CHISELED);
    static final IBlockState ORANGE_TERRACOTTA = Blocks.STAINED_HARDENED_CLAY.getDefaultState().withProperty(BlockColored.COLOR, EnumDyeColor.ORANGE);
    static final IBlockState BLUE_TERRACOTTA = Blocks.STAINED_HARDENED_CLAY.getDefaultState().withProperty(BlockColored.COLOR, EnumDyeColor.BLUE);

    /** 1.20's cellar origin in the piece: {@code new BlockPos(16, -4, 13)}. */
    static final int CX = 16;
    static final int CY = -4;
    static final int CZ = 13;

    /** What 1.20 adds to one pyramid inside {@code box}: the cellar, then the suspicious sand. */
    public static void apply(World world, StructureComponent pyramid, BoundingBoxes start, StructureBoundingBox box, Random random) {
        Frame f = new Frame(pyramid);
        // 1.20 stops this wall one block sooner (generateBox 4, 3, 5 .. 4, 3, 17; 1.12.2 runs it to 18).
        BlockPos bump = f.pos(4, 3, 18);
        if (box.isVecInside(bump) && world.getBlockState(bump).getBlock() == Blocks.SANDSTONE) {
            world.setBlockToAir(bump);
        }
        List<BlockPos> spots = new ArrayList<BlockPos>();
        addCellarStairs(world, f, box, random);
        BlockPos roof = addCellarRoom(world, world.getSeed(), f, box, random, spots);
        afterPlace(world, start.center, box, spots, roof);
    }

    /** The start's box middle ({@code calculateBoundingBox().getCenter()}). */
    public static final class BoundingBoxes {
        final BlockPos center;

        public BoundingBoxes(StructureBoundingBox b) {
            this.center = new BlockPos(b.minX + (b.maxX - b.minX + 1) / 2, b.minY + (b.maxY - b.minY + 1) / 2, b.minZ + (b.maxZ - b.minZ + 1) / 2);
        }
    }

    private static void addCellarStairs(World world, Frame f, StructureBoundingBox box, Random random) {
        int x = CX;
        int y = CY;
        int z = CZ;
        // SANDSTONE_STAIRS.defaultBlockState().rotate(COUNTERCLOCKWISE_90): facing west.
        IBlockState stairs = Blocks.SANDSTONE_STAIRS.getDefaultState().withProperty(BlockStairs.FACING, EnumFacing.WEST);
        f.place(world, stairs, 13, -1, 17, box);
        f.place(world, stairs, 14, -2, 17, box);
        f.place(world, stairs, 15, -3, 17, box);
        boolean b = random.nextBoolean();
        f.place(world, SAND, x - 4, y + 4, z + 4, box);
        f.place(world, SAND, x - 3, y + 4, z + 4, box);
        f.place(world, SAND, x - 2, y + 4, z + 4, box);
        f.place(world, SAND, x - 1, y + 4, z + 4, box);
        f.place(world, SAND, x, y + 4, z + 4, box);
        f.place(world, SAND, x - 2, y + 3, z + 4, box);
        f.place(world, b ? SAND : SANDSTONE, x - 1, y + 3, z + 4, box);
        f.place(world, !b ? SAND : SANDSTONE, x, y + 3, z + 4, box);
        f.place(world, SAND, x - 1, y + 2, z + 4, box);
        f.place(world, SANDSTONE, x, y + 2, z + 4, box);
        f.place(world, SAND, x, y + 1, z + 4, box);
    }

    /** @return the collapsed roof's random spot */
    private static BlockPos addCellarRoom(@Nullable World world, long seed, Frame f, StructureBoundingBox box, Random random, List<BlockPos> spots) {
        int x = CX;
        int y = CY;
        int z = CZ;
        IBlockState cut = CUT_SANDSTONE;
        IBlockState chiseled = CHISELED_SANDSTONE;
        f.box(world, box, x - 3, y + 1, z - 3, x - 3, y + 1, z + 2, cut, true);
        f.box(world, box, x + 3, y + 1, z - 3, x + 3, y + 1, z + 2, cut, true);
        f.box(world, box, x - 3, y + 1, z - 3, x + 3, y + 1, z - 2, cut, true);
        f.box(world, box, x - 3, y + 1, z + 3, x + 3, y + 1, z + 3, cut, true);
        f.box(world, box, x - 3, y + 2, z - 3, x - 3, y + 2, z + 2, chiseled, true);
        f.box(world, box, x + 3, y + 2, z - 3, x + 3, y + 2, z + 2, chiseled, true);
        f.box(world, box, x - 3, y + 2, z - 3, x + 3, y + 2, z - 2, chiseled, true);
        f.box(world, box, x - 3, y + 2, z + 3, x + 3, y + 2, z + 3, chiseled, true);
        f.box(world, box, x - 3, -1, z - 3, x - 3, -1, z + 2, cut, true);
        f.box(world, box, x + 3, -1, z - 3, x + 3, -1, z + 2, cut, true);
        f.box(world, box, x - 3, -1, z - 3, x + 3, -1, z - 2, cut, true);
        f.box(world, box, x - 3, -1, z + 3, x + 3, -1, z + 3, cut, true);
        sandBox(f, spots, x - 2, y + 1, z - 2, x + 2, y + 3, z + 2);
        BlockPos roof = collapsedRoof(world, seed, f, box, random, x - 2, y + 4, z - 2, x + 2, z + 2);
        f.place(world, BLUE_TERRACOTTA, x, y, z, box);
        f.place(world, ORANGE_TERRACOTTA, x + 1, y, z - 1, box);
        f.place(world, ORANGE_TERRACOTTA, x + 1, y, z + 1, box);
        f.place(world, ORANGE_TERRACOTTA, x - 1, y, z - 1, box);
        f.place(world, ORANGE_TERRACOTTA, x - 1, y, z + 1, box);
        f.place(world, ORANGE_TERRACOTTA, x + 2, y, z, box);
        f.place(world, ORANGE_TERRACOTTA, x - 2, y, z, box);
        f.place(world, ORANGE_TERRACOTTA, x, y, z + 2, box);
        f.place(world, ORANGE_TERRACOTTA, x, y, z - 2, box);
        f.place(world, ORANGE_TERRACOTTA, x + 3, y, z, box);
        spots.add(f.pos(x + 3, y + 1, z));
        spots.add(f.pos(x + 3, y + 2, z));
        f.place(world, cut, x + 4, y + 1, z, box);
        f.place(world, chiseled, x + 4, y + 2, z, box);
        f.place(world, ORANGE_TERRACOTTA, x - 3, y, z, box);
        spots.add(f.pos(x - 3, y + 1, z));
        spots.add(f.pos(x - 3, y + 2, z));
        f.place(world, cut, x - 4, y + 1, z, box);
        f.place(world, chiseled, x - 4, y + 2, z, box);
        f.place(world, ORANGE_TERRACOTTA, x, y, z + 3, box);
        spots.add(f.pos(x, y + 1, z + 3));
        spots.add(f.pos(x, y + 2, z + 3));
        f.place(world, ORANGE_TERRACOTTA, x, y, z - 3, box);
        spots.add(f.pos(x, y + 1, z - 3));
        spots.add(f.pos(x, y + 2, z - 3));
        f.place(world, cut, x, y + 1, z - 4, box);
        f.place(world, chiseled, x, -2, z - 4, box);
        return roof;
    }

    private static void sandBox(Frame f, List<BlockPos> spots, int x0, int y0, int z0, int x1, int y1, int z1) {
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    spots.add(f.pos(x, y, z));
                }
            }
        }
    }

    private static BlockPos collapsedRoof(@Nullable World world, long seed, Frame f, StructureBoundingBox box, Random random, int x0, int y, int z0, int x1, int z1) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                f.place(world, random.nextFloat() < 0.33F ? SANDSTONE : SAND, x, y, z, box);
            }
        }
        Random r = VanillaRandom.positional(seed, f.pos(x0, y, z0));
        int x = VanillaRandom.between(r, x0, x1);
        int z = VanillaRandom.between(r, z0, z1);
        return new BlockPos(f.x(x, z), f.y(y), f.z(x, z));
    }

    /** {@code Vec3i.compareTo}: by y, then z, then x. */
    static final Comparator<BlockPos> VEC3I = (a, b) -> a.getY() != b.getY() ? Integer.compare(a.getY(), b.getY())
            : a.getZ() != b.getZ() ? Integer.compare(a.getZ(), b.getZ()) : Integer.compare(a.getX(), b.getX());

    /** {@code DesertPyramidStructure.afterPlace}. */
    static void afterPlace(World world, BlockPos center, StructureBoundingBox box, List<BlockPos> spots, BlockPos roof) {
        placeSuspicious(world, box, roof);
        List<BlockPos> sorted = new ArrayList<BlockPos>(new TreeSet<BlockPos>(VEC3I) {
            {
                addAll(spots);
            }
        });
        Random random = VanillaRandom.positional(world.getSeed(), center);
        VanillaRandom.shuffle(sorted, random);
        int n = Math.min(sorted.size(), VanillaRandom.between(random, 5, 7));
        for (BlockPos p : sorted) {
            if (n > 0) {
                n--;
                placeSuspicious(world, box, p);
            } else if (box.isVecInside(p)) {
                world.setBlockState(p, SAND, 2);
            }
        }
    }

    /**
     * Every position 1.20 makes suspicious in a pyramid, whatever chunk it is in: the collapsed roof's spot, then the
     * 5-7 cellar spots picked. For tests and tools; builds nothing.
     */
    public static List<BlockPos> suspiciousSpots(long seed, StructureComponent pyramid, StructureBoundingBox start) {
        Frame f = new Frame(pyramid);
        List<BlockPos> spots = new ArrayList<BlockPos>();
        BlockPos roof = addCellarRoom(null, seed, f, start, new Random(0L), spots);
        List<BlockPos> out = new ArrayList<BlockPos>();
        out.add(roof);
        out.addAll(picks(seed, new BoundingBoxes(start).center, spots));
        return out;
    }

    /** The picks alone, for tests: the shuffled spots, the first {@code n} suspicious. */
    public static List<BlockPos> picks(long seed, BlockPos center, List<BlockPos> spots) {
        List<BlockPos> sorted = new ArrayList<BlockPos>(new TreeSet<BlockPos>(VEC3I) {
            {
                addAll(spots);
            }
        });
        Random random = VanillaRandom.positional(seed, center);
        VanillaRandom.shuffle(sorted, random);
        int n = Math.min(sorted.size(), VanillaRandom.between(random, 5, 7));
        return new ArrayList<BlockPos>(sorted.subList(0, n));
    }

    private static void placeSuspicious(World world, StructureBoundingBox box, BlockPos pos) {
        if (box.isVecInside(pos)) {
            Archaeology.set(world, pos, false, TABLE, VanillaRandom.asLong(pos));
        }
    }
}
