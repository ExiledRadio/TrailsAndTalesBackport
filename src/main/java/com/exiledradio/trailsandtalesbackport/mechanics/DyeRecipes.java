package com.exiledradio.trailsandtalesbackport.mechanics;

import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.OreIngredient;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 1.20's re-dyeing (23w12a): {@code dye_<color>_wool}, {@code dye_<color>_bed} (one dye and one of any other color)
 * and {@code dye_<color>_carpet} (one dye in the middle of eight of any other colors, mixed), in place of 1.12.2's
 * white-only recipes, which stay (they give the same). The dye is the color's ore name, as 1.12.2's other dye recipes
 * take any mod's dye.
 */
@Mod.EventBusSubscriber(modid = TrailsAndTalesBackport.MOD_ID)
public final class DyeRecipes {

    /** Forge's dye ore names, in {@code EnumDyeColor.getDyeDamage} order. */
    private static final String[] DYES = {"dyeBlack", "dyeRed", "dyeGreen", "dyeBrown", "dyeBlue", "dyePurple", "dyeCyan",
            "dyeLightGray", "dyeGray", "dyePink", "dyeLime", "dyeYellow", "dyeLightBlue", "dyeMagenta", "dyeOrange", "dyeWhite"};

    private DyeRecipes() {
    }

    @SubscribeEvent
    public static void register(RegistryEvent.Register<IRecipe> event) {
        if (!ModConfig.mechanics.redyeing) {
            return;
        }
        Item wool = Item.getItemFromBlock(Blocks.WOOL);
        Item carpet = Item.getItemFromBlock(Blocks.CARPET);
        for (EnumDyeColor color : EnumDyeColor.values()) {
            String name = color.getName().toLowerCase(Locale.ROOT);
            String dye = DYES[color.getDyeDamage()];
            event.getRegistry().register(shapeless("dye_" + name + "_wool", "wool", new ItemStack(wool, 1, color.getMetadata()),
                    dye, others(wool, color)));
            event.getRegistry().register(shapeless("dye_" + name + "_bed", "bed", new ItemStack(Items.BED, 1, color.getMetadata()),
                    dye, others(Items.BED, color)));
            ShapedOreRecipe carpets = new ShapedOreRecipe(new ResourceLocation("carpet"), new ItemStack(carpet, 8, color.getMetadata()),
                    "###", "#$#", "###", '#', others(carpet, color), '$', new OreIngredient(dye));
            carpets.setRegistryName(new ResourceLocation(TrailsAndTalesBackport.MOD_ID, "dye_" + name + "_carpet"));
            event.getRegistry().register(carpets);
        }
    }

    private static IRecipe shapeless(String id, String group, ItemStack out, String dye, Ingredient others) {
        NonNullList<Ingredient> in = NonNullList.create();
        in.add(new OreIngredient(dye));
        in.add(others);
        ShapelessOreRecipe recipe = new ShapelessOreRecipe(new ResourceLocation(group), in, out);
        recipe.setRegistryName(new ResourceLocation(TrailsAndTalesBackport.MOD_ID, id));
        return recipe;
    }

    /** Every color of the item but {@code color} (1.20's tag minus that color's item). */
    static Ingredient others(Item item, EnumDyeColor color) {
        List<ItemStack> stacks = new ArrayList<ItemStack>();
        for (EnumDyeColor c : EnumDyeColor.values()) {
            if (c != color) {
                stacks.add(new ItemStack(item, 1, c.getMetadata()));
            }
        }
        return Ingredient.fromStacks(stacks.toArray(new ItemStack[0]));
    }
}
