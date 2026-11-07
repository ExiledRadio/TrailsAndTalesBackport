package com.exiledradio.trailsandtalesbackport.selftest;

import com.exiledradio.trailsandtalesbackport.cherry.CherryContent;
import com.exiledradio.trailsandtalesbackport.hangingsign.BlockHangingSign;
import com.exiledradio.trailsandtalesbackport.hangingsign.HangingSignContent;
import com.exiledradio.trailsandtalesbackport.hangingsign.HangingWood;
import com.exiledradio.trailsandtalesbackport.hangingsign.TileHangingSign;

import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.client.CPacketUpdateSign;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;

/** Stream H: hanging signs and the chain copy. */
public final class HangingSignSelfTest implements SelfTest {

    @Override
    public String name() {
        return "hangingsign";
    }

    @Override
    public void run(Check c) {
        if (!c.ok(!HangingSignContent.CEILING.isEmpty(), "hanging signs registered")) {
            return;
        }
        for (HangingWood w : HangingWood.values()) {
            boolean expected = w.present();
            c.eq(expected, HangingSignContent.CEILING.containsKey(w), w.name + " hanging sign registered iff its wood exists");
        }
        c.eq(false, HangingSignContent.CEILING.containsKey(HangingWood.CRIMSON), "no crimson sign without Nether Update");
        c.eq(false, HangingSignContent.CEILING.containsKey(HangingWood.MANGROVE), "no mangrove sign without Wild Update");
        c.ok(HangingSignContent.chain != null, "chain copy registered without Nether Update");

        BlockHangingSign.Ceiling oak = HangingSignContent.CEILING.get(HangingWood.OAK);
        BlockHangingSign.Wall oakWall = HangingSignContent.WALL.get(HangingWood.OAK);
        Item oakItem = HangingSignContent.ITEMS.get(HangingWood.OAK);
        for (IBlockState s : oak.getBlockState().getValidStates()) {
            c.eq(s.getValue(BlockHangingSign.Ceiling.ROTATION), oak.getStateFromMeta(oak.getMetaFromState(s)).getValue(BlockHangingSign.Ceiling.ROTATION), "rotation round trip");
        }
        for (EnumFacing f : EnumFacing.Plane.HORIZONTAL) {
            IBlockState s = oakWall.getDefaultState().withProperty(BlockHangingSign.Wall.FACING, f);
            c.eq(s, oakWall.getStateFromMeta(oakWall.getMetaFromState(s)), "wall facing round trip " + f);
        }

        // Recipes.
        ItemStack chain = new ItemStack(HangingSignContent.chain);
        ItemStack none = ItemStack.EMPTY;
        ItemStack nugget = new ItemStack(Items.IRON_NUGGET);
        Crafting.expect(c, Crafting.grid(1, 3, nugget, new ItemStack(Items.IRON_INGOT), nugget), chain, "chain from nuggets and an ingot");
        ItemStack log = HangingWood.OAK.log();
        Crafting.expect(c, Crafting.grid(3, 3, chain, none, chain, log, log, log, log, log, log), new ItemStack(oakItem, 6),
                "6 oak hanging signs from " + log.getItem().getRegistryName());
        ItemStack darkOak = HangingWood.DARK_OAK.log();
        Crafting.expect(c, Crafting.grid(3, 3, chain, none, chain, darkOak, darkOak, darkOak, darkOak, darkOak, darkOak),
                new ItemStack(HangingSignContent.ITEMS.get(HangingWood.DARK_OAK), 6), "dark oak from dark oak logs");
        if (CherryContent.set != null) {
            ItemStack s = new ItemStack(CherryContent.set.strippedLog);
            Crafting.expect(c, Crafting.grid(3, 3, chain, none, chain, s, s, s, s, s, s),
                    new ItemStack(HangingSignContent.ITEMS.get(HangingWood.CHERRY), 6), "cherry from stripped cherry logs");
        }
        c.eq(800, TileEntityFurnace.getItemBurnTime(new ItemStack(oakItem)), "hanging signs burn 800");
        c.eq(16, oakItem.getItemStackLimit(new ItemStack(oakItem)), "stack to 16");

        BlockPos area = c.area();
        EntityPlayerMP player = Players.real(c.world, "hanger");
        player.setPosition(area.getX() + 8.5, area.getY(), area.getZ() + 2.5);
        player.rotationYaw = 0.0F; // looking south
        ItemStack signs = new ItemStack(oakItem, 16);
        player.setHeldItem(EnumHand.MAIN_HAND, signs);

        // Under a full block: straight chains, four directions only.
        BlockPos roof = area.add(8, 3, 8);
        c.world.setBlockState(roof, Blocks.STONE.getDefaultState());
        use(player, c, roof, EnumFacing.DOWN);
        BlockPos at = roof.down();
        IBlockState s = c.world.getBlockState(at);
        c.eq(oak, s.getBlock(), "ceiling sign under a block");
        c.eq(8, s.getValue(BlockHangingSign.Ceiling.ROTATION), "facing the player (north), a cardinal rotation");
        TileEntity tile = c.world.getTileEntity(at);
        c.ok(tile instanceof TileHangingSign && !((TileHangingSign) tile).attached(), "straight chains under a full block");
        c.ok(((TileHangingSign) tile).getPlayer() == player, "the sign editor opened for the placer");
        c.eq(15, signs.getCount(), "one sign used");

        // Text from the editor packet.
        // The packet's constructor is client-only on a dedicated server; read it from bytes, as the server does.
        CPacketUpdateSign packet = new CPacketUpdateSign();
        net.minecraft.network.PacketBuffer buf = new net.minecraft.network.PacketBuffer(io.netty.buffer.Unpooled.buffer());
        buf.writeBlockPos(at);
        for (String line : new String[] {"Trails", "&", "Tales", ""}) {
            buf.writeString(line);
        }
        try {
            packet.readPacketData(buf);
        } catch (java.io.IOException e) {
            c.fail("packet: " + e);
        }
        player.connection.processUpdateSign(packet);
        c.eq("Trails", ((TileHangingSign) c.world.getTileEntity(at)).signText[0].getUnformattedText(), "text set through the sign packet");

        // Sneaking under a full block, or under a fence: the V of chains, sixteen directions.
        BlockPos roof2 = area.add(12, 3, 8);
        c.world.setBlockState(roof2, Blocks.STONE.getDefaultState());
        player.rotationYaw = 22.5F;
        player.setSneaking(true);
        use(player, c, roof2, EnumFacing.DOWN);
        player.setSneaking(false);
        c.ok(((TileHangingSign) c.world.getTileEntity(roof2.down())).attached(), "sneaking hangs it by a V of chains");
        c.eq(9, c.world.getBlockState(roof2.down()).getValue(BlockHangingSign.Ceiling.ROTATION), "attached: 16 directions");
        BlockPos fence = area.add(16, 3, 8);
        c.world.setBlockState(fence, Blocks.OAK_FENCE.getDefaultState());
        player.rotationYaw = 0.0F;
        use(player, c, fence, EnumFacing.DOWN);
        c.eq(oak, c.world.getBlockState(fence.down()).getBlock(), "hangs under a fence");
        c.ok(((TileHangingSign) c.world.getTileEntity(fence.down())).attached(), "a fence's bottom is not full: V of chains");
        BlockPos torchRoof = area.add(20, 3, 8);
        c.world.setBlockState(torchRoof, Blocks.GLASS_PANE.getDefaultState());
        use(player, c, torchRoof, EnumFacing.DOWN);
        c.eq(oak, c.world.getBlockState(torchRoof.down()).getBlock(), "hangs under a glass pane's post");

        // Never on the ground; never in the air.
        BlockPos floor = area.add(4, -1, 12);
        int before = signs.getCount();
        use(player, c, floor, EnumFacing.UP);
        c.ok(c.world.isAirBlock(floor.up()), "no hanging sign standing on the ground");
        c.eq(before, signs.getCount(), "nothing used");

        // Losing support drops it.
        c.world.setBlockToAir(roof);
        c.ok(c.world.isAirBlock(at), "the sign falls when its block goes");
        c.eq(1, Drops.count(c.world, at, oakItem), "and drops as its item");

        // On a wall: the board turned to face the player, the bracket in the wall.
        BlockPos wall = area.add(8, 0, 16);
        c.world.setBlockState(wall, Blocks.STONE.getDefaultState());
        player.setPosition(wall.getX() + 3.5, wall.getY(), wall.getZ() - 1.5);
        player.rotationYaw = 90.0F; // looking west, along the wall's north face
        use(player, c, wall, EnumFacing.NORTH);
        IBlockState w = c.world.getBlockState(wall.north());
        c.eq(oakWall, w.getBlock(), "wall hanging sign against a wall");
        c.eq(EnumFacing.EAST, w.getValue(BlockHangingSign.Wall.FACING), "its board faces the player to the east");
        c.ok(oakWall.canStay(c.world, wall.north(), w), "held by the wall beside it");
        c.world.setBlockToAir(wall);
        c.ok(c.world.isAirBlock(wall.north()), "falls when the wall goes");
        Drops.count(c.world, wall.north(), oakItem);

        // Saved and loaded with its chains.
        TileHangingSign t = (TileHangingSign) c.world.getTileEntity(roof2.down());
        NBTTagCompound tag = t.writeToNBT(new NBTTagCompound());
        TileHangingSign back = new TileHangingSign();
        back.readFromNBT(tag);
        c.ok(back.attached(), "attached saved");
        c.eq(true, oak.getActualState(c.world.getBlockState(roof2.down()), c.world, roof2.down()).getValue(BlockHangingSign.Ceiling.ATTACHED), "actual state shows the V");

        // The chain block.
        BlockPos chainAt = area.add(26, 0, 4);
        c.world.setBlockState(chainAt, HangingSignContent.chain.getDefaultState().withProperty(BlockRotatedPillar.AXIS, EnumFacing.Axis.X));
        c.eq(EnumFacing.Axis.X, HangingSignContent.chain.getStateFromMeta(HangingSignContent.chain.getMetaFromState(c.world.getBlockState(chainAt))).getValue(BlockRotatedPillar.AXIS), "chain axis round trip");
        c.world.setBlockState(chainAt.up(4), HangingSignContent.chain.getDefaultState());
        c.ok(BlockHangingSign.Ceiling.holdsCenter(c.world, chainAt.up(4)), "a chain holds a sign up: "
                + c.world.getBlockState(chainAt.up(4)).getCollisionBoundingBox(c.world, chainAt.up(4)));
        use(player, c, chainAt.up(4), EnumFacing.DOWN);
        c.eq(oak, c.world.getBlockState(chainAt.up(3)).getBlock(), "hangs under a chain");
    }

    private static void use(EntityPlayerMP player, Check c, BlockPos pos, EnumFacing face) {
        ItemStack held = player.getHeldItemMainhand();
        float hy = face == EnumFacing.DOWN ? 0.0F : face == EnumFacing.UP ? 1.0F : 0.5F;
        player.interactionManager.processRightClickBlock(player, c.world, held, EnumHand.MAIN_HAND, pos, face, 0.5F, hy, 0.5F);
    }
}
