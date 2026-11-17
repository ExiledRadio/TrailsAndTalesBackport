package com.exiledradio.trailsandtalesbackport.mechanics;

import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemArrow;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemChorusFruit;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * The smaller 1.20.x rules: the shield's facing (P8), Jump Boost on slimes (P10), chorus fruit and fall distance,
 * named arrows (with P11). Each hook checks its own setting, so with it off the game behaves as 1.12.2.
 */
@Mod.EventBusSubscriber(modid = TrailsAndTalesBackport.MOD_ID)
public final class MechanicsHooks {

    /** The entity tag key holding a shot arrow's item, as 1.20.3's {@code AbstractArrow.pickupItemStack}. */
    public static final String ARROW_ITEM = "trailsandtalesbackport:item";

    private static final Map<EntityLivingBase, Vec3d> EATING = new WeakHashMap<EntityLivingBase, Vec3d>();

    private MechanicsHooks() {
    }

    // ---- P8: shields (1.20.3, 23w40a) -----------------------------------------------------------------------

    /**
     * Replaces {@code this.getLook(1.0F)} in {@code EntityLivingBase.canBlockDamageSource}. 1.20.3's
     * {@code isDamageSourceBlocked} uses {@code calculateViewVector(0, yHeadRot)}: the facing without the pitch, so a
     * player looking straight up or down still blocks what is in front (1.12.2's vector shrinks to nothing there).
     */
    public static Vec3d shieldLook(EntityLivingBase entity, float partialTicks) {
        if (!ModConfig.mechanics.shieldPitch) {
            return entity.getLook(partialTicks);
        }
        float yaw = -entity.rotationYawHead * 0.017453292F - (float) Math.PI;
        return new Vec3d(-MathHelper.sin(yaw), 0.0D, -MathHelper.cos(yaw));
    }

    // ---- P10: slimes and magma cubes (1.20, 23w12a) -----------------------------------------------------------

    /**
     * Replaces {@code this.motionY = v} in {@code EntitySlime.jump} and {@code EntityMagmaCube.jump}. 1.20's
     * {@code Slime.jumpFromGround} uses {@code getJumpPower()}, which adds Jump Boost's {@code 0.1 * (amplifier + 1)}
     * as every other mob's jump does (1.12.2's slimes set a fixed speed).
     */
    public static void slimeJump(Entity slime, double motionY) {
        if (ModConfig.mechanics.slimeJumpBoost && slime instanceof EntityLivingBase) {
            PotionEffect boost = ((EntityLivingBase) slime).getActivePotionEffect(MobEffects.JUMP_BOOST);
            if (boost != null) {
                motionY += (double) ((float) (boost.getAmplifier() + 1) * 0.1F);
            }
        }
        slime.motionY = motionY;
    }

    // ---- chorus fruit (1.20, 23w12a) --------------------------------------------------------------------------

    /** Where an entity eating chorus fruit stood on its last use tick, just before the fruit is eaten. */
    @SubscribeEvent
    public static void eatingTick(LivingEntityUseItemEvent.Tick event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (!entity.world.isRemote && event.getItem().getItem() instanceof ItemChorusFruit) {
            EATING.put(entity, new Vec3d(entity.posX, entity.posY, entity.posZ));
        }
    }

    /** 1.20's {@code ChorusFruitItem.finishUsingItem} calls {@code resetFallDistance()} after a teleport. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void eaten(LivingEntityUseItemEvent.Finish event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (entity.world.isRemote || !(event.getItem().getItem() instanceof ItemChorusFruit)) {
            return;
        }
        Vec3d before = EATING.remove(entity);
        if (ModConfig.mechanics.chorusFallDistance && before != null
                && before.squareDistanceTo(entity.posX, entity.posY, entity.posZ) > 1.0E-7D) {
            entity.fallDistance = 0.0F;
        }
    }

    // ---- named arrows (1.20.3, 23w43a) ------------------------------------------------------------------------

    /**
     * 1.20.3's {@code AbstractArrow(type, owner, level, stack)}: the arrow keeps the shot item and takes its custom
     * name. A bow spawns its arrow from {@code onPlayerStoppedUsing} while the bow is still the player's active item
     * and before the ammunition shrinks, so the ammunition is found as {@code ItemBow.findAmmo} finds it.
     */
    @SubscribeEvent
    public static void arrowShot(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote || !(event.getEntity() instanceof EntityArrow) || !ModConfig.mechanics.arrowNames) {
            return;
        }
        EntityArrow arrow = (EntityArrow) event.getEntity();
        if (arrow.ticksExisted > 0 || !(arrow.shootingEntity instanceof EntityPlayer)
                || arrow.getEntityData().hasKey(ARROW_ITEM)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) arrow.shootingEntity;
        if (!player.isHandActive() || !(player.getActiveItemStack().getItem() instanceof ItemBow)) {
            return;
        }
        ItemStack ammo = findAmmo(player);
        if (ammo.isEmpty() || !ammo.hasDisplayName()) {
            return;
        }
        ItemStack kept = ammo.copy();
        kept.setCount(1);
        arrow.getEntityData().setTag(ARROW_ITEM, kept.writeToNBT(new NBTTagCompound()));
        arrow.setCustomNameTag(ammo.getDisplayName());
    }

    /** {@code ItemBow.findAmmo}: the off hand, the main hand, then the inventory in order. */
    public static ItemStack findAmmo(EntityPlayer player) {
        if (isArrow(player.getHeldItem(EnumHand.OFF_HAND))) {
            return player.getHeldItem(EnumHand.OFF_HAND);
        }
        if (isArrow(player.getHeldItem(EnumHand.MAIN_HAND))) {
            return player.getHeldItem(EnumHand.MAIN_HAND);
        }
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (isArrow(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean isArrow(ItemStack stack) {
        return stack.getItem() instanceof ItemArrow;
    }

    /**
     * P11: follows {@code this.getArrowStack()} in {@code EntityArrow.onCollideWithPlayer}. An arrow shot from a named
     * item gives that item back, as 1.20.3's {@code getPickupItem} returns a copy of {@code pickupItemStack}.
     */
    public static ItemStack arrowPickup(ItemStack vanilla, EntityArrow arrow) {
        if (!ModConfig.mechanics.arrowNames || !arrow.getEntityData().hasKey(ARROW_ITEM, 10)) {
            return vanilla;
        }
        ItemStack kept = new ItemStack(arrow.getEntityData().getCompoundTag(ARROW_ITEM));
        return kept.isEmpty() ? vanilla : kept;
    }
}
