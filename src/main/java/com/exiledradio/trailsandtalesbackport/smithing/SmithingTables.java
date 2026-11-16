package com.exiledradio.trailsandtalesbackport.smithing;

import com.exiledradio.backportcore.smithing.SmithingTemplates;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.common.Owners;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 1.20's smithing table on whichever table is in the game.
 *
 * <p>1.20 has only the three-slot table, and the template requirement is on by default (Decided 2). Village &amp;
 * Pillage's table and Nether Update's copy of it are 1.16's two-slot table; until their own code has the template
 * slot ({@code SmithingTemplates.declareTableSupport} from them, Backport Core request 1), right-clicking either
 * opens this mod's 1.20 container instead, on that block. Their tables stay theirs otherwise (drops, recipe,
 * placement). Once a table's owner declares support, its own screen is left alone.
 *
 * <p>As vanilla, sneaking with an item in either hand places the item rather than opening the table.
 */
public final class SmithingTables {

    private static final ResourceLocation VP_TABLE = new ResourceLocation(Owners.VILLAGE_AND_PILLAGE, "smithing_table");
    private static final ResourceLocation NU_TABLE = new ResourceLocation(Owners.NETHER_UPDATE, "smithing_table");

    private SmithingTables() {
    }

    static void preInit() {
        if (Loader.isModLoaded(Owners.VILLAGE_AND_PILLAGE) || Loader.isModLoaded(Owners.NETHER_UPDATE)) {
            MinecraftForge.EVENT_BUS.register(new SmithingTables());
        }
    }

    /** @return true if this block is a smithing table our container opens on */
    public static boolean isTable(Block block) {
        ResourceLocation id = block.getRegistryName();
        if (id == null) {
            return false;
        }
        if (block instanceof BlockSmithingTable) {
            return true;
        }
        return VP_TABLE.equals(id) && !SmithingTemplates.tables().contains(Owners.VILLAGE_AND_PILLAGE)
                || NU_TABLE.equals(id) && !SmithingTemplates.tables().contains(Owners.NETHER_UPDATE);
    }

    static void open(EntityPlayer player, BlockPos pos) {
        player.openGui(TrailsAndTalesBackport.instance, SmithingGui.SMITHING, player.world, pos.getX(), pos.getY(), pos.getZ());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void rightClick(PlayerInteractEvent.RightClickBlock event) {
        Block block = event.getWorld().getBlockState(event.getPos()).getBlock();
        if (block instanceof BlockSmithingTable || !isTable(block)) {
            return;
        }
        EntityPlayer player = event.getEntityPlayer();
        if (player.isSneaking() && (!player.getHeldItemMainhand().isEmpty() || !player.getHeldItemOffhand().isEmpty())) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
        if (!event.getWorld().isRemote) {
            open(player, event.getPos());
        }
    }
}
