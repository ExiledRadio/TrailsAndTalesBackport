package com.exiledradio.trailsandtalesbackport.selftest;

import com.exiledradio.backportcore.waterlog.Waterloggable;
import com.exiledradio.backportcore.waterlog.Waterlogging;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.cherry.CherryTree;
import com.exiledradio.trailsandtalesbackport.hangingsign.BlockHangingSign;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockWallSign;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Waterlogging: 1.20.6's over Backport Core's {@code waterlog} API, with Fluidlogged API and without it.
 *
 * <p>With it: exactly the blocks of ours 1.20.6 waterlogs hold a water source (never flowing water or lava);
 * placing into a source keeps it (by {@code setBlockState} and by the player's items), into flowing water
 * replaces it; flowing water does not run into a dry block; buckets fill and empty them; breaking one, or a
 * hanging sign losing its support, leaves the water; a cherry tree grown in water keeps it in its leaves. Without
 * it ({@code -PnoFluidlogged}): nothing holds water, as before 1.13.
 */
public final class WaterlogSelfTest implements SelfTest {

    /** Other streams' blocks, checked by their own tests (the calibrated sculk sensor, the sculk sensor copy). */
    private static final List<String> OTHERS = Arrays.asList("calibrated_sculk_sensor", "sculk_sensor");

    @Override
    public String name() {
        return "waterlog";
    }

    /** @return whether 1.20.6 makes this block of ours waterloggable (a double slab never) */
    static boolean waterloggable(String path) {
        if (path.endsWith("_double_slab")) {
            return false;
        }
        return path.endsWith("_slab") || path.endsWith("_stairs") || path.endsWith("_fence") || path.endsWith("_trapdoor")
                || path.endsWith("_sign") // standing, wall, hanging and wall hanging signs
                || path.equals("chain") || path.equals("decorated_pot") || path.equals("cherry_leaves")
                // The amethyst copy (stream Sm): 1.20.6's AmethystClusterBlock is a SimpleWaterloggedBlock.
                || path.endsWith("_amethyst_bud") || path.equals("amethyst_cluster");
    }

    @Override
    public void run(Check c) throws Exception {
        boolean fl = Waterlogging.isAvailable();
        TrailsAndTalesBackport.LOGGER.info("SELFTEST waterlog: Fluidlogged API {}", fl ? "installed" : "absent");
        classes(c);
        if (fl) {
            states(c);
            placing(c);
            items(c);
            breaking(c);
            flowing(c);
            buckets(c);
            tree(c, true);
        } else {
            dry(c);
            tree(c, false);
        }
    }

    /** @return our block of this name, or null when it is not registered (the registry would hand back air) */
    private static Block ours(String name) {
        ResourceLocation id = new ResourceLocation(TrailsAndTalesBackport.MOD_ID, name);
        return ForgeRegistries.BLOCKS.containsKey(id) ? ForgeRegistries.BLOCKS.getValue(id) : null;
    }

    private static boolean mine(Block block) {
        ResourceLocation name = block.getRegistryName();
        return name != null && TrailsAndTalesBackport.MOD_ID.equals(name.getNamespace()) && !OTHERS.contains(name.getPath());
    }

    // --- 1. Which blocks are Waterloggable (both ways) ---------------------------------------------------------

    private static void classes(Check c) {
        List<String> wrong = new ArrayList<String>();
        List<String> yes = new ArrayList<String>();
        for (Block block : ForgeRegistries.BLOCKS) {
            if (!mine(block)) {
                continue;
            }
            String path = block.getRegistryName().getPath();
            // A double slab shares its half's class (and so the interface); canHoldWater says no for it.
            boolean expected = waterloggable(path) || path.endsWith("_double_slab") && block instanceof Waterloggable;
            if ((block instanceof Waterloggable) != expected) {
                wrong.add(path);
            }
            if (block instanceof Waterloggable && waterloggable(path)) {
                yes.add(path);
            }
        }
        c.ok(wrong.isEmpty(), "exactly the blocks 1.20.6 waterlogs are Waterloggable (" + yes.size() + "), wrong: " + wrong);
        TrailsAndTalesBackport.LOGGER.info("SELFTEST waterlog: waterloggable: {}", yes);
        for (String n : new String[] {"oak_hanging_sign", "oak_wall_hanging_sign", "cherry_sign", "cherry_wall_sign", "bamboo_sign",
                "cherry_stairs", "bamboo_mosaic_stairs", "cherry_slab", "bamboo_mosaic_slab", "cherry_fence", "bamboo_fence",
                "cherry_trapdoor", "bamboo_trapdoor", "decorated_pot", "cherry_leaves"}) {
            Block b = ours(n);
            c.ok(b instanceof Waterloggable, n + " is waterloggable, as in 1.20.6");
        }
        Block chain = ours("chain");
        c.ok(chain == null || chain instanceof Waterloggable, "our chain copy (when registered) is waterloggable");
        for (String n : new String[] {"cherry_door", "cherry_fence_gate", "cherry_button", "cherry_pressure_plate", "cherry_planks",
                "cherry_log", "cherry_sapling", "pink_petals", "sniffer_egg", "chiseled_bookshelf", "suspicious_sand", "piglin_head",
                "bamboo", "bamboo_sapling", "torchflower", "pitcher_crop", "potted_cherry_sapling"}) {
            Block b = ours(n);
            c.ok(b == null || !(b instanceof Waterloggable), n + " is not waterloggable, as in 1.20.6");
        }
    }

    // --- 2. Every state of ours, with Fluidlogged API ----------------------------------------------------------

    private static void states(Check c) {
        WorldServer world = c.world;
        BlockPos probe = c.area().add(16, 1, 16);
        int states = 0, holding = 0;
        List<String> wrong = new ArrayList<String>();
        for (Block block : ForgeRegistries.BLOCKS) {
            if (!mine(block)) {
                continue;
            }
            boolean expected = waterloggable(block.getRegistryName().getPath());
            for (IBlockState state : block.getBlockState().getValidStates()) {
                states++;
                boolean can = Waterlogging.canHoldWater(state, world, probe);
                holding += can ? 1 : 0;
                if (can != expected) {
                    wrong.add(state + " holds water: " + can);
                }
                String other = WaterlogFluidChecks.otherFluids(state, world, probe);
                if (other != null) {
                    wrong.add(state + " takes " + other);
                }
            }
        }
        c.ok(wrong.isEmpty(), states + " states of ours checked against 1.20.6 (" + holding + " hold a water source), wrong: "
                + (wrong.size() > 20 ? wrong.subList(0, 20) + " ..." : wrong));
        TrailsAndTalesBackport.LOGGER.info("SELFTEST waterlog: {} states of ours, {} hold a water source, none flowing water or lava",
                states, holding);
    }

    // --- 3. Placing into water ----------------------------------------------------------------------------------

    /** A 14x14 stone-walled pool of still water on a fresh area's floor. @return its corner, one below the water */
    static BlockPos pool(Check c, int depth) {
        BlockPos corner = c.area();
        for (int x = 0; x < 14; x++) {
            for (int z = 0; z < 14; z++) {
                boolean edge = x == 0 || z == 0 || x == 13 || z == 13;
                for (int y = 0; y < depth; y++) {
                    c.world.setBlockState(corner.add(x, y, z), (edge ? Blocks.STONE : Blocks.WATER).getDefaultState(), 2 | 16);
                }
            }
        }
        return corner.add(0, -1, 0);
    }

    private static List<IBlockState> wetStates(Check c) {
        List<IBlockState> wet = new ArrayList<IBlockState>();
        for (String n : new String[] {"cherry_stairs", "bamboo_stairs", "bamboo_mosaic_stairs", "cherry_fence", "bamboo_fence",
                "cherry_trapdoor", "bamboo_trapdoor", "cherry_sign", "cherry_wall_sign", "bamboo_sign", "bamboo_wall_sign",
                "oak_hanging_sign", "oak_wall_hanging_sign", "cherry_hanging_sign", "bamboo_wall_hanging_sign", "spruce_hanging_sign",
                "decorated_pot", "cherry_leaves", "chain"}) {
            Block b = ours(n);
            if (b != null || !"chain".equals(n)) {
                if (c.ok(b != null, n + " exists")) {
                    wet.add(b.getDefaultState());
                }
            }
        }
        wet.add(ours("cherry_slab").getDefaultState().withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.TOP));
        wet.add(ours("bamboo_slab").getDefaultState());
        wet.add(ours("bamboo_mosaic_slab").getDefaultState());
        return wet;
    }

    private static void placing(Check c) {
        WorldServer world = c.world;
        BlockPos pool = pool(c, 1);
        List<IBlockState> wet = wetStates(c);
        for (int i = 0; i < wet.size(); i++) {
            BlockPos p = pool.add(1 + i % 12, 1, 1 + i / 12);
            IBlockState s = wet.get(i);
            world.setBlockState(p, s, 2);
            c.ok(world.getBlockState(p).getBlock() == s.getBlock() && Waterlogging.hasWaterSource(world, p),
                    s.getBlock().getRegistryName() + " placed into a water source keeps it");
        }
        String[] dry = {"cherry_double_slab", "bamboo_mosaic_double_slab", "cherry_fence_gate", "cherry_door", "cherry_planks",
                "chiseled_bookshelf", "sniffer_egg", "cherry_log"};
        for (int i = 0; i < dry.length; i++) {
            BlockPos p = pool.add(1 + i, 1, 6);
            world.setBlockState(p, ours(dry[i]).getDefaultState(), 2);
            c.ok(!Waterlogging.hasWater(world, p), dry[i] + " placed into water replaces it");
        }
    }

    // --- 4. The player's items, into water ------------------------------------------------------------------------

    private static EntityPlayerMP player(Check c, String name) {
        EntityPlayerMP player = Players.real(c.world, name);
        player.interactionManager.setGameType(GameType.SURVIVAL);
        return player;
    }

    /** Uses an item on a block face, as a right click does. */
    private static void use(EntityPlayerMP player, Item item, BlockPos clicked, EnumFacing face, float yaw) {
        player.setLocationAndAngles(clicked.getX() + 0.5D, clicked.getY() + 1.0D, clicked.getZ() + 2.5D, yaw, 30.0F);
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(item));
        player.getHeldItem(EnumHand.MAIN_HAND).onItemUse(player, player.world, clicked, EnumHand.MAIN_HAND, face, 0.5F, 1.0F, 0.5F);
    }

    private static void items(Check c) {
        WorldServer world = c.world;
        BlockPos pool = pool(c, 3);
        EntityPlayerMP player = player(c, "wlplacer");
        BlockPos floor = pool.add(3, 0, 3);

        // A cherry sign on the pool floor, the decorated pot beside it: both keep the source they go into.
        use(player, itemOf("cherry_sign"), floor, EnumFacing.UP, 0.0F);
        c.ok(world.getBlockState(floor.up()).getBlock() == ours("cherry_sign") && Waterlogging.hasWaterSource(world, floor.up()),
                "a cherry sign placed into water keeps the source");
        BlockPos potFloor = floor.east(2);
        use(player, itemOf("decorated_pot"), potFloor, EnumFacing.UP, 0.0F);
        c.ok(world.getBlockState(potFloor.up()).getBlock() == ours("decorated_pot") && Waterlogging.hasWaterSource(world, potFloor.up()),
                "a decorated pot placed into water keeps the source");
        BlockPos stairFloor = floor.east(4);
        use(player, itemOf("cherry_stairs"), stairFloor, EnumFacing.UP, 0.0F);
        c.ok(world.getBlockState(stairFloor.up()).getBlock() == ours("cherry_stairs") && Waterlogging.hasWaterSource(world, stairFloor.up()),
                "cherry stairs placed into water keep the source");

        // A hanging sign under a submerged stone ceiling, and on a wall underwater.
        BlockPos ceiling = pool.add(8, 3, 8);
        world.setBlockState(ceiling, Blocks.STONE.getDefaultState(), 2);
        use(player, itemOf("oak_hanging_sign"), ceiling, EnumFacing.DOWN, 0.0F);
        BlockPos hang = ceiling.down();
        c.ok(world.getBlockState(hang).getBlock() instanceof BlockHangingSign.Ceiling && Waterlogging.hasWaterSource(world, hang),
                "a hanging sign placed into water keeps the source");
        BlockPos wall = pool.add(13, 2, 5); // the pool's east wall, above its floor
        use(player, itemOf("cherry_hanging_sign"), wall, EnumFacing.WEST, 90.0F);
        BlockPos onWall = wall.west();
        c.ok(world.getBlockState(onWall).getBlock() instanceof BlockHangingSign.Wall && Waterlogging.hasWaterSource(world, onWall),
                "a wall hanging sign placed into water keeps the source: " + world.getBlockState(onWall));
        use(player, itemOf("cherry_sign"), wall.south(3), EnumFacing.WEST, 90.0F);
        c.ok(world.getBlockState(wall.south(3).west()).getBlock() instanceof BlockWallSign
                        && Waterlogging.hasWaterSource(world, wall.south(3).west()),
                "a cherry wall sign placed into water keeps the source");
        BlockPos chainAt = pool.add(5, 3, 10);
        Item chain = itemOf("chain");
        if (chain != null) {
            world.setBlockState(chainAt, Blocks.STONE.getDefaultState(), 2);
            use(player, chain, chainAt, EnumFacing.DOWN, 0.0F);
            c.ok(world.getBlockState(chainAt.down()).getBlock() == ours("chain") && Waterlogging.hasWaterSource(world, chainAt.down()),
                    "a chain placed into water keeps the source");
        }
    }

    private static Item itemOf(String name) {
        ResourceLocation id = new ResourceLocation(TrailsAndTalesBackport.MOD_ID, name);
        return ForgeRegistries.ITEMS.containsKey(id) ? ForgeRegistries.ITEMS.getValue(id) : null;
    }

    // --- 5. Breaking keeps the water ------------------------------------------------------------------------------

    private static void breaking(Check c) {
        WorldServer world = c.world;
        BlockPos pool = pool(c, 2);
        String[] names = {"cherry_stairs", "decorated_pot", "cherry_sign", "chain", "cherry_leaves", "cherry_slab"};
        for (int i = 0; i < names.length; i++) {
            Block b = ours(names[i]);
            if (b == null) {
                continue;
            }
            BlockPos p = pool.add(2 + 2 * i, 1, 2);
            world.setBlockState(p, b.getDefaultState(), 2);
            world.destroyBlock(p, true);
            c.ok(world.getBlockState(p).getMaterial() == Material.WATER && Waterlogging.hasWaterSource(world, p),
                    "breaking waterlogged " + names[i] + " leaves the water source");
        }
        // A hanging sign whose ceiling goes drops and leaves its water, as 1.20's.
        BlockPos ceiling = pool.add(6, 2, 8);
        world.setBlockState(ceiling, Blocks.STONE.getDefaultState(), 2);
        BlockPos hang = ceiling.down();
        world.setBlockState(hang, ours("oak_hanging_sign").getDefaultState(), 2);
        c.ok(Waterlogging.hasWaterSource(world, hang), "a hanging sign under a stone in water holds the source");
        world.setBlockState(ceiling, Blocks.WATER.getDefaultState(), 3);
        c.ok(!(world.getBlockState(hang).getBlock() instanceof BlockHangingSign), "without its ceiling it drops");
        c.ok(Waterlogging.hasWaterSource(world, hang), "leaving its water");
    }

    // --- 6. Flowing water -------------------------------------------------------------------------------------

    private static void flowing(Check c) {
        WorldServer world = c.world;
        BlockPos runs = c.area();
        List<String> names = new ArrayList<String>(Arrays.asList("cherry_stairs", "cherry_fence", "oak_hanging_sign", "decorated_pot",
                "cherry_sign", "cherry_leaves"));
        if (ours("chain") != null) {
            names.add("chain");
        }
        for (int i = 0; i < names.size(); i++) {
            BlockPos p = runs.add(2 + 2 * i, 0, 2);
            world.setBlockState(p, Blocks.FLOWING_WATER.getDefaultState().withProperty(BlockLiquid.LEVEL, 3), 2);
            Block b = ours(names.get(i));
            world.setBlockState(p, b.getDefaultState(), 2);
            c.ok(world.getBlockState(p).getBlock() == b && !Waterlogging.hasWater(world, p), names.get(i) + " placed into flowing water replaces it");
        }
        BlockPos spring = c.area().add(10, 0, 10);
        world.setBlockState(spring.east(), ours("cherry_fence").getDefaultState(), 3);
        world.setBlockState(spring.west(), ours("decorated_pot").getDefaultState(), 3);
        world.setBlockState(spring.north(), ours("cherry_trapdoor").getDefaultState(), 3);
        world.setBlockState(spring, Blocks.FLOWING_WATER.getDefaultState(), 3);
        settle(world);
        c.ok(Waterlogging.hasWater(world, spring.south()), "the spring's water spreads");
        c.ok(!Waterlogging.hasWater(world, spring.east()), "not into a dry cherry fence");
        c.ok(!Waterlogging.hasWater(world, spring.west()), "nor a dry decorated pot");
        c.ok(!Waterlogging.hasWater(world, spring.north()), "nor a dry cherry trapdoor");
        // A waterlogged bottom slab lets its water flow out, as 1.20's does.
        BlockPos slab = c.area().add(16, 0, 16);
        Waterlogging.placeWithWater(world, slab, ours("cherry_slab").getDefaultState(), 3);
        settle(world);
        c.ok(Waterlogging.hasWaterSource(world, slab) && Waterlogging.hasWater(world, slab.east()), "a waterlogged slab lets its water flow out");
        BlockPos pot = c.area().add(16, 0, 16);
        Waterlogging.placeWithWater(world, pot, ours("decorated_pot").getDefaultState(), 3);
        settle(world);
        c.ok(Waterlogging.hasWaterSource(world, pot) && Waterlogging.hasWater(world, pot.east()), "so does a waterlogged decorated pot");
    }

    // --- 7. Buckets -------------------------------------------------------------------------------------------

    private static void buckets(Check c) {
        WorldServer world = c.world;
        BlockPos cups = c.area();
        EntityPlayerMP player = player(c, "wlbucket");
        List<String> names = new ArrayList<String>(Arrays.asList("cherry_stairs", "bamboo_mosaic_slab", "cherry_fence", "cherry_trapdoor",
                "decorated_pot", "cherry_sign", "cherry_leaves"));
        if (ours("chain") != null) {
            names.add("chain");
        }
        for (int i = 0; i < names.size(); i++) {
            BlockPos s = cup(world, cups.add(2 + 4 * (i % 7), 0, 2 + 4 * (i / 7)));
            Block b = ours(names.get(i));
            world.setBlockState(s, b.getDefaultState(), 3);
            c.eq(Items.BUCKET, bucket(player, s, Items.WATER_BUCKET).getItem(), "a water bucket on dry " + names.get(i) + " comes back empty");
            c.ok(world.getBlockState(s).getBlock() == b && Waterlogging.hasWaterSource(world, s), "and waterlogs it");
            c.eq(Items.WATER_BUCKET, bucket(player, s, Items.BUCKET).getItem(), "an empty bucket takes the water back");
            c.ok(world.getBlockState(s).getBlock() == b && !Waterlogging.hasWater(world, s), "leaving it dry");
        }
        // A hanging sign under a stone, the bucket used on it from the side.
        BlockPos h = cup(world, cups.add(2, 0, 12));
        world.setBlockState(h.up(), Blocks.STONE.getDefaultState(), 2);
        world.setBlockState(h, ours("oak_hanging_sign").getDefaultState(), 3);
        c.ok(Waterlogging.setWater(world, h, true, 3) && Waterlogging.hasWaterSource(world, h), "a hanging sign takes a water source");
        c.ok(Waterlogging.setWater(world, h, false, 3) && !Waterlogging.hasWater(world, h)
                && world.getBlockState(h).getBlock() == ours("oak_hanging_sign"), "and gives it up, staying");
        BlockPos d = cup(world, cups.add(10, 0, 12));
        world.setBlockState(d, ours("cherry_double_slab").getDefaultState(), 3);
        bucket(player, d, Items.WATER_BUCKET);
        c.ok(!Waterlogging.hasWater(world, d) && Waterlogging.hasWater(world, d.up()), "a water bucket on a double slab pours on top of it");
    }

    private static ItemStack bucket(EntityPlayerMP player, BlockPos pos, Item bucket) {
        player.setLocationAndAngles(pos.getX() + 0.5D, pos.getY() + 1.4D, pos.getZ() + 0.5D, 0.0F, 90.0F);
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(bucket));
        ActionResult<ItemStack> result = bucket.onItemRightClick(player.world, player, EnumHand.MAIN_HAND);
        return result.getResult();
    }

    private static BlockPos cup(WorldServer world, BlockPos pos) {
        world.setBlockState(pos.down(), Blocks.STONE.getDefaultState(), 2);
        for (EnumFacing side : EnumFacing.HORIZONTALS) {
            world.setBlockState(pos.offset(side), Blocks.STONE.getDefaultState(), 2);
        }
        return pos;
    }

    // --- 8. A cherry tree grown in water ---------------------------------------------------------------------------

    /** Grows cherry trees on grass at the bottom of a 16-deep pool: logs replace the water; leaves keep it (with Fluidlogged API). */
    private static void tree(Check c, boolean fl) {
        WorldServer world = c.world;
        Block leaves = ours("cherry_leaves");
        Block log = ours("cherry_log");
        int grown = 0, wetLeaves = 0, dryLeaves = 0, wetLogs = 0;
        for (int attempt = 0; attempt < 6; attempt++) {
            BlockPos corner = c.area();
            for (int x = 0; x < 32; x++) {
                for (int z = 0; z < 32; z++) {
                    boolean edge = x == 0 || z == 0 || x == 31 || z == 31;
                    for (int y = 0; y < 16; y++) {
                        world.setBlockState(corner.add(x, y, z), (edge ? Blocks.STONE : Blocks.WATER).getDefaultState(), 2 | 16);
                    }
                }
            }
            BlockPos at = corner.add(16, 1, 16);
            world.setBlockState(at.down(), Blocks.GRASS.getDefaultState(), 2 | 16);
            if (!new CherryTree(false).generate(world, new Random(attempt), at)) {
                continue;
            }
            grown++;
            for (BlockPos p : BlockPos.getAllInBox(corner.add(1, 0, 1), corner.add(30, 15, 30))) {
                Block b = world.getBlockState(p).getBlock();
                boolean water = Waterlogging.hasWater(world, p);
                if (b == leaves) {
                    if (water) {
                        wetLeaves++;
                    } else {
                        dryLeaves++;
                    }
                } else if (b == log && water) {
                    wetLogs++;
                }
            }
        }
        c.ok(grown > 0, "cherry trees grow in a deep pool (" + grown + " of 6): 1.20 trees may replace water");
        c.eq(0, wetLogs, "logs replace the water");
        if (fl) {
            c.ok(wetLeaves > 0 && dryLeaves == 0, "every leaf placed into a water source keeps it (" + wetLeaves + " wet, " + dryLeaves + " dry)");
        } else {
            c.ok(dryLeaves > 0 && wetLeaves == 0, "without Fluidlogged API the leaves replace the water (" + dryLeaves + " dry)");
        }
        TrailsAndTalesBackport.LOGGER.info("SELFTEST waterlog: cherry trees in a pool: {} grown, {} waterlogged leaves, {} dry", grown,
                wetLeaves, dryLeaves);
    }

    // --- Without Fluidlogged API ---------------------------------------------------------------------------------

    private static void dry(Check c) {
        WorldServer world = c.world;
        BlockPos probe = c.area().add(16, 1, 16);
        List<String> wrong = new ArrayList<String>();
        for (Block block : ForgeRegistries.BLOCKS) {
            if (!mine(block) || !(block instanceof Waterloggable)) {
                continue;
            }
            for (IBlockState state : block.getBlockState().getValidStates()) {
                if (Waterlogging.canHoldWater(state, world, probe)) {
                    wrong.add(state.toString());
                    break;
                }
            }
        }
        c.ok(wrong.isEmpty(), "without Fluidlogged API no block of ours holds water: " + wrong);
        BlockPos pool = pool(c, 1);
        List<IBlockState> wet = wetStates(c);
        int replaced = 0;
        for (int i = 0; i < wet.size(); i++) {
            BlockPos p = pool.add(1 + i % 12, 1, 1 + i / 12);
            world.setBlockState(p, wet.get(i), 2);
            replaced += world.getBlockState(p).getBlock() == wet.get(i).getBlock() && !Waterlogging.hasWater(world, p) ? 1 : 0;
        }
        c.eq(wet.size(), replaced, "each placed into water replaces it, as before 1.13");
        BlockPos t = pool.add(3, 1, 8);
        Waterlogging.placeKeepingWater(world, t, ours("cherry_leaves").getDefaultState(), 2);
        c.ok(world.getBlockState(t).getBlock() == ours("cherry_leaves") && !Waterlogging.hasWater(world, t),
                "generation's placeKeepingWater places leaves dry");
        c.ok(!Waterlogging.setWater(world, pool.add(1, 1, 1), true, 3), "and nothing can be given water");
    }

    /** Runs every pending block tick now, a few rounds, so water finishes flowing. */
    static void settle(WorldServer world) {
        for (int i = 0; i < 40; i++) {
            world.tickUpdates(true);
        }
    }
}
