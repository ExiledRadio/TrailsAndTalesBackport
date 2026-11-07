package com.exiledradio.trailsandtalesbackport.bookshelf;

import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

/**
 * The chiseled bookshelf.
 *
 * <h2>1.20's rules</h2>
 * <ul>
 *   <li>Faces the player who placed it; only that face takes books.</li>
 *   <li>The face is two rows of three slots. Clicking a slot with a book puts one in; clicking a full slot
 *       takes its book out (into the hand, or dropped if there is no room). Clicking an empty slot with
 *       anything else does nothing at all (it doesn't place the held block either).</li>
 *   <li>A comparator reads the slot last touched, 1-6.</li>
 *   <li>Breaking it drops the books; the shelf itself only with Silk Touch. No enchanting power.</li>
 * </ul>
 *
 * <h2>Slots in the state</h2>
 * The six {@code slot_N_occupied} properties are worked out from the tile ({@link #getActualState}); only
 * the facing is saved in metadata. They have 1.20's names, so 1.20's multipart blockstate fits as it is.
 */
public class BlockChiseledBookshelf extends Block implements ITileEntityProvider {

    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    public static final PropertyBool[] SLOTS = new PropertyBool[TileChiseledBookshelf.SLOTS];

    static {
        for (int i = 0; i < SLOTS.length; i++) {
            SLOTS[i] = PropertyBool.create("slot_" + i + "_occupied");
        }
    }

    private final SoundEvent insert;
    private final SoundEvent insertEnchanted;
    private final SoundEvent pickup;
    private final SoundEvent pickupEnchanted;

    public BlockChiseledBookshelf(SoundType sound, SoundEvent insert, SoundEvent insertEnchanted, SoundEvent pickup,
                                  SoundEvent pickupEnchanted) {
        super(Material.WOOD, MapColor.WOOD);
        this.insert = insert;
        this.insertEnchanted = insertEnchanted;
        this.pickup = pickup;
        this.pickupEnchanted = pickupEnchanted;
        setHardness(1.5F);
        setSoundType(sound);
        IBlockState state = this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH);
        for (PropertyBool slot : SLOTS) {
            state = state.withProperty(slot, false);
        }
        setDefaultState(state);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        net.minecraft.block.properties.IProperty<?>[] props = new net.minecraft.block.properties.IProperty<?>[SLOTS.length + 1];
        props[0] = FACING;
        System.arraycopy(SLOTS, 0, props, 1, SLOTS.length);
        return new BlockStateContainer(this, props);
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex();
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileChiseledBookshelf) {
            for (int i = 0; i < SLOTS.length; i++) {
                state = state.withProperty(SLOTS[i], ((TileChiseledBookshelf) tile).occupied(i));
            }
        }
        return state;
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState withRotation(IBlockState state, Rotation rotation) {
        return state.withProperty(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState withMirror(IBlockState state, Mirror mirror) {
        return state.withRotation(mirror.toRotation(state.getValue(FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ,
                                            int meta, EntityLivingBase placer, EnumHand hand) {
        return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileChiseledBookshelf();
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * 1.20's {@code getHitSlot}: where on the front face the click landed. Across the face, from the
     * viewer's left: under 6/16 is the first column, under 11/16 the second, else the third; the top half is
     * the first row.
     *
     * @return 0-5, or -1 if the click was not on the front
     */
    public static int slotAt(EnumFacing front, EnumFacing clicked, float hitX, float hitY, float hitZ) {
        if (clicked != front) {
            return -1;
        }
        float across;
        switch (front) {
            case NORTH:
                across = 1.0F - hitX;
                break;
            case SOUTH:
                across = hitX;
                break;
            case WEST:
                across = hitZ;
                break;
            default:
                across = 1.0F - hitZ;
                break;
        }
        int row = hitY >= 0.5F ? 0 : 1;
        int column = across < 0.375F ? 0 : across < 0.6875F ? 1 : 2;
        return column + row * 3;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY, float hitZ) {
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileChiseledBookshelf)) {
            return false;
        }
        int slot = slotAt(state.getValue(FACING), facing, hitX, hitY, hitZ);
        if (slot < 0) {
            return false;
        }
        TileChiseledBookshelf shelf = (TileChiseledBookshelf) tile;
        if (shelf.occupied(slot)) {
            if (!world.isRemote) {
                ItemStack book = shelf.take(slot);
                world.playSound(null, pos, book.getItem() == Items.ENCHANTED_BOOK ? this.pickupEnchanted : this.pickup,
                        SoundCategory.BLOCKS, 1.0F, 1.0F);
                if (!player.inventory.addItemStackToInventory(book)) {
                    player.dropItem(book, false);
                }
            }
            return true;
        }
        ItemStack held = player.getHeldItem(hand);
        if (TileChiseledBookshelf.isBook(held)) {
            if (!world.isRemote) {
                world.playSound(null, pos, held.getItem() == Items.ENCHANTED_BOOK ? this.insertEnchanted : this.insert,
                        SoundCategory.BLOCKS, 1.0F, 1.0F);
                shelf.put(slot, held);
                if (!player.capabilities.isCreativeMode) {
                    held.shrink(1);
                }
            }
            return true;
        }
        // 1.20 consumes the click: nothing happens, and nothing is placed against the shelf.
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        return tile instanceof TileChiseledBookshelf ? ((TileChiseledBookshelf) tile).signal() : 0;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileChiseledBookshelf) {
            for (int i = 0; i < TileChiseledBookshelf.SLOTS; i++) {
                ItemStack book = ((TileChiseledBookshelf) tile).book(i);
                if (!book.isEmpty()) {
                    InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), book.copy());
                }
            }
            world.updateComparatorOutputLevel(pos, this);
        }
        super.breakBlock(world, pos, state);
    }

    /** Only Silk Touch gives the shelf back. */
    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Items.AIR;
    }

    @Override
    public boolean canSilkHarvest(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        return true;
    }
}
