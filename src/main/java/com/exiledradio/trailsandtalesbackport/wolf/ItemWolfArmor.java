package com.exiledradio.trailsandtalesbackport.wolf;

import com.exiledradio.trailsandtalesbackport.common.Owners;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * 1.20.5's wolf armor: an {@code AnimalArmorItem} of the armadillo material, canine body type, with an overlay.
 *
 * <ul>
 *   <li>One per stack, 64 durability ({@code ArmorItem.Type.BODY.getDurability(4)}: 16 x 4).</li>
 *   <li>Not enchantable, at the table or with books ({@code AnimalArmorItem.isEnchantable} is false).</li>
 *   <li>Repaired in an anvil with armadillo scutes (the material's repair ingredient).</li>
 *   <li>Dyeable: the color is kept where 1.12.2 keeps leather's ({@code display.color}), which also gives it
 *       1.12.2's "Dyed" / "Color: #RRGGBB" tooltip line.</li>
 * </ul>
 */
public class ItemWolfArmor extends Item {

    /** 1.20.5's {@code ArmorMaterials.ARMADILLO} defense for the body. */
    public static final int DEFENSE = 11;
    public static final int DURABILITY = 64;

    public ItemWolfArmor() {
        setMaxStackSize(1);
        setMaxDamage(DURABILITY);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public int getItemEnchantability() {
        return 0;
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
    }

    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        Item scute = Owners.ours("armadillo_scute");
        return scute != null && !repair.isEmpty() && repair.getItem() == scute;
    }

    // ------------------------------------------------------------------ color, as 1.12.2's leather armor

    public static boolean hasColor(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null && tag.hasKey("display", 10) && tag.getCompoundTag("display").hasKey("color", 3);
    }

    /** @return the dyed RGB, or -1 when undyed */
    public static int getColor(ItemStack stack) {
        return hasColor(stack) ? stack.getTagCompound().getCompoundTag("display").getInteger("color") : -1;
    }

    public static void setColor(ItemStack stack, int rgb) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        NBTTagCompound display = tag.getCompoundTag("display");
        if (!tag.hasKey("display", 10)) {
            tag.setTag("display", display);
        }
        display.setInteger("color", rgb);
    }

    public static void removeColor(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && tag.hasKey("display", 10)) {
            NBTTagCompound display = tag.getCompoundTag("display");
            display.removeTag("color");
            if (display.isEmpty()) {
                tag.removeTag("display");
            }
            if (tag.isEmpty()) {
                stack.setTagCompound(null);
            }
        }
    }
}
