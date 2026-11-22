package com.exiledradio.trailsandtalesbackport.boat;

import com.exiledradio.backportcore.boat.Boats;
import com.exiledradio.backportcore.boat.ItemWoodBoat;
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
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Stream Bo: cherry boats, bamboo rafts, and their chest forms.
 *
 * <p>The boats are Backport Core's ({@code backportcore:boat} and {@code chest_boat}, Core's {@code boat/}): this
 * registers our two woods with Core, the items under our names, and the recipes. Worlds saved before (our own
 * {@code trailsandtalesbackport:boat} and {@code chest_boat}, ids 1 and 2) load as Core's, wood and chest kept
 * ({@link #preInit}). Recipes are 1.20's: five planks in a U make a boat (bamboo planks a raft); a boat and a chest,
 * shapeless, make the chest form.
 */
@Mod.EventBusSubscriber(modid = TrailsAndTalesBackport.MOD_ID)
public final class BoatContent {

    private static final Map<String, Item> ITEMS = new HashMap<String, Item>();

    private BoatContent() {
    }

    public static boolean enabled() {
        return ModConfig.boats.enabled;
    }

    /** Our boats saved before Core 1.4.4 load as Core's, with or without this stream on. */
    public static void preInit() {
        Boats.renamed(Content.id("boat"), false);
        Boats.renamed(Content.id("chest_boat"), true);
    }

    /** @return the item of a wood's boat, or null if that boat is not in this game */
    @Nullable
    public static Item item(BoatWood wood, boolean chest) {
        return ITEMS.get(wood.itemName(chest));
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        for (final BoatWood w : BoatWood.values()) {
            if (!w.present()) {
                continue;
            }
            com.exiledradio.backportcore.boat.BoatWood wood = com.exiledradio.backportcore.boat.BoatWood.register(Content.id(w.name),
                    () -> w.planks(1), w.raft)
                    .setTextures(Content.id("textures/entity/boat/" + w.name + ".png"), Content.id("textures/entity/chest_boat/" + w.name + ".png"));
            for (boolean chest : new boolean[] {false, true}) {
                String name = w.itemName(chest);
                Item item = Content.item(event.getRegistry(), name, new ItemWoodBoat(wood, chest));
                ITEMS.put(name, item);
                if (chest) {
                    wood.setChestBoatItem(item);
                } else {
                    wood.setBoatItem(item);
                }
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
