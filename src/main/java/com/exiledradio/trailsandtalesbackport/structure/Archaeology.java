package com.exiledradio.trailsandtalesbackport.structure;

import com.exiledradio.backportcore.structure.ModernTemplate;
import com.exiledradio.backportcore.structure.TemplatePlacement;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * 1.20's archaeology processors, worked out the way 1.20's {@code StructureTemplate.processBlockInfos} does: every block
 * of the file through each processor's {@code processBlock}, then each processor's {@code finalizeProcessing} over the
 * whole list. Only {@code capped} has a list step: it shuffles the indices of the whole processed list with the random
 * {@code RandomSource.create(seed).forkPositional().at(piece position)} and lets its delegate change up to
 * {@code limit} blocks. So which gravel or sand becomes suspicious depends on the whole file, not on the chunk being
 * built, and is worked out here in full for every chunk (the result is the same each time).
 *
 * <p>Backport Core places the files and their per-block {@code rule} processors (with the same per-position random, so
 * gravel turns to dirt where 1.20 turns it); it skips {@code capped}. {@link #place} then puts the suspicious blocks
 * this works out into the chunk, by name ({@code trailsandtalesbackport:suspicious_gravel}, {@code LootTable} and
 * {@code LootTableSeed} in its tile entity).
 */
public final class Archaeology {

    private Archaeology() {
    }

    /** A processed block: where it goes, today's name and properties, its data. */
    public static final class Info {
        public final BlockPos pos;
        public final String name;
        public final Map<String, String> props;
        @Nullable
        public final NBTTagCompound nbt;

        public Info(BlockPos pos, String name, Map<String, String> props, @Nullable NBTTagCompound nbt) {
            this.pos = pos;
            this.name = name;
            this.props = props;
            this.nbt = nbt;
        }

        boolean same(Info o) {
            return o != null && this.pos.equals(o.pos) && this.name.equals(o.name) && this.props.equals(o.props)
                    && (this.nbt == null ? o.nbt == null : this.nbt.equals(o.nbt));
        }

        @Override
        public String toString() {
            return this.name + this.props + " at " + this.pos + (this.nbt == null ? "" : " " + this.nbt);
        }
    }

    /** One processor of a list. */
    public interface Step {
        /** {@code processBlock}; null drops the block. */
        @Nullable
        Info process(Info original, Info current);

        /** {@code finalizeProcessing}. */
        default List<Info> finish(long worldSeed, BlockPos position, List<Info> originals, List<Info> processed) {
            return processed;
        }
    }

    // ---- the processors

    /** A rule's input test. */
    interface Test {
        boolean test(Info block, Random random);
    }

    static final class Rule {
        final Test input;
        final String output;
        final Map<String, String> outputProps;
        @Nullable
        final String appendLoot;

        Rule(Test input, String output, Map<String, String> outputProps, @Nullable String appendLoot) {
            this.input = input;
            this.output = output;
            this.outputProps = outputProps;
            this.appendLoot = appendLoot;
        }
    }

    /** {@code RuleProcessor}: one random per block from its position; the first rule that matches. */
    public static final class RuleStep implements Step {
        final List<Rule> rules;

        RuleStep(List<Rule> rules) {
            this.rules = rules;
        }

        @Override
        public Info process(Info original, Info current) {
            Random random = VanillaRandom.at(current.pos);
            for (Rule rule : this.rules) {
                // location_predicate and position_predicate: always_true here (checked when read).
                if (rule.input.test(current, random)) {
                    NBTTagCompound nbt = current.nbt;
                    if (rule.appendLoot != null) {
                        // AppendLoot.apply
                        nbt = nbt == null ? new NBTTagCompound() : nbt.copy();
                        nbt.setString("LootTable", rule.appendLoot);
                        nbt.setLong("LootTableSeed", random.nextLong());
                    }
                    return new Info(current.pos, rule.output, rule.outputProps, nbt);
                }
            }
            return current;
        }
    }

    /** {@code CappedProcessor}: at most {@code limit} of the list changed by the delegate, in a shuffled order. */
    public static final class CappedStep implements Step {
        final Step delegate;
        final int min;
        final int max;

        public CappedStep(Step delegate, int min, int max) {
            this.delegate = delegate;
            this.min = min;
            this.max = max;
        }

        @Override
        public Info process(Info original, Info current) {
            return current;
        }

        @Override
        public List<Info> finish(long worldSeed, BlockPos position, List<Info> originals, List<Info> processed) {
            if (this.max == 0 || processed.isEmpty() || originals.size() != processed.size()) {
                return processed;
            }
            Random random = VanillaRandom.positional(worldSeed, position);
            int limit = Math.min(this.min == this.max ? this.min : VanillaRandom.between(random, this.min, this.max), processed.size());
            if (limit < 1) {
                return processed;
            }
            List<Integer> order = new ArrayList<Integer>(processed.size());
            for (int i = 0; i < processed.size(); i++) {
                order.add(i);
            }
            VanillaRandom.shuffle(order, random);
            int changed = 0;
            for (int i : order) {
                if (changed >= limit) {
                    break;
                }
                Info before = processed.get(i);
                Info after = this.delegate.process(originals.get(i), before);
                if (after != null && !before.same(after)) {
                    changed++;
                    processed.set(i, after);
                }
            }
            return processed;
        }
    }

    /** {@code BlockIgnoreProcessor}. */
    public static Step ignore(String... names) {
        Set<String> set = new HashSet<String>();
        for (String n : names) {
            set.add(VanillaTemplate.full(n));
        }
        return (original, current) -> set.contains(current.name) ? null : current;
    }

    /** A rule step of one block-match rule: {@code block} becomes {@code output} with a loot table (1.20's ocean ruins). */
    public static Step blockToLoot(String block, String output, String lootTable) {
        String b = VanillaTemplate.full(block);
        List<Rule> rules = Collections.singletonList(new Rule((info, r) -> info.name.equals(b), VanillaTemplate.full(output),
                Collections.<String, String>emptyMap(), lootTable));
        return new RuleStep(rules);
    }

    // ---- reading processor lists

    private static final Map<ResourceLocation, List<Step>> LISTS = new HashMap<ResourceLocation, List<Step>>();
    private static final Set<String> REPORTED = new HashSet<String>();

    /**
     * A processor list file, {@code assets/<this mod>/worldgen/processor_list/<path>.json} for {@code minecraft:<path>},
     * as steps. Processors with no list step and no effect on what {@code capped} sees are kept as they are (rules);
     * anything else unknown is skipped with a warning.
     */
    public static synchronized List<Step> list(ResourceLocation id) {
        List<Step> known = LISTS.get(id);
        if (known != null) {
            return known;
        }
        List<Step> steps = new ArrayList<Step>();
        JsonElement json = json("/assets/" + TrailsAndTalesBackport.MOD_ID + "/worldgen/processor_list/" + id.getPath() + ".json");
        if (json != null) {
            JsonArray array = json.isJsonArray() ? json.getAsJsonArray() : json.getAsJsonObject().getAsJsonArray("processors");
            for (JsonElement e : array) {
                Step s = step(e.getAsJsonObject());
                if (s != null) {
                    steps.add(s);
                }
            }
        }
        LISTS.put(id, steps);
        return steps;
    }

    /** @return true if the list has a {@code capped} processor, the only one Backport Core leaves to us */
    public static boolean hasListStep(ResourceLocation id) {
        for (Step s : list(id)) {
            if (s instanceof CappedStep) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private static Step step(JsonObject p) {
        String type = VanillaTemplate.full(p.has("processor_type") ? p.get("processor_type").getAsString() : "minecraft:nop");
        switch (type) {
            case "minecraft:rule":
                return rule(p.getAsJsonArray("rules"));
            case "minecraft:capped": {
                Step delegate = step(p.getAsJsonObject("delegate"));
                if (delegate == null) {
                    return null;
                }
                JsonElement limit = p.get("limit");
                int min;
                int max;
                if (limit.isJsonPrimitive()) {
                    min = max = limit.getAsInt();
                } else {
                    JsonObject l = limit.getAsJsonObject();
                    String t = VanillaTemplate.full(l.has("type") ? l.get("type").getAsString() : "minecraft:constant");
                    if ("minecraft:uniform".equals(t)) {
                        JsonObject v = l.has("value") ? l.getAsJsonObject("value") : l;
                        min = v.get("min_inclusive").getAsInt();
                        max = v.get("max_inclusive").getAsInt();
                    } else {
                        min = max = l.get("value").getAsInt();
                    }
                }
                return new CappedStep(delegate, min, max);
            }
            case "minecraft:nop":
                return null;
            default:
                if (REPORTED.add(type)) {
                    TrailsAndTalesBackport.LOGGER.warn("Archaeology: processor {} is not read; it is skipped.", type);
                }
                return null;
        }
    }

    private static Step rule(JsonArray array) {
        List<Rule> rules = new ArrayList<Rule>();
        for (JsonElement e : array) {
            JsonObject r = e.getAsJsonObject();
            if (!alwaysTrue(r.getAsJsonObject("location_predicate")) || (r.has("position_predicate") && !alwaysTrue(r.getAsJsonObject("position_predicate")))) {
                TrailsAndTalesBackport.LOGGER.warn("Archaeology: a rule with a location or position test is not read: {}", r);
                continue;
            }
            JsonObject out = r.getAsJsonObject("output_state");
            Map<String, String> props = new LinkedHashMap<String, String>();
            if (out.has("Properties")) {
                for (Map.Entry<String, JsonElement> p : out.getAsJsonObject("Properties").entrySet()) {
                    props.put(p.getKey(), p.getValue().getAsString());
                }
            }
            String loot = null;
            if (r.has("block_entity_modifier")) {
                JsonObject m = r.getAsJsonObject("block_entity_modifier");
                if ("minecraft:append_loot".equals(VanillaTemplate.full(m.get("type").getAsString()))) {
                    loot = VanillaTemplate.full(m.get("loot_table").getAsString());
                }
            }
            rules.add(new Rule(test(r.getAsJsonObject("input_predicate")), VanillaTemplate.full(out.get("Name").getAsString()),
                    Collections.unmodifiableMap(props), loot));
        }
        return new RuleStep(rules);
    }

    private static boolean alwaysTrue(@Nullable JsonObject p) {
        return p == null || "minecraft:always_true".equals(VanillaTemplate.full(p.get("predicate_type").getAsString()));
    }

    private static Test test(JsonObject p) {
        String type = VanillaTemplate.full(p.get("predicate_type").getAsString());
        switch (type) {
            case "minecraft:always_true":
                return (b, r) -> true;
            case "minecraft:block_match": {
                String block = VanillaTemplate.full(p.get("block").getAsString());
                return (b, r) -> b.name.equals(block);
            }
            case "minecraft:random_block_match": {
                String block = VanillaTemplate.full(p.get("block").getAsString());
                float probability = p.get("probability").getAsFloat();
                return (b, r) -> b.name.equals(block) && r.nextFloat() < probability;
            }
            case "minecraft:tag_match": {
                Set<String> members = tag(VanillaTemplate.full(p.get("tag").getAsString()));
                return (b, r) -> members.contains(b.name);
            }
            default:
                TrailsAndTalesBackport.LOGGER.warn("Archaeology: rule test {} is not read; it never matches.", type);
                return (b, r) -> false;
        }
    }

    /** A block tag, from {@code assets/<this mod>/tags/blocks/<path>.json} (vanilla's file, copied). */
    static Set<String> tag(String id) {
        Set<String> out = new HashSet<String>();
        String path = id.substring(id.indexOf(':') + 1);
        JsonElement json = json("/assets/" + TrailsAndTalesBackport.MOD_ID + "/tags/blocks/" + path + ".json");
        if (json != null) {
            for (JsonElement v : json.getAsJsonObject().getAsJsonArray("values")) {
                String s = v.getAsString();
                if (s.startsWith("#")) {
                    out.addAll(tag(VanillaTemplate.full(s.substring(1))));
                } else {
                    out.add(VanillaTemplate.full(s));
                }
            }
        }
        return out;
    }

    @Nullable
    static JsonElement json(String path) {
        try (InputStream in = Archaeology.class.getResourceAsStream(path)) {
            if (in == null) {
                TrailsAndTalesBackport.LOGGER.warn("Archaeology: {} is missing.", path);
                return null;
            }
            return new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (Exception e) {
            TrailsAndTalesBackport.LOGGER.error("Archaeology: could not read {}.", path, e);
            return null;
        }
    }

    // ---- working it out

    /**
     * {@code StructureTemplate.processBlockInfos} for one placed file.
     *
     * @param before the steps a pool element or piece puts ahead of the list (structure block ignore, jigsaw
     *               replacement, integrity); applied in order
     * @param steps  the processor list
     * @return the processed blocks, in the file's order
     */
    public static List<Info> process(VanillaTemplate template, BlockPos position, TemplatePlacement placement, long worldSeed,
                                     List<Step> before, List<Step> steps) {
        List<Info> originals = new ArrayList<Info>();
        List<Info> processed = new ArrayList<Info>();
        for (VanillaTemplate.Block b : template.blocks) {
            BlockPos at = ModernTemplate.worldPos(position, b.local, placement);
            Info original = new Info(b.local, b.name, b.props, b.nbt);
            Info info = new Info(at, b.name, b.props, b.nbt == null ? null : b.nbt.copy());
            for (Step s : before) {
                info = s.process(original, info);
                if (info == null) {
                    break;
                }
            }
            if (info != null) {
                for (Step s : steps) {
                    info = s.process(original, info);
                    if (info == null) {
                        break;
                    }
                }
            }
            if (info != null) {
                processed.add(info);
                originals.add(original);
            }
        }
        for (Step s : before) {
            processed = s.finish(worldSeed, position, originals, processed);
        }
        for (Step s : steps) {
            processed = s.finish(worldSeed, position, originals, processed);
        }
        return processed;
    }

    /** The suspicious blocks among the processed ones. */
    public static List<Info> suspicious(List<Info> processed) {
        List<Info> out = new ArrayList<Info>();
        for (Info i : processed) {
            if ((i.name.equals("minecraft:suspicious_gravel") || i.name.equals("minecraft:suspicious_sand")) && i.nbt != null) {
                out.add(i);
            }
        }
        return out;
    }

    /** {@code SinglePoolElement}'s own processors ahead of its list: structure blocks left out, jigsaws to their final state. */
    public static final Step STRUCTURE_BLOCK = ignore("minecraft:structure_block");

    public static final Step JIGSAW_REPLACEMENT = (original, current) -> {
        if (!"minecraft:jigsaw".equals(current.name)) {
            return current;
        }
        String finalState = current.nbt != null && current.nbt.hasKey("final_state") ? current.nbt.getString("final_state") : "minecraft:air";
        Map<String, String> props = new LinkedHashMap<String, String>();
        String name = finalState;
        int open = finalState.indexOf('[');
        if (open >= 0) {
            name = finalState.substring(0, open);
            for (String kv : finalState.substring(open + 1, finalState.length() - 1).split(",")) {
                int eq = kv.indexOf('=');
                if (eq > 0) {
                    props.put(kv.substring(0, eq).trim(), kv.substring(eq + 1).trim());
                }
            }
        }
        name = VanillaTemplate.full(name.trim());
        if ("minecraft:structure_void".equals(name)) {
            return null;
        }
        return new Info(current.pos, name, props, null);
    };

    // ---- placing

    private static Block suspiciousSand;
    private static Block suspiciousGravel;
    private static boolean looked;

    /** Trails &amp; Tales' blocks, by name; null while archaeology is off. */
    @Nullable
    static synchronized Block block(boolean gravel) {
        if (!looked) {
            looked = true;
            suspiciousSand = byName(TrailsAndTalesBackport.MOD_ID + ":suspicious_sand");
            suspiciousGravel = byName(TrailsAndTalesBackport.MOD_ID + ":suspicious_gravel");
        }
        return gravel ? suspiciousGravel : suspiciousSand;
    }

    @Nullable
    private static Block byName(String name) {
        Block b = Block.getBlockFromName(name);
        return b == null || b == Blocks.AIR ? null : b;
    }

    /** 1.20's archaeology tables, as this mod names them. */
    public static ResourceLocation table(String vanilla) {
        ResourceLocation v = new ResourceLocation(vanilla);
        return "minecraft".equals(v.getNamespace()) && v.getPath().startsWith("archaeology/")
                ? new ResourceLocation(TrailsAndTalesBackport.MOD_ID, v.getPath()) : v;
    }

    /**
     * Sets one suspicious block, as 1.20's template sets it: the block (flags 2, no neighbor updates) and its
     * {@code LootTable} and {@code LootTableSeed}.
     *
     * @return true if it was set
     */
    public static boolean set(World world, BlockPos pos, boolean gravel, ResourceLocation table, long seed) {
        Block block = block(gravel);
        if (block == null) {
            return false;
        }
        if (!world.setBlockState(pos, block.getDefaultState(), 2)) {
            return world.getBlockState(pos).getBlock() == block && loot(world, pos, table, seed);
        }
        return loot(world, pos, table, seed);
    }

    /**
     * 1.20's world generation places blocks without {@code onPlace}, so a suspicious block it puts over air gets no
     * tick and stays until a neighbor changes. 1.12.2's {@code onBlockAdded} schedules the block's fall check; this
     * takes that one tick back out. {@code getPendingBlockUpdates} hands over the whole column's ticks, so every other
     * one is put back as it was.
     */
    static void unschedule(World world, BlockPos pos) {
        if (!(world instanceof net.minecraft.world.WorldServer)) {
            return;
        }
        net.minecraft.world.WorldServer server = (net.minecraft.world.WorldServer) world;
        List<net.minecraft.world.NextTickListEntry> taken = server.getPendingBlockUpdates(
                new StructureBoundingBox(pos.getX(), 0, pos.getZ(), pos.getX() + 1, 255, pos.getZ() + 1), true);
        if (taken == null) {
            return;
        }
        long now = server.getTotalWorldTime();
        for (net.minecraft.world.NextTickListEntry e : taken) {
            if (e.position.equals(pos)) {
                continue;
            }
            server.scheduleBlockUpdate(e.position, e.getBlock(), (int) Math.max(0L, e.scheduledTime - now), e.priority);
        }
    }

    /**
     * 1.20's world generation schedules no tick for the blocks a structure places, so its sand and gravel over air or
     * water stay put until the world runs and a neighbor changes. 1.12.2's {@code onBlockAdded} schedules the fall of
     * every falling block a structure sets outside the generator's own decoration (Backport Core's scattered driver and
     * this mod's generators run as world generators), and a plain gravel block that falls takes the suspicious one beside
     * it along. This takes back the pending ticks of the falling blocks inside {@code box} (both corners included); every
     * other tick of those columns is put back as it was.
     *
     * @param box a piece's box clipped to the populate step's box
     */
    public static void unscheduleFalling(World world, @Nullable StructureBoundingBox box) {
        if (box == null || !(world instanceof net.minecraft.world.WorldServer)) {
            return;
        }
        net.minecraft.world.WorldServer server = (net.minecraft.world.WorldServer) world;
        // getPendingBlockUpdates takes whole columns, its max x and z exclusive.
        List<net.minecraft.world.NextTickListEntry> taken = server.getPendingBlockUpdates(
                new StructureBoundingBox(box.minX, 0, box.minZ, box.maxX + 1, 255, box.maxZ + 1), true);
        if (taken == null) {
            return;
        }
        long now = server.getTotalWorldTime();
        for (net.minecraft.world.NextTickListEntry e : taken) {
            if (box.isVecInside(e.position) && e.getBlock() instanceof net.minecraft.block.BlockFalling
                    && server.getBlockState(e.position).getBlock() == e.getBlock()) {
                continue;
            }
            server.scheduleBlockUpdate(e.position, e.getBlock(), (int) Math.max(0L, e.scheduledTime - now), e.priority);
        }
    }

    /** @return the part of {@code a} inside {@code b}, or null when they do not meet */
    @Nullable
    public static StructureBoundingBox clip(StructureBoundingBox a, StructureBoundingBox b) {
        if (!a.intersectsWith(b)) {
            return null;
        }
        return new StructureBoundingBox(Math.max(a.minX, b.minX), Math.max(a.minY, b.minY), Math.max(a.minZ, b.minZ),
                Math.min(a.maxX, b.maxX), Math.min(a.maxY, b.maxY), Math.min(a.maxZ, b.maxZ));
    }

    private static boolean loot(World world, BlockPos pos, ResourceLocation table, long seed) {
        unschedule(world, pos);
        TileEntity te = world.getTileEntity(pos);
        if (te == null) {
            return false;
        }
        NBTTagCompound tag = te.writeToNBT(new NBTTagCompound());
        tag.setString("LootTable", table.toString());
        tag.setLong("LootTableSeed", seed);
        te.readFromNBT(tag);
        te.markDirty();
        return true;
    }

    /**
     * Puts the suspicious blocks of a processed file that fall inside {@code clip} into the world.
     *
     * @return how many were set
     */
    public static int place(World world, List<Info> processed, StructureBoundingBox clip) {
        int n = 0;
        for (Info i : suspicious(processed)) {
            if (clip.isVecInside(i.pos) && set(world, i.pos, i.name.equals("minecraft:suspicious_gravel"),
                    table(i.nbt.getString("LootTable")), i.nbt.getLong("LootTableSeed"))) {
                n++;
            }
        }
        return n;
    }
}
