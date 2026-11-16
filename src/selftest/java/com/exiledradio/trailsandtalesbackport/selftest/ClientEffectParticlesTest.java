package com.exiledradio.trailsandtalesbackport.selftest;

import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.asm.Asm;
import com.exiledradio.trailsandtalesbackport.effectparticles.EffectParticles;
import com.exiledradio.trailsandtalesbackport.effectparticles.client.EffectParticlesClient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * {@code runClient -Pselftest=liveeffects}: 1.20.5's effect particles on a real client. Opens a flat creative world,
 * gives the player speed and an ambient (beacon-style) regeneration and a pig nearby strength, then checks for 400
 * ticks: the lists arrived through the server's packets, patch P3 was applied, the particles drawn are each effect's
 * own color (never a blend) at 1.20.6's rate, and with the option off 1.12.2's swirl is back. Saves a third-person
 * frame to {@code eclipse/effectparticles/screenshots/particles.png}, then quits. Logs "CLIENT SELFTEST DONE pass= fail=".
 */
public final class ClientEffectParticlesTest {

    private static final String WORLD = "effectparticles-client-test";
    private static final int WATCH = 400;

    private int ticks;
    private int pass;
    private int fail;
    private int phase;
    private int drawnBefore;
    private int lastSeen;
    private final Set<Integer> seen = new HashSet<Integer>();
    private int pigId = -1;

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new ClientEffectParticlesTest());
        TrailsAndTalesBackport.LOGGER.info("CLIENT SELFTEST registered (effect particles)");
    }

    private void ok(boolean condition, String what) {
        if (condition) {
            this.pass++;
            TrailsAndTalesBackport.LOGGER.info("CLIENT SELFTEST ok {}", what);
        } else {
            this.fail++;
            TrailsAndTalesBackport.LOGGER.error("CLIENT SELFTEST FAIL {}", what);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.END || this.phase >= 9) {
            return;
        }
        try {
            if (this.phase == 0 && mc.currentScreen instanceof GuiMainMenu && ++this.ticks >= 100) {
                this.phase = 1;
                this.ticks = 0;
                // Nobody is at the window: an unfocused game would open the pause menu and stop the integrated server.
                mc.gameSettings.pauseOnLostFocus = false;
                // The integrated server would run the server self-tests and stop itself.
                System.clearProperty(TrailsAndTalesBackport.MOD_ID + ".selftest");
                FileUtils.deleteQuietly(new File(new File(mc.gameDir, "saves"), WORLD));
                WorldSettings settings = new WorldSettings(1L, GameType.CREATIVE, false, false, WorldType.FLAT);
                mc.launchIntegratedServer(WORLD, WORLD, settings);
            }
            if (this.phase >= 1 && mc.world != null && mc.currentScreen != null && mc.currentScreen.doesGuiPauseGame()) {
                mc.displayGuiScreen(null);
            }
            if (this.phase == 0) {
                return;
            } else if (this.phase == 1 && ++this.ticks > 2400) {
                ok(false, "the test world never opened");
                finish(mc);
            } else if (this.phase == 1 && mc.world != null && mc.player != null && this.ticks >= 60) {
                this.phase = 2;
                this.ticks = 0;
                giveEffects(mc);
            } else if (this.phase == 2 && ++this.ticks >= 40) {
                this.phase = 3;
                this.ticks = 0;
                arrived(mc);
                this.drawnBefore = EffectParticlesClient.drawn;
                this.lastSeen = EffectParticlesClient.drawn;
            } else if (this.phase == 3) {
                if (EffectParticlesClient.drawn != this.lastSeen) {
                    this.seen.add(EffectParticlesClient.lastColor);
                    this.lastSeen = EffectParticlesClient.drawn;
                }
                if (++this.ticks >= WATCH) {
                    this.phase = 4;
                    this.ticks = 0;
                    drew(mc);
                    mc.gameSettings.thirdPersonView = 2;
                }
            } else if (this.phase == 4 && ++this.ticks >= 10) {
                File dir = new File(mc.gameDir, "effectparticles");
                dir.mkdirs();
                ScreenShotHelper.saveScreenshot(dir, "particles.png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
                mc.gameSettings.thirdPersonView = 0;
                off(mc);
                finish(mc);
            }
        } catch (Throwable t) {
            TrailsAndTalesBackport.LOGGER.error("CLIENT SELFTEST threw", t);
            this.fail++;
            finish(mc);
        }
    }

    /** On the integrated server's thread: the effects, and a pig three blocks in front with its own. */
    private void giveEffects(Minecraft mc) {
        final IntegratedServer server = mc.getIntegratedServer();
        final String name = mc.player.getName();
        server.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                EntityPlayerMP player = server.getPlayerList().getPlayerByUsername(name);
                player.addPotionEffect(new PotionEffect(MobEffects.SPEED, 20000, 0, false, true));
                player.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 20000, 0, true, true));
                WorldServer world = (WorldServer) player.world;
                EntityPig pig = new EntityPig(world);
                pig.setNoAI(true);
                pig.setPosition(player.posX + 3, player.posY, player.posZ);
                world.spawnEntity(pig);
                pig.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 20000, 0, false, true));
                ClientEffectParticlesTest.this.pigId = pig.getEntityId();
            }
        });
    }

    private void arrived(Minecraft mc) {
        ok(Asm.APPLIED.contains("P3"), "patch P3 applied on the client: " + Asm.APPLIED);
        ok(EffectParticles.active(), "1.20.5's particles are active (option on, 1.20.5 shown)");
        EffectParticles.Synced own = EffectParticles.clientLists().get(mc.player.getEntityId());
        ok(own != null && own.colors.length == 2 && !own.ambient, "the player's own list arrived: " + own);
        if (own != null) {
            ok(contains(own.colors, 0xFF000000 | MobEffects.SPEED.getLiquidColor()), "speed in its own color, alpha 255");
            ok(contains(own.colors, 38 << 24 | MobEffects.REGENERATION.getLiquidColor()), "ambient regeneration at alpha 38");
        }
        EffectParticles.Synced pig = EffectParticles.clientLists().get(this.pigId);
        ok(pig != null && pig.colors.length == 1 && pig.colors[0] == (0xFF000000 | MobEffects.STRENGTH.getLiquidColor()),
                "the pig's list arrived when the player started tracking it or its effect changed: " + pig);
        ok(EffectParticles.effectColor(12345, mc.player) == 0, "1.12.2's blended swirl is replaced for the player");
    }

    private void drew(Minecraft mc) {
        int drawn = EffectParticlesClient.drawn - this.drawnBefore;
        // Player and pig: one in 4 each per tick, 200 expected over 400 ticks (the probe above may add one).
        ok(drawn >= 140 && drawn <= 270, "about one particle in 4 ticks per entity: " + drawn + " in " + WATCH + " ticks for 2 entities");
        Set<Integer> expected = new HashSet<Integer>();
        expected.add(0xFF000000 | MobEffects.SPEED.getLiquidColor());
        expected.add(38 << 24 | MobEffects.REGENERATION.getLiquidColor());
        expected.add(0xFF000000 | MobEffects.STRENGTH.getLiquidColor());
        ok(expected.containsAll(this.seen), "every particle is one effect's own color, never a blend: " + hex(this.seen));
        ok(this.seen.size() == 3, "all three effects were drawn: " + hex(this.seen));
    }

    private void off(Minecraft mc) {
        ModConfig.misc.effectParticles = false;
        ok(!EffectParticles.active(), "option off: inactive");
        ok(EffectParticles.effectColor(12345, mc.player) == 12345, "option off: 1.12.2's blended color is drawn again");
        ModConfig.misc.effectParticles = true;
    }

    private void finish(Minecraft mc) {
        this.phase = 9;
        TrailsAndTalesBackport.LOGGER.info("CLIENT SELFTEST DONE pass={} fail={}", this.pass, this.fail);
        mc.shutdown();
    }

    private static boolean contains(int[] colors, int color) {
        for (int c : colors) {
            if (c == color) {
                return true;
            }
        }
        return false;
    }

    private static String hex(Set<Integer> colors) {
        StringBuilder b = new StringBuilder();
        for (int c : colors) {
            b.append(String.format("%08x ", c));
        }
        return b.toString().trim();
    }
}
