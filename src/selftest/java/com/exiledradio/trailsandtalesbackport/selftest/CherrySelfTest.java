package com.exiledradio.trailsandtalesbackport.selftest;

import com.exiledradio.trailsandtalesbackport.cherry.BlockCherryLeaves;
import com.exiledradio.trailsandtalesbackport.cherry.BlockCherrySapling;
import com.exiledradio.trailsandtalesbackport.cherry.BlockPinkPetals;
import com.exiledradio.trailsandtalesbackport.cherry.CherryContent;
import com.exiledradio.trailsandtalesbackport.cherry.CherryTree;
import com.exiledradio.trailsandtalesbackport.common.Content;
import com.exiledradio.trailsandtalesbackport.common.wood.WoodSet;
import com.exiledradio.trailsandtalesbackport.common.wood.WoodSign;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStandingSign;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.oredict.OreDictionary;

import java.util.Random;

/** Stream C: cherry wood, leaves, saplings and trees, pink petals. */
public final class CherrySelfTest implements SelfTest {

    @Override
    public String name() {
        return "cherry";
    }

    @Override
    public void run(Check c) {
        WoodSet set = CherryContent.set;
        if (!c.ok(set != null, "cherry stream registered")) {
            return;
        }
        jigsawFeatures(c, set);
        registration(c, set);
        recipesAndFuel(c, set);
        stripping(c, set);
        blocksInUse(c, set);
        trees(c);
        leaves(c, set);
        petals(c);
        pot(c);
    }

    // ---------------------------------------------------------------------------------------------

    private void registration(Check c, WoodSet set) {
        String[] withItems = {"cherry_log", "cherry_wood", "stripped_cherry_log", "stripped_cherry_wood", "cherry_planks",
                "cherry_stairs", "cherry_slab", "cherry_fence", "cherry_fence_gate", "cherry_trapdoor",
                "cherry_pressure_plate", "cherry_button", "cherry_leaves", "cherry_sapling", "pink_petals"};
        for (String name : withItems) {
            Block b = Block.REGISTRY.getObject(Content.id(name));
            c.ok(b != Blocks.AIR, name + " registered");
            c.ok(Item.getItemFromBlock(b) != Items.AIR, name + " has an item");
        }
        for (String name : new String[] {"cherry_double_slab", "cherry_wall_sign", "cherry_sign", "potted_cherry_sapling", "cherry_door"}) {
            Block b = Block.REGISTRY.getObject(Content.id(name));
            c.ok(b != Blocks.AIR, name + " registered");
            c.eq(Items.AIR, Item.getItemFromBlock(b), name + " has no block item");
        }
        c.ok(Item.REGISTRY.getObject(Content.id("cherry_door")) == set.doorItem, "door item");
        c.ok(Item.REGISTRY.getObject(Content.id("cherry_sign")) == set.signItem, "sign item");

        // Every state survives saving and loading where it stores everything (stairs, fences and the rest
        // compute some properties from neighbors, which metadata never holds).
        Block[] exact = {set.log, set.wood, set.strippedLog, set.strippedWood, set.slab, CherryContent.leaves,
                CherryContent.sapling, CherryContent.petals, set.trapdoor, set.button, set.pressurePlate,
                set.sign, set.wallSign};
        for (Block b : exact) {
            for (IBlockState s : b.getBlockState().getValidStates()) {
                IBlockState back = b.getStateFromMeta(b.getMetaFromState(s));
                if (!back.equals(s)) {
                    c.fail(b.getRegistryName() + " state " + s + " came back as " + back);
                }
            }
        }
        c.eq(EnumFacing.Axis.Y, set.log.getDefaultState().getValue(BlockRotatedPillar.AXIS), "logs are upright by default");
        c.eq(16, CherryContent.petals.getBlockState().getValidStates().size(), "pink petals: 4 facings x 4 amounts");
        c.eq(4, CherryContent.leaves.getBlockState().getValidStates().size(), "leaves: decay flags only");

        c.ok(OreDictionary.getOres("logWood").stream().anyMatch(s -> s.getItem() == Item.getItemFromBlock(set.log)), "cherry log is logWood");
        c.ok(OreDictionary.getOres("plankWood").stream().anyMatch(s -> s.getItem() == Item.getItemFromBlock(set.planks)), "cherry planks are plankWood");
        c.ok(OreDictionary.getOres("treeLeaves").stream().anyMatch(s -> s.getItem() == Item.getItemFromBlock(CherryContent.leaves)), "cherry leaves are treeLeaves");

        c.eq(20, Blocks.FIRE.getFlammability(set.planks), "planks flammability");
        c.eq(5, Blocks.FIRE.getFlammability(set.log), "log flammability");
        c.eq(60, Blocks.FIRE.getFlammability(CherryContent.leaves), "leaves flammability");
        c.eq(100, Blocks.FIRE.getFlammability(CherryContent.petals), "petals flammability");
        c.eq(2.0F, set.planks.getBlockHardness(set.planks.getDefaultState(), c.world, BlockPos.ORIGIN), "planks hardness");
        c.eq(3.0F, set.door.getBlockHardness(set.door.getDefaultState(), c.world, BlockPos.ORIGIN), "door hardness");
        c.eq(CherryContent.CHERRY.sound, set.planks.getSoundType(), "planks use the cherry wood sounds");
        c.ok(CherryContent.CHERRY.sound.getBreakSound().getRegistryName() != null
                && "trailsandtalesbackport:block.cherry_wood.break".equals(CherryContent.CHERRY.sound.getBreakSound().getRegistryName().toString()),
                "cherry wood break sound registered");
    }

    private void recipesAndFuel(Check c, WoodSet set) {
        ItemStack log = new ItemStack(set.log);
        ItemStack planks = new ItemStack(set.planks);
        ItemStack stick = new ItemStack(Items.STICK);
        ItemStack none = ItemStack.EMPTY;
        Crafting.expect(c, Crafting.grid(1, 1, log), new ItemStack(set.planks, 4), "log -> 4 planks");
        Crafting.expect(c, Crafting.grid(1, 1, new ItemStack(set.strippedWood)), new ItemStack(set.planks, 4), "stripped wood -> 4 planks");
        Crafting.expect(c, Crafting.grid(2, 2, log, log, log, log), new ItemStack(set.wood, 3), "4 logs -> 3 wood");
        Crafting.expect(c, Crafting.grid(3, 3, planks, none, none, planks, planks, none, planks, planks, planks),
                new ItemStack(set.stairs, 4), "stairs");
        Crafting.expect(c, Crafting.grid(3, 1, planks, planks, planks), new ItemStack(set.slab, 6), "slabs");
        Crafting.expect(c, Crafting.grid(3, 2, planks, stick, planks, planks, stick, planks), new ItemStack(set.fence, 3), "fence");
        Crafting.expect(c, Crafting.grid(3, 2, stick, planks, stick, stick, planks, stick), new ItemStack(set.fenceGate), "fence gate");
        Crafting.expect(c, Crafting.grid(2, 3, planks, planks, planks, planks, planks, planks), new ItemStack(set.doorItem, 3), "door");
        Crafting.expect(c, Crafting.grid(3, 2, planks, planks, planks, planks, planks, planks), new ItemStack(set.trapdoor, 2), "trapdoor");
        Crafting.expect(c, Crafting.grid(2, 1, planks, planks), new ItemStack(set.pressurePlate), "pressure plate");
        Crafting.expect(c, Crafting.grid(1, 1, planks), new ItemStack(set.button), "button");
        Crafting.expect(c, Crafting.grid(3, 3, planks, planks, planks, planks, planks, planks, none, stick, none),
                new ItemStack(set.signItem, 3), "sign");
        // Vanilla's wooden recipes take any plankWood, so cherry planks make the everyday wooden things.
        Crafting.expect(c, Crafting.grid(2, 2, planks, planks, planks, planks), new ItemStack(Blocks.CRAFTING_TABLE), "crafting table from cherry planks");
        Crafting.expect(c, Crafting.grid(1, 2, planks, planks), new ItemStack(Items.STICK, 4), "sticks from cherry planks");
        Crafting.expect(c, Crafting.grid(3, 3, planks, planks, planks, planks, none, planks, planks, planks, planks),
                new ItemStack(Blocks.CHEST), "chest from cherry planks");
        ItemStack oak = new ItemStack(Blocks.PLANKS);
        Crafting.expect(c, Crafting.grid(3, 2, oak, oak, oak, oak, oak, oak), new ItemStack(Blocks.TRAPDOOR, 2), "oak planks still make the oak trapdoor");
        Crafting.expect(c, Crafting.grid(1, 1, oak), new ItemStack(Blocks.WOODEN_BUTTON), "oak planks still make the oak button");
        Crafting.expect(c, Crafting.grid(1, 1, new ItemStack(CherryContent.petals)),
                new ItemStack(Items.DYE, 1, EnumDyeColor.PINK.getDyeDamage()), "pink petals -> pink dye");

        c.eq(300, TileEntityFurnace.getItemBurnTime(planks), "planks burn 300");
        c.eq(300, TileEntityFurnace.getItemBurnTime(log), "log burns 300");
        c.eq(150, TileEntityFurnace.getItemBurnTime(new ItemStack(set.slab)), "slab burns 150");
        c.eq(200, TileEntityFurnace.getItemBurnTime(new ItemStack(set.doorItem)), "door burns 200");
        c.eq(200, TileEntityFurnace.getItemBurnTime(new ItemStack(set.signItem)), "sign burns 200");
        c.eq(100, TileEntityFurnace.getItemBurnTime(new ItemStack(set.button)), "button burns 100");
        c.eq(100, TileEntityFurnace.getItemBurnTime(new ItemStack(CherryContent.sapling)), "sapling burns 100");
    }

    private void stripping(Check c, WoodSet set) {
        BlockPos area = c.area();
        BlockPos at = area.add(4, 0, 4);
        c.world.setBlockState(at, set.log.getDefaultState().withProperty(BlockRotatedPillar.AXIS, EnumFacing.Axis.X));
        EntityPlayerMP player = Players.real(c.world, "stripper");
        player.setPosition(at.getX() + 0.5, at.getY(), at.getZ() - 1.5);
        ItemStack axe = new ItemStack(Items.IRON_AXE);
        player.setHeldItem(EnumHand.MAIN_HAND, axe);
        player.interactionManager.processRightClickBlock(player, c.world, axe, EnumHand.MAIN_HAND, at, EnumFacing.NORTH, 0.5F, 0.5F, 0.0F);
        IBlockState got = c.world.getBlockState(at);
        c.eq(set.strippedLog, got.getBlock(), "axe strips the log");
        c.eq(EnumFacing.Axis.X, got.getValue(BlockRotatedPillar.AXIS), "stripping keeps the axis");
        c.eq(1, axe.getItemDamage(), "stripping wears the axe");

        c.world.setBlockState(at, set.wood.getDefaultState());
        player.interactionManager.processRightClickBlock(player, c.world, axe, EnumHand.MAIN_HAND, at, EnumFacing.NORTH, 0.5F, 0.5F, 0.0F);
        c.eq(set.strippedWood, c.world.getBlockState(at).getBlock(), "axe strips wood");

        c.world.setBlockState(at, set.planks.getDefaultState());
        player.interactionManager.processRightClickBlock(player, c.world, axe, EnumHand.MAIN_HAND, at, EnumFacing.NORTH, 0.5F, 0.5F, 0.0F);
        c.eq(set.planks, c.world.getBlockState(at).getBlock(), "planks are not stripped");
    }

    private void blocksInUse(Check c, WoodSet set) {
        BlockPos area = c.area();
        EntityPlayerMP player = Players.real(c.world, "builder");
        player.setPosition(area.getX() + 8.5, area.getY(), area.getZ() + 2.5);
        player.rotationYaw = 0.0F; // looking south

        // Door: placed by its item, two halves, opened by a click, dropping one door.
        BlockPos floor = area.add(8, -1, 8);
        ItemStack doors = new ItemStack(set.doorItem, 4);
        player.setHeldItem(EnumHand.MAIN_HAND, doors);
        set.doorItem.onItemUse(player, c.world, floor, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
        BlockPos door = floor.up();
        c.eq(set.door, c.world.getBlockState(door).getBlock(), "door placed");
        c.eq(set.door, c.world.getBlockState(door.up()).getBlock(), "door's upper half");
        c.eq(3, doors.getCount(), "one door used");
        set.door.onBlockActivated(c.world, door.up(), c.world.getBlockState(door.up()), player, EnumHand.MAIN_HAND, EnumFacing.NORTH, 0.5F, 0.5F, 0.5F);
        c.eq(true, c.world.getBlockState(door).getValue(BlockDoor.OPEN), "clicking the top half opens the door");
        c.world.destroyBlock(door, true);
        c.eq(1, Drops.count(c.world, door, set.doorItem), "a broken door drops one door");
        c.ok(c.world.isAirBlock(door.up()), "both halves go");

        // Door opened by redstone.
        set.doorItem.onItemUse(player, c.world, floor, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
        c.world.setBlockState(door.west(), Blocks.REDSTONE_BLOCK.getDefaultState());
        c.eq(true, c.world.getBlockState(door).getValue(BlockDoor.OPEN), "redstone opens the door");
        c.world.setBlockToAir(door.west());
        c.eq(false, c.world.getBlockState(door).getValue(BlockDoor.OPEN), "and closes it");
        c.world.setBlockToAir(door.up());
        c.world.setBlockToAir(door);
        Drops.count(c.world, door, set.doorItem);

        // Fence gate: opens with a click, by redstone.
        BlockPos gate = area.add(12, 0, 8);
        c.world.setBlockState(gate, set.fenceGate.getDefaultState());
        set.fenceGate.onBlockActivated(c.world, gate, c.world.getBlockState(gate), player, EnumHand.MAIN_HAND, EnumFacing.NORTH, 0.5F, 0.5F, 0.5F);
        c.eq(true, c.world.getBlockState(gate).getValue(BlockFenceGate.OPEN), "gate opens");
        BlockPos gate2 = area.add(14, 0, 8);
        c.world.setBlockState(gate2, set.fenceGate.getDefaultState());
        c.world.setBlockState(gate2.east(), Blocks.REDSTONE_BLOCK.getDefaultState());
        c.eq(true, c.world.getBlockState(gate2).getValue(BlockFenceGate.OPEN), "redstone opens a gate");

        // Fence joins vanilla's oak fence.
        BlockPos fence = area.add(4, 0, 14);
        c.world.setBlockState(fence, set.fence.getDefaultState());
        c.world.setBlockState(fence.east(), Blocks.OAK_FENCE.getDefaultState());
        c.eq(true, set.fence.getActualState(c.world.getBlockState(fence), c.world, fence).getValue(BlockFence.EAST), "cherry fence joins an oak fence");

        // Slabs: a second slab on a slab makes the double slab, which drops two.
        BlockPos slab = area.add(4, 0, 18);
        ItemStack slabs = new ItemStack(set.slab, 4);
        player.setHeldItem(EnumHand.MAIN_HAND, slabs);
        slabs.onItemUse(player, c.world, slab.down(), EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
        c.eq(set.slab, c.world.getBlockState(slab).getBlock(), "slab placed");
        c.eq(BlockSlab.EnumBlockHalf.BOTTOM, c.world.getBlockState(slab).getValue(BlockSlab.HALF), "bottom slab");
        slabs.onItemUse(player, c.world, slab, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 0.5F, 0.5F);
        c.eq(set.doubleSlab, c.world.getBlockState(slab).getBlock(), "two slabs make the double slab");
        c.world.destroyBlock(slab, true);
        c.eq(2, Drops.count(c.world, slab, Item.getItemFromBlock(set.slab)), "double slab drops two");

        // Signs: standing on the floor, on a wall; both our tile.
        BlockPos signFloor = area.add(20, -1, 8);
        ItemStack signs = new ItemStack(set.signItem, 4);
        player.setHeldItem(EnumHand.MAIN_HAND, signs);
        set.signItem.onItemUse(player, c.world, signFloor, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
        c.eq(set.sign, c.world.getBlockState(signFloor.up()).getBlock(), "standing sign placed");
        TileEntity tile = c.world.getTileEntity(signFloor.up());
        c.ok(tile instanceof WoodSign.Tile, "standing sign has our sign tile: " + tile);
        c.eq(8, c.world.getBlockState(signFloor.up()).getValue(BlockStandingSign.ROTATION), "faces the player who looked south");
        BlockPos wall = area.add(22, 0, 8);
        c.world.setBlockState(wall, Blocks.STONE.getDefaultState());
        set.signItem.onItemUse(player, c.world, wall, EnumHand.MAIN_HAND, EnumFacing.NORTH, 0.5F, 0.5F, 0.0F);
        c.eq(set.wallSign, c.world.getBlockState(wall.north()).getBlock(), "wall sign placed");
        c.ok(c.world.getTileEntity(wall.north()) instanceof WoodSign.Tile, "wall sign has our tile");
        c.eq(2, signs.getCount(), "two signs used");
        c.world.destroyBlock(wall.north(), true);
        c.eq(1, Drops.count(c.world, wall.north(), set.signItem), "a wall sign drops the sign item");
        c.world.setBlockToAir(wall);
        c.world.destroyBlock(signFloor.up(), true);
        Drops.count(c.world, signFloor.up(), set.signItem);

        // Pressure plate and button work as wooden ones.
        BlockPos button = area.add(26, 0, 8);
        c.world.setBlockState(button, set.button.getStateForPlacement(c.world, button, EnumFacing.UP, 0.5F, 0F, 0.5F, 0, player, EnumHand.MAIN_HAND));
        set.button.onBlockActivated(c.world, button, c.world.getBlockState(button), player, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 0.5F, 0.5F);
        c.ok(c.world.isBlockPowered(button.down()) || c.world.getRedstonePower(button.down(), EnumFacing.DOWN) > 0
                || set.button.getWeakPower(c.world.getBlockState(button), c.world, button, EnumFacing.UP) > 0, "pressed button powers");
    }

    private void trees(Check c) {
        BlockPos area = c.area();
        Random rand = new Random(2030L);
        int grown = 0;
        int minLogs = Integer.MAX_VALUE;
        int maxLogs = 0;
        int minLeaves = Integer.MAX_VALUE;
        int unreachable = 0;
        boolean sawHanging = false;
        for (int i = 0; i < 9; i++) {
            BlockPos base = c.area().add(16, 0, 16);
            c.world.setBlockState(base.down(), Blocks.GRASS.getDefaultState());
            if (!new CherryTree(false).generate(c.world, rand, base)) {
                continue;
            }
            grown++;
            int logs = 0;
            int leaves = 0;
            for (BlockPos p : BlockPos.getAllInBox(base.add(-10, 0, -10), base.add(10, 14, 10))) {
                IBlockState s = c.world.getBlockState(p);
                if (s.getBlock() == CherryContent.set.log) {
                    logs++;
                } else if (s.getBlock() == CherryContent.leaves) {
                    leaves++;
                    if (!s.getValue(BlockLeaves.DECAYABLE) || s.getValue(BlockLeaves.CHECK_DECAY)) {
                        c.fail("generated leaves should be decayable and settled: " + s);
                    }
                    if (!reaches(c, p)) {
                        unreachable++;
                    }
                    IBlockState above = c.world.getBlockState(p.up());
                    IBlockState below = c.world.getBlockState(p.down());
                    if (below.getBlock() == Blocks.AIR && above.getBlock() == CherryContent.leaves && c.world.isAirBlock(p.down())
                            && c.world.getBlockState(p.north()).getBlock() != CherryContent.leaves
                            && c.world.getBlockState(p.south()).getBlock() != CherryContent.leaves) {
                        sawHanging = true;
                    }
                }
            }
            minLogs = Math.min(minLogs, logs);
            maxLogs = Math.max(maxLogs, logs);
            minLeaves = Math.min(minLeaves, leaves);
            c.eq(Blocks.DIRT, c.world.getBlockState(base.down()).getBlock(), "grass under a tree becomes dirt");
        }
        c.eq(9, grown, "every tree grows in open space");
        c.ok(minLogs >= 8, "a cherry tree has a trunk and a branch at least: " + minLogs + ".." + maxLogs + " logs");
        c.ok(maxLogs <= 30, "and not too many logs: " + maxLogs);
        c.ok(minLeaves >= 40, "a full canopy: at least " + minLeaves + " leaves");
        c.eq(0, unreachable, "every generated leaf is within 1.20's decay distance of a log");
        c.ok(sawHanging, "some leaves hang below the canopy");

        // A tree needs room: a stone roof two above the sapling stops it.
        BlockPos base = area.add(4, 0, 4);
        c.world.setBlockState(base.down(), Blocks.GRASS.getDefaultState());
        c.world.setBlockState(base.up(3), Blocks.STONE.getDefaultState());
        c.ok(!new CherryTree(false).generate(c.world, rand, base), "no tree under a low roof");
        c.world.setBlockState(base.up(3), Blocks.AIR.getDefaultState());

        // The sapling grows in two stages by bone meal (forced here), then into a tree.
        BlockCherrySapling sapling = CherryContent.sapling;
        c.world.setBlockState(base, sapling.getDefaultState());
        sapling.grow(c.world, rand, base, c.world.getBlockState(base));
        c.eq(1, c.world.getBlockState(base).getValue(BlockCherrySapling.STAGE), "first growth: stage 1");
        sapling.grow(c.world, rand, base, c.world.getBlockState(base));
        c.eq(CherryContent.set.log, c.world.getBlockState(base).getBlock(), "second growth: a tree");
        c.ok(!sapling.canPlaceBlockAt(c.world, area.add(10, 0, 10)), "sapling not on stone");
    }

    private static boolean reaches(Check c, BlockPos p) {
        try {
            java.lang.reflect.Method m = BlockCherryLeaves.class.getDeclaredMethod("reachesLog", net.minecraft.world.World.class, BlockPos.class);
            m.setAccessible(true);
            return (Boolean) m.invoke(null, c.world, p);
        } catch (ReflectiveOperationException e) {
            c.fail("reachesLog: " + e);
            return true;
        }
    }

    private void leaves(Check c, WoodSet set) {
        BlockPos area = c.area();
        BlockCherryLeaves leaves = CherryContent.leaves;
        IBlockState natural = leaves.getDefaultState().withProperty(BlockLeaves.CHECK_DECAY, false);
        BlockPos log = area.add(2, 0, 10);
        c.world.setBlockState(log, set.log.getDefaultState());
        for (int i = 1; i <= 7; i++) {
            c.world.setBlockState(log.east(i), natural, 2);
        }
        // Breaking a log makes them check; here the flag is set by hand on the 5th, 6th and 7th.
        Random rand = new Random(1L);
        for (int i : new int[] {5, 6, 7}) {
            BlockPos p = log.east(i);
            c.world.setBlockState(p, natural.withProperty(BlockLeaves.CHECK_DECAY, true), 2);
            leaves.updateTick(c.world, p, c.world.getBlockState(p), rand);
        }
        c.eq(leaves, c.world.getBlockState(log.east(5)).getBlock(), "leaf 5 from a log stays (1.12.2's would decay)");
        c.eq(false, c.world.getBlockState(log.east(5)).getValue(BlockLeaves.CHECK_DECAY), "and stops checking");
        c.eq(leaves, c.world.getBlockState(log.east(6)).getBlock(), "leaf 6 from a log stays");
        c.ok(c.world.isAirBlock(log.east(7)), "leaf 7 from a log decays");
        Drops.count(c.world, log.east(7), Item.getItemFromBlock(CherryContent.sapling));

        // Breaking the log tells leaves to check, out to 7.
        c.world.setBlockToAir(log);
        c.eq(true, c.world.getBlockState(log.east(6)).getValue(BlockLeaves.CHECK_DECAY), "breaking a log reaches leaves 6 away");

        // A player's leaves never decay.
        IBlockState placed = leaves.getStateForPlacement(c.world, log, EnumFacing.UP, 0, 0, 0, 0, Players.real(c.world, "leafer"), EnumHand.MAIN_HAND);
        c.eq(false, placed.getValue(BlockLeaves.DECAYABLE), "placed leaves are persistent");

        // Drops: 1.20's sapling and stick chances, no apples.
        int saplings = 0;
        int sticks = 0;
        int stickDrops = 0;
        int apples = 0;
        int n = 20000;
        for (int i = 0; i < n; i++) {
            NonNullList<ItemStack> drops = NonNullList.create();
            leaves.getDrops(drops, c.world, log, natural, 0);
            for (ItemStack s : drops) {
                if (s.getItem() == Item.getItemFromBlock(CherryContent.sapling)) {
                    saplings++;
                } else if (s.getItem() == Items.STICK) {
                    sticks += s.getCount();
                    stickDrops++;
                } else if (s.getItem() == Items.APPLE) {
                    apples++;
                }
            }
        }
        c.ok(saplings > n * 0.04 && saplings < n * 0.06, "saplings ~5%: " + saplings + "/" + n);
        c.ok(stickDrops > n * 0.015 && stickDrops < n * 0.025, "sticks 2% of the time: " + stickDrops + "/" + n);
        c.ok(sticks >= stickDrops && sticks <= 2 * stickDrops, "1 or 2 at a time: " + sticks);
        c.eq(0, apples, "no apples");
        c.eq(1, leaves.onSheared(new ItemStack(Items.SHEARS), c.world, log, 0).size(), "shears give the leaves");
    }

    private void petals(Check c) {
        BlockPos area = c.area();
        BlockPinkPetals petals = CherryContent.petals;
        Item item = Item.getItemFromBlock(petals);
        BlockPos ground = area.add(10, -1, 10);
        c.world.setBlockState(ground, Blocks.GRASS.getDefaultState());
        EntityPlayerMP player = Players.real(c.world, "gardener");
        player.setPosition(ground.getX() + 0.5, ground.getY() + 1, ground.getZ() - 2.5);
        player.rotationYaw = 0.0F; // facing south
        ItemStack stack = new ItemStack(item, 10);
        player.setHeldItem(EnumHand.MAIN_HAND, stack);
        stack.onItemUse(player, c.world, ground, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
        BlockPos at = ground.up();
        IBlockState s = c.world.getBlockState(at);
        c.eq(petals, s.getBlock(), "petals placed on grass");
        c.eq(EnumFacing.NORTH, s.getValue(BlockPinkPetals.FACING), "petals face back toward the player");
        c.eq(1, s.getValue(BlockPinkPetals.AMOUNT), "one petal");
        c.eq(new AxisAlignedBB(0, 0, 0, 0.5, 3 / 16.0, 0.5), s.getBoundingBox(c.world, at), "one petal fills the north-west quadrant");
        for (int i = 2; i <= 4; i++) {
            stack.onItemUse(player, c.world, at, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 0.1F, 0.5F);
            c.eq(i, c.world.getBlockState(at).getValue(BlockPinkPetals.AMOUNT), "adding petal " + i);
        }
        c.eq(6, stack.getCount(), "four petals used");
        c.eq(new AxisAlignedBB(0, 0, 0, 1, 3 / 16.0, 1), c.world.getBlockState(at).getBoundingBox(c.world, at), "four petals fill the block");
        stack.onItemUse(player, c.world, at, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 0.1F, 0.5F);
        c.eq(4, c.world.getBlockState(at).getValue(BlockPinkPetals.AMOUNT), "a full block takes no more");

        // Bone meal: adds one, and on a full block drops one.
        petals.grow(c.world, new Random(), at, c.world.getBlockState(at));
        c.eq(1, Drops.count(c.world, at, item), "bone meal on four petals drops one");
        c.world.setBlockState(at, petals.getDefaultState().withProperty(BlockPinkPetals.AMOUNT, 2));
        petals.grow(c.world, new Random(), at, c.world.getBlockState(at));
        c.eq(3, c.world.getBlockState(at).getValue(BlockPinkPetals.AMOUNT), "bone meal adds a petal");
        c.world.destroyBlock(at, true);
        c.eq(3, Drops.count(c.world, at, item), "three petals drop three");
        c.ok(!petals.canPlaceBlockAt(c.world, area.add(3, 0, 3)), "petals not on stone");

        // Every facing's outline for two petals is a half block on the expected side.
        IBlockState two = petals.getDefaultState().withProperty(BlockPinkPetals.AMOUNT, 2);
        c.eq(new AxisAlignedBB(0, 0, 0, 0.5, 3 / 16.0, 1), two.withProperty(BlockPinkPetals.FACING, EnumFacing.NORTH).getBoundingBox(c.world, at), "two petals facing north: west half");
        c.eq(new AxisAlignedBB(0, 0, 0, 1, 3 / 16.0, 0.5), two.withProperty(BlockPinkPetals.FACING, EnumFacing.EAST).getBoundingBox(c.world, at), "two petals facing east: north half");
    }

    private void pot(Check c) {
        BlockPos area = c.area();
        BlockPos pot = area.add(5, 0, 5);
        c.world.setBlockState(pot, Blocks.FLOWER_POT.getDefaultState());
        EntityPlayerMP player = Players.real(c.world, "potter");
        player.setPosition(pot.getX() + 0.5, pot.getY(), pot.getZ() - 1.5);
        ItemStack saplings = new ItemStack(CherryContent.sapling, 2);
        player.setHeldItem(EnumHand.MAIN_HAND, saplings);
        player.interactionManager.processRightClickBlock(player, c.world, saplings, EnumHand.MAIN_HAND, pot, EnumFacing.UP, 0.5F, 0.3F, 0.5F);
        c.eq(CherryContent.pottedSapling, c.world.getBlockState(pot).getBlock(), "sapling goes into an empty pot");
        c.eq(1, saplings.getCount(), "one sapling used");
        player.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
        player.interactionManager.processRightClickBlock(player, c.world, ItemStack.EMPTY, EnumHand.MAIN_HAND, pot, EnumFacing.UP, 0.5F, 0.3F, 0.5F);
        c.eq(Blocks.FLOWER_POT, c.world.getBlockState(pot).getBlock(), "empty hand takes it out");
        c.eq(Item.getItemFromBlock(CherryContent.sapling), player.getHeldItemMainhand().getItem(), "and gives the sapling back");
        c.world.setBlockState(pot, CherryContent.pottedSapling.getDefaultState());
        c.world.destroyBlock(pot, true);
        c.eq(1, Drops.count(c.world, pot, Items.FLOWER_POT) + 0, "broken potted sapling drops the pot");
    }

    /** Game Drops Backport's camps: cherry_checked and cherry_bees_005 grow this mod's cherry through Core's JigsawPools. */
    private static void jigsawFeatures(Check c, WoodSet set) {
        for (String id : new String[] {"minecraft:cherry_checked", "minecraft:cherry_bees_005"}) {
            Object placer;
            try {
                java.lang.reflect.Method m = com.exiledradio.backportcore.structure.JigsawPools.class.getDeclaredMethod("featurePlacer",
                        com.google.gson.JsonElement.class);
                m.setAccessible(true);
                placer = m.invoke(null, new com.google.gson.JsonPrimitive(id));
            } catch (ReflectiveOperationException e) {
                c.ok(false, id + ": JigsawPools.featurePlacer reachable: " + e);
                continue;
            }
            if (!c.ok(placer != null, id + " has a placer")) {
                continue;
            }
            BlockPos ground = new BlockPos(id.endsWith("checked") ? 2200 : 2240, 80, 2200);
            for (BlockPos p : BlockPos.getAllInBox(ground.add(-6, 0, -6), ground.add(6, 16, 6))) {
                c.world.setBlockState(p, p.getY() == 80 ? Blocks.GRASS.getDefaultState() : Blocks.AIR.getDefaultState(), 2);
            }
            ((com.exiledradio.backportcore.structure.JigsawPools.FeaturePlacer) placer).place(c.world, ground.up(), new java.util.Random(3L),
                    new com.google.gson.JsonPrimitive(id));
            c.eq(set.log, c.world.getBlockState(ground.up()).getBlock(), id + " grows a cherry log on grass");
            c.world.setBlockState(ground, Blocks.STONE.getDefaultState(), 2);
            for (BlockPos p : BlockPos.getAllInBox(ground.add(-6, 1, -6), ground.add(6, 16, 6))) {
                c.world.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
            }
            ((com.exiledradio.backportcore.structure.JigsawPools.FeaturePlacer) placer).place(c.world, ground.up(), new java.util.Random(3L),
                    new com.google.gson.JsonPrimitive(id));
            c.ok(c.world.isAirBlock(ground.up()), id + " places nothing where a sapling could not stand (stone)");
        }
    }
}
