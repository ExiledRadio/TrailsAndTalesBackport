package com.exiledradio.trailsandtalesbackport.structure;

import com.exiledradio.backportcore.structure.JigsawElement;
import com.exiledradio.backportcore.structure.JigsawPiece;
import com.exiledradio.backportcore.structure.JigsawPools;
import com.exiledradio.backportcore.structure.TemplatePlacement;
import com.exiledradio.backportcore.structure.TerrainAdaptation;
import com.google.gson.JsonElement;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Mirror;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.template.TemplateManager;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * One trail ruins piece: 1.20.1's {@code PoolElementStructurePiece} with a {@code SinglePoolElement}. Backport Core
 * places the file and its {@code rule} processors (gravel to dirt and coarse dirt, mud bricks to packed mud); this adds
 * what Core leaves out, the {@code capped} archaeology processors that turn some of the gravel into suspicious gravel
 * with the common or rare table ({@link Archaeology}). As 1.20.1's piece it tells the terrain how it meets the ground
 * ({@link TerrainAdaptation.AdaptingPiece}: rigid, its ground level delta, its jigsaw junctions) for {@code bury}.
 */
public class TrailRuinsPiece extends JigsawPiece implements TerrainAdaptation.AdaptingPiece {

    public static final String SAVE_NAME = "TTB_TrailRuinsPiece";

    /** {@code JigsawJunction}s: source x, source ground y, source z, three ints each, in the order added. */
    private int[] junctions = new int[0];
    /** The suspicious blocks this piece's file gets, worked out once (the same for every chunk). */
    @Nullable
    private transient List<Archaeology.Info> suspicious;

    /** For loading. */
    public TrailRuinsPiece() {
    }

    public TrailRuinsPiece(JigsawElement element, BlockPos position, int groundLevelDelta, Rotation rotation, StructureBoundingBox box) {
        super(element, position, groundLevelDelta, rotation, box);
    }

    void setPivot(BlockPos pivot) {
        this.pivot = pivot;
    }

    /** {@code PoolElementStructurePiece.addJunction}. */
    void addJunction(int x, int groundY, int z) {
        int n = this.junctions.length;
        this.junctions = Arrays.copyOf(this.junctions, n + 3);
        this.junctions[n] = x;
        this.junctions[n + 1] = groundY;
        this.junctions[n + 2] = z;
    }

    @Override
    public boolean rigid() {
        return this.element == null || this.element.rigid();
    }

    @Override
    public int[] junctions() {
        return this.junctions.clone();
    }

    @Override
    public void offset(int x, int y, int z) {
        super.offset(x, y, z);
        this.pivot = this.pivot.add(x, y, z);
        for (int n = 0; n < this.junctions.length; n += 3) {
            this.junctions[n] += x;
            this.junctions[n + 1] += y;
            this.junctions[n + 2] += z;
        }
        this.suspicious = null;
    }

    @Override
    protected void writeStructureToNBT(NBTTagCompound tag) {
        super.writeStructureToNBT(tag);
        tag.setIntArray("junctions", this.junctions);
    }

    @Override
    protected void readStructureFromNBT(NBTTagCompound tag, TemplateManager manager) {
        super.readStructureFromNBT(tag, manager);
        int[] j = tag.getIntArray("junctions");
        this.junctions = j.length % 3 == 0 ? j : new int[0];
    }

    /** @return the template's name as the pool gives it, or null */
    @Nullable
    public ResourceLocation template() {
        return this.element == null ? null : this.element.template();
    }

    /** @return the element's processor list id, or null for none (or one written out in the pool) */
    @Nullable
    public ResourceLocation processorList() {
        if (this.element == null) {
            return null;
        }
        JsonElement p = this.element.json().get("processors");
        return p != null && p.isJsonPrimitive() ? new ResourceLocation(p.getAsString()) : null;
    }

    @Override
    public boolean addComponentParts(World world, Random random, StructureBoundingBox chunk) {
        super.addComponentParts(world, random, chunk);
        if (this.element != null) {
            Archaeology.place(world, suspicious(world.getSeed()), chunk);
        }
        Archaeology.unscheduleFalling(world, Archaeology.clip(getBoundingBox(), chunk));
        return true;
    }

    /**
     * The suspicious blocks 1.20.1 gives this piece in a world of this seed: its file through the element's processors
     * ({@code SinglePoolElement.getSettings}: structure blocks left out, jigsaws to their final state, then the list).
     */
    public synchronized List<Archaeology.Info> suspicious(long worldSeed) {
        List<Archaeology.Info> known = this.suspicious;
        if (known != null && this.seedFor == worldSeed) {
            return known;
        }
        ResourceLocation list = processorList();
        List<Archaeology.Info> out = list != null && Archaeology.hasListStep(list)
                ? Archaeology.suspicious(processed(worldSeed)) : Collections.<Archaeology.Info>emptyList();
        this.suspicious = out;
        this.seedFor = worldSeed;
        return out;
    }

    private transient long seedFor;

    /** Every block 1.20.1 places for this piece, processed, in its file's order (for tests and tools). */
    public List<Archaeology.Info> processed(long worldSeed) {
        ResourceLocation location = template();
        if (location == null) {
            return Collections.emptyList();
        }
        ResourceLocation list = processorList();
        VanillaTemplate file = VanillaTemplate.load(JigsawPools.templateId(location));
        return Archaeology.process(file, this.position, TemplatePlacement.of(this.pieceRotation, Mirror.NONE), worldSeed,
                Arrays.asList(Archaeology.STRUCTURE_BLOCK, Archaeology.JIGSAW_REPLACEMENT),
                list == null ? Collections.<Archaeology.Step>emptyList() : Archaeology.list(list));
    }
}
