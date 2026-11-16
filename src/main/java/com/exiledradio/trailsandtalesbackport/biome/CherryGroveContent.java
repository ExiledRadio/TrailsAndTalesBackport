package com.exiledradio.trailsandtalesbackport.biome;

import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.cherry.CherryContent;
import com.exiledradio.trailsandtalesbackport.common.Content;
import com.exiledradio.trailsandtalesbackport.common.Owners;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.terraingen.BiomeEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.lang.reflect.Method;
import java.util.function.Predicate;

/**
 * Stream G: the cherry grove biome ({@code trailsandtalesbackport:cherry_grove}). Config {@code cherryGrove}; no
 * packets (ids 20-24 unused), no entities.
 *
 * <ul>
 *   <li><b>The biome</b>: {@link BiomeCherryGrove}, decorated by {@link CherryGroveDecorator}. It needs the cherry
 *       stream's trees, leaves, sapling and petals, so it is registered only when both streams are on.</li>
 *   <li><b>1.12.2's terrain</b>: {@link CherryGroveLayer}, part of the extreme hills (<i>Decided</i> 1).</li>
 *   <li><b>Caves &amp; Cliffs' 1.18 terrain</b>: that world, when made with our cherry log, leaves and petals
 *       registered, places 1.20's cherry grove by 1.20's climate and decorates it with 1.20.1's features (the
 *       same numbers as 1.20.6's), our blocks by name; its {@code BiomeMap} takes a biome a mod registers under the
 *       modern name, so its cherry grove columns are this biome (spawns, colors, music, bees). Its generator places
 *       the features itself, so this biome's decorator does not run there; it also steps aside for any generator
 *       that says it places a feature ({@link #generatorPlaces}).</li>
 *   <li><b>Bees</b> (Buzzy Bees Backport, by name): cherry logs carry nests and the grove's chance per tree is 1.20's
 *       5% ({@code cherry_bees_005}); cherry leaves and pink petals are flowers to bees, as 1.20's {@code flowers}
 *       block and item tags list them ({@link #bees}).</li>
 *   <li><b>Music</b>: 1.20's {@code music.overworld.cherry_grove}, played by 1.20's biome music ({@code music.BiomeMusic}).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TrailsAndTalesBackport.MOD_ID)
public final class CherryGroveContent {

    /** 1.20's {@code cherry_bees_005}: a nest on one cherry tree in twenty. */
    public static final float BEE_NEST_CHANCE = 0.05F;

    /** 1.20.6's {@code music.overworld.cherry_grove}, null while the stream is off. */
    public static SoundEvent music;
    /** Null while the biome is not registered. */
    public static BiomeCherryGrove biome;

    private CherryGroveContent() {
    }

    /** Whether the biome is registered: both this stream and the cherry stream are on. */
    public static boolean enabled() {
        return ModConfig.cherryGrove.enabled && CherryContent.enabled();
    }

    /** Whether new 1.12.2 biome maps get the grove. */
    public static boolean generates() {
        return biome != null && ModConfig.cherryGrove.enabled;
    }

    /** Pre-init. */
    public static void preInit() {
        if (enabled()) {
            CherryGroveLayer.register();
        }
    }

    /** Init: the other backports' tables. */
    public static void init() {
        if (biome != null && Loader.isModLoaded(Owners.BUZZY_BEES)) {
            bees();
        }
    }

    @SubscribeEvent
    public static void registerBiomes(RegistryEvent.Register<Biome> event) {
        if (!enabled()) {
            return;
        }
        biome = new BiomeCherryGrove();
        biome.setRegistryName(Content.id(BiomeCherryGrove.NAME));
        event.getRegistry().register(biome);
        // 1.20's tags: #is_mountain and #is_overworld (Forge 1.12.2 has no overworld type).
        BiomeDictionary.addTypes(biome, BiomeDictionary.Type.MOUNTAIN);
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        if (!enabled()) {
            return;
        }
        ResourceLocation id = Content.id("music.overworld.cherry_grove");
        music = new SoundEvent(id).setRegistryName(id);
        event.getRegistry().register(music);
    }

    /**
     * 1.12.2's water texture is blue and the biome's tint multiplies it; when something replaces the tint (Update
     * Aquatic's 1.13 gray texture and its colors), this runs last and gives 1.20's exact color.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void waterColor(BiomeEvent.GetWaterColor event) {
        if (biome != null && event.getBiome() == biome && event.getNewColor() != BiomeCherryGrove.WATER_TINT) {
            event.setNewColor(BiomeCherryGrove.WATER_COLOR);
        }
    }

    // --- Buzzy Bees -------------------------------------------------------------------------------------------

    private static final String BEE_NESTS = "com.exiledradio.buzzybeesbackport.api.BeeNests";
    private static final String BEE_FLOWERS = "com.exiledradio.buzzybeesbackport.api.BeeFlowers";

    /**
     * Buzzy Bees' {@code BeeNests} and {@code BeeFlowers}, by name: cherry logs (any axis) carry nests; the grove's
     * chance per tree is 5%; cherry leaves and pink petals, block and item, are flowers. Buzzy Bees' populate pass
     * then gives the grove's trees their nests in 1.12.2 terrain, and its sapling rule (5% with a flower within 2)
     * the cherry sapling's, as 1.20's {@code CherryTreeGrower} picks {@code cherry_bees_005} near flowers.
     *
     * @return true when every table took them
     */
    public static boolean bees() {
        final Block log = CherryContent.set == null ? null : CherryContent.set.log;
        final Block leaves = CherryContent.leaves;
        final Block petals = CherryContent.petals;
        if (log == null || leaves == null || petals == null) {
            return false;
        }
        try {
            Class<?> nests = Class.forName(BEE_NESTS);
            nests.getMethod("addTree", Predicate.class).invoke(null, (Predicate<IBlockState>) s -> s.getBlock() == log);
            nests.getMethod("setBiomeChance", Biome.class, float.class).invoke(null, biome, BEE_NEST_CHANCE);
            Class<?> flowers = Class.forName(BEE_FLOWERS);
            flowers.getMethod("addFlower", Predicate.class).invoke(null,
                    (Predicate<IBlockState>) s -> s.getBlock() == leaves || s.getBlock() == petals);
            final Item leavesItem = Item.getItemFromBlock(leaves);
            final Item petalsItem = Item.getItemFromBlock(petals);
            flowers.getMethod("addFlowerItem", Predicate.class).invoke(null,
                    (Predicate<ItemStack>) s -> !s.isEmpty() && (s.getItem() == leavesItem || s.getItem() == petalsItem));
            return true;
        } catch (ReflectiveOperationException | LinkageError e) {
            TrailsAndTalesBackport.LOGGER.warn("Buzzy Bees Backport's nest and flower tables were not found; cherry groves get no bee nests", e);
            return false;
        }
    }

    // --- generators that place 1.20's features themselves -----------------------------------------------------

    private static final String DECORATION_SOURCE = "com.exiledradio.backportcore.terrain.DecorationSource";
    private static boolean sourceLooked;
    private static Class<?> sourceClass;
    private static Method sourcePlaces;

    /**
     * Backport Core's {@code DecorationSource} (1.2.0 onward), asked by name so this works with the Core it compiles
     * against: whether the world's generator places this 1.20 feature itself.
     */
    public static boolean generatorPlaces(World world, String feature) {
        if (!(world instanceof WorldServer)) {
            return false;
        }
        if (!sourceLooked) {
            sourceLooked = true;
            try {
                sourceClass = Class.forName(DECORATION_SOURCE);
                sourcePlaces = sourceClass.getMethod("placesFeature", World.class, String.class);
            } catch (ReflectiveOperationException | LinkageError ignored) {
                sourceClass = null;
            }
        }
        if (sourceClass == null) {
            return false;
        }
        IChunkGenerator generator = ((WorldServer) world).getChunkProvider().chunkGenerator;
        if (!sourceClass.isInstance(generator)) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(sourcePlaces.invoke(generator, world, feature));
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }
}
