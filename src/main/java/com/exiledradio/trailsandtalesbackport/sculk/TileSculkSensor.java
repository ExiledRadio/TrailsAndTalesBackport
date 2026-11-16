package com.exiledradio.trailsandtalesbackport.sculk;

import com.exiledradio.backportcore.gameevent.GameEvent;
import com.exiledradio.backportcore.gameevent.GameEventContext;
import com.exiledradio.backportcore.gameevent.GameEvents;
import com.exiledradio.backportcore.gameevent.PositionSource;
import com.exiledradio.backportcore.gameevent.VibrationSystem;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;

/**
 * The sculk sensor's tile entity: 1.20.6's {@code SculkSensorBlockEntity} on Backport Core's
 * {@link VibrationSystem}. Listens 8 blocks to {@code #vibrations}, only while inactive and not to its own block
 * being placed or broken; sneaking past it earns <i>Sneak 100</i> (with Wild Update Backport). Keeps
 * {@code last_vibration_frequency}, the {@code listener} tag, and the sensor's redstone power.
 */
public class TileSculkSensor extends TileEntity implements ITickable {

    public static final int LISTENER_RANGE = 8;

    private final VibrationSystem.Data data = new VibrationSystem.Data();
    private VibrationSystem.User user;
    private VibrationSystem.Listener listener;
    private int lastVibrationFrequency;
    private int power;
    private boolean listening;

    /** 1.20's {@code SculkSensorBlockEntity.VibrationUser}. */
    protected class User implements VibrationSystem.User {
        protected final BlockPos blockPos;
        private final PositionSource position;

        protected User(BlockPos pos) {
            this.blockPos = pos.toImmutable();
            this.position = new PositionSource.Block(this.blockPos);
        }

        @Override
        public int listenerRadius() {
            return LISTENER_RANGE;
        }

        @Override
        public PositionSource positionSource() {
            return this.position;
        }

        @Override
        public boolean canTriggerAvoidVibration() {
            return true;
        }

        @Override
        public boolean canReceiveVibration(WorldServer world, BlockPos origin, GameEvent event, GameEventContext context) {
            if (origin.equals(this.blockPos) && (event == GameEvent.BLOCK_DESTROY || event == GameEvent.BLOCK_PLACE)) {
                return false;
            }
            return !TileSculkSensor.this.isInvalid() && BlockSculkSensor.canActivate(world.getBlockState(this.blockPos));
        }

        @Override
        public void onReceiveVibration(WorldServer world, BlockPos origin, GameEvent event, @Nullable Entity source,
                                       @Nullable Entity projectileOwner, float distance) {
            IBlockState state = world.getBlockState(this.blockPos);
            if (BlockSculkSensor.canActivate(state)) {
                setLastVibrationFrequency(VibrationSystem.frequency(event));
                int strength = VibrationSystem.redstoneStrengthForDistance(distance, listenerRadius());
                ((BlockSculkSensor) state.getBlock()).activate(source, world, this.blockPos, state, strength, lastVibrationFrequency());
            }
        }

        @Override
        public void onDataChanged() {
            TileSculkSensor.this.markDirty();
        }

        @Override
        public boolean requiresAdjacentChunksToBeTicking() {
            return true;
        }
    }

    protected User createUser() {
        return new User(this.pos);
    }

    public VibrationSystem.User user() {
        if (this.user == null) {
            this.user = createUser();
        }
        return this.user;
    }

    public VibrationSystem.Listener listener() {
        if (this.listener == null) {
            this.listener = new VibrationSystem.Listener(user(), this.data);
        }
        return this.listener;
    }

    public VibrationSystem.Data data() {
        return this.data;
    }

    public int lastVibrationFrequency() {
        return this.lastVibrationFrequency;
    }

    public void setLastVibrationFrequency(int frequency) {
        this.lastVibrationFrequency = frequency;
        this.markDirty();
    }

    public int power() {
        return this.power;
    }

    public void setPower(int power) {
        this.power = power;
        this.markDirty();
    }

    @Override
    public void update() {
        if (this.world.isRemote) {
            return;
        }
        if (!this.listening) {
            GameEvents.add(this.world, listener());
            this.listening = true;
        }
        VibrationSystem.tick(this.world, this.data, user());
    }

    @Override
    public void onLoad() {
        if (this.world != null && !this.world.isRemote) {
            GameEvents.add(this.world, listener());
            this.listening = true;
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        stopListening();
    }

    @Override
    public void onChunkUnload() {
        stopListening();
    }

    private void stopListening() {
        if (this.world != null && !this.world.isRemote && this.listener != null) {
            GameEvents.remove(this.world, this.listener);
        }
        this.listening = false;
    }

    @Override
    public void setPos(BlockPos pos) {
        super.setPos(pos);
        // The user and listener are made at the block's position.
        if (this.listener != null && this.world != null && !this.world.isRemote) {
            GameEvents.remove(this.world, this.listener);
            this.listening = false;
        }
        this.user = null;
        this.listener = null;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.lastVibrationFrequency = tag.getInteger("last_vibration_frequency");
        this.power = tag.getInteger("power");
        if (tag.hasKey("listener", 10)) {
            this.data.read(tag.getCompoundTag("listener"));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("last_vibration_frequency", this.lastVibrationFrequency);
        tag.setInteger("power", this.power);
        tag.setTag("listener", this.data.write());
        return tag;
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }
}
