package com.exiledradio.trailsandtalesbackport.archaeology;

import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.registries.IForgeRegistryEntry;

/**
 * 1.20's {@code crafting_decorated_pot}: four bricks or sherds in a diamond on a 3x3 grid make a pot with
 * those faces - the top one is the back, then left, right, and the bottom one the front.
 *
 * <p>A sherd is any item in the {@code potterySherd} ore name (this mod's twenty, and any other mod's), so
 * a later update's sherds go onto pots without code here.
 */
public class RecipeDecoratedPot extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {

    public static final String SHERD_ORE = "potterySherd";

    private final Item pot;

    public RecipeDecoratedPot(Item pot) {
        this.pot = pot;
    }

    /** 1.20's {@code decorated_pot_ingredients} tag: brick or a sherd. */
    public static boolean isIngredient(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() == Items.BRICK || isSherd(stack));
    }

    public static boolean isSherd(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        for (int id : OreDictionary.getOreIDs(stack)) {
            if (SHERD_ORE.equals(OreDictionary.getOreName(id))) {
                return true;
            }
        }
        return false;
    }

    /** Grid slots of back, left, right, front, on a 3-wide grid. */
    static final int[] SLOTS = {1, 3, 5, 7};

    @Override
    public boolean matches(InventoryCrafting inv, World world) {
        if (inv.getWidth() != 3 || inv.getHeight() != 3) {
            return false;
        }
        int count = 0;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            if (!inv.getStackInSlot(i).isEmpty()) {
                count++;
            }
        }
        if (count != 4) {
            return false;
        }
        for (int slot : SLOTS) {
            if (!isIngredient(inv.getStackInSlot(slot))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        Item[] faces = new Item[4];
        for (int i = 0; i < 4; i++) {
            faces[i] = inv.getStackInSlot(SLOTS[i]).getItem();
        }
        return TileDecoratedPot.potItem(this.pot, faces);
    }

    /** @return true if every face of this grid is a sherd (<i>Careful Restoration</i>) */
    static boolean onlySherds(net.minecraft.inventory.IInventory grid) {
        if (grid.getSizeInventory() != 9) {
            return false;
        }
        for (int slot : SLOTS) {
            if (!isSherd(grid.getStackInSlot(slot))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean canFit(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return new ItemStack(this.pot);
    }

    @Override
    public boolean isDynamic() {
        return true;
    }
}
