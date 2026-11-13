package com.exiledradio.trailsandtalesbackport.archaeology;

import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The decorated pot's item: its faces are under {@code BlockEntityTag.sherds}. The tooltip lists them as
 * 1.20 does (front, left, right, back), after a blank line, unless all four are bricks.
 */
public class ItemDecoratedPot extends ItemBlock {

    public ItemDecoratedPot(Block block) {
        super(block);
    }

    /** @return back, left, right, front of a pot stack */
    public static Item[] sherds(ItemStack stack) {
        NBTTagCompound be = stack.getSubCompound("BlockEntityTag");
        return TileDecoratedPot.readSherds(be != null ? be : new NBTTagCompound());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        Item[] s = sherds(stack);
        if (TileDecoratedPot.allBricks(s)) {
            return;
        }
        tooltip.add("");
        for (int i : new int[]{3, 1, 2, 0}) {
            tooltip.add(TextFormatting.GRAY + new ItemStack(s[i]).getDisplayName());
        }
    }
}
