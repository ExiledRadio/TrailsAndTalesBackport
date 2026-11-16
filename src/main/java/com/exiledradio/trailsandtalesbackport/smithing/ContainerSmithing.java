package com.exiledradio.trailsandtalesbackport.smithing;

import com.exiledradio.backportcore.smithing.ArmorTrims;
import com.exiledradio.backportcore.smithing.SmithingTemplates;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * 1.20's {@code SmithingMenu}: template (8, 48), base (26, 48), addition (44, 48), result (98, 48).
 *
 * <ul>
 * <li>Each input slot takes only what some recipe takes there; shift-clicking from the inventory sends an item to the
 * first input slot it fits, template, base, then addition (1.20's {@code getSlotToQuickMoveTo}).</li>
 * <li>The result is {@link SmithingTemplates#find}'s; taking it uses up one of each input (the template too) and
 * plays the smithing table's sound.</li>
 * <li>Transforms keep the base's enchantments, name and damage; trims write the trim (Backport Core).</li>
 * </ul>
 */
public class ContainerSmithing extends Container {

    public static final int SLOT_TEMPLATE = 0;
    public static final int SLOT_BASE = 1;
    public static final int SLOT_ADDITION = 2;
    public static final int SLOT_RESULT = 3;

    private final World world;
    private final BlockPos pos;
    private final InventoryCraftResult result = new InventoryCraftResult();
    @Nullable
    private SmithingTemplates.Result selected;

    private final InventoryBasic inputs = new InventoryBasic("Smithing", true, 3) {
        @Override
        public void markDirty() {
            super.markDirty();
            ContainerSmithing.this.onCraftMatrixChanged(this);
        }
    };

    public ContainerSmithing(InventoryPlayer playerInventory, World world, BlockPos pos) {
        this.world = world;
        this.pos = pos;
        addSlotToContainer(new Input(SLOT_TEMPLATE, 8, 48));
        addSlotToContainer(new Input(SLOT_BASE, 26, 48));
        addSlotToContainer(new Input(SLOT_ADDITION, 44, 48));
        addSlotToContainer(new ResultSlot(98, 48));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlotToContainer(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlotToContainer(new Slot(playerInventory, column, 8 + column * 18, 142));
        }
    }

    /** @return true if this stack goes in that input slot */
    public static boolean accepts(int slot, ItemStack stack) {
        switch (slot) {
            case SLOT_TEMPLATE:
                return SmithingTemplates.isTemplate(stack);
            case SLOT_BASE:
                return SmithingTemplates.isBase(stack);
            case SLOT_ADDITION:
                return SmithingTemplates.isAddition(stack);
            default:
                return false;
        }
    }

    @Override
    public void onCraftMatrixChanged(IInventory inventory) {
        super.onCraftMatrixChanged(inventory);
        if (inventory == this.inputs) {
            updateResult();
        }
    }

    private void updateResult() {
        this.selected = SmithingTemplates.find(this.inputs.getStackInSlot(SLOT_TEMPLATE),
                this.inputs.getStackInSlot(SLOT_BASE), this.inputs.getStackInSlot(SLOT_ADDITION));
        this.result.setInventorySlotContents(0, this.selected == null ? ItemStack.EMPTY : this.selected.result());
        detectAndSendChanges();
    }

    /** 1.20's {@code hasRecipeError}: all three inputs filled and no result. */
    public boolean hasRecipeError() {
        return getSlot(SLOT_TEMPLATE).getHasStack() && getSlot(SLOT_BASE).getHasStack()
                && getSlot(SLOT_ADDITION).getHasStack() && !getSlot(SLOT_RESULT).getHasStack();
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return SmithingTables.isTable(this.world.getBlockState(this.pos).getBlock())
                && player.getDistanceSq(this.pos.getX() + 0.5D, this.pos.getY() + 0.5D, this.pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        if (!this.world.isRemote) {
            clearContainer(player, this.world, this.inputs);
        }
    }

    @Override
    public boolean canMergeSlot(ItemStack stack, Slot slot) {
        return slot.inventory != this.result && super.canMergeSlot(stack, slot);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        Slot slot = this.inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();
        int inventoryStart = 4;
        int end = this.inventorySlots.size();
        int hotbarStart = end - 9;
        if (index == SLOT_RESULT) {
            if (!mergeItemStack(stack, inventoryStart, end, true)) {
                return ItemStack.EMPTY;
            }
            slot.onSlotChange(stack, original);
        } else if (index < SLOT_RESULT) {
            if (!mergeItemStack(stack, inventoryStart, end, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            int to = SmithingTemplates.isTemplate(stack) ? SLOT_TEMPLATE : SmithingTemplates.isBase(stack) ? SLOT_BASE
                    : SmithingTemplates.isAddition(stack) ? SLOT_ADDITION : -1;
            boolean moved = to >= 0 && mergeInto(stack, to);
            if (!moved) {
                if (index < hotbarStart) {
                    if (!mergeItemStack(stack, hotbarStart, end, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!mergeItemStack(stack, inventoryStart, hotbarStart, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }
        if (stack.isEmpty()) {
            slot.putStack(ItemStack.EMPTY);
        } else {
            slot.onSlotChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    /** 1.20's {@code moveItemStackTo(stack, slot, 3)}: that input slot and the ones after it that take the stack. */
    private boolean mergeInto(ItemStack stack, int first) {
        boolean moved = false;
        for (int i = first; i <= SLOT_ADDITION && !stack.isEmpty(); i++) {
            if (accepts(i, stack) && mergeItemStack(stack, i, i + 1, false)) {
                moved = true;
            }
        }
        return moved;
    }

    private class Input extends Slot {

        Input(int index, int x, int y) {
            super(ContainerSmithing.this.inputs, index, x, y);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return accepts(getSlotIndex(), stack);
        }
    }

    /** Taking the result uses up one of each input, with the smithing table's sound (1.20's level event 1044). */
    private class ResultSlot extends Slot {

        ResultSlot(int x, int y) {
            super(ContainerSmithing.this.result, 0, x, y);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return false;
        }

        @Override
        public boolean canTakeStack(EntityPlayer player) {
            return ContainerSmithing.this.selected != null && getHasStack();
        }

        @Override
        public ItemStack onTake(EntityPlayer player, ItemStack stack) {
            ContainerSmithing c = ContainerSmithing.this;
            // A shift-click has moved the stack on by now (it may be empty): the recipe says what was made.
            ItemStack made = c.selected == null ? stack : c.selected.result();
            made.onCrafting(player.world, player, 1);
            ItemStack template = c.inputs.getStackInSlot(SLOT_TEMPLATE).copy();
            if (c.selected == null || c.selected.consumesTemplate()) {
                c.inputs.decrStackSize(SLOT_TEMPLATE, 1);
            }
            c.inputs.decrStackSize(SLOT_BASE, 1);
            c.inputs.decrStackSize(SLOT_ADDITION, 1);
            if (!c.world.isRemote) {
                if (SmithingContent.use != null) {
                    c.world.playSound(null, c.pos, SmithingContent.use, SoundCategory.BLOCKS, 1.0F,
                            c.world.rand.nextFloat() * 0.1F + 0.9F);
                }
                if (player instanceof EntityPlayerMP && ArmorTrims.get(made) != null) {
                    SmithingAdvancements.trimmed((EntityPlayerMP) player, Templates.of(template));
                }
            }
            return stack;
        }
    }
}
