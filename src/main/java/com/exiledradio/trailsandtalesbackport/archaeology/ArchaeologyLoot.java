package com.exiledradio.trailsandtalesbackport.archaeology;

import com.exiledradio.trailsandtalesbackport.common.Content;
import com.exiledradio.trailsandtalesbackport.common.Owners;

import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootEntryItem;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraft.world.storage.loot.RandomValueRange;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraft.world.storage.loot.functions.SetMetadata;
import net.minecraftforge.event.LootTableLoadEvent;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * The six archaeology loot tables, as 1.20.6 has them, under this mod's id:
 * {@code trailsandtalesbackport:archaeology/<desert_pyramid|desert_well|ocean_ruin_cold|ocean_ruin_warm|
 * trail_ruins_common|trail_ruins_rare>}.
 *
 * <p>The JSON files (from {@code tools/archaeology/generate.py}) hold every entry 1.12.2 always has. The
 * entries for items another stream or family mod owns are added here when the table loads, each only when
 * its item exists, with 1.20.6's weight; without it that entry is simply not in the table:
 * <ul>
 *   <li>desert well: suspicious stew (Village &amp; Pillage), one of six effects as 1.20.6's
 *       {@code set_stew_effect};</li>
 *   <li>warm ocean ruins: sniffer egg (stream N);</li>
 *   <li>common trail ruins: blue and white dye (Village &amp; Pillage's, else 1.12.2's lapis lazuli and bone
 *       meal, which are its blue and white dye), red, green, purple and brown candles (Caves &amp; Cliffs),
 *       spruce and oak hanging signs (stream H);</li>
 *   <li>rare trail ruins: the wayfinder, raiser, shaper and host trim templates (stream Sm) and the music
 *       disc Relic (stream St).</li>
 * </ul>
 */
public final class ArchaeologyLoot {

    public static final ResourceLocation DESERT_PYRAMID = Content.id("archaeology/desert_pyramid");
    public static final ResourceLocation DESERT_WELL = Content.id("archaeology/desert_well");
    public static final ResourceLocation OCEAN_RUIN_COLD = Content.id("archaeology/ocean_ruin_cold");
    public static final ResourceLocation OCEAN_RUIN_WARM = Content.id("archaeology/ocean_ruin_warm");
    public static final ResourceLocation TRAIL_RUINS_COMMON = Content.id("archaeology/trail_ruins_common");
    public static final ResourceLocation TRAIL_RUINS_RARE = Content.id("archaeology/trail_ruins_rare");

    public static final ResourceLocation[] TABLES = {DESERT_PYRAMID, DESERT_WELL, OCEAN_RUIN_COLD, OCEAN_RUIN_WARM,
            TRAIL_RUINS_COMMON, TRAIL_RUINS_RARE};

    private ArchaeologyLoot() {
    }

    static void register() {
        for (ResourceLocation table : TABLES) {
            LootTableList.register(table);
        }
    }

    static void onLoad(LootTableLoadEvent event) {
        ResourceLocation name = event.getName();
        LootPool pool = event.getTable().getPool("main");
        if (pool == null) {
            return;
        }
        if (DESERT_WELL.equals(name)) {
            add(pool, Owners.theirs(Owners.VILLAGE_AND_PILLAGE, "suspicious_stew"), 0, 1, "suspicious_stew",
                    new SetStewEffect());
        } else if (OCEAN_RUIN_WARM.equals(name)) {
            add(pool, Owners.ours("sniffer_egg"), 0, 1, "sniffer_egg");
        } else if (TRAIL_RUINS_COMMON.equals(name)) {
            dye(pool, "blue_dye", 4);
            dye(pool, "white_dye", 15);
            for (String candle : new String[]{"red_candle", "green_candle", "purple_candle", "brown_candle"}) {
                add(pool, Owners.theirs(Owners.CAVES_AND_CLIFFS, candle), 0, 2, candle);
            }
            add(pool, Owners.ours("spruce_hanging_sign"), 0, 1, "spruce_hanging_sign");
            add(pool, Owners.ours("oak_hanging_sign"), 0, 1, "oak_hanging_sign");
        } else if (TRAIL_RUINS_RARE.equals(name)) {
            for (String template : new String[]{"wayfinder", "raiser", "shaper", "host"}) {
                String id = template + "_armor_trim_smithing_template";
                add(pool, Owners.ours(id), 0, 1, id);
            }
            add(pool, Owners.ours("music_disc_relic"), 0, 1, "music_disc_relic");
        }
    }

    /** Village &amp; Pillage's 1.14 dye when installed, else 1.12.2's dye of that color. */
    private static void dye(LootPool pool, String name, int vanillaMeta) {
        Item theirs = Owners.theirs(Owners.VILLAGE_AND_PILLAGE, name);
        if (theirs != null) {
            add(pool, theirs, 0, 2, name);
        } else {
            add(pool, Items.DYE, vanillaMeta, 2, name);
        }
    }

    private static void add(LootPool pool, @Nullable Item item, int meta, int weight, String entryName, LootFunction... extra) {
        if (item == null || pool.getEntry(entryName) != null) {
            return;
        }
        LootFunction[] functions = extra;
        if (meta != 0) {
            functions = new LootFunction[extra.length + 1];
            functions[0] = new SetMetadata(new LootCondition[0], new RandomValueRange(meta));
            System.arraycopy(extra, 0, functions, 1, extra.length);
        }
        pool.addEntry(new LootEntryItem(item, weight, 0, functions, new LootCondition[0], entryName));
    }

    /**
     * 1.20.6's {@code set_stew_effect} for the desert well: one of six effects at random, its duration in
     * whole seconds from the range (ticks for an instant effect, as vanilla), written in 1.14's stew NBT.
     */
    static final class SetStewEffect extends LootFunction {

        private static final Object[][] EFFECTS = {
                {MobEffects.NIGHT_VISION, 7, 10},
                {MobEffects.JUMP_BOOST, 7, 10},
                {MobEffects.WEAKNESS, 6, 8},
                {MobEffects.BLINDNESS, 5, 7},
                {MobEffects.POISON, 10, 20},
                {MobEffects.SATURATION, 7, 10},
        };

        SetStewEffect() {
            super(new LootCondition[0]);
        }

        @Override
        public ItemStack apply(ItemStack stack, Random rand, LootContext context) {
            Object[] e = EFFECTS[rand.nextInt(EFFECTS.length)];
            Potion potion = (Potion) e[0];
            int min = (Integer) e[1];
            int max = (Integer) e[2];
            int duration = min + rand.nextInt(max - min + 1);
            if (!potion.isInstant()) {
                duration *= 20;
            }
            NBTTagCompound effect = new NBTTagCompound();
            effect.setByte("EffectId", (byte) Potion.getIdFromPotion(potion));
            effect.setInteger("EffectDuration", duration);
            NBTTagList list = new NBTTagList();
            list.appendTag(effect);
            NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
            tag.setTag("Effects", list);
            stack.setTagCompound(tag);
            return stack;
        }
    }
}
