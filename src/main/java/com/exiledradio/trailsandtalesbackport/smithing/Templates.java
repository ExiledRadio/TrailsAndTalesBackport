package com.exiledradio.trailsandtalesbackport.smithing;

import com.exiledradio.trailsandtalesbackport.common.Owners;

import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;

/**
 * 1.20's seventeen smithing templates, with what each duplicates with (1.20.6's
 * {@code <name>_smithing_template.json}: 7 diamonds, the template and this block make two).
 */
public enum Templates {

    NETHERITE_UPGRADE("netherite_upgrade", null, "minecraft:netherrack"),
    COAST("coast", false, "minecraft:cobblestone"),
    DUNE("dune", false, "minecraft:sandstone"),
    EYE("eye", false, "minecraft:end_stone"),
    HOST("host", false, "minecraft:hardened_clay"),
    RAISER("raiser", false, "minecraft:hardened_clay"),
    RIB("rib", true, "minecraft:netherrack"),
    SENTRY("sentry", false, "minecraft:cobblestone"),
    SHAPER("shaper", false, "minecraft:hardened_clay"),
    SILENCE("silence", true, Owners.CAVES_AND_CLIFFS + ":cobbled_deepslate"),
    SNOUT("snout", true, Owners.NETHER_UPDATE + ":blackstone"),
    SPIRE("spire", true, "minecraft:purpur_block"),
    TIDE("tide", true, "minecraft:prismarine"),
    VEX("vex", true, "minecraft:cobblestone"),
    WARD("ward", true, Owners.CAVES_AND_CLIFFS + ":cobbled_deepslate"),
    WAYFINDER("wayfinder", true, "minecraft:hardened_clay"),
    WILD("wild", false, "minecraft:mossy_cobblestone");

    /** The pattern's name (and the trim pattern's id path); {@code netherite_upgrade} for the upgrade. */
    public final String pattern;
    /**
     * In 1.20's {@code Smithing with Style} (the eight "exclusive" patterns); null for the netherite upgrade, which
     * is no trim.
     */
    @Nullable
    public final Boolean exclusive;
    /** The block it duplicates with, {@code modid:name}. */
    public final String duplicatesWith;

    @Nullable
    Item item;

    Templates(String pattern, @Nullable Boolean exclusive, String duplicatesWith) {
        this.pattern = pattern;
        this.exclusive = exclusive;
        this.duplicatesWith = duplicatesWith;
    }

    public boolean isTrim() {
        return this != NETHERITE_UPGRADE;
    }

    /** 1.20's item name: {@code coast_armor_trim_smithing_template}, {@code netherite_upgrade_smithing_template}. */
    public String itemName() {
        return isTrim() ? this.pattern + "_armor_trim_smithing_template" : "netherite_upgrade_smithing_template";
    }

    @Nullable
    public Item item() {
        return this.item;
    }

    /** @return the template this stack is, or null */
    @Nullable
    public static Templates of(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        for (Templates t : values()) {
            if (t.item != null && t.item == stack.getItem()) {
                return t;
            }
        }
        return null;
    }

    /** The block a template duplicates with, as an item stack; empty when its owner is absent. */
    ItemStack duplicationBlock() {
        String[] id = this.duplicatesWith.split(":");
        if ("minecraft".equals(id[0])) {
            net.minecraft.block.Block b = net.minecraft.block.Block.getBlockFromName(this.duplicatesWith);
            return b == null || b == Blocks.AIR ? ItemStack.EMPTY : new ItemStack(b, 1, 0);
        }
        Item theirs = Owners.theirs(id[0], id[1]);
        return theirs == null ? ItemStack.EMPTY : new ItemStack(theirs);
    }
}
