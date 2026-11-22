package com.exiledradio.trailsandtalesbackport.structure;

import com.exiledradio.backportcore.structure.JigsawElement;
import com.exiledradio.backportcore.structure.JigsawPools;
import com.exiledradio.backportcore.structure.ModernJigsaw;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;
import java.util.function.IntBinaryOperator;

/**
 * 1.20.1's jigsaw assembly ({@code JigsawPlacement.addPieces} and its {@code Placer}), draw for draw, for trail ruins:
 * the start piece's rotation and element, the start projected to {@code WORLD_SURFACE_WG} at its middle plus the start
 * height, then breadth first to the depth, each jigsaw (shuffled) trying its pool's elements and then the fallback's
 * (shuffled), each in shuffled rotations, inside a box {@code max_distance_from_center} out from the start, or the
 * parent's own box for a joint inside it. Records the jigsaw junctions as 1.20.1 does, for the terrain's beard.
 *
 * <p>The stub is ours; the placer is Backport Core's {@link ModernJigsaw} (1.20+'s, {@code Util.shuffle}'s n - 1
 * draws, the junctions kept), on Core's pools, elements and templates.
 */
public final class TrailLayout {

    private TrailLayout() {
    }

    /** {@code Structure.GenerationStub}: where the biome is checked, and the start piece. */
    public static final class Stub {
        /** The start piece's middle at the start's height. */
        public final BlockPos position;
        final TrailRuinsPiece start;

        Stub(BlockPos position, TrailRuinsPiece start) {
            this.position = position;
            this.start = start;
        }
    }

    /**
     * {@code JigsawPlacement.addPieces} up to the stub. Draws from {@code random} as 1.20.1 does.
     *
     * @param random  {@code StructureRandoms.start(seed, chunkX, chunkZ)}
     * @param surface {@code getFirstFreeHeight(x, z, WORLD_SURFACE_WG)}
     * @return null where 1.20.1 has no start (an empty start element, a missing pool)
     */
    @Nullable
    public static Stub stub(int chunkX, int chunkZ, Random random, IntBinaryOperator surface) {
        TrailRuins s = TrailRuins.get();
        // start_height: absolute, no draw.
        BlockPos origin = new BlockPos(chunkX << 4, s.startHeight, chunkZ << 4);
        Rotation rotation = Rotation.values()[random.nextInt(4)];
        JigsawPools.Pool pool = JigsawPools.pool(s.startPool);
        if (pool == null || pool.size() == 0) {
            TrailsAndTalesBackport.LOGGER.warn("The trail ruins start pool {} is missing.", s.startPool);
            return null;
        }
        JigsawElement element = pool.random(random);
        if (element.isEmpty()) {
            return null;
        }
        TrailRuinsPiece first = new TrailRuinsPiece(element, origin, element.groundLevelDelta(), rotation, element.box(origin, rotation));
        StructureBoundingBox box = first.getBoundingBox();
        int midX = (box.maxX + box.minX) / 2;
        int midZ = (box.maxZ + box.minZ) / 2;
        int y = s.projectToSurface ? origin.getY() + surface.applyAsInt(midX, midZ) : origin.getY();
        int floor = box.minY + first.groundLevelDelta();
        first.offset(0, y - floor, 0);
        return new Stub(new BlockPos(midX, y, midZ), first);
    }

    /**
     * The children of a stub made from {@code random} (continue with the same random): Backport Core's
     * {@link ModernJigsaw} with 1.20.1's rules (no priorities, no aliases, no expansion hack: trail ruins have
     * {@code use_expansion_hack: false}), the ground asked for terrain-matching pieces.
     */
    public static List<TrailRuinsPiece> assemble(Stub stub, Random random, IntBinaryOperator surface) {
        TrailRuins s = TrailRuins.get();
        BlockPos c = stub.position;
        int d = s.maxDistance;
        // AABB(x - d .. x + d + 1): blocks x - d to x + d.
        return ModernJigsaw.of(TrailRuinsPiece::new).maxDepth(s.maxDepth).surface(surface)
                .assemble(stub.start, new StructureBoundingBox(c.getX() - d, c.getY() - d, c.getZ() - d, c.getX() + d, c.getY() + d, c.getZ() + d), random);
    }
}
