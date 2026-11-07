package com.exiledradio.trailsandtalesbackport.boat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ITeleporter;

/**
 * A boat (or raft) with a chest: 1.19's chest boat, which is Wild Update's mechanic, done here for this mod's
 * two woods until Backport Core has one shared chest boat (see {@code docs/CORE_REQUESTS.md} 4).
 *
 * <ul>
 *   <li>27 slots, opened by sneaking and right-clicking it (anyone, as a chest); closes when the boat is gone
 *       or more than 8 blocks away.</li>
 *   <li>One rider, sitting toward the front (1.19: 0.15 forward), since the chest takes the back seat.</li>
 *   <li>Breaking it drops the boat and everything in the chest; moving to another dimension keeps the
 *       contents with the boat instead.</li>
 * </ul>
 */
public class EntityModChestBoat extends EntityModBoat {

    public static final int SLOTS = 27;

    private final Inventory inventory = new Inventory();
    private boolean dropContentsWhenDead = true;

    public EntityModChestBoat(World world) {
        super(world);
    }

    public EntityModChestBoat(World world, double x, double y, double z, BoatWood wood) {
        super(world, x, y, z, wood);
    }

    public InventoryBasic inventory() {
        return this.inventory;
    }

    @Override
    protected boolean canFitPassenger(Entity passenger) {
        return getPassengers().isEmpty();
    }

    @Override
    public void updatePassenger(Entity passenger) {
        super.updatePassenger(passenger);
        if (isPassenger(passenger)) {
            Vec3d forward = new Vec3d(0.15D, 0.0D, 0.0D).rotateYaw(-this.rotationYaw * 0.017453292F - ((float) Math.PI / 2F));
            passenger.setPosition(passenger.posX + forward.x, passenger.posY, passenger.posZ + forward.z);
        }
    }

    @Override
    public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
        if (player.isSneaking()) {
            if (!this.world.isRemote) {
                player.displayGUIChest(this.inventory);
            }
            return true;
        }
        return super.processInitialInteract(player, hand);
    }

    @Override
    public void setDead() {
        if (!this.world.isRemote && this.dropContentsWhenDead) {
            InventoryHelper.dropInventoryItems(this.world, this, this.inventory);
        }
        super.setDead();
    }

    @Override
    public Entity changeDimension(int dimension, ITeleporter teleporter) {
        this.dropContentsWhenDead = false;
        return super.changeDimension(dimension, teleporter);
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < SLOTS; i++) {
            items.set(i, this.inventory.getStackInSlot(i));
        }
        ItemStackHelper.saveAllItems(tag, items);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        ItemStackHelper.loadAllItems(tag, items);
        for (int i = 0; i < SLOTS; i++) {
            this.inventory.setInventorySlotContents(i, items.get(i));
        }
    }

    /** The chest: usable while the boat lives and the player is within 8 blocks, as a chest minecart's. */
    final class Inventory extends InventoryBasic {
        Inventory() {
            super("", false, SLOTS);
        }

        @Override
        public net.minecraft.util.text.ITextComponent getDisplayName() {
            return new TextComponentTranslation(getItemBoat().getTranslationKey() + ".name");
        }

        @Override
        public boolean isUsableByPlayer(EntityPlayer player) {
            return !EntityModChestBoat.this.isDead && player.getDistanceSq(EntityModChestBoat.this) <= 64.0D;
        }
    }
}
