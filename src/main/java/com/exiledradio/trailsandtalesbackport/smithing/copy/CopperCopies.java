package com.exiledradio.trailsandtalesbackport.smithing.copy;

import com.exiledradio.backportcore.ownership.ItemOwnership;
import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.common.Content;
import com.exiledradio.trailsandtalesbackport.common.Owners;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;

/**
 * Our copy of Caves &amp; Cliffs' copper (Rule 1), registered only without it and without Tricky Trials (which copies
 * the whole copper set): the copper ingot for the brush and the copper trim, with what makes it obtainable - copper
 * ore (2-5 raw copper, Fortune multiplying), its veins in the Overworld's stone (6 a chunk, up to 10 blocks, y 1-96),
 * raw copper, and smelting either into an ingot (0.7 experience). The ore and veins are Tricky Trials' copy of the
 * owner's classes ({@code tools/smithing/copy_from_owner.py}). The copper blocks are not copied: nothing of ours is
 * made from them.
 *
 * <p>Registered when the smithing or archaeology stream is on. Ore names {@code oreCopper}, {@code ingotCopper},
 * {@code rawCopper}, so any mod's copper works in our recipes and ours in theirs.
 */
@Mod.EventBusSubscriber(modid = TrailsAndTalesBackport.MOD_ID)
public final class CopperCopies {

    static final String TRICKY_TRIALS = "trickytrialsbackport";

    public static Block ore;
    public static Item ingot;
    public static Item raw;

    private CopperCopies() {
    }

    public static boolean registering() {
        return (ModConfig.smithing.enabled || ModConfig.archaeology.enabled)
                && !Loader.isModLoaded(Owners.CAVES_AND_CLIFFS) && !Loader.isModLoaded(TRICKY_TRIALS)
                && Owners.shouldRegisterCopy("copper_ore")
                && ItemOwnership.shouldRegisterItem(TrailsAndTalesBackport.MOD_ID, "copper_ingot");
    }

    public static void preInit() {
    }

    public static void init() {
        if (ore != null) {
            GameRegistry.registerWorldGenerator(new CopperOreGenerator(), 0);
        }
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (registering()) {
            ore = Content.block(event.getRegistry(), "copper_ore", new BlockCopperOre(() -> raw), "pickaxe");
            // Content.block sets level 0; the owner's ore needs a stone pickaxe.
            ore.setHarvestLevel("pickaxe", 1);
            TrailsAndTalesBackport.LOGGER.info("Smithing: our copy of Caves & Cliffs' copper ore, raw copper and ingot.");
        }
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (ore == null) {
            return;
        }
        Content.registerBlockItem(event.getRegistry(), ore);
        ingot = Content.item(event.getRegistry(), "copper_ingot", new Item());
        raw = Content.item(event.getRegistry(), "raw_copper", new Item());
        OreDictionary.registerOre("oreCopper", ore);
        OreDictionary.registerOre("ingotCopper", ingot);
        OreDictionary.registerOre("rawCopper", raw);
        FurnaceRecipes f = FurnaceRecipes.instance();
        f.addSmeltingRecipeForBlock(ore, new ItemStack(ingot), 0.7F);
        f.addSmelting(raw, new ItemStack(ingot), 0.7F);
    }
}
