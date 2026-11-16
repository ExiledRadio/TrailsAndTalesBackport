package com.exiledradio.trailsandtalesbackport.selftest;

import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.common.Content;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;

/**
 * Test builds only: stand-ins under the names of this mod's planned amethyst copy ({@code amethyst_shard},
 * {@code amethyst_block}, PLAN.md "Copies this adds"), so the calibrated sensor's recipe and resonance can be
 * tested without Caves &amp; Cliffs Backport. Registers nothing with Caves &amp; Cliffs installed or once the real copy exists (its content class is
 * looked for by name; update the name here when the copy lands).
 */
@Mod.EventBusSubscriber(modid = TrailsAndTalesBackport.MOD_ID)
public final class TestAmethyst {

    static Block block;
    static Item shard;

    private TestAmethyst() {
    }

    private static boolean copyExists() {
        if (net.minecraftforge.fml.common.Loader.isModLoaded("cavesandcliffsbackport")) {
            return true; // Caves & Cliffs Backport's own amethyst is used
        }
        try {
            Class.forName("com.exiledradio.trailsandtalesbackport.amethyst.AmethystContent", false, TestAmethyst.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (copyExists()) {
            return;
        }
        // Classes of the self-test package, so the state and item dumps leave them out of the asset check.
        block = new Block(Material.GLASS) {
        };
        block.setRegistryName(Content.id("amethyst_block"));
        event.getRegistry().register(block);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (copyExists()) {
            return;
        }
        shard = new Item() {
        };
        shard.setRegistryName(Content.id("amethyst_shard"));
        event.getRegistry().register(shard);
        OreDictionary.registerOre("gemAmethyst", shard);
    }

    static Block amethystBlock() {
        if (block != null) {
            return block;
        }
        Block cc = ForgeRegistries.BLOCKS.getValue(new net.minecraft.util.ResourceLocation("cavesandcliffsbackport", "amethyst_block"));
        return cc != null && cc != net.minecraft.init.Blocks.AIR ? cc : ForgeRegistries.BLOCKS.getValue(Content.id("amethyst_block"));
    }

    static Item amethystShard() {
        if (shard != null) {
            return shard;
        }
        Item cc = ForgeRegistries.ITEMS.getValue(new net.minecraft.util.ResourceLocation("cavesandcliffsbackport", "amethyst_shard"));
        return cc != null ? cc : ForgeRegistries.ITEMS.getValue(Content.id("amethyst_shard"));
    }
}
