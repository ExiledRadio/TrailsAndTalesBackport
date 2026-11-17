package com.exiledradio.trailsandtalesbackport.structure;

import net.minecraft.init.Items;
import net.minecraft.item.Item;

/**
 * Called from patch P7 ({@code asm.JukeboxTransformer}, see {@code docs/PATCHES.md}): each value
 * {@code BlockJukebox.getComparatorInputOverride} returns.
 */
public final class JukeboxHooks {

    /** 1.20's {@code RecordItem} analog output for Relic. */
    public static final int RELIC_SIGNAL = 14;

    private JukeboxHooks() {
    }

    /**
     * @return 14 for the value 1.12.2 counts for Relic ({@code id(relic) + 1 - id(record_13)}), every other value as
     * it is (vanilla's discs, an empty jukebox's 0, another mod's own answer)
     */
    public static int signal(int vanilla) {
        Item relic = StructuresContent.relic;
        if (relic == null || !StructuresContent.enabled()) {
            return vanilla;
        }
        return vanilla == Item.getIdFromItem(relic) + 1 - Item.getIdFromItem(Items.RECORD_13) ? RELIC_SIGNAL : vanilla;
    }
}
