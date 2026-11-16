package com.exiledradio.trailsandtalesbackport.bookshelf;

import com.exiledradio.backportcore.gameevent.GameEvent;
import com.exiledradio.trailsandtalesbackport.common.Vibrations;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The six books of a chiseled bookshelf and the slot last touched (1.20's {@code ChiseledBookShelfBlockEntity}).
 *
 * <ul>
 *   <li>Each slot holds one book: book, written book, book and quill, enchanted book or knowledge book
 *       (1.20's {@code bookshelf_books} tag), plus any item another mod adds to the {@code bookshelfBook} ore name.</li>
 *   <li>Hoppers and droppers put books in and take them out through Forge's item handler, from any side.</li>
 *   <li>{@link #lastSlot} is what a comparator reads: the slot last added to or taken from, 1-6, 0 for none.</li>
 *   <li>Which slots hold a book is part of the update packet, so the client draws the right books; every
 *       change re-renders the block there.</li>
 * </ul>
 */
public class TileChiseledBookshelf extends TileEntity {

    public static final int SLOTS = 6;

    /** Slot last touched, 0-5, or -1. */
    private int lastSlot = -1;

    private final ItemStackHandler books = new ItemStackHandler(SLOTS) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return isBook(stack);
        }

        /**
         * Books only (this Forge's {@code ItemStackHandler.insertItem} does not ask {@link #isItemValid}), and
         * a copy is stored: an empty slot would otherwise keep the caller's own stack object.
         */
        @Override
        @Nonnull
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            if (!isBook(stack)) {
                return stack;
            }
            return super.insertItem(slot, stack.copy(), simulate);
        }

        @Override
        protected void onContentsChanged(int slot) {
            TileChiseledBookshelf.this.changed(slot);
        }
    };

    /** @return true if 1.20's chiseled bookshelf takes this item */
    public static boolean isBook(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        if (item == Items.BOOK || item == Items.WRITTEN_BOOK || item == Items.WRITABLE_BOOK
                || item == Items.ENCHANTED_BOOK || item == Items.KNOWLEDGE_BOOK) {
            return true;
        }
        for (int id : net.minecraftforge.oredict.OreDictionary.getOreIDs(stack)) {
            if ("bookshelfBook".equals(net.minecraftforge.oredict.OreDictionary.getOreName(id))) {
                return true;
            }
        }
        return false;
    }

    public ItemStack book(int slot) {
        return this.books.getStackInSlot(slot);
    }

    public boolean occupied(int slot) {
        return !this.books.getStackInSlot(slot).isEmpty();
    }

    /** Puts a copy of one of the stack into an empty slot. */
    public void put(int slot, ItemStack stack) {
        ItemStack one = stack.copy();
        one.setCount(1);
        this.books.setStackInSlot(slot, one);
    }

    /** @return the book taken out of a slot */
    public ItemStack take(int slot) {
        ItemStack out = this.books.getStackInSlot(slot);
        this.books.setStackInSlot(slot, ItemStack.EMPTY);
        return out;
    }

    /** @return the comparator signal: last slot touched, 1-6, or 0 */
    public int signal() {
        return this.lastSlot + 1;
    }

    private void changed(int slot) {
        this.lastSlot = slot;
        markDirty();
        World world = getWorld();
        if (world != null) {
            IBlockState state = world.getBlockState(this.pos);
            world.notifyBlockUpdate(this.pos, state, state, 3);
            world.updateComparatorOutputLevel(this.pos, state.getBlock());
            // 1.20's updateState: block_change for any change of the books (players, hoppers), the new state as context.
            Vibrations.block(world, GameEvent.BLOCK_CHANGE, this.pos, null, state.getActualState(world, this.pos));
        }
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    // ---------------------------------------------------------------------------------------------

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.books.deserializeNBT(tag.getCompoundTag("Items"));
        this.lastSlot = tag.hasKey("last_interacted_slot") ? tag.getInteger("last_interacted_slot") : -1;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setTag("Items", this.books.serializeNBT());
        tag.setInteger("last_interacted_slot", this.lastSlot);
        return tag;
    }

    /** The client needs the books (their kind: enchanted ones look no different, but a mod might). */
    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.pos, 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
        readFromNBT(packet.getNbtCompound());
        if (this.world != null) {
            this.world.markBlockRangeForRenderUpdate(this.pos, this.pos);
        }
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        readFromNBT(tag);
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Override
    @Nullable
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast((IItemHandler) this.books);
        }
        return super.getCapability(capability, facing);
    }
}
