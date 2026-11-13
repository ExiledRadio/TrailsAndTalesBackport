package com.exiledradio.trailsandtalesbackport.heads;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityNote;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * 1.20's mob heads on note blocks, for 1.12.2's own note block.
 *
 * <p>A head on top of a note block makes it play that mob's sound instead of a note (1.20's
 * {@code NoteBlockInstrument} mob-head instruments: pitch 1, volume 3, record category, no note particle):
 * skeleton, wither skeleton, zombie, creeper, ender dragon, piglin. A player head is an instrument too, but
 * with no sound of its own (1.20 reads one from the head's {@code note_block_sound}, which 1.12.2's heads
 * cannot carry), so it plays nothing. 1.12.2's note block plays nothing at all with a block above it, so
 * every case here is added, none replaced:
 * <ul>
 *   <li><b>Using it</b> (right click): vanilla still tunes it; the head's sound plays.</li>
 *   <li><b>Hitting it</b> (survival left click): the head's sound plays.</li>
 *   <li><b>Powering it</b>: note blocks with a head on top are tracked per world (found when a chunk loads
 *       and when a head or note block is placed) and the head's sound plays on each rising edge of power,
 *       read at the end of the world tick - 1.20's {@code POWERED} property, kept here instead of in the
 *       block state.</li>
 *   <li><b>Placing a head</b>: a head item used on a note block's top face goes on it without sneaking
 *       (1.20's {@code note_block_top_instruments} tag: the note block skips its own use).</li>
 * </ul>
 */
public final class NoteBlockHeads {

    /** Per world: tracked note block positions and whether each was powered at the last look. */
    private static final Map<World, Map<BlockPos, Boolean>> TRACKED = new WeakHashMap<World, Map<BlockPos, Boolean>>();
    /** Per world: positions to start tracking at the next tick, once neighbors can be read safely. */
    private static final Map<World, Set<BlockPos>> PENDING = new WeakHashMap<World, Set<BlockPos>>();

    private NoteBlockHeads() {
    }

    // --- what plays ------------------------------------------------------------------------------

    /** @return true if the block above a note block makes it a mob head instrument (player heads included) */
    public static boolean isHead(IBlockState state) {
        Block block = state.getBlock();
        return block == Blocks.SKULL || (HeadsContent.head != null && block == HeadsContent.head);
    }

    /**
     * @return the sound a note block at {@code pos} plays with this head on top, or null when the head has
     * none (a player head) or there is no head
     */
    @Nullable
    public static SoundEvent soundFor(World world, BlockPos notePos) {
        BlockPos up = notePos.up();
        IBlockState state = world.getBlockState(up);
        if (HeadsContent.head != null && state.getBlock() == HeadsContent.head) {
            return HeadsContent.IMITATE_PIGLIN;
        }
        if (state.getBlock() != Blocks.SKULL) {
            return null;
        }
        TileEntity tile = world.getTileEntity(up);
        if (!(tile instanceof TileEntitySkull)) {
            return null;
        }
        switch (((TileEntitySkull) tile).getSkullType()) {
            case 0:
                return HeadsContent.IMITATE_SKELETON;
            case 1:
                return HeadsContent.IMITATE_WITHER_SKELETON;
            case 2:
                return HeadsContent.IMITATE_ZOMBIE;
            case 4:
                return HeadsContent.IMITATE_CREEPER;
            case 5:
                return HeadsContent.IMITATE_ENDER_DRAGON;
            default:
                return null; // the player head (3): 1.20's custom-head instrument with no sound set
        }
    }

    /** @return true if a note block at this position has a head (any head) on top */
    public static boolean hasHead(World world, BlockPos notePos) {
        return world.getBlockState(notePos).getBlock() == Blocks.NOTEBLOCK && isHead(world.getBlockState(notePos.up()));
    }

    /** 1.20's {@code NoteBlock.triggerEvent} for a mob head: the sound at the block's center, pitch 1. */
    public static boolean play(World world, BlockPos notePos) {
        SoundEvent sound = soundFor(world, notePos);
        if (sound == null) {
            return false;
        }
        world.playSound(null, notePos.getX() + 0.5D, notePos.getY() + 0.5D, notePos.getZ() + 0.5D, sound,
                SoundCategory.RECORDS, 3.0F, 1.0F);
        return true;
    }

    /** @return true for the items 1.20's {@code note_block_top_instruments} tag holds: every head */
    public static boolean isHeadItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        return item == Items.SKULL || (HeadsContent.headItem != null && item == HeadsContent.headItem);
    }

    // --- players ---------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        if (world.getBlockState(pos).getBlock() != Blocks.NOTEBLOCK) {
            return;
        }
        if (event.getFace() == EnumFacing.UP && isHeadItem(event.getItemStack())) {
            // The head goes on top; the note block is not used (1.20's SKIP_DEFAULT_BLOCK_INTERACTION).
            event.setUseBlock(Event.Result.DENY);
            return;
        }
        if (world.isRemote || event.getHand() != EnumHand.MAIN_HAND || event.getUseBlock() == Event.Result.DENY
                || !isHead(world.getBlockState(pos.up())) || !activates(event.getEntityPlayer())) {
            return;
        }
        // Vanilla still tunes it (as 1.20's use does), then its own note stays silent under a block.
        play(world, pos);
    }

    /** 1.12.2's rule for whether a right click reaches the block: not sneaking, or sneaking empty-handed. */
    static boolean activates(EntityPlayer player) {
        if (!player.isSneaking()) {
            return true;
        }
        ItemStack main = player.getHeldItemMainhand();
        ItemStack off = player.getHeldItemOffhand();
        return main.doesSneakBypassUse(player.world, BlockPos.ORIGIN, player)
                && off.doesSneakBypassUse(player.world, BlockPos.ORIGIN, player);
    }

    @SubscribeEvent
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        World world = event.getWorld();
        if (world.isRemote || event.getUseBlock() == Event.Result.DENY || event.getEntityPlayer().capabilities.isCreativeMode) {
            return;
        }
        BlockPos pos = event.getPos();
        if (hasHead(world, pos)) {
            play(world, pos);
        }
    }

    // --- redstone --------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onPlace(BlockEvent.PlaceEvent event) {
        World world = event.getWorld();
        if (world.isRemote) {
            return;
        }
        BlockPos pos = event.getPos();
        Block placed = event.getPlacedBlock().getBlock();
        if (isHead(event.getPlacedBlock())) {
            pending(world).add(pos.down());
        } else if (placed == Blocks.NOTEBLOCK) {
            pending(world).add(pos);
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        World world = event.getWorld();
        if (world.isRemote) {
            return;
        }
        Chunk chunk = event.getChunk();
        for (Map.Entry<BlockPos, TileEntity> e : chunk.getTileEntityMap().entrySet()) {
            BlockPos pos = e.getKey();
            if (e.getValue() instanceof TileEntityNote && pos.getY() < 255 && isHead(chunk.getBlockState(pos.up()))) {
                pending(world).add(pos);
            }
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        World world = event.getWorld();
        Map<BlockPos, Boolean> tracked = TRACKED.get(world);
        Set<BlockPos> pending = PENDING.get(world);
        ChunkPos chunk = event.getChunk().getPos();
        if (tracked != null) {
            tracked.keySet().removeIf(p -> (p.getX() >> 4) == chunk.x && (p.getZ() >> 4) == chunk.z);
        }
        if (pending != null) {
            pending.removeIf(p -> (p.getX() >> 4) == chunk.x && (p.getZ() >> 4) == chunk.z);
        }
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        TRACKED.remove(event.getWorld());
        PENDING.remove(event.getWorld());
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote) {
            return;
        }
        tick(event.world);
    }

    /** Starts tracking what is pending, then plays every tracked note block whose power just rose. */
    public static void tick(World world) {
        Set<BlockPos> pending = PENDING.get(world);
        Map<BlockPos, Boolean> tracked = TRACKED.get(world);
        if (pending != null && !pending.isEmpty()) {
            if (tracked == null) {
                tracked = new HashMap<BlockPos, Boolean>();
                TRACKED.put(world, tracked);
            }
            for (BlockPos pos : pending) {
                if (!tracked.containsKey(pos) && world.isBlockLoaded(pos) && hasHead(world, pos)) {
                    // Whatever power it has now is not an edge: a head placed on a powered block is silent.
                    tracked.put(pos, world.isBlockPowered(pos));
                }
            }
            pending.clear();
        }
        if (tracked == null || tracked.isEmpty()) {
            return;
        }
        for (Iterator<Map.Entry<BlockPos, Boolean>> it = tracked.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<BlockPos, Boolean> e = it.next();
            BlockPos pos = e.getKey();
            if (!world.isBlockLoaded(pos) || !hasHead(world, pos)) {
                it.remove();
                continue;
            }
            boolean powered = world.isBlockPowered(pos);
            if (powered && !e.getValue()) {
                play(world, pos);
            }
            e.setValue(powered);
        }
    }

    private static Set<BlockPos> pending(World world) {
        Set<BlockPos> set = PENDING.get(world);
        if (set == null) {
            set = new HashSet<BlockPos>();
            PENDING.put(world, set);
        }
        return set;
    }

    /** @return whether this note block is tracked, and its last power reading (null when not tracked) */
    @Nullable
    public static Boolean trackedPower(World world, BlockPos pos) {
        Map<BlockPos, Boolean> tracked = TRACKED.get(world);
        return tracked == null ? null : tracked.get(pos);
    }
}
