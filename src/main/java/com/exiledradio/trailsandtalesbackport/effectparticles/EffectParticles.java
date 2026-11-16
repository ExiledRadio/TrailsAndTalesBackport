package com.exiledradio.trailsandtalesbackport.effectparticles;

import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.network.NetworkHandler;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.PotionColorCalculationEvent;
import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

/**
 * 1.20.5's effect particles (24w03a): "Status effect particle colors are no longer blended into one particle color.
 * All active visible status effect particles are rendered separately. Emission frequency was lowered."
 *
 * <h2>1.20.6</h2>
 * {@code LivingEntity.updateSynchronizedMobEffectParticles} syncs a list, one particle per visible effect ({@code
 * MobEffect.createParticleOptions}: {@code entity_effect} in the effect's color, alpha 255, or {@code
 * floor(38.25)} = 38 for an ambient one), and whether every visible effect is ambient ({@code
 * areAllEffectsAmbient}). Each tick {@code tickEffects}, on the client, draws one chance in 4 (15 while invisible,
 * five times less when all are ambient) of one particle, picked at random from the list, at a random point of the
 * body.
 *
 * <h2>Here</h2>
 * 1.12.2 syncs one blended color ({@code POTION_EFFECTS}) and one ambient flag. The server sends each tracked
 * entity's list in a packet of its own ({@link EffectParticlesMessage}, packet 90) when it changes, to a player who
 * starts tracking the entity, and to a player for themselves on log-in, respawn and dimension change. On the client,
 * patch P3 hands {@code EntityLivingBase.updatePotionEffects}' blended color to {@link #effectColor}, which draws
 * 1.20.6's particle and answers 0 (so 1.12.2 draws nothing) for an entity the server sent a list for, while
 * {@link #active}. Off, or for an entity no list came for (a server without this mod), 1.12.2's swirl is drawn.
 *
 * <p>Tricky Trials draws its four 1.21 effects (wind charged, weaving, oozing, infested) with their own particles,
 * on 1.21's same roll. They stay in the list (so the pick is among all visible effects, as 1.21's), sent with alpha 0:
 * a pick that lands on one is left to Tricky Trials.
 */
@Mod.EventBusSubscriber(modid = TrailsAndTalesBackport.MOD_ID)
public final class EffectParticles {

    /** 1.20.6's {@code MobEffect.AMBIENT_ALPHA}: {@code Mth.floor(38.25F)}. */
    public static final int AMBIENT_ALPHA = 38;
    /** The version these particles came in. */
    public static final String VERSION = "1.20.5";

    /** Effects another mod draws its own particle for (alpha 0 in the list). */
    public static final List<ResourceLocation> OWN_PARTICLE = Arrays.asList(
            new ResourceLocation("trickytrialsbackport", "wind_charged"),
            new ResourceLocation("trickytrialsbackport", "weaving"),
            new ResourceLocation("trickytrialsbackport", "oozing"),
            new ResourceLocation("trickytrialsbackport", "infested"));

    /** Server: what each entity's trackers were last sent. Server thread only. */
    private static final Map<EntityLivingBase, Synced> LAST = new WeakHashMap<EntityLivingBase, Synced>();
    /** Server: entities whose effects changed this tick. Server thread only. */
    private static final Map<EntityLivingBase, Boolean> PENDING = new WeakHashMap<EntityLivingBase, Boolean>();
    /** Messages sent, for the tests. */
    public static int sent;

    /** Client: entity id to the list the server sent. Client thread only. */
    static final Map<Integer, Synced> CLIENT = new HashMap<Integer, Synced>();

    private EffectParticles() {
    }

    /** @return true while the client draws 1.20.5's particles: the option is on and 1.20.5 or later is shown */
    public static boolean active() {
        return ModConfig.misc.effectParticles && Presentation.isAtLeast(VERSION);
    }

    // ---------------------------------------------------------------------------------------------
    // What is synced
    // ---------------------------------------------------------------------------------------------

    /** One entity's particles: each visible effect's ARGB color, and whether every visible effect is ambient. */
    public static final class Synced {
        public final int[] colors;
        public final boolean ambient;

        public Synced(int[] colors, boolean ambient) {
            this.colors = colors;
            this.ambient = ambient;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Synced && ((Synced) o).ambient == this.ambient && Arrays.equals(((Synced) o).colors, this.colors);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(this.colors) * 2 + (this.ambient ? 1 : 0);
        }

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder("[");
            for (int c : this.colors) {
                b.append(b.length() > 1 ? "," : "").append(String.format("%08x", c));
            }
            return b.append(this.ambient ? "] ambient" : "]").toString();
        }
    }

    /** 1.20.6's {@code updateSynchronizedMobEffectParticles} and {@code areAllEffectsAmbient}. */
    public static Synced compute(EntityLivingBase entity) {
        Collection<PotionEffect> effects = entity.getActivePotionEffects();
        int[] colors = new int[effects.size()];
        int n = 0;
        boolean ambient = true;
        for (PotionEffect effect : effects) {
            if (!effect.doesShowParticles()) {
                continue;
            }
            ambient &= effect.getIsAmbient();
            colors[n++] = color(effect);
        }
        colors = Arrays.copyOf(colors, n);
        // The pick is random, so the order means nothing; sorted, a list compares equal whatever the map's order.
        Arrays.sort(colors);
        return new Synced(colors, ambient);
    }

    /** 1.20.6's {@code MobEffect.createParticleOptions}: the effect's color, alpha 38 when ambient, else 255. */
    public static int color(PotionEffect effect) {
        Potion potion = effect.getPotion();
        int alpha = OWN_PARTICLE.contains(potion.getRegistryName()) ? 0 : effect.getIsAmbient() ? AMBIENT_ALPHA : 255;
        return alpha << 24 | potion.getLiquidColor() & 0xFFFFFF;
    }

    /** @return what the entity's trackers were last sent, or null (for the tests) */
    public static Synced lastSent(EntityLivingBase entity) {
        return LAST.get(entity);
    }

    // ---------------------------------------------------------------------------------------------
    // Server: sending
    // ---------------------------------------------------------------------------------------------

    /** Fired whenever 1.12.2 recomputes the blended color: an effect was added, changed or removed. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void colorCalculated(PotionColorCalculationEvent event) {
        changed(event.getEntityLiving());
    }

    /** The last effect going leaves no color to compute, so removals are watched as well. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void removed(PotionEvent.PotionRemoveEvent event) {
        changed(event.getEntityLiving());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void expired(PotionEvent.PotionExpiryEvent event) {
        changed(event.getEntityLiving());
    }

    private static void changed(EntityLivingBase entity) {
        if (entity != null && entity.world != null && !entity.world.isRemote) {
            PENDING.put(entity, Boolean.TRUE);
        }
    }

    /** After every entity's update: each changed entity's list, sent if it differs from what was sent. */
    @SubscribeEvent
    public static void worldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote) {
            return;
        }
        if (event.world.getTotalWorldTime() % 20 == 0) {
            // Once a second, entities with particles are looked at again: an effect taken away by a mod without an
            // event is caught here.
            for (EntityLivingBase entity : LAST.keySet()) {
                PENDING.put(entity, Boolean.TRUE);
            }
        }
        if (PENDING.isEmpty()) {
            return;
        }
        List<EntityLivingBase> here = new ArrayList<EntityLivingBase>();
        for (EntityLivingBase entity : PENDING.keySet()) {
            if (entity.world == event.world) {
                here.add(entity);
            }
        }
        for (EntityLivingBase entity : here) {
            PENDING.remove(entity);
            update(entity);
        }
    }

    /** Sends the entity's list to its trackers (and itself) if it changed. */
    static void update(EntityLivingBase entity) {
        Synced now = entity.isDead ? new Synced(new int[0], true) : compute(entity);
        Synced last = LAST.get(entity);
        boolean empty = now.colors.length == 0;
        if (last == null ? empty : last.equals(now)) {
            if (empty) {
                LAST.remove(entity);
            }
            return;
        }
        if (entity.world instanceof WorldServer && NetworkHandler.channel != null) {
            ((WorldServer) entity.world).getEntityTracker().sendToTrackingAndSelf(entity,
                    NetworkHandler.channel.getPacketFrom(new EffectParticlesMessage(entity.getEntityId(), now)));
            sent++;
        }
        if (empty) {
            LAST.remove(entity);
        } else {
            LAST.put(entity, now);
        }
    }

    /** Sends one player the entity's list, if it has particles. */
    static void sendTo(EntityLivingBase entity, EntityPlayerMP player) {
        Synced now = compute(entity);
        if (now.colors.length > 0 && NetworkHandler.channel != null && player.connection != null) {
            NetworkHandler.channel.sendTo(new EffectParticlesMessage(entity.getEntityId(), now), player);
            sent++;
        }
    }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof EntityLivingBase && event.getEntityPlayer() instanceof EntityPlayerMP) {
            sendTo((EntityLivingBase) event.getTarget(), (EntityPlayerMP) event.getEntityPlayer());
        }
    }

    @SubscribeEvent
    public static void loggedIn(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent event) {
        self(event.player);
    }

    @SubscribeEvent
    public static void respawned(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent event) {
        self(event.player);
    }

    @SubscribeEvent
    public static void changedDimension(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent event) {
        self(event.player);
    }

    private static void self(net.minecraft.entity.player.EntityPlayer player) {
        if (player instanceof EntityPlayerMP) {
            sendTo(player, (EntityPlayerMP) player);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Client: drawing (patch P3)
    // ---------------------------------------------------------------------------------------------

    /**
     * Patch P3's hook, in {@code EntityLivingBase.updatePotionEffects} on both sides, with the blended color 1.12.2
     * just read. On the client, for an entity the server sent a list for, while {@link #active}: 1.20.6's {@code
     * tickEffects} particle, and 0 back so 1.12.2 draws nothing. Otherwise the color, unchanged.
     */
    public static int effectColor(int vanilla, EntityLivingBase entity) {
        World world = entity.world;
        if (world == null || !world.isRemote || CLIENT.isEmpty()) {
            return vanilla;
        }
        Synced synced = CLIENT.get(entity.getEntityId());
        if (synced == null || !active()) {
            return vanilla;
        }
        int color = roll(entity.getRNG(), entity.isInvisible(), synced);
        if (color != 0) {
            Random random = entity.getRNG();
            // 1.20.6's getRandomX(0.5), getRandomY(), getRandomZ(0.5), in that order.
            double x = entity.posX + (2.0 * random.nextDouble() - 1.0) * entity.width * 0.5;
            double y = entity.posY + entity.height * random.nextDouble();
            double z = entity.posZ + (2.0 * random.nextDouble() - 1.0) * entity.width * 0.5;
            com.exiledradio.trailsandtalesbackport.effectparticles.client.EffectParticlesClient.spawn(x, y, z, color);
        }
        return 0;
    }

    /**
     * 1.20.6's roll: one chance in 4 (15 while invisible, times 5 when every effect is ambient), then one of the
     * list's particles.
     *
     * @return the picked ARGB color, or 0 for no particle this tick (none rolled, or one another mod draws)
     */
    public static int roll(Random random, boolean invisible, Synced synced) {
        if (synced.colors.length == 0) {
            return 0;
        }
        int rate = invisible ? 15 : 4;
        int ambient = synced.ambient ? 5 : 1;
        if (random.nextInt(rate * ambient) != 0) {
            return 0;
        }
        int color = synced.colors[random.nextInt(synced.colors.length)];
        return color >>> 24 == 0 ? 0 : color;
    }

    /** @return the client's lists by entity id (client thread only) */
    public static Map<Integer, Synced> clientLists() {
        return CLIENT;
    }

    /** Client thread: the server's list for an entity (an empty one too: the server speaks for that entity). */
    public static void received(int entityId, Synced synced) {
        CLIENT.put(entityId, synced);
    }
}
