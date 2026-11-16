package com.exiledradio.trailsandtalesbackport.selftest;

import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.asm.Asm;
import com.exiledradio.trailsandtalesbackport.effectparticles.EffectParticles;
import com.exiledradio.trailsandtalesbackport.effectparticles.EffectParticlesMessage;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;

import java.lang.reflect.Method;
import java.util.Random;

/**
 * 1.20.5's effect particles, server side: patch P3 applied, each entity's list (one color per visible effect, alpha
 * 38 when ambient, whether every visible one is ambient) as 1.20.6 computes it, sent when it changes and to a new
 * tracker, the packet's wire format, and 1.20.6's roll (one in 4, 15 invisible, five times less all ambient).
 */
public final class EffectParticlesSelfTest implements SelfTest {

    private static final Method UPDATE = ReflectionHelper.findMethod(EntityLivingBase.class, "updatePotionEffects", "func_70679_bo");

    @Override
    public String name() {
        return "effectparticles";
    }

    @Override
    public void run(Check c) throws Exception {
        c.ok(Asm.APPLIED.contains("P3"), "patch P3 (EntityLivingBase.updatePotionEffects) applied: " + Asm.APPLIED);
        c.ok(ModConfig.misc.effectParticles, "misc.effectParticles defaults to on");

        BlockPos corner = c.area();
        EntityPig pig = new EntityPig(c.world);
        pig.setPosition(corner.getX() + 8.5, corner.getY(), corner.getZ() + 8.5);
        c.world.spawnEntity(pig);

        // The list, as 1.20.6's updateSynchronizedMobEffectParticles.
        pig.addPotionEffect(new PotionEffect(MobEffects.SPEED, 600, 0, false, true));
        pig.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 600, 0, true, true));
        pig.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION, 600, 0, false, false));
        EffectParticles.Synced list = EffectParticles.compute(pig);
        c.eq(2, list.colors.length, "two visible effects, two particles (night vision hides its particles): " + list);
        c.ok(contains(list.colors, 0xFF000000 | MobEffects.SPEED.getLiquidColor()), "speed: its own color, alpha 255: " + list);
        c.ok(contains(list.colors, 38 << 24 | MobEffects.REGENERATION.getLiquidColor()), "ambient regeneration: alpha 38: " + list);
        c.ok(!list.ambient, "not every visible effect is ambient");

        // Sent when the effects change (PotionColorCalculationEvent from 1.12.2's own update), once.
        int before = EffectParticles.sent;
        UPDATE.invoke(pig);
        endTick(c);
        c.eq(list, EffectParticles.lastSent(pig), "the pig's trackers were sent its list");
        c.eq(before + 1, EffectParticles.sent, "one message for the change");
        UPDATE.invoke(pig);
        endTick(c);
        c.eq(before + 1, EffectParticles.sent, "nothing more while nothing changed");

        // A beacon's effects are all ambient: fewer particles.
        pig.removePotionEffect(MobEffects.SPEED);
        UPDATE.invoke(pig);
        endTick(c);
        EffectParticles.Synced beacon = EffectParticles.lastSent(pig);
        c.ok(beacon != null && beacon.ambient && beacon.colors.length == 1, "only ambient regeneration left: all ambient: " + beacon);
        c.eq(before + 2, EffectParticles.sent, "the removal was sent");

        // The last visible effect going: an empty list is sent, and the pig is forgotten.
        pig.removePotionEffect(MobEffects.REGENERATION);
        UPDATE.invoke(pig);
        endTick(c);
        c.eq(before + 3, EffectParticles.sent, "the empty list was sent");
        c.ok(EffectParticles.lastSent(pig) == null, "an entity without particles is not kept");
        c.eq(0, EffectParticles.compute(pig).colors.length, "night vision alone draws nothing");

        // A new tracker is sent the current list.
        pig.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 600, 1));
        EntityPlayerMP watcher = Players.real(c.world, "effectwatcher");
        int sentBefore = EffectParticles.sent;
        EffectParticles.startTracking(new PlayerEvent.StartTracking(watcher, pig));
        c.eq(sentBefore + 1, EffectParticles.sent, "a player starting to track the pig is sent its list");
        pig.clearActivePotions();
        endTick(c);
        c.eq(0, EffectParticles.compute(pig).colors.length, "milk-style clearing leaves nothing");

        // The server never draws: the hook hands 1.12.2's color back.
        c.eq(1234, EffectParticles.effectColor(1234, pig), "on the server the blended color is left alone");

        // Tricky Trials' four effects with their own particles are listed with alpha 0.
        Potion weaving = new Potion(false, 7891290) {
        };
        weaving.setRegistryName(EffectParticles.OWN_PARTICLE.get(1));
        c.eq(0, EffectParticles.color(new PotionEffect(weaving, 100)) >>> 24, "Tricky Trials' weaving is listed with alpha 0");

        // The packet.
        EffectParticles.Synced sample = new EffectParticles.Synced(new int[] {0xFF123456, 38 << 24 | 0xABCDEF}, true);
        ByteBuf buf = Unpooled.buffer();
        new EffectParticlesMessage(77, sample).toBytes(buf);
        EffectParticlesMessage read = new EffectParticlesMessage();
        read.fromBytes(buf);
        c.eq(77, read.entity, "packet: entity id");
        c.eq(sample, read.synced, "packet: colors and the ambient flag");
        c.eq(0, buf.readableBytes(), "packet: nothing left over");

        // 1.20.6's roll, 60000 ticks each.
        int a = 0xFF000001;
        int b = 0xFF000002;
        rate(c, false, false, 4, a, b);
        rate(c, false, true, 20, a, b);
        rate(c, true, false, 15, a, b);
        rate(c, true, true, 75, a, b);
        Random random = new Random(5);
        int other = 0;
        EffectParticles.Synced withOwn = new EffectParticles.Synced(new int[] {a, 0x00FFFFFF}, false);
        for (int i = 0; i < 60000; i++) {
            int got = EffectParticles.roll(random, false, withOwn);
            if (got != 0 && got != a) {
                other++;
            }
        }
        c.eq(0, other, "a pick of an effect another mod draws draws nothing here");
    }

    private static void rate(Check c, boolean invisible, boolean ambient, int oneIn, int a, int b) {
        Random random = new Random(oneIn * 31L + (invisible ? 1 : 0));
        EffectParticles.Synced synced = new EffectParticles.Synced(new int[] {a, b}, ambient);
        int n = 60000;
        int got = 0;
        int gotA = 0;
        for (int i = 0; i < n; i++) {
            int color = EffectParticles.roll(random, invisible, synced);
            if (color != 0) {
                got++;
                if (color == a) {
                    gotA++;
                }
            }
        }
        double expected = (double) n / oneIn;
        c.ok(Math.abs(got - expected) < expected * 0.1,
                "one particle in " + oneIn + " ticks (invisible " + invisible + ", ambient " + ambient + "): " + got + " of " + n);
        c.ok(Math.abs(gotA - got / 2.0) < got * 0.1, "each effect picked about half the time: " + gotA + " of " + got);
    }

    private static void endTick(Check c) {
        EffectParticles.worldTick(new TickEvent.WorldTickEvent(Side.SERVER, TickEvent.Phase.END, c.world));
    }

    private static boolean contains(int[] colors, int color) {
        for (int c : colors) {
            if (c == color) {
                return true;
            }
        }
        return false;
    }
}
