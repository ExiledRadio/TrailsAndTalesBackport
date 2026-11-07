package com.exiledradio.trailsandtalesbackport.cherry;

import com.exiledradio.backportcore.ownership.BlockOwnership;
import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.common.BlockPotted;
import com.exiledradio.trailsandtalesbackport.common.Compat;
import com.exiledradio.trailsandtalesbackport.common.Content;
import com.exiledradio.trailsandtalesbackport.common.wood.AxeStripping;
import com.exiledradio.trailsandtalesbackport.common.wood.WoodKind;
import com.exiledradio.trailsandtalesbackport.common.wood.WoodSet;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.List;

/**
 * Stream C: the cherry wood set, cherry leaves and saplings, and pink petals.
 *
 * <p>Map colors are 1.20's: cherry planks and stripped wood {@code TERRACOTTA_WHITE}, bark
 * {@code TERRACOTTA_GRAY}, leaves {@code COLOR_PINK}. 1.12.2 names those {@link MapColor#WHITE_STAINED_HARDENED_CLAY},
 * {@link MapColor#GRAY_STAINED_HARDENED_CLAY} and {@link MapColor#PINK}.
 */
@Mod.EventBusSubscriber(modid = TrailsAndTalesBackport.MOD_ID)
public final class CherryContent {

    public static final WoodKind CHERRY = new WoodKind("cherry", "cherry_wood",
            MapColor.WHITE_STAINED_HARDENED_CLAY, MapColor.GRAY_STAINED_HARDENED_CLAY);

    private static final List<SoundEvent> SOUNDS = new ArrayList<SoundEvent>();
    static final SoundType LEAVES_SOUND = WoodKind.soundType(SOUNDS, "block.cherry_leaves", 1.0F, 1.0F);
    static final SoundType SAPLING_SOUND = WoodKind.soundType(SOUNDS, "block.cherry_sapling", 1.0F, 1.0F);
    static final SoundType PETALS_SOUND = WoodKind.soundType(SOUNDS, "block.pink_petals", 1.0F, 1.0F);

    /** Null when the stream is off. */
    public static WoodSet set;
    public static BlockCherryLeaves leaves;
    public static BlockCherrySapling sapling;
    public static BlockPotted pottedSapling;
    public static BlockPinkPetals petals;

    private CherryContent() {
    }

    public static boolean enabled() {
        return ModConfig.cherry.enabled;
    }

    /** Pre-init: tell Backport Core which blocks this mod ships first. */
    public static void preInit() {
        if (enabled()) {
            BlockOwnership.declare(TrailsAndTalesBackport.MOD_ID, "cherry_log", "cherry_wood", "stripped_cherry_log",
                    "stripped_cherry_wood", "cherry_planks", "cherry_stairs", "cherry_slab", "cherry_fence",
                    "cherry_fence_gate", "cherry_door", "cherry_trapdoor", "cherry_pressure_plate", "cherry_button",
                    "cherry_sign", "cherry_wall_sign", "cherry_leaves", "cherry_sapling", "potted_cherry_sapling",
                    "pink_petals");
        }
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (!enabled()) {
            return;
        }
        set = WoodSet.tree(CHERRY);
        set.registerBlocks(event.getRegistry());
        leaves = Content.block(event.getRegistry(), "cherry_leaves", new BlockCherryLeaves(LEAVES_SOUND), "hoe");
        sapling = Content.block(event.getRegistry(), "cherry_sapling", new BlockCherrySapling(SAPLING_SOUND), null);
        pottedSapling = Content.block(event.getRegistry(), "potted_cherry_sapling", new BlockPotted(), null);
        pottedSapling.setCreativeTab(null);
        petals = Content.block(event.getRegistry(), "pink_petals", new BlockPinkPetals(PETALS_SOUND), null);
        Content.multipart(petals);
        Blocks.FIRE.setFireInfo(leaves, 30, 60);
        Blocks.FIRE.setFireInfo(petals, 60, 100);
        AxeStripping.add(set.stripping());
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        set.registerItems(event.getRegistry());
        Item leavesItem = Content.registerBlockItem(event.getRegistry(), leaves);
        Item saplingItem = Content.blockItem(event.getRegistry(), sapling, new ItemBlock(sapling) {
            @Override
            public int getItemBurnTime(ItemStack stack) {
                return 100;
            }
        });
        Item petalsItem = Content.blockItem(event.getRegistry(), petals, new ItemPinkPetals(petals));
        leaves.setSapling(saplingItem);
        petals.setItem(petalsItem);
        pottedSapling.setPlant(saplingItem);

        OreDictionary.registerOre("treeLeaves", leaves);
        OreDictionary.registerOre("treeSapling", sapling);

        Compat.compost(0.3F, leavesItem);
        Compat.compost(0.3F, saplingItem);
        Compat.compost(0.3F, petalsItem);
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (!enabled()) {
            return;
        }
        set.registerRecipes(event.getRegistry());
        // 1.20: one pink petal makes one pink dye.
        net.minecraft.util.ResourceLocation id = Content.id("pink_dye_from_pink_petals");
        event.getRegistry().register(new net.minecraftforge.oredict.ShapelessOreRecipe(id,
                new ItemStack(net.minecraft.init.Items.DYE, 1, net.minecraft.item.EnumDyeColor.PINK.getDyeDamage()),
                petals).setRegistryName(id));
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        if (!enabled()) {
            return;
        }
        for (SoundEvent e : CHERRY.soundEvents()) {
            event.getRegistry().register(e);
        }
        for (SoundEvent e : SOUNDS) {
            event.getRegistry().register(e);
        }
    }
}
