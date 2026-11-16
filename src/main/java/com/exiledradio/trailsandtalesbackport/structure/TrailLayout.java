package com.exiledradio.trailsandtalesbackport.structure;

import com.exiledradio.backportcore.structure.JigsawElement;
import com.exiledradio.backportcore.structure.JigsawPools;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
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
 * <p>Backport Core's {@code Jigsaw} is 1.16.5's: its pool shuffle is fastutil's, one draw more than 1.20's
 * {@code Util.shuffledCopy}, and it keeps no junctions; so this is our own, on Core's pools, elements and templates.
 * Every shuffle here is {@link VanillaRandom#shuffle} (n - 1 draws).
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

    /** The children of a stub made from {@code random} (continue with the same random). */
    public static List<TrailRuinsPiece> assemble(Stub stub, Random random, IntBinaryOperator surface) {
        TrailRuins s = TrailRuins.get();
        List<TrailRuinsPiece> pieces = new ArrayList<TrailRuinsPiece>();
        TrailRuinsPiece first = stub.start;
        pieces.add(first);
        if (s.maxDepth > 0) {
            BlockPos c = stub.position;
            int d = s.maxDistance;
            // AABB(x - d .. x + d + 1): blocks x - d to x + d.
            Space cube = new Space(new StructureBoundingBox(c.getX() - d, c.getY() - d, c.getZ() - d, c.getX() + d, c.getY() + d, c.getZ() + d));
            cube.taken.add(new StructureBoundingBox(first.getBoundingBox()));
            Placer placer = new Placer(s.maxDepth, pieces, random, surface);
            placer.placing.addLast(new State(first, cube, 0));
            while (!placer.placing.isEmpty()) {
                State next = placer.placing.removeFirst();
                placer.tryPlacingChildren(next.piece, next.free, next.depth);
            }
        }
        // StructureStart.placeInChunk: every piece's pivot is the first piece's middle at its floor.
        StructureBoundingBox b = first.getBoundingBox();
        BlockPos pivot = new BlockPos(b.minX + (b.maxX - b.minX + 1) / 2, b.minY, b.minZ + (b.maxZ - b.minZ + 1) / 2);
        for (TrailRuinsPiece piece : pieces) {
            piece.setPivot(pivot);
        }
        return pieces;
    }

    // ---- free space: VoxelShapes of whole-block boxes

    static final class Space {
        @Nullable
        final StructureBoundingBox outside;
        final List<StructureBoundingBox> taken = new ArrayList<StructureBoundingBox>();

        Space(@Nullable StructureBoundingBox outside) {
            this.outside = outside;
        }

        /** "The box, shrunk by a quarter block, is all inside the free shape." */
        boolean fits(StructureBoundingBox box) {
            if (this.outside != null && !(this.outside.minX <= box.minX && box.maxX <= this.outside.maxX
                    && this.outside.minY <= box.minY && box.maxY <= this.outside.maxY
                    && this.outside.minZ <= box.minZ && box.maxZ <= this.outside.maxZ)) {
                return false;
            }
            for (StructureBoundingBox t : this.taken) {
                if (t.intersectsWith(box)) {
                    return false;
                }
            }
            return true;
        }
    }

    static final class State {
        final TrailRuinsPiece piece;
        final Space free;
        final int depth;

        State(TrailRuinsPiece piece, Space free, int depth) {
            this.piece = piece;
            this.free = free;
            this.depth = depth;
        }
    }

    static <T> List<T> shuffled(List<T> in, Random random) {
        List<T> out = new ArrayList<T>(in);
        VanillaRandom.shuffle(out, random);
        return out;
    }

    /** {@code JigsawPlacement.Placer} (no expansion hack: trail ruins have {@code use_expansion_hack: false}). */
    static final class Placer {
        final int maxDepth;
        final List<TrailRuinsPiece> pieces;
        final Random random;
        final IntBinaryOperator surface;
        final Deque<State> placing = new ArrayDeque<State>();

        Placer(int maxDepth, List<TrailRuinsPiece> pieces, Random random, IntBinaryOperator surface) {
            this.maxDepth = maxDepth;
            this.pieces = pieces;
            this.random = random;
            this.surface = surface;
        }

        void tryPlacingChildren(TrailRuinsPiece parent, Space outerFree, int depth) {
            JigsawElement element = parent.element();
            BlockPos position = parent.position();
            Rotation rotation = parent.rotation();
            boolean parentRigid = element.rigid();
            Space inner = null;
            StructureBoundingBox parentBox = parent.getBoundingBox();
            int parentMinY = parentBox.minY;

            connectorLoop:
            for (JigsawElement.Connector connector : shuffled(element.connectors(position, rotation), this.random)) {
                EnumFacing front = connector.front;
                BlockPos at = connector.pos;
                BlockPos target = at.offset(front);
                int connectorY = at.getY() - parentMinY;
                int surfaceY = -1;
                ResourceLocation poolId = new ResourceLocation(connector.pool);
                JigsawPools.Pool pool = JigsawPools.pool(poolId);
                if (pool == null || (pool.size() == 0 && !poolId.equals(JigsawPools.EMPTY))) {
                    TrailsAndTalesBackport.LOGGER.warn("Empty or non-existent pool: {}", poolId);
                    continue;
                }
                ResourceLocation fallbackId = pool.fallback();
                JigsawPools.Pool fallback = JigsawPools.pool(fallbackId);
                if (fallback == null || (fallback.size() == 0 && !fallbackId.equals(JigsawPools.EMPTY))) {
                    TrailsAndTalesBackport.LOGGER.warn("Empty or non-existent fallback pool: {}", fallbackId);
                    continue;
                }
                Space free;
                if (parentBox.isVecInside(target)) {
                    if (inner == null) {
                        inner = new Space(new StructureBoundingBox(parentBox));
                    }
                    free = inner;
                } else {
                    free = outerFree;
                }
                List<JigsawElement> candidates = new ArrayList<JigsawElement>();
                if (depth != this.maxDepth) {
                    candidates.addAll(shuffled(pool.elements(), this.random));
                }
                candidates.addAll(shuffled(fallback.elements(), this.random));

                for (JigsawElement candidate : candidates) {
                    if (candidate.isEmpty()) {
                        break;
                    }
                    for (Rotation r : shuffled(Arrays.asList(Rotation.values()), this.random)) {
                        List<JigsawElement.Connector> theirs = shuffled(candidate.connectors(BlockPos.ORIGIN, r), this.random);
                        // The bounding box at the origin is read here in 1.20.1; it draws nothing.
                        for (JigsawElement.Connector their : theirs) {
                            if (!connector.canAttach(their)) {
                                continue;
                            }
                            BlockPos theirPos = their.pos;
                            BlockPos offset = target.subtract(theirPos);
                            StructureBoundingBox childBox = candidate.box(offset, r);
                            int childMinY = childBox.minY;
                            boolean childRigid = candidate.rigid();
                            int theirY = theirPos.getY();
                            int delta = connectorY - theirY + front.getYOffset();
                            int targetMinY;
                            if (parentRigid && childRigid) {
                                targetMinY = parentMinY + delta;
                            } else {
                                if (surfaceY == -1) {
                                    surfaceY = this.surface.applyAsInt(at.getX(), at.getZ());
                                }
                                targetMinY = surfaceY - theirY;
                            }
                            int dy = targetMinY - childMinY;
                            StructureBoundingBox moved = new StructureBoundingBox(childBox);
                            moved.offset(0, dy, 0);
                            BlockPos childPos = offset.add(0, dy, 0);
                            if (free.fits(moved)) {
                                free.taken.add(new StructureBoundingBox(moved));
                                int parentGround = parent.groundLevelDelta();
                                int childGround = childRigid ? parentGround - delta : candidate.groundLevelDelta();
                                TrailRuinsPiece child = new TrailRuinsPiece(candidate, childPos, childGround, r, moved);
                                int sourceGroundY;
                                if (parentRigid) {
                                    sourceGroundY = parentMinY + connectorY;
                                } else if (childRigid) {
                                    sourceGroundY = targetMinY + theirY;
                                } else {
                                    if (surfaceY == -1) {
                                        surfaceY = this.surface.applyAsInt(at.getX(), at.getZ());
                                    }
                                    sourceGroundY = surfaceY + delta / 2;
                                }
                                parent.addJunction(target.getX(), sourceGroundY - connectorY + parentGround, target.getZ());
                                child.addJunction(at.getX(), sourceGroundY - theirY + childGround, at.getZ());
                                this.pieces.add(child);
                                if (depth + 1 <= this.maxDepth) {
                                    this.placing.addLast(new State(child, free, depth + 1));
                                }
                                continue connectorLoop;
                            }
                        }
                    }
                }
            }
        }
    }
}
