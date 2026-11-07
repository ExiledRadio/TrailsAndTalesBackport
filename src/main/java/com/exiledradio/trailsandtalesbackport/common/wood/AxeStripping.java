package com.exiledradio.trailsandtalesbackport.common.wood;

import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.common.Content;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Right-clicking one of this mod's logs with an axe strips it (1.13's rule), keeping its axis.
 *
 * <p>Only for this mod's own blocks: other mods' stripping handlers look up their own logs, so the
 * handlers never compete for the same block. Streams add their pairs with {@link #add}.
 */
@Mod.EventBusSubscriber(modid = TrailsAndTalesBackport.MOD_ID)
public final class AxeStripping {

    /** 1.13's {@code item.axe.strip}; 1.12.2 has no stripping sound. */
    public static final SoundEvent STRIP = event("item.axe.strip");

    private static final Map<Block, Block> PAIRS = new HashMap<Block, Block>();

    private AxeStripping() {
    }

    private static SoundEvent event(String name) {
        SoundEvent e = new SoundEvent(Content.id(name));
        e.setRegistryName(Content.id(name));
        return e;
    }

    public static void add(Map<Block, Block> pairs) {
        PAIRS.putAll(pairs);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        event.getRegistry().register(STRIP);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack held = event.getItemStack();
        if (held.isEmpty() || !held.getItem().getToolClasses(held).contains("axe")) {
            return;
        }
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        IBlockState state = world.getBlockState(pos);
        Block stripped = PAIRS.get(state.getBlock());
        if (stripped == null) {
            return;
        }
        EntityPlayer player = event.getEntityPlayer();
        world.playSound(player, pos, STRIP, SoundCategory.BLOCKS, 1.0F, 1.0F);
        if (!world.isRemote) {
            IBlockState out = stripped.getDefaultState();
            if (state.getPropertyKeys().contains(BlockRotatedPillar.AXIS)) {
                out = out.withProperty(BlockRotatedPillar.AXIS, state.getValue(BlockRotatedPillar.AXIS));
            }
            world.setBlockState(pos, out, 11);
            held.damageItem(1, player);
        }
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
    }
}
