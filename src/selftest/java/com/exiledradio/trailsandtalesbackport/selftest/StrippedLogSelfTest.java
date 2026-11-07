package com.exiledradio.trailsandtalesbackport.selftest;

import com.exiledradio.trailsandtalesbackport.common.Owners;
import com.exiledradio.trailsandtalesbackport.hangingsign.HangingWood;
import com.exiledradio.trailsandtalesbackport.woodcopy.StrippedLogCopies;
import com.exiledradio.trailsandtalesbackport.woodcopy.Wood;

import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockNewLog;
import net.minecraft.block.BlockOldLog;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;

/** The stripped log copies: present only without their owner, usable, and what hanging signs are made of. */
public final class StrippedLogSelfTest implements SelfTest {

    @Override
    public String name() {
        return "strippedlogs";
    }

    @Override
    public void run(Check c) {
        boolean owner = Owners.present(Owners.VILLAGE_AND_PILLAGE) || Owners.present(Owners.UPDATE_AQUATIC);
        if (owner) {
            c.ok(StrippedLogCopies.LOGS.isEmpty(), "no copies with an owner installed");
            c.eq(Owners.present(Owners.VILLAGE_AND_PILLAGE) ? "villagerbackport" : "updateaquaticbackport",
                    HangingWood.OAK.log().getItem().getRegistryName().getNamespace(), "hanging signs use the owner's stripped log");
            return;
        }
        c.eq(6, StrippedLogCopies.LOGS.size(), "six stripped logs");
        c.eq(6, StrippedLogCopies.BARK.size(), "six stripped woods");
        c.eq("trailsandtalesbackport:stripped_spruce_log", StrippedLogCopies.LOGS.get(Wood.SPRUCE).getRegistryName().toString(), "owner's names in our namespace");
        c.eq(StrippedLogCopies.LOGS.get(Wood.OAK).getRegistryName(), HangingWood.OAK.log().getItem().getRegistryName(), "hanging signs use our copy");

        // States: the owner's layout.
        for (Wood w : Wood.values()) {
            for (IBlockState s : StrippedLogCopies.LOGS.get(w).getBlockState().getValidStates()) {
                c.eq(s, s.getBlock().getStateFromMeta(s.getBlock().getMetaFromState(s)), "round trip " + s);
            }
        }

        // Recipes and smelting.
        ItemStack s = new ItemStack(StrippedLogCopies.LOGS.get(Wood.BIRCH));
        Crafting.expect(c, Crafting.grid(1, 1, s), new ItemStack(Blocks.PLANKS, 4, BlockPlanks.EnumType.BIRCH.getMetadata()), "birch planks from a stripped birch log");
        Crafting.expect(c, Crafting.grid(2, 2, s, s, s, s), new ItemStack(StrippedLogCopies.BARK.get(Wood.BIRCH), 3), "3 stripped birch wood");
        ItemStack charcoal = FurnaceRecipes.instance().getSmeltingResult(s);
        c.ok(charcoal.getItem() == Items.COAL && charcoal.getMetadata() == 1, "smelts to charcoal");

        // Axe stripping of vanilla logs, keeping the axis; bark becomes stripped wood.
        BlockPos at = c.area().add(4, 0, 4);
        EntityPlayerMP player = Players.real(c.world, "logstripper");
        ItemStack axe = new ItemStack(Items.GOLDEN_AXE);
        player.setHeldItem(EnumHand.MAIN_HAND, axe);
        c.world.setBlockState(at, Blocks.LOG2.getDefaultState().withProperty(BlockNewLog.VARIANT, BlockPlanks.EnumType.DARK_OAK)
                .withProperty(BlockLog.LOG_AXIS, BlockLog.EnumAxis.Z));
        player.interactionManager.processRightClickBlock(player, c.world, axe, EnumHand.MAIN_HAND, at, EnumFacing.NORTH, 0.5F, 0.5F, 0.0F);
        IBlockState got = c.world.getBlockState(at);
        c.eq(StrippedLogCopies.LOGS.get(Wood.DARK_OAK), got.getBlock(), "dark oak log stripped");
        c.eq(BlockLog.EnumAxis.Z, got.getValue(BlockLog.LOG_AXIS), "axis kept");
        c.world.setBlockState(at, Blocks.LOG.getDefaultState().withProperty(BlockOldLog.VARIANT, BlockPlanks.EnumType.JUNGLE)
                .withProperty(BlockLog.LOG_AXIS, BlockLog.EnumAxis.NONE));
        player.interactionManager.processRightClickBlock(player, c.world, axe, EnumHand.MAIN_HAND, at, EnumFacing.NORTH, 0.5F, 0.5F, 0.0F);
        c.eq(StrippedLogCopies.BARK.get(Wood.JUNGLE), c.world.getBlockState(at).getBlock(), "all-bark jungle log becomes stripped jungle wood");
    }
}
