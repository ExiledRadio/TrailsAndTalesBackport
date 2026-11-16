package com.exiledradio.trailsandtalesbackport.smithing;

import com.exiledradio.trailsandtalesbackport.common.Owners;

import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootEntry;
import net.minecraft.world.storage.loot.LootEntryEmpty;
import net.minecraft.world.storage.loot.LootEntryItem;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.RandomValueRange;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraft.world.storage.loot.functions.SetCount;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Where 1.20 puts its templates: each in a pool of its own added to the structure's loot table, with 1.20.6's
 * weights (an empty entry against the template) and counts.
 *
 * <p>Structures 1.12.2 has get theirs always: desert pyramid (dune), jungle temple (wild), nether fortress (rib),
 * end city (spire), stronghold corridor and library (eye), woodland mansion (vex), elder guardian (tide). The rest
 * go into their owner's table when the owner is installed: Village &amp; Pillage's pillager outpost (sentry),
 * Update Aquatic's shipwrecks (coast), Nether Update's bastions (snout, netherite upgrade), Wild Update's ancient
 * city (ward, silence). Trail ruins' four are archaeology's (stream A).
 *
 * <p><b>Fallback loot</b> (Decided 3), only while that owner is absent, at the same odds, in the nearest 1.12.2
 * structure: sentry in woodland mansions (the illagers' other home), coast in dungeons, snout and the netherite
 * upgrade in nether fortresses (the Nether's one structure), ward and silence in abandoned mineshafts (deep
 * underground). With the owner installed, only vanilla's sources.
 */
final class SmithingLoot {

    private static final class Drop {
        final Templates template;
        final int empty;
        final int weight;
        final int count;

        Drop(Templates template, int empty, int weight, int count) {
            this.template = template;
            this.empty = empty;
            this.weight = weight;
            this.count = count;
        }
    }

    /** Loot table to the pools added to it; each pool is a list (one template, or ward and silence together). */
    private static final Map<ResourceLocation, List<List<Drop>>> POOLS = new LinkedHashMap<ResourceLocation, List<List<Drop>>>();

    private SmithingLoot() {
    }

    static void register() {
        add("minecraft:chests/desert_pyramid", one(Templates.DUNE, 6, 2));
        add("minecraft:chests/jungle_temple", one(Templates.WILD, 2, 2));
        add("minecraft:chests/nether_bridge", one(Templates.RIB, 14, 1));
        add("minecraft:chests/end_city_treasure", one(Templates.SPIRE, 14, 1));
        add("minecraft:chests/stronghold_corridor", one(Templates.EYE, 9, 1));
        add("minecraft:chests/stronghold_library", one(Templates.EYE, 0, 1));
        add("minecraft:chests/woodland_mansion", one(Templates.VEX, 1, 1));
        add("minecraft:entities/elder_guardian", one(Templates.TIDE, 4, 1));

        String outpost = Loader.isModLoaded(Owners.VILLAGE_AND_PILLAGE) ? Owners.VILLAGE_AND_PILLAGE + ":chests/pillager_outpost"
                : "minecraft:chests/woodland_mansion";
        add(outpost, one(Templates.SENTRY, 3, 2));

        if (Loader.isModLoaded(Owners.UPDATE_AQUATIC)) {
            for (String s : new String[] {"map", "supply", "treasure"}) {
                add(Owners.UPDATE_AQUATIC + ":chests/shipwreck_" + s, one(Templates.COAST, 5, 2));
            }
        } else {
            add("minecraft:chests/simple_dungeon", one(Templates.COAST, 5, 2));
        }

        if (Loader.isModLoaded(Owners.NETHER_UPDATE)) {
            for (String s : new String[] {"bridge", "hoglin_stable", "other"}) {
                add(Owners.NETHER_UPDATE + ":chests/bastion_" + s, one(Templates.SNOUT, 11, 1));
                add(Owners.NETHER_UPDATE + ":chests/bastion_" + s, one(Templates.NETHERITE_UPGRADE, 9, 1));
            }
            add(Owners.NETHER_UPDATE + ":chests/bastion_treasure", one(Templates.SNOUT, 11, 1));
            add(Owners.NETHER_UPDATE + ":chests/bastion_treasure", one(Templates.NETHERITE_UPGRADE, 0, 1));
        } else {
            add("minecraft:chests/nether_bridge", one(Templates.SNOUT, 11, 1));
            add("minecraft:chests/nether_bridge", one(Templates.NETHERITE_UPGRADE, 9, 1));
        }

        List<Drop> city = new ArrayList<Drop>();
        city.add(new Drop(Templates.WARD, 75, 4, 1));
        city.add(new Drop(Templates.SILENCE, 75, 1, 1));
        add(Loader.isModLoaded(Owners.WILD_UPDATE) ? Owners.WILD_UPDATE + ":chests/ancient_city"
                : "minecraft:chests/abandoned_mineshaft", city);

        MinecraftForge.EVENT_BUS.register(new SmithingLoot());
    }

    private static List<Drop> one(Templates t, int empty, int count) {
        List<Drop> l = new ArrayList<Drop>();
        l.add(new Drop(t, empty, 1, count));
        return l;
    }

    private static void add(String table, List<Drop> pool) {
        ResourceLocation id = new ResourceLocation(table);
        List<List<Drop>> pools = POOLS.get(id);
        if (pools == null) {
            pools = new ArrayList<List<Drop>>();
            POOLS.put(id, pools);
        }
        pools.add(pool);
    }

    /** @return the pools added to that table, for the self-test */
    static int poolsFor(ResourceLocation table) {
        List<List<Drop>> pools = POOLS.get(table);
        return pools == null ? 0 : pools.size();
    }

    @SubscribeEvent
    public void load(LootTableLoadEvent event) {
        List<List<Drop>> pools = POOLS.get(event.getName());
        if (pools == null) {
            return;
        }
        for (List<Drop> drops : pools) {
            String name = "trailsandtalesbackport:" + drops.get(0).template.itemName();
            if (event.getTable().getPool(name) != null) {
                continue;
            }
            List<LootEntry> entries = new ArrayList<LootEntry>();
            if (drops.get(0).empty > 0) {
                entries.add(new LootEntryEmpty(drops.get(0).empty, 0, new LootCondition[0], "empty"));
            }
            for (Drop d : drops) {
                Item item = d.template.item();
                if (item == null) {
                    continue;
                }
                LootFunction[] functions = d.count == 1 ? new LootFunction[0]
                        : new LootFunction[] {new SetCount(new LootCondition[0], new RandomValueRange(d.count))};
                entries.add(new LootEntryItem(item, d.weight, 0, functions, new LootCondition[0], d.template.itemName()));
            }
            event.getTable().addPool(new LootPool(entries.toArray(new LootEntry[0]), new LootCondition[0],
                    new RandomValueRange(1), new RandomValueRange(0), name));
        }
    }
}
