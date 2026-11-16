// Copied from CavesAndCliffsBackport/src/main/java/com/exiledradio/cavesandcliffsbackport/common/BlockBasic.java by tools/smithing/copy_from_owner.py; edit the owner's file, not this one.
package com.exiledradio.trailsandtalesbackport.smithing.copy;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;

/** A plain full block with its material, map color, sound, hardness and blast resistance. */
public class BlockBasic extends Block {

    /** @param resistance blast resistance in current Java's units, as the wiki gives it */
    public BlockBasic(Material material, MapColor color, SoundType sound, float hardness, float resistance) {
        super(material, color);
        setSoundType(sound);
        setHardness(hardness);
        resistance(this, resistance);
    }

    /**
     * Sets blast resistance in current Java's units. 1.12's {@code setResistance(r)} stores {@code 3r} and
     * explosions read a fifth of that, so it behaves as Java's {@code 0.6r} (stone: 10 here, 6 in Java).
     */
    public static <B extends Block> B resistance(B block, float javaResistance) {
        block.setResistance(javaResistance * 5.0F / 3.0F);
        return block;
    }
}
