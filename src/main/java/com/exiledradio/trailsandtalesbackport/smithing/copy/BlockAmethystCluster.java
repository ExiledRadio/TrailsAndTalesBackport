// Copied from CavesAndCliffsBackport/src/main/java/com/exiledradio/cavesandcliffsbackport/amethyst/BlockAmethystCluster.java by tools/smithing/copy_from_owner.py; edit the owner's file, not this one.
package com.exiledradio.trailsandtalesbackport.smithing.copy;

import com.exiledradio.backportcore.waterlog.Waterloggable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.NonNullList;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * The small, medium and large amethyst buds and the amethyst cluster: six facings, each needing a solid
 * face behind it, giving light 1, 2, 4 and 5. Buds drop nothing (Silk Touch gives them); the cluster drops
 * 4 shards when mined with a pickaxe (Fortune's ore formula) and 2 otherwise. Pistons break them.
 */
public class BlockAmethystCluster extends Block implements Waterloggable {

    public static final PropertyDirection FACING = BlockDirectional.FACING;

    /** The next stage a budding block grows this into; null for the cluster. */
    @Nullable
    BlockAmethystCluster next;
    private final boolean cluster;
    private final AxisAlignedBB[] boxes = new AxisAlignedBB[6];
    private final int light;

    /**
     * @param height and offset as 1.18's: cluster 7/3, large 5/3, medium 4/3, small 3/4 (sixteenths)
     */
    public BlockAmethystCluster(SoundType sound, int height, int offset, int light, boolean cluster) {
        super(Material.GLASS, MapColor.PURPLE);
        setSoundType(sound);
        setHardness(1.5F);
        BlockBasic.resistance(this, 1.5F);
        this.light = light;
        this.cluster = cluster;
        double h = height / 16.0, o = offset / 16.0, p = 1 - o;
        this.boxes[EnumFacing.UP.getIndex()] = new AxisAlignedBB(o, 0, o, p, h, p);
        this.boxes[EnumFacing.DOWN.getIndex()] = new AxisAlignedBB(o, 1 - h, o, p, 1, p);
        this.boxes[EnumFacing.NORTH.getIndex()] = new AxisAlignedBB(o, o, 1 - h, p, p, 1);
        this.boxes[EnumFacing.SOUTH.getIndex()] = new AxisAlignedBB(o, o, 0, p, p, h);
        this.boxes[EnumFacing.EAST.getIndex()] = new AxisAlignedBB(0, o, o, h, p, p);
        this.boxes[EnumFacing.WEST.getIndex()] = new AxisAlignedBB(1 - h, o, o, 1, p, p);
        setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.UP));
    }

    public boolean isCluster() {
        return this.cluster;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING, EnumFacing.byIndex(meta & 7));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getIndex();
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirror) {
        return state.withProperty(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        return this.light;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return this.boxes[state.getValue(FACING).getIndex()];
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
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    @net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
    public net.minecraft.util.BlockRenderLayer getRenderLayer() {
        return net.minecraft.util.BlockRenderLayer.CUTOUT;
    }

    @Override
    public EnumPushReaction getPushReaction(IBlockState state) {
        return EnumPushReaction.DESTROY;
    }

    // ---------------------------------------------------------------------------------------------
    // Support

    private static boolean supported(World world, BlockPos pos, EnumFacing facing) {
        BlockPos behind = pos.offset(facing.getOpposite());
        return world.getBlockState(behind).getBlockFaceShape(world, behind, facing) == BlockFaceShape.SOLID;
    }

    @Override
    public boolean canPlaceBlockOnSide(World world, BlockPos pos, EnumFacing side) {
        return super.canPlaceBlockAt(world, pos) && supported(world, pos, side);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ,
                                            int meta, EntityLivingBase placer) {
        return getDefaultState().withProperty(FACING, facing);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos from) {
        if (!supported(world, pos, state.getValue(FACING))) {
            dropBlockAsItem(world, pos, state, 0);
            world.setBlockToAir(pos);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Drops

    /** Set by the harvest that is breaking this block, so drops can know the tool (1.12 does not pass it). */
    private static final ThreadLocal<ItemStack> TOOL = new ThreadLocal<ItemStack>();

    @Override
    public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state,
                             @Nullable net.minecraft.tileentity.TileEntity te, ItemStack stack) {
        TOOL.set(stack);
        try {
            super.harvestBlock(world, player, pos, state, te, stack);
        } finally {
            TOOL.remove();
        }
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        if (!this.cluster || AmethystCopies.shard == null) {
            return;
        }
        ItemStack tool = TOOL.get();
        boolean pickaxe = tool != null && !tool.isEmpty() && tool.getItem().getToolClasses(tool).contains("pickaxe");
        int count = pickaxe ? 4 : 2;
        if (pickaxe && fortune > 0) {
            Random r = world instanceof World ? ((World) world).rand : new Random();
            count *= Math.max(r.nextInt(fortune + 2) - 1, 0) + 1;
        }
        drops.add(new ItemStack(AmethystCopies.shard, count));
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return this.cluster ? AmethystCopies.shard : Items.AIR;
    }

    @Override
    protected ItemStack getSilkTouchDrop(IBlockState state) {
        return new ItemStack(this);
    }

    @Override
    public boolean canSilkHarvest(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        return true;
    }
}
