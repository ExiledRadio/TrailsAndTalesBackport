package com.exiledradio.trailsandtalesbackport.structure;

import com.exiledradio.backportcore.locate.StructureLocate;
import com.exiledradio.backportcore.structure.JigsawPools;
import com.exiledradio.backportcore.structure.ModernPalette;
import com.exiledradio.backportcore.structure.ModernProcessors;
import com.exiledradio.backportcore.structure.ScatteredStructure;
import com.exiledradio.backportcore.structure.Structures;
import com.exiledradio.backportcore.structure.TerrainAdaptation;
import com.exiledradio.backportcore.terrain.WorldHeights;
import com.exiledradio.backportcore.worldgen.FlatWorlds;
import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.archaeology.ArchaeologyContent;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.structure.MapGenStructureIO;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent;
import net.minecraftforge.fml.common.IWorldGenerator;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemRecord;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import com.exiledradio.trailsandtalesbackport.common.Content;

import java.util.Random;

/**
 * Stream St: everywhere 1.20 puts suspicious sand and gravel.
 *
 * <ul>
 *   <li><b>Trail ruins</b> ({@link TrailRuinsStructure}): 1.20.1's structure set, structure, pools, processor lists,
 *       tags and 84 templates, unchanged, laid out by 1.20.1's jigsaw rules ({@link TrailLayout}), placed by Backport
 *       Core's templates and scattered driver, buried 15 blocks under the surface, with suspicious gravel from the
 *       {@code capped} archaeology processors ({@link Archaeology}). {@code /locate structure minecraft:trail_ruins}.</li>
 *   <li><b>Desert pyramids</b> ({@link DesertPyramids}): 1.20's cellar and its suspicious sand, in whichever
 *       generator's pyramid (1.12.2's, Backport Core's, Caves &amp; Cliffs').</li>
 *   <li><b>Desert wells</b> ({@link DesertWells}): 1.20's well in 1.12.2's terrain.</li>
 *   <li><b>Ocean ruins</b> ({@link OceanRuins}): Update Aquatic's, when it is installed.</li>
 * </ul>
 * All of it needs the archaeology stream's blocks; with archaeology off nothing here generates. The music disc
 * <b>Relic</b> (trail ruins' rare loot) is registered here; archaeology's loot adds it to the table.
 */
@Mod.EventBusSubscriber(modid = TrailsAndTalesBackport.MOD_ID)
public final class StructuresContent {

    public static final SoundEvent RELIC_SOUND = new SoundEvent(Content.id("music_disc.relic")).setRegistryName(Content.id("music_disc.relic"));
    public static Item relic;

    /** 1.20's music disc Relic by Aaron Cherof: 1.12.2's record item, named "Music Disc" as every disc. */
    static final class ItemRelic extends ItemRecord {
        ItemRelic() {
            super("relic", RELIC_SOUND);
        }
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (enabled()) {
            relic = Content.item(event.getRegistry(), "music_disc_relic", new ItemRelic());
            relic.setTranslationKey("record");
        }
    }

    /** Decided 3's fallback loot for the items whose structure's owner is absent ({@link FallbackLoot}). */
    @SubscribeEvent
    public static void onLootTableLoad(net.minecraftforge.event.LootTableLoadEvent event) {
        if (enabled() && ArchaeologyContent.enabled()) {
            FallbackLoot.onLoad(event);
        }
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        if (enabled()) {
            event.getRegistry().register(RELIC_SOUND);
        }
    }

    /**
     * Blocks of the trail ruins' files that no family mod installed may provide: Wild Update's mud and mud bricks,
     * Village &amp; Pillage's 1.14 workstations, campfire and brick wall. The owner's block is always used when present.
     */
    static final String[][] FALLBACKS = {
            {"minecraft:mud", "minecraft:dirt"},
            {"minecraft:packed_mud", "minecraft:brown_terracotta"},
            {"minecraft:mud_bricks", "minecraft:terracotta"},
            {"minecraft:mud_brick_slab", "minecraft:sandstone_slab"},
            {"minecraft:mud_brick_stairs", "minecraft:sandstone_stairs"},
            {"minecraft:mud_brick_wall", "minecraft:cobblestone_wall"},
            {"minecraft:brick_wall", "minecraft:cobblestone_wall"},
            {"minecraft:blast_furnace", "minecraft:furnace"},
            {"minecraft:cartography_table", "minecraft:crafting_table"},
            {"minecraft:smithing_table", "minecraft:crafting_table"},
            {"minecraft:loom", "minecraft:crafting_table"},
            {"minecraft:grindstone", "minecraft:air"},
            {"minecraft:composter", "minecraft:air"},
            {"minecraft:campfire", "minecraft:air"},
    };

    private StructuresContent() {
    }

    public static boolean enabled() {
        return ModConfig.structures.enabled;
    }

    /** @return true when new chunks get this stream's structures (it needs archaeology's blocks) */
    public static boolean generates() {
        return enabled() && ArchaeologyContent.enabled();
    }

    /** Pre-init: this jar holds 1.20.1's files under their minecraft: names. */
    public static void preInit() {
        if (!enabled()) {
            return;
        }
        JigsawPools.source(TrailsAndTalesBackport.MOD_ID);
        ModernPalette.source(TrailsAndTalesBackport.MOD_ID);
        for (String[] f : FALLBACKS) {
            ModernPalette.fallback(f[0], f[1]);
        }
        ModernProcessors.tag("minecraft:trail_ruins_replaceable", "minecraft:gravel");
    }

    /** From init. */
    public static void init() {
        if (!enabled()) {
            return;
        }
        MapGenStructureIO.registerStructure(TrailRuinsStructure.Start.class, TrailRuinsStructure.SAVE_NAME);
        MapGenStructureIO.registerStructureComponent(TrailRuinsPiece.class, TrailRuinsPiece.SAVE_NAME);
        ScatteredStructure.register(Structures.TRAIL_RUINS,
                world -> generates() && world.provider.getDimension() == 0 && FlatWorlds.structure(world, Structures.TRAIL_RUINS)
                        ? new TrailRuinsStructure() : null);
        StructureLocate.scattered("minecraft:trail_ruins", Structures.TRAIL_RUINS, null, "minecraft:trail_ruins");
        // 1.20's terrain_adaptation: bury. A terrain that adapts to structures does it in its own noise.
        TerrainAdaptation.register(Structures.TRAIL_RUINS, TerrainAdaptation.Kind.BURY, (world, cx, cz, out) -> {
            ScatteredStructure s = ScatteredStructure.of(world, Structures.TRAIL_RUINS);
            if (s instanceof TrailRuinsStructure) {
                ((TrailRuinsStructure) s).startsNear(world, cx, cz, out);
            }
        });
        // After Backport Core's scattered driver (weight 0), so Update Aquatic's ruins are built when this runs.
        GameRegistry.registerWorldGenerator(new Generator(), 1);
        MinecraftForge.TERRAIN_GEN_BUS.register(new Events());
    }

    /** The populate step's box: 1.12.2's MapGenStructure box, the world's full height. */
    static StructureBoundingBox populateBox(World world, int chunkX, int chunkZ) {
        int x = (chunkX << 4) + 8;
        int z = (chunkZ << 4) + 8;
        return new StructureBoundingBox(x, WorldHeights.minY(world), z, x + 15, WorldHeights.maxYExclusive(world) - 1, z + 15);
    }

    /** Pyramids and ocean ruins, once their builders have placed this chunk's part. */
    static final class Generator implements IWorldGenerator {
        @Override
        public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
            if (!(world instanceof WorldServer) || world.provider.getDimension() != 0 || !generates()) {
                return;
            }
            StructureBoundingBox box = populateBox(world, chunkX, chunkZ);
            try {
                for (StructureComponent pyramid : DesertPyramids.pyramidsIn((WorldServer) world, box)) {
                    Random r = new Random(world.getSeed() ^ ChunkPos.asLong(chunkX, chunkZ) * 341873128712L);
                    DesertPyramids.apply(world, pyramid, new DesertPyramids.BoundingBoxes(pyramid.getBoundingBox()), box, r);
                }
                OceanRuins.populate(world, box);
            } catch (RuntimeException e) {
                TrailsAndTalesBackport.LOGGER.error("Could not add 1.20's archaeology at chunk {}, {}.", chunkX, chunkZ, e);
            }
        }
    }

    public static final class Events {
        @SubscribeEvent(priority = EventPriority.LOW)
        public void decorate(DecorateBiomeEvent.Decorate event) {
            if (generates() && !event.getWorld().isRemote && event.getWorld().provider.getDimension() == 0) {
                DesertWells.decorate(event);
            }
        }
    }
}
