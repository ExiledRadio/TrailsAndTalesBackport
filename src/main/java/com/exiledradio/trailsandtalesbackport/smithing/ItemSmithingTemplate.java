package com.exiledradio.trailsandtalesbackport.smithing;

import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A smithing template: named "Smithing Template", with 1.20's {@code SmithingTemplateItem} lines under the name:
 * what it is (gray), then "Applies to:" and "Ingredients:" (gray) with their answers (blue).
 */
public class ItemSmithingTemplate extends Item {

    private final Templates template;

    public ItemSmithingTemplate(Templates template) {
        this.template = template;
    }

    public Templates template() {
        return this.template;
    }

    /** 1.20: every template's own name key gives "Smithing Template", the same for all. */
    @Override
    public String getTranslationKey(ItemStack stack) {
        return "item." + TrailsAndTalesBackport.MOD_ID + ".smithing_template";
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        boolean trim = this.template.isTrim();
        String what = trim ? "trim_pattern." + TrailsAndTalesBackport.MOD_ID + "." + this.template.pattern
                : "upgrade.minecraft.netherite_upgrade";
        String kind = trim ? "armor_trim" : "netherite_upgrade";
        tooltip.add(TextFormatting.GRAY + I18n.format(what));
        tooltip.add("");
        tooltip.add(TextFormatting.GRAY + I18n.format("item.minecraft.smithing_template.applies_to"));
        tooltip.add(" " + TextFormatting.BLUE + I18n.format("item.minecraft.smithing_template." + kind + ".applies_to"));
        tooltip.add(TextFormatting.GRAY + I18n.format("item.minecraft.smithing_template.ingredients"));
        tooltip.add(" " + TextFormatting.BLUE + I18n.format("item.minecraft.smithing_template." + kind + ".ingredients"));
    }
}
