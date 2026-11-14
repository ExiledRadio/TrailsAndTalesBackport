package com.exiledradio.trailsandtalesbackport.biome;

import net.minecraft.init.Biomes;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.GenLayerFuzzyZoom;
import net.minecraft.world.gen.layer.GenLayerSmooth;
import net.minecraft.world.gen.layer.GenLayerVoronoiZoom;
import net.minecraft.world.gen.layer.GenLayerZoom;
import net.minecraft.world.gen.layer.IntCache;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.WorldTypeEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Places the cherry grove on 1.12.2's biome layers (plan, <i>Decided</i> 1): part of the extreme hills becomes cherry
 * grove.
 *
 * <h2>Why this way</h2>
 * 1.20 put the cherry grove into the plateau table's variant row (high, uneroded land) in the cold and temperate
 * columns at the two driest humidities, in the meadow's place: a mountain-slope biome of the cool and temperate
 * latitudes. 1.12.2's biome layers have no climate or plateaus; their high land of the temperate and cool bands is the
 * extreme hills, in both of those bands and no other. So a second, independent layer of patches about 256 blocks
 * across is laid over the biome map, and extreme hills (and their wooded hills, {@code extreme_hills_with_trees})
 * under one of them is cherry grove. The gravelly variants (1.18's windswept gravelly hills) and the extreme hills
 * edge stay as they are.
 *
 * <p>The grove has the extreme hills' height numbers, so the terrain is the same block for block with or without it,
 * and nothing else in the biome map moves: a seed's map is 1.12.2's (Chunkbase's) apart from the groves. How much of
 * the hills becomes grove ({@link #CHANCE}) is set so the grove's share of the world is 1.20's (0.30% of the
 * overworld's area, measured over 1.20's own biome source; {@code docs/streams/grove.md}).
 *
 * <h2>How</h2>
 * Forge's {@link WorldTypeEvent.InitBiomeGens} hands over the final layers: the 1:4 biome map that shapes terrain,
 * and the 1:1 map stored in chunks, which is vanilla's voronoi zoom of that same 1:4 map. The grove goes into the
 * 1:4 map ({@link Grove}, which reads the patch mask at the same scale), and the zoom is pointed at the new 1:4 map,
 * so the 1:1 map is the zoom of the map with groves, block for block as vanilla's two maps agree. The zoom itself
 * stays in place, so Backport Core's later steps on the same event (it seeds the hills layer's river noise and swaps
 * the voronoi zoom for 1.15's fuzzy one, both at {@code LOWEST}) find the layers they look for.
 *
 * <p>Caves &amp; Cliffs' 1.18 terrain places biomes by 1.20's climate (its {@code Editions.CHERRY}) and does not use
 * these layers.
 */
public final class CherryGroveLayer {

    /** One mask cell in this many is grove. */
    public static final int CHANCE = 38;
    /** The mask's own seed, so its patches are independent of every vanilla layer. */
    static final long MASK_SEED = 120230607L;

    private CherryGroveLayer() {
    }

    /** Pre-init: the event is on Forge's terrain bus, which annotations do not reach. */
    public static void register() {
        MinecraftForge.TERRAIN_GEN_BUS.register(CherryGroveLayer.class);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onInitBiomeGens(WorldTypeEvent.InitBiomeGens event) {
        BiomeCherryGrove grove = CherryGroveContent.biome;
        GenLayer[] layers = event.getNewBiomeGens();
        if (grove == null || !CherryGroveContent.generates() || layers == null || layers.length < 2) {
            return;
        }
        event.setNewBiomeGens(wrap(layers, event.getSeed(), grove));
    }

    private static final java.lang.reflect.Field PARENT =
            net.minecraftforge.fml.relauncher.ReflectionHelper.findField(GenLayer.class, "parent", "field_75909_a");

    /**
     * The final layers with the grove laid over the 1:4 map: {@code layers[0]} (and any later layer that is the same
     * object, as vanilla's third is) becomes the grove layer, and the 1:1 zoom ({@code layers[1]}) reads from it.
     */
    public static GenLayer[] wrap(GenLayer[] layers, long seed, Biome grove) {
        GenLayer[] out = layers.clone();
        GenLayer coarse = layers[0];
        Grove g = new Grove(coarse, mask(), grove);
        for (int i = 0; i < out.length; i++) {
            if (out[i] == coarse) {
                out[i] = g;
            }
        }
        try {
            if (PARENT.get(layers[1]) == coarse) {
                PARENT.set(layers[1], g);
            } else {
                // Some other mod's 1:1 layer: lay the grove over it at its own scale.
                out[1] = new Grove(layers[1], new GenLayerVoronoiZoom(10L, mask()), grove);
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        for (GenLayer layer : out) {
            if (layer != null) {
                layer.initWorldGenSeed(seed);
            }
        }
        return out;
    }

    /** The patch mask at the biome map's 1:4 scale: cells of 1:256, fuzzy then plain zooms, smoothed. */
    static GenLayer mask() {
        GenLayer mask = new Cells(MASK_SEED);
        mask = new GenLayerFuzzyZoom(2000L, mask);
        mask = GenLayerZoom.magnify(2001L, mask, 5);
        return new GenLayerSmooth(1000L, mask);
    }

    /** Whether a 1.12.2 biome id is ground the grove takes. */
    static boolean takes(int id) {
        return id == Biome.getIdForBiome(Biomes.EXTREME_HILLS) || id == Biome.getIdForBiome(Biomes.EXTREME_HILLS_WITH_TREES);
    }

    /** 1 in {@link #CHANCE} cells grove (1), the rest not (0). */
    static final class Cells extends GenLayer {
        Cells(long seed) {
            super(seed);
        }

        @Override
        public int[] getInts(int x, int z, int width, int depth) {
            int[] out = IntCache.getIntCache(width * depth);
            for (int dz = 0; dz < depth; dz++) {
                for (int dx = 0; dx < width; dx++) {
                    initChunkSeed(x + dx, z + dz);
                    out[dx + dz * width] = nextInt(CHANCE) == 0 ? 1 : 0;
                }
            }
            return out;
        }
    }

    /** Extreme hills where the mask is grove become the cherry grove. */
    static final class Grove extends GenLayer {
        private final GenLayer biomes;
        private final GenLayer mask;
        private final int groveId;

        Grove(GenLayer biomes, GenLayer mask, Biome grove) {
            super(0L);
            this.biomes = biomes;
            this.mask = mask;
            this.groveId = Biome.getIdForBiome(grove);
            this.parent = biomes;
        }

        @Override
        public void initWorldGenSeed(long seed) {
            this.biomes.initWorldGenSeed(seed);
            this.mask.initWorldGenSeed(seed);
        }

        @Override
        public int[] getInts(int x, int z, int width, int depth) {
            int[] in = this.biomes.getInts(x, z, width, depth);
            int[] cells = this.mask.getInts(x, z, width, depth);
            int[] out = IntCache.getIntCache(width * depth);
            for (int i = 0; i < width * depth; i++) {
                out[i] = cells[i] == 1 && takes(in[i]) ? this.groveId : in[i];
            }
            return out;
        }
    }
}
