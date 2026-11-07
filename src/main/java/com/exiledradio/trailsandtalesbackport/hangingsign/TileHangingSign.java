package com.exiledradio.trailsandtalesbackport.hangingsign;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A hanging sign's tile: vanilla's sign tile (so editing and the sign packets work unchanged) plus whether
 * a ceiling sign hangs by a V of chains ({@code attached}), which its metadata has no room for. Saved, and
 * sent with the sign's update packet, which is its whole save.
 */
public class TileHangingSign extends TileEntitySign {

    private boolean attached;

    public boolean attached() {
        return this.attached;
    }

    public void setAttached(boolean attached) {
        this.attached = attached;
        markDirty();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setBoolean("Attached", this.attached);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.attached = tag.getBoolean("Attached");
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }
}
