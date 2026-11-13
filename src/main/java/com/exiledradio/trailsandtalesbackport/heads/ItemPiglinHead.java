package com.exiledradio.trailsandtalesbackport.heads;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.EntityEquipmentSlot;
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

import javax.annotation.Nullable;

/**
 * The piglin head item: placed as 1.12.2's {@code ItemSkull} places a head (standing on a top face, turned
 * to the player in 16 steps; on a wall facing out of it), and worn in the head slot.
 */
public class ItemPiglinHead extends ItemBlock {

    public ItemPiglinHead(BlockPiglinHead block) {
        super(block);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing,
                                      float hitX, float hitY, float hitZ) {
        if (facing == EnumFacing.DOWN) {
            return EnumActionResult.FAIL;
        }
        if (world.getBlockState(pos).getBlock().isReplaceable(world, pos)) {
            facing = EnumFacing.UP;
            pos = pos.down();
        }
        IBlockState clicked = world.getBlockState(pos);
        if (!clicked.getBlock().isReplaceable(world, pos)) {
            if (!clicked.getMaterial().isSolid() && !world.isSideSolid(pos, facing, true)) {
                return EnumActionResult.FAIL;
            }
            pos = pos.offset(facing);
        }
        ItemStack stack = player.getHeldItem(hand);
        if (!player.canPlayerEdit(pos, facing, stack) || !this.block.canPlaceBlockAt(world, pos)) {
            return EnumActionResult.FAIL;
        }
        if (world.isRemote) {
            return EnumActionResult.SUCCESS;
        }
        IBlockState state = this.block.getDefaultState().withProperty(BlockPiglinHead.FACING, facing);
        world.setBlockState(pos, state, 11);
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TilePiglinHead && facing == EnumFacing.UP) {
            ((TilePiglinHead) tile).setRotation(rotationFor(player.rotationYaw));
        }
        SoundType sound = this.block.getSoundType(state, world, pos, player);
        world.playSound(null, pos, sound.getPlaceSound(), SoundCategory.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F,
                sound.getPitch() * 0.8F);
        if (player instanceof EntityPlayerMP) {
            CriteriaTriggers.PLACED_BLOCK.trigger((EntityPlayerMP) player, pos, stack);
        }
        stack.shrink(1);
        return EnumActionResult.SUCCESS;
    }

    /** 1.12.2's and 1.20's 16-step rotation from the placer's yaw. */
    public static int rotationFor(float yaw) {
        return MathHelper.floor(yaw * 16.0F / 360.0F + 0.5D) & 15;
    }

    @Nullable
    @Override
    public EntityEquipmentSlot getEquipmentSlot(ItemStack stack) {
        return EntityEquipmentSlot.HEAD;
    }

    @Override
    public boolean isValidArmor(ItemStack stack, EntityEquipmentSlot armorType, Entity entity) {
        return armorType == EntityEquipmentSlot.HEAD;
    }
}
