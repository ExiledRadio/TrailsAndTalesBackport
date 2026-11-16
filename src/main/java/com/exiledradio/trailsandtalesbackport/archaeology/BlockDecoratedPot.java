package com.exiledradio.trailsandtalesbackport.archaeology;

import com.exiledradio.backportcore.gameevent.GameEvent;
import com.exiledradio.trailsandtalesbackport.common.Vibrations;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.NonNullList;
import net.minecraft.util.Rotation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Random;
import java.util.function.Predicate;

/**
 * The decorated pot: 1.20.4's {@code DecoratedPotBlock}.
 *
 * <ul>
 *   <li>Placed facing the way the player faces; drawn only by its tile entity renderer.</li>
 *   <li>Used with an item: an empty pot takes one, a pot holding that item takes one more up to a stack
 *       (it wobbles, fills with a rising pitch and puffs dust); anything else, or an empty hand, makes it
 *       wobble the other way with the fail sound. Creative players keep their item.</li>
 *   <li>Broken with a sword, axe, pickaxe, shovel, hoe or trident (and no Silk Touch) it cracks and drops
 *       its four sherds and bricks with the shatter sound; otherwise it drops itself with its faces. A
 *       projectile shatters it too. A piston breaks it (drops itself). Whatever it holds spills out.</li>
 *   <li>A comparator reads how full it is.</li>
 * </ul>
 */
public class BlockDecoratedPot extends Block {

    public static final PropertyDirection FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
    public static final PropertyBool CRACKED = PropertyBool.create("cracked");

    /** 1.20's shape: a 14-wide column, full height. */
    private static final AxisAlignedBB SHAPE = new AxisAlignedBB(1 / 16D, 0, 1 / 16D, 15 / 16D, 1, 15 / 16D);

    /**
     * Client hook: true while the local player is breaking the pot at this position with a tool that shatters it. The breaking
     * player's own client plays the break sound from the state it still has, which is not cracked yet.
     */
    public static Predicate<BlockPos> localShattering = pos -> false;

    private final SoundType sound;
    private final SoundType crackedSound;
    private final SoundEvent insert;
    private final SoundEvent insertFail;

    public BlockDecoratedPot(SoundType sound, SoundType crackedSound, SoundEvent insert, SoundEvent insertFail) {
        super(Material.CLAY, MapColor.RED_STAINED_HARDENED_CLAY);
        this.sound = sound;
        this.crackedSound = crackedSound;
        this.insert = insert;
        this.insertFail = insertFail;
        setHardness(0.0F);
        setResistance(0.0F);
        setSoundType(sound);
        setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(CRACKED, false));
    }

    Item potItem() {
        return Item.getItemFromBlock(this);
    }

    // --- states ----------------------------------------------------------------------------------

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, new IProperty[]{FACING, CRACKED});
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
                .withProperty(CRACKED, (meta & 4) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex() | (state.getValue(CRACKED) ? 4 : 0);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY,
                                            float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        return getDefaultState().withProperty(FACING, placer.getHorizontalFacing()).withProperty(CRACKED, false);
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirror) {
        return state.withRotation(mirror.toRotation(state.getValue(FACING)));
    }

    // --- shape and drawing -----------------------------------------------------------------------

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return SHAPE;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return face.getAxis() == EnumFacing.Axis.Y ? BlockFaceShape.CENTER_BIG : BlockFaceShape.UNDEFINED;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public SoundType getSoundType(IBlockState state, World world, BlockPos pos, @Nullable Entity entity) {
        if (state.getValue(CRACKED)) {
            return this.crackedSound;
        }
        // The break effect (2001) asks with no entity; on the breaking player's client the pot is not yet
        // cracked there.
        if (entity == null && world != null && world.isRemote && localShattering.test(pos)) {
            return this.crackedSound;
        }
        return this.sound;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileDecoratedPot();
    }

    @Override
    public boolean eventReceived(IBlockState state, World world, BlockPos pos, int id, int param) {
        TileEntity te = world.getTileEntity(pos);
        return te != null && te.receiveClientEvent(id, param);
    }

    // --- use -------------------------------------------------------------------------------------

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY, float hitZ) {
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileDecoratedPot)) {
            return false;
        }
        if (world.isRemote) {
            return true;
        }
        TileDecoratedPot pot = (TileDecoratedPot) te;
        ItemStack held = player.getHeldItem(hand);
        ItemStack inside = pot.getTheItem();
        if (!held.isEmpty() && (inside.isEmpty()
                || ItemStack.areItemsEqual(inside, held) && ItemStack.areItemStackTagsEqual(inside, held)
                && inside.getCount() < inside.getMaxStackSize())) {
            pot.wobble(TileDecoratedPot.Wobble.POSITIVE);
            StatBase stat = StatList.getObjectUseStats(held.getItem());
            if (stat != null) {
                player.addStat(stat);
            }
            ItemStack one = held.copy();
            one.setCount(1);
            if (!player.capabilities.isCreativeMode) {
                held.shrink(1);
            }
            float fill;
            if (pot.isEmpty()) {
                pot.setTheItem(one);
                fill = (float) one.getCount() / (float) one.getMaxStackSize();
            } else {
                inside.grow(1);
                fill = (float) inside.getCount() / (float) inside.getMaxStackSize();
            }
            world.playSound(null, pos, this.insert, SoundCategory.BLOCKS, 1.0F, 0.7F + 0.5F * fill);
            ArchaeologyNetwork.dustPlume(world, pos);
            pot.changed();
            Vibrations.block(world, GameEvent.BLOCK_CHANGE, pos, player);
        } else {
            world.playSound(null, pos, this.insertFail, SoundCategory.BLOCKS, 1.0F, 1.0F);
            pot.wobble(TileDecoratedPot.Wobble.NEGATIVE);
            Vibrations.block(world, GameEvent.BLOCK_CHANGE, pos, player);
        }
        return true;
    }

    // --- breaking --------------------------------------------------------------------------------

    /** 1.20's {@code breaks_decorated_pots} item tag: swords, axes, pickaxes, shovels, hoes, the trident. */
    public static boolean breaksPots(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        if (item instanceof ItemSword || item instanceof ItemHoe) {
            return true;
        }
        for (String tool : item.getToolClasses(stack)) {
            if ("pickaxe".equals(tool) || "axe".equals(tool) || "shovel".equals(tool)) {
                return true;
            }
        }
        return item.getRegistryName() != null && "trident".equals(item.getRegistryName().getPath());
    }

    /** True if breaking with this main-hand item cracks the pot (vanilla's {@code playerWillDestroy}). */
    public static boolean shatters(ItemStack mainHand) {
        return breaksPots(mainHand) && EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, mainHand) == 0;
    }

    /** Keeps the tile entity until the drops are made (as the flower pot does). */
    @Override
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
        if (willHarvest) {
            return true;
        }
        return super.removedByPlayer(state, world, pos, player, willHarvest);
    }

    @Override
    public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state, @Nullable TileEntity te,
                             ItemStack tool) {
        // The state passed in was read before {@link ArchaeologyContent} cracked the pot; the world has the
        // cracked one.
        IBlockState now = world.getBlockState(pos);
        super.harvestBlock(world, player, pos, now.getBlock() == this ? now : state, te, tool);
        world.setBlockToAir(pos);
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        TileEntity te = world.getTileEntity(pos);
        Item[] sherds = te instanceof TileDecoratedPot ? ((TileDecoratedPot) te).sherds()
                : new Item[]{Items.BRICK, Items.BRICK, Items.BRICK, Items.BRICK};
        if (state.getValue(CRACKED)) {
            for (Item s : sherds) {
                drops.add(new ItemStack(s));
            }
        } else {
            drops.add(TileDecoratedPot.potItem(potItem(), sherds));
        }
    }

    @Override
    public boolean canSilkHarvest(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        return false;
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileDecoratedPot) {
            return TileDecoratedPot.potItem(potItem(), ((TileDecoratedPot) te).sherds());
        }
        return new ItemStack(potItem());
    }

    /** Whatever it holds spills out when it goes, however it goes (vanilla's {@code onRemove}). */
    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileDecoratedPot) {
            ItemStack inside = ((TileDecoratedPot) te).getTheItem();
            if (!inside.isEmpty()) {
                InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), inside.copy());
                ((TileDecoratedPot) te).setTheItem(ItemStack.EMPTY);
            }
            world.updateComparatorOutputLevel(pos, this);
        }
        super.breakBlock(world, pos, state);
    }

    /**
     * A projectile shatters it ({@link ArchaeologyContent} calls this from the impact event): 1.20's
     * {@code destroyBlock(pos, true, projectile)}, which also posts {@code block_destroy} from the projectile.
     */
    public void shatter(World world, BlockPos pos, IBlockState state, @Nullable net.minecraft.entity.Entity projectile) {
        world.setBlockState(pos, state.withProperty(CRACKED, true), 4);
        IBlockState cracked = world.getBlockState(pos);
        if (world.destroyBlock(pos, true)) {
            Vibrations.block(world, GameEvent.BLOCK_DESTROY, pos, projectile, cracked);
        }
    }

    @Override
    public EnumPushReaction getPushReaction(IBlockState state) {
        return EnumPushReaction.DESTROY;
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        return te instanceof TileDecoratedPot ? ((TileDecoratedPot) te).comparatorSignal() : 0;
    }

    @Override
    public boolean isPassable(IBlockAccess world, BlockPos pos) {
        return false;
    }

    @Override
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
    }
}
