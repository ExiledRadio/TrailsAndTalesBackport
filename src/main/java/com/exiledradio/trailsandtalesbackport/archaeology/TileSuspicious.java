package com.exiledradio.trailsandtalesbackport.archaeology;

import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTable;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * The buried item of a suspicious block and how far it has been brushed: 1.20's {@code BrushableBlockEntity}.
 *
 * <h2>Brushing (vanilla's numbers)</h2>
 * A brush brushes every 10 ticks while it is held on the block. Each brush pushes the reset back to 40
 * ticks away; one counts only after a 10-tick cooldown. The first brush rolls the loot table (so the
 * item is decided by whoever brushes first, with their luck). 10 brushes finish it: the item drops on the
 * brushed face and the block turns into sand or gravel. The {@code dusted} state is 1 from one brush, 2
 * from three, 3 from six. Left alone, it loses 2 brushes every 4 ticks once the 40 ticks are up.
 *
 * <h2>NBT (1.20's names, for structures and other mods)</h2>
 * {@code LootTable} (string) and {@code LootTableSeed} (long, 0 = random) until it is first brushed, then
 * {@code item} (an item stack); {@code hit_direction} (the brushed face, 0-5) while brushing. A structure
 * places the block and writes {@code LootTable}/{@code LootTableSeed} into its tile entity; see
 * {@code docs/streams/archaeology.md}.
 */
public class TileSuspicious extends TileEntity {

    private static final int BRUSH_COOLDOWN_TICKS = 10;
    private static final int BRUSH_RESET_TICKS = 40;
    private static final int REQUIRED_BRUSHES_TO_BREAK = 10;

    private int brushCount;
    private long brushCountResetsAtTick;
    private long coolDownEndsAtTick;
    private ItemStack item = ItemStack.EMPTY;
    @Nullable
    private EnumFacing hitDirection;
    @Nullable
    private ResourceLocation lootTable;
    private long lootTableSeed;

    /**
     * One brush stroke at game time {@code time}.
     *
     * @return true if this stroke finished the block (the brush then loses one durability)
     */
    public boolean brush(long time, EntityPlayer player, EnumFacing face) {
        if (this.hitDirection == null) {
            this.hitDirection = face;
        }
        this.brushCountResetsAtTick = time + BRUSH_RESET_TICKS;
        if (time < this.coolDownEndsAtTick || !(this.world instanceof WorldServer)) {
            return false;
        }
        this.coolDownEndsAtTick = time + BRUSH_COOLDOWN_TICKS;
        unpackLootTable(player);
        int before = completionState();
        if (++this.brushCount >= REQUIRED_BRUSHES_TO_BREAK) {
            brushingCompleted(player);
            return true;
        }
        this.world.scheduleUpdate(this.pos, getBlockType(), BlockSuspicious.TICK_DELAY);
        int after = completionState();
        if (before != after) {
            IBlockState state = this.world.getBlockState(this.pos);
            this.world.setBlockState(this.pos, state.withProperty(BlockSuspicious.DUSTED, after), 3);
        }
        return false;
    }

    /** Rolls the loot table, once. */
    void unpackLootTable(@Nullable EntityPlayer player) {
        if (this.lootTable == null || !(this.world instanceof WorldServer)) {
            return;
        }
        WorldServer world = (WorldServer) this.world;
        ResourceLocation name = this.lootTable;
        LootTable table = world.getLootTableManager().getLootTableFromLocation(name);
        if (player instanceof EntityPlayerMP) {
            ArchaeologyContent.lootGenerated((EntityPlayerMP) player, name);
        }
        LootContext.Builder context = new LootContext.Builder(world);
        if (player != null) {
            context.withLuck(player.getLuck()).withPlayer(player);
        }
        Random random = this.lootTableSeed == 0L ? new Random() : new Random(this.lootTableSeed);
        List<ItemStack> items = table.generateLootForPools(random, context.build());
        if (items.size() > 1) {
            TrailsAndTalesBackport.LOGGER.warn("Expected max 1 loot from loot table {}, but got {}", name, items.size());
        }
        this.item = items.isEmpty() ? ItemStack.EMPTY : items.get(0);
        this.lootTable = null;
        markDirty();
    }

    private void brushingCompleted(EntityPlayer player) {
        dropContent(player);
        IBlockState state = this.world.getBlockState(this.pos);
        ArchaeologyNetwork.brushCompleted(this.world, this.pos, state);
        Block block = state.getBlock();
        IBlockState next = block instanceof BlockSuspicious ? ((BlockSuspicious) block).turnsInto()
                : net.minecraft.init.Blocks.AIR.getDefaultState();
        this.world.setBlockState(this.pos, next, 3);
    }

    private void dropContent(EntityPlayer player) {
        unpackLootTable(player);
        if (this.item.isEmpty()) {
            return;
        }
        // An item entity is 0.25 wide and tall: vanilla's spot, half a block out from the brushed face.
        double width = 0.25D;
        double rest = 1.0D - width;
        double half = width / 2.0D;
        EnumFacing face = this.hitDirection != null ? this.hitDirection : EnumFacing.UP;
        BlockPos at = this.pos.offset(face, 1);
        double x = at.getX() + 0.5D * rest + half;
        double y = at.getY() + 0.5D + 0.125D;
        double z = at.getZ() + 0.5D * rest + half;
        EntityItem drop = new EntityItem(this.world, x, y, z, this.item.splitStack(this.world.rand.nextInt(21) + 10));
        drop.motionX = 0.0D;
        drop.motionY = 0.0D;
        drop.motionZ = 0.0D;
        this.world.spawnEntity(drop);
        this.item = ItemStack.EMPTY;
    }

    /** From the block's scheduled tick: winds the brushing back once it is left alone. */
    public void checkReset() {
        long now = this.world.getTotalWorldTime();
        if (this.brushCount != 0 && now >= this.brushCountResetsAtTick) {
            int before = completionState();
            this.brushCount = Math.max(0, this.brushCount - 2);
            int after = completionState();
            if (before != after) {
                IBlockState state = this.world.getBlockState(this.pos);
                this.world.setBlockState(this.pos, state.withProperty(BlockSuspicious.DUSTED, after), 3);
            }
            this.brushCountResetsAtTick = now + 4L;
        }
        if (this.brushCount == 0) {
            this.hitDirection = null;
            this.brushCountResetsAtTick = 0L;
            this.coolDownEndsAtTick = 0L;
        } else {
            this.world.scheduleUpdate(this.pos, getBlockType(), BlockSuspicious.TICK_DELAY);
        }
    }

    private int completionState() {
        if (this.brushCount == 0) {
            return 0;
        } else if (this.brushCount < 3) {
            return 1;
        } else {
            return this.brushCount < 6 ? 2 : 3;
        }
    }

    // --- access ----------------------------------------------------------------------------------

    public void setLootTable(ResourceLocation table, long seed) {
        this.lootTable = table;
        this.lootTableSeed = seed;
        markDirty();
    }

    @Nullable
    public ResourceLocation lootTable() {
        return this.lootTable;
    }

    @Nullable
    public EnumFacing hitDirection() {
        return this.hitDirection;
    }

    public ItemStack item() {
        return this.item;
    }

    public int brushCount() {
        return this.brushCount;
    }

    /** Sets the buried item directly (creative, tests); clears any loot table. */
    public void setItem(ItemStack stack) {
        this.item = stack;
        this.lootTable = null;
        markDirty();
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    // --- NBT -------------------------------------------------------------------------------------

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        if (tag.hasKey("LootTable", 8)) {
            this.lootTable = new ResourceLocation(tag.getString("LootTable"));
            this.lootTableSeed = tag.getLong("LootTableSeed");
            this.item = ItemStack.EMPTY;
        } else {
            this.lootTable = null;
            this.item = tag.hasKey("item", 10) ? new ItemStack(tag.getCompoundTag("item")) : ItemStack.EMPTY;
        }
        this.hitDirection = tag.hasKey("hit_direction", 99) ? EnumFacing.byIndex(tag.getInteger("hit_direction")) : null;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        if (this.lootTable != null) {
            tag.setString("LootTable", this.lootTable.toString());
            if (this.lootTableSeed != 0L) {
                tag.setLong("LootTableSeed", this.lootTableSeed);
            }
        } else if (!this.item.isEmpty()) {
            tag.setTag("item", this.item.writeToNBT(new NBTTagCompound()));
        }
        if (this.hitDirection != null) {
            tag.setInteger("hit_direction", this.hitDirection.getIndex());
        }
        return tag;
    }

    /** The client draws the item poking out of the brushed face: it needs the item and the face. */
    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound tag = super.writeToNBT(new NBTTagCompound());
        if (this.hitDirection != null) {
            tag.setInteger("hit_direction", this.hitDirection.getIndex());
        }
        if (!this.item.isEmpty()) {
            tag.setTag("item", this.item.writeToNBT(new NBTTagCompound()));
        }
        return tag;
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.pos, 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
        handleUpdateTag(packet.getNbtCompound());
    }
}
