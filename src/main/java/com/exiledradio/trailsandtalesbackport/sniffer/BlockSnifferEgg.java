package com.exiledradio.trailsandtalesbackport.sniffer;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * 1.20's sniffer egg: three stages, then a snifflet.
 *
 * <p>As 1.20's {@code SnifferEggBlock}: placing it schedules its next stage a third of 24000 ticks away
 * (12000 on a hatch-boost block - Caves & Cliffs' moss block), plus up to 299; each stage cracks (sound) and
 * schedules the next, and the third hatches a baby sniffer at the block's center, facing a random way. On
 * moss the egg shows green sparkles when placed. Mobs never path through it. Hardness 0.5, metal sounds.
 *
 * <p>1.12.2 runs {@code onBlockAdded} only when the block itself changes, not for a new stage of the same
 * block, so each stage schedules the next from {@code updateTick}; the delay is the same as 1.20's.
 */
public class BlockSnifferEgg extends Block {

    public static final PropertyInteger HATCH = PropertyInteger.create("hatch", 0, 2);
    public static final int REGULAR_HATCH_TIME_TICKS = 24000;
    public static final int BOOSTED_HATCH_TIME_TICKS = 12000;
    public static final int RANDOM_HATCH_OFFSET_TICKS = 300;
    /** Block event: the moss sparkles (1.20's level event 3009), shown by every client nearby. */
    static final int EVENT_BOOST_PARTICLES = 1;

    private static final AxisAlignedBB AABB = new AxisAlignedBB(1.0D / 16, 0.0D, 2.0D / 16, 15.0D / 16, 1.0D, 14.0D / 16);

    public BlockSnifferEgg() {
        super(Material.CLAY, MapColor.RED);
        setHardness(0.5F);
        setSoundType(SoundType.METAL);
        setDefaultState(this.blockState.getBaseState().withProperty(HATCH, 0));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, HATCH);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(HATCH, Math.min(meta, 2));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(HATCH);
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return AABB;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public boolean isPassable(IBlockAccess world, BlockPos pos) {
        return false;
    }

    @Override
    public net.minecraft.pathfinding.PathNodeType getAiPathNodeType(IBlockState state, IBlockAccess world, BlockPos pos, EntityLiving entity) {
        return net.minecraft.pathfinding.PathNodeType.BLOCKED;
    }

    /** @return true if the block below speeds hatching (1.20's {@code sniffer_egg_hatch_boost}: moss) */
    public static boolean hatchBoost(IBlockAccess world, BlockPos pos) {
        return SnifferContent.isHatchBoost(world.getBlockState(pos.down()));
    }

    /** @return the ticks to the next stage: a third of the hatch time, plus 0-299 */
    static int stageDelay(World world, BlockPos pos) {
        int total = hatchBoost(world, pos) ? BOOSTED_HATCH_TIME_TICKS : REGULAR_HATCH_TIME_TICKS;
        return total / 3 + world.rand.nextInt(RANDOM_HATCH_OFFSET_TICKS);
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        if (world.isRemote) {
            return;
        }
        boolean boost = hatchBoost(world, pos);
        if (boost) {
            world.addBlockEvent(pos, this, EVENT_BOOST_PARTICLES, 0);
        }
        world.scheduleUpdate(pos, this, stageDelay(world, pos));
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean eventReceived(IBlockState state, World world, BlockPos pos, int id, int param) {
        if (id == EVENT_BOOST_PARTICLES) {
            if (world.isRemote) {
                SnifferContent.proxy().eggBoostParticles(world, pos);
            }
            return true;
        }
        return false;
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        if (world.isRemote) {
            return;
        }
        int hatch = state.getValue(HATCH);
        if (hatch < 2) {
            world.playSound(null, pos, SnifferContent.EGG_CRACK, SoundCategory.BLOCKS, 0.7F, 0.9F + rand.nextFloat() * 0.2F);
            world.setBlockState(pos, state.withProperty(HATCH, hatch + 1), 2);
            world.scheduleUpdate(pos, this, stageDelay(world, pos));
            if (hatchBoost(world, pos)) {
                world.addBlockEvent(pos, this, EVENT_BOOST_PARTICLES, 0);
            }
            return;
        }
        world.playSound(null, pos, SnifferContent.EGG_HATCH, SoundCategory.BLOCKS, 0.7F, 0.9F + rand.nextFloat() * 0.2F);
        world.destroyBlock(pos, false);
        EntitySniffer sniffer = new EntitySniffer(world);
        sniffer.setGrowingAge(-EntitySniffer.BABY_AGE_TICKS);
        sniffer.setLocationAndAngles(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                MathHelper.wrapDegrees(world.rand.nextFloat() * 360.0F), 0.0F);
        world.spawnEntity(sniffer);
    }
}
