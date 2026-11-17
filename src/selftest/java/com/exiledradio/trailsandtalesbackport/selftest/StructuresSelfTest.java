package com.exiledradio.trailsandtalesbackport.selftest;

import com.exiledradio.backportcore.locate.LocateChecks;
import com.exiledradio.backportcore.locate.StructureLocate;
import com.exiledradio.backportcore.structure.JigsawElement;
import com.exiledradio.backportcore.structure.JigsawPools;
import com.exiledradio.backportcore.structure.ModernTemplate;
import com.exiledradio.backportcore.structure.ScatteredStructure;
import com.exiledradio.backportcore.structure.StructureRandoms;
import com.exiledradio.backportcore.structure.Structures;
import com.exiledradio.backportcore.structure.TerrainAdaptation;
import com.exiledradio.trailsandtalesbackport.structure.Archaeology;
import com.exiledradio.trailsandtalesbackport.structure.DesertPyramids;
import com.exiledradio.trailsandtalesbackport.structure.DesertWells;
import com.exiledradio.trailsandtalesbackport.structure.StructuresContent;
import com.exiledradio.trailsandtalesbackport.structure.TrailLayout;
import com.exiledradio.trailsandtalesbackport.structure.TrailRuins;
import com.exiledradio.trailsandtalesbackport.structure.TrailRuinsPiece;
import com.exiledradio.trailsandtalesbackport.structure.TrailRuinsStructure;
import com.exiledradio.trailsandtalesbackport.structure.VanillaRandom;
import com.exiledradio.trailsandtalesbackport.structure.VanillaTemplate;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.ComponentScatteredFeaturePieces;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;

/**
 * Stream St: trail ruins, desert pyramid cellars, desert wells.
 *
 * <ul>
 *   <li>The settings read from 1.20.1's files, the pools, templates and processor lists load, every block resolves.</li>
 *   <li>Against vanilla 1.20.1 ({@code tools/structures/oracle120/<seed>.json}, a real server's saved starts and
 *       brushable block entities): every valid trail ruins start laid out piece for piece (element, position,
 *       rotation, box, junctions), from vanilla's start height; every suspicious gravel block (position, table,
 *       seed) worked out from the pieces; every desert pyramid's suspicious sand from its box and facing.</li>
 *   <li>This world: {@code /locate structure minecraft:trail_ruins} and Core's ring order, the ruins built, their
 *       suspicious gravel in the world with this mod's tables; a desert pyramid's cellar and sand; a well.</li>
 * </ul>
 */
public final class StructuresSelfTest implements SelfTest {

    @Override
    public String name() {
        return "structures";
    }

    @Override
    public void run(Check c) throws Exception {
        settings(c);
        randoms(c);
        oracle(c);
        world(c);
        pyramid(c);
        well(c);
        generationTicks(c);
        relicJukebox(c);
        if (net.minecraftforge.fml.common.Loader.isModLoaded("updateaquaticbackport")) {
            oceanRuins(c);
        }
        fallbackLoot(c);
    }

    /** Decided 3: the ocean-ruin-only items in the closest table here exactly when Update Aquatic is absent. */
    private void fallbackLoot(Check c) {
        boolean ua = net.minecraftforge.fml.common.Loader.isModLoaded("updateaquaticbackport");
        net.minecraft.world.storage.loot.LootTableManager tables = c.world.getLootTableManager();
        net.minecraft.world.storage.loot.LootPool pyramid = tables.getLootTableFromLocation(
                new ResourceLocation("trailsandtalesbackport", "archaeology/desert_pyramid")).getPool("main");
        net.minecraft.world.storage.loot.LootPool trail = tables.getLootTableFromLocation(
                new ResourceLocation("trailsandtalesbackport", "archaeology/trail_ruins_common")).getPool("main");
        for (String s : new String[]{"angler", "shelter", "snort"}) {
            c.eq(!ua, pyramid != null && pyramid.getEntry("fallback_" + s + "_pottery_sherd") != null,
                    s + " sherd in the desert pyramid's table " + (ua ? "not added (Update Aquatic has warm ruins)" : "as fallback"));
        }
        boolean egg = Block.getBlockFromName("trailsandtalesbackport:sniffer_egg") != null
                && Block.getBlockFromName("trailsandtalesbackport:sniffer_egg") != Blocks.AIR;
        c.eq(!ua && egg, pyramid != null && pyramid.getEntry("fallback_sniffer_egg") != null, "sniffer egg fallback");
        for (String s : new String[]{"blade", "explorer", "mourner", "plenty"}) {
            c.eq(!ua, trail != null && trail.getEntry("fallback_" + s + "_pottery_sherd") != null,
                    s + " sherd in common trail ruins' table " + (ua ? "not added" : "as fallback"));
        }
    }

    /** Update Aquatic's ocean ruins near spawn, built, with 1.20's suspicious sand and gravel where worked out. */
    private void oceanRuins(Check c) {
        WorldServer world = c.world;
        ScatteredStructure ruins = ScatteredStructure.of(world, Structures.OCEAN_RUIN);
        if (!c.ok(ruins != null, "Update Aquatic's ocean ruins are Core's ocean_ruin kind")) {
            return;
        }
        int warm = 0, cold = 0, starts = 0, checked = 0, placed = 0, worked = 0, fell = 0;
        List<BlockPos> inWorld = new ArrayList<BlockPos>();
        List<StructureBoundingBox> built = new ArrayList<StructureBoundingBox>();
        BlockPos from = BlockPos.ORIGIN;
        for (int n = 0; n < 6; n++) {
            BlockPos at = ruins.nearest(world, from, 100);
            if (at == null) {
                break;
            }
            from = at.add(500, 0, 300);
            int cx = at.getX() >> 4, cz = at.getZ() >> 4;
            LocateChecks.generateAround(world, cx, cz, 3);
            // nearest gives the start chunk's middle for a start not laid out yet, its box's middle for one that is.
            StructureStart start = LocateChecks.savedStart(ruins, cx, cz);
            if (start == null) {
                for (StructureStart st : DesertPyramids.starts(ruins)) {
                    StructureBoundingBox bb = st.getBoundingBox();
                    if (bb != null && bb.minX <= at.getX() && at.getX() <= bb.maxX && bb.minZ <= at.getZ() && at.getZ() <= bb.maxZ) {
                        start = st;
                    }
                }
            }
            if (start == null || !start.isSizeableStructure()) {
                continue;
            }
            starts++;
            StructureBoundingBox b = start.getBoundingBox();
            built.add(b);
            for (int x = (b.minX >> 4) - 1; x <= (b.maxX >> 4) + 2; x++) {
                for (int z = (b.minZ >> 4) - 1; z <= (b.maxZ >> 4) + 2; z++) {
                    world.getChunk(x, z);
                }
            }
            for (StructureComponent piece : start.getComponents()) {
                for (com.exiledradio.trailsandtalesbackport.structure.Archaeology.Info i
                        : com.exiledradio.trailsandtalesbackport.structure.OceanRuins.suspicious(world.getSeed(), piece)) {
                    worked++;
                    checked++;
                    TileEntity te = world.getTileEntity(i.pos);
                    if (te == null && overNothing(world, i.pos)) {
                        fell++;
                    }
                    if (te != null) {
                        String table = te.writeToNBT(new NBTTagCompound()).getString("LootTable");
                        if (table.endsWith("ocean_ruin_warm") || table.endsWith("ocean_ruin_cold")) {
                            placed++;
                            inWorld.add(i.pos);
                            warm += table.endsWith("warm") ? 1 : 0;
                            cold += table.endsWith("cold") ? 1 : 0;
                        }
                    }
                }
            }
        }
        com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport.LOGGER.info("SELFTEST structures: ocean ruins {} built, {} suspicious worked out, {} in the world ({} warm, {} cold), {} gone over water or air", starts, worked, placed, warm, cold, fell);
        c.ok(starts > 0, starts + " ocean ruins built");
        c.ok(worked > 0, worked + " suspicious blocks worked out (at most 5 a piece)");
        // A worked-out block is set only where the ruin really put sand or gravel; nearly all are.
        c.ok(placed > 0 && (placed + fell) * 10 >= checked * 8, placed + " (+" + fell + " fallen) of " + checked + " in the world (" + warm + " warm, " + cold + " cold)");
        c.eq(0, fell, "no ocean ruin suspicious block fell during generation (1.20's world generation sends no updates)");
        for (StructureBoundingBox bb : built) {
            populateAround(world, bb, 3);
        }
        stays(c, "ocean ruins", inWorld);
    }

    // ---- data

    private void settings(Check c) {
        TrailRuins s = TrailRuins.get();
        c.eq(34, s.grid.spacing(), "trail ruins spacing");
        c.eq(8, s.grid.separation(), "trail ruins separation");
        c.eq(83469867, s.grid.salt(), "trail ruins salt");
        c.ok(s.linear, "trail ruins spread linearly");
        c.eq(7, s.maxDepth, "size 7");
        c.eq(-15, s.startHeight, "start height y -15");
        c.ok(s.projectToSurface, "start projected to WORLD_SURFACE_WG");
        c.eq(80, s.maxDistance, "max distance 80");
        c.eq("bury", s.terrainAdaptation, "terrain adaptation bury");
        c.eq(6, s.biomes.size(), "six biomes: " + s.biomes);
        c.ok(s.biomes.contains("minecraft:old_growth_birch_forest") && s.biomes.contains("minecraft:jungle")
                && s.biomes.contains("minecraft:snowy_taiga"), "the biome tag");
        c.eq("trail_ruins/tower", s.startPool.getPath(), "start pool");

        String[] pools = {"tower", "tower/tower_top", "tower/additions", "roads", "buildings", "buildings/grouped", "decor"};
        int templates = 0;
        java.util.Set<String> unresolved = new TreeSet<String>();
        for (String p : pools) {
            JigsawPools.Pool pool = JigsawPools.pool(new ResourceLocation("minecraft", "trail_ruins/" + p));
            c.ok(pool != null && pool.size() > 0, "pool trail_ruins/" + p + " loads (" + (pool == null ? 0 : pool.size()) + ")");
            if (pool == null) {
                continue;
            }
            for (JigsawElement e : pool.elements()) {
                if (e.template() == null) {
                    continue;
                }
                ModernTemplate t = ModernTemplate.load(JigsawPools.templateId(e.template()));
                templates += t.isEmpty() ? 0 : 1;
                c.ok(!t.isEmpty(), "template " + e.template() + " loads");
                unresolved.addAll(t.unresolvedBlocks());
                c.ok(!VanillaTemplate.load(JigsawPools.templateId(e.template())).blocks.isEmpty(), "template " + e.template() + " reads in 1.20's order");
            }
        }
        c.ok(templates >= 70, templates + " templates load");
        c.ok(unresolved.isEmpty(), "every block of the ruins resolves: " + unresolved);

        List<Archaeology.Step> houses = Archaeology.list(new ResourceLocation("minecraft", "trail_ruins_houses_archaeology"));
        c.eq(3, houses.size(), "houses archaeology: a rule and two capped");
        c.ok(houses.size() == 3 && houses.get(1) instanceof Archaeology.CappedStep && houses.get(2) instanceof Archaeology.CappedStep, "two capped steps");
        c.eq(2, Archaeology.list(new ResourceLocation("minecraft", "trail_ruins_roads_archaeology")).size(), "roads archaeology");
        c.eq(1, Archaeology.list(new ResourceLocation("minecraft", "trail_ruins_tower_top_archaeology")).size(), "tower top archaeology");
        c.ok(Archaeology.hasListStep(new ResourceLocation("minecraft", "trail_ruins_houses_archaeology")), "capped is ours to do");
        c.eq("trailsandtalesbackport:archaeology/trail_ruins_rare", Archaeology.table("minecraft:archaeology/trail_ruins_rare").toString(), "table rename");

        boolean registered = false;
        for (TerrainAdaptation.Entry e : TerrainAdaptation.registered()) {
            registered |= e.kind.equals(Structures.TRAIL_RUINS) && e.adaptation == TerrainAdaptation.Kind.BURY;
        }
        c.ok(registered, "trail ruins registered with Core's terrain adaptation as bury");
    }

    private void randoms(Check c) {
        c.eq(-33674130277896L, VanillaRandom.seed(1, 2, 3), "Mth.getSeed(1, 2, 3)");
        c.eq(65333762479374L, VanillaRandom.seed(-1000, 64, 12345), "Mth.getSeed(-1000, 64, 12345)");
        c.eq(-2858836897979L, VanillaRandom.seed(123456, -20, -98765), "Mth.getSeed(123456, -20, -98765)");
        c.eq(-274877856378816L, VanillaRandom.asLong(new BlockPos(-1000, 64, 12345)), "1.20's BlockPos.asLong");
        c.eq(1649267417085L, VanillaRandom.asLong(new BlockPos(5, -3, -7)), "1.20's BlockPos.asLong, negative");
    }

    // ---- vanilla 1.20.1

    static final class Oracle {
        long seed;
        final List<JsonObject> trail = new ArrayList<JsonObject>();
        final List<JsonObject> pyramids = new ArrayList<JsonObject>();
        final List<JsonObject> all = new ArrayList<JsonObject>();
        int candidates;
    }

    static List<Oracle> oracles() throws Exception {
        List<Oracle> out = new ArrayList<Oracle>();
        File dir = new File("../tools/structures/oracle120");
        File[] files = dir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) {
            return out;
        }
        java.util.Arrays.sort(files);
        for (File f : files) {
            try (InputStreamReader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
                JsonObject o = new JsonParser().parse(r).getAsJsonObject();
                Oracle oracle = new Oracle();
                oracle.seed = o.get("seed").getAsLong();
                for (JsonElement e : o.getAsJsonArray("starts")) {
                    JsonObject s = e.getAsJsonObject();
                    String id = s.get("structure").getAsString();
                    if (id.equals("minecraft:trail_ruins")) {
                        oracle.candidates++;
                        if (!s.has("missing")) {
                            oracle.all.add(s);
                        }
                    }
                    if (!s.has("valid") || !s.get("valid").getAsBoolean()) {
                        continue;
                    }
                    (id.equals("minecraft:trail_ruins") ? oracle.trail : oracle.pyramids).add(s);
                }
                out.add(oracle);
            }
        }
        return out;
    }

    static int[] ints(JsonArray a) {
        int[] out = new int[a.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = a.get(i).getAsInt();
        }
        return out;
    }

    static String describe(TrailRuinsPiece p) {
        StructureBoundingBox b = p.getBoundingBox();
        return p.template() + " " + p.position().getX() + "," + p.position().getY() + "," + p.position().getZ() + " " + p.rotation()
                + " [" + b.minX + "," + b.minY + "," + b.minZ + "," + b.maxX + "," + b.maxY + "," + b.maxZ + "]";
    }

    static String describe(JsonObject v) {
        int[] pos = ints(v.getAsJsonArray("pos"));
        int[] bb = ints(v.getAsJsonArray("bb"));
        return v.get("location").getAsString() + " " + pos[0] + "," + pos[1] + "," + pos[2] + " " + v.get("rotation").getAsString()
                + " [" + bb[0] + "," + bb[1] + "," + bb[2] + "," + bb[3] + "," + bb[4] + "," + bb[5] + "]";
    }

    static String junctions(TrailRuinsPiece p) {
        StringBuilder b = new StringBuilder();
        int[] j = p.junctions();
        for (int i = 0; i < j.length; i += 3) {
            b.append(j[i]).append(',').append(j[i + 1]).append(',').append(j[i + 2]).append(';');
        }
        return b.toString();
    }

    static String junctions(JsonObject v) {
        StringBuilder b = new StringBuilder();
        if (v.has("junctions") && v.get("junctions").isJsonArray()) {
            for (JsonElement e : v.getAsJsonArray("junctions")) {
                JsonObject j = e.getAsJsonObject();
                b.append(j.get("source_x").getAsInt()).append(',').append(j.get("source_ground_y").getAsInt()).append(',')
                        .append(j.get("source_z").getAsInt()).append(';');
            }
        }
        return b.toString();
    }

    /** The suspicious blocks a layout leaves once every piece is placed in order: position to "table seed". */
    static Map<BlockPos, String> finalSuspicious(List<TrailRuinsPiece> pieces, long seed) {
        Map<BlockPos, String> out = new LinkedHashMap<BlockPos, String>();
        Map<BlockPos, String> blocks = new HashMap<BlockPos, String>();
        for (TrailRuinsPiece p : pieces) {
            for (Archaeology.Info i : p.processed(seed)) {
                blocks.put(i.pos, i.name);
                boolean sus = i.name.equals("minecraft:suspicious_gravel") && i.nbt != null && i.nbt.hasKey("LootTable");
                if (sus) {
                    out.put(i.pos, i.nbt.getString("LootTable") + " " + i.nbt.getLong("LootTableSeed"));
                } else {
                    out.remove(i.pos);
                }
            }
        }
        return out;
    }

    private void oracle(Check c) throws Exception {
        List<Oracle> oracles = oracles();
        c.ok(oracles.size() >= 3, "the 1.20.1 oracle has " + oracles.size() + " seeds");
        int starts = 0, piecesOk = 0, pieces = 0, junctionsOk = 0, susOk = 0, sus = 0, susExtra = 0, pyramids = 0, sandOk = 0, sand = 0;
        for (Oracle o : oracles) {
            for (JsonObject v : o.trail) {
                int[] chunk = ints(v.getAsJsonArray("chunk"));
                JsonArray vp = v.getAsJsonArray("pieces");
                JsonObject first = vp.get(0).getAsJsonObject();
                // Vanilla's start y = surface - 15 = the first piece's floor; give the layout vanilla's surface.
                int surface = ints(first.getAsJsonArray("bb"))[1] + first.get("ground").getAsInt() - TrailRuins.get().startHeight;
                Random random = StructureRandoms.start(o.seed, chunk[0], chunk[1]);
                TrailLayout.Stub stub = TrailLayout.stub(chunk[0], chunk[1], random, (x, z) -> surface);
                if (!c.ok(stub != null, "seed " + o.seed + " chunk " + chunk[0] + "," + chunk[1] + " has a stub")) {
                    continue;
                }
                List<TrailRuinsPiece> ours = TrailLayout.assemble(stub, random, (x, z) -> surface);
                starts++;
                pieces += vp.size();
                boolean all = ours.size() == vp.size();
                for (int i = 0; i < Math.min(ours.size(), vp.size()); i++) {
                    JsonObject p = vp.get(i).getAsJsonObject();
                    boolean same = describe(ours.get(i)).equals(describe(p));
                    if (same) {
                        piecesOk++;
                    } else if (all) {
                        c.fail("seed " + o.seed + " chunk " + chunk[0] + "," + chunk[1] + " piece " + i + ": ours " + describe(ours.get(i))
                                + ", vanilla " + describe(p));
                        all = false;
                    }
                    if (junctions(ours.get(i)).equals(junctions(p))) {
                        junctionsOk++;
                    }
                }
                c.ok(ours.size() == vp.size(), "seed " + o.seed + " chunk " + chunk[0] + "," + chunk[1] + ": " + ours.size()
                        + " pieces, vanilla " + vp.size());

                Map<BlockPos, String> want = new HashMap<BlockPos, String>();
                for (JsonElement e : v.getAsJsonArray("suspicious")) {
                    JsonObject s = e.getAsJsonObject();
                    int[] p = ints(s.getAsJsonArray("pos"));
                    want.put(new BlockPos(p[0], p[1], p[2]), s.get("loot").getAsString() + " " + s.get("seed").getAsLong());
                }
                Map<BlockPos, String> got = finalSuspicious(ours, o.seed);
                sus += want.size();
                int same = 0;
                for (Map.Entry<BlockPos, String> e : want.entrySet()) {
                    if (e.getValue().equals(got.get(e.getKey()))) {
                        same++;
                    }
                }
                susOk += same;
                // Ours may hold a few more: vanilla's later features (a water spring, an ore blob) can hollow the ground
                // under a suspicious block after the ruins are built, and it falls; that is terrain, not the ruins.
                int extra = got.size() - same;
                susExtra += extra;
                if (!c.ok(same == want.size() && extra <= 2, "seed " + o.seed + " chunk " + chunk[0] + "," + chunk[1]
                        + ": suspicious gravel " + same + "/" + want.size() + " of vanilla's (ours " + got.size() + ")")) {
                    for (Map.Entry<BlockPos, String> e : want.entrySet()) {
                        if (!e.getValue().equals(got.get(e.getKey()))) {
                            c.fail("  vanilla " + e.getKey() + " " + e.getValue() + ", ours " + got.get(e.getKey()));
                            break;
                        }
                    }
                    for (Map.Entry<BlockPos, String> e : got.entrySet()) {
                        if (!want.containsKey(e.getKey())) {
                            c.fail("  ours only " + e.getKey() + " " + e.getValue());
                        }
                    }
                }
            }
            for (JsonObject v : o.pyramids) {
                JsonObject p = v.getAsJsonArray("pieces").get(0).getAsJsonObject();
                int[] bb = ints(p.getAsJsonArray("bb"));
                ComponentScatteredFeaturePieces.DesertPyramid piece = new ComponentScatteredFeaturePieces.DesertPyramid(new Random(0L), 0, 0);
                piece.setCoordBaseMode(EnumFacing.byHorizontalIndex(p.get("orientation").getAsInt()));
                StructureBoundingBox b = piece.getBoundingBox();
                b.minX = bb[0];
                b.minY = bb[1];
                b.minZ = bb[2];
                b.maxX = bb[3];
                b.maxY = bb[4];
                b.maxZ = bb[5];
                List<BlockPos> ours = DesertPyramids.suspiciousSpots(o.seed, piece, new StructureBoundingBox(b));
                Map<BlockPos, String> want = new HashMap<BlockPos, String>();
                for (JsonElement e : v.getAsJsonArray("suspicious")) {
                    JsonObject s = e.getAsJsonObject();
                    if (s.get("loot").getAsString().equals("minecraft:archaeology/desert_pyramid")) {
                        int[] q = ints(s.getAsJsonArray("pos"));
                        want.put(new BlockPos(q[0], q[1], q[2]), String.valueOf(s.get("seed").getAsLong()));
                    }
                }
                pyramids++;
                sand += want.size();
                int same = 0;
                for (BlockPos q : ours) {
                    if (String.valueOf(VanillaRandom.asLong(q)).equals(want.get(q))) {
                        same++;
                    }
                }
                sandOk += same;
                c.ok(same == want.size() && ours.size() == want.size(), "seed " + o.seed + " pyramid at " + bb[0] + "," + bb[2]
                        + ": suspicious sand " + same + "/" + want.size() + " (ours " + ours.size() + ")");
            }
        }
        com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport.LOGGER.info("SELFTEST structures: oracle {} seeds, {} trail ruins, pieces {}/{}, junctions {}/{}, suspicious gravel {}/{} (+{} ours), {} pyramids, sand {}/{}",
                oracles.size(), starts, piecesOk, pieces, junctionsOk, pieces, susOk, sus, susExtra, pyramids, sandOk, sand);
        c.ok(starts > 0, "the oracle has " + starts + " valid trail ruins");
        c.eq(pieces, piecesOk, "trail ruins pieces as vanilla's: " + piecesOk + "/" + pieces);
        c.eq(pieces, junctionsOk, "junctions as vanilla's: " + junctionsOk + "/" + pieces);
        c.eq(sus, susOk, "suspicious gravel as vanilla's: " + susOk + "/" + sus);
        c.ok(susExtra * 100 <= sus, susExtra + " more of ours than vanilla kept (ground hollowed under them later)");
        c.ok(pyramids > 0, "the oracle has " + pyramids + " desert pyramids");
        c.eq(sand, sandOk, "pyramid suspicious sand as vanilla's: " + sandOk + "/" + sand);
    }

    // ---- this world

    private void world(Check c) throws Exception {
        WorldServer world = c.world;
        ScatteredStructure s = ScatteredStructure.of(world, Structures.TRAIL_RUINS);
        c.ok(s instanceof TrailRuinsStructure, "this world has trail ruins");
        if (!(s instanceof TrailRuinsStructure)) {
            return;
        }
        StructureLocate.Found found = StructureLocate.nearest(world, BlockPos.ORIGIN, "minecraft:trail_ruins");
        if (!c.ok(found != null && found.pos != null, "/locate finds trail ruins: " + (found == null ? null : found.pos))) {
            return;
        }
        List<String> said = LocateChecks.run(world.getMinecraftServer(), world, BlockPos.ORIGIN, "locate structure minecraft:trail_ruins");
        c.ok(!said.isEmpty() && said.get(0).contains(String.valueOf(found.pos.getX())), "/locate structure minecraft:trail_ruins says " + said);
        String ring = LocateChecks.checkRingOrder(world, BlockPos.ORIGIN, "minecraft:trail_ruins", found);
        c.ok(ring == null, "the located ruins are today's search result" + (ring == null ? "" : ": " + ring));
        int cx = found.pos.getX() >> 4, cz = found.pos.getZ() >> 4;
        c.ok(s.startsAt(world, cx, cz), "ruins start in the located chunk");
        TrailLayout.Stub stub = TrailRuinsStructure.stub(world, cx, cz);
        c.ok(stub != null && TrailRuinsStructure.validBiome(world, stub.position), "the stub's biome is in the tag");

        List<TrailRuinsPiece> layout = TrailRuinsStructure.layout(world, cx, cz);
        StructureBoundingBox all = new StructureBoundingBox(layout.get(0).getBoundingBox());
        for (TrailRuinsPiece p : layout) {
            all.expandTo(p.getBoundingBox());
        }
        boolean modern = "com.exiledradio.cavesandcliffsbackport.terrain.ModernGenerator".equals(
                world.getChunkProvider().chunkGenerator.getClass().getName());
        // The bare terrain's height there (a chunk generated aside, not added to the world), as WORLD_SURFACE_WG.
        net.minecraft.world.chunk.Chunk bare = world.getChunkProvider().chunkGenerator.generateChunk(stub.position.getX() >> 4, stub.position.getZ() >> 4);
        int surface = bare.getHeightValue(stub.position.getX() & 15, stub.position.getZ() & 15);
        // In 1.18 terrain the bare chunk already has bury's beard over the ruins; the vanilla comparison below covers it.
        c.ok(modern || Math.abs(stub.position.getY() + 15 - surface) <= 1, "the start is 15 under the surface: start y " + stub.position.getY()
                + ", bare terrain " + surface);
        // This world's seed is in the oracle: the layout here is vanilla's, at this world's height.
        for (Oracle o : oracles()) {
            if (o.seed != world.getSeed()) {
                continue;
            }
            // Which candidates start: in 1.18 terrain the noise biomes are vanilla's, so exactly vanilla's.
            int agree = 0, total = 0;
            for (JsonObject v : o.all) {
                int[] ch = ints(v.getAsJsonArray("chunk"));
                boolean valid = v.has("valid") && v.get("valid").getAsBoolean();
                total++;
                agree += s.startsAt(world, ch[0], ch[1]) == valid ? 1 : 0;
            }
            com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport.LOGGER.info("SELFTEST structures: {} of {} trail ruins candidates start as in vanilla 1.20.1 ({})",
                    agree, total, modern ? "1.18 terrain" : "1.12.2 terrain, its own biomes");
            if (modern) {
                c.eq(total, agree, "every trail ruins candidate starts or not as in vanilla 1.20.1");
            }
            for (JsonObject v : o.trail) {
                int[] chunk = ints(v.getAsJsonArray("chunk"));
                if (chunk[0] == cx && chunk[1] == cz) {
                    JsonArray vp = v.getAsJsonArray("pieces");
                    int dy = layout.get(0).getBoundingBox().minY - ints(vp.get(0).getAsJsonObject().getAsJsonArray("bb"))[1];
                    int match = 0;
                    for (int i = 0; i < Math.min(layout.size(), vp.size()); i++) {
                        TrailRuinsPiece p = layout.get(i);
                        StructureBoundingBox b = p.getBoundingBox();
                        String ours = p.template() + " " + p.position().getX() + "," + (p.position().getY() - dy) + "," + p.position().getZ()
                                + " " + p.rotation() + " [" + b.minX + "," + (b.minY - dy) + "," + b.minZ + "," + b.maxX + "," + (b.maxY - dy) + "," + b.maxZ + "]";
                        match += ours.equals(describe(vp.get(i).getAsJsonObject())) ? 1 : 0;
                    }
                    c.eq(vp.size(), match, "this world's ruins are vanilla's, " + dy + " up (" + match + "/" + vp.size() + ")");
                    com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport.LOGGER.info("SELFTEST structures: located ruins at chunk {},{}, {} pieces, {} up from vanilla's", cx, cz, vp.size(), dy);
                    if (modern) {
                        c.eq(0, dy, "in Caves & Cliffs' 1.18 terrain the ruins are at vanilla's height");
                    }
                }
            }
        }

        // Build it: every chunk it touches, and a ring around so each is populated.
        for (int x = (all.minX >> 4) - 1; x <= (all.maxX >> 4) + 2; x++) {
            for (int z = (all.minZ >> 4) - 1; z <= (all.maxZ >> 4) + 2; z++) {
                world.getChunk(x, z);
            }
        }
        StructureStart saved = LocateChecks.savedStart(s, cx, cz);
        c.ok(saved instanceof TrailRuinsStructure.Start && saved.isSizeableStructure(), "the start is laid out and saved");
        if (saved != null) {
            c.eq(layout.size(), saved.getComponents().size(), "the saved start has the layout's pieces");
        }
        Map<BlockPos, String> want = finalSuspicious(layout, world.getSeed());
        c.ok(!want.isEmpty(), want.size() + " suspicious gravel worked out");
        Block gravel = Block.getBlockFromName("trailsandtalesbackport:suspicious_gravel");
        int in = 0, tables = 0, common = 0, rare = 0, fell = 0;
        for (Map.Entry<BlockPos, String> e : want.entrySet()) {
            if (world.getBlockState(e.getKey()).getBlock() == gravel) {
                in++;
            } else if (overNothing(world, e.getKey())) {
                // A later neighbor update in a world built in an earlier run (flowing water, a feature) let it fall.
                fell++;
                in++;
                tables++;
                continue;
            }
            TileEntity te = world.getTileEntity(e.getKey());
            if (te != null) {
                NBTTagCompound t = te.writeToNBT(new NBTTagCompound());
                String table = t.getString("LootTable");
                String vanilla = e.getValue().substring(0, e.getValue().indexOf(' '));
                if (table.equals(Archaeology.table(vanilla).toString()) && e.getValue().endsWith(" " + t.getLong("LootTableSeed"))) {
                    tables++;
                }
                common += table.endsWith("trail_ruins_common") ? 1 : 0;
                rare += table.endsWith("trail_ruins_rare") ? 1 : 0;
            }
        }
        com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport.LOGGER.info("SELFTEST structures: built ruins: {} suspicious gravel worked out, {} in the world ({} fallen since), {} common, {} rare", want.size(), in, fell, common, rare);
        c.eq(want.size(), in, "suspicious gravel in the world where worked out (" + in + "/" + want.size() + ")");
        c.eq(0, fell, "none fell during generation (1.20's world generation sends no updates)");
        com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport.LOGGER.info("SELFTEST structures: {} chunk populations marked so far",
                com.exiledradio.trailsandtalesbackport.archaeology.Population.marked());
        c.eq(want.size(), tables, "each with its table and seed");
        int ticking = 0;
        for (BlockPos p : want.keySet()) {
            ticking += pendingTick(world, p) ? 1 : 0;
        }
        c.eq(0, ticking, "none waits for a tick (1.20's world generation gives them none, so one over air stays)");
        c.ok(common > 0 && rare > 0, common + " common and " + rare + " rare");
        int extra = 0;
        for (BlockPos p : BlockPos.getAllInBox(new BlockPos(all.minX, all.minY, all.minZ), new BlockPos(all.maxX, all.maxY, all.maxZ))) {
            // One over air is still waiting for the tick that drops it, as in 1.20.
            if (world.getBlockState(p).getBlock() == gravel && !want.containsKey(p) && !world.isAirBlock(p.down())) {
                extra++;
            }
        }
        c.eq(0, extra, "no other suspicious gravel in the ruins' box");
        List<BlockPos> placedGravel = new ArrayList<BlockPos>();
        for (BlockPos p : want.keySet()) {
            if (world.getBlockState(p).getBlock() == gravel) {
                placedGravel.add(p);
            }
        }
        populateAround(world, all, 3);
        if (stays(c, "trail ruins", placedGravel)) {
            BlockPos held = null;
            for (BlockPos p : placedGravel) {
                if (held == null && !overNothing(world, p) && world.getBlockState(p.down()).getBlockHardness(world, p.down()) >= 0) {
                    held = p;
                }
            }
            c.ok(held != null, "trail ruins: one suspicious gravel held up by a block");
            if (held != null) {
                disturb(c, "trail ruins", held);
            }
        }
    }

    private void pyramid(Check c) throws Exception {
        WorldServer world = c.world;
        // The world's temple generator itself (1.12.2's, Core's or Caves & Cliffs'): its candidate chunks outwards from
        // spawn, the first whose start is a desert pyramid.
        ComponentScatteredFeaturePieces.DesertPyramid pyramid = null;
        StructureBoundingBox startBox = null;
        java.lang.reflect.Method canSpawn = net.minecraftforge.fml.relauncher.ReflectionHelper.findMethod(
                net.minecraft.world.gen.structure.MapGenStructure.class, "canSpawnStructureAtCoords", "func_75047_a", int.class, int.class);
        java.lang.reflect.Method getStart = net.minecraftforge.fml.relauncher.ReflectionHelper.findMethod(
                net.minecraft.world.gen.structure.MapGenStructure.class, "getStructureStart", "func_75049_b", int.class, int.class);
        java.lang.reflect.Field worldField = net.minecraftforge.fml.relauncher.ReflectionHelper.findField(
                net.minecraft.world.gen.MapGenBase.class, "world", "field_75039_c");
        int pcx = 0, pcz = 0;
        search:
        for (net.minecraft.world.gen.structure.MapGenStructure gen : DesertPyramids.templeGenerators(world)) {
            if (worldField.get(gen) == null) {
                worldField.set(gen, world);
            }
            for (int r = 0; r <= 96; r++) {
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != r || !(Boolean) canSpawn.invoke(gen, dx, dz)) {
                            continue;
                        }
                        StructureStart st = (StructureStart) getStart.invoke(gen, dx, dz);
                        for (StructureComponent comp : st.getComponents()) {
                            if (comp instanceof ComponentScatteredFeaturePieces.DesertPyramid) {
                                pcx = dx;
                                pcz = dz;
                                break search;
                            }
                        }
                    }
                }
            }
        }
        LocateChecks.generateAround(world, pcx, pcz, 2);
        for (StructureComponent comp : DesertPyramids.pyramidsIn(world, new StructureBoundingBox((pcx << 4) - 8, -64, (pcz << 4) - 8,
                (pcx << 4) + 40, 320, (pcz << 4) + 40))) {
            pyramid = (ComponentScatteredFeaturePieces.DesertPyramid) comp;
            startBox = new StructureBoundingBox(comp.getBoundingBox());
        }
        boolean modern = "com.exiledradio.cavesandcliffsbackport.terrain.ModernGenerator".equals(
                world.getChunkProvider().chunkGenerator.getClass().getName());
        if (pyramid == null && modern) {
            // Seed 1013's 1.18 terrain has no desert within 1,500 blocks (vanilla 1.20.1 has no pyramid within 4,000).
            com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport.LOGGER.info("SELFTEST structures: no desert pyramid near spawn in this 1.18 terrain; skipped");
            return;
        }
        if (!c.ok(pyramid != null, "a desert pyramid near spawn")) {
            return;
        }
        StructureBoundingBox b = pyramid.getBoundingBox();
        for (int x = (b.minX >> 4) - 1; x <= (b.maxX >> 4) + 2; x++) {
            for (int z = (b.minZ >> 4) - 1; z <= (b.maxZ >> 4) + 2; z++) {
                world.getChunk(x, z);
            }
        }
        List<BlockPos> spots = DesertPyramids.suspiciousSpots(world.getSeed(), pyramid, startBox);
        c.ok(spots.size() >= 6 && spots.size() <= 8, spots.size() + " suspicious sand spots (the roof's and 5-7)");
        Block sand = Block.getBlockFromName("trailsandtalesbackport:suspicious_sand");
        int in = 0, tables = 0;
        for (BlockPos p : spots) {
            if (world.getBlockState(p).getBlock() == sand) {
                in++;
            }
            TileEntity te = world.getTileEntity(p);
            if (te != null) {
                NBTTagCompound t = te.writeToNBT(new NBTTagCompound());
                tables += "trailsandtalesbackport:archaeology/desert_pyramid".equals(t.getString("LootTable"))
                        && t.getLong("LootTableSeed") == VanillaRandom.asLong(p) ? 1 : 0;
            }
        }
        com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport.LOGGER.info("SELFTEST structures: pyramid at {}: {} suspicious sand, {} in the world", b, spots.size(), in);
        c.eq(spots.size(), in, "the pyramid's suspicious sand is in the world (" + in + "/" + spots.size() + ")");
        c.eq(spots.size(), tables, "with the desert pyramid table and 1.20's seed");
        List<BlockPos> placedSand = new ArrayList<BlockPos>();
        for (BlockPos p : spots) {
            if (world.getBlockState(p).getBlock() == sand) {
                placedSand.add(p);
            }
        }
        populateAround(world, b, 3);
        stays(c, "desert pyramid", placedSand);
        // The cellar's middle: blue terracotta at (16, -4, 13) in the piece.
        BlockPos middle = DesertPyramidsAccess.pos(pyramid, 16, -4, 13);
        c.eq(Blocks.STAINED_HARDENED_CLAY, world.getBlockState(middle).getBlock(), "the cellar's floor at " + middle);
        c.eq(net.minecraft.item.EnumDyeColor.BLUE, world.getBlockState(middle).getValue(net.minecraft.block.BlockColored.COLOR), "blue in the middle");
        BlockPos stair = DesertPyramidsAccess.pos(pyramid, 15, -3, 17);
        c.eq(Blocks.SANDSTONE_STAIRS, world.getBlockState(stair).getBlock(), "the cellar's stairs at " + stair);
    }

    private void well(Check c) {
        WorldServer world = c.world;
        BlockPos corner = c.area();
        c.fill(corner.add(0, -9, 0), corner.add(31, -9, 31), Blocks.STONE.getDefaultState());
        c.fill(corner.add(0, -8, 0), corner.add(31, -1, 31), Blocks.SAND.getDefaultState());
        BlockPos at = corner.add(16, 0, 16);
        c.ok(DesertWells.place(world, at), "a well on sand");
        BlockPos water = at.down();
        c.eq(Blocks.WATER, world.getBlockState(water).getBlock(), "water in the middle");
        c.eq(Blocks.SANDSTONE, world.getBlockState(water.add(2, -2, 2)).getBlock(), "1.20's basin goes two below the water");
        Block sand = Block.getBlockFromName("trailsandtalesbackport:suspicious_sand");
        int sus = 0;
        for (BlockPos p : BlockPos.getAllInBox(water.add(-1, -2, -1), water.add(1, -1, 1))) {
            if (world.getBlockState(p).getBlock() == sand) {
                sus++;
                NBTTagCompound t = world.getTileEntity(p).writeToNBT(new NBTTagCompound());
                c.eq("trailsandtalesbackport:archaeology/desert_well", t.getString("LootTable"), "well table at " + p);
            } else if (p.getY() == water.getY() - 1 && Math.abs(p.getX() - water.getX()) + Math.abs(p.getZ() - water.getZ()) <= 1) {
                c.eq(Blocks.SAND, world.getBlockState(p).getBlock(), "sand under the water at " + p);
            }
        }
        c.eq(2, sus, "two suspicious sand under the well");
        List<BlockPos> wellSand = new ArrayList<BlockPos>();
        for (BlockPos p : BlockPos.getAllInBox(water.add(-1, -2, -1), water.add(1, -1, 1))) {
            if (world.getBlockState(p).getBlock() == sand) {
                wellSand.add(p.toImmutable());
            }
        }
        stays(c, "desert well", wellSand);

        // The decoration event: denied, and the same draws make 1.20's well.
        BlockPos corner2 = c.area();
        c.fill(corner2.add(0, -9, 0), corner2.add(31, -9, 31), Blocks.STONE.getDefaultState());
        c.fill(corner2.add(0, -8, 0), corner2.add(31, -1, 31), Blocks.SAND.getDefaultState());
        ChunkPos chunk = new ChunkPos(new BlockPos(corner2.getX() + 8, 0, corner2.getZ() + 8));
        Random always = new Random(5L) {
            @Override
            public int nextInt(int bound) {
                return bound == 1000 ? 0 : 0;
            }
        };
        DecorateBiomeEvent.Decorate event = new DecorateBiomeEvent.Decorate(world, always, chunk, null, DecorateBiomeEvent.Decorate.EventType.DESERT_WELL);
        new StructuresContent.Events().decorate(event);
        c.eq(Event.Result.DENY, event.getResult(), "1.12.2's well is denied");
        BlockPos top = world.getHeight(new BlockPos((chunk.x << 4) + 8, 0, (chunk.z << 4) + 8));
        c.ok(world.getBlockState(top.down()).getBlock() == Blocks.SANDSTONE || world.getBlockState(top.down()).getBlock() == Blocks.STONE_SLAB,
                "1.20's well stands where 1.12.2's would: " + world.getBlockState(top.down()));
    }

    // ---- generation ticks and Relic

    /**
     * The suspicious blocks a structure put in the world, once the chunks around have populated too, wait for no tick
     * and are all still there after every pending tick of the world has run (1.20's world generation gives them none).
     *
     * @return true when there was at least one and all stayed
     */
    private boolean stays(Check c, String what, List<BlockPos> spots) {
        WorldServer world = c.world;
        int ticking = 0;
        List<Block> blocks = new ArrayList<Block>();
        for (BlockPos p : spots) {
            Block b = world.getBlockState(p).getBlock();
            blocks.add(b);
            ticking += pendingTick(world, p) ? 1 : 0;
        }
        c.eq(0, ticking, what + ": none of " + spots.size() + " suspicious blocks waits for a tick");
        int fallen = run(world);
        int stayed = 0, overNothing = 0;
        for (int i = 0; i < spots.size(); i++) {
            if (world.getBlockState(spots.get(i)).getBlock() == blocks.get(i)) {
                stayed++;
                overNothing += overNothing(world, spots.get(i)) ? 1 : 0;
            }
        }
        com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport.LOGGER.info("SELFTEST structures: {}: {} of {} suspicious blocks still in place after the world's ticks ({} falling blocks landed meanwhile), {} of them over air or water",
                what, stayed, spots.size(), fallen, overNothing);
        c.eq(spots.size(), stayed, what + ": every suspicious block still in place after the world's ticks (" + overNothing + " over air or water)");
        return !spots.isEmpty() && stayed == spots.size();
    }

    /**
     * Runs the world on: every pending block tick, then every falling block until it lands (1.12.2's falling block
     * takes its block away on its first update), then the ticks that set off, until nothing more falls.
     *
     * @return how many falling blocks were run
     */
    private static int run(WorldServer world) {
        int ran = 0;
        Pools.settle(world);
        for (int round = 0; round < 50; round++) {
            List<net.minecraft.entity.item.EntityFallingBlock> falling = world.getEntities(
                    net.minecraft.entity.item.EntityFallingBlock.class, e -> e != null && !e.isDead);
            if (falling.isEmpty()) {
                break;
            }
            for (net.minecraft.entity.item.EntityFallingBlock e : falling) {
                ran++;
                for (int t = 0; t < 400 && !e.isDead; t++) {
                    e.onUpdate();
                }
                e.setDead();
            }
            Pools.settle(world);
        }
        return ran;
    }

    /**
     * Loads and so populates {@code ring} more chunks on each side of a box than building it did: the later population
     * of neighboring chunks, whose features set blocks next to the structure's (1.12.2 with notifying flags).
     */
    private static void populateAround(WorldServer world, StructureBoundingBox b, int ring) {
        for (int x = (b.minX >> 4) - 1 - ring; x <= (b.maxX >> 4) + 2 + ring; x++) {
            for (int z = (b.minZ >> 4) - 1 - ring; z <= (b.maxZ >> 4) + 2 + ring; z++) {
                world.getChunk(x, z);
            }
        }
    }

    /**
     * A player breaks the block under one: the neighbor update schedules its fall, and it falls. Both are put back
     * afterwards, so a later run in this world finds the structure whole.
     */
    private void disturb(Check c, String what, BlockPos pos) {
        WorldServer world = c.world;
        Block b = world.getBlockState(pos).getBlock();
        net.minecraft.block.state.IBlockState support = world.getBlockState(pos.down());
        NBTTagCompound loot = world.getTileEntity(pos).writeToNBT(new NBTTagCompound());
        world.setBlockState(pos.down(), Blocks.AIR.getDefaultState(), 3);
        c.ok(pendingTick(world, pos), what + ": breaking the block under " + pos + " schedules its fall");
        Pools.settle(world);
        List<net.minecraft.entity.item.EntityFallingBlock> falling = world.getEntitiesWithinAABB(
                net.minecraft.entity.item.EntityFallingBlock.class, new net.minecraft.util.math.AxisAlignedBB(pos).grow(1));
        c.ok(fallsAway(world, pos, falling), what + ": the disturbed one falls (" + falling.size() + " falling)");
        falling.forEach(net.minecraft.entity.Entity::setDead);
        world.setBlockState(pos.down(), support, 2);
        Archaeology.set(world, pos, b == Block.getBlockFromName("trailsandtalesbackport:suspicious_gravel"),
                new ResourceLocation(loot.getString("LootTable")), loot.getLong("LootTableSeed"));
    }

    /** @return true when exactly one falling block is there and, after its first update, the block is gone */
    private static boolean fallsAway(WorldServer world, BlockPos pos, List<net.minecraft.entity.item.EntityFallingBlock> falling) {
        if (falling.size() != 1) {
            return false;
        }
        falling.get(0).onUpdate();
        return world.getBlockState(pos).getBlock() != falling.get(0).getBlock().getBlock();
    }

    /**
     * @return true when a block tick waits at this position. 1.12.2's {@code isBlockTickPending} looks only at the
     * ticks being run this tick; {@code getPendingBlockUpdates} hands over the whole column, kept.
     */
    static boolean pendingTick(WorldServer world, BlockPos pos) {
        List<net.minecraft.world.NextTickListEntry> all = world.getPendingBlockUpdates(
                new StructureBoundingBox(pos.getX(), 0, pos.getZ(), pos.getX() + 1, 255, pos.getZ() + 1), false);
        if (all != null) {
            for (net.minecraft.world.NextTickListEntry e : all) {
                if (e.position.equals(pos)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Population as 1.12.2 runs it (Population marks the chunk): a suspicious block set by name over air and one set by
     * this mod's structure code, then a feature setting a block beside them with a notifying flag. Neither gets a tick,
     * both stay after the world's ticks; afterwards a player placing a block beside one makes it fall.
     */
    private void generationTicks(Check c) {
        WorldServer world = c.world;
        c.ok(!com.exiledradio.trailsandtalesbackport.archaeology.Population.active(world), "no chunk is populating while the test runs");
        BlockPos corner = c.area();
        BlockPos byName = corner.add(8, 10, 8);
        BlockPos ours = corner.add(12, 10, 8);
        int cx = byName.getX() >> 4, cz = byName.getZ() >> 4;
        Block sand = Block.getBlockFromName("trailsandtalesbackport:suspicious_sand");
        com.exiledradio.trailsandtalesbackport.archaeology.Population.begin(world, cx, cz);
        c.ok(com.exiledradio.trailsandtalesbackport.archaeology.Population.active(world), "the chunk is populating");
        world.setBlockState(byName, sand.getDefaultState(), 3);
        Archaeology.set(world, ours, false, Archaeology.table("minecraft:archaeology/desert_well"), 7L);
        world.setBlockState(byName.east(), Blocks.STONE.getDefaultState(), 3);
        world.setBlockState(ours.east(), Blocks.STONE.getDefaultState(), 3);
        com.exiledradio.trailsandtalesbackport.archaeology.Population.end(world, cx, cz);
        c.ok(!com.exiledradio.trailsandtalesbackport.archaeology.Population.active(world), "population over");
        c.ok(!pendingTick(world, byName) && !pendingTick(world, ours),
                "set and updated while populating: no tick (1.20's WorldGenRegion calls no onPlace and sends no updates)");
        Pools.settle(world);
        c.eq(sand, world.getBlockState(byName).getBlock(), "set by name over air: still there after the world's ticks");
        c.eq(sand, world.getBlockState(ours).getBlock(), "set by the structure code over air: still there after the world's ticks");
        // A player places a block beside it.
        world.setBlockState(byName.west(), Blocks.DIRT.getDefaultState(), 3);
        c.ok(pendingTick(world, byName), "a player's block beside it schedules the fall");
        Pools.settle(world);
        List<net.minecraft.entity.item.EntityFallingBlock> falling = world.getEntitiesWithinAABB(
                net.minecraft.entity.item.EntityFallingBlock.class, new net.minecraft.util.math.AxisAlignedBB(byName).grow(1));
        c.ok(fallsAway(world, byName, falling), "and it falls (" + falling.size() + " falling)");
        c.eq(sand, world.getBlockState(ours).getBlock(), "the other one, not disturbed, stays");
        falling.forEach(net.minecraft.entity.Entity::setDead);
    }

    /** P7: a jukebox playing Relic gives 14, as 1.20; vanilla's discs (and Nether Update's Pigstep) keep theirs. */
    private void relicJukebox(Check c) {
        boolean p7 = com.exiledradio.trailsandtalesbackport.asm.Asm.APPLIED.contains("P7 BlockJukebox.getComparatorInputOverride");
        c.ok(p7, "P7 is applied (BlockJukebox.getComparatorInputOverride): " + com.exiledradio.trailsandtalesbackport.asm.Asm.APPLIED);
        BlockPos at = c.area().add(4, 0, 4);
        c.ok(StructuresContent.relic != null, "Relic is registered");
        c.eq(14, jukebox(c, at, StructuresContent.relic), "a jukebox playing Relic: comparator 14");
        c.eq(1, jukebox(c, at, net.minecraft.init.Items.RECORD_13), "13: 1");
        c.eq(2, jukebox(c, at, net.minecraft.init.Items.RECORD_CAT), "cat: 2");
        c.eq(12, jukebox(c, at, net.minecraft.init.Items.RECORD_WAIT), "wait: 12");
        c.eq(0, jukebox(c, at, null), "an empty jukebox: 0");
        if (net.minecraftforge.fml.common.Loader.isModLoaded("netherupdatebackport")) {
            net.minecraft.item.Item pigstep = net.minecraft.item.Item.getByNameOrId("netherupdatebackport:music_disc_pigstep");
            c.ok(pigstep != null, "Nether Update's Pigstep is there");
            if (pigstep != null) {
                c.eq(13, jukebox(c, at, pigstep), "with Nether Update's P2: Pigstep still 13");
            }
            c.eq(14, jukebox(c, at, StructuresContent.relic), "with Nether Update's P2: Relic still 14");
        }
        if (net.minecraftforge.fml.common.Loader.isModLoaded("wildupdatebackport")) {
            jukebox(c, at, StructuresContent.relic);
            net.minecraft.block.state.IBlockState s = c.world.getBlockState(at);
            com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport.LOGGER.info("SELFTEST structures: with Wild Update, a jukebox playing Relic: comparator {}, power source {}",
                    s.getComparatorInputOverride(c.world, at), s.canProvidePower());
            c.eq(14, s.getComparatorInputOverride(c.world, at), "with Wild Update's P5: Relic still 14");
        }
        c.world.setBlockState(at, Blocks.AIR.getDefaultState(), 2);
    }

    /** @return the comparator signal of a jukebox at {@code at} holding {@code disc} (null: none) */
    private static int jukebox(Check c, BlockPos at, net.minecraft.item.Item disc) {
        WorldServer world = c.world;
        world.setBlockState(at, Blocks.JUKEBOX.getDefaultState().withProperty(net.minecraft.block.BlockJukebox.HAS_RECORD, disc != null), 2);
        TileEntity te = world.getTileEntity(at);
        if (te instanceof net.minecraft.block.BlockJukebox.TileEntityJukebox) {
            ((net.minecraft.block.BlockJukebox.TileEntityJukebox) te).setRecord(disc == null ? net.minecraft.item.ItemStack.EMPTY : new net.minecraft.item.ItemStack(disc));
        }
        return world.getBlockState(at).getComparatorInputOverride(world, at);
    }

    /** Nothing holds a block up there: air or a liquid below. */
    static boolean overNothing(WorldServer world, BlockPos pos) {
        net.minecraft.block.state.IBlockState below = world.getBlockState(pos.down());
        return world.isAirBlock(pos.down()) || below.getMaterial().isLiquid()
                || (world.isAirBlock(pos) || world.getBlockState(pos).getMaterial().isLiquid());
    }

    /** The piece frame of {@link DesertPyramids}, for the test (1.12.2's getXWithOffset and friends are protected). */
    static final class DesertPyramidsAccess {
        static BlockPos pos(StructureComponent c, int x, int y, int z) {
            StructureBoundingBox b = c.getBoundingBox();
            EnumFacing f = c.getCoordBaseMode();
            int wx = f == EnumFacing.WEST ? b.maxX - z : f == EnumFacing.EAST ? b.minX + z : b.minX + x;
            int wz = f == EnumFacing.NORTH ? b.maxZ - z : f == EnumFacing.SOUTH ? b.minZ + z : b.minZ + x;
            return new BlockPos(wx, b.minY + y, wz);
        }
    }
}
