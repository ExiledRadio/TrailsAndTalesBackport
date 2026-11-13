package com.exiledradio.trailsandtalesbackport.wolf;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import net.minecraftforge.oredict.DyeUtils;
import net.minecraftforge.registries.IForgeRegistryEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 1.20.5's {@code ArmorDyeRecipe} for wolf armor: one wolf armor and at least one dye anywhere in the grid,
 * nothing else. The color is mixed as 1.20.5's {@code DyedItemColor.applyDyes} mixes it (the item's own color,
 * if any, counts as one more dye). Dyes are any item the ore dictionary calls a dye.
 */
public class RecipeWolfArmorDye extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {

    /** 1.20.6's {@code DyeColor} texture colors, by 1.12.2 dye metadata order (white 0 ... black 15). */
    private static final int[] DIFFUSE = {16383998, 16351261, 13061821, 3847130, 16701501, 8439583, 15961002, 4673362,
            10329495, 1481884, 8991416, 3949738, 8606770, 6192150, 11546150, 1908001};

    @Override
    public boolean matches(InventoryCrafting inv, World world) {
        return !assemble(inv).isEmpty();
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        return assemble(inv);
    }

    private static ItemStack assemble(InventoryCrafting inv) {
        ItemStack armor = ItemStack.EMPTY;
        List<EnumDyeColor> dyes = new ArrayList<EnumDyeColor>();
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack s = inv.getStackInSlot(i);
            if (s.isEmpty()) {
                continue;
            }
            if (s.getItem() instanceof ItemWolfArmor) {
                if (!armor.isEmpty()) {
                    return ItemStack.EMPTY;
                }
                armor = s;
            } else {
                Optional<EnumDyeColor> color = DyeUtils.colorFromStack(s);
                if (!color.isPresent()) {
                    return ItemStack.EMPTY;
                }
                dyes.add(color.get());
            }
        }
        if (armor.isEmpty() || dyes.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return applyDyes(armor, dyes);
    }

    /** 1.20.5's {@code DyedItemColor.applyDyes}. */
    public static ItemStack applyDyes(ItemStack stack, List<EnumDyeColor> dyes) {
        ItemStack out = stack.copy();
        out.setCount(1);
        int r = 0;
        int g = 0;
        int b = 0;
        int max = 0;
        int n = 0;
        if (ItemWolfArmor.hasColor(out)) {
            int rgb = ItemWolfArmor.getColor(out);
            int cr = rgb >> 16 & 255;
            int cg = rgb >> 8 & 255;
            int cb = rgb & 255;
            max += Math.max(cr, Math.max(cg, cb));
            r += cr;
            g += cg;
            b += cb;
            n++;
        }
        for (EnumDyeColor dye : dyes) {
            int d = DIFFUSE[dye.getMetadata()];
            // Through 1.20's floats, as it does: (int) ((c / 255F) * 255F).
            int dr = (int) ((float) (d >> 16 & 255) / 255.0F * 255.0F);
            int dg = (int) ((float) (d >> 8 & 255) / 255.0F * 255.0F);
            int db = (int) ((float) (d & 255) / 255.0F * 255.0F);
            max += Math.max(dr, Math.max(dg, db));
            r += dr;
            g += dg;
            b += db;
            n++;
        }
        int ar = r / n;
        int ag = g / n;
        int ab = b / n;
        float avgMax = (float) max / (float) n;
        float top = (float) Math.max(ar, Math.max(ag, ab));
        ar = (int) ((float) ar * avgMax / top);
        ag = (int) ((float) ag * avgMax / top);
        ab = (int) ((float) ab * avgMax / top);
        ItemWolfArmor.setColor(out, (ar << 16) + (ag << 8) + ab);
        return out;
    }

    @Override
    public boolean canFit(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isDynamic() {
        return true;
    }
}
