package com.exiledradio.trailsandtalesbackport.structure;

import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A structure file's blocks in the order 1.20's {@code StructureTemplate} keeps them ({@code loadPalette} and
 * {@code buildInfoList}): the full blocks, then the other blocks without data, then the blocks with data, each sorted by
 * y, then x, then z. The order is what a {@code capped} processor's shuffle picks from, so this is read from the file
 * itself (Backport Core's {@code ModernTemplate} keeps jigsaws and structure blocks apart and does not need the order).
 *
 * <p>"Full" is 1.20's {@code !hasDynamicShape() && isCollisionShapeFullBlock}: {@link #full} answers it by today's
 * block name and properties for every block the archaeology structures' files have, and by name pattern for the rest.
 */
public final class VanillaTemplate {

    /** One block of the file: its position in the file, today's name and properties, and its data, if any. */
    public static final class Block {
        public final BlockPos local;
        public final String name;
        public final Map<String, String> props;
        @Nullable
        public final NBTTagCompound nbt;

        Block(BlockPos local, String name, Map<String, String> props, @Nullable NBTTagCompound nbt) {
            this.local = local;
            this.name = name;
            this.props = props;
            this.nbt = nbt;
        }
    }

    private static final Map<ResourceLocation, VanillaTemplate> CACHE = new HashMap<ResourceLocation, VanillaTemplate>();

    public final ResourceLocation id;
    /** Palette 0's blocks in 1.20's order (the archaeology structures' files have one palette). */
    public final List<Block> blocks;

    private VanillaTemplate(ResourceLocation id, List<Block> blocks) {
        this.id = id;
        this.blocks = blocks;
    }

    /**
     * @param id the file's id as {@code ModernTemplate.load} takes it ({@code assets/<ns>/structures/<path>.nbt}, or
     *           {@code structure/})
     * @return the template; an empty one, logged, if the file is missing
     */
    public static synchronized VanillaTemplate load(ResourceLocation id) {
        VanillaTemplate known = CACHE.get(id);
        if (known != null) {
            return known;
        }
        List<Block> blocks = new ArrayList<Block>();
        NBTTagCompound tag = read(id);
        if (tag == null) {
            TrailsAndTalesBackport.LOGGER.warn("Structure file {} is missing; its archaeology is left out.", id);
        } else {
            NBTTagList palette = tag.hasKey("palette", Constants.NBT.TAG_LIST) ? tag.getTagList("palette", Constants.NBT.TAG_COMPOUND)
                    : tag.getTagList("palettes", Constants.NBT.TAG_LIST).tagCount() > 0
                    ? (NBTTagList) tag.getTagList("palettes", Constants.NBT.TAG_LIST).get(0) : new NBTTagList();
            String[] names = new String[palette.tagCount()];
            List<Map<String, String>> props = new ArrayList<Map<String, String>>();
            for (int i = 0; i < palette.tagCount(); i++) {
                NBTTagCompound p = palette.getCompoundTagAt(i);
                names[i] = full(p.hasKey("Name") ? p.getString("Name") : p.getString("id"));
                NBTTagCompound pr = p.hasKey("Properties") ? p.getCompoundTag("Properties") : p.getCompoundTag("properties");
                Map<String, String> m = new LinkedHashMap<String, String>();
                for (String k : pr.getKeySet()) {
                    m.put(k, pr.getString(k));
                }
                props.add(Collections.unmodifiableMap(m));
            }
            List<Block> fullBlocks = new ArrayList<Block>();
            List<Block> withData = new ArrayList<Block>();
            List<Block> others = new ArrayList<Block>();
            NBTTagList list = tag.getTagList("blocks", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound b = list.getCompoundTagAt(i);
                NBTTagList p = b.getTagList("pos", Constants.NBT.TAG_INT);
                int state = b.getInteger("state");
                if (state < 0 || state >= names.length) {
                    continue;
                }
                NBTTagCompound nbt = b.hasKey("nbt", Constants.NBT.TAG_COMPOUND) ? b.getCompoundTag("nbt") : null;
                Block block = new Block(new BlockPos(p.getIntAt(0), p.getIntAt(1), p.getIntAt(2)), names[state], props.get(state), nbt);
                // StructureTemplate.addToLists
                if (nbt != null) {
                    withData.add(block);
                } else if (full(block.name, block.props)) {
                    fullBlocks.add(block);
                } else {
                    others.add(block);
                }
            }
            Comparator<Block> order = Comparator.<Block>comparingInt(b -> b.local.getY()).thenComparingInt(b -> b.local.getX())
                    .thenComparingInt(b -> b.local.getZ());
            fullBlocks.sort(order);
            withData.sort(order);
            others.sort(order);
            // buildInfoList: full blocks, then the others, then the ones with data.
            blocks.addAll(fullBlocks);
            blocks.addAll(others);
            blocks.addAll(withData);
        }
        VanillaTemplate t = new VanillaTemplate(id, Collections.unmodifiableList(blocks));
        CACHE.put(id, t);
        return t;
    }

    @Nullable
    private static NBTTagCompound read(ResourceLocation id) {
        for (String folder : new String[] {"structures", "structure"}) {
            String path = "/assets/" + id.getNamespace() + "/" + folder + "/" + id.getPath() + ".nbt";
            try (InputStream in = VanillaTemplate.class.getResourceAsStream(path)) {
                if (in != null) {
                    return CompressedStreamTools.readCompressed(in);
                }
            } catch (Exception e) {
                TrailsAndTalesBackport.LOGGER.error("Could not read structure file {}.", path, e);
                return null;
            }
        }
        return null;
    }

    static String full(String name) {
        return name.indexOf(':') < 0 ? "minecraft:" + name : name;
    }

    // ---- 1.20's "full block" test

    /** Blocks whose collision shape is not a full cube (or that have a dynamic shape), by today's name. */
    private static final Set<String> NOT_FULL = new HashSet<String>(Arrays.asList(
            "minecraft:air", "minecraft:cave_air", "minecraft:void_air", "minecraft:water", "minecraft:lava",
            "minecraft:ladder", "minecraft:campfire", "minecraft:soul_campfire", "minecraft:cauldron", "minecraft:water_cauldron",
            "minecraft:lava_cauldron", "minecraft:powder_snow_cauldron", "minecraft:composter", "minecraft:anvil",
            "minecraft:chipped_anvil", "minecraft:damaged_anvil", "minecraft:grindstone", "minecraft:mud", "minecraft:soul_sand",
            "minecraft:iron_bars", "minecraft:iron_chain", "minecraft:chain", "minecraft:lantern", "minecraft:soul_lantern",
            "minecraft:torch", "minecraft:wall_torch", "minecraft:soul_torch", "minecraft:soul_wall_torch", "minecraft:flower_pot",
            "minecraft:decorated_pot", "minecraft:snow", "minecraft:farmland", "minecraft:dirt_path", "minecraft:scaffolding",
            "minecraft:moving_piston", "minecraft:piston_head", "minecraft:sea_pickle", "minecraft:seagrass", "minecraft:tall_seagrass",
            "minecraft:kelp", "minecraft:kelp_plant", "minecraft:cobweb", "minecraft:chest", "minecraft:trapped_chest",
            "minecraft:ender_chest", "minecraft:enchanting_table", "minecraft:brewing_stand", "minecraft:hopper", "minecraft:lectern",
            "minecraft:stonecutter", "minecraft:bell", "minecraft:end_portal_frame", "minecraft:daylight_detector", "minecraft:cake",
            "minecraft:honey_block", "minecraft:structure_void", "minecraft:bamboo", "minecraft:pointed_dripstone",
            "minecraft:sniffer_egg", "minecraft:turtle_egg", "minecraft:conduit", "minecraft:rail", "minecraft:redstone_wire",
            "minecraft:lever", "minecraft:tripwire", "minecraft:tripwire_hook", "minecraft:glass_pane", "minecraft:vine",
            "minecraft:short_grass", "minecraft:grass", "minecraft:tall_grass", "minecraft:fern", "minecraft:large_fern",
            "minecraft:dead_bush", "minecraft:bubble_column", "minecraft:fire", "minecraft:soul_fire", "minecraft:barrier"));

    /** Name endings of block families whose collision is never (or not always) a full cube. */
    private static final String[] NOT_FULL_SUFFIXES = {"_stairs", "_wall", "_fence", "_fence_gate", "_pane", "_carpet", "_button",
            "_pressure_plate", "_trapdoor", "_door", "_sign", "_banner", "_bed", "_candle", "candle", "_head", "_skull", "_sapling",
            "_flower", "_tulip", "_mushroom", "_coral", "_coral_fan", "_shulker_box", "shulker_box", "_rail", "_cake", "_bars",
            "_lantern", "_campfire"};

    /** 1.20's {@code !state.getBlock().hasDynamicShape() && state.isCollisionShapeFullBlock(...)}. */
    public static boolean full(String name, Map<String, String> props) {
        String n = full(name);
        if (n.endsWith("_slab")) {
            return "double".equals(props.get("type"));
        }
        if (n.equals("minecraft:snow")) {
            return "8".equals(props.get("layers"));
        }
        if (NOT_FULL.contains(n) || n.startsWith("minecraft:potted_")) {
            return false;
        }
        for (String s : NOT_FULL_SUFFIXES) {
            if (n.endsWith(s)) {
                return false;
            }
        }
        return true;
    }
}
