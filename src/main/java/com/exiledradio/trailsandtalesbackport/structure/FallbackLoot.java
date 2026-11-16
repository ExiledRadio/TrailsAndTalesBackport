package com.exiledradio.trailsandtalesbackport.structure;

import com.exiledradio.trailsandtalesbackport.common.Content;
import com.exiledradio.trailsandtalesbackport.common.Owners;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootEntryItem;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraftforge.event.LootTableLoadEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Decided 3's fallback loot: an item whose only 1.20 source is a structure another family mod owns is added, when
 * that owner is absent, to the archaeology table of the structure here that is most like it, with its 1.20 weight.
 * With the owner installed nothing is added: only vanilla's sources.
 *
 * <ul>
 *   <li>Warm ocean ruins (Update Aquatic): angler, shelter and snort sherds and the sniffer egg, weight 1 each (as in
 *       1.20's {@code ocean_ruin_warm}), into the desert pyramid's suspicious sand: the warm, sandy site.</li>
 *   <li>Cold ocean ruins (Update Aquatic): blade, explorer, mourner and plenty sherds, weight 1 each (as in
 *       {@code ocean_ruin_cold}), into common trail ruins' suspicious gravel: the cold, gravelly site.</li>
 * </ul>
 * Each entry is named {@code fallback_<item>}. Another stream adds its own owner's items with {@link #add}.
 */
public final class FallbackLoot {

    /** One fallback: an item of ours, the owner whose structure is its real source, the table it joins, its weight. */
    public static final class Entry {
        public final String item;
        public final String owner;
        public final ResourceLocation table;
        public final int weight;

        Entry(String item, String owner, ResourceLocation table, int weight) {
            this.item = item;
            this.owner = owner;
            this.table = table;
            this.weight = weight;
        }
    }

    static final ResourceLocation DESERT_PYRAMID = Content.id("archaeology/desert_pyramid");
    static final ResourceLocation TRAIL_RUINS_COMMON = Content.id("archaeology/trail_ruins_common");

    private static final List<Entry> ENTRIES = new ArrayList<Entry>();

    static {
        for (String s : new String[]{"angler", "shelter", "snort"}) {
            add(s + "_pottery_sherd", Owners.UPDATE_AQUATIC, DESERT_PYRAMID, 1);
        }
        add("sniffer_egg", Owners.UPDATE_AQUATIC, DESERT_PYRAMID, 1);
        for (String s : new String[]{"blade", "explorer", "mourner", "plenty"}) {
            add(s + "_pottery_sherd", Owners.UPDATE_AQUATIC, TRAIL_RUINS_COMMON, 1);
        }
    }

    private FallbackLoot() {
    }

    /** Adds a fallback, before tables load (pre-init or init). */
    public static synchronized void add(String item, String owner, ResourceLocation table, int weight) {
        ENTRIES.add(new Entry(item, owner, table, weight));
    }

    public static synchronized List<Entry> entries() {
        return Collections.unmodifiableList(new ArrayList<Entry>(ENTRIES));
    }

    /** Adds the fallbacks of absent owners to the table that is loading. */
    static void onLoad(LootTableLoadEvent event) {
        LootPool pool = event.getTable().getPool("main");
        if (pool == null) {
            return;
        }
        for (Entry e : entries()) {
            if (!e.table.equals(event.getName()) || Owners.present(e.owner)) {
                continue;
            }
            Item item = Owners.ours(e.item);
            String name = "fallback_" + e.item;
            if (item != null && pool.getEntry(name) == null) {
                pool.addEntry(new LootEntryItem(item, e.weight, 0, new LootFunction[0], new LootCondition[0], name));
            }
        }
    }
}
