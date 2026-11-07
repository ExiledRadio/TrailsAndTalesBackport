package com.exiledradio.trailsandtalesbackport.hangingsign;

import com.exiledradio.backportcore.ownership.BlockOwnership;
import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.common.Content;
import com.exiledradio.trailsandtalesbackport.common.Owners;
import com.exiledradio.trailsandtalesbackport.common.wood.WoodKind;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.ShapedOreRecipe;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stream H: hanging signs for every wood in the game, and the chain they hang from when no mod adds one.
 *
 * <p>Recipe (1.20): 2 chains over 6 stripped logs make 6 hanging signs, each wood its own; see
 * {@link HangingWood#log()} for which stripped log each wood takes.
 */
@Mod.EventBusSubscriber(modid = TrailsAndTalesBackport.MOD_ID)
public final class HangingSignContent {

    private static final List<SoundEvent> SOUNDS = new ArrayList<SoundEvent>();
    /** One sound type per 1.20 sound set, made eagerly (blocks take it in their constructors). */
    private static final Map<String, SoundType> SOUND_TYPES = new LinkedHashMap<String, SoundType>();
    static final SoundType CHAIN_SOUND = WoodKind.soundType(SOUNDS, "block.chain", 1.0F, 1.0F);

    static {
        for (HangingWood w : HangingWood.values()) {
            if (!SOUND_TYPES.containsKey(w.soundSet)) {
                SOUND_TYPES.put(w.soundSet, WoodKind.soundType(SOUNDS, "block." + w.soundSet, 1.0F, 1.0F));
            }
        }
    }

    public static final Map<HangingWood, BlockHangingSign.Ceiling> CEILING = new EnumMap<HangingWood, BlockHangingSign.Ceiling>(HangingWood.class);
    public static final Map<HangingWood, BlockHangingSign.Wall> WALL = new EnumMap<HangingWood, BlockHangingSign.Wall>(HangingWood.class);
    public static final Map<HangingWood, Item> ITEMS = new EnumMap<HangingWood, Item>(HangingWood.class);

    /** Our chain, or null when another mod provides chains (or the stream is off). */
    public static BlockChain chain;

    private HangingSignContent() {
    }

    public static boolean enabled() {
        return ModConfig.hangingSigns.enabled;
    }

    public static void preInit() {
        if (!enabled()) {
            return;
        }
        List<String> names = new ArrayList<String>();
        for (HangingWood w : HangingWood.values()) {
            names.add(w.name + "_hanging_sign");
            names.add(w.name + "_wall_hanging_sign");
        }
        BlockOwnership.declare(TrailsAndTalesBackport.MOD_ID, names.toArray(new String[0]));
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (!enabled()) {
            return;
        }
        for (HangingWood w : HangingWood.values()) {
            if (!w.present()) {
                continue;
            }
            SoundType sound = SOUND_TYPES.get(w.soundSet);
            BlockHangingSign.Ceiling c = Content.block(event.getRegistry(), w.name + "_hanging_sign", new BlockHangingSign.Ceiling(w, sound), "axe");
            BlockHangingSign.Wall wall = Content.block(event.getRegistry(), w.name + "_wall_hanging_sign", new BlockHangingSign.Wall(w, sound), "axe");
            c.setCreativeTab(null);
            wall.setCreativeTab(null);
            CEILING.put(w, c);
            WALL.put(w, wall);
        }
        GameRegistry.registerTileEntity(TileHangingSign.class, Content.id("hanging_sign"));
        if (Owners.shouldRegisterCopy("chain")) {
            chain = Content.block(event.getRegistry(), "chain", new BlockChain(CHAIN_SOUND), "pickaxe");
        }
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        for (Map.Entry<HangingWood, BlockHangingSign.Ceiling> e : CEILING.entrySet()) {
            ITEMS.put(e.getKey(), Content.item(event.getRegistry(), e.getKey().name + "_hanging_sign",
                    new ItemHangingSign(e.getValue(), WALL.get(e.getKey()))));
        }
        if (chain != null) {
            Content.registerBlockItem(event.getRegistry(), chain);
        }
    }

    /** @return the chain item the recipe takes: the owner's, or ours */
    public static Item chainItem() {
        Item theirs = Owners.theirs(Owners.NETHER_UPDATE, "chain");
        if (theirs != null) {
            return theirs;
        }
        return chain == null ? null : Item.getItemFromBlock(chain);
    }

    /** Low priority: other mods' stripped logs and chains are registered by then. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (!enabled()) {
            return;
        }
        if (chain != null) {
            ResourceLocation id = Content.id("chain");
            event.getRegistry().register(new ShapedOreRecipe(id, new ItemStack(chain), "N", "I", "N",
                    'N', "nuggetIron", 'I', "ingotIron").setRegistryName(id));
        }
        Item chains = chainItem();
        if (chains == null) {
            TrailsAndTalesBackport.LOGGER.warn("No chain in this game: hanging signs have no recipe.");
            return;
        }
        for (Map.Entry<HangingWood, Item> e : ITEMS.entrySet()) {
            ItemStack log = e.getKey().log();
            if (log == null) {
                TrailsAndTalesBackport.LOGGER.warn("No stripped {} log: its hanging sign has no recipe.", e.getKey().name);
                continue;
            }
            ResourceLocation id = Content.id(e.getKey().name + "_hanging_sign");
            event.getRegistry().register(new ShapedOreRecipe(id, new ItemStack(e.getValue(), 6), "X X", "###", "###",
                    'X', chains, '#', log).setRegistryName(id));
        }
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        if (enabled()) {
            for (SoundEvent e : SOUNDS) {
                event.getRegistry().register(e);
            }
        }
    }
}
