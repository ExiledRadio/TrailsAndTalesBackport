package com.exiledradio.trailsandtalesbackport.wolf.client;

import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.wolf.ItemWolfArmor;
import com.exiledradio.trailsandtalesbackport.wolf.WolfContent;
import com.exiledradio.trailsandtalesbackport.wolf.WolfData;
import com.exiledradio.trailsandtalesbackport.wolf.WolfVariant;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Client parts of the wolves stream: the wolf renderer, wolf armor's item colors and dyed model, and the wolf
 * state the server sends.
 */
public final class WolfClient {

    /** States that arrived before their wolf: entity id -> (variant, armor). */
    private static final Map<Integer, Object[]> PENDING = new HashMap<Integer, Object[]>();

    private WolfClient() {
    }

    public static void init() {
        if (!WolfContent.enabled()) {
            return;
        }
        RenderingRegistry.registerEntityRenderingHandler(EntityWolf.class, RenderWolfVariant::new);
        MinecraftForge.EVENT_BUS.register(new WolfClient.Events());
    }

    public static final class Events {
        /** The item shows its overlay only when dyed: 1.20.5 tints it with a transparent color when it is not. */
        @SubscribeEvent
        public void models(ModelRegistryEvent event) {
            if (WolfContent.wolfArmor != null) {
                WolfContent.wolfArmor.addPropertyOverride(new ResourceLocation(TrailsAndTalesBackport.MOD_ID, "dyed"),
                        (stack, world, entity) -> ItemWolfArmor.hasColor(stack) ? 1.0F : 0.0F);
            }
        }

        /** 1.20.5's item color: layer 1 (the overlay) in the dyed color. */
        @SubscribeEvent
        public void colors(ColorHandlerEvent.Item event) {
            if (WolfContent.wolfArmor != null) {
                event.getItemColors().registerItemColorHandler((stack, tint) -> tint != 1 ? -1
                        : ItemWolfArmor.hasColor(stack) ? ItemWolfArmor.getColor(stack) : -1, WolfContent.wolfArmor);
            }
        }
    }

    /** Packet 70, on the network thread. */
    public static void state(final int id, final WolfVariant variant, final ItemStack armor) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            WorldClient world = Minecraft.getMinecraft().world;
            Entity e = world == null ? null : world.getEntityByID(id);
            if (e instanceof EntityWolf) {
                apply((EntityWolf) e, variant, armor);
            } else {
                PENDING.put(id, new Object[] {variant, armor});
            }
        });
    }

    /** A wolf joined the client world: a state that came first is applied now. */
    public static void joined(EntityWolf wolf) {
        Object[] s = PENDING.remove(wolf.getEntityId());
        if (s != null) {
            apply(wolf, (WolfVariant) s[0], (ItemStack) s[1]);
        }
    }

    private static void apply(EntityWolf wolf, WolfVariant variant, ItemStack armor) {
        WolfData data = WolfData.of(wolf);
        if (data != null) {
            data.variant = variant;
            data.armor = armor;
        }
    }

    /**
     * Packet 71: 1.20's {@code LivingEntity.breakItem} for the body slot - the armor's break sound and five item
     * particles from the wolf's eyes.
     */
    public static void armorBroke(final int id, final ItemStack armor) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            WorldClient world = Minecraft.getMinecraft().world;
            Entity e = world == null ? null : world.getEntityByID(id);
            if (!(e instanceof EntityWolf)) {
                return;
            }
            EntityWolf wolf = (EntityWolf) e;
            if (!wolf.isSilent()) {
                world.playSound(wolf.posX, wolf.posY, wolf.posZ, WolfContent.WOLF_ARMOR_BREAK, wolf.getSoundCategory(), 0.8F,
                        0.8F + world.rand.nextFloat() * 0.4F, false);
            }
            for (int i = 0; i < 5; i++) {
                Vec3d v = new Vec3d((wolf.getRNG().nextFloat() - 0.5D) * 0.1D, Math.random() * 0.1D + 0.1D, 0.0D);
                v = v.rotatePitch(-wolf.rotationPitch * 0.017453292F).rotateYaw(-wolf.rotationYaw * 0.017453292F);
                double y = -wolf.getRNG().nextFloat() * 0.6D - 0.3D;
                Vec3d p = new Vec3d((wolf.getRNG().nextFloat() - 0.5D) * 0.3D, y, 0.6D);
                p = p.rotatePitch(-wolf.rotationPitch * 0.017453292F).rotateYaw(-wolf.rotationYaw * 0.017453292F);
                p = p.add(wolf.posX, wolf.posY + wolf.getEyeHeight(), wolf.posZ);
                world.spawnParticle(EnumParticleTypes.ITEM_CRACK, p.x, p.y, p.z, v.x, v.y + 0.05D, v.z,
                        Item.getIdFromItem(armor.getItem()), armor.getMetadata());
            }
        });
    }
}
