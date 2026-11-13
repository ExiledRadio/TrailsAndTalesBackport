package com.exiledradio.trailsandtalesbackport.heads;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;

/**
 * A piglin head's rotation (standing heads, 16 steps, as 1.12.2's {@code TileEntitySkull} keeps it) and its
 * powered animation.
 *
 * <p>1.20.2's {@code SkullBlockEntity.animation}: on the client, while the head is powered, the animation
 * counter runs one step a tick (the ears flap); unpowered it stops where it is. Only the client ticks it,
 * as 1.20's ticker exists only client-side.
 */
public class TilePiglinHead extends TileEntity implements ITickable {

    private int rotation;
    private int animationTicks;
    private boolean animating;

    public int getRotation() {
        return this.rotation;
    }

    public void setRotation(int rotation) {
        this.rotation = rotation & 15;
    }

    @Override
    public void update() {
        if (this.world == null || !this.world.isRemote) {
            return;
        }
        if (this.world.isBlockPowered(this.pos)) {
            this.animating = true;
            this.animationTicks++;
        } else {
            this.animating = false;
        }
    }

    /** 1.20's {@code getAnimation}: the counter, plus the partial tick while it runs. */
    public float getAnimation(float partialTicks) {
        return this.animating ? this.animationTicks + partialTicks : this.animationTicks;
    }

    boolean isAnimating() {
        return this.animating;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.rotation = tag.getByte("Rot") & 15;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setByte("Rot", (byte) this.rotation);
        return tag;
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.pos, 4, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
        readFromNBT(packet.getNbtCompound());
    }

    private boolean standing() {
        if (this.world == null) {
            return true;
        }
        IBlockState state = this.world.getBlockState(this.pos);
        return !state.getPropertyKeys().contains(BlockPiglinHead.FACING) || state.getValue(BlockPiglinHead.FACING) == EnumFacing.UP;
    }

    @Override
    public void mirror(Mirror mirror) {
        if (standing()) {
            this.rotation = mirror.mirrorRotation(this.rotation, 16);
        }
    }

    @Override
    public void rotate(Rotation rotation) {
        if (standing()) {
            this.rotation = rotation.rotate(this.rotation, 16);
        }
    }
}
