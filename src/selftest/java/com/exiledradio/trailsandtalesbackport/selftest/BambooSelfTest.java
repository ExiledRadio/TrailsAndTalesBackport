package com.exiledradio.trailsandtalesbackport.selftest;

import com.exiledradio.trailsandtalesbackport.bamboo.BambooWoodContent;
import com.exiledradio.trailsandtalesbackport.common.Content;
import com.exiledradio.trailsandtalesbackport.common.Owners;
import com.exiledradio.trailsandtalesbackport.common.wood.WoodSet;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.oredict.OreDictionary;

/** Stream B: the bamboo wood set, with and without Village & Pillage's bamboo. */
public final class BambooSelfTest implements SelfTest {

    @Override
    public String name() {
        return "bamboo";
    }

    @Override
    public void run(Check c) {
        WoodSet set = BambooWoodContent.set;
        if (!c.ok(set != null, "bamboo stream registered")) {
            return;
        }
        String[] withItems = {"bamboo_block", "stripped_bamboo_block", "bamboo_planks", "bamboo_mosaic", "bamboo_stairs",
                "bamboo_mosaic_stairs", "bamboo_slab", "bamboo_mosaic_slab", "bamboo_fence", "bamboo_fence_gate",
                "bamboo_trapdoor", "bamboo_pressure_plate", "bamboo_button"};
        for (String name : withItems) {
            Block b = Block.REGISTRY.getObject(Content.id(name));
            c.ok(b != Blocks.AIR && Item.getItemFromBlock(b) != Items.AIR, name + " registered with an item");
        }
        for (String name : new String[] {"bamboo_double_slab", "bamboo_mosaic_double_slab", "bamboo_wall_sign", "bamboo_sign", "bamboo_door"}) {
            Block b = Block.REGISTRY.getObject(Content.id(name));
            c.ok(b != Blocks.AIR && Item.getItemFromBlock(b) == Items.AIR, name + " registered without a block item");
        }
        c.eq(null, set.wood, "bamboo has no wood block");
        c.eq(BambooWoodContent.BAMBOO.sound, set.planks.getSoundType(), "bamboo wood sounds");
        for (IBlockState s : set.mosaicSlab.getBlockState().getValidStates()) {
            c.eq(s, set.mosaicSlab.getStateFromMeta(set.mosaicSlab.getMetaFromState(s)), "mosaic slab state round trip");
        }

        ItemStack block = new ItemStack(set.log);
        ItemStack planks = new ItemStack(set.planks);
        ItemStack slab = new ItemStack(set.slab);
        ItemStack mosaic = new ItemStack(set.mosaic);
        ItemStack none = ItemStack.EMPTY;
        Crafting.expect(c, Crafting.grid(1, 1, block), new ItemStack(set.planks, 2), "block of bamboo -> 2 planks");
        Crafting.expect(c, Crafting.grid(1, 1, new ItemStack(set.strippedLog)), new ItemStack(set.planks, 2), "stripped block -> 2 planks");
        Crafting.expect(c, Crafting.grid(1, 2, slab, slab), new ItemStack(set.mosaic), "2 bamboo slabs -> mosaic");
        Crafting.expect(c, Crafting.grid(3, 3, mosaic, none, none, mosaic, mosaic, none, mosaic, mosaic, mosaic),
                new ItemStack(set.mosaicStairs, 4), "mosaic stairs");
        Crafting.expect(c, Crafting.grid(3, 1, mosaic, mosaic, mosaic), new ItemStack(set.mosaicSlab, 6), "mosaic slabs");
        Crafting.expect(c, Crafting.grid(3, 1, planks, planks, planks), new ItemStack(set.slab, 6), "bamboo slabs");
        Crafting.expect(c, Crafting.grid(2, 2, planks, planks, planks, planks), new ItemStack(Blocks.CRAFTING_TABLE), "crafting table from bamboo planks");
        c.ok(OreDictionary.getOres("plankWood").stream().anyMatch(s -> s.getItem() == Item.getItemFromBlock(set.mosaic)), "mosaic is plankWood");
        c.eq(300, TileEntityFurnace.getItemBurnTime(mosaic), "mosaic burns 300");
        c.eq(5, Blocks.FIRE.getFlammability(set.log), "block of bamboo burns like a log");

        Item bamboo = BambooWoodContent.bambooItem();
        if (Owners.present(Owners.VILLAGE_AND_PILLAGE)) {
            c.ok(bamboo != null && "villagerbackport".equals(bamboo.getRegistryName().getNamespace()),
                    "Village & Pillage's bamboo is used: " + bamboo);
            ItemStack b = new ItemStack(bamboo);
            Crafting.expect(c, Crafting.grid(3, 3, b, b, b, b, b, b, b, b, b), new ItemStack(set.log), "9 bamboo -> block of bamboo");
        } else {
            c.eq(null, Item.REGISTRY.getObject(new net.minecraft.util.ResourceLocation("villagerbackport", "bamboo")),
                    "no Village & Pillage bamboo in this run");
            ItemStack sugar = new ItemStack(Items.REEDS);
            c.ok(CraftingManager.findMatchingResult(Crafting.grid(3, 3, sugar, sugar, sugar, sugar, sugar, sugar, sugar, sugar, sugar), c.world).getItem()
                    != Item.getItemFromBlock(set.log), "no stand-in recipe for the block of bamboo");
        }

        // Stripping the block of bamboo.
        BlockPos at = c.area().add(4, 0, 4);
        c.world.setBlockState(at, set.log.getDefaultState().withProperty(BlockRotatedPillar.AXIS, EnumFacing.Axis.Z));
        EntityPlayerMP player = Players.real(c.world, "bamboostripper");
        ItemStack axe = new ItemStack(Items.STONE_AXE);
        player.setHeldItem(EnumHand.MAIN_HAND, axe);
        player.interactionManager.processRightClickBlock(player, c.world, axe, EnumHand.MAIN_HAND, at, EnumFacing.NORTH, 0.5F, 0.5F, 0.0F);
        IBlockState got = c.world.getBlockState(at);
        c.eq(set.strippedLog, got.getBlock(), "axe strips the block of bamboo");
        c.eq(EnumFacing.Axis.Z, got.getValue(BlockRotatedPillar.AXIS), "keeping its axis");
    }
}
