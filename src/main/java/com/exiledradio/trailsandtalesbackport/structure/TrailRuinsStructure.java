package com.exiledradio.trailsandtalesbackport.structure;

import com.exiledradio.backportcore.locate.BiomeLocate;
import com.exiledradio.backportcore.structure.ScatteredStructure;
import com.exiledradio.backportcore.structure.StructureRandoms;
import com.exiledradio.backportcore.terrain.NetherHeights;
import com.exiledradio.backportcore.terrain.WorldHeights;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.IntBinaryOperator;

/**
 * 1.20.1's trail ruins: structure set {@code trail_ruins} (its {@code random_spread} grid, from vanilla's file), the
 * jigsaw structure of {@link TrailLayout}, valid where the stub's biome is in {@code #has_structure/trail_ruins}
 * (taiga, snowy taiga, both old growth taigas, old growth birch forest, jungle). Built from the populate step by
 * Backport Core's scattered driver, so it works on any terrain: the start sits 15 blocks under the surface at its
 * middle, as 1.20's {@code start_height} and {@code project_start_to_heightmap} put it, so the ruins are buried in
 * 1.12.2's terrain as in 1.20's; a terrain that adapts to structures (Caves &amp; Cliffs' 1.18 generator) also gets
 * 1.20's {@code bury} around the pieces.
 */
public final class TrailRuinsStructure extends ScatteredStructure {

    public static final String SAVE_NAME = "TTB_TrailRuins";

    public TrailRuinsStructure() {
        super(SAVE_NAME, TrailRuins.get().grid);
        // A start is laid out by every chunk populated within this many chunks of it: its pieces stay inside a box 80
        // blocks out from the start piece's middle, which is within a chunk of the start chunk's corner.
        this.range = 7;
    }

    /** {@code getFirstFreeHeight(x, z, WORLD_SURFACE_WG)} of the world's terrain, without loading chunks. */
    static IntBinaryOperator surface(World world) {
        NetherHeights heights = NetherHeights.forWorld(world);
        int sea = world.getSeaLevel();
        return (x, z) -> {
            int y = heights == null ? NetherHeights.UNKNOWN : heights.height(x, z);
            return y == NetherHeights.UNKNOWN ? sea : y;
        };
    }

    /** 1.20.1's stub for this chunk, from a fresh start random, or null. */
    @Nullable
    public static TrailLayout.Stub stub(World world, int chunkX, int chunkZ) {
        Random random = StructureRandoms.start(world.getSeed(), chunkX, chunkZ);
        return TrailLayout.stub(chunkX, chunkZ, random, surface(world));
    }

    /** The pieces of the trail ruins starting in this chunk (whatever the biome). Loads nothing. */
    public static List<TrailRuinsPiece> layout(World world, int chunkX, int chunkZ) {
        Random random = StructureRandoms.start(world.getSeed(), chunkX, chunkZ);
        IntBinaryOperator surface = surface(world);
        TrailLayout.Stub stub = TrailLayout.stub(chunkX, chunkZ, random, surface);
        return stub == null ? new ArrayList<TrailRuinsPiece>() : TrailLayout.assemble(stub, random, surface);
    }

    @Override
    protected boolean canStartAt(World world, int chunkX, int chunkZ) {
        if (!StructuresContent.generates()) {
            return false;
        }
        TrailLayout.Stub stub = stub(world, chunkX, chunkZ);
        return stub != null && validBiome(world, stub.position);
    }

    /** {@code Structure.isValidBiome}: the stub's biome (its noise biome where the terrain has them) is in the tag. */
    public static boolean validBiome(World world, BlockPos pos) {
        String noise = noiseBiome(world, pos.getX() >> 2, pos.getY() >> 2, pos.getZ() >> 2);
        if (noise != null) {
            return TrailRuins.get().biomes.contains(noise);
        }
        Biome biome = world.getBiomeProvider().getBiome(pos, null);
        if (biome == null) {
            return false;
        }
        String id = BiomeLocate.id(biome);
        return id != null && TrailRuins.get().biomes.contains(VanillaTemplate.full(id));
    }

    /** True while this places starts (iterating its start map), during which a new chunk must not add to the map. */
    private boolean building;

    @Override
    public synchronized boolean generateStructure(World world, Random random, ChunkPos chunk) {
        this.building = true;
        try {
            return super.generateStructure(world, random, chunk);
        } finally {
            this.building = false;
        }
    }

    /** How far, in chunks, a start's pieces and the terrain's 12-block reach around them go from its start chunk. */
    private static final int REACH_CHUNKS = 8;

    /**
     * Backport Core's terrain adaptation planner: every start whose box, grown by 12 blocks, reaches chunk (cx, cz),
     * laid out (and kept) as the populate step will lay it out. Loads no chunks.
     */
    public void startsNear(World world, int cx, int cz, Consumer<StructureStart> out) {
        this.world = world;
        if (!StructuresContent.generates()) {
            return;
        }
        int s = placement().spacing();
        for (int gx = Math.floorDiv(cx - REACH_CHUNKS, s); gx <= Math.floorDiv(cx + REACH_CHUNKS, s); gx++) {
            for (int gz = Math.floorDiv(cz - REACH_CHUNKS, s); gz <= Math.floorDiv(cz + REACH_CHUNKS, s); gz++) {
                ChunkPos c = placement().candidate(world.getSeed(), gx * s, gz * s);
                if (Math.abs(c.x - cx) > REACH_CHUNKS || Math.abs(c.z - cz) > REACH_CHUNKS) {
                    continue;
                }
                long key = ChunkPos.asLong(c.x, c.z);
                StructureStart start = this.structureMap.get(key);
                if (start == null) {
                    if (!canSpawnStructureAtCoords(c.x, c.z)) {
                        continue;
                    }
                    if (this.building) {
                        start = getStructureStart(c.x, c.z);
                    } else {
                        generate(world, c.x, c.z, null);
                        start = this.structureMap.get(key);
                    }
                }
                if (start != null && start.isSizeableStructure() && reaches(start.getBoundingBox(), cx, cz)) {
                    out.accept(start);
                }
            }
        }
    }

    static boolean reaches(StructureBoundingBox b, int cx, int cz) {
        int x = cx << 4;
        int z = cz << 4;
        return b != null && b.maxX + 12 >= x && b.minX - 12 <= x + 15 && b.maxZ + 12 >= z && b.minZ - 12 <= z + 15;
    }

    @Override
    protected StructureStart getStructureStart(int chunkX, int chunkZ) {
        return new Start(this.world, chunkX, chunkZ);
    }

    public static final class Start extends StructureStart {
        /** For loading. */
        public Start() {
        }

        Start(World world, int chunkX, int chunkZ) {
            super(chunkX, chunkZ);
            this.components.addAll(layout(world, chunkX, chunkZ));
            updateBoundingBox();
        }

        @Override
        public void generateStructure(World world, Random random, StructureBoundingBox box) {
            // 1.12.2's MapGenStructure clips each chunk to y 1-512; a world that goes below 0 builds its whole height.
            StructureBoundingBox chunk = new StructureBoundingBox(box.minX, Math.min(box.minY, WorldHeights.minY(world)), box.minZ,
                    box.maxX, box.maxY, box.maxZ);
            super.generateStructure(world, random, chunk);
        }

        /** @return the pieces, start piece first */
        public List<StructureComponent> pieces() {
            return this.components;
        }
    }

    // ---- Caves & Cliffs' noise biomes, as Tricky Trials' trial chambers and Wild Update's ancient cities reach them

    private static boolean noiseLooked;
    private static Field biomes3d, source;
    private static Method sourceBiome, biomeName;

    /** @return the 1.18 terrain's noise biome at a quart ("minecraft:..."), or null in a world without it */
    @Nullable
    static String noiseBiome(World world, int qx, int qy, int qz) {
        if (!(world instanceof WorldServer)) {
            return null;
        }
        Object generator = ((WorldServer) world).getChunkProvider().chunkGenerator;
        if (generator == null || !generator.getClass().getName().equals("com.exiledradio.cavesandcliffsbackport.terrain.ModernGenerator")) {
            return null;
        }
        try {
            synchronized (TrailRuinsStructure.class) {
                if (!noiseLooked) {
                    noiseLooked = true;
                    biomes3d = generator.getClass().getDeclaredField("biomes3d");
                    biomes3d.setAccessible(true);
                    source = biomes3d.getType().getDeclaredField("source");
                    source.setAccessible(true);
                    sourceBiome = source.getType().getMethod("biome", int.class, int.class, int.class);
                    biomeName = Class.forName(source.getType().getPackage().getName() + ".Biomes").getMethod("name", int.class);
                }
            }
            if (sourceBiome == null) {
                return null;
            }
            Object chunks = biomes3d.get(generator);
            synchronized (chunks) {
                Object src = source.get(chunks);
                int id = (Integer) sourceBiome.invoke(src, qx, qy, qz);
                return "minecraft:" + biomeName.invoke(null, id);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            synchronized (TrailRuinsStructure.class) {
                if (sourceBiome != null) {
                    TrailsAndTalesBackport.LOGGER.warn("Caves & Cliffs' noise biome is out of reach; trail ruins check the column's biome.", e);
                }
                sourceBiome = null;
            }
            return null;
        }
    }
}
