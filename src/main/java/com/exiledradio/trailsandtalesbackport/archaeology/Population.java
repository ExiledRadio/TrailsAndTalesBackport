package com.exiledradio.trailsandtalesbackport.archaeology;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.fml.common.IWorldGenerator;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Which chunks of a world are being populated right now, so a suspicious block placed or disturbed then gets no
 * tick ({@link BlockSuspicious}).
 *
 * <p>1.20 places the blocks of structures and features into a {@code WorldGenRegion}: no {@code onPlace}, no neighbor
 * or shape updates, so a suspicious block over air or water stays until the world itself runs and a neighbor changes.
 * 1.12.2 populates straight into the world: {@code onBlockAdded} runs for every block set, and a later structure or
 * feature setting a block next to it with a notifying flag calls {@code neighborChanged}. Both would schedule the fall.
 *
 * <p>A chunk's population is 1.12.2's {@code Chunk.populate}: the generator's {@code populate} (from Forge's
 * {@link PopulateChunkEvent.Pre}, which vanilla's and the family's generators fire first) and then every
 * {@link IWorldGenerator} (this mod's first and last ones, by weight). Only the server thread populates; the set is
 * emptied at the start of every server tick, so a generator that throws cannot leave a chunk marked.
 */
public final class Population {

    private static final Map<World, Set<Long>> POPULATING = new IdentityHashMap<World, Set<Long>>();
    private static long marked;

    private Population() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new Population());
        GameRegistry.registerWorldGenerator(new Edge(true), Integer.MIN_VALUE);
        GameRegistry.registerWorldGenerator(new Edge(false), Integer.MAX_VALUE);
    }

    /** @return true while any chunk of this world is being populated */
    public static synchronized boolean active(World world) {
        if (world.isRemote) {
            return false;
        }
        Set<Long> chunks = POPULATING.get(world);
        return chunks != null && !chunks.isEmpty();
    }

    public static synchronized void begin(World world, int chunkX, int chunkZ) {
        Set<Long> chunks = POPULATING.get(world);
        if (chunks == null) {
            POPULATING.put(world, chunks = new HashSet<Long>());
        }
        if (chunks.add(ChunkPos.asLong(chunkX, chunkZ))) {
            marked++;
        }
    }

    /** @return how many times a chunk was marked as populating since the game started (for the self-test) */
    public static synchronized long marked() {
        return marked;
    }

    public static synchronized void end(World world, int chunkX, int chunkZ) {
        Set<Long> chunks = POPULATING.get(world);
        if (chunks != null) {
            chunks.remove(ChunkPos.asLong(chunkX, chunkZ));
            if (chunks.isEmpty()) {
                POPULATING.remove(world);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void pre(PopulateChunkEvent.Pre event) {
        if (!event.getWorld().isRemote) {
            begin(event.getWorld(), event.getChunkX(), event.getChunkZ());
        }
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            synchronized (Population.class) {
                POPULATING.clear();
            }
        }
    }

    /** The first (begin) and last (end) of the world generators {@code GameRegistry.generateWorld} runs. */
    static final class Edge implements IWorldGenerator {
        private final boolean first;

        Edge(boolean first) {
            this.first = first;
        }

        @Override
        public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
            if (world.isRemote) {
                return;
            }
            if (this.first) {
                begin(world, chunkX, chunkZ);
            } else {
                end(world, chunkX, chunkZ);
            }
        }
    }
}
