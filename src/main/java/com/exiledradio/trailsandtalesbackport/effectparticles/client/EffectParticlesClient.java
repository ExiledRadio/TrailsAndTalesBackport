package com.exiledradio.trailsandtalesbackport.effectparticles.client;

import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.effectparticles.EffectParticles;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Iterator;

/**
 * Client side of 1.20.5's effect particles: keeps the lists the server sends, and draws one particle as 1.20.6's
 * {@code SpellParticle.MobEffectProvider} does ({@code entity_effect}: 1.12.2's spell particle, given the color and
 * alpha after it is made, so its speed is 1.20.6's (1, 1, 1) and not the color, as 1.12.2's {@code mobSpell} takes it).
 */
public final class EffectParticlesClient {

    /** Particles drawn, and the last one's color, for the client test. */
    public static int drawn;
    public static int lastColor;

    private int ticks;

    private EffectParticlesClient() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new EffectParticlesClient());
        // `runClient -Pselftest=liveeffects`: the client check (not "client", which runs the look-and-feel one), reached by name (a release jar has no test classes).
        String selftest = System.getProperty(TrailsAndTalesBackport.MOD_ID + ".selftest");
        if (selftest != null && selftest.contains("liveeffects")) {
            try {
                Class.forName("com.exiledradio.trailsandtalesbackport.selftest.ClientEffectParticlesTest").getMethod("register").invoke(null);
            } catch (ReflectiveOperationException e) {
                TrailsAndTalesBackport.LOGGER.error("CLIENT SELFTEST DONE pass=0 fail=1: the test classes are not in this build", e);
            }
        }
    }

    /** From the network thread. */
    public static void schedule(final int entityId, final EffectParticles.Synced synced) {
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                EffectParticles.received(entityId, synced);
            }
        });
    }

    /** A new entity under an old id (it came back into view, or a player respawned) starts with no list. */
    @SubscribeEvent
    public void joined(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote) {
            clear(event.getEntity().getEntityId());
        }
    }

    @SubscribeEvent
    public void unload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) {
            clearAll();
        }
    }

    /** Lists of entities no longer in the client's world are dropped every five seconds. */
    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ++this.ticks % 100 != 0) {
            return;
        }
        World world = Minecraft.getMinecraft().world;
        if (world == null) {
            clearAll();
            return;
        }
        for (Iterator<Integer> it = ids().iterator(); it.hasNext(); ) {
            if (world.getEntityByID(it.next()) == null) {
                it.remove();
            }
        }
    }

    private static java.util.Set<Integer> ids() {
        return EffectParticles.clientLists().keySet();
    }

    private static void clear(int id) {
        EffectParticles.clientLists().remove(id);
    }

    private static void clearAll() {
        EffectParticles.clientLists().clear();
    }

    /** 1.20.6's {@code addParticle} for an {@code entity_effect}: within 32 blocks and the particle setting. */
    public static void spawn(double x, double y, double z, int argb) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!shown(mc, x, y, z)) {
            return;
        }
        Particle particle = mc.effectRenderer.spawnEffectParticle(EnumParticleTypes.SPELL_MOB.getParticleID(), x, y, z, 1.0, 1.0, 1.0);
        if (particle == null) {
            return;
        }
        particle.setRBGColorF((argb >> 16 & 255) / 255.0F, (argb >> 8 & 255) / 255.0F, (argb & 255) / 255.0F);
        particle.setAlphaF((argb >>> 24) / 255.0F);
        drawn++;
        lastColor = argb;
    }

    /** 1.12.2's {@code RenderGlobal} rule for a particle that doesn't ignore range. */
    private static boolean shown(Minecraft mc, double x, double y, double z) {
        Entity view = mc.getRenderViewEntity();
        if (view == null || mc.world == null || mc.effectRenderer == null) {
            return false;
        }
        double dx = view.posX - x;
        double dy = view.posY - y;
        double dz = view.posZ - z;
        // calculateParticleLevel runs first, and draws from the world's random whether or not the particle is in range.
        int level = mc.gameSettings.particleSetting;
        if (level == 1 && mc.world.rand.nextInt(3) == 0) {
            level = 2;
        }
        return dx * dx + dy * dy + dz * dz <= 1024.0 && level <= 1;
    }
}
