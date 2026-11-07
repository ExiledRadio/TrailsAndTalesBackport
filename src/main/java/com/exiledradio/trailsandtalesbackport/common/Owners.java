package com.exiledradio.trailsandtalesbackport.common;

import com.exiledradio.backportcore.ownership.BlockOwnership;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;

/**
 * Who owns a block or item that more than one mod of the family needs (family Rule 1).
 *
 * <h2>The rule</h2>
 * Content belongs to the mod for the update it first shipped in. When the owner is installed this mod
 * registers nothing for it and uses the owner's; when it is not, this mod registers its own copy under its
 * own id, with the same states and metadata, so the owner can later rename it in place. Backport Core's
 * {@link BlockOwnership} keeps the table; this class adds the family's mod ids and lookups by registry
 * name that never link against another mod's classes.
 */
public final class Owners {

    public static final String UPDATE_AQUATIC = "updateaquaticbackport";
    /** Owns the smithing table, bamboo, stripped logs and the five woods' extra signs, among others. */
    public static final String VILLAGE_AND_PILLAGE = "villagerbackport";
    /** Owns honeycomb (sign waxing). */
    public static final String BUZZY_BEES = "buzzybeesbackport";
    /** Owns chains, crimson and warped wood, netherite, bastions and piglins. */
    public static final String NETHER_UPDATE = "netherupdatebackport";
    /** Owns amethyst, copper, glow ink, moss and the grove. */
    public static final String CAVES_AND_CLIFFS = "cavesandcliffsbackport";
    /** Owns sculk and the sculk sensor, mangrove wood, mud bricks, chest boats and ancient cities. */
    public static final String WILD_UPDATE = "wildupdatebackport";

    private Owners() {
    }

    /** @return true if that family mod is installed */
    public static boolean present(String owner) {
        return Loader.isModLoaded(owner);
    }

    /**
     * For a block this mod copies from its owner: register ours only if no installed mod provides it.
     * Call from the block registry event (Core's table is complete by then).
     */
    public static boolean shouldRegisterCopy(String path) {
        return BlockOwnership.shouldRegister(TrailsAndTalesBackport.MOD_ID, path);
    }

    /**
     * @return the block registered under {@code owner:name} if the owner is installed, else ours under
     * this mod's id; null if neither exists
     */
    @Nullable
    public static Block block(String owner, String name) {
        ResourceLocation id = present(owner) ? new ResourceLocation(owner, name) : Content.id(name);
        Block block = ForgeRegistries.BLOCKS.getValue(id);
        return block == null || !id.equals(block.getRegistryName()) ? null : block;
    }

    /** @return the item for {@code name} the same way as {@link #block}, or null */
    @Nullable
    public static Item item(String owner, String name) {
        ResourceLocation id = present(owner) ? new ResourceLocation(owner, name) : Content.id(name);
        Item item = ForgeRegistries.ITEMS.getValue(id);
        return item == null || !id.equals(item.getRegistryName()) ? null : item;
    }

    /** @return another family mod's item {@code owner:name}, or null when that mod or item is absent */
    @Nullable
    public static Item theirs(String owner, String name) {
        if (!present(owner)) {
            return null;
        }
        ResourceLocation id = new ResourceLocation(owner, name);
        Item item = ForgeRegistries.ITEMS.getValue(id);
        return item == null || !id.equals(item.getRegistryName()) ? null : item;
    }

    /** As {@link #theirs}, for blocks. */
    @Nullable
    public static Block theirBlock(String owner, String name) {
        if (!present(owner)) {
            return null;
        }
        ResourceLocation id = new ResourceLocation(owner, name);
        Block block = ForgeRegistries.BLOCKS.getValue(id);
        return block == null || !id.equals(block.getRegistryName()) ? null : block;
    }

    /**
     * Looks up another stream's item by name, so streams never link against each other's classes.
     *
     * @return this mod's item {@code name}, or null when its stream is switched off
     */
    @Nullable
    public static Item ours(String name) {
        Item item = ForgeRegistries.ITEMS.getValue(Content.id(name));
        return item == null || !Content.id(name).equals(item.getRegistryName()) ? null : item;
    }

    /** As {@link #ours(String)}, for blocks. */
    @Nullable
    public static Block ourBlock(String name) {
        Block block = ForgeRegistries.BLOCKS.getValue(Content.id(name));
        return block == null || !Content.id(name).equals(block.getRegistryName()) ? null : block;
    }
}
