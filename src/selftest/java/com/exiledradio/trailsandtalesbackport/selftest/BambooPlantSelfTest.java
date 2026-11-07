package com.exiledradio.trailsandtalesbackport.selftest;

import com.exiledradio.trailsandtalesbackport.bamboo.BambooWoodContent;
import com.exiledradio.trailsandtalesbackport.bamboo.plant.BambooPlant;
import com.exiledradio.trailsandtalesbackport.common.Owners;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;

import java.util.Random;

/** The bamboo plant copy: present without Village & Pillage, and usable on its own. */
public final class BambooPlantSelfTest implements SelfTest {

    @Override
    public String name() {
        return "bambooplant";
    }

    @Override
    public void run(Check c) {
        if (Owners.present(Owners.VILLAGE_AND_PILLAGE)) {
            c.eq(null, BambooPlant.bamboo, "no copy with Village & Pillage installed");
            return;
        }
        if (!c.ok(BambooPlant.bamboo != null && BambooPlant.bambooItem != null, "bamboo copy registered")) {
            return;
        }
        c.eq("trailsandtalesbackport:bamboo", BambooPlant.bambooItem.getRegistryName().toString(), "same name as the owner's, our namespace");
        c.eq(BambooPlant.bambooItem, BambooWoodContent.bambooItem(), "the block of bamboo is made from it");
        ItemStack b = new ItemStack(BambooPlant.bambooItem);
        Crafting.expect(c, Crafting.grid(3, 3, b, b, b, b, b, b, b, b, b), new ItemStack(BambooWoodContent.set.log), "9 bamboo -> block of bamboo");

        // Every state of both blocks survives saving.
        for (Block block : new Block[] {BambooPlant.bamboo, BambooPlant.bambooSapling}) {
            for (IBlockState s : block.getBlockState().getValidStates()) {
                IBlockState back = block.getStateFromMeta(block.getMetaFromState(s));
                if (block.getMetaFromState(back) != block.getMetaFromState(s)) {
                    c.fail(block.getRegistryName() + " " + s + " -> " + back);
                }
            }
        }

        // Planted by a player on grass: a shoot, which bone meal grows into a stalk.
        BlockPos area = c.area();
        BlockPos ground = area.add(8, -1, 8);
        c.world.setBlockState(ground, Blocks.GRASS.getDefaultState());
        EntityPlayerMP player = Players.real(c.world, "planter");
        player.setPosition(ground.getX() + 0.5, ground.getY() + 1, ground.getZ() - 1.5);
        ItemStack held = new ItemStack(BambooPlant.bambooItem, 4);
        player.setHeldItem(EnumHand.MAIN_HAND, held);
        player.interactionManager.processRightClickBlock(player, c.world, held, EnumHand.MAIN_HAND, ground, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
        c.eq(BambooPlant.bambooSapling, c.world.getBlockState(ground.up()).getBlock(), "bamboo plants a shoot");
        Random rand = new Random(3L);
        for (int i = 0; i < 6; i++) {
            IBlockState s = c.world.getBlockState(ground.up());
            if (s.getBlock() instanceof net.minecraft.block.IGrowable) {
                ((net.minecraft.block.IGrowable) s.getBlock()).grow(c.world, rand, ground.up(), s);
            }
        }
        c.eq(BambooPlant.bamboo, c.world.getBlockState(ground.up()).getBlock(), "bone meal turns the shoot into bamboo");
        c.eq(BambooPlant.bamboo, c.world.getBlockState(ground.up(2)).getBlock(), "and it grows taller");
        c.world.destroyBlock(ground.up(), true);
        c.eq(1, Drops.count(c.world, ground.up(), BambooPlant.bambooItem), "the broken piece drops one bamboo");
        // The piece above loses its support and breaks on its scheduled tick, as 1.14's does.
        BlockPos above = ground.up(2);
        IBlockState top = c.world.getBlockState(above);
        top.getBlock().updateTick(c.world, above, top, rand);
        c.ok(c.world.getBlockState(above).getBlock() != BambooPlant.bamboo, "the stalk above comes down on its tick");
    }
}
