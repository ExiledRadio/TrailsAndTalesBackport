package com.exiledradio.trailsandtalesbackport.cherry;

import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The pink petals item: placed on petals that are not yet full (the clicked block, or the block the
 * click would place into), it adds one petal instead of placing a new block.
 */
public class ItemPinkPetals extends ItemBlock {

    public ItemPinkPetals(BlockPinkPetals block) {
        super(block);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing,
                                      float hitX, float hitY, float hitZ) {
        BlockPos target = pos;
        IBlockState state = world.getBlockState(target);
        if (!BlockPinkPetals.canAddTo(state)) {
            target = pos.offset(facing);
            state = world.getBlockState(target);
        }
        ItemStack stack = player.getHeldItem(hand);
        if (BlockPinkPetals.canAddTo(state) && player.canPlayerEdit(target, facing, stack)) {
            if (!world.isRemote) {
                world.setBlockState(target, state.withProperty(BlockPinkPetals.AMOUNT, state.getValue(BlockPinkPetals.AMOUNT) + 1), 3);
            }
            SoundType sound = state.getBlock().getSoundType(state, world, target, player);
            world.playSound(player, target, sound.getPlaceSound(), SoundCategory.BLOCKS,
                    (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
            if (!player.capabilities.isCreativeMode) {
                stack.shrink(1);
            }
            return EnumActionResult.SUCCESS;
        }
        return super.onItemUse(player, world, pos, hand, facing, hitX, hitY, hitZ);
    }
}
