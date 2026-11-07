package com.exiledradio.trailsandtalesbackport.selftest;

import com.exiledradio.trailsandtalesbackport.bookshelf.BlockChiseledBookshelf;
import com.exiledradio.trailsandtalesbackport.bookshelf.BookshelfContent;
import com.exiledradio.trailsandtalesbackport.bookshelf.TileChiseledBookshelf;

import net.minecraft.advancements.Advancement;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

/** Stream K: the chiseled bookshelf. */
public final class BookshelfSelfTest implements SelfTest {

    @Override
    public String name() {
        return "bookshelf";
    }

    @Override
    public void run(Check c) {
        BlockChiseledBookshelf shelf = BookshelfContent.shelf;
        if (!c.ok(shelf != null, "chiseled bookshelf registered")) {
            return;
        }
        Item item = Item.getItemFromBlock(shelf);
        c.eq(4 * 64, shelf.getBlockState().getValidStates().size(), "4 facings x 64 slot combinations");
        for (EnumFacing f : EnumFacing.Plane.HORIZONTAL) {
            IBlockState s = shelf.getDefaultState().withProperty(BlockChiseledBookshelf.FACING, f);
            c.eq(s, shelf.getStateFromMeta(shelf.getMetaFromState(s)), "facing round trip " + f);
        }
        ItemStack planks = new ItemStack(Blocks.PLANKS, 1, 2);
        ItemStack slab = new ItemStack(Blocks.WOODEN_SLAB, 1, 0);
        Crafting.expect(c, Crafting.grid(3, 3, planks, planks, planks, slab, slab, slab, planks, planks, planks),
                new ItemStack(shelf), "6 planks and 3 wooden slabs");
        c.eq(300, TileEntityFurnace.getItemBurnTime(new ItemStack(shelf)), "burns 300");
        c.eq(0.0F, shelf.getEnchantPowerBonus(c.world, BlockPos.ORIGIN), "no enchanting power");

        // Slot under the cursor, 1.20's layout, on every facing.
        c.eq(0, BlockChiseledBookshelf.slotAt(EnumFacing.NORTH, EnumFacing.NORTH, 0.9F, 0.8F, 0.0F), "north face, viewer's top left");
        c.eq(2, BlockChiseledBookshelf.slotAt(EnumFacing.NORTH, EnumFacing.NORTH, 0.1F, 0.8F, 0.0F), "north face, top right");
        c.eq(4, BlockChiseledBookshelf.slotAt(EnumFacing.NORTH, EnumFacing.NORTH, 0.5F, 0.2F, 0.0F), "north face, bottom middle");
        c.eq(3, BlockChiseledBookshelf.slotAt(EnumFacing.SOUTH, EnumFacing.SOUTH, 0.1F, 0.2F, 1.0F), "south face, bottom left");
        c.eq(0, BlockChiseledBookshelf.slotAt(EnumFacing.WEST, EnumFacing.WEST, 0.0F, 0.9F, 0.2F), "west face, top left");
        c.eq(5, BlockChiseledBookshelf.slotAt(EnumFacing.EAST, EnumFacing.EAST, 1.0F, 0.1F, 0.1F), "east face, bottom right");
        c.eq(1, BlockChiseledBookshelf.slotAt(EnumFacing.NORTH, EnumFacing.NORTH, 1.0F - 0.5F, 0.6F, 0.0F), "column boundaries 6/16 and 11/16");
        c.eq(-1, BlockChiseledBookshelf.slotAt(EnumFacing.NORTH, EnumFacing.EAST, 0.5F, 0.5F, 0.5F), "a side face has no slots");

        BlockPos area = c.area();
        BlockPos pos = area.add(8, 0, 8);
        EntityPlayerMP player = Players.real(c.world, "reader");
        player.setPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() - 1.5);
        player.rotationYaw = 0.0F; // facing south, toward the shelf

        // Placed by a player: faces them.
        ItemStack shelves = new ItemStack(item, 2);
        player.setHeldItem(EnumHand.MAIN_HAND, shelves);
        player.interactionManager.processRightClickBlock(player, c.world, shelves, EnumHand.MAIN_HAND, pos.down(), EnumFacing.UP, 0.5F, 1.0F, 0.5F);
        c.eq(shelf, c.world.getBlockState(pos).getBlock(), "shelf placed");
        c.eq(EnumFacing.NORTH, c.world.getBlockState(pos).getValue(BlockChiseledBookshelf.FACING), "facing the player north of it");
        TileChiseledBookshelf tile = (TileChiseledBookshelf) c.world.getTileEntity(pos);

        // Books in by hand.
        ItemStack books = new ItemStack(Items.BOOK, 3);
        player.setHeldItem(EnumHand.MAIN_HAND, books);
        click(player, c, pos, books, 0.9F, 0.8F);
        c.ok(tile.occupied(0), "a book goes into the slot clicked");
        c.eq(2, books.getCount(), "one book used");
        c.eq(1, shelf.getComparatorInputOverride(c.world.getBlockState(pos), c.world, pos), "comparator reads slot 1");
        click(player, c, pos, books, 0.1F, 0.2F);
        c.ok(tile.occupied(5), "bottom right");
        c.eq(6, shelf.getComparatorInputOverride(c.world.getBlockState(pos), c.world, pos), "comparator reads slot 6");
        IBlockState actual = c.world.getBlockState(pos).getActualState(c.world, pos);
        c.eq(true, actual.getValue(BlockChiseledBookshelf.SLOTS[0]), "state shows slot 0 full");
        c.eq(false, actual.getValue(BlockChiseledBookshelf.SLOTS[1]), "state shows slot 1 empty");
        c.eq(true, actual.getValue(BlockChiseledBookshelf.SLOTS[5]), "state shows slot 5 full");

        ItemStack enchanted = new ItemStack(Items.ENCHANTED_BOOK);
        player.setHeldItem(EnumHand.MAIN_HAND, enchanted);
        click(player, c, pos, enchanted, 0.5F, 0.8F);
        c.ok(tile.occupied(1) && tile.book(1).getItem() == Items.ENCHANTED_BOOK, "enchanted books go in");

        ItemStack sticks = new ItemStack(Items.STICK, 5);
        player.setHeldItem(EnumHand.MAIN_HAND, sticks);
        click(player, c, pos, sticks, 0.5F, 0.2F);
        c.ok(!tile.occupied(4), "a stick does not go in");
        c.eq(5, sticks.getCount(), "and is not used");

        // Out by hand: into the inventory.
        player.inventory.clear();
        player.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
        click(player, c, pos, ItemStack.EMPTY, 0.9F, 0.8F);
        c.ok(!tile.occupied(0), "clicking a full slot takes the book");
        c.ok(player.inventory.hasItemStack(new ItemStack(Items.BOOK)), "into the player's inventory");
        c.eq(1, shelf.getComparatorInputOverride(c.world.getBlockState(pos), c.world, pos), "comparator reads the slot taken from");

        // Hoppers: the item handler takes one book per slot, only books, and stores a copy.
        IItemHandler handler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.UP);
        ItemStack two = new ItemStack(Items.WRITABLE_BOOK, 2);
        ItemStack rest = handler.insertItem(0, two, false);
        c.eq(1, rest.getCount(), "one of two books fits a slot");
        c.eq(2, two.getCount(), "the caller's stack is left alone");
        c.ok(tile.book(0) != two, "the slot holds its own stack");
        c.eq(Items.STICK, handler.insertItem(3, new ItemStack(Items.STICK), false).getItem(), "a stick is refused");

        // A hopper above fills empty slots; a hopper below empties them.
        BlockPos top = pos.up();
        c.world.setBlockState(top, Blocks.HOPPER.getDefaultState());
        TileEntityHopper hopper = (TileEntityHopper) c.world.getTileEntity(top);
        hopper.setInventorySlotContents(0, new ItemStack(Items.BOOK, 2));
        for (int i = 0; i < 40; i++) {
            hopper.update();
        }
        c.ok(hopper.getStackInSlot(0).isEmpty(), "a hopper above put its books in: " + hopper.getStackInSlot(0));
        int full = 0;
        for (int i = 0; i < 6; i++) {
            full += tile.occupied(i) ? 1 : 0;
        }
        c.eq(5, full, "five slots full now");

        // Saving and loading.
        NBTTagCompound saved = tile.writeToNBT(new NBTTagCompound());
        TileChiseledBookshelf copy = new TileChiseledBookshelf();
        copy.readFromNBT(saved);
        for (int i = 0; i < 6; i++) {
            c.ok(ItemStack.areItemStacksEqual(tile.book(i), copy.book(i)), "slot " + i + " saved");
        }
        c.eq(tile.signal(), copy.signal(), "last slot saved");

        // Breaking: books drop, the shelf does not; with Silk Touch it does.
        c.world.setBlockToAir(top);
        c.world.destroyBlock(pos, true);
        int dropped = 0;
        for (net.minecraft.entity.item.EntityItem e : c.world.getEntitiesWithinAABB(net.minecraft.entity.item.EntityItem.class,
                new net.minecraft.util.math.AxisAlignedBB(pos).grow(2))) {
            if (TileChiseledBookshelf.isBook(e.getItem())) {
                dropped += e.getItem().getCount();
            }
            e.setDead();
        }
        c.eq(5, dropped, "the five books drop");
        c.world.setBlockState(pos, shelf.getDefaultState());
        Drops.count(c.world, pos, item);
        c.world.destroyBlock(pos, true);
        c.eq(0, Drops.count(c.world, pos, item), "no shelf without Silk Touch");
        c.world.setBlockState(pos, shelf.getDefaultState());
        ItemStack axe = new ItemStack(Items.IRON_AXE);
        axe.addEnchantment(Enchantments.SILK_TOUCH, 1);
        IBlockState state = c.world.getBlockState(pos);
        shelf.harvestBlock(c.world, player, pos, state, c.world.getTileEntity(pos), axe);
        c.eq(1, Drops.count(c.world, pos, item), "Silk Touch gives the shelf");
        c.world.setBlockToAir(pos);

        // The Power of Books: a comparator placed to read a shelf.
        Advancement adv = c.world.getMinecraftServer().getAdvancementManager().getAdvancement(BookshelfContent.ADVANCEMENT);
        if (c.ok(adv != null, "advancement loaded")) {
            c.ok(!player.getAdvancements().getProgress(adv).isDone(), "not yet done");
            c.world.setBlockState(pos, shelf.getDefaultState());
            ItemStack comparator = new ItemStack(Items.COMPARATOR);
            player.setHeldItem(EnumHand.MAIN_HAND, comparator);
            player.rotationYaw = 0.0F; // facing south: the comparator faces north, reading the shelf north of it
            player.interactionManager.processRightClickBlock(player, c.world, comparator, EnumHand.MAIN_HAND,
                    pos.south().down(), EnumFacing.UP, 0.5F, 1.0F, 0.5F);
            c.ok(c.world.getBlockState(pos.south()).getBlock() == Blocks.UNPOWERED_COMPARATOR, "comparator placed");
            c.ok(player.getAdvancements().getProgress(adv).isDone(), "The Power of Books granted");
        }
    }

    /** A click on the north face; there hitX runs from the viewer's right (0) to left (1). */
    private static void click(EntityPlayerMP player, Check c, BlockPos pos, ItemStack held, float hitX, float y) {
        player.interactionManager.processRightClickBlock(player, c.world, held, EnumHand.MAIN_HAND, pos, EnumFacing.NORTH,
                hitX, y, 0.0F);
    }
}
