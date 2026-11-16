package com.exiledradio.trailsandtalesbackport.misc;

import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.CombatEntry;
import net.minecraft.util.CombatTracker;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.border.WorldBorder;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 1.20's two new damage types (23w06a moved damage to data; {@code generic_kill} and {@code outside_border} are
 * 1.20's), as death messages: {@code /kill} kills with {@code genericKill} ("%1$s was killed")
 * instead of {@code outOfWorld} ("fell out of the world"), and the world border hurts with {@code outsideBorder}
 * ("%1$s left the confines of this world") instead of {@code inWall} ("suffocated in a wall").
 *
 * <p>The damage itself is left alone. 1.12.2 tests {@code source == DamageSource.OUT_OF_WORLD} by identity wherever
 * 1.20.6 tests the {@code bypasses_invulnerability} and {@code bypasses_resistance} tags that {@code generic_kill}
 * is in (the Invulnerable flag, a player's respawn invulnerability, a spawning wither, Resistance), and other mods test
 * it to let {@code /kill} through their own immunities; a new source would fail all of those. So {@code /kill} still
 * hurts with {@code OUT_OF_WORLD} and the border with {@code IN_WALL}, and only the combat tracker's record of that hit,
 * which every death message is made from, gets the new source. That record is made inside the same
 * {@code attackEntityFrom} call, after {@link LivingAttackEvent}: the event arms the entity's combat list, the list's
 * next entry of the expected source (in the same tick) takes the new one. The record also decides 1.12.2's "fell"
 * messages: an {@code OUT_OF_WORLD} entry counts as the longest fall, which 1.20.6 keeps for {@code out_of_world}
 * only ({@code always_most_significant_fall}), so a {@code /kill} after a fall is "was killed" in both.
 *
 * <p>Recognized as vanilla's own calls: {@code /kill} is {@code EntityLivingBase.onKillCommand}'s
 * {@code OUT_OF_WORLD} with {@code Float.MAX_VALUE} (void damage is 4); the border is
 * {@code EntityLivingBase.onEntityUpdate}'s {@code IN_WALL} hit of a player that is not inside an opaque block, is
 * outside the border, and gets exactly the border's damage for its distance ({@code max(1, floor(-d * perBlock))}).
 */
@Mod.EventBusSubscriber(modid = TrailsAndTalesBackport.MOD_ID)
public final class DeathMessages {

    /** 1.20's {@code generic_kill}, with {@code OUT_OF_WORLD}'s properties. */
    public static final DamageSource GENERIC_KILL = new DamageSource("genericKill").setDamageBypassesArmor().setDamageAllowedInCreativeMode();
    /** 1.20's {@code outside_border}, with {@code IN_WALL}'s properties. */
    public static final DamageSource OUTSIDE_BORDER = new DamageSource("outsideBorder").setDamageBypassesArmor();

    private static Field entriesField;
    private static Field sourceField;
    private static boolean broken;

    private DeathMessages() {
    }

    /** @return true while the change is on */
    public static boolean enabled() {
        return ModConfig.misc.enabled && ModConfig.misc.deathMessages;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void attack(LivingAttackEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (broken || entity.world.isRemote || !enabled()) {
            return;
        }
        DamageSource replacement = replacement(entity, event.getSource(), event.getAmount());
        if (replacement != null) {
            arm(entity, event.getSource(), replacement);
        }
    }

    /** @return the 1.20 source this vanilla hit is recorded with, or null when it is not one of the two */
    public static DamageSource replacement(EntityLivingBase entity, DamageSource source, float amount) {
        if (source == DamageSource.OUT_OF_WORLD && amount == Float.MAX_VALUE) {
            return GENERIC_KILL;
        }
        if (source == DamageSource.IN_WALL && entity instanceof EntityPlayer && amount == borderDamage(entity)) {
            return OUTSIDE_BORDER;
        }
        return null;
    }

    /** @return 1.12.2's border damage for this entity this tick (as {@code onEntityUpdate}), or -1 when it takes none */
    static float borderDamage(EntityLivingBase entity) {
        WorldBorder border = entity.world.getWorldBorder();
        if (entity.isEntityInsideOpaqueBlock() || border.contains(entity.getEntityBoundingBox())) {
            return -1;
        }
        double distance = border.getClosestDistance(entity) + border.getDamageBuffer();
        double perBlock = border.getDamageAmount();
        if (distance >= 0.0D || perBlock <= 0.0D) {
            return -1;
        }
        return (float) Math.max(1, MathHelper.floor(-distance * perBlock));
    }

    private static void arm(EntityLivingBase entity, DamageSource from, DamageSource to) {
        try {
            if (entriesField == null) {
                entriesField = ObfuscationReflectionHelper.findField(CombatTracker.class, "field_94556_a");
                sourceField = ObfuscationReflectionHelper.findField(CombatEntry.class, "field_94569_a");
            }
            CombatTracker tracker = entity.getCombatTracker();
            @SuppressWarnings("unchecked")
            List<CombatEntry> list = (List<CombatEntry>) entriesField.get(tracker);
            Entries entries;
            if (list instanceof Entries) {
                entries = (Entries) list;
            } else {
                entries = new Entries(list);
                entriesField.set(tracker, entries);
            }
            entries.from = from;
            entries.to = to;
            entries.entity = entity;
            entries.tick = entity.world.getTotalWorldTime();
        } catch (Exception e) {
            broken = true;
            TrailsAndTalesBackport.LOGGER.error("1.20 death messages are off: the combat tracker could not be reached.", e);
        }
    }

    /** A combat tracker's entry list that records the next armed hit with its 1.20 source. */
    static final class Entries extends ArrayList<CombatEntry> {
        private static final long serialVersionUID = 1L;

        DamageSource from;
        DamageSource to;
        EntityLivingBase entity;
        long tick;

        Entries(Collection<CombatEntry> existing) {
            super(existing);
        }

        @Override
        public boolean add(CombatEntry entry) {
            if (this.to != null && entry.getDamageSrc() == this.from && this.entity.world.getTotalWorldTime() == this.tick) {
                try {
                    sourceField.set(entry, this.to);
                } catch (IllegalAccessException e) {
                    broken = true;
                    TrailsAndTalesBackport.LOGGER.error("1.20 death messages are off: a combat entry could not be changed.", e);
                }
                this.to = null;
                this.from = null;
                this.entity = null;
            }
            return super.add(entry);
        }
    }
}
