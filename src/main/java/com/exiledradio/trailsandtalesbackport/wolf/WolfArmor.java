package com.exiledradio.trailsandtalesbackport.wolf;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.WorldServer;

import com.exiledradio.trailsandtalesbackport.common.Owners;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A wolf's body armor: 1.20.5's {@code Wolf.actuallyHurt}, {@code hurtArmor}, {@code Crackiness} and the armor's
 * attribute modifier.
 */
public final class WolfArmor {

    /** 1.20.5's {@code ArmorItem} modifier UUID for the body slot. */
    public static final UUID BODY_MODIFIER = UUID.fromString("C1C72771-8B8E-BA4A-ACE0-81A93C8928B2");

    /**
     * 1.20.5's {@code #bypasses_wolf_armor} damage types, by 1.12.2 (and family) damage type names: everything that
     * bypasses invulnerability (the void, {@code /kill}), cramming, drowning, drying out, freezing, suffocation,
     * magic, standing outside the border, starving, thorns and wither.
     */
    static final Set<String> BYPASSES_WOLF_ARMOR = new HashSet<String>(Arrays.asList("outOfWorld", "genericKill", "cramming",
            "drown", "dryout", "dryOut", "freeze", "inWall", "indirectMagic", "magic", "outsideBorder", "starve", "thorns",
            "wither"));

    /** 1.20.5's {@code Crackiness.WOLF_ARMOR}: the fractions of durability left below which each level starts. */
    public enum Crack {
        NONE, LOW, MEDIUM, HIGH;

        public static Crack byFraction(float left) {
            if (left < 0.32F) {
                return HIGH;
            } else if (left < 0.69F) {
                return MEDIUM;
            }
            return left < 0.95F ? LOW : NONE;
        }

        public static Crack byDamage(int damage, int max) {
            return byFraction((float) (max - damage) / (float) max);
        }

        public static Crack of(ItemStack stack) {
            return stack.isEmpty() || !stack.isItemStackDamageable() ? NONE : byDamage(stack.getItemDamage(), stack.getMaxDamage());
        }
    }

    private WolfArmor() {
    }

    public static boolean isWolfArmor(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemWolfArmor;
    }

    public static boolean bypasses(DamageSource source) {
        return BYPASSES_WOLF_ARMOR.contains(source.getDamageType());
    }

    /** 1.20.5's {@code canArmorAbsorb}. */
    public static boolean canAbsorb(WolfData data, DamageSource source) {
        return data.hasArmor() && !bypasses(source);
    }

    /** Puts on or takes off the armor (empty), with its 11 armor points, and tells the trackers. */
    public static void set(EntityWolf wolf, WolfData data, ItemStack stack) {
        data.armor = stack;
        updateModifier(wolf, data);
        WolfNetwork.broadcastState(wolf);
    }

    /** Keeps 1.20.5's armor attribute modifier in step with the armor worn. */
    static void updateModifier(EntityWolf wolf, WolfData data) {
        IAttributeInstance armor = wolf.getEntityAttribute(SharedMonsterAttributes.ARMOR);
        AttributeModifier have = armor.getModifier(BODY_MODIFIER);
        if (data.hasArmor() && have == null) {
            armor.applyModifier(new AttributeModifier(BODY_MODIFIER, "Armor modifier", ItemWolfArmor.DEFENSE, 0));
        } else if (!data.hasArmor() && have != null) {
            armor.removeModifier(BODY_MODIFIER);
        }
    }

    /**
     * 1.20.5's {@code Wolf.actuallyHurt} when the armor absorbs: the armor takes {@code ceil(amount)} damage and
     * the wolf none; crossing into another crack level plays the crack sound with 20 scute particles.
     */
    static void absorb(EntityWolf wolf, WolfData data, float amount) {
        ItemStack stack = data.armor;
        int before = stack.getItemDamage();
        int max = stack.getMaxDamage();
        boolean broke = hurtAndBreak(wolf, data, (int) Math.ceil(amount));
        Crack after = broke ? Crack.NONE : Crack.of(data.armor);
        if (Crack.byDamage(before, max) != after) {
            wolf.playSound(WolfContent.WOLF_ARMOR_CRACK, 1.0F, 1.0F);
            if (wolf.world instanceof WorldServer) {
                Item scute = Owners.ours("armadillo_scute");
                Item shown = scute == null ? Items.LEATHER : scute;
                ((WorldServer) wolf.world).spawnParticle(EnumParticleTypes.ITEM_CRACK, wolf.posX, wolf.posY + 1.0D, wolf.posZ,
                        20, 0.2D, 0.1D, 0.2D, 0.1D, Item.getIdFromItem(shown), 0);
            }
        }
    }

    /**
     * 1.20.5's {@code hurtArmor} ({@code doHurtEquipment} for the body): damage that is not absorbed but is
     * blocked by armor wears it by a quarter, at least 1.
     */
    static void hurtByUnabsorbed(EntityWolf wolf, WolfData data, float amount) {
        if (amount <= 0.0F || !data.hasArmor()) {
            return;
        }
        float d = amount / 4.0F;
        if (d < 1.0F) {
            d = 1.0F;
        }
        hurtAndBreak(wolf, data, (int) d);
    }

    /**
     * 1.20.5's {@code ItemStack.hurtAndBreak} for the body slot: on breaking, the armor is gone and every tracker
     * plays the break (entity event 65).
     *
     * @return true if it broke
     */
    static boolean hurtAndBreak(EntityWolf wolf, WolfData data, int amount) {
        if (amount <= 0 || !data.hasArmor()) {
            return false;
        }
        ItemStack stack = data.armor.copy();
        int damage = stack.getItemDamage() + amount;
        if (damage >= stack.getMaxDamage()) {
            set(wolf, data, ItemStack.EMPTY);
            WolfNetwork.broadcastArmorBroke(wolf, stack);
            return true;
        }
        stack.setItemDamage(damage);
        set(wolf, data, stack);
        return false;
    }

    /** 1.20.5's {@code Wolf.mobInteract} scute repair: an eighth of the durability. */
    static void repair(EntityWolf wolf, WolfData data) {
        ItemStack stack = data.armor.copy();
        int unit = (int) ((float) stack.getMaxDamage() * 0.125F);
        stack.setItemDamage(Math.max(0, stack.getItemDamage() - unit));
        set(wolf, data, stack);
        wolf.playSound(WolfContent.WOLF_ARMOR_REPAIR, 1.0F, 1.0F);
    }
}
