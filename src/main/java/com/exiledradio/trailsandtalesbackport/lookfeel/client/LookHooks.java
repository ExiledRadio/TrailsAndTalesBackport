package com.exiledradio.trailsandtalesbackport.lookfeel.client;

import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.trailsandtalesbackport.lookfeel.LookAndFeel;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Client hooks the coremod's patches call (see {@code docs/PATCHES.md}). */
@SideOnly(Side.CLIENT)
public final class LookHooks {

    /** For the client self-test: how many picked stacks P1 changed. */
    public static volatile int pickedChanged;

    private LookHooks() {
    }

    /**
     * P1: what {@code Minecraft.storeTEInStack} returns. 1.20.5 (24w09a's components) stopped adding "(+NBT)" to a
     * Ctrl + picked block, and a named block (a renamed chest) gives a named item ({@code BlockEntity.collectComponents}
     * hands over its custom name).
     */
    public static ItemStack pickedBlock(ItemStack stack) {
        if (!LookAndFeel.enabled() || !Presentation.isAtLeast("1.20.5") || !stack.hasTagCompound()) {
            return stack;
        }
        NBTTagCompound tag = stack.getTagCompound();
        boolean changed = false;
        if (tag.hasKey("display", Constants.NBT.TAG_COMPOUND)) {
            NBTTagCompound display = tag.getCompoundTag("display");
            NBTTagList lore = display.getTagList("Lore", Constants.NBT.TAG_STRING);
            if (lore.tagCount() == 1 && "(+NBT)".equals(lore.getStringTagAt(0))) {
                display.removeTag("Lore");
                changed = true;
            }
            if (display.isEmpty()) {
                tag.removeTag("display");
            }
        }
        NBTTagCompound block = tag.getCompoundTag("BlockEntityTag");
        if (block.hasKey("CustomName", Constants.NBT.TAG_STRING) && !block.getString("CustomName").isEmpty() && !stack.hasDisplayName()) {
            stack.setStackDisplayName(block.getString("CustomName"));
            changed = true;
        }
        if (changed) {
            pickedChanged++;
        }
        return stack;
    }
}
