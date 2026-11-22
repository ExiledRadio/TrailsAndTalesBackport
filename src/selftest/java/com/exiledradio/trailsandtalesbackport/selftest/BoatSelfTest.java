package com.exiledradio.trailsandtalesbackport.selftest;

import com.exiledradio.trailsandtalesbackport.boat.BoatContent;
import com.exiledradio.trailsandtalesbackport.boat.BoatWood;
import com.exiledradio.trailsandtalesbackport.common.Content;
import com.exiledradio.backportcore.boat.EntityWoodBoat;
import com.exiledradio.backportcore.boat.EntityWoodChestBoat;
import com.exiledradio.backportcore.boat.Boats;
import com.exiledradio.backportcore.boat.ItemWoodBoat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

/** Stream Bo: cherry boats, bamboo rafts and their chest forms. */
public final class BoatSelfTest implements SelfTest {

    @Override
    public String name() {
        return "boat";
    }

    @Override
    public void run(Check c) {
        Item boat = BoatContent.item(BoatWood.CHERRY, false);
        Item chestBoat = BoatContent.item(BoatWood.CHERRY, true);
        Item raft = BoatContent.item(BoatWood.BAMBOO, false);
        Item chestRaft = BoatContent.item(BoatWood.BAMBOO, true);
        if (!c.ok(boat != null && chestBoat != null && raft != null && chestRaft != null, "four boat items")) {
            return;
        }
        c.eq("trailsandtalesbackport:cherry_chest_boat", chestBoat.getRegistryName().toString(), "chest boat name");
        c.eq("trailsandtalesbackport:bamboo_chest_raft", chestRaft.getRegistryName().toString(), "chest raft name");
        c.eq(1, boat.getItemStackLimit(new ItemStack(boat)), "boats stack to 1");
        c.eq(Boats.BOAT, Boats.renamedTo(Content.id("boat")), "our old boats load as Core's");
        c.eq(Boats.CHEST_BOAT, Boats.renamedTo(Content.id("chest_boat")), "our old chest boats load as Core's");
        c.ok(BoatWood.BAMBOO.core() != null && BoatWood.BAMBOO.core().raft, "bamboo is a raft in Core");
        c.eq(1200, TileEntityFurnace.getItemBurnTime(new ItemStack(raft)), "rafts burn 1200");

        ItemStack cherry = new ItemStack(Item.REGISTRY.getObject(new net.minecraft.util.ResourceLocation("trailsandtalesbackport:cherry_planks")));
        ItemStack bamboo = new ItemStack(Item.REGISTRY.getObject(new net.minecraft.util.ResourceLocation("trailsandtalesbackport:bamboo_planks")));
        ItemStack none = ItemStack.EMPTY;
        Crafting.expect(c, Crafting.grid(3, 2, cherry, none, cherry, cherry, cherry, cherry), new ItemStack(boat), "cherry boat from 5 planks");
        Crafting.expect(c, Crafting.grid(3, 2, bamboo, none, bamboo, bamboo, bamboo, bamboo), new ItemStack(raft), "bamboo raft from 5 planks");
        Crafting.expect(c, Crafting.grid(2, 1, new ItemStack(Blocks.CHEST), new ItemStack(boat)), new ItemStack(chestBoat), "boat and chest");
        Crafting.expect(c, Crafting.grid(2, 1, new ItemStack(raft), new ItemStack(Blocks.CHEST)), new ItemStack(chestRaft), "raft and chest");

        BlockPos area = c.area();
        // A pool to float in.
        c.fill(area.add(2, -3, 2), area.add(12, -1, 12), Blocks.STONE.getDefaultState());
        c.fill(area.add(3, -2, 3), area.add(11, -1, 11), Blocks.WATER.getDefaultState());

        // Placed by its item, looking down at the water.
        EntityPlayerMP player = Players.real(c.world, "sailor");
        player.setPositionAndRotation(area.getX() + 7.5, area.getY() + 1, area.getZ() + 4.5, 0.0F, 60.0F);
        player.prevRotationPitch = 60.0F;
        player.prevRotationYaw = 0.0F;
        player.prevPosX = player.posX;
        player.prevPosY = player.posY;
        player.prevPosZ = player.posZ;
        ItemStack held = new ItemStack(boat);
        player.setHeldItem(EnumHand.MAIN_HAND, held);
        ((ItemWoodBoat) boat).onItemRightClick(c.world, player, EnumHand.MAIN_HAND);
        EntityWoodBoat placed = first(c, area, EntityWoodBoat.class);
        c.ok(placed != null && !(placed instanceof EntityWoodChestBoat), "a cherry boat was placed on the water");
        c.eq(0, held.getCount(), "the item is used");
        if (placed != null) {
            c.eq(BoatWood.CHERRY.core(), placed.getWood(), "it is cherry");
            c.eq("backportcore:boat", net.minecraft.entity.EntityList.getKey(placed).toString(), "it is Backport Core's boat");
            c.eq(boat, placed.getItemBoat(), "and is picked as the cherry boat");
            for (int i = 0; i < 40; i++) {
                placed.onUpdate();
            }
            c.ok(!placed.isDead, "it floats for two seconds");
            placed.attackEntityFrom(DamageSource.causePlayerDamage(player), 50.0F);
            c.ok(placed.isDead, "hitting it breaks it");
            c.eq(1, countItems(c, area, boat), "it drops the cherry boat");
        }

        // Riders: a raft seats its rider higher than a boat does.
        EntityWoodBoat r = new EntityWoodBoat(c.world, area.getX() + 7.5, area.getY() - 1, area.getZ() + 7.5, BoatWood.BAMBOO.core());
        EntityWoodBoat b = new EntityWoodBoat(c.world, area.getX() + 7.5, area.getY() - 1, area.getZ() + 7.5, BoatWood.CHERRY.core());
        c.ok(r.getMountedYOffset() > b.getMountedYOffset(), "a raft's rider sits higher: " + r.getMountedYOffset());

        // Chest boats: one rider, a 27-slot chest, contents saved and dropped.
        EntityWoodChestBoat chest = new EntityWoodChestBoat(c.world, area.getX() + 7.5, area.getY() - 1, area.getZ() + 9.5, BoatWood.BAMBOO.core());
        c.world.spawnEntity(chest);
        EntityPig pig = new EntityPig(c.world);
        pig.setPosition(chest.posX, chest.posY, chest.posZ);
        c.world.spawnEntity(pig);
        EntityPig pig2 = new EntityPig(c.world);
        pig2.setPosition(chest.posX, chest.posY, chest.posZ);
        c.world.spawnEntity(pig2);
        c.ok(pig.startRiding(chest), "one rider fits");
        c.ok(!pig2.startRiding(chest), "a second does not");
        pig.dismountRidingEntity();
        pig.setDead();
        pig2.setDead();
        c.eq(27, chest.getSizeInventory(), "27 slots");
        chest.setInventorySlotContents(0, new ItemStack(Items.DIAMOND, 3));
        chest.setInventorySlotContents(26, new ItemStack(Items.APPLE, 5));

        player.setSneaking(true);
        chest.processInitialInteract(player, EnumHand.MAIN_HAND);
        player.setSneaking(false);
        c.ok(player.openContainer instanceof ContainerChest, "sneak-clicking opens the chest: " + player.openContainer);
        player.closeScreen();

        NBTTagCompound tag = new NBTTagCompound();
        chest.writeToNBTOptional(tag);
        Entity loaded = EntityList.createEntityFromNBT(tag, c.world);
        c.ok(loaded instanceof EntityWoodChestBoat, "saved and loaded as a chest boat: " + loaded);
        if (loaded instanceof EntityWoodChestBoat) {
            EntityWoodChestBoat l = (EntityWoodChestBoat) loaded;
            c.eq(BoatWood.BAMBOO.core(), l.getWood(), "keeps its wood");
            c.eq(3, l.getStackInSlot(0).getCount(), "keeps its diamonds");
            c.eq(Items.APPLE, l.getStackInSlot(26).getItem(), "keeps its last slot");
        }
        chest.attackEntityFrom(DamageSource.causePlayerDamage(player), 50.0F);
        c.ok(chest.isDead, "chest raft broken");
        c.eq(3, countItems(c, area, Items.DIAMOND), "its contents drop");
        c.eq(1, countItems(c, area, chestRaft), "and the chest raft itself");
    }

    private static <T extends Entity> T first(Check c, BlockPos area, Class<T> type) {
        for (T e : c.world.getEntitiesWithinAABB(type, new AxisAlignedBB(area, area.add(32, 10, 32)).grow(0, 5, 0))) {
            return e;
        }
        return null;
    }

    private static int countItems(Check c, BlockPos area, Item item) {
        int n = 0;
        for (EntityItem e : c.world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(area, area.add(32, 10, 32)).grow(0, 5, 0))) {
            if (!e.isDead && e.getItem().getItem() == item) {
                n += e.getItem().getCount();
                e.setDead();
            }
        }
        return n;
    }
}
