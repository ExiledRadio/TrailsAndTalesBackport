package com.exiledradio.trailsandtalesbackport.mechanics;

import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.EnderTeleportEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Collections;
import java.util.List;

/** The smallest 1.20.x changes: hoppers under full blocks (P14), barrier landings (P16), the ender pearl's sound. */
@Mod.EventBusSubscriber(modid = TrailsAndTalesBackport.MOD_ID)
public final class SmallTweaks {

    private SmallTweaks() {
    }

    // ---- P14: hoppers (1.20.5, 24w04a) -------------------------------------------------------------------------

    /**
     * Replaces {@code TileEntityHopper.getCaptureItems(world, x, y, z)} in {@code pullItems}, reached when there is no
     * container above. 1.20.5's {@code HopperBlockEntity.suckInItems}: a hopper block (grid aligned: not a hopper
     * minecart) with a block above whose collision box is a full cube picks up no items, unless that block is a
     * beehive or bee nest ({@code #does_not_block_hoppers}).
     */
    public static List<EntityItem> captureItems(World world, double x, double y, double z) {
        if (ModConfig.mechanics.hopperPickup && blocked(world, x, y, z)) {
            return Collections.emptyList();
        }
        return TileEntityHopper.getCaptureItems(world, x, y, z);
    }

    static boolean blocked(World world, double x, double y, double z) {
        BlockPos pos = new BlockPos(x, y, z);
        // A hopper block reports its block's center; a hopper minecart its own position.
        if (x != pos.getX() + 0.5D || y != pos.getY() + 0.5D || z != pos.getZ() + 0.5D) {
            return false;
        }
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileEntityHopper)) {
            return false;
        }
        BlockPos above = pos.up();
        IBlockState state = world.getBlockState(above);
        ResourceLocation id = state.getBlock().getRegistryName();
        if (id != null && ("beehive".equals(id.getPath()) || "bee_nest".equals(id.getPath()))) {
            return false;
        }
        AxisAlignedBB box = state.getCollisionBoundingBox(world, above);
        return box != null && box.minX <= 0.0D && box.minY <= 0.0D && box.minZ <= 0.0D
                && box.maxX >= 1.0D && box.maxY >= 1.0D && box.maxZ >= 1.0D;
    }

    // ---- P16: barriers (1.20.2, 23w31a) ------------------------------------------------------------------------

    /**
     * The {@code addLandingEffects} P16 adds to {@code BlockBarrier}: true means handled, so a fall onto a barrier
     * spawns none of the dust 1.12.2 draws with the barrier's sprite. (Running over one already shows none: 1.12.2
     * skips invisible blocks there.)
     */
    public static boolean barrierLanding() {
        return ModConfig.mechanics.barrierParticles;
    }

    // ---- the ender pearl's landing sound (1.20.3, 23w43a) ------------------------------------------------------

    /**
     * 1.20.3's {@code ThrownEnderpearl.onHit} plays {@code entity.player.teleport} at the pearl when it teleports its
     * player. 1.12.2's pearl posts this event for a player it is about to teleport, with the pearl's position; a
     * chorus fruit or an enderman posts it without a pearl there.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void pearlTeleport(EnderTeleportEvent event) {
        if (!ModConfig.mechanics.pearlSound || !(event.getEntityLiving() instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.getEntityLiving();
        World world = player.world;
        double x = event.getTargetX();
        double y = event.getTargetY();
        double z = event.getTargetZ();
        AxisAlignedBB at = new AxisAlignedBB(x - 0.01D, y - 0.01D, z - 0.01D, x + 0.01D, y + 0.01D, z + 0.01D);
        for (EntityEnderPearl pearl : world.getEntitiesWithinAABB(EntityEnderPearl.class, at)) {
            if (pearl.getThrower() == player) {
                world.playSound(null, x, y, z, BlockSounds.PLAYER_TELEPORT, SoundCategory.PLAYERS, 1.0F, 1.0F);
                return;
            }
        }
    }
}
