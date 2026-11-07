package com.exiledradio.trailsandtalesbackport.woodcopy;

import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.common.Content;
import com.exiledradio.trailsandtalesbackport.common.Owners;
import com.exiledradio.trailsandtalesbackport.common.wood.AxeStripping;

import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockNewLog;
import net.minecraft.block.BlockOldLog;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

/**
 * Our copies of Village & Pillage's stripped logs and stripped wood (family Rule 1): the 1.20 hanging sign
 * recipes take stripped logs, so this mod must have them even alone. Registered only when neither Village &
 * Pillage nor Update Aquatic (which copies them too) is installed, and the hanging sign stream is on.
 *
 * <p>The blocks are the owner's classes ({@link BlockStrippedLog}, {@link Wood}), copied by
 * {@code tools/woodcopy/copy_from_owner.py}. As the owner does: an axe strips vanilla's logs and bark into them
 * (keeping the axis), they make the wood's planks, 4 stripped logs make 3 stripped wood, they smelt to charcoal,
 * burn and are {@code logWood}.
 */
@Mod.EventBusSubscriber(modid = TrailsAndTalesBackport.MOD_ID)
public final class StrippedLogCopies {

    public static final Map<Wood, BlockStrippedLog> LOGS = new EnumMap<Wood, BlockStrippedLog>(Wood.class);
    public static final Map<Wood, BlockStrippedLog> BARK = new EnumMap<Wood, BlockStrippedLog>(Wood.class);

    private StrippedLogCopies() {
    }

    public static boolean registering() {
        return ModConfig.hangingSigns.enabled && !Loader.isModLoaded(Owners.VILLAGE_AND_PILLAGE)
                && !Loader.isModLoaded(Owners.UPDATE_AQUATIC) && Owners.shouldRegisterCopy("stripped_oak_log");
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<net.minecraft.block.Block> event) {
        if (!registering()) {
            return;
        }
        for (Wood w : Wood.values()) {
            BlockStrippedLog log = Content.block(event.getRegistry(), "stripped_" + w.name + "_log", new BlockStrippedLog(w, false), "axe");
            BlockStrippedLog bark = Content.block(event.getRegistry(), "stripped_" + w.name + "_wood", new BlockStrippedLog(w, true), "axe");
            LOGS.put(w, log);
            BARK.put(w, bark);
            Blocks.FIRE.setFireInfo(log, 5, 5);
            Blocks.FIRE.setFireInfo(bark, 5, 5);
        }
        AxeStripping.addRule(StrippedLogCopies::stripped);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!registering()) {
            return;
        }
        for (Wood w : Wood.values()) {
            for (BlockStrippedLog b : new BlockStrippedLog[] {LOGS.get(w), BARK.get(w)}) {
                Content.blockItem(event.getRegistry(), b, new ItemBlock(b));
                OreDictionary.registerOre("logWood", b);
            }
        }
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (!registering()) {
            return;
        }
        for (Wood w : Wood.values()) {
            ItemStack planks = new ItemStack(Blocks.PLANKS, 4, w.planksMeta());
            for (BlockStrippedLog b : new BlockStrippedLog[] {LOGS.get(w), BARK.get(w)}) {
                ResourceLocation id = Content.id(w.name + "_planks_from_" + b.getRegistryName().getPath());
                event.getRegistry().register(new ShapelessOreRecipe(id, planks, b).setRegistryName(id));
                FurnaceRecipes.instance().addSmeltingRecipeForBlock(b, new ItemStack(Items.COAL, 1, 1), 0.15F);
            }
            ResourceLocation id = Content.id("stripped_" + w.name + "_wood");
            event.getRegistry().register(new ShapedOreRecipe(id, new ItemStack(BARK.get(w), 3), "##", "##", '#', LOGS.get(w))
                    .setRegistryName(id));
        }
    }

    /** The owner's rule: vanilla log or bark of a wood to its stripped form, keeping the axis. */
    @Nullable
    static IBlockState stripped(IBlockState state) {
        BlockPlanks.EnumType type;
        if (state.getBlock() == Blocks.LOG) {
            type = state.getValue(BlockOldLog.VARIANT);
        } else if (state.getBlock() == Blocks.LOG2) {
            type = state.getValue(BlockNewLog.VARIANT);
        } else {
            return null;
        }
        Wood wood = Wood.of(type);
        if (wood == null) {
            return null;
        }
        BlockLog.EnumAxis axis = state.getValue(BlockLog.LOG_AXIS);
        if (axis == BlockLog.EnumAxis.NONE) {
            return BARK.get(wood).getDefaultState().withProperty(BlockLog.LOG_AXIS, BlockLog.EnumAxis.Y);
        }
        return LOGS.get(wood).getDefaultState().withProperty(BlockLog.LOG_AXIS, axis);
    }
}
