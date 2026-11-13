package com.exiledradio.trailsandtalesbackport.selftest;

import com.exiledradio.trailsandtalesbackport.archaeology.ArchaeologyContent;
import com.exiledradio.trailsandtalesbackport.archaeology.ArchaeologyLoot;
import com.exiledradio.trailsandtalesbackport.archaeology.BlockDecoratedPot;
import com.exiledradio.trailsandtalesbackport.archaeology.BlockSuspicious;
import com.exiledradio.trailsandtalesbackport.archaeology.ItemDecoratedPot;
import com.exiledradio.trailsandtalesbackport.archaeology.TileDecoratedPot;
import com.exiledradio.trailsandtalesbackport.archaeology.TileSuspicious;
import com.exiledradio.trailsandtalesbackport.common.Content;
import com.exiledradio.trailsandtalesbackport.common.Owners;

import net.minecraft.advancements.Advancement;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.oredict.OreDictionary;

import java.util.List;
import java.util.Random;

/** Stream A: brush, suspicious sand and gravel, sherds, decorated pot, archaeology loot and advancements. */
public final class ArchaeologySelfTest implements SelfTest {

    @Override
    public String name() {
        return "archaeology";
    }

    @Override
    public void run(Check c) throws Exception {
        if (!c.ok(ArchaeologyContent.suspiciousSand != null && ArchaeologyContent.decoratedPot != null
                && ArchaeologyContent.brush != null, "archaeology registered")) {
            return;
        }
        registration(c);
        recipes(c);
        lootTables(c);
        brushing(c);
        reset(c);
        falling(c);
        pot(c);
        potBreaking(c);
    }

    // ---------------------------------------------------------------------------------------------

    private void registration(Check c) {
        BlockSuspicious sand = ArchaeologyContent.suspiciousSand;
        c.eq(4, sand.getBlockState().getValidStates().size(), "suspicious sand: dusted 0-3");
        for (IBlockState s : sand.getBlockState().getValidStates()) {
            c.eq(s, sand.getStateFromMeta(sand.getMetaFromState(s)), "dusted round trip " + s);
        }
        c.eq(Blocks.SAND.getDefaultState(), sand.turnsInto(), "sand turns into sand");
        c.eq(Blocks.GRAVEL.getDefaultState(), ArchaeologyContent.suspiciousGravel.turnsInto(), "gravel turns into gravel");
        c.eq(0.25F, sand.getBlockHardness(sand.getDefaultState(), c.world, BlockPos.ORIGIN), "hardness 0.25");
        c.eq("shovel", sand.getHarvestTool(sand.getDefaultState()), "mined with a shovel");
        c.eq(EnumPushReaction.DESTROY, sand.getPushReaction(sand.getDefaultState()), "pistons break it");
        BlockDecoratedPot pot = ArchaeologyContent.decoratedPot;
        c.eq(8, pot.getBlockState().getValidStates().size(), "pot: 4 facings x cracked");
        for (IBlockState s : pot.getBlockState().getValidStates()) {
            c.eq(s, pot.getStateFromMeta(pot.getMetaFromState(s)), "pot round trip " + s);
        }
        c.eq(0.0F, pot.getBlockHardness(pot.getDefaultState(), c.world, BlockPos.ORIGIN), "pot breaks instantly");
        c.eq(20, ArchaeologyContent.SHERD_ITEMS.size(), "20 sherds");
        for (Item sherd : ArchaeologyContent.SHERD_ITEMS.values()) {
            c.eq(EnumRarity.UNCOMMON, sherd.getRarity(new ItemStack(sherd)), "uncommon " + sherd.getRegistryName());
            c.ok(OreDictionary.getOres("potterySherd").stream().anyMatch(s -> s.getItem() == sherd), "potterySherd " + sherd.getRegistryName());
        }
        c.eq(64, ArchaeologyContent.brush.getMaxDamage(), "brush durability 64");
        c.eq(1, ArchaeologyContent.brush.getItemStackLimit(), "brush stacks to 1");
        c.eq(200, ArchaeologyContent.brush.getMaxItemUseDuration(new ItemStack(ArchaeologyContent.brush)), "use lasts 200 ticks");
    }

    private void recipes(Check c) {
        ItemStack copper;
        if (OreDictionary.getOres("ingotCopper").isEmpty()) {
            // No copper mod in the dev environment: a test-only ingotCopper so the recipe can be checked.
            OreDictionary.registerOre("ingotCopper", new ItemStack(Items.IRON_INGOT));
        }
        copper = OreDictionary.getOres("ingotCopper").get(0).copy();
        ItemStack e = ItemStack.EMPTY;
        Crafting.expect(c, Crafting.grid(3, 3, e, new ItemStack(Items.FEATHER), e, e, copper, e, e, new ItemStack(Items.STICK), e),
                new ItemStack(ArchaeologyContent.brush), "brush: feather, copper, stick");
        ItemStack brick = new ItemStack(Items.BRICK);
        Crafting.expect(c, Crafting.grid(3, 3, e, brick, e, brick, e, brick, e, brick, e),
                new ItemStack(ArchaeologyContent.potItem), "four bricks: a plain pot");
        Item angler = ArchaeologyContent.sherd("angler");
        Item heart = ArchaeologyContent.sherd("heart");
        ItemStack result = craft(c, Crafting.grid(3, 3, e, new ItemStack(angler), e, brick, e, new ItemStack(heart), e,
                new ItemStack(Items.BRICK), e));
        Item[] faces = ItemDecoratedPot.sherds(result);
        c.ok(result.getItem() == ArchaeologyContent.potItem, "sherd pot crafted");
        c.eq(angler, faces[0], "top of the diamond is the back");
        c.eq(Items.BRICK, faces[1], "left");
        c.eq(heart, faces[2], "right");
        c.eq(Items.BRICK, faces[3], "bottom is the front");
        c.ok(craft(c, Crafting.grid(3, 3, brick, brick, e, brick, e, brick, e, e, e)).isEmpty(), "not a diamond: no pot");
        c.ok(craft(c, Crafting.grid(3, 3, e, brick, e, brick, brick, brick, e, brick, e)).isEmpty(), "five items: no pot");
        c.ok(craft(c, Crafting.grid(3, 3, e, brick, e, brick, e, new ItemStack(Items.STICK), e, brick, e)).isEmpty(), "a stick: no pot");
        c.ok(ItemDecoratedPot.sherds(new ItemStack(ArchaeologyContent.potItem))[0] == Items.BRICK, "plain pot faces are bricks");
        c.ok(!new ItemStack(ArchaeologyContent.potItem).hasTagCompound(), "plain pot item has no NBT");
    }

    private static ItemStack craft(Check c, net.minecraft.inventory.InventoryCrafting grid) {
        return net.minecraft.item.crafting.CraftingManager.findMatchingResult(grid, c.world);
    }

    private void lootTables(Check c) {
        int[] sizes = {8, 5, 10, 9, -1, 7};
        for (int i = 0; i < ArchaeologyLoot.TABLES.length; i++) {
            ResourceLocation name = ArchaeologyLoot.TABLES[i];
            LootTable table = c.world.getLootTableManager().getLootTableFromLocation(name);
            LootPool pool = table.getPool("main");
            if (!c.ok(pool != null, name + " loaded")) {
                continue;
            }
            int expect = sizes[i];
            if (name.equals(ArchaeologyLoot.DESERT_WELL) && Owners.theirs(Owners.VILLAGE_AND_PILLAGE, "suspicious_stew") != null) {
                expect++;
            }
            if (name.equals(ArchaeologyLoot.TRAIL_RUINS_COMMON)) {
                expect = 25 + (Owners.ours("oak_hanging_sign") != null ? 2 : 0)
                        + (Owners.present(Owners.CAVES_AND_CLIFFS) ? 4 : 0);
                c.ok(pool.getEntry("blue_dye") != null && pool.getEntry("white_dye") != null, "blue and white dye in common trail ruins");
            }
            int n = 0;
            Random r = new Random(1);
            for (int k = 0; k < 400; k++) {
                List<ItemStack> roll = table.generateLootForPools(r, new net.minecraft.world.storage.loot.LootContext.Builder(c.world).build());
                c.ok(roll.size() == 1, name + " rolls one item");
                if (k == 0) {
                    n = roll.size();
                }
            }
            c.eq(1, n, name + " gives one item");
            c.eq(expect, countEntries(pool, name), name + " entries");
        }
        // Every sherd comes from some table.
        for (String s : ArchaeologyContent.SHERDS) {
            boolean found = false;
            for (ResourceLocation name : ArchaeologyLoot.TABLES) {
                LootPool pool = c.world.getLootTableManager().getLootTableFromLocation(name).getPool("main");
                found |= pool != null && pool.getEntry(s + "_pottery_sherd") != null;
            }
            c.ok(found, s + " sherd is in a table");
        }
    }

    /** LootPool has no entry list accessor: count the 1.20.6 entry names it answers to. */
    private static int countEntries(LootPool pool, ResourceLocation name) {
        String[] all = {"archer_pottery_sherd", "miner_pottery_sherd", "prize_pottery_sherd", "skull_pottery_sherd", "diamond",
                "tnt", "gunpowder", "emerald", "arms_up_pottery_sherd", "brewer_pottery_sherd", "brick", "stick",
                "suspicious_stew", "blade_pottery_sherd", "explorer_pottery_sherd", "mourner_pottery_sherd",
                "plenty_pottery_sherd", "iron_axe", "wheat", "wooden_hoe", "coal", "gold_nugget", "angler_pottery_sherd",
                "shelter_pottery_sherd", "snort_pottery_sherd", "sniffer_egg", "clay", "yellow_dye", "blue_dye",
                "light_blue_dye", "white_dye", "orange_dye", "red_candle", "green_candle", "purple_candle", "brown_candle",
                "magenta_stained_glass_pane", "pink_stained_glass_pane", "blue_stained_glass_pane",
                "light_blue_stained_glass_pane", "red_stained_glass_pane", "yellow_stained_glass_pane",
                "purple_stained_glass_pane", "spruce_hanging_sign", "oak_hanging_sign", "wheat_seeds", "beetroot_seeds",
                "dead_bush", "flower_pot", "string", "lead", "burn_pottery_sherd", "danger_pottery_sherd",
                "friend_pottery_sherd", "heart_pottery_sherd", "heartbreak_pottery_sherd", "howl_pottery_sherd",
                "sheaf_pottery_sherd", "wayfinder_armor_trim_smithing_template", "raiser_armor_trim_smithing_template",
                "shaper_armor_trim_smithing_template", "host_armor_trim_smithing_template", "music_disc_relic"};
        int n = 0;
        for (String e : all) {
            if (pool.getEntry(e) != null) {
                n++;
            }
        }
        return n;
    }

    // ---------------------------------------------------------------------------------------------

    /** A player standing south of the block, looking straight at its south face. */
    private static EntityPlayerMP brusher(Check c, BlockPos pos, String name) {
        EntityPlayerMP player = Players.real(c.world, name);
        player.setPositionAndRotation(pos.getX() + 0.5D, pos.getY() + 0.5D - player.getEyeHeight(), pos.getZ() + 2.5D, 180.0F, 0.0F);
        player.prevRotationYaw = 180.0F;
        player.rotationYawHead = 180.0F;
        ItemStack brush = new ItemStack(ArchaeologyContent.brush);
        player.setHeldItem(EnumHand.MAIN_HAND, brush);
        player.setActiveHand(EnumHand.MAIN_HAND);
        return player;
    }

    /** Places a suspicious block the way a structure does: by name, then the loot table in its NBT. */
    private static TileSuspicious placeByName(Check c, BlockPos pos, String block, ResourceLocation table, long seed) {
        c.world.setBlockState(pos, Content.id(block).equals(ArchaeologyContent.suspiciousSand.getRegistryName())
                ? ArchaeologyContent.suspiciousSand.getDefaultState() : ArchaeologyContent.suspiciousGravel.getDefaultState(), 2);
        TileEntity te = c.world.getTileEntity(pos);
        NBTTagCompound tag = te.writeToNBT(new NBTTagCompound());
        tag.setString("LootTable", table.toString());
        tag.setLong("LootTableSeed", seed);
        te.readFromNBT(tag);
        return (TileSuspicious) te;
    }

    /** Ticks the brush through its use, one world tick each, for {@code ticks} ticks. */
    private static void useBrush(Check c, EntityPlayerMP player, int fromElapsed, int ticks) {
        ItemStack stack = player.getHeldItemMainhand();
        for (int elapsed = fromElapsed; elapsed < fromElapsed + ticks; elapsed++) {
            c.world.getWorldInfo().setWorldTotalTime(c.world.getTotalWorldTime() + 1);
            stack.getItem().onUsingTick(stack, player, 200 - elapsed + 1);
        }
    }

    private void brushing(Check c) {
        BlockPos area = c.area();
        BlockPos pos = area.add(8, 2, 8);
        c.world.setBlockState(pos.down(), Blocks.STONE.getDefaultState(), 2);
        TileSuspicious te = placeByName(c, pos, "suspicious_sand", ArchaeologyLoot.DESERT_PYRAMID, 1234L);
        c.eq(ArchaeologyLoot.DESERT_PYRAMID, te.lootTable(), "loot table set through NBT");
        NBTTagCompound saved = te.writeToNBT(new NBTTagCompound());
        c.eq(ArchaeologyLoot.DESERT_PYRAMID.toString(), saved.getString("LootTable"), "LootTable saved");
        c.eq(1234L, saved.getLong("LootTableSeed"), "LootTableSeed saved");

        EntityPlayerMP player = brusher(c, pos, "brusher");
        Advancement adv = c.world.getMinecraftServer().getAdvancementManager().getAdvancement(ArchaeologyContent.SALVAGE_SHERD);
        c.ok(adv != null, "Respecting the Remnants loaded");

        useBrush(c, player, 1, 5);
        c.eq(1, te.brushCount(), "first stroke on tick 5");
        c.eq(1, state(c, pos).getValue(BlockSuspicious.DUSTED), "dusted 1 after one brush");
        c.eq(EnumFacing.SOUTH, te.hitDirection(), "brushed from the south face");
        c.ok(te.lootTable() == null && !te.item().isEmpty(), "first brush rolls the loot");
        ItemStack found = te.item().copy();
        NBTTagCompound after = te.writeToNBT(new NBTTagCompound());
        c.ok(!after.hasKey("LootTable") && after.hasKey("item"), "item saved instead of the table");
        if (adv != null) {
            c.ok(player.getAdvancements().getProgress(adv).getCriterionProgress("desert_pyramid").isObtained(),
                    "desert_pyramid criterion on rolling its table");
        }
        useBrush(c, player, 6, 20);
        c.eq(3, te.brushCount(), "three strokes by tick 25");
        c.eq(2, state(c, pos).getValue(BlockSuspicious.DUSTED), "dusted 2 at three brushes");
        useBrush(c, player, 26, 30);
        c.eq(6, te.brushCount(), "six by tick 55");
        c.eq(3, state(c, pos).getValue(BlockSuspicious.DUSTED), "dusted 3 at six brushes");
        useBrush(c, player, 56, 39);
        c.eq(ArchaeologyContent.suspiciousSand, state(c, pos).getBlock(), "still suspicious at tick 94");
        useBrush(c, player, 95, 1);
        c.eq(Blocks.SAND.getDefaultState(), state(c, pos), "tenth stroke (tick 95, the 96th of brushing) leaves sand");
        c.eq(1, player.getHeldItemMainhand().getItemDamage(), "the brush lost one durability");
        List<EntityItem> drops = c.world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(pos.south()).grow(0.5));
        drops.removeIf(d -> d.isDead);
        c.ok(drops.size() == 1 && ItemStack.areItemsEqual(drops.get(0).getItem(), found), "the item dropped on the brushed side");
        if (!drops.isEmpty()) {
            EntityItem d = drops.get(0);
            c.eq(pos.getX() + 0.5D, d.posX, "drop x centered");
            c.eq(pos.getZ() + 1.5D, d.posZ, "drop z one block south");
            c.eq(0.0D, d.motionY, "drop does not move");
            d.setDead();
        }

        // Same seed, same item.
        TileSuspicious a = placeByName(c, pos.east(2), "suspicious_sand", ArchaeologyLoot.TRAIL_RUINS_RARE, 99L);
        TileSuspicious b = placeByName(c, pos.east(4), "suspicious_gravel", ArchaeologyLoot.TRAIL_RUINS_RARE, 99L);
        a.brush(c.world.getTotalWorldTime(), player, EnumFacing.UP);
        b.brush(c.world.getTotalWorldTime(), player, EnumFacing.UP);
        c.ok(ItemStack.areItemsEqual(a.item(), b.item()), "a seed gives the same item");
        c.ok(ArchaeologyContent.placeSuspicious(c.world, pos.east(6), true, ArchaeologyLoot.OCEAN_RUIN_COLD, 0L, 2), "placeSuspicious");
        c.eq(ArchaeologyContent.suspiciousGravel, state(c, pos.east(6)).getBlock(), "placed gravel");

        // Mined: nothing.
        BlockPos mined = pos.east(6);
        EntityPlayerMP miner = Players.real(c.world, "miner");
        miner.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.IRON_SHOVEL));
        miner.interactionManager.tryHarvestBlock(mined);
        c.eq(Blocks.AIR, state(c, mined).getBlock(), "mined");
        c.eq(0, c.world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(mined).grow(1.5)).size(), "drops nothing when mined");
        ItemStack silk = new ItemStack(Items.DIAMOND_SHOVEL);
        silk.addEnchantment(Enchantments.SILK_TOUCH, 1);
        placeByName(c, mined, "suspicious_gravel", ArchaeologyLoot.OCEAN_RUIN_COLD, 0L);
        miner.setHeldItem(EnumHand.MAIN_HAND, silk);
        miner.interactionManager.tryHarvestBlock(mined);
        c.eq(0, c.world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(mined).grow(1.5)).size(), "nothing with Silk Touch");
    }

    private void reset(Check c) {
        BlockPos area = c.area();
        BlockPos pos = area.add(8, 2, 8);
        c.world.setBlockState(pos.down(), Blocks.STONE.getDefaultState(), 2);
        TileSuspicious te = placeByName(c, pos, "suspicious_gravel", ArchaeologyLoot.OCEAN_RUIN_COLD, 5L);
        EntityPlayerMP player = brusher(c, pos, "resetter");
        useBrush(c, player, 1, 35);
        c.eq(4, te.brushCount(), "four strokes");
        c.eq(2, state(c, pos).getValue(BlockSuspicious.DUSTED), "dusted 2");
        long t = c.world.getTotalWorldTime();
        c.world.getWorldInfo().setWorldTotalTime(t + 39);
        te.checkReset();
        c.eq(4, te.brushCount(), "nothing lost before 40 ticks");
        c.world.getWorldInfo().setWorldTotalTime(t + 41);
        te.checkReset();
        c.eq(2, te.brushCount(), "2 lost once the 40 ticks are up");
        c.eq(1, state(c, pos).getValue(BlockSuspicious.DUSTED), "dusted back to 1");
        te.checkReset();
        c.eq(2, te.brushCount(), "then only every 4 ticks");
        c.world.getWorldInfo().setWorldTotalTime(t + 45);
        te.checkReset();
        c.eq(0, te.brushCount(), "back to 0");
        c.eq(0, state(c, pos).getValue(BlockSuspicious.DUSTED), "dusted 0");
        c.ok(te.hitDirection() == null, "brushed face forgotten");
        c.ok(!te.item().isEmpty(), "the rolled item stays");
    }

    private void falling(Check c) {
        BlockPos area = c.area();
        BlockPos pos = area.add(8, 10, 8);
        placeByName(c, pos, "suspicious_sand", ArchaeologyLoot.DESERT_WELL, 0L);
        BlockSuspicious sand = ArchaeologyContent.suspiciousSand;
        sand.updateTick(c.world, pos, state(c, pos), c.world.rand);
        List<EntityFallingBlock> falling = c.world.getEntitiesWithinAABB(EntityFallingBlock.class, new AxisAlignedBB(pos).grow(1));
        if (!c.ok(falling.size() == 1, "unsupported: it falls")) {
            return;
        }
        EntityFallingBlock e = falling.get(0);
        c.ok(!e.shouldDropItem, "its falling block drops nothing");
        for (int i = 0; i < 200 && !e.isDead; i++) {
            e.onUpdate();
        }
        c.ok(e.isDead, "landed");
        BlockPos floor = new BlockPos(pos.getX(), 100, pos.getZ());
        c.eq(Blocks.AIR, state(c, floor).getBlock(), "broke where it landed");
        c.eq(Blocks.AIR, state(c, pos).getBlock(), "gone from where it was");
        c.eq(0, c.world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(floor).grow(2)).size(), "dropped nothing");
        c.world.getEntitiesWithinAABB(EntityFallingBlock.class, new AxisAlignedBB(floor).grow(12)).forEach(f -> f.setDead());
    }

    // ---------------------------------------------------------------------------------------------

    private void pot(Check c) {
        BlockPos area = c.area();
        BlockPos pos = area.add(8, 0, 8);
        EntityPlayerMP player = Players.real(c.world, "potter");
        player.setPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() - 1.5);
        player.rotationYaw = 0.0F; // facing south

        Item angler = ArchaeologyContent.sherd("angler");
        ItemStack held = TileDecoratedPot.potItem(ArchaeologyContent.potItem,
                new Item[]{angler, Items.BRICK, Items.BRICK, ArchaeologyContent.sherd("skull")});
        player.setHeldItem(EnumHand.MAIN_HAND, held);
        player.interactionManager.processRightClickBlock(player, c.world, held, EnumHand.MAIN_HAND, pos.down(), EnumFacing.UP, 0.5F, 1.0F, 0.5F);
        c.eq(ArchaeologyContent.decoratedPot, state(c, pos).getBlock(), "pot placed");
        c.eq(EnumFacing.SOUTH, state(c, pos).getValue(BlockDecoratedPot.FACING), "faces the way the player faces");
        TileDecoratedPot pot = (TileDecoratedPot) c.world.getTileEntity(pos);
        c.eq(angler, pot.sherds()[0], "faces copied from the item: back");
        c.eq(ArchaeologyContent.sherd("skull"), pot.sherds()[3], "front");
        ItemStack pick = ArchaeologyContent.decoratedPot.getPickBlock(state(c, pos), null, c.world, pos, player);
        c.eq(angler, ItemDecoratedPot.sherds(pick)[0], "pick block keeps the faces");

        // Inserting by hand.
        ItemStack diamonds = new ItemStack(Items.DIAMOND, 3);
        player.setHeldItem(EnumHand.MAIN_HAND, diamonds);
        use(c, player, pos);
        c.eq(1, pot.getTheItem().getCount(), "one diamond goes in");
        c.eq(2, diamonds.getCount(), "one taken from the hand");
        use(c, player, pos);
        c.eq(2, pot.getTheItem().getCount(), "a second one");
        c.eq(1, ArchaeologyContent.decoratedPot.getComparatorInputOverride(state(c, pos), c.world, pos), "comparator 1 at 2 of 64");
        ItemStack emeralds = new ItemStack(Items.EMERALD, 5);
        player.setHeldItem(EnumHand.MAIN_HAND, emeralds);
        use(c, player, pos);
        c.eq(5, emeralds.getCount(), "another item does not go in");
        c.eq(Items.DIAMOND, pot.getTheItem().getItem(), "still diamonds");
        player.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
        use(c, player, pos);
        c.eq(2, pot.getTheItem().getCount(), "empty hand: nothing happens to the contents");
        c.ok(pot.receiveClientEvent(TileDecoratedPot.EVENT_POT_WOBBLES, 1), "wobble event handled");
        c.eq(TileDecoratedPot.Wobble.NEGATIVE, pot.lastWobble, "negative wobble");

        // Hoppers.
        IItemHandler h = pot.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.UP);
        ItemStack in = new ItemStack(Items.DIAMOND, 70);
        ItemStack left = h.insertItem(0, in, false);
        c.eq(64, pot.getTheItem().getCount(), "filled to a stack");
        c.eq(8, left.getCount(), "the rest is handed back");
        c.eq(70, in.getCount(), "the caller's stack untouched");
        c.eq(15, ArchaeologyContent.decoratedPot.getComparatorInputOverride(state(c, pos), c.world, pos), "comparator 15 when full");
        c.eq(5, h.insertItem(0, new ItemStack(Items.EMERALD, 5), false).getCount(), "a full pot takes nothing");
        c.eq(1, h.extractItem(0, 1, false).getCount(), "a hopper takes one out");
        c.eq(63, pot.getTheItem().getCount(), "63 left");

        // Saving.
        NBTTagCompound tag = pot.writeToNBT(new NBTTagCompound());
        c.eq(4, tag.getTagList("sherds", 8).tagCount(), "sherds saved");
        TileDecoratedPot copy = new TileDecoratedPot();
        copy.readFromNBT(tag);
        c.eq(angler, copy.sherds()[0], "sherds loaded");
        c.eq(63, copy.getTheItem().getCount(), "item loaded");

        // Broken by hand: itself with its faces, the contents spill.
        player.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
        player.interactionManager.tryHarvestBlock(pos);
        c.eq(Blocks.AIR, state(c, pos).getBlock(), "broken by hand");
        List<EntityItem> drops = c.world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(pos).grow(2));
        int pots = 0;
        int spilled = 0;
        for (EntityItem d : drops) {
            if (d.isDead) {
                continue;
            }
            if (d.getItem().getItem() == ArchaeologyContent.potItem) {
                pots++;
                c.eq(angler, ItemDecoratedPot.sherds(d.getItem())[0], "the dropped pot keeps its faces");
            } else if (d.getItem().getItem() == Items.DIAMOND) {
                spilled += d.getItem().getCount();
            }
            d.setDead();
        }
        c.eq(1, pots, "drops itself");
        c.eq(63, spilled, "its diamonds spill out");
    }

    private static void use(Check c, EntityPlayerMP player, BlockPos pos) {
        ItemStack held = player.getHeldItemMainhand();
        player.interactionManager.processRightClickBlock(player, c.world, held, EnumHand.MAIN_HAND, pos, EnumFacing.NORTH, 0.5F, 0.5F, 0.0F);
    }

    private void potBreaking(Check c) {
        BlockPos area = c.area();
        BlockPos pos = area.add(8, 0, 8);
        EntityPlayerMP player = Players.real(c.world, "breaker");
        Item heart = ArchaeologyContent.sherd("heart");
        Item[] faces = {heart, Items.BRICK, ArchaeologyContent.sherd("howl"), Items.BRICK};

        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
        placePot(c, pos, faces);
        player.interactionManager.tryHarvestBlock(pos);
        c.eq(1, Drops.count(c.world, pos, heart), "pickaxe: the heart sherd");

        placePot(c, pos, faces);
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.WOODEN_SWORD));
        player.interactionManager.tryHarvestBlock(pos);
        List<EntityItem> drops = c.world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(pos).grow(2));
        int bricks = 0;
        int sherds = 0;
        for (EntityItem d : drops) {
            if (d.isDead) {
                continue;
            }
            if (d.getItem().getItem() == Items.BRICK) {
                bricks += d.getItem().getCount();
            } else if (d.getItem().getItem() == heart || d.getItem().getItem() == ArchaeologyContent.sherd("howl")) {
                sherds++;
            } else if (d.getItem().getItem() == ArchaeologyContent.potItem) {
                c.fail("a sword should shatter the pot");
            }
            d.setDead();
        }
        c.eq(2, bricks, "sword: two bricks");
        c.eq(2, sherds, "sword: two sherds");

        ItemStack silk = new ItemStack(Items.DIAMOND_PICKAXE);
        silk.addEnchantment(Enchantments.SILK_TOUCH, 1);
        placePot(c, pos, faces);
        player.setHeldItem(EnumHand.MAIN_HAND, silk);
        player.interactionManager.tryHarvestBlock(pos);
        c.eq(1, Drops.count(c.world, pos, ArchaeologyContent.potItem), "Silk Touch: the pot itself");

        placePot(c, pos, faces);
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.STICK));
        player.interactionManager.tryHarvestBlock(pos);
        c.eq(1, Drops.count(c.world, pos, ArchaeologyContent.potItem), "a stick: the pot itself");

        // An arrow shatters it.
        placePot(c, pos, faces);
        EntityTippedArrow arrow = new EntityTippedArrow(c.world, pos.getX() + 0.5, pos.getY() + 3, pos.getZ() + 0.5);
        RayTraceResult ray = new RayTraceResult(new Vec3d(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5), EnumFacing.UP, pos);
        MinecraftForge.EVENT_BUS.post(new ProjectileImpactEvent.Arrow(arrow, ray));
        c.eq(Blocks.AIR, state(c, pos).getBlock(), "arrow: shattered");
        c.eq(1, Drops.count(c.world, pos, heart), "arrow: its sherds drop");

        c.eq(EnumPushReaction.DESTROY, ArchaeologyContent.decoratedPot.getPushReaction(ArchaeologyContent.decoratedPot.getDefaultState()),
                "pistons break it");
        c.ok(BlockDecoratedPot.breaksPots(new ItemStack(Items.GOLDEN_HOE)) && BlockDecoratedPot.breaksPots(new ItemStack(Items.STONE_AXE))
                && BlockDecoratedPot.breaksPots(new ItemStack(Items.DIAMOND_SHOVEL)) && !BlockDecoratedPot.breaksPots(new ItemStack(Items.SHEARS)),
                "breaks_decorated_pots: tools yes, shears no");

        // Careful Restoration.
        Advancement adv = c.world.getMinecraftServer().getAdvancementManager().getAdvancement(ArchaeologyContent.CAREFUL_RESTORATION);
        if (c.ok(adv != null, "Careful Restoration loaded")) {
            ItemStack e = ItemStack.EMPTY;
            ItemStack b = new ItemStack(Items.BRICK);
            ItemStack s = new ItemStack(heart);
            FMLCommonHandler.instance().firePlayerCraftingEvent(player, new ItemStack(ArchaeologyContent.potItem),
                    Crafting.grid(3, 3, e, s, e, s, e, b, e, s, e));
            c.ok(!player.getAdvancements().getProgress(adv).isDone(), "not with a brick in it");
            FMLCommonHandler.instance().firePlayerCraftingEvent(player, new ItemStack(ArchaeologyContent.potItem),
                    Crafting.grid(3, 3, e, s, e, s, e, s, e, s, e));
            c.ok(player.getAdvancements().getProgress(adv).isDone(), "four sherds: Careful Restoration");
        }
    }

    private static void placePot(Check c, BlockPos pos, Item[] faces) {
        c.world.setBlockState(pos, ArchaeologyContent.decoratedPot.getDefaultState(), 2);
        ((TileDecoratedPot) c.world.getTileEntity(pos)).setSherds(faces[0], faces[1], faces[2], faces[3]);
    }

    private static IBlockState state(Check c, BlockPos pos) {
        return c.world.getBlockState(pos);
    }
}
