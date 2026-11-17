package com.exiledradio.trailsandtalesbackport.structure;

import com.exiledradio.backportcore.structure.ScatteredStructure;
import com.exiledradio.backportcore.structure.Structures;
import com.exiledradio.backportcore.structure.TemplatePiece;
import com.exiledradio.backportcore.structure.TemplatePlacement;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Mirror;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

/**
 * 1.20's suspicious sand and gravel in Update Aquatic Backport's ocean ruins ({@code OceanRuinPieces}: each ruin file
 * through {@code CappedProcessor(RuleProcessor(sand or gravel to suspicious sand or gravel with the warm or cold
 * table), 5)}, after its integrity and the structure-and-air ignore). Update Aquatic owns the ruins and builds them
 * with Backport Core's templates; nothing here names its classes: its ruins are Core's ocean ruin kind
 * ({@code ScatteredStructure.of(world, "ocean_ruin")}), its pieces Core's {@code TemplatePiece}s, and a piece's
 * warm or cold is the {@code BiomeType} it saves. Without Update Aquatic there are no ocean ruins
 * ({@code docs/PLAN.md}: the ocean-ruin loot then comes from fallback loot, Decided 3).
 *
 * <p>Which blocks the capped processor picks depends on what survives the ruin's integrity, which Backport Core rolls
 * with its own per-position random (Update Aquatic's ruins are 1.13's, not 1.20's, so they are not vanilla's block
 * for block anyway); the same roll is made here, so the five come from blocks that are really there.
 */
public final class OceanRuins {

    private OceanRuins() {
    }

    static final ResourceLocation WARM = new ResourceLocation("minecraft", "archaeology/ocean_ruin_warm");
    static final ResourceLocation COLD = new ResourceLocation("minecraft", "archaeology/ocean_ruin_cold");

    /** Worked-out suspicious blocks per piece (a piece is the same object for as long as its start is kept). */
    private static final Map<StructureComponent, List<Archaeology.Info>> KNOWN = new WeakHashMap<StructureComponent, List<Archaeology.Info>>();

    /** Adds the archaeology to every ocean ruin piece inside the populate step's box. */
    static void populate(World world, StructureBoundingBox box) {
        ScatteredStructure ruins = ScatteredStructure.of(world, Structures.OCEAN_RUIN);
        if (ruins == null) {
            return;
        }
        for (StructureStart start : DesertPyramids.starts(ruins)) {
            if (!start.isSizeableStructure() || !start.getBoundingBox().intersectsWith(box)) {
                continue;
            }
            for (StructureComponent c : start.getComponents()) {
                if (c instanceof TemplatePiece && c.getBoundingBox().intersectsWith(box)) {
                    for (Archaeology.Info i : suspicious(world.getSeed(), c)) {
                        if (!box.isVecInside(i.pos)) {
                            continue;
                        }
                        boolean gravel = i.name.equals("minecraft:suspicious_gravel");
                        Block there = world.getBlockState(i.pos).getBlock();
                        // Only where the ruin really put the sand or gravel (its own integrity, a block it left out).
                        if (there == (gravel ? Blocks.GRAVEL : Blocks.SAND)) {
                            Archaeology.set(world, i.pos, gravel, Archaeology.table(i.nbt.getString("LootTable")), i.nbt.getLong("LootTableSeed"));
                        }
                    }
                    // The ruin's plain sand and gravel too: in 1.20 they get no tick either, and one falling drags its
                    // suspicious neighbor along.
                    Archaeology.unscheduleFalling(world, Archaeology.clip(c.getBoundingBox(), box));
                }
            }
        }
    }

    public static List<Archaeology.Info> suspicious(long worldSeed, StructureComponent piece) {
        synchronized (KNOWN) {
            List<Archaeology.Info> known = KNOWN.get(piece);
            if (known != null) {
                return known;
            }
        }
        List<Archaeology.Info> out = work(worldSeed, piece.createStructureBaseNBT());
        synchronized (KNOWN) {
            KNOWN.put(piece, out);
        }
        return out;
    }

    /** One ruin piece as it saves itself ({@code TemplatePiece}'s keys and Update Aquatic's {@code BiomeType}). */
    public static List<Archaeology.Info> work(long worldSeed, NBTTagCompound tag) {
        if (!tag.hasKey("Template") || !tag.hasKey("BiomeType")) {
            return Collections.emptyList();
        }
        ResourceLocation template = new ResourceLocation(tag.getString("Template"));
        if (!template.getPath().startsWith("underwater_ruin/")) {
            return Collections.emptyList();
        }
        boolean warm = "WARM".equals(tag.getString("BiomeType"));
        BlockPos origin = new BlockPos(tag.getInteger("TPX"), tag.getInteger("TPY"), tag.getInteger("TPZ"));
        Rotation rotation = enumOr(Rotation.class, tag.getString("Rot"), Rotation.NONE);
        Mirror mirror = enumOr(Mirror.class, tag.getString("Mi"), Mirror.NONE);
        BlockPos pivot = new BlockPos(tag.getInteger("PvX"), tag.getInteger("PvY"), tag.getInteger("PvZ"));
        float integrity = tag.hasKey("Integrity") ? tag.getFloat("Integrity") : 1.0F;
        long seed = tag.getLong("Seed");
        TemplatePlacement placement = TemplatePlacement.of(rotation, mirror).pivot(pivot);
        // Backport Core's integrity roll (ModernTemplate.place): a random of the block's position and the piece's seed.
        Archaeology.Step integrityRoll = (original, current) -> integrity < 1.0F
                && new Random(VanillaRandom.seed(current.pos) ^ seed).nextFloat() > integrity ? null : current;
        List<Archaeology.Step> before = Arrays.asList(integrityRoll,
                Archaeology.ignore("minecraft:structure_block", "minecraft:air", "minecraft:jigsaw", "minecraft:structure_void"));
        Archaeology.Step capped = new Archaeology.CappedStep(warm
                ? Archaeology.blockToLoot("minecraft:sand", "minecraft:suspicious_sand", WARM.toString())
                : Archaeology.blockToLoot("minecraft:gravel", "minecraft:suspicious_gravel", COLD.toString()), 5, 5);
        List<Archaeology.Info> processed = Archaeology.process(VanillaTemplate.load(template), origin, placement, worldSeed, before,
                Collections.singletonList(capped));
        return new ArrayList<Archaeology.Info>(Archaeology.suspicious(processed));
    }

    private static <E extends Enum<E>> E enumOr(Class<E> type, String name, E fallback) {
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
