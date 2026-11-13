package com.exiledradio.trailsandtalesbackport.selftest;

import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.common.Content;

import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Test builds only: a stand-in {@code trailsandtalesbackport:brush} (64 durability) when the archaeology
 * stream's brush is not in this build, so the armadillo test can brush. With the archaeology stream merged,
 * its own brush is used and this registers nothing.
 */
@Mod.EventBusSubscriber(modid = TrailsAndTalesBackport.MOD_ID)
public final class TestBrush {

    private TestBrush() {
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        try {
            Class.forName("com.exiledradio.trailsandtalesbackport.archaeology.ItemBrush", false, TestBrush.class.getClassLoader());
            return;
        } catch (ClassNotFoundException e) {
            // No archaeology stream in this build: register the stand-in.
        }
        // A class of the self-test package, so the item dump leaves it out of the asset check.
        Item brush = new Item() {
        }.setMaxStackSize(1).setMaxDamage(64);
        brush.setRegistryName(Content.id("brush"));
        brush.setTranslationKey(TrailsAndTalesBackport.MOD_ID + ".brush");
        event.getRegistry().register(brush);
    }
}
