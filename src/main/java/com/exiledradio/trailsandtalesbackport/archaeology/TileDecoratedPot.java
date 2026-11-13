package com.exiledradio.trailsandtalesbackport.archaeology;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * A decorated pot's four faces and its one stack: 1.20.4's {@code DecoratedPotBlockEntity}.
 *
 * <h2>NBT (1.20.4's names)</h2>
 * {@code sherds}: four item ids, back, left, right, front ({@code minecraft:brick} for a plain face), left
 * out when all four are bricks; {@code item}: the stack inside; {@code LootTable}/{@code LootTableSeed}
 * for a pot a structure fills (rolled when it is first opened or emptied, as a chest's).
 * The item form keeps the faces under {@code BlockEntityTag}, which 1.12.2 copies into the tile entity on
 * placement.
 *
 * <h2>Hoppers</h2>
 * One slot: an empty pot takes any stack, a filled one only more of the same item, up to its stack size.
 */
public class TileDecoratedPot extends TileEntity {

    public static final String TAG_SHERDS = "sherds";
    public static final String TAG_ITEM = "item";
    /** Block event id for a wobble; its parameter is the {@link Wobble} ordinal. */
    public static final int EVENT_POT_WOBBLES = 1;

    /** Back, left, right, front, as 1.20's {@code PotDecorations.ordered()}. */
    private final Item[] sherds = {Items.BRICK, Items.BRICK, Items.BRICK, Items.BRICK};
    private ItemStack item = ItemStack.EMPTY;
    @Nullable
    private ResourceLocation lootTable;
    private long lootTableSeed;

    /** Client: when the last wobble started, and which. */
    public long wobbleStartedAtTick;
    @Nullable
    public Wobble lastWobble;

    public enum Wobble {
        POSITIVE(7), NEGATIVE(10);

        public final int duration;

        Wobble(int duration) {
            this.duration = duration;
        }
    }

    private final IItemHandler handler = new IItemHandler() {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        @Nonnull
        public ItemStack getStackInSlot(int slot) {
            return slot == 0 ? getTheItem() : ItemStack.EMPTY;
        }

        @Override
        @Nonnull
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            if (slot != 0 || stack.isEmpty()) {
                return stack;
            }
            ItemStack inside = getTheItem();
            if (!inside.isEmpty() && !ItemHandlerHelper.canItemStacksStack(inside, stack)) {
                return stack;
            }
            int room = stack.getMaxStackSize() - inside.getCount();
            if (room <= 0) {
                return stack;
            }
            int moved = Math.min(room, stack.getCount());
            if (!simulate) {
                if (inside.isEmpty()) {
                    setTheItem(ItemHandlerHelper.copyStackWithSize(stack, moved));
                } else {
                    inside.grow(moved);
                }
                changed();
            }
            return moved == stack.getCount() ? ItemStack.EMPTY : ItemHandlerHelper.copyStackWithSize(stack, stack.getCount() - moved);
        }

        @Override
        @Nonnull
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack inside = getTheItem();
            if (slot != 0 || amount <= 0 || inside.isEmpty()) {
                return ItemStack.EMPTY;
            }
            int taken = Math.min(amount, inside.getCount());
            ItemStack out = ItemHandlerHelper.copyStackWithSize(inside, taken);
            if (!simulate) {
                inside.shrink(taken);
                if (inside.isEmpty()) {
                    TileDecoratedPot.this.item = ItemStack.EMPTY;
                }
                changed();
            }
            return out;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }
    };

    // --- faces -----------------------------------------------------------------------------------

    /** @return back, left, right, front (bricks for plain faces) */
    public Item[] sherds() {
        return this.sherds.clone();
    }

    public void setSherds(Item back, Item left, Item right, Item front) {
        this.sherds[0] = back;
        this.sherds[1] = left;
        this.sherds[2] = right;
        this.sherds[3] = front;
        markDirty();
    }

    public EnumFacing facing() {
        IBlockState state = this.world != null ? this.world.getBlockState(this.pos) : null;
        return state != null && state.getBlock() instanceof BlockDecoratedPot
                ? state.getValue(BlockDecoratedPot.FACING) : EnumFacing.NORTH;
    }

    /** @return the pot as an item with these faces (1.20's {@code createDecoratedPotItem}) */
    public static ItemStack potItem(Item pot, Item[] sherds) {
        ItemStack stack = new ItemStack(pot);
        if (!allBricks(sherds)) {
            NBTTagCompound be = new NBTTagCompound();
            be.setTag(TAG_SHERDS, sherdList(sherds));
            stack.setTagInfo("BlockEntityTag", be);
        }
        return stack;
    }

    static boolean allBricks(Item[] sherds) {
        for (Item s : sherds) {
            if (s != Items.BRICK) {
                return false;
            }
        }
        return true;
    }

    static NBTTagList sherdList(Item[] sherds) {
        NBTTagList list = new NBTTagList();
        for (Item s : sherds) {
            list.appendTag(new NBTTagString(String.valueOf(s.getRegistryName())));
        }
        return list;
    }

    /** Reads a {@code sherds} list into back, left, right, front; unknown or missing ids are bricks. */
    static Item[] readSherds(NBTTagCompound tag) {
        Item[] out = {Items.BRICK, Items.BRICK, Items.BRICK, Items.BRICK};
        NBTTagList list = tag.getTagList(TAG_SHERDS, 8);
        for (int i = 0; i < 4 && i < list.tagCount(); i++) {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(list.getStringTagAt(i)));
            out[i] = item == null || item == Items.AIR ? Items.BRICK : item;
        }
        return out;
    }

    // --- the stack -------------------------------------------------------------------------------

    public ItemStack getTheItem() {
        unpackLootTable();
        return this.item;
    }

    public void setTheItem(ItemStack stack) {
        unpackLootTable();
        this.item = stack;
    }

    public boolean isEmpty() {
        return getTheItem().isEmpty();
    }

    void changed() {
        markDirty();
        if (this.world != null) {
            this.world.updateComparatorOutputLevel(this.pos, getBlockType());
        }
    }

    /** Vanilla's container signal for one slot of one stack. */
    public int comparatorSignal() {
        ItemStack stack = getTheItem();
        if (stack.isEmpty()) {
            return 0;
        }
        float fill = (float) stack.getCount() / (float) Math.min(64, stack.getMaxStackSize());
        return (int) Math.floor(fill * 14.0F) + 1;
    }

    public void setLootTable(ResourceLocation table, long seed) {
        this.lootTable = table;
        this.lootTableSeed = seed;
        markDirty();
    }

    private void unpackLootTable() {
        if (this.lootTable == null || !(this.world instanceof WorldServer)) {
            return;
        }
        ResourceLocation name = this.lootTable;
        this.lootTable = null;
        Random random = this.lootTableSeed == 0L ? new Random() : new Random(this.lootTableSeed);
        List<ItemStack> items = ((WorldServer) this.world).getLootTableManager().getLootTableFromLocation(name)
                .generateLootForPools(random, new LootContext.Builder((WorldServer) this.world).build());
        this.item = items.isEmpty() ? ItemStack.EMPTY : items.get(0);
        markDirty();
    }

    // --- wobble ----------------------------------------------------------------------------------

    public void wobble(Wobble style) {
        if (this.world != null && !this.world.isRemote) {
            this.world.addBlockEvent(this.pos, getBlockType(), EVENT_POT_WOBBLES, style.ordinal());
        }
    }

    @Override
    public boolean receiveClientEvent(int id, int type) {
        if (this.world != null && id == EVENT_POT_WOBBLES && type >= 0 && type < Wobble.values().length) {
            this.wobbleStartedAtTick = this.world.getTotalWorldTime();
            this.lastWobble = Wobble.values()[type];
            return true;
        }
        return super.receiveClientEvent(id, type);
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Override
    @Nullable
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(this.handler);
        }
        return super.getCapability(capability, facing);
    }

    // --- NBT -------------------------------------------------------------------------------------

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        Item[] read = readSherds(tag);
        System.arraycopy(read, 0, this.sherds, 0, 4);
        if (tag.hasKey("LootTable", 8)) {
            this.lootTable = new ResourceLocation(tag.getString("LootTable"));
            this.lootTableSeed = tag.getLong("LootTableSeed");
            this.item = ItemStack.EMPTY;
        } else {
            this.item = tag.hasKey(TAG_ITEM, 10) ? new ItemStack(tag.getCompoundTag(TAG_ITEM)) : ItemStack.EMPTY;
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        writeOwn(tag);
        return tag;
    }

    private void writeOwn(NBTTagCompound tag) {
        if (!allBricks(this.sherds)) {
            tag.setTag(TAG_SHERDS, sherdList(this.sherds));
        }
        if (this.lootTable != null) {
            tag.setString("LootTable", this.lootTable.toString());
            if (this.lootTableSeed != 0L) {
                tag.setLong("LootTableSeed", this.lootTableSeed);
            }
        } else if (!this.item.isEmpty()) {
            tag.setTag(TAG_ITEM, this.item.writeToNBT(new NBTTagCompound()));
        }
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound tag = super.writeToNBT(new NBTTagCompound());
        writeOwn(tag);
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

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        readFromNBT(tag);
        if (this.world != null && this.world.isRemote) {
            this.world.markBlockRangeForRenderUpdate(this.pos, this.pos);
        }
    }
}
