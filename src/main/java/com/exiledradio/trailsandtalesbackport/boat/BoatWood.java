package com.exiledradio.trailsandtalesbackport.boat;

import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.common.Owners;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;

/**
 * The woods of this mod that float: cherry boats and bamboo rafts. Their boats are Backport Core's
 * ({@link com.exiledradio.backportcore.boat.BoatWood}), each wood registered there as {@code trailsandtalesbackport:<name>}.
 */
public enum BoatWood {
    CHERRY("cherry", false, "cherry_planks"),
    BAMBOO("bamboo", true, "bamboo_planks");

    public final String name;
    /** 1.20's raft: its own model, riders sitting higher. */
    public final boolean raft;
    private final String planks;

    BoatWood(String name, boolean raft, String planks) {
        this.name = name;
        this.raft = raft;
        this.planks = planks;
    }

    /** @return true if this wood's stream is on, so its boats exist */
    public boolean present() {
        return this == CHERRY ? ModConfig.cherry.enabled : ModConfig.bamboo.enabled;
    }

    /** "cherry_boat", "bamboo_raft"; with a chest: "cherry_chest_boat", "bamboo_chest_raft" */
    public String itemName(boolean chest) {
        return this.name + (chest ? "_chest" : "") + (this.raft ? "_raft" : "_boat");
    }

    @Nullable
    public ItemStack planks(int count) {
        Block b = Owners.ourBlock(this.planks);
        return b == null ? null : new ItemStack(b, count);
    }

    /** @return Backport Core's wood for this one, or null when its boats are not in this game */
    @Nullable
    public com.exiledradio.backportcore.boat.BoatWood core() {
        return com.exiledradio.backportcore.boat.BoatWood.find(com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport.MOD_ID + ":" + this.name);
    }

    @Nullable
    public static BoatWood byName(String name) {
        for (BoatWood w : values()) {
            if (w.name.equals(name)) {
                return w;
            }
        }
        return null;
    }
}
