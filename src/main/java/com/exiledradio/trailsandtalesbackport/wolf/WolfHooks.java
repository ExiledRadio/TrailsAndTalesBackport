package com.exiledradio.trailsandtalesbackport.wolf;

import com.exiledradio.trailsandtalesbackport.armadillo.ArmadilloContent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.ai.EntityAIPanic;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

/**
 * Hooks for patches P5 ({@code EntityWolf}) and P6 ({@code WorldEntitySpawner}), and 1.20.6's wolf panic goal.
 *
 * <ul>
 *   <li><b>Pack variant</b> (1.20.5's {@code Wolf.finalizeSpawn} and {@code WolfPackData}): the first wolf of a spawned
 *       pack takes its biome's variant and hands it on in the spawn data 1.12.2 passes from one wolf of a pack to the
 *       next ({@code onInitialSpawn}'s {@code IEntityLivingData}); the rest take the same.</li>
 *   <li><b>The {@code variant} tag</b>: 1.20.5 saves a wolf's variant as {@code variant} at the entity's top level, so
 *       {@code /summon minecraft:wolf ~ ~ ~ {variant:"minecraft:ashen"}} and a spawn egg's {@code EntityTag} set it.
 *       Read and written beside this mod's capability.</li>
 *   <li><b>Creature spawn chance</b> at chunk generation ({@code MobSpawnSettings.creatureGenerationProbability}):
 *       1.20.5's badlands 0.03 and wooded badlands 0.04 (1.12.2: 0.1 for every biome).</li>
 * </ul>
 */
public final class WolfHooks {

    /** 1.20.5's {@code Wolf.WolfPackData}. */
    public static final class PackData implements IEntityLivingData {
        public final WolfVariant variant;

        public PackData(WolfVariant variant) {
            this.variant = variant;
        }
    }

    private WolfHooks() {
    }

    /** P5: {@code EntityWolf.onInitialSpawn}, after the superclass's; @return the spawn data for the pack's next wolf */
    @Nullable
    public static IEntityLivingData initialSpawn(EntityWolf wolf, @Nullable IEntityLivingData data) {
        WolfData d = WolfContent.enabled() ? WolfData.of(wolf) : null;
        if (d == null || wolf.world.isRemote) {
            return data;
        }
        if (data instanceof PackData) {
            d.variant = ((PackData) data).variant;
            return data;
        }
        if (d.variant == null) {
            d.variant = WolfVariant.forBiome(wolf.world.getBiome(new BlockPos(wolf)));
        }
        // Spawn data some other mod made is left as it is (vanilla wolves pass none).
        return data == null ? new PackData(d.variant) : data;
    }

    /** P5: the first line of {@code EntityWolf.readEntityFromNBT}. */
    public static void readNbt(EntityWolf wolf, NBTTagCompound tag) {
        if (!WolfContent.enabled() || !tag.hasKey("variant", 8)) {
            return;
        }
        WolfData d = WolfData.of(wolf);
        WolfVariant v = byTag(tag.getString("variant"));
        if (d != null && v != null) {
            d.variant = v;
        }
    }

    /** P5: the first line of {@code EntityWolf.writeEntityToNBT}. */
    public static void writeNbt(EntityWolf wolf, NBTTagCompound tag) {
        WolfData d = WolfContent.enabled() ? WolfData.of(wolf) : null;
        if (d != null && d.variant != null) {
            tag.setString("variant", d.variant.id());
        }
    }

    /** @return the variant of a tag value, with or without its namespace (1.20.5 reads a resource location) */
    @Nullable
    static WolfVariant byTag(String value) {
        try {
            return WolfVariant.byId(new ResourceLocation(value).toString());
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** P6: {@code WorldEntitySpawner.performWorldGenSpawning}'s {@code biome.getSpawningChance()}. */
    public static float spawningChance(Biome biome) {
        float vanilla = biome.getSpawningChance();
        ResourceLocation id = biome.getRegistryName();
        if (id == null) {
            return vanilla;
        }
        String path = id.getPath();
        boolean mc = "minecraft".equals(id.getNamespace());
        if (WolfContent.enabled() && (mc ? path.equals("mesa_rock") || path.equals("mutated_mesa_rock") : path.equals("wooded_badlands"))) {
            return 0.04F;
        }
        if (ArmadilloContent.enabled() && (mc ? path.equals("mesa") || path.equals("mutated_mesa") || path.equals("mesa_clear_rock")
                || path.equals("mutated_mesa_clear_rock") : path.equals("badlands") || path.equals("eroded_badlands"))) {
            return 0.03F;
        }
        return vanilla;
    }

    /** 1.20.6's {@code Wolf.WolfPanicGoal}: a wolf runs only when on fire or freezing (not when hurt, as other animals). */
    public static final class Panic extends EntityAIPanic {
        public Panic(EntityCreature wolf) {
            super(wolf, 1.5D);
        }

        @Override
        public boolean shouldExecute() {
            if (!this.creature.isBurning() && !freezing(this.creature)) {
                return false;
            }
            if (this.creature.isBurning()) {
                // 1.12.2's own: water within 5 when burning, else a random spot.
                return super.shouldExecute();
            }
            return findRandomPosition();
        }
    }

    private static Method inSnow;
    private static boolean noFreezing;

    /** 1.20.6's {@code isFreezing}: in powder snow, which is Caves &amp; Cliffs' ({@code powdersnow.Freezing.inSnow}). */
    static boolean freezing(Entity entity) {
        if (noFreezing) {
            return false;
        }
        try {
            if (inSnow == null) {
                inSnow = Class.forName("com.exiledradio.cavesandcliffsbackport.powdersnow.Freezing").getMethod("inSnow", Entity.class);
            }
            return Boolean.TRUE.equals(inSnow.invoke(null, entity));
        } catch (ReflectiveOperationException | RuntimeException e) {
            noFreezing = true;
            return false;
        }
    }
}
