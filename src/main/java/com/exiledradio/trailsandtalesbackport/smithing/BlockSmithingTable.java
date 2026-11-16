package com.exiledradio.trailsandtalesbackport.smithing;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The smithing table, as Village &amp; Pillage registers it, for games without that mod and without Nether Update's
 * copy (Rule 1): one state (metadata 0), wood, hardness 2.5, resistance as Village &amp; Pillage sets it
 * ({@code setResistance(hardness * 5)}), wood sounds. Right-clicked it opens 1.20's three-slot screen.
 */
public class BlockSmithingTable extends Block {

    public BlockSmithingTable() {
        super(Material.WOOD, MapColor.WOOD);
        setHardness(2.5F);
        setResistance(2.5F * 5.0F);
        setSoundType(SoundType.WOOD);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            SmithingTables.open(player, pos);
        }
        return true;
    }
}
