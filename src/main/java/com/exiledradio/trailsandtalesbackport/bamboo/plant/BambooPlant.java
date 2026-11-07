package com.exiledradio.trailsandtalesbackport.bamboo.plant;

import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.common.Content;
import com.exiledradio.trailsandtalesbackport.common.Owners;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Our copy of Village & Pillage's bamboo plant (family Rule 1), registered only when that mod is absent and
 * no other installed mod provides bamboo: the stalk ({@code bamboo}), the shoot ({@code bamboo_sapling}), the
 * bamboo item, and bamboo scattered through jungles. The block classes are the owner's, copied by
 * {@code tools/bamboo/copy_from_owner.py}; this class holds what the owner's {@code BambooContent} held for
 * them, under the same field names.
 */
@Mod.EventBusSubscriber(modid = TrailsAndTalesBackport.MOD_ID)
public final class BambooPlant {

    public static BlockBamboo bamboo;
    public static BlockBambooSapling bambooSapling;
    public static Item bambooItem;
    public static SoundType bambooSound;
    public static SoundType bambooSaplingSound;

    private BambooPlant() {
    }

    /** @return true if this game gets our copy: the bamboo stream is on and nobody else adds bamboo */
    public static boolean registering() {
        return ModConfig.bamboo.enabled && !Loader.isModLoaded(Owners.VILLAGE_AND_PILLAGE)
                && Owners.shouldRegisterCopy("bamboo");
    }

    /** The owner leaves jungles alone when Future MC puts its own bamboo there; so does the copy. */
    static boolean futureMcBamboo() {
        return Loader.isModLoaded("futuremc");
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (!registering()) {
            return;
        }
        bamboo = Content.block(event.getRegistry(), "bamboo", new BlockBamboo(), null);
        bambooSapling = Content.block(event.getRegistry(), "bamboo_sapling", new BlockBambooSapling(), null);
        bambooSapling.setCreativeTab(null);
        Content.multipart(bamboo);
        MinecraftForge.EVENT_BUS.register(JungleBamboo.class);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!registering()) {
            return;
        }
        // The owner's: the stalk's own item, which plants a shoot.
        bambooItem = Content.blockItem(event.getRegistry(), bamboo, new ItemBlock(bamboo));
        Blocks.FIRE.setFireInfo(bamboo, 60, 60);
        Blocks.FIRE.setFireInfo(bambooSapling, 60, 60);
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        if (!registering()) {
            return;
        }
        SoundEvent bambooBreak = sound(event, "block.bamboo.break");
        SoundEvent bambooFall = sound(event, "block.bamboo.fall");
        SoundEvent bambooHit = sound(event, "block.bamboo.hit");
        SoundEvent bambooPlace = sound(event, "block.bamboo.place");
        SoundEvent bambooStep = sound(event, "block.bamboo.step");
        SoundEvent saplingBreak = sound(event, "block.bamboo_sapling.break");
        SoundEvent saplingHit = sound(event, "block.bamboo_sapling.hit");
        SoundEvent saplingPlace = sound(event, "block.bamboo_sapling.place");
        // The owner's sound types, as 1.14 has them.
        bambooSound = new SoundType(1.0F, 1.0F, bambooBreak, bambooStep, bambooPlace, bambooHit, bambooFall);
        bambooSaplingSound = new SoundType(1.0F, 1.0F, saplingBreak, bambooStep, saplingPlace, saplingHit, bambooFall);
    }

    private static SoundEvent sound(RegistryEvent.Register<SoundEvent> event, String name) {
        SoundEvent sound = new SoundEvent(Content.id(name));
        sound.setRegistryName(Content.id(name));
        event.getRegistry().register(sound);
        return sound;
    }
}
