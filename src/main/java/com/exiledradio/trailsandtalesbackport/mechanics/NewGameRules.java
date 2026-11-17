package com.exiledradio.trailsandtalesbackport.mechanics;

import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Random;

/**
 * The game rules 1.20.2 to 1.20.5 added, where 1.12.2 has the behavior they control, and 1.20.2's
 * {@code randomTickSpeed} for snow and ice. Added to each world's rules when it loads (an existing value is kept);
 * with the setting off they are not added and every hook gives 1.12.2's answer.
 * <ul>
 *   <li>{@code enderPearlsVanishOnDeath} (1.20.2, true): P17. False keeps a dead player's thrown pearls flying.</li>
 *   <li>{@code projectilesCanBreakBlocks} (1.20.3, true): whether a projectile shatters a decorated pot (the only
 *       block it breaks in this mod; other backports may read it with {@link #projectilesCanBreakBlocks}).</li>
 *   <li>{@code playersNetherPortalDefaultDelay} (1.20.3, 80) and {@code playersNetherPortalCreativeDelay} (1.20.3, 0):
 *       P18, the ticks a player stands in a portal before it takes them, at least 1.</li>
 *   <li>{@code spawnChunkRadius} (1.20.5, 2, 0 to 32): the spawn chunks' radius ({@link SpawnChunks}), read from the
 *       overworld's rules.</li>
 *   <li>{@code randomTickSpeed} (1.20.2, 23w31a): P19. 1.20.2's {@code ServerLevel.tickChunk} tries
 *       {@code randomTickSpeed} times per chunk, each with a 1 in 48 chance (1.12.2: once, 1 in 16, whatever the
 *       rule), to freeze water, lay snow or fill a cauldron.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TrailsAndTalesBackport.MOD_ID)
public final class NewGameRules {

    public static final String PEARLS_VANISH = "enderPearlsVanishOnDeath";
    public static final String PROJECTILES_BREAK = "projectilesCanBreakBlocks";
    public static final String PORTAL_DELAY = "playersNetherPortalDefaultDelay";
    public static final String PORTAL_CREATIVE_DELAY = "playersNetherPortalCreativeDelay";
    public static final String SPAWN_CHUNK_RADIUS = "spawnChunkRadius";

    private NewGameRules() {
    }

    public static boolean enabled() {
        return ModConfig.mechanics.gameRules;
    }

    @SubscribeEvent
    public static void worldLoad(WorldEvent.Load event) {
        if (!event.getWorld().isRemote && enabled()) {
            add(event.getWorld().getGameRules());
        }
    }

    /** Adds the rules a world's rules lack, at 1.20.5's defaults. */
    public static void add(GameRules rules) {
        add(rules, PEARLS_VANISH, "true", GameRules.ValueType.BOOLEAN_VALUE);
        add(rules, PROJECTILES_BREAK, "true", GameRules.ValueType.BOOLEAN_VALUE);
        add(rules, PORTAL_DELAY, "80", GameRules.ValueType.NUMERICAL_VALUE);
        add(rules, PORTAL_CREATIVE_DELAY, "0", GameRules.ValueType.NUMERICAL_VALUE);
        add(rules, SPAWN_CHUNK_RADIUS, "2", GameRules.ValueType.NUMERICAL_VALUE);
    }

    private static void add(GameRules rules, String name, String value, GameRules.ValueType type) {
        if (!rules.hasRule(name)) {
            rules.addGameRule(name, value, type);
        }
    }

    private static boolean bool(World world, String rule, boolean otherwise) {
        if (!enabled() || world == null || !world.getGameRules().hasRule(rule)) {
            return otherwise;
        }
        return world.getGameRules().getBoolean(rule);
    }

    private static int number(World world, String rule, int otherwise) {
        if (!enabled() || world == null || !world.getGameRules().hasRule(rule)) {
            return otherwise;
        }
        return world.getGameRules().getInt(rule);
    }

    /**
     * P17, {@code EntityEnderPearl.onUpdate}'s {@code !thrower.isEntityAlive()}: a dead player's pearl vanishes only
     * while {@code enderPearlsVanishOnDeath} is true (1.20.2's {@code ThrownEnderpearl.tick}).
     */
    public static boolean pearlOwnerAlive(EntityLivingBase thrower) {
        return thrower.isEntityAlive() || !bool(thrower.world, PEARLS_VANISH, true);
    }

    /** {@code projectilesCanBreakBlocks}: true without the setting. */
    public static boolean projectilesCanBreakBlocks(World world) {
        return bool(world, PROJECTILES_BREAK, true);
    }

    /**
     * P18, the body of {@code EntityPlayer.getMaxInPortalTime}: 1.20.3's {@code Player.getPortalWaitTime},
     * {@code max(1, rule)} by whether the player is invulnerable (1.12.2: 1 or 80).
     */
    public static int portalTime(EntityPlayer player) {
        boolean invulnerable = player.capabilities.disableDamage;
        if (!enabled() || !player.world.getGameRules().hasRule(PORTAL_DELAY)) {
            return invulnerable ? 1 : 80;
        }
        return Math.max(1, number(player.world, invulnerable ? PORTAL_CREATIVE_DELAY : PORTAL_DELAY, invulnerable ? 0 : 80));
    }

    /** The spawn chunks' radius from the overworld's {@code spawnChunkRadius}, or {@code fallback} without the rule. */
    public static int spawnChunkRadius(int fallback) {
        if (!enabled()) {
            return fallback;
        }
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        WorldServer overworld = server == null ? null : server.getWorld(0);
        if (overworld == null || !overworld.getGameRules().hasRule(SPAWN_CHUNK_RADIUS)) {
            return fallback;
        }
        return Math.max(0, Math.min(32, overworld.getGameRules().getInt(SPAWN_CHUNK_RADIUS)));
    }

    /**
     * P19, {@code WorldServer.updateBlocks}'s {@code this.rand.nextInt(16)} that decides a chunk's snow and ice try:
     * 0 means try. With the setting, 0 comes back with 1.20.2's chance of at least one of {@code randomTickSpeed}
     * tries at 1 in 48 (3: 6.1%, 1.12.2: 6.25%; 0: never). At most one try per chunk per tick, as 1.12.2's code has
     * one.
     */
    public static int precipitationRoll(Random random, int bound, World world) {
        if (!enabled()) {
            return random.nextInt(bound);
        }
        int speed = world.getGameRules().getInt("randomTickSpeed");
        if (speed <= 0) {
            return 1;
        }
        double chance = 1.0D - Math.pow(47.0D / 48.0D, speed);
        return random.nextDouble() < chance ? 0 : 1;
    }
}
