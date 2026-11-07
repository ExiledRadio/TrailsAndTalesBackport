package com.exiledradio.trailsandtalesbackport.bamboo;

import com.exiledradio.backportcore.ownership.BlockOwnership;
import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.common.Content;
import com.exiledradio.trailsandtalesbackport.common.Owners;
import com.exiledradio.trailsandtalesbackport.common.wood.AxeStripping;
import com.exiledradio.trailsandtalesbackport.common.wood.WoodKind;
import com.exiledradio.trailsandtalesbackport.common.wood.WoodSet;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.ShapedOreRecipe;

/**
 * Stream B: 1.20's bamboo wood set. The bamboo plant itself is Village & Pillage's (1.14).
 *
 * <p>The block of bamboo is made from 9 of the installed bamboo: Village & Pillage's
 * ({@code villagerbackport:bamboo}), or this mod's own copy of it when that mod is absent and the copy is
 * built (see {@code docs/PLAN.md}, Still open 5); with neither, the bamboo wood set is still registered
 * (a world keeps its blocks) but the block of bamboo cannot be crafted.
 *
 * <p>Map colors are 1.20's: planks, mosaic and stripped bamboo {@code COLOR_YELLOW}, the block of bamboo's
 * sides {@code PLANT} (1.12.2: {@link MapColor#YELLOW}, {@link MapColor#FOLIAGE}).
 */
@Mod.EventBusSubscriber(modid = TrailsAndTalesBackport.MOD_ID)
public final class BambooWoodContent {

    public static final WoodKind BAMBOO = new WoodKind("bamboo", "bamboo_wood", MapColor.YELLOW, MapColor.FOLIAGE);

    /** Null when the stream is off. */
    public static WoodSet set;

    private BambooWoodContent() {
    }

    public static boolean enabled() {
        return ModConfig.bamboo.enabled;
    }

    /** Pre-init: the blocks this mod ships first, for Backport Core's ownership table. */
    public static void preInit() {
        if (enabled()) {
            BlockOwnership.declare(TrailsAndTalesBackport.MOD_ID, "bamboo_block", "stripped_bamboo_block",
                    "bamboo_planks", "bamboo_mosaic", "bamboo_stairs", "bamboo_mosaic_stairs", "bamboo_slab",
                    "bamboo_mosaic_slab", "bamboo_fence", "bamboo_fence_gate", "bamboo_door", "bamboo_trapdoor",
                    "bamboo_pressure_plate", "bamboo_button", "bamboo_sign", "bamboo_wall_sign");
        }
    }

    /** @return the bamboo item the block of bamboo is made from, or null when no mod adds bamboo */
    public static Item bambooItem() {
        Item theirs = Owners.theirs(Owners.VILLAGE_AND_PILLAGE, "bamboo");
        return theirs != null ? theirs : Owners.ours("bamboo");
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (!enabled()) {
            return;
        }
        set = WoodSet.bamboo(BAMBOO);
        set.registerBlocks(event.getRegistry());
        AxeStripping.add(set.stripping());
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (enabled()) {
            set.registerItems(event.getRegistry());
        }
    }

    /** Low priority, so the bamboo item of any mod registering in the item event exists by now. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (!enabled()) {
            return;
        }
        set.registerRecipes(event.getRegistry());
        Item bamboo = bambooItem();
        if (bamboo != null) {
            ResourceLocation id = Content.id("bamboo_block");
            event.getRegistry().register(new ShapedOreRecipe(id, new ItemStack(set.log), "###", "###", "###", '#', bamboo)
                    .setRegistryName(id));
        } else {
            TrailsAndTalesBackport.LOGGER.info("No mod adds bamboo: the block of bamboo has no recipe.");
        }
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        if (enabled()) {
            for (SoundEvent e : BAMBOO.soundEvents()) {
                event.getRegistry().register(e);
            }
        }
    }
}
