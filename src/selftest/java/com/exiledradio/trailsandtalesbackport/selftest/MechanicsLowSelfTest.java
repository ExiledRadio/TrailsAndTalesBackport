package com.exiledradio.trailsandtalesbackport.selftest;

import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.asm.Asm;
import com.exiledradio.trailsandtalesbackport.mechanics.BlockSounds;
import com.exiledradio.trailsandtalesbackport.mechanics.EnchantingTables;
import com.exiledradio.trailsandtalesbackport.mechanics.NewGameRules;
import com.exiledradio.trailsandtalesbackport.mechanics.SmallTweaks;
import com.exiledradio.trailsandtalesbackport.mechanics.SpawnChunks;
import net.minecraft.block.BlockSponge;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.ContainerEnchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.IWorldEventListener;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.EnderTeleportEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * {@code -Pselftest=mechanicslow}: the smaller 1.20.x changes. Enchanting through replaceable blocks (P13) through a
 * real {@code ContainerEnchantment}; hoppers under full blocks (P14) through {@code pullItems}; the block sound swaps
 * and the sponge's sounds (P15); barrier landings (P16); the game rules: ender pearls of dead players (P17), portal
 * delays (P18), snow and ice by {@code randomTickSpeed} (P19), {@code spawnChunkRadius}; the pearl's landing sound;
 * Wither's color. Each also with its setting off, where 1.12.2's answer differs. (P20 and the bat are client-only.)
 */
public final class MechanicsLowSelfTest implements SelfTest {

    @Override
    public String name() {
        return "mechanicslow";
    }

    @Override
    public void run(Check c) throws Exception {
        enchanting(c); // loads ContainerEnchantment
        c.ok(applied("P13 ContainerEnchantment.onCraftMatrixChanged"), "P13 is applied to ContainerEnchantment: " + Asm.APPLIED);
        c.ok(applied("P14 TileEntityHopper.pullItems"), "P14 is applied");
        hoppers(c);
        c.ok(applied("P15 BlockSponge.getSoundType"), "P15 adds BlockSponge.getSoundType");
        c.ok(applied("P15 BlockSponge.tryAbsorb"), "P15 is applied to BlockSponge.tryAbsorb");
        sounds(c);
        c.ok(applied("P16 BlockBarrier.addLandingEffects"), "P16 is applied");
        barrier(c);
        c.ok(applied("P17 EntityEnderPearl.onUpdate"), "P17 is applied");
        c.ok(applied("P18 EntityPlayer.getMaxInPortalTime"), "P18 is applied");
        c.ok(applied("P19 WorldServer.updateBlocks"), "P19 is applied");
        gameRules(c);
        pearlSound(c);
        wither(c);
        languages(c);
    }

    // ---- the settings' names -----------------------------------------------------------------------------------

    /** The 20 languages with their own translations; Viossa (vp_vl) translates vanilla's text only, as 1.20's does. */
    static final String[] LANGUAGES = {"de_de", "en_us", "es_ar", "es_es", "es_mx", "es_uy", "es_ve", "fr_fr", "it_it",
            "ja_jp", "ko_kr", "nl_nl", "pl_pl", "pt_br", "ru_ru", "tr_tr", "uk_ua", "zh_cn", "zh_tw"};

    /** Every Mechanics setting, and the page itself, is named (with its tooltip) in every translated language. */
    private static void languages(Check c) throws Exception {
        java.util.List<String> keys = new java.util.ArrayList<String>();
        keys.add("config.trailsandtalesbackport.mechanics");
        for (java.lang.reflect.Field f : com.exiledradio.trailsandtalesbackport.ModConfig.Mechanics.class.getFields()) {
            net.minecraftforge.common.config.Config.LangKey key = f.getAnnotation(net.minecraftforge.common.config.Config.LangKey.class);
            c.ok(key != null, "the Mechanics setting " + f.getName() + " has a lang key");
            if (key != null) {
                keys.add(key.value());
            }
        }
        c.ok(keys.size() > 10, "the Mechanics page has its settings: " + (keys.size() - 1));
        for (String lang : LANGUAGES) {
            java.util.Properties p = new java.util.Properties();
            java.io.InputStream in = MechanicsLowSelfTest.class.getResourceAsStream("/assets/trailsandtalesbackport/lang/" + lang + ".lang");
            c.ok(in != null, lang + ".lang is in the jar");
            if (in == null) {
                continue;
            }
            try {
                p.load(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
            } finally {
                in.close();
            }
            java.util.List<String> missing = new java.util.ArrayList<String>();
            for (String key : keys) {
                String name = p.getProperty(key);
                if (name == null || name.trim().isEmpty() || p.getProperty(key + ".tooltip") == null) {
                    missing.add(key);
                }
            }
            c.ok(missing.isEmpty(), lang + ": every Mechanics setting is named, with its tooltip" + (missing.isEmpty() ? "" : ", missing " + missing));
        }
    }

    private static boolean applied(String what) {
        return Asm.APPLIED.contains(what);
    }

    // ---- P13 -------------------------------------------------------------------------------------------------

    /** The third slot's level a real container offers for a diamond sword: 30 at full power, at most 8 at none. */
    private static int topLevel(Check c, BlockPos table) {
        EntityPlayerMP player = c.player("enchanting", table.up());
        ContainerEnchantment container = new ContainerEnchantment(player.inventory, c.world, table);
        container.tableInventory.setInventorySlotContents(0, new ItemStack(Items.DIAMOND_SWORD));
        return container.enchantLevels[2];
    }

    private static void enchanting(Check c) {
        BlockPos a = c.area();
        BlockPos table = a.add(8, 1, 8);
        c.fill(table, table, Blocks.ENCHANTING_TABLE.getDefaultState());
        // 1.20's 16 shelf places in both rows: 32 bookshelves, and tall grass, snow layers and vines in between.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 0; dy <= 1; dy++) {
                    if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
                        c.fill(table.add(dx, dy, dz), table.add(dx, dy, dz), Blocks.BOOKSHELF.getDefaultState());
                    } else if (dx != 0 || dz != 0) {
                        IBlockState gap = dy == 1 ? Blocks.VINE.getDefaultState()
                                : (dx + dz) % 2 == 0 ? Blocks.SNOW_LAYER.getDefaultState() : Blocks.TALLGRASS.getStateFromMeta(1);
                        c.fill(table.add(dx, dy, dz), table.add(dx, dy, dz), gap);
                    }
                }
            }
        }
        boolean was = ModConfig.mechanics.enchantingGaps;
        try {
            ModConfig.mechanics.enchantingGaps = true;
            c.eq(32.0F, EnchantingTables.power(0.0F, c.world, table), "1.20: 32 bookshelves count through grass, snow and vines");
            c.eq(30, topLevel(c, table), "1.20: the table offers level 30 through grass, snow and vines");
            ModConfig.mechanics.enchantingGaps = false;
            c.ok(topLevel(c, table) <= 8, "off (1.12.2): no bookshelf counts through them, the top level is at most 8");

            // 1.20's geometry: a shelf needs only the block halfway to it, at its own height. Stone at (1, 1, 1) blocks
            // just the shelf at (2, 1, 2); 1.12.2 lost the six shelves of that corner (both rows).
            c.fill(table.add(-1, 0, -1), table.add(1, 1, 1), Blocks.AIR.getDefaultState());
            c.fill(table, table, Blocks.ENCHANTING_TABLE.getDefaultState());
            c.fill(table.add(1, 1, 1), table.add(1, 1, 1), Blocks.STONE.getDefaultState());
            ModConfig.mechanics.enchantingGaps = true;
            c.eq(31.0F, EnchantingTables.power(0.0F, c.world, table), "1.20: stone at (1, 1, 1) blocks only the shelf at (2, 1, 2)");
            ModConfig.mechanics.enchantingGaps = false;
            c.eq(26.0F, EnchantingTables.power(26.0F, c.world, table), "off: the hook hands 1.12.2's power back");
        } finally {
            ModConfig.mechanics.enchantingGaps = was;
        }
    }

    // ---- P14 -------------------------------------------------------------------------------------------------

    private static boolean pulls(Check c, BlockPos hopper, double itemY) {
        EntityItem item = new EntityItem(c.world, hopper.getX() + 0.5D, itemY, hopper.getZ() + 0.5D, new ItemStack(Items.APPLE));
        item.setNoPickupDelay();
        c.world.spawnEntity(item);
        TileEntity te = c.world.getTileEntity(hopper);
        boolean pulled = TileEntityHopper.pullItems((TileEntityHopper) te);
        item.setDead();
        ((TileEntityHopper) te).clear();
        return pulled;
    }

    private static void hoppers(Check c) {
        BlockPos a = c.area();
        BlockPos hopper = a.add(4, 1, 4);
        c.world.setBlockState(hopper, Blocks.HOPPER.getDefaultState(), 2);
        boolean was = ModConfig.mechanics.hopperPickup;
        try {
            for (boolean on : new boolean[]{true, false}) {
                ModConfig.mechanics.hopperPickup = on;
                String mode = on ? "1.20.5: " : "off (1.12.2): ";
                c.fill(hopper.up(), hopper.up(), Blocks.AIR.getDefaultState());
                c.ok(pulls(c, hopper, hopper.getY() + 1.2D), mode + "a hopper with air above picks up an item there");
                c.fill(hopper.up(), hopper.up(), Blocks.STONE.getDefaultState());
                c.eq(!on, pulls(c, hopper, hopper.getY() + 1.2D), mode + "a hopper picks up an item inside the full block above");
                c.fill(hopper.up(), hopper.up(), Blocks.STONE_SLAB.getDefaultState());
                c.ok(pulls(c, hopper, hopper.getY() + 1.2D), mode + "a slab above does not stop it");
            }
            ModConfig.mechanics.hopperPickup = true;
            c.fill(hopper.up(), hopper.up(), Blocks.STONE.getDefaultState());
            EntityItem item = new EntityItem(c.world, hopper.getX() + 0.5D, hopper.getY() + 1.2D, hopper.getZ() + 0.5D, new ItemStack(Items.APPLE));
            c.world.spawnEntity(item);
            c.ok(!SmallTweaks.captureItems(c.world, hopper.getX() + 0.6D, hopper.getY() + 0.7D, hopper.getZ() + 0.5D).isEmpty(),
                    "a hopper minecart (off the block grid) still picks up items inside a full block");
            c.ok(SmallTweaks.captureItems(c.world, hopper.getX() + 0.5D, hopper.getY() + 0.5D, hopper.getZ() + 0.5D).isEmpty(),
                    "the hopper block itself does not");
            item.setDead();
        } finally {
            ModConfig.mechanics.hopperPickup = was;
        }
    }

    // ---- P15 -------------------------------------------------------------------------------------------------

    private static final class Sounds implements IWorldEventListener {
        final List<SoundEvent> heard = new ArrayList<SoundEvent>();

        @Override
        public void playSoundToAllNearExcept(@Nullable EntityPlayer player, SoundEvent sound, SoundCategory category,
                                             double x, double y, double z, float volume, float pitch) {
            this.heard.add(sound);
        }

        @Override public void notifyBlockUpdate(World w, BlockPos p, IBlockState o, IBlockState n, int f) { }
        @Override public void notifyLightSet(BlockPos p) { }
        @Override public void markBlockRangeForRenderUpdate(int a, int b, int c, int d, int e, int f) { }
        @Override public void playRecord(SoundEvent s, BlockPos p) { }
        @Override public void spawnParticle(int id, boolean i, double x, double y, double z, double a, double b, double c, int... p) { }
        @Override public void spawnParticle(int id, boolean i, boolean m, double x, double y, double z, double a, double b, double c, int... p) { }
        @Override public void onEntityAdded(Entity e) { }
        @Override public void onEntityRemoved(Entity e) { }
        @Override public void broadcastSound(int id, BlockPos p, int d) { }
        @Override public void playEvent(EntityPlayer pl, int t, BlockPos p, int d) { }
        @Override public void sendBlockBreakProgress(int id, BlockPos p, int prog) { }
    }

    private static SoundType sound(Check c, IBlockState state) {
        return state.getBlock().getSoundType(state, c.world, BlockPos.ORIGIN, null);
    }

    private static void sounds(Check c) {
        boolean was = ModConfig.mechanics.blockSounds;
        BlockPos a = c.area();
        Sounds heard = new Sounds();
        c.world.addEventListener(heard);
        try {
            for (boolean on : new boolean[]{true, false}) {
                ModConfig.mechanics.blockSounds = on;
                BlockSounds.apply();
                String mode = on ? "1.20.2: " : "off (1.12.2): ";
                c.eq(on ? SoundType.WOOD : SoundType.STONE, sound(c, Blocks.JUKEBOX.getDefaultState()), mode + "jukebox");
                c.eq(on ? SoundType.WOOD : SoundType.STONE, sound(c, Blocks.TRIPWIRE_HOOK.getDefaultState()), mode + "tripwire hook");
                c.eq(on ? SoundType.STONE : SoundType.WOOD, sound(c, Blocks.LEVER.getDefaultState()), mode + "lever");
                c.eq(on ? SoundType.STONE : SoundType.WOOD, sound(c, Blocks.UNPOWERED_REPEATER.getDefaultState()), mode + "repeater");
                c.eq(on ? SoundType.STONE : SoundType.WOOD, sound(c, Blocks.POWERED_COMPARATOR.getDefaultState()), mode + "comparator");
                c.eq(on ? BlockSounds.COBWEB : SoundType.STONE, sound(c, Blocks.WEB.getDefaultState()), mode + "cobweb (1.20.5)");
                IBlockState dry = Blocks.SPONGE.getDefaultState().withProperty(BlockSponge.WET, false);
                IBlockState wet = Blocks.SPONGE.getDefaultState().withProperty(BlockSponge.WET, true);
                c.eq(on ? BlockSounds.SPONGE : SoundType.PLANT, sound(c, dry), mode + "sponge");
                c.eq(on ? BlockSounds.WET_SPONGE : SoundType.PLANT, sound(c, wet), mode + "wet sponge");

                BlockPos sponge = a.add(on ? 4 : 10, 1, 4);
                c.fill(sponge.east(), sponge.east(), Blocks.WATER.getDefaultState());
                heard.heard.clear();
                c.world.setBlockState(sponge, dry, 3); // onBlockAdded soaks up the water
                c.eq(wet, c.world.getBlockState(sponge), mode + "a sponge next to water soaks it up");
                c.eq(on, heard.heard.contains(BlockSounds.SPONGE_ABSORB), mode + "and plays block.sponge.absorb");
            }
        } finally {
            c.world.removeEventListener(heard);
            ModConfig.mechanics.blockSounds = was;
            BlockSounds.apply();
        }
    }

    // ---- P16 -------------------------------------------------------------------------------------------------

    private static void barrier(Check c) {
        boolean was = ModConfig.mechanics.barrierParticles;
        EntityPlayerMP player = c.player("barrier", c.area().up());
        IBlockState b = Blocks.BARRIER.getDefaultState();
        try {
            ModConfig.mechanics.barrierParticles = true;
            c.ok(b.getBlock().addLandingEffects(b, c.world, BlockPos.ORIGIN, b, player, 10), "1.20.2: a barrier handles its landing (no dust)");
            ModConfig.mechanics.barrierParticles = false;
            c.ok(!b.getBlock().addLandingEffects(b, c.world, BlockPos.ORIGIN, b, player, 10), "off (1.12.2): the dust is drawn");
        } finally {
            ModConfig.mechanics.barrierParticles = was;
        }
    }

    // ---- game rules: P17, P18, P19, spawnChunkRadius, projectilesCanBreakBlocks ---------------------------

    private static void gameRules(Check c) {
        GameRules rules = c.world.getGameRules();
        for (String rule : new String[]{NewGameRules.PEARLS_VANISH, NewGameRules.PROJECTILES_BREAK, NewGameRules.PORTAL_DELAY,
                NewGameRules.PORTAL_CREATIVE_DELAY, NewGameRules.SPAWN_CHUNK_RADIUS}) {
            c.ok(rules.hasRule(rule), "the overworld has the game rule " + rule);
        }
        c.eq("2", rules.getString(NewGameRules.SPAWN_CHUNK_RADIUS), "spawnChunkRadius defaults to 2");
        boolean was = ModConfig.mechanics.gameRules;
        try {
            ModConfig.mechanics.gameRules = true;
            pearls(c, rules);
            portals(c, rules);
            precipitation(c, rules);
            spawnRadius(c, rules);
            rules.setOrCreateGameRule(NewGameRules.PROJECTILES_BREAK, "false");
            c.ok(!NewGameRules.projectilesCanBreakBlocks(c.world), "projectilesCanBreakBlocks false keeps pots whole");
            ModConfig.mechanics.gameRules = false;
            c.ok(NewGameRules.projectilesCanBreakBlocks(c.world), "off: projectiles always break pots");
        } finally {
            ModConfig.mechanics.gameRules = was;
            rules.setOrCreateGameRule(NewGameRules.PROJECTILES_BREAK, "true");
        }
    }

    private static void pearls(Check c, GameRules rules) {
        BlockPos a = c.area();
        for (String value : new String[]{"true", "false"}) {
            rules.setOrCreateGameRule(NewGameRules.PEARLS_VANISH, value);
            EntityPlayerMP owner = Players.real(c.world, "pearl" + value);
            owner.setPosition(a.getX() + 2.5D, a.getY() + 1, a.getZ() + 2.5D);
            owner.setHealth(0.0F);
            EntityEnderPearl pearl = new EntityEnderPearl(c.world, owner);
            pearl.setPosition(a.getX() + 8.5D, a.getY() + 20, a.getZ() + 8.5D);
            pearl.onUpdate();
            c.eq("true".equals(value), pearl.isDead, "enderPearlsVanishOnDeath " + value + ": a dead player's pearl vanishes");
        }
        rules.setOrCreateGameRule(NewGameRules.PEARLS_VANISH, "true");
    }

    private static void portals(Check c, GameRules rules) {
        EntityPlayerMP player = Players.real(c.world, "portal");
        c.eq(80, player.getMaxInPortalTime(), "a survival player waits 80 ticks in a portal");
        rules.setOrCreateGameRule(NewGameRules.PORTAL_DELAY, "40");
        c.eq(40, player.getMaxInPortalTime(), "playersNetherPortalDefaultDelay 40: 40 ticks");
        rules.setOrCreateGameRule(NewGameRules.PORTAL_DELAY, "0");
        c.eq(1, player.getMaxInPortalTime(), "playersNetherPortalDefaultDelay 0: at least 1 tick");
        player.capabilities.disableDamage = true;
        c.eq(1, player.getMaxInPortalTime(), "an invulnerable player: playersNetherPortalCreativeDelay 0, at least 1");
        rules.setOrCreateGameRule(NewGameRules.PORTAL_CREATIVE_DELAY, "30");
        c.eq(30, player.getMaxInPortalTime(), "playersNetherPortalCreativeDelay 30: 30 ticks");
        ModConfig.mechanics.gameRules = false;
        c.eq(1, player.getMaxInPortalTime(), "off (1.12.2): an invulnerable player waits 1 tick");
        player.capabilities.disableDamage = false;
        c.eq(80, player.getMaxInPortalTime(), "off (1.12.2): others 80");
        ModConfig.mechanics.gameRules = true;
        rules.setOrCreateGameRule(NewGameRules.PORTAL_DELAY, "80");
        rules.setOrCreateGameRule(NewGameRules.PORTAL_CREATIVE_DELAY, "0");
    }

    private static int tries(Check c, int n) {
        Random random = new Random(42L);
        int hits = 0;
        for (int i = 0; i < n; i++) {
            if (NewGameRules.precipitationRoll(random, 16, c.world) == 0) {
                hits++;
            }
        }
        return hits;
    }

    private static void precipitation(Check c, GameRules rules) {
        String speed = rules.getString("randomTickSpeed");
        try {
            rules.setOrCreateGameRule("randomTickSpeed", "0");
            c.eq(0, tries(c, 2000), "randomTickSpeed 0: no snow or ice tries");
            rules.setOrCreateGameRule("randomTickSpeed", "3");
            int three = tries(c, 20000);
            c.ok(three > 20000 * 0.05 && three < 20000 * 0.072, "randomTickSpeed 3: about 6.1% of chunk ticks try (" + three + ")");
            rules.setOrCreateGameRule("randomTickSpeed", "300");
            c.ok(tries(c, 2000) > 1990, "randomTickSpeed 300: nearly every chunk tick tries");
            ModConfig.mechanics.gameRules = false;
            int off = tries(c, 20000);
            c.ok(off > 20000 * 0.055 && off < 20000 * 0.07, "off (1.12.2): 1 in 16 whatever the rule (" + off + ")");
            ModConfig.mechanics.gameRules = true;
        } finally {
            rules.setOrCreateGameRule("randomTickSpeed", speed);
        }
    }

    private static void spawnRadius(Check c, GameRules rules) {
        int radius = SpawnChunks.radius;
        boolean spawnChunks = ModConfig.mechanics.spawnChunks;
        BlockPos spawn = c.world.getSpawnPoint();
        int sx = spawn.getX() >> 4;
        int sz = spawn.getZ() >> 4;
        try {
            ModConfig.mechanics.spawnChunks = true;
            c.eq(2, SpawnChunks.effectiveRadius(), "spawnChunkRadius 2: radius 2");
            rules.setOrCreateGameRule(NewGameRules.SPAWN_CHUNK_RADIUS, "4");
            c.eq(4, SpawnChunks.effectiveRadius(), "spawnChunkRadius 4: radius 4");
            c.ok(c.world.isSpawnChunk(sx + 4, sz), "and a chunk 4 away is a spawn chunk");
            c.ok(!c.world.isSpawnChunk(sx + 5, sz), "a chunk 5 away is not");
            rules.setOrCreateGameRule(NewGameRules.SPAWN_CHUNK_RADIUS, "0");
            c.ok(!c.world.isSpawnChunk(sx, sz), "spawnChunkRadius 0: none");
            rules.setOrCreateGameRule(NewGameRules.SPAWN_CHUNK_RADIUS, "4");
            SpawnChunks.radius = 0;
            c.eq(0, SpawnChunks.effectiveRadius(), "another backport's radius 0 wins over the rule");
            SpawnChunks.radius = radius;
            ModConfig.mechanics.gameRules = false;
            c.eq(radius, SpawnChunks.effectiveRadius(), "off: the radius is SpawnChunks.radius");
        } finally {
            ModConfig.mechanics.gameRules = true;
            SpawnChunks.radius = radius;
            ModConfig.mechanics.spawnChunks = spawnChunks;
            rules.setOrCreateGameRule(NewGameRules.SPAWN_CHUNK_RADIUS, "2");
        }
    }

    // ---- the ender pearl's sound ---------------------------------------------------------------------------

    private static void pearlSound(Check c) {
        boolean was = ModConfig.mechanics.pearlSound;
        BlockPos a = c.area();
        EntityPlayerMP player = Players.real(c.world, "pearlsound");
        player.setPosition(a.getX() + 2.5D, a.getY() + 1, a.getZ() + 2.5D);
        EntityEnderPearl pearl = new EntityEnderPearl(c.world, player);
        pearl.setPosition(a.getX() + 8.5D, a.getY() + 1, a.getZ() + 8.5D);
        c.world.spawnEntity(pearl);
        Sounds heard = new Sounds();
        c.world.addEventListener(heard);
        try {
            for (boolean on : new boolean[]{true, false}) {
                ModConfig.mechanics.pearlSound = on;
                heard.heard.clear();
                MinecraftForge.EVENT_BUS.post(new EnderTeleportEvent(player, pearl.posX, pearl.posY, pearl.posZ, 5.0F));
                c.eq(on, heard.heard.contains(BlockSounds.PLAYER_TELEPORT), (on ? "1.20.3" : "off (1.12.2)")
                        + ": a player's pearl landing plays entity.player.teleport");
            }
            ModConfig.mechanics.pearlSound = true;
            heard.heard.clear();
            MinecraftForge.EVENT_BUS.post(new EnderTeleportEvent(player, pearl.posX + 5.0D, pearl.posY, pearl.posZ, 0.0F));
            c.ok(!heard.heard.contains(BlockSounds.PLAYER_TELEPORT), "a teleport with no pearl there (chorus fruit) plays none");
        } finally {
            c.world.removeEventListener(heard);
            pearl.setDead();
            ModConfig.mechanics.pearlSound = was;
        }
    }

    // ---- Wither's color --------------------------------------------------------------------------------------

    private static void wither(Check c) {
        boolean was = ModConfig.mechanics.witherColor;
        try {
            ModConfig.mechanics.witherColor = true;
            BlockSounds.apply();
            c.eq(7561558, MobEffects.WITHER.getLiquidColor(), "1.20: Wither's color is 7561558");
            ModConfig.mechanics.witherColor = false;
            BlockSounds.apply();
            c.eq(3484199, MobEffects.WITHER.getLiquidColor(), "off (1.12.2): 3484199");
        } finally {
            ModConfig.mechanics.witherColor = was;
            BlockSounds.apply();
        }
    }
}
