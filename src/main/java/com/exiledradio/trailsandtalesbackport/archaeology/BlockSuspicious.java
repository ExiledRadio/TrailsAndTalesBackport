package com.exiledradio.trailsandtalesbackport.archaeology;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Suspicious sand and suspicious gravel: 1.20's {@code BrushableBlock}.
 *
 * <ul>
 *   <li>{@code dusted} 0-3, in metadata; the brushing itself is in {@link TileSuspicious}.</li>
 *   <li>Every placement and neighbor change schedules a tick 2 ticks later. The tick lets the tile entity
 *       wind its brushing back ({@link TileSuspicious#checkReset}) and, with nothing solid below, the block
 *       falls like sand. A falling suspicious block never becomes a block again: where it lands it breaks
 *       with the block-break particles and sound, and drops nothing (vanilla's falling block with its drop
 *       disabled). 1.12.2's falling entity places the block first, so {@link #onEndFalling} removes it at
 *       once.</li>
 *   <li>Mined, pushed by a piston or blown up, it drops nothing (1.20's loot table for it is empty).</li>
 * </ul>
 */
public class BlockSuspicious extends BlockFalling {

    public static final PropertyInteger DUSTED = PropertyInteger.create("dusted", 0, 3);

    /** Vanilla's {@code BrushableBlock.TICK_DELAY}. */
    public static final int TICK_DELAY = 2;

    private final IBlockState turnsInto;
    private final MapColor mapColor;
    final SoundEvent brushSound;
    final SoundEvent brushCompletedSound;

    public BlockSuspicious(IBlockState turnsInto, MapColor mapColor, SoundType sound, SoundEvent brushSound,
                           SoundEvent brushCompletedSound) {
        super(Material.SAND);
        this.turnsInto = turnsInto;
        this.mapColor = mapColor;
        this.brushSound = brushSound;
        this.brushCompletedSound = brushCompletedSound;
        setHardness(0.25F);
        setSoundType(sound);
        setDefaultState(this.blockState.getBaseState().withProperty(DUSTED, 0));
    }

    /** @return the block brushing it to the end leaves behind: sand or gravel */
    public IBlockState turnsInto() {
        return this.turnsInto;
    }

    public SoundEvent brushSound() {
        return this.brushSound;
    }

    public SoundEvent brushCompletedSound() {
        return this.brushCompletedSound;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, new IProperty[]{DUSTED});
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(DUSTED, meta & 3);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(DUSTED);
    }

    @Override
    public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
        return this.mapColor;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileSuspicious();
    }

    // --- ticks and falling -----------------------------------------------------------------------

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        world.scheduleUpdate(pos, this, TICK_DELAY);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
        world.scheduleUpdate(pos, this, TICK_DELAY);
    }

    @Override
    public int tickRate(World world) {
        return TICK_DELAY;
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        if (world.isRemote) {
            return;
        }
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileSuspicious) {
            ((TileSuspicious) te).checkReset();
        }
        if (canFallThrough(world.getBlockState(pos.down())) && pos.getY() >= 0 && !fallInstantly) {
            EntityFallingBlock falling = new EntityFallingBlock(world, pos.getX() + 0.5D, pos.getY(),
                    pos.getZ() + 0.5D, world.getBlockState(pos));
            falling.shouldDropItem = false;
            onStartFalling(falling);
            world.spawnEntity(falling);
        }
    }

    /** Where it lands it breaks (vanilla's {@code onBrokenAfterFall}: the block-break effect, no drop). */
    @Override
    public void onEndFalling(World world, BlockPos pos, IBlockState fallingState, IBlockState hitState) {
        if (world.getBlockState(pos).getBlock() == this) {
            world.setBlockToAir(pos);
        }
        world.playEvent(2001, pos, Block.getStateId(fallingState));
    }

    // --- drops -----------------------------------------------------------------------------------

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Items.AIR;
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    @Override
    public boolean canSilkHarvest(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        return false;
    }

    @Override
    public EnumPushReaction getPushReaction(IBlockState state) {
        return EnumPushReaction.DESTROY;
    }

    /**
     * 1.20's suspicious blocks are not falling blocks there, so their falling dust takes the block's map
     * color (sand's, stone's) rather than sand's dust color.
     */
    @Override
    public int getDustColor(IBlockState state) {
        return this.mapColor.colorValue;
    }
}
