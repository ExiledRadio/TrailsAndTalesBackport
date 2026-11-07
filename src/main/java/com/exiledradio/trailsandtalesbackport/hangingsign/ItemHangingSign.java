package com.exiledradio.trailsandtalesbackport.hangingsign;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/**
 * A hanging sign item, placing 1.20's way.
 *
 * <ul>
 *   <li>Clicking the underside of a block hangs a ceiling sign under it.</li>
 *   <li>Clicking a side of a block puts a wall hanging sign against it, its board turned to face the
 *       player; failing that, a ceiling sign if the block above the new spot holds one.</li>
 *   <li>Clicking a top face hangs a ceiling sign there only if the block above holds it: hanging signs
 *       never stand on the ground.</li>
 *   <li>A ceiling sign under a block without a full bottom face, or placed while sneaking, hangs by a V of
 *       chains ({@code attached}) and turns to any of 16 directions; otherwise it turns to the nearest of 4.</li>
 * </ul>
 * Burns for 800 ticks; stacks to 16; opens the sign editor once placed.
 */
public class ItemHangingSign extends Item {

    private final BlockHangingSign.Ceiling ceiling;
    private final BlockHangingSign.Wall wall;

    public ItemHangingSign(BlockHangingSign.Ceiling ceiling, BlockHangingSign.Wall wall) {
        this.ceiling = ceiling;
        this.wall = wall;
        this.maxStackSize = 16;
        ceiling.item = this;
        wall.item = this;
    }

    @Override
    public int getItemBurnTime(ItemStack stack) {
        return 800;
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing,
                                      float hitX, float hitY, float hitZ) {
        IBlockState clicked = world.getBlockState(pos);
        BlockPos at = clicked.getBlock().isReplaceable(world, pos) ? pos : pos.offset(facing);
        ItemStack stack = player.getHeldItem(hand);
        if (!world.getBlockState(at).getBlock().isReplaceable(world, at) || !player.canPlayerEdit(at, facing, stack)) {
            return EnumActionResult.FAIL;
        }
        IBlockState state = null;
        boolean attached = false;
        if (facing.getAxis().isHorizontal()) {
            IBlockState w = wallState(player, facing);
            if (this.wall.canStay(world, at, w)) {
                state = w;
            }
        }
        if (state == null && BlockHangingSign.Ceiling.holdsCenter(world, at.up())) {
            attached = !BlockHangingSign.Ceiling.fullBelow(world, at.up()) || player.isSneaking();
            state = this.ceiling.getDefaultState().withProperty(BlockHangingSign.Ceiling.ROTATION, rotation(player, attached));
        }
        if (state == null) {
            return EnumActionResult.FAIL;
        }
        if (world.isRemote) {
            return EnumActionResult.SUCCESS;
        }
        world.setBlockState(at, state, 11);
        TileEntity tile = world.getTileEntity(at);
        if (tile instanceof TileHangingSign) {
            ((TileHangingSign) tile).setAttached(attached);
            IBlockState now = world.getBlockState(at);
            world.notifyBlockUpdate(at, now, now, 3);
            if (!ItemBlock.setTileEntityNBT(world, player, at, stack)) {
                player.openEditSign((TileHangingSign) tile);
            }
        }
        SoundType sound = state.getBlock().getSoundType(state, world, at, player);
        world.playSound(null, at, sound.getPlaceSound(), SoundCategory.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        if (player instanceof EntityPlayerMP) {
            CriteriaTriggers.PLACED_BLOCK.trigger((EntityPlayerMP) player, at, stack);
        }
        if (!player.capabilities.isCreativeMode) {
            stack.shrink(1);
        }
        return EnumActionResult.SUCCESS;
    }

    /** The board faces the player, across the clicked wall: of the two directions along the wall, the one toward them. */
    IBlockState wallState(EntityPlayer player, EnumFacing clickedFace) {
        EnumFacing a = clickedFace.rotateY();
        double yaw = Math.toRadians(player.rotationYaw);
        // The player's look as a horizontal vector (1.12.2: yaw 0 looks south, +z).
        double lx = -Math.sin(yaw);
        double lz = Math.cos(yaw);
        double along = a.getXOffset() * lx + a.getZOffset() * lz;
        EnumFacing look = along >= 0 ? a : a.getOpposite();
        return this.wall.getDefaultState().withProperty(BlockHangingSign.Wall.FACING, look.getOpposite());
    }

    /** 1.20: 16 directions when attached, else the nearest of 4 (in 16ths). */
    static int rotation(EntityPlayer player, boolean attached) {
        if (attached) {
            return MathHelper.floor((player.rotationYaw + 180.0F) * 16.0F / 360.0F + 0.5D) & 15;
        }
        return horizontal(player);
    }

    private static int horizontal(EntityPlayer player) {
        // Standing-sign rotation of the direction facing the player: south 0, west 4, north 8, east 12.
        switch (player.getHorizontalFacing().getOpposite()) {
            case NORTH:
                return 8;
            case EAST:
                return 12;
            case WEST:
                return 4;
            default:
                return 0;
        }
    }
}
