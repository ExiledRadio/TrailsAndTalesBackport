package com.exiledradio.trailsandtalesbackport.cherry;

import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.IGrowable;
import net.minecraft.block.SoundType;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.NonNullList;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Pink petals: 1 to 4 petals on the ground, turned to face the player who placed the first.
 *
 * <ul>
 *   <li>Placing more on a block of petals adds one, up to 4 ({@link ItemPinkPetals}).</li>
 *   <li>Bone meal adds one; on a full block it drops one petal instead.</li>
 *   <li>Drops as many petals as it has.</li>
 *   <li>Its outline covers the quadrants its petals fill (1.20.2), in the model's order: for a block
 *       facing north, north-west, south-west, south-east, north-east.</li>
 * </ul>
 */
public class BlockPinkPetals extends BlockBush implements IGrowable {

    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    public static final PropertyInteger AMOUNT = PropertyInteger.create("flower_amount", 1, 4);

    /** The quadrants of a north-facing block, in petal order, as {minX, minZ} in sixteenths. */
    private static final int[][] QUADRANTS = {{0, 0}, {0, 8}, {8, 8}, {8, 0}};
    private static final AxisAlignedBB[][] BOXES = new AxisAlignedBB[4][4];

    static {
        EnumFacing[] order = {EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.WEST};
        for (int f = 0; f < 4; f++) {
            for (int amount = 1; amount <= 4; amount++) {
                double minX = 16, minZ = 16, maxX = 0, maxZ = 0;
                for (int i = 0; i < amount; i++) {
                    int x = QUADRANTS[i][0];
                    int z = QUADRANTS[i][1];
                    // y rotation by 90 per step, clockwise from above: (x, z) -> (16 - z, x) on the quadrant's far corner.
                    for (int r = 0; r < f; r++) {
                        int nx = 8 - z;
                        z = x;
                        x = nx;
                    }
                    minX = Math.min(minX, x);
                    minZ = Math.min(minZ, z);
                    maxX = Math.max(maxX, x + 8);
                    maxZ = Math.max(maxZ, z + 8);
                }
                BOXES[order[f].getHorizontalIndex()][amount - 1] =
                        new AxisAlignedBB(minX / 16, 0, minZ / 16, maxX / 16, 3.0D / 16, maxZ / 16);
            }
        }
    }

    private Item item;

    public BlockPinkPetals(SoundType sound) {
        setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(AMOUNT, 1));
        setSoundType(sound);
        setHardness(0.0F);
    }

    void setItem(Item item) {
        this.item = item;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, AMOUNT);
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3)).withProperty(AMOUNT, 1 + (meta >> 2));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex() | (state.getValue(AMOUNT) - 1) << 2;
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

    /** 1.20: facing the opposite way to the placer. */
    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ,
                                            int meta, EntityLivingBase placer, EnumHand hand) {
        return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return BOXES[state.getValue(FACING).getHorizontalIndex()][state.getValue(AMOUNT) - 1];
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        drops.add(new ItemStack(this.item, state.getValue(AMOUNT)));
    }

    @Override
    public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
        return new ItemStack(this.item);
    }

    @Override
    public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean isClient) {
        return true;
    }

    @Override
    public boolean canUseBonemeal(World world, Random rand, BlockPos pos, IBlockState state) {
        return true;
    }

    @Override
    public void grow(World world, Random rand, BlockPos pos, IBlockState state) {
        int amount = state.getValue(AMOUNT);
        if (amount < 4) {
            world.setBlockState(pos, state.withProperty(AMOUNT, amount + 1), 2);
        } else {
            spawnAsEntity(world, pos, new ItemStack(this.item));
        }
    }

    /** @return true if more petals can be placed into this state */
    public static boolean canAddTo(IBlockState state) {
        return state.getBlock() instanceof BlockPinkPetals && state.getValue(AMOUNT) < 4;
    }
}
