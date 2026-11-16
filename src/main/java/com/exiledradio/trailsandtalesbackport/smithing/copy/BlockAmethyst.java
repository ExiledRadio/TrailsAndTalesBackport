// Copied from CavesAndCliffsBackport/src/main/java/com/exiledradio/cavesandcliffsbackport/amethyst/BlockAmethyst.java by tools/smithing/copy_from_owner.py; edit the owner's file, not this one.
package com.exiledradio.trailsandtalesbackport.smithing.copy;


import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Block of amethyst, and {@link Budding} amethyst. Both chime when walked on (at most once a second per
 * walker, louder the faster it goes) and when a projectile hits them ({@link AmethystEvents}).
 */
public class BlockAmethyst extends BlockBasic {

    public BlockAmethyst(SoundType sound) {
        super(Material.ROCK, MapColor.PURPLE, sound, 1.5F, 1.5F); // needs a pickaxe, as in Java
    }

    @Override
    public void onEntityWalk(World world, BlockPos pos, Entity entity) {
        AmethystEvents.stepChime(world, pos, entity);
        super.onEntityWalk(world, pos, entity);
    }

    /**
     * Budding amethyst: on each random tick, one in five, it picks a face at random and grows a small bud
     * there if the space is air or still water, or grows the bud already there one stage. It cannot be
     * obtained (it drops nothing, even with Silk Touch) and breaks when pushed by a piston.
     */
    public static class Budding extends BlockAmethyst {
        public Budding(SoundType sound) {
            super(sound);
            setTickRandomly(true);
        }

        @Override
        public void randomTick(World world, BlockPos pos, IBlockState state, Random rand) {
            if (rand.nextInt(5) != 0) {
                return;
            }
            EnumFacing face = EnumFacing.byIndex(rand.nextInt(6));
            BlockPos at = pos.offset(face);
            IBlockState there = world.getBlockState(at);
            BlockAmethystCluster next = null;
            if (canGrowInto(there)) {
                next = AmethystCopies.stages[0];
            } else if (there.getBlock() instanceof BlockAmethystCluster
                    && there.getValue(BlockAmethystCluster.FACING) == face) {
                next = ((BlockAmethystCluster) there.getBlock()).next;
            }
            if (next != null) {
                world.setBlockState(at, next.getDefaultState().withProperty(BlockAmethystCluster.FACING, face), 3);
            }
        }

        /** Air or a still water source, which the bud keeps (waterlogged, with Fluidlogged API installed). */
        public static boolean canGrowInto(IBlockState state) {
            return state.getBlock() == Blocks.AIR
                    || state.getBlock() == Blocks.WATER && state.getValue(net.minecraft.block.BlockLiquid.LEVEL) == 0;
        }

        @Override
        public Item getItemDropped(IBlockState state, Random rand, int fortune) {
            return net.minecraft.init.Items.AIR;
        }

        @Override
        protected boolean canSilkHarvest() {
            return false;
        }

        @Override
        public EnumPushReaction getPushReaction(IBlockState state) {
            return EnumPushReaction.DESTROY;
        }
    }

    /** Tinted glass: glass that light does not pass; drops itself, unlike glass. */
    public static class Tinted extends BlockBasic {
        public Tinted() {
            super(Material.GLASS, MapColor.GRAY, SoundType.GLASS, 0.3F, 0.3F);
            setLightOpacity(255);
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
        @net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
        public net.minecraft.util.BlockRenderLayer getRenderLayer() {
            return net.minecraft.util.BlockRenderLayer.TRANSLUCENT;
        }

        @Override
        @net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
        public boolean shouldSideBeRendered(IBlockState state, net.minecraft.world.IBlockAccess world, BlockPos pos, EnumFacing side) {
            IBlockState other = world.getBlockState(pos.offset(side));
            return other.getBlock() != this && super.shouldSideBeRendered(state, world, pos, side);
        }

        @Override
        public int quantityDropped(Random random) {
            return 1;
        }
    }

    /** The chime, at 1.18's volume (0.5 + random) and pitch (0.5 + random * 1.2). */
    static void chime(World world, BlockPos pos, float volumeScale) {
        world.playSound(null, pos, AmethystCopies.chime, SoundCategory.BLOCKS, volumeScale * (0.5F + world.rand.nextFloat()),
                0.5F + world.rand.nextFloat() * 1.2F);
    }
}
