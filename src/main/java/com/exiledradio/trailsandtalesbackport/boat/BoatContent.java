package com.exiledradio.trailsandtalesbackport.boat;

import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.common.Content;

import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Stream Bo: cherry boats, bamboo rafts, and their chest forms.
 *
 * <p>Two entities (ids 1 and 2 of this mod's block 1-4): {@code boat} and {@code chest_boat}, the wood in
 * their data. Recipes are 1.20's: five planks in a U make a boat (bamboo planks a raft); a boat and a chest,
 * shapeless, make the chest form.
 */
@Mod.EventBusSubscriber(modid = TrailsAndTalesBackport.MOD_ID)
public final class BoatContent {

    public static final int BOAT_ID = 1;
    public static final int CHEST_BOAT_ID = 2;

    private static final Map<String, Item> ITEMS = new HashMap<String, Item>();

    private BoatContent() {
    }

    public static boolean enabled() {
        return ModConfig.boats.enabled;
    }

    /** @return the item of a wood's boat, or null if that boat is not in this game */
    @Nullable
    public static Item item(BoatWood wood, boolean chest) {
        return ITEMS.get(wood.itemName(chest));
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        if (!enabled()) {
            return;
        }
        event.getRegistry().register(EntityEntryBuilder.create().entity(EntityModBoat.class).id(Content.id("boat"), BOAT_ID)
                .name(TrailsAndTalesBackport.MOD_ID + ".boat").tracker(80, 3, true).build());
        event.getRegistry().register(EntityEntryBuilder.create().entity(EntityModChestBoat.class).id(Content.id("chest_boat"), CHEST_BOAT_ID)
                .name(TrailsAndTalesBackport.MOD_ID + ".chest_boat").tracker(80, 3, true).build());
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        for (BoatWood w : BoatWood.values()) {
            if (!w.present()) {
                continue;
            }
            for (boolean chest : new boolean[] {false, true}) {
                String name = w.itemName(chest);
                ITEMS.put(name, Content.item(event.getRegistry(), name, new ItemModBoat(w, chest)));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (!enabled()) {
            return;
        }
        for (BoatWood w : BoatWood.values()) {
            Item boat = item(w, false);
            Item chestBoat = item(w, true);
            ItemStack planks = w.planks(1);
            if (boat == null || planks == null) {
                continue;
            }
            ResourceLocation id = Content.id(w.itemName(false));
            event.getRegistry().register(new ShapedOreRecipe(id, new ItemStack(boat), "# #", "###", '#', planks).setRegistryName(id));
            ResourceLocation cid = Content.id(w.itemName(true));
            event.getRegistry().register(new ShapelessOreRecipe(cid, new ItemStack(chestBoat), Blocks.CHEST, boat).setRegistryName(cid));
        }
    }
}
