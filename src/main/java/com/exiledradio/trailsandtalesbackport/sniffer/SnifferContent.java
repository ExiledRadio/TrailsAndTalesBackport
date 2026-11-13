package com.exiledradio.trailsandtalesbackport.sniffer;

import com.exiledradio.backportcore.ownership.BlockOwnership;
import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.common.BlockPotted;
import com.exiledradio.trailsandtalesbackport.common.Compat;
import com.exiledradio.trailsandtalesbackport.common.Content;
import com.exiledradio.trailsandtalesbackport.common.Owners;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntityParrot;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemSeeds;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Stream N: the sniffer, the sniffer egg, torchflowers and pitcher plants.
 *
 * <ul>
 *   <li>Entity {@code sniffer}, id 5 of this mod's block 5-9, with its spawn egg (1.20's colors).</li>
 *   <li>Blocks {@code torchflower}, {@code potted_torchflower}, {@code torchflower_crop},
 *       {@code pitcher_crop}, {@code pitcher_plant}, {@code sniffer_egg}; items {@code torchflower_seeds}
 *       and {@code pitcher_pod} plant the crops on farmland.</li>
 *   <li>1.20's recipes: a torchflower makes an orange dye, a pitcher plant two cyan dyes.</li>
 *   <li>The sniffer egg comes from breeding here and from warm ocean ruins' suspicious sand, which the
 *       archaeology and structure streams fill; they find the item by name ({@code Owners.ours("sniffer_egg")}).</li>
 *   <li>With the family installed: dug from Caves & Cliffs' moss and rooted dirt and Wild Update's mud and
 *       muddy mangrove roots, hatching twice as fast on Caves & Cliffs' moss; torchflowers and pitcher plants
 *       are flowers to Buzzy Bees' bees; the composter (Village & Pillage) takes the seeds, pods and flowers
 *       at 1.20's chances.</li>
 *   <li>1.20 also feeds both new seeds to chickens and parrots: they are added to 1.12.2's own lists.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TrailsAndTalesBackport.MOD_ID)
public final class SnifferContent {

    public static final int SNIFFER_ID = 5;

    /** Blocks of the family that 1.20's {@code sniffer_diggable_block} tag lists, by registry path. */
    private static final Set<String> FAMILY_DIGGABLE = new HashSet<String>(Arrays.asList(
            "rooted_dirt", "moss_block", "mud", "muddy_mangrove_roots"));
    /** 1.20's {@code sniffer_egg_hatch_boost}: the moss block. */
    private static final String HATCH_BOOST = "moss_block";
    /** Mods whose blocks of those names count: the owners and any family copies. */
    private static final Set<String> FAMILY = new HashSet<String>(Arrays.asList(
            Owners.CAVES_AND_CLIFFS, Owners.WILD_UPDATE));

    private static final List<SoundEvent> SOUNDS = new ArrayList<SoundEvent>();
    public static final SoundEvent DEATH = sound("entity.sniffer.death");
    public static final SoundEvent DIGGING = sound("entity.sniffer.digging");
    public static final SoundEvent DIGGING_STOP = sound("entity.sniffer.digging_stop");
    public static final SoundEvent DROP_SEED = sound("entity.sniffer.drop_seed");
    public static final SoundEvent EAT = sound("entity.sniffer.eat");
    public static final SoundEvent HAPPY = sound("entity.sniffer.happy");
    public static final SoundEvent HURT = sound("entity.sniffer.hurt");
    public static final SoundEvent IDLE = sound("entity.sniffer.idle");
    public static final SoundEvent SCENTING = sound("entity.sniffer.scenting");
    public static final SoundEvent SEARCHING = sound("entity.sniffer.searching");
    public static final SoundEvent SNIFFING = sound("entity.sniffer.sniffing");
    public static final SoundEvent STEP = sound("entity.sniffer.step");
    public static final SoundEvent EGG_CRACK = sound("block.sniffer_egg.crack");
    public static final SoundEvent EGG_HATCH = sound("block.sniffer_egg.hatch");
    public static final SoundEvent EGG_PLOP = sound("block.sniffer_egg.plop");

    private static SnifferSide side = new SnifferSide();

    /** Null when the stream is off. */
    public static BlockTorchflower torchflower;
    public static BlockPotted pottedTorchflower;
    public static BlockTorchflowerCrop torchflowerCrop;
    public static BlockPitcherCrop pitcherCrop;
    public static BlockPitcherPlant pitcherPlant;
    public static BlockSnifferEgg snifferEgg;
    public static Item torchflowerSeeds;
    public static Item pitcherPod;
    public static Item torchflowerItem;
    public static Item pitcherPlantItem;
    public static Item eggItem;

    private SnifferContent() {
    }

    public static boolean enabled() {
        return ModConfig.sniffer.enabled;
    }

    private static SoundEvent sound(String name) {
        SoundEvent e = new SoundEvent(Content.id(name)).setRegistryName(Content.id(name));
        SOUNDS.add(e);
        return e;
    }

    static SnifferSide proxy() {
        return side;
    }

    /** Called by the client's setup to put its side in place. */
    public static void setSide(SnifferSide clientSide) {
        side = clientSide;
    }

    /** Pre-init: tell Backport Core which blocks this mod ships first, and register the digging loot. */
    public static void preInit() {
        if (enabled()) {
            BlockOwnership.declare(TrailsAndTalesBackport.MOD_ID, "torchflower", "potted_torchflower", "torchflower_crop",
                    "pitcher_crop", "pitcher_plant", "sniffer_egg");
            LootTableList.register(EntitySniffer.DIGGING_LOOT);
        }
    }

    /** Init: the family hooks that need every block and item registered. */
    public static void init() {
        if (!enabled()) {
            return;
        }
        addToSeedList(EntityChicken.class, "field_184761_bD");
        addToSeedList(EntityParrot.class, "field_192016_bJ");
        beeFlowers();
    }

    // ------------------------------------------------------------------ registry events

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (!enabled()) {
            return;
        }
        torchflower = Content.block(event.getRegistry(), "torchflower", new BlockTorchflower(), null);
        pottedTorchflower = Content.block(event.getRegistry(), "potted_torchflower", new BlockPotted(), null);
        pottedTorchflower.setCreativeTab(null);
        torchflowerCrop = Content.block(event.getRegistry(), "torchflower_crop", new BlockTorchflowerCrop(), null);
        pitcherCrop = Content.block(event.getRegistry(), "pitcher_crop", new BlockPitcherCrop(), null);
        pitcherPlant = Content.block(event.getRegistry(), "pitcher_plant", new BlockPitcherPlant(), null);
        snifferEgg = Content.block(event.getRegistry(), "sniffer_egg", new BlockSnifferEgg(), null);
        // 1.20: the pitcher plant burns (ignitedByLava, flammable as tall flowers are).
        Blocks.FIRE.setFireInfo(torchflower, 60, 100);
        Blocks.FIRE.setFireInfo(pitcherPlant, 60, 100);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        torchflowerItem = Content.registerBlockItem(event.getRegistry(), torchflower);
        pitcherPlantItem = Content.registerBlockItem(event.getRegistry(), pitcherPlant);
        eggItem = Content.blockItem(event.getRegistry(), snifferEgg, new ItemBlock(snifferEgg) {
            @Override
            public EnumRarity getRarity(ItemStack stack) {
                return EnumRarity.UNCOMMON;
            }
        });
        torchflowerSeeds = Content.item(event.getRegistry(), "torchflower_seeds", new ItemSeeds(torchflowerCrop, Blocks.FARMLAND));
        pitcherPod = Content.item(event.getRegistry(), "pitcher_pod", new ItemSeeds(pitcherCrop, Blocks.FARMLAND));
        torchflowerCrop.link(torchflowerSeeds, torchflower);
        pitcherCrop.link(pitcherPod, pitcherPlantItem);
        pitcherPlant.link(pitcherPlantItem);
        pottedTorchflower.setPlant(torchflowerItem);

        // 1.20's composter chances.
        Compat.compost(0.3F, torchflowerSeeds);
        Compat.compost(0.3F, pitcherPod);
        Compat.compost(0.85F, torchflowerItem);
        Compat.compost(0.85F, pitcherPlantItem);
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        if (!enabled()) {
            return;
        }
        event.getRegistry().register(EntityEntryBuilder.create().entity(EntitySniffer.class)
                .id(Content.id("sniffer"), SNIFFER_ID).name(TrailsAndTalesBackport.MOD_ID + ".sniffer")
                .tracker(80, 3, true).egg(8855049, 2468720).build());
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (!enabled()) {
            return;
        }
        ResourceLocation orange = Content.id("orange_dye_from_torchflower");
        event.getRegistry().register(new ShapelessOreRecipe(orange,
                new ItemStack(Items.DYE, 1, EnumDyeColor.ORANGE.getDyeDamage()), torchflowerItem).setRegistryName(orange));
        ResourceLocation cyan = Content.id("cyan_dye_from_pitcher_plant");
        event.getRegistry().register(new ShapelessOreRecipe(cyan,
                new ItemStack(Items.DYE, 2, EnumDyeColor.CYAN.getDyeDamage()), pitcherPlantItem).setRegistryName(cyan));
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        if (!enabled()) {
            return;
        }
        for (SoundEvent e : SOUNDS) {
            event.getRegistry().register(e);
        }
    }

    // ------------------------------------------------------------------ lookups

    /** 1.20's {@code sniffer_diggable_block}: dirt, coarse dirt, podzol, grass, and the family's blocks of the tag. */
    public static boolean isDiggable(IBlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.DIRT || block == Blocks.GRASS) {
            return true;
        }
        ResourceLocation id = block.getRegistryName();
        return id != null && FAMILY.contains(id.getNamespace()) && FAMILY_DIGGABLE.contains(id.getPath());
    }

    /** 1.20's {@code sniffer_egg_hatch_boost}: the moss block (Caves & Cliffs'). */
    public static boolean isHatchBoost(IBlockState state) {
        ResourceLocation id = state.getBlock().getRegistryName();
        return id != null && FAMILY.contains(id.getNamespace()) && HATCH_BOOST.equals(id.getPath());
    }

    // ------------------------------------------------------------------ family hooks

    @SuppressWarnings("unchecked")
    private static void addToSeedList(Class<?> mob, String srgField) {
        try {
            Set<Item> items = (Set<Item>) ObfuscationReflectionHelper.getPrivateValue((Class<Object>) mob, null, srgField);
            items.add(torchflowerSeeds);
            items.add(pitcherPod);
        } catch (RuntimeException e) {
            TrailsAndTalesBackport.LOGGER.warn("Could not add torchflower seeds and pitcher pods to {}'s food.", mob.getSimpleName(), e);
        }
    }

    /** Buzzy Bees' {@code BeeFlowers}: torchflowers and pitcher plants are flowers, as 1.20's flower tags list them. */
    private static void beeFlowers() {
        if (!Owners.present(Owners.BUZZY_BEES)) {
            return;
        }
        try {
            Class<?> api = Class.forName("com.exiledradio.buzzybeesbackport.api.BeeFlowers");
            Predicate<IBlockState> flower = state -> state.getBlock() == torchflower || state.getBlock() == pitcherPlant;
            Predicate<ItemStack> item = stack -> !stack.isEmpty()
                    && (stack.getItem() == torchflowerItem || stack.getItem() == pitcherPlantItem);
            api.getMethod("addFlower", Predicate.class).invoke(null, flower);
            api.getMethod("addFlowerItem", Predicate.class).invoke(null, item);
        } catch (ReflectiveOperationException | RuntimeException e) {
            TrailsAndTalesBackport.LOGGER.warn("Could not make torchflowers and pitcher plants flowers to bees.", e);
        }
    }

    /** Little Sniffs: a player fed a snifflet. */
    static void grantLittleSniffs(net.minecraft.entity.player.EntityPlayerMP player) {
        SnifferAdvancements.grantFeedSnifflet(player);
    }
}
