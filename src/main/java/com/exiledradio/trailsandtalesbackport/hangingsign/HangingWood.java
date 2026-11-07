package com.exiledradio.trailsandtalesbackport.hangingsign;

import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.common.Owners;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;

import javax.annotation.Nullable;

/**
 * Every wood 1.20 gives a hanging sign, and where its stripped log comes from (family Rule 2: hanging signs
 * are 1.20's, so this mod adds them for other mods' woods too).
 *
 * <p>A wood whose mod is not installed (crimson and warped without Nether Update, mangrove without Wild
 * Update) or whose stream is switched off (cherry, bamboo) gets no hanging sign at all.
 */
public enum HangingWood {
    OAK("oak", "hanging_sign", null, 0),
    SPRUCE("spruce", "hanging_sign", null, 1),
    BIRCH("birch", "hanging_sign", null, 2),
    JUNGLE("jungle", "hanging_sign", null, 3),
    ACACIA("acacia", "hanging_sign", null, 4),
    DARK_OAK("dark_oak", "hanging_sign", null, 5),
    CHERRY("cherry", "cherry_wood_hanging_sign", null, -1),
    BAMBOO("bamboo", "bamboo_wood_hanging_sign", null, -1),
    CRIMSON("crimson", "nether_wood_hanging_sign", Owners.NETHER_UPDATE, -1),
    WARPED("warped", "nether_wood_hanging_sign", Owners.NETHER_UPDATE, -1),
    MANGROVE("mangrove", "hanging_sign", Owners.WILD_UPDATE, -1);

    public final String name;
    /** 1.20's sound set: "hanging_sign" gives block.hanging_sign.break and the rest. */
    public final String soundSet;
    /** The family mod that adds this wood, or null for vanilla's and ours. */
    @Nullable
    public final String mod;
    /** 1.12.2's planks metadata for vanilla woods, else -1. */
    private final int vanillaMeta;

    HangingWood(String name, String soundSet, @Nullable String mod, int vanillaMeta) {
        this.name = name;
        this.soundSet = soundSet;
        this.mod = mod;
        this.vanillaMeta = vanillaMeta;
    }

    /** @return true if this wood exists in this game, so its hanging signs are registered */
    public boolean present() {
        if (this == CHERRY) {
            return ModConfig.cherry.enabled;
        }
        if (this == BAMBOO) {
            return ModConfig.bamboo.enabled;
        }
        return this.mod == null || Loader.isModLoaded(this.mod);
    }

    /**
     * What the recipe takes: 1.20's stripped log of this wood from whichever mod has it. For vanilla woods:
     * Village & Pillage's, else Update Aquatic's copy, else this mod's own copy.
     *
     * @return the ingredient stack, or null when the wood's mod lacks it (no recipe)
     */
    @Nullable
    public ItemStack log() {
        switch (this) {
            case CHERRY:
                return stack(Owners.ours("stripped_cherry_log"));
            case BAMBOO:
                return stack(Owners.ours("stripped_bamboo_block"));
            case CRIMSON:
                return stack(Owners.theirs(this.mod, "stripped_crimson_stem"));
            case WARPED:
                return stack(Owners.theirs(this.mod, "stripped_warped_stem"));
            case MANGROVE:
                return stack(Owners.theirs(this.mod, "stripped_mangrove_log"));
            default:
                Item stripped = Owners.theirs(Owners.VILLAGE_AND_PILLAGE, "stripped_" + this.name + "_log");
                if (stripped == null) {
                    stripped = Owners.theirs(Owners.UPDATE_AQUATIC, "stripped_" + this.name + "_log");
                }
                if (stripped == null) {
                    // Neither installed: our own copy of the owner's stripped log (woodcopy).
                    stripped = Owners.ours("stripped_" + this.name + "_log");
                }
                return stack(stripped);
        }
    }

    @Nullable
    private static ItemStack stack(@Nullable Item item) {
        return item == null ? null : new ItemStack(item);
    }
}
