// Copied from CavesAndCliffsBackport/src/main/java/com/exiledradio/cavesandcliffsbackport/amethyst/AmethystEvents.java by tools/smithing/copy_from_owner.py; edit the owner's file, not this one.
package com.exiledradio.trailsandtalesbackport.smithing.copy;

import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Amethyst chimes: when a projectile hits amethyst, and when something walks on it. */
@Mod.EventBusSubscriber(modid = TrailsAndTalesBackport.MOD_ID)
public final class AmethystEvents {

    private static final String LAST_CHIME = TrailsAndTalesBackport.MOD_ID + ":chime";

    private AmethystEvents() {
    }

    static boolean chimes(Block b) {
        return b instanceof BlockAmethyst && !(b instanceof BlockAmethyst.Tinted);
    }

    @SubscribeEvent
    public static void projectile(ProjectileImpactEvent event) {
        RayTraceResult hit = event.getRayTraceResult();
        World world = event.getEntity().world;
        if (world.isRemote || hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK || !AmethystCopies.registering()) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        Block b = world.getBlockState(pos).getBlock();
        if (chimes(b)) {
            world.playSound(null, pos, b.getSoundType().getHitSound(), net.minecraft.util.SoundCategory.BLOCKS, 1.0F, 1.0F);
            BlockAmethyst.chime(world, pos, 1.0F);
        }
    }

    /** At most one chime a second for each walker; quieter at walking pace (current Java). */
    static void stepChime(World world, BlockPos pos, Entity entity) {
        if (world.isRemote || entity.isSneaking()) {
            return;
        }
        long now = world.getTotalWorldTime();
        long last = entity.getEntityData().getLong(LAST_CHIME);
        // Distance moved this tick: a server's player motion fields are not kept up to date, positions are.
        double dx = entity.posX - entity.lastTickPosX, dz = entity.posZ - entity.lastTickPosZ;
        double speed = Math.sqrt(dx * dx + dz * dz);
        if (now < last + 20 || speed < 0.01) {
            return;
        }
        entity.getEntityData().setLong(LAST_CHIME, now);
        BlockAmethyst.chime(world, pos, (float) Math.min(1.0, speed * 3));
    }
}
