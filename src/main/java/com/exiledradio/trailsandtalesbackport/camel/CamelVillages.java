package com.exiledradio.trailsandtalesbackport.camel;

import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.MapGenStructureData;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Camels in desert villages: the one place this mod generates camels.
 *
 * <h2>What 1.20 does</h2>
 * Each of 1.20's three desert town centers ({@code village/desert/town_centers/desert_meeting_point_1..3})
 * has one jigsaw to the {@code village/desert/camel} pool, whose one element ({@code camel_spawn}) places one
 * camel standing on the floor beside it: so every (non-zombie) desert village has one camel at its center.
 * The jigsaws: meeting point 1 at (8, 0, 4) facing up with its top south, 2 at (6, 0, 1) top west, 3 at
 * (7, 0, 8) top north; the camel piece turns to match the top, and its camel stands at its block's corner as
 * the turn moves it (north 0,0; east 1,0; south 1,1; west 0,1), facing the turn.
 *
 * <h2>Here</h2>
 * Villages are Village &amp; Pillage's. Its 1.14 desert town centers are the same three templates without the
 * camel jigsaw, so the camel is put where 1.20 puts it. Nothing of that mod is linked against: its villages are
 * read from the world's saved structure data ({@code Village}, or {@code OTGVillage} with Open Terrain
 * Generator) - its pieces save their pool, element index, position and rotation - and the element's template
 * name from its own {@code pools.json}, found by resource name. Anything missing or different is skipped.
 *
 * <p>Without it, vanilla 1.12.2's desert villages (start piece {@code ViStart}, type 1, not zombie) get one
 * camel at a corner of their well, the first corner where it fits.
 *
 * <p>Each camel is spawned once, when the chunk whose population area holds its spot is populated (after the
 * village pieces there are placed), with no random draws, so the same seed gives the same camels.
 * {@link #spawnInChunk} is the entry point for any other terrain generator that populates chunks its own way.
 */
public final class CamelVillages {

    static final String VP_START = "VBP_ModernVillage";
    static final String VP_PIECE = "VBP_ModernPiece";
    static final String VP_POOLS = "/assets/villagerbackport/structures/village/pools.json";
    static final String TOWN_CENTERS = "village/desert/town_centers";
    private static final String[] STRUCTURE_NAMES = {"Village", "OTGVillage"};

    /** 1.20's camel jigsaw in each desert meeting point: position and the direction of its top. */
    private static final Map<String, Object[]> JIGSAWS = new HashMap<String, Object[]>();

    static {
        JIGSAWS.put("village/desert/town_centers/desert_meeting_point_1", new Object[] {new BlockPos(8, 0, 4), EnumFacing.SOUTH});
        JIGSAWS.put("village/desert/town_centers/desert_meeting_point_2", new Object[] {new BlockPos(6, 0, 1), EnumFacing.WEST});
        JIGSAWS.put("village/desert/town_centers/desert_meeting_point_3", new Object[] {new BlockPos(7, 0, 8), EnumFacing.NORTH});
    }

    /** Village & Pillage's town center templates by element index; empty when it is absent. */
    @Nullable
    private static List<String> townCenters;

    @SubscribeEvent
    public void populate(PopulateChunkEvent.Post event) {
        if (!event.getWorld().isRemote && CamelContent.enabled()) {
            spawnInChunk(event.getWorld(), event.getChunkX(), event.getChunkZ());
        }
    }

    /** A camel to place: where, and which way it faces. */
    public static final class Spot {
        public final Vec3d pos;
        public final float yaw;

        Spot(Vec3d pos, float yaw) {
            this.pos = pos;
            this.yaw = yaw;
        }
    }

    /** Spawns the camels of every desert village whose camel stands in this chunk's population area. */
    public static void spawnInChunk(World world, int chunkX, int chunkZ) {
        int x0 = (chunkX << 4) + 8;
        int z0 = (chunkZ << 4) + 8;
        for (Spot spot : spotsNear(world, chunkX, chunkZ)) {
            int x = (int) Math.floor(spot.pos.x);
            int z = (int) Math.floor(spot.pos.z);
            if (x >= x0 && x <= x0 + 15 && z >= z0 && z <= z0 + 15) {
                spawn(world, spot);
            }
        }
    }

    /** @return the camel spots of the desert villages that start within two chunks of this one */
    static List<Spot> spotsNear(World world, int chunkX, int chunkZ) {
        List<Spot> spots = new ArrayList<Spot>();
        for (String name : STRUCTURE_NAMES) {
            MapGenStructureData data;
            try {
                data = (MapGenStructureData) world.getPerWorldStorage().getOrLoadData(MapGenStructureData.class, name);
            } catch (RuntimeException e) {
                continue;
            }
            if (data == null) {
                continue;
            }
            NBTTagCompound features = data.getTagCompound();
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    String key = MapGenStructureData.formatChunkCoords(chunkX + dx, chunkZ + dz);
                    if (features.hasKey(key, Constants.NBT.TAG_COMPOUND)) {
                        Spot spot = spotOf(world, features.getCompoundTag(key));
                        if (spot != null) {
                            spots.add(spot);
                        }
                    }
                }
            }
        }
        return spots;
    }

    /** @return the camel spot of a saved village start, or null if it is not a (live) desert village */
    @Nullable
    static Spot spotOf(World world, NBTTagCompound start) {
        String id = start.getString("id");
        NBTTagList children = start.getTagList("Children", Constants.NBT.TAG_COMPOUND);
        if (VP_START.equals(id)) {
            for (int i = 0; i < children.tagCount(); i++) {
                NBTTagCompound piece = children.getCompoundTagAt(i);
                if (VP_PIECE.equals(piece.getString("id")) && TOWN_CENTERS.equals(piece.getString("Pool"))) {
                    return modernSpot(piece);
                }
            }
            return null;
        }
        if ("Village".equals(id) && start.getBoolean("Valid")) {
            for (int i = 0; i < children.tagCount(); i++) {
                NBTTagCompound piece = children.getCompoundTagAt(i);
                if ("ViStart".equals(piece.getString("id"))) {
                    return piece.getByte("Type") == 1 && !piece.getBoolean("Zombie") ? wellSpot(world, piece) : null;
                }
            }
        }
        return null;
    }

    /** 1.20's camel beside a Village &amp; Pillage desert meeting point. */
    @Nullable
    public static Spot modernSpot(NBTTagCompound piece) {
        List<String> centers = townCenters();
        int element = piece.getInteger("Element");
        if (element < 0 || element >= centers.size()) {
            return null;
        }
        Object[] jigsaw = JIGSAWS.get(centers.get(element));
        if (jigsaw == null) {
            return null;
        }
        Rotation rotation;
        try {
            rotation = Rotation.valueOf(piece.getString("Rotation"));
        } catch (IllegalArgumentException e) {
            return null;
        }
        BlockPos origin = new BlockPos(piece.getInteger("PosX"), piece.getInteger("PosY"), piece.getInteger("PosZ"));
        BlockPos rel = transform((BlockPos) jigsaw[0], rotation);
        BlockPos camelPiece = origin.add(rel).up();
        EnumFacing top = rotation.rotate((EnumFacing) jigsaw[1]);
        double x;
        double z;
        switch (top) {
            case EAST:
                x = 1.0D;
                z = 0.0D;
                break;
            case SOUTH:
                x = 1.0D;
                z = 1.0D;
                break;
            case WEST:
                x = 0.0D;
                z = 1.0D;
                break;
            default:
                x = 0.0D;
                z = 0.0D;
        }
        return new Spot(new Vec3d(camelPiece.getX() + x, camelPiece.getY(), camelPiece.getZ() + z), top.getHorizontalAngle() - 180.0F);
    }

    /** 1.12.2's template rotation of a block offset (about the origin), as Village &amp; Pillage turns its pieces. */
    static BlockPos transform(BlockPos pos, Rotation rotation) {
        switch (rotation) {
            case CLOCKWISE_90:
                return new BlockPos(-pos.getZ(), pos.getY(), pos.getX());
            case CLOCKWISE_180:
                return new BlockPos(-pos.getX(), pos.getY(), -pos.getZ());
            case COUNTERCLOCKWISE_90:
                return new BlockPos(pos.getZ(), pos.getY(), -pos.getX());
            default:
                return pos;
        }
    }

    /**
     * A vanilla 1.12.2 desert village: one camel at the first corner of its well (north-west, north-east,
     * south-east, south-west) where it fits, standing on the ground there; null until the well is placed.
     */
    @Nullable
    static Spot wellSpot(World world, NBTTagCompound piece) {
        if (piece.getInteger("HPos") < 0) {
            return null;
        }
        int[] bb = piece.getIntArray("BB");
        if (bb.length != 6) {
            return null;
        }
        StructureBoundingBox box = new StructureBoundingBox(bb);
        double[][] corners = {{box.minX, box.minZ}, {box.maxX + 1, box.minZ}, {box.maxX + 1, box.maxZ + 1}, {box.minX, box.maxZ + 1}};
        Spot first = null;
        for (double[] c : corners) {
            BlockPos column = new BlockPos(c[0] - 0.5D, 0, c[1] - 0.5D);
            if (!world.isBlockLoaded(column)) {
                continue;
            }
            int y = world.getHeight(column.getX(), column.getZ());
            Spot spot = new Spot(new Vec3d(c[0], y, c[1]), 0.0F);
            if (first == null) {
                first = spot;
            }
            double half = EntityCamel.WIDTH / 2.0D;
            AxisAlignedBB aabb = new AxisAlignedBB(c[0] - half, y, c[1] - half, c[0] + half, y + EntityCamel.HEIGHT, c[1] + half);
            if (!world.collidesWithAnyBlock(aabb)) {
                return spot;
            }
        }
        return first;
    }

    private static void spawn(World world, Spot spot) {
        EntityCamel camel = new EntityCamel(world);
        camel.setLocationAndAngles(spot.pos.x, spot.pos.y, spot.pos.z, spot.yaw, 0.0F);
        camel.rotationYawHead = spot.yaw;
        camel.renderYawOffset = spot.yaw;
        camel.onInitialSpawn(world.getDifficultyForLocation(new BlockPos(camel)), null);
        world.spawnEntity(camel);
    }

    /**
     * Village &amp; Pillage's desert town center templates, by the element index its pieces save: the
     * {@code village/desert/town_centers} pool of its {@code pools.json}, read once by resource name.
     */
    public static synchronized List<String> townCenters() {
        if (townCenters != null) {
            return townCenters;
        }
        List<String> names = new ArrayList<String>();
        try (InputStream in = CamelVillages.class.getResourceAsStream(VP_POOLS)) {
            if (in != null) {
                JsonObject root = new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                JsonObject pool = root.getAsJsonObject(TOWN_CENTERS);
                JsonArray elements = pool == null ? null : pool.getAsJsonArray("elements");
                if (elements != null) {
                    for (JsonElement e : elements) {
                        JsonObject element = e.getAsJsonObject().getAsJsonObject("element");
                        names.add(element != null && element.has("location") ? element.get("location").getAsString() : "");
                    }
                }
            }
        } catch (Exception e) {
            TrailsAndTalesBackport.LOGGER.warn("Could not read Village & Pillage's village pools; its desert villages get no camels.", e);
            names.clear();
        }
        townCenters = names;
        return names;
    }
}
