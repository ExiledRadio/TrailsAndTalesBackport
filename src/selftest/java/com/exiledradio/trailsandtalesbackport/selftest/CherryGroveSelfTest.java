package com.exiledradio.trailsandtalesbackport.selftest;

import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.biome.BiomeCherryGrove;
import com.exiledradio.trailsandtalesbackport.biome.CherryGroveContent;
import com.exiledradio.trailsandtalesbackport.biome.CherryGroveLayer;
import com.exiledradio.trailsandtalesbackport.biome.GroveTestAccess;
import com.exiledradio.trailsandtalesbackport.cherry.BlockPinkPetals;
import com.exiledradio.trailsandtalesbackport.cherry.CherryContent;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.monster.EntityWitch;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.monster.EntityZombieVillager;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.passive.EntityRabbit;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.IntCache;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.BiomeEvent;
import net.minecraftforge.event.terraingen.WorldTypeEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Stream G: the cherry grove biome. */
public final class CherryGroveSelfTest implements SelfTest {

    /** 1.20's share of the overworld's area (0.30%, docs/streams/grove.md), and how far a 1.12.2 census may stray. */
    static final double SHARE_LOW = 0.0020;
    static final double SHARE_HIGH = 0.0045;

    @Override
    public String name() {
        return "grove";
    }

    @Override
    public void run(Check c) throws Exception {
        BiomeCherryGrove grove = CherryGroveContent.biome;
        if (!c.ok(grove != null, "the cherry grove is registered")) {
            return;
        }
        registration(c, grove);
        spawns(c, grove);
        colors(c, grove);
        layers(c, grove);
        worldLayers(c, grove);
        heightmaps(c);
        patches(c);
        integration(c, grove);
        realGrove(c, grove);
        density(c, grove);
    }

    // ---------------------------------------------------------------------------------------------------------

    private static void registration(Check c, BiomeCherryGrove grove) {
        c.eq(new ResourceLocation(TrailsAndTalesBackport.MOD_ID, "cherry_grove"), grove.getRegistryName(), "registry name");
        c.ok(ForgeRegistries.BIOMES.getValue(new ResourceLocation(TrailsAndTalesBackport.MOD_ID, "cherry_grove")) == grove,
                "found in the biome registry");
        c.eq(0.5F, grove.getDefaultTemperature(), "temperature");
        c.eq(0.8F, grove.getRainfall(), "downfall");
        c.ok(grove.canRain() && !grove.getEnableSnow(), "rain, no snow");
        c.eq(Biomes.EXTREME_HILLS.getBaseHeight(), grove.getBaseHeight(), "base height is the extreme hills'");
        c.eq(Biomes.EXTREME_HILLS.getHeightVariation(), grove.getHeightVariation(), "height variation is the extreme hills'");
        c.eq(Biomes.EXTREME_HILLS_WITH_TREES.getBaseHeight(), grove.getBaseHeight(), "base height is the wooded hills' too");
        c.eq(Biomes.EXTREME_HILLS_WITH_TREES.getHeightVariation(), grove.getHeightVariation(), "variation is the wooded hills' too");
        c.ok(BiomeDictionary.hasType(grove, BiomeDictionary.Type.MOUNTAIN), "MOUNTAIN, as 1.20's #is_mountain");
        c.ok(grove.topBlock.getBlock() == Blocks.GRASS && grove.fillerBlock.getBlock() == Blocks.DIRT, "grass over dirt");
        // At y 255 the temperature still stays above snow's 0.15: 0.5 - (255 - 64) * 0.05 / 30 (noise aside).
        c.ok(grove.getTemperature(new BlockPos(0, 200, 0)) > 0.15F, "no snow on the heights");
        c.ok(CherryGroveContent.generates(), "new worlds get groves");
        SoundEvent music = CherryGroveContent.music;
        c.ok(music != null && new ResourceLocation(TrailsAndTalesBackport.MOD_ID, "music.overworld.cherry_grove").equals(music.getRegistryName()),
                "music.overworld.cherry_grove is registered");
        c.ok(ForgeRegistries.SOUND_EVENTS.containsValue(music), "the music event is in the registry");
    }

    private static void spawns(Check c, BiomeCherryGrove grove) {
        List<Biome.SpawnListEntry> creatures = grove.getSpawnableList(EnumCreatureType.CREATURE);
        c.eq(3, creatures.size(), "three creatures");
        spawn(c, creatures, EntityPig.class, 1, 1, 2);
        spawn(c, creatures, EntityRabbit.class, 2, 2, 6);
        spawn(c, creatures, EntitySheep.class, 2, 2, 4);
        List<Biome.SpawnListEntry> monsters = grove.getSpawnableList(EnumCreatureType.MONSTER);
        c.eq(8, monsters.size(), "eight monsters");
        spawn(c, monsters, EntitySpider.class, 100, 4, 4);
        spawn(c, monsters, EntityZombie.class, 95, 4, 4);
        spawn(c, monsters, EntityZombieVillager.class, 5, 1, 1);
        spawn(c, monsters, EntitySkeleton.class, 100, 4, 4);
        spawn(c, monsters, EntityCreeper.class, 100, 4, 4);
        spawn(c, monsters, EntitySlime.class, 100, 4, 4);
        spawn(c, monsters, EntityEnderman.class, 10, 1, 4);
        spawn(c, monsters, EntityWitch.class, 5, 1, 1);
        List<Biome.SpawnListEntry> ambient = grove.getSpawnableList(EnumCreatureType.AMBIENT);
        c.eq(1, ambient.size(), "one ambient");
        spawn(c, ambient, EntityBat.class, 10, 8, 8);
        c.ok(grove.getSpawnableList(EnumCreatureType.WATER_CREATURE).isEmpty(), "no water creatures (1.20's list is empty)");
        c.eq(0.1F, grove.getSpawningChance(), "creature spawn probability 0.1");
    }

    private static void spawn(Check c, List<Biome.SpawnListEntry> list, Class<?> type, int weight, int min, int max) {
        for (Biome.SpawnListEntry e : list) {
            if (e.entityClass == type) {
                c.ok(e.itemWeight == weight && e.minGroupCount == min && e.maxGroupCount == max,
                        type.getSimpleName() + " " + weight + " " + min + "-" + max + ": got " + e.itemWeight + " " + e.minGroupCount + "-" + e.maxGroupCount);
                return;
            }
        }
        c.fail("no " + type.getSimpleName() + " entry");
    }

    private static void colors(Check c, BiomeCherryGrove grove) {
        c.eq(0xB6DB61, BiomeCherryGrove.GRASS_COLOR, "grass color");
        c.eq(0xB6DB61, BiomeCherryGrove.FOLIAGE_COLOR, "foliage color");
        c.eq(0x5DB7EF, BiomeCherryGrove.WATER_COLOR, "water color");
        c.eq(0x5DB7EF, BiomeCherryGrove.WATER_FOG_COLOR, "water fog color");
        c.eq(0xFFFFFF, grove.getWaterColorMultiplier(), "the tint over the blue texture is white (every channel above 1.13's blue)");
        // 1.12.2's sky formula at temperature 0.5 gives 1.20's sky color.
        float f = MathHelper.clamp(0.5F / 3.0F, -1.0F, 1.0F);
        c.eq(BiomeCherryGrove.SKY_COLOR, hsv(0.62222224F - f * 0.05F, 0.5F + f * 0.1F, 1.0F), "sky 0x7BA4FF from 1.12.2's formula");
        c.eq(0xC0D8FF, BiomeCherryGrove.FOG_COLOR, "fog is 1.12.2's default");
        // 1.20.6's Biome.BIOME_INFO_NOISE.getValue(x, z, false), evaluated with the 1.20.6 jar itself.
        double[][] noise = {{0, 0, 0.0}, {-2.88, -2.96, 0.3350935197050871}, {1.5, 7.25, -0.38497102930610994},
                {-40.08, 13.36, -0.17708698375489296}, {123.4, -567.8, 0.24512905839140722}};
        for (double[] n : noise) {
            c.ok(Math.abs(BiomeCherryGrove.infoNoise(n[0], n[1]) - n[2]) < 1e-12, "biome info noise at " + n[0] + ", " + n[1] + " is 1.20.6's");
        }

        BiomeEvent.GetWaterColor same = new BiomeEvent.GetWaterColor(grove, BiomeCherryGrove.WATER_TINT);
        MinecraftForge.EVENT_BUS.post(same);
        c.eq(BiomeCherryGrove.WATER_TINT, same.getNewColor(), "nothing replacing the tint: it stays");
        BiomeEvent.GetWaterColor replaced = new BiomeEvent.GetWaterColor(grove, 0x3F76E4);
        MinecraftForge.EVENT_BUS.post(replaced);
        c.eq(BiomeCherryGrove.WATER_COLOR, replaced.getNewColor(), "a replaced tint (1.13's texture): the exact color");
        BiomeEvent.GetWaterColor other = new BiomeEvent.GetWaterColor(Biomes.PLAINS, 0x3F76E4);
        MinecraftForge.EVENT_BUS.post(other);
        c.eq(0x3F76E4, other.getNewColor(), "other biomes untouched");
    }

    /** 1.12.2's {@code MathHelper.hsvToRGB}, which is client-only. */
    static int hsv(float hue, float saturation, float value) {
        int i = (int) (hue * 6.0F) % 6;
        float f = hue * 6.0F - (float) i;
        float f1 = value * (1.0F - saturation);
        float f2 = value * (1.0F - f * saturation);
        float f3 = value * (1.0F - (1.0F - f) * saturation);
        float[][] rgb = {{value, f3, f1}, {f2, value, f1}, {f1, value, f3}, {f1, f2, value}, {f3, f1, value}, {value, f1, f2}};
        int r = MathHelper.clamp((int) (rgb[i][0] * 255.0F), 0, 255);
        int g = MathHelper.clamp((int) (rgb[i][1] * 255.0F), 0, 255);
        int b = MathHelper.clamp((int) (rgb[i][2] * 255.0F), 0, 255);
        return r << 16 | g << 8 | b;
    }

    // ---- 1.12.2 layers ---------------------------------------------------------------------------------------

    private static final int TILE = 256;
    private static final int TILES = 8;

    /**
     * The final layers as a world gets them: 1.12.2's, then every {@code InitBiomeGens} listener (Backport Core's and
     * ours), with the grove on or off.
     */
    static GenLayer[] layersFor(long seed, WorldType type, boolean grove) {
        boolean was = ModConfig.cherryGrove.enabled;
        ModConfig.cherryGrove.enabled = grove;
        try {
            GenLayer[] layers = GenLayer.initializeAllBiomeGenerators(seed, type, null);
            WorldTypeEvent.InitBiomeGens event = new WorldTypeEvent.InitBiomeGens(type, seed, layers);
            MinecraftForge.TERRAIN_GEN_BUS.post(event);
            return event.getNewBiomeGens();
        } finally {
            ModConfig.cherryGrove.enabled = was;
        }
    }

    /** The grove over the layers for several seeds: what changes, and how much. */
    private static void layers(Check c, BiomeCherryGrove grove) {
        int groveId = Biome.getIdForBiome(grove);
        long cells = 0;
        long groveCells = 0;
        long hillsCells = 0;
        long wrong = 0;
        long heightWrong = 0;
        long fine = 0;
        long fineGrove = 0;
        long fineWrong = 0;
        boolean sameZoom = true;
        for (long seed : new long[] {1013L, 1L, 42L, -2903490073629024182L, 8675309L}) {
            GenLayer[] vanilla = layersFor(seed, WorldType.DEFAULT, false);
            GenLayer[] ours = layersFor(seed, WorldType.DEFAULT, true);
            sameZoom &= vanilla[1].getClass() == ours[1].getClass();
            for (int tx = -TILES / 2; tx < TILES / 2; tx++) {
                for (int tz = -TILES / 2; tz < TILES / 2; tz++) {
                    IntCache.resetIntCache();
                    int[] a = vanilla[0].getInts(tx * TILE, tz * TILE, TILE, TILE).clone();
                    IntCache.resetIntCache();
                    int[] b = ours[0].getInts(tx * TILE, tz * TILE, TILE, TILE).clone();
                    for (int i = 0; i < TILE * TILE; i++) {
                        cells++;
                        if (GroveTestAccess.takes(a[i])) {
                            hillsCells++;
                        }
                        if (b[i] == groveId) {
                            groveCells++;
                            Biome was = Biome.getBiome(a[i]);
                            if (!GroveTestAccess.takes(a[i])) {
                                wrong++;
                            } else if (was.getBaseHeight() != grove.getBaseHeight() || was.getHeightVariation() != grove.getHeightVariation()) {
                                heightWrong++;
                            }
                        } else if (b[i] != a[i]) {
                            wrong++;
                        }
                    }
                }
            }
            // The 1:1 map, a 2048-block square: grove only on hills, everything else as without it.
            for (int t = 0; t < 4; t++) {
                int x0 = -1024 + (t & 1) * 1024;
                int z0 = -1024 + (t >> 1) * 1024;
                IntCache.resetIntCache();
                int[] a = vanilla[1].getInts(x0, z0, 1024, 1024).clone();
                IntCache.resetIntCache();
                int[] b = ours[1].getInts(x0, z0, 1024, 1024).clone();
                for (int i = 0; i < 1024 * 1024; i++) {
                    fine++;
                    if (b[i] == groveId) {
                        fineGrove++;
                        if (!GroveTestAccess.takes(a[i])) {
                            fineWrong++;
                        }
                    } else if (b[i] != a[i]) {
                        fineWrong++;
                    }
                }
            }
        }
        IntCache.resetIntCache();
        double share = groveCells / (double) cells;
        TrailsAndTalesBackport.LOGGER.info("SELFTEST grove census: {} cells, extreme hills {} ({}%), grove {} ({}%, {}% of the hills); 1:1 {} of {} ({}%)",
                cells, hillsCells, String.format("%.3f", 100.0 * hillsCells / cells), groveCells, String.format("%.3f", 100.0 * share),
                String.format("%.1f", 100.0 * groveCells / Math.max(1, hillsCells)), fineGrove, fine, String.format("%.3f", 100.0 * fineGrove / fine));
        c.ok(sameZoom, "the 1:1 zoom is the same layer with and without the grove (Backport Core's swap still happens)");
        c.eq(0L, wrong, "the 1:4 map changes only extreme hills, and only to grove");
        c.eq(0L, heightWrong, "every grove cell has the height numbers of the hills it replaced (terrain unchanged)");
        c.eq(0L, fineWrong, "the 1:1 map changes only extreme hills, and only to grove");
        c.ok(share >= SHARE_LOW && share <= SHARE_HIGH, "grove share " + share + " near 1.20's 0.30%");
        double fineShare = fineGrove / (double) fine;
        c.ok(fineShare >= SHARE_LOW / 4 && fineShare <= SHARE_HIGH * 4, "the 1:1 map has groves too: " + fineShare);
    }

    /** The world's own biome provider went through the event: its maps are the ones with groves. */
    private static void worldLayers(Check c, BiomeCherryGrove grove) {
        long seed = c.world.getSeed();
        BiomeProvider provider = c.world.getBiomeProvider();
        if (!layered(c)) {
            TrailsAndTalesBackport.LOGGER.info("SELFTEST grove: the world's biomes come from {}, not 1.12.2's layers", provider.getClass().getName());
            return;
        }
        GenLayer[] ours = layersFor(seed, c.world.getWorldType(), true);
        int n = 512;
        IntCache.resetIntCache();
        int[] expected = ours[0].getInts(-256, -256, n, n).clone();
        Biome[] got = provider.getBiomesForGeneration(null, -256, -256, n, n);
        int differ = 0;
        for (int i = 0; i < n * n; i++) {
            if (Biome.getIdForBiome(got[i]) != expected[i]) {
                differ++;
            }
        }
        c.eq(0, differ, "the world's 1:4 map is the event's, groves included");
        IntCache.resetIntCache();
        int[] expectedFine = ours[1].getInts(-1024, -1024, n, n).clone();
        Biome[] gotFine = provider.getBiomes(null, -1024, -1024, n, n, false);
        differ = 0;
        for (int i = 0; i < n * n; i++) {
            if (Biome.getIdForBiome(gotFine[i]) != expectedFine[i]) {
                differ++;
            }
        }
        c.eq(0, differ, "the world's 1:1 map is the event's, groves included");
    }

    /** Whether the world's biomes come from 1.12.2's layers (not, say, Caves & Cliffs' 1.18 terrain). */
    static boolean layered(Check c) {
        return c.world.getBiomeProvider().getClass() == BiomeProvider.class;
    }

    // ---- decoration pieces ----------------------------------------------------------------------------------

    private static void heightmaps(Check c) {
        BlockPos o = c.area();
        c.fill(o, o.add(31, 0, 31), Blocks.GRASS.getDefaultState());
        Chunk chunk = c.world.getChunk(o);
        // Leaves over grass.
        BlockPos leaves = o.add(2, 0, 2);
        c.world.setBlockState(leaves.up(6), Blocks.LEAVES.getDefaultState(), 2 | 16);
        c.eq(leaves.up(7), GroveTestAccess.worldSurface(c.world, leaves), "world surface above leaves");
        c.eq(leaves.up(7), GroveTestAccess.motionBlocking(c.world, leaves), "leaves block motion");
        c.eq(leaves.up(7), GroveTestAccess.oceanFloor(c.world, leaves), "leaves are an ocean floor");
        c.eq(leaves.up(), GroveTestAccess.worldSurfaceWG(c.world, leaves), "the worldgen surface is under the leaves");
        c.eq(leaves.up(), GroveTestAccess.oceanFloorWG(c.world, leaves), "the worldgen ocean floor too");
        // Water over grass.
        BlockPos water = o.add(4, 0, 4);
        c.fill(water.up(), water.up(3), Blocks.WATER.getDefaultState());
        c.eq(water.up(4), GroveTestAccess.worldSurface(c.world, water), "world surface above water");
        c.eq(water.up(4), GroveTestAccess.motionBlocking(c.world, water), "water counts for motion blocking");
        c.eq(water.up(), GroveTestAccess.oceanFloor(c.world, water), "ocean floor under water");
        c.eq(water.up(4), GroveTestAccess.worldSurfaceWG(c.world, water), "the worldgen surface is above water");
        c.eq(water.up(), GroveTestAccess.oceanFloorWG(c.world, water), "the worldgen ocean floor under it: 3 deep");
        // A plant on grass.
        BlockPos plant = o.add(6, 0, 6);
        c.world.setBlockState(plant.up(), Blocks.TALLGRASS.getDefaultState().withProperty(BlockTallGrass.TYPE, BlockTallGrass.EnumType.GRASS), 2 | 16);
        c.eq(plant.up(2), GroveTestAccess.worldSurface(c.world, plant), "world surface above a plant");
        c.eq(plant.up(), GroveTestAccess.motionBlocking(c.world, plant), "a plant does not block motion");
        c.eq(plant.up(), GroveTestAccess.oceanFloor(c.world, plant), "nor is it an ocean floor");
        c.eq(plant.up(), GroveTestAccess.worldSurfaceWG(c.world, plant), "the worldgen surface does not see the plant");
        // Where a cherry sapling would stand.
        c.ok(GroveTestAccess.saplingSurvives(c.world, o.add(8, 1, 8)), "a sapling stands on grass");
        c.world.setBlockState(o.add(10, 0, 10), Blocks.STONE.getDefaultState(), 2 | 16);
        c.ok(!GroveTestAccess.saplingSurvives(c.world, o.add(10, 1, 10)), "not on stone");
        BlockPos spot = o.add(12, 1, 12);
        c.ok(GroveTestAccess.validTreePos(c.world, spot), "a tree may start in air");
        c.world.setBlockState(spot, Blocks.TALLGRASS.getDefaultState().withProperty(BlockTallGrass.TYPE, BlockTallGrass.EnumType.GRASS), 2 | 16);
        c.ok(GroveTestAccess.validTreePos(c.world, spot), "or in grass (#replaceable_by_trees)");
        c.world.setBlockState(spot, CherryContent.petals.getDefaultState(), 2 | 16);
        c.ok(!GroveTestAccess.validTreePos(c.world, spot), "not in pink petals");
        c.ok(chunk.isLoaded(), "area chunk loaded");
    }

    private static void patches(Check c) {
        BlockPinkPetals petals = CherryContent.petals;
        Random rand = new Random(120);
        Set<IBlockState> seen = new HashSet<IBlockState>();
        int placed = 0;
        int floating = 0;
        for (int n = 0; n < 4; n++) {
            BlockPos o = c.area();
            c.fill(o, o.add(31, 0, 31), Blocks.GRASS.getDefaultState());
            BlockPos origin = o.add(16, 1, 16);
            placed += GroveTestAccess.petals(c.world, rand, origin, petals);
            for (BlockPos p : BlockPos.getAllInBox(origin.add(-6, -2, -6), origin.add(6, 2, 6))) {
                IBlockState s = c.world.getBlockState(p);
                if (s.getBlock() == petals) {
                    seen.add(s);
                    if (c.world.getBlockState(p.down()).getBlock() != Blocks.GRASS) {
                        floating++;
                    }
                }
            }
        }
        // 1.20's RandomPatchFeature on flat ground, simulated: 27.8 petals a patch (96 tries, 6 across, 2 up or down).
        c.ok(placed >= 4 * 22 && placed <= 4 * 34, "four flower_cherry patches place about 4 x 27.8 petals: " + placed);
        c.eq(0, floating, "every petal on grass");
        c.ok(seen.size() >= 12, "facings and amounts drawn from all sixteen states: " + seen.size());

        BlockPos o = c.area();
        c.fill(o, o.add(31, 0, 31), Blocks.GRASS.getDefaultState());
        int grass = 0;
        for (int n = 0; n < 10; n++) {
            BlockPos g = c.area();
            c.fill(g, g.add(31, 0, 31), Blocks.GRASS.getDefaultState());
            grass += GroveTestAccess.grassPatch(c.world, rand, g.add(16, 1, 16));
        }
        int tall = GroveTestAccess.tallGrassPatch(c.world, rand, o.add(16, 1, 16));
        // Simulated: 7.8 a patch (32 tries, 7 across, 3 up or down).
        c.ok(grass >= 10 * 6 && grass <= 10 * 10, "ten patch_grass patches place about 10 x 7.8 grass: " + grass);
        c.ok(tall > 0, "patch_tall_grass places tall grass: " + tall);
        int halves = 0;
        int lone = 0;
        for (BlockPos p : BlockPos.getAllInBox(o.add(8, 1, 8), o.add(24, 5, 24))) {
            IBlockState s = c.world.getBlockState(p);
            if (s.getBlock() == Blocks.DOUBLE_PLANT && s.getValue(BlockDoublePlant.HALF) == BlockDoublePlant.EnumBlockHalf.LOWER) {
                halves++;
                if (c.world.getBlockState(p.up()).getBlock() != Blocks.DOUBLE_PLANT) {
                    lone++;
                }
            }
        }
        c.eq(tall, halves, "each tall grass placed is a lower half");
        c.eq(0, lone, "with its upper half");
    }

    // ---- other backports -------------------------------------------------------------------------------------

    private static void integration(Check c, BiomeCherryGrove grove) throws Exception {
        if (Loader.isModLoaded("buzzybeesbackport")) {
            Class<?> nests = Class.forName("com.exiledradio.buzzybeesbackport.api.BeeNests");
            Class<?> flowers = Class.forName("com.exiledradio.buzzybeesbackport.api.BeeFlowers");
            c.eq(0.05F, nests.getMethod("chance", Biome.class, boolean.class).invoke(null, grove, false), "Buzzy Bees: 5% per tree in the grove");
            c.eq(Boolean.TRUE, nests.getMethod("isNestTree", IBlockState.class).invoke(null, CherryContent.set.log.getDefaultState()), "cherry logs carry nests");
            c.eq(Boolean.TRUE, flowers.getMethod("isFlower", IBlockState.class).invoke(null, CherryContent.petals.getDefaultState()), "pink petals are flowers");
            c.eq(Boolean.TRUE, flowers.getMethod("isFlower", IBlockState.class).invoke(null, CherryContent.leaves.getDefaultState()), "cherry leaves are flowers");
            c.eq(Boolean.TRUE, flowers.getMethod("isFlowerItem", net.minecraft.item.ItemStack.class).invoke(null,
                    new net.minecraft.item.ItemStack(CherryContent.petals)), "the pink petals item is a flower item");
        }
    }

    /**
     * Decoration density over several groves (whole grove chunks at least 40 chunks apart, each with its 3x3
     * decorated), per 256 grove columns: logs, leaves, petals and grass. Logged, to compare 1.12.2 terrain with
     * Caves &amp; Cliffs' 1.20.1 port in its worlds.
     */
    private static void density(Check c, BiomeCherryGrove grove) {
        BiomeProvider provider = c.world.getBiomeProvider();
        List<BlockPos> groves = new java.util.ArrayList<BlockPos>();
        for (int r = 0; r < 400 && groves.size() < 4; r++) {
            for (int dx = -r; dx <= r && groves.size() < 4; dx++) {
                for (int dz = -r; dz <= r && groves.size() < 4; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    int cx = dx * 2;
                    int cz = dz * 2;
                    boolean far = true;
                    for (BlockPos g : groves) {
                        far &= Math.max(Math.abs(g.getX() - cx), Math.abs(g.getZ() - cz)) >= 40;
                    }
                    if (!far) {
                        continue;
                    }
                    boolean all = true;
                    for (Biome x : provider.getBiomes(null, cx * 16, cz * 16, 16, 16, false)) {
                        if (x != grove) {
                            all = false;
                            break;
                        }
                    }
                    if (all) {
                        groves.add(new BlockPos(cx, 0, cz));
                    }
                }
            }
        }
        long[] stats = new long[6];
        GroveTestAccess.recordStats(stats);
        Block log = CherryContent.set.log;
        long columns = 0;
        long slope = 0;
        long steep = 0;
        long[] per = new long[4];
        long[] inner = new long[2];
        long innerColumns = 0;
        for (BlockPos g : groves) {
            for (int x = g.getX() - 1; x <= g.getX() + 2; x++) {
                for (int z = g.getZ() - 1; z <= g.getZ() + 2; z++) {
                    c.world.getChunk(x, z);
                }
            }
            for (int x = (g.getX() - 1) * 16; x < (g.getX() + 2) * 16; x++) {
                for (int z = (g.getZ() - 1) * 16; z < (g.getZ() + 2) * 16; z++) {
                    if (c.world.getBiome(new BlockPos(x, 0, z)) != grove) {
                        continue;
                    }
                    columns++;
                    boolean inside = c.world.getBiome(new BlockPos(x + 7, 0, z)) == grove && c.world.getBiome(new BlockPos(x - 7, 0, z)) == grove
                            && c.world.getBiome(new BlockPos(x, 0, z + 7)) == grove && c.world.getBiome(new BlockPos(x, 0, z - 7)) == grove;
                    int h = GroveTestAccess.oceanFloorWG(c.world, new BlockPos(x, 0, z)).getY();
                    int east = GroveTestAccess.oceanFloorWG(c.world, new BlockPos(x + 1, 0, z)).getY();
                    slope += Math.abs(east - h);
                    steep += Math.abs(east - h) > 2 ? 1 : 0;
                    for (int y = 1; y < 256; y++) {
                        Block b = c.world.getBlockState(new BlockPos(x, y, z)).getBlock();
                        per[0] += b == log ? 1 : 0;
                        per[1] += b == CherryContent.leaves ? 1 : 0;
                        per[2] += b == CherryContent.petals ? 1 : 0;
                        per[3] += b == Blocks.TALLGRASS || b == Blocks.DOUBLE_PLANT ? 1 : 0;
                        if (inside) {
                            inner[0] += b == CherryContent.petals ? 1 : 0;
                            inner[1] += b == Blocks.TALLGRASS || b == Blocks.DOUBLE_PLANT ? 1 : 0;
                        }
                    }
                    innerColumns += inside ? 1 : 0;
                }
            }
        }
        TrailsAndTalesBackport.LOGGER.info("SELFTEST grove interior ({} columns 7 or more from an edge), per 256: {} petals, {} grass",
                innerColumns, String.format("%.1f", 256.0 * inner[0] / Math.max(1, innerColumns)), String.format("%.1f", 256.0 * inner[1] / Math.max(1, innerColumns)));
        TrailsAndTalesBackport.LOGGER.info("SELFTEST grove density over {} groves ({} grove columns), per 256 columns: {} logs, {} leaves, {} petals, {} grass",
                groves.size(), columns, String.format("%.1f", 256.0 * per[0] / Math.max(1, columns)),
                String.format("%.1f", 256.0 * per[1] / Math.max(1, columns)), String.format("%.1f", 256.0 * per[2] / Math.max(1, columns)),
                String.format("%.1f", 256.0 * per[3] / Math.max(1, columns)));
        GroveTestAccess.recordStats(null);
        TrailsAndTalesBackport.LOGGER.info("SELFTEST grove decorator: {} chunks, {} grass patches placing {}, {} petal patches placing {} ({} starting on a canopy)",
                stats[0], stats[1], stats[2], stats[3], stats[4], stats[5]);
        TrailsAndTalesBackport.LOGGER.info("SELFTEST grove terrain: mean height step to the east {}, steps over 2 in {}% of columns",
                String.format("%.2f", slope / (double) Math.max(1, columns)), String.format("%.1f", 100.0 * steep / Math.max(1, columns)));
        c.ok(groves.size() >= 2 && columns > 0, "several groves to measure: " + groves.size());
    }

    // ---- a real grove ----------------------------------------------------------------------------------------

    /** The nearest whole chunk of grove to 0, 0, decorated, counted. */
    private static void realGrove(Check c, BiomeCherryGrove grove) {
        BiomeProvider provider = c.world.getBiomeProvider();
        BlockPos found = null;
        search:
        for (int r = 0; r < 256 && found == null; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    int cx = dx * 2;
                    int cz = dz * 2;
                    Biome[] b = provider.getBiomes(null, cx * 16, cz * 16, 16, 16, false);
                    boolean all = true;
                    for (Biome x : b) {
                        if (x != grove) {
                            all = false;
                            break;
                        }
                    }
                    if (all) {
                        found = new BlockPos(cx * 16, 0, cz * 16);
                        break search;
                    }
                }
            }
        }
        if (!c.ok(found != null, "a whole chunk of grove within 8192 blocks")) {
            return;
        }
        int cx = found.getX() >> 4;
        int cz = found.getZ() >> 4;
        List<BlockPos> tries = new java.util.ArrayList<BlockPos>();
        GroveTestAccess.recordTrees(tries);
        final int[] cascades = {0};
        Object probe = new Object() {
            @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
            public void load(net.minecraftforge.event.world.ChunkEvent.Load e) {
                StringBuilder b = new StringBuilder();
                for (StackTraceElement el : Thread.currentThread().getStackTrace()) {
                    String n = el.getClassName();
                    if (n.contains("trailsandtales") || n.contains("gen.feature") || n.contains("biome") || n.contains("ChunkGenerator")
                            || n.contains("structure") || n.contains("Chunk")) {
                        b.append(n.substring(n.lastIndexOf('.') + 1)).append('.').append(el.getMethodName()).append(':').append(el.getLineNumber()).append(' ');
                    }
                }
                if (b.indexOf("CherryGroveDecorator") >= 0 || b.indexOf("CherryTree") >= 0) {
                    cascades[0]++;
                    TrailsAndTalesBackport.LOGGER.info("SELFTEST grove chunk load {} {} during population: {}", e.getChunk().x, e.getChunk().z, b);
                }
            }
        };
        MinecraftForge.EVENT_BUS.register(probe);
        for (int x = cx - 1; x <= cx + 2; x++) {
            for (int z = cz - 1; z <= cz + 2; z++) {
                c.world.getChunk(x, z);
            }
        }
        GroveTestAccess.recordTrees(null);
        MinecraftForge.EVENT_BUS.unregister(probe);
        c.eq(0, cascades[0], "the grove's decoration loads no chunk");
        Chunk chunk = c.world.getChunk(cx, cz);
        c.ok(chunk.isTerrainPopulated(), "the grove chunk is populated");
        Block log = CherryContent.set.log;
        int logs = 0;
        int leaves = 0;
        int petals = 0;
        int grass = 0;
        int tallGrass = 0;
        int oak = 0;
        int grassTop = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 1; y < 256; y++) {
                    Block b = chunk.getBlockState(x, y, z).getBlock();
                    if (b == log) {
                        logs++;
                    } else if (b == CherryContent.leaves) {
                        leaves++;
                    } else if (b == CherryContent.petals) {
                        petals++;
                    } else if (b == Blocks.TALLGRASS) {
                        grass++;
                    } else if (b == Blocks.DOUBLE_PLANT) {
                        tallGrass++;
                    } else if (b == Blocks.LOG || b == Blocks.LOG2) {
                        oak++;
                    }
                }
                BlockPos top = c.world.getTopSolidOrLiquidBlock(new BlockPos(found.getX() + x, 0, found.getZ() + z)).down();
                Block t = c.world.getBlockState(top).getBlock();
                if (t == Blocks.GRASS || t == Blocks.DIRT || t == log || t == CherryContent.leaves) {
                    grassTop++;
                }
            }
        }
        TrailsAndTalesBackport.LOGGER.info("SELFTEST grove chunk {} {}: {} logs, {} leaves, {} petals, {} grass, {} tall grass blocks, {} vanilla logs, {} grass/dirt/tree tops; {} tree tries in the 4x4",
                cx, cz, logs, leaves, petals, grass, tallGrass, oak, grassTop, tries.size());
        // Per grove column over the decorated 3x3, to compare with Caves & Cliffs' 1.20.1 port in its worlds.
        int columns = 0;
        int[] per = new int[4];
        for (int x = (cx - 1) * 16; x < (cx + 2) * 16; x++) {
            for (int z = (cz - 1) * 16; z < (cz + 2) * 16; z++) {
                if (c.world.getBiome(new BlockPos(x, 0, z)) != grove) {
                    continue;
                }
                columns++;
                for (int y = 1; y < 256; y++) {
                    Block b = c.world.getBlockState(new BlockPos(x, y, z)).getBlock();
                    per[0] += b == log ? 1 : 0;
                    per[1] += b == CherryContent.leaves ? 1 : 0;
                    per[2] += b == CherryContent.petals ? 1 : 0;
                    per[3] += b == Blocks.TALLGRASS || b == Blocks.DOUBLE_PLANT ? 1 : 0;
                }
            }
        }
        TrailsAndTalesBackport.LOGGER.info("SELFTEST grove 3x3 ({} grove columns), per 256 columns: {} logs, {} leaves, {} petals, {} grass",
                columns, String.format("%.1f", 256.0 * per[0] / columns), String.format("%.1f", 256.0 * per[1] / columns),
                String.format("%.1f", 256.0 * per[2] / columns), String.format("%.1f", 256.0 * per[3] / columns));
        c.ok(logs > 0, "cherry logs: " + logs);
        c.ok(leaves > logs, "more leaves than logs: " + leaves);
        c.ok(petals > 0, "pink petals: " + petals);
        c.ok(grass > 0, "grass: " + grass);
        c.eq(0, oak, "no oak or other vanilla trees (1.12.2's extreme hills decoration does not run)");
        c.ok(grassTop > 200, "grass, dirt or trees on top in most columns: " + grassTop);
        if (!layered(c)) {
            // Caves & Cliffs' 1.18 terrain places 1.20's features itself; this biome's decorator does not run.
            c.ok(tries.isEmpty(), "the 1.18 terrain decorates the grove itself: our decorator did not run");
            return;
        }
        c.ok(!tries.isEmpty(), "trees were tried");
        int stand = 0;
        for (BlockPos p : tries) {
            Block below = c.world.getBlockState(p.down()).getBlock();
            if (below == Blocks.DIRT || below == Blocks.GRASS) {
                stand++;
            }
        }
        c.eq(tries.size(), stand, "every tree try stands on grass or dirt");

        if (Loader.isModLoaded("buzzybeesbackport")) {
            // Buzzy Bees' populate pass over the decorated 3x3, with every trunk given a nest: it finds the cherry
            // trees' trunks.
            try {
                java.lang.reflect.Method scan = Class.forName("com.exiledradio.buzzybeesbackport.nestgen.NestGenerator")
                        .getMethod("scan", net.minecraft.world.World.class, Random.class, int.class, int.class, boolean.class);
                int placed = 0;
                for (int x = cx - 1; x <= cx + 1; x++) {
                    for (int z = cz - 1; z <= cz + 1; z++) {
                        placed += (Integer) scan.invoke(null, c.world, new Random(5), x * 16, z * 16, true);
                    }
                }
                TrailsAndTalesBackport.LOGGER.info("SELFTEST grove: Buzzy Bees put {} nests on the 3x3's cherry trees", placed);
                c.ok(placed > 0, "Buzzy Bees' nest pass finds cherry trunks: " + placed);
            } catch (ReflectiveOperationException e) {
                c.fail("Buzzy Bees' NestGenerator.scan: " + e);
            }
        }
    }
}
