package com.exiledradio.trailsandtalesbackport.selftest;

import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.asm.Asm;
import com.exiledradio.trailsandtalesbackport.biome.CherryGroveContent;
import com.exiledradio.trailsandtalesbackport.misc.DeathMessages;
import com.exiledradio.trailsandtalesbackport.misc.StepSounds;
import com.exiledradio.trailsandtalesbackport.music.BiomeMusic;
import com.exiledradio.trailsandtalesbackport.sign.SignData;
import com.exiledradio.trailsandtalesbackport.sign.SignNetwork;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.IScoreCriteria;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentScore;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.border.WorldBorder;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.List;

/**
 * {@code -Pselftest=leftovers}: the items 1.0.0 had left out, on a server: 1.20's biome music table, sound lists and
 * biome mapping; the death messages; the step sounds (patch P2); the back face's selectors and scores; the wax-on
 * packet. The client side (portal screens, pick block, particles, arm poses, the music step) is
 * {@code runClient -Pselftest=client}.
 */
public final class LeftoversSelfTest implements SelfTest {

    @Override
    public String name() {
        return "leftovers";
    }

    @Override
    public void run(Check c) throws Exception {
        music(c);
        deathMessages(c);
        stepSounds(c);
        signs(c);
    }

    private static void music(Check c) {
        c.ok(BiomeMusic.enabled(), "biome music is on");
        c.eq(17, BiomeMusic.lists().size(), "1.20.6's 17 music biomes");
        List<SoundEvent> events = BiomeMusic.events();
        c.eq(17, events.size(), "17 events registered (the grove's with the biome)");
        for (SoundEvent e : events) {
            c.ok(ForgeRegistries.SOUND_EVENTS.containsValue(e), "registered: " + e.getRegistryName());
        }
        c.eq(new ResourceLocation(TrailsAndTalesBackport.MOD_ID, "music.overworld.desert"), BiomeMusic.event("desert").getRegistryName(), "desert event");
        c.eq("desert", BiomeMusic.musicOf(Biomes.DESERT), "desert");
        c.eq("desert", BiomeMusic.musicOf(Biomes.MUTATED_DESERT), "desert M");
        c.eq("badlands", BiomeMusic.musicOf(Biomes.MESA_ROCK), "wooded badlands");
        c.eq("forest", BiomeMusic.musicOf(Biomes.ROOFED_FOREST), "dark forest plays the forest's");
        c.eq("forest", BiomeMusic.musicOf(Biomes.BIRCH_FOREST), "birch forest plays the forest's");
        c.eq("flower_forest", BiomeMusic.musicOf(Biomes.MUTATED_FOREST), "flower forest");
        c.eq("old_growth_taiga", BiomeMusic.musicOf(Biomes.MUTATED_REDWOOD_TAIGA), "old growth spruce taiga");
        c.eq("jungle", BiomeMusic.musicOf(Biomes.JUNGLE_HILLS), "jungle hills");
        c.eq("sparse_jungle", BiomeMusic.musicOf(Biomes.JUNGLE_EDGE), "sparse jungle");
        c.eq(null, BiomeMusic.musicOf(Biomes.PLAINS), "plains: the game music");
        c.eq(null, BiomeMusic.musicOf(Biomes.TAIGA), "taiga: the game music");
        c.eq(null, BiomeMusic.musicOf(Biomes.SWAMPLAND), "swamp: Wild Update's");
        if (CherryGroveContent.biome != null) {
            c.eq("cherry_grove", BiomeMusic.musicOf(CherryGroveContent.biome), "cherry grove");
        }
        c.eq("minecraft:music/game/calm2", BiomeMusic.file("clark"), "Clark is 1.12.2's calm2");
        c.eq("minecraft:music/game/piano3", BiomeMusic.file("mice_on_venus"), "Mice on Venus is piano3");
        c.eq(TrailsAndTalesBackport.MOD_ID + ":music/game/bromeliad", BiomeMusic.file("bromeliad"), "Bromeliad is ours");
        boolean cc = BiomeMusic.class.getResource("/assets/cavesandcliffsbackport/sounds/music/game/stand_tall.ogg") != null;
        c.eq(cc ? "cavesandcliffsbackport:music/game/stand_tall" : null, BiomeMusic.file("stand_tall"), "Stand Tall only with Caves & Cliffs");
        boolean wu = BiomeMusic.class.getResource("/assets/wildupdatebackport/sounds/music/game/swamp/aerie.ogg") != null;
        c.eq(wu ? "wildupdatebackport:music/game/swamp/aerie" : null, BiomeMusic.file("swamp/aerie"), "Aerie only with Wild Update");

        JsonObject json = new JsonParser().parse(BiomeMusic.soundsJson()).getAsJsonObject();
        c.eq(17, json.entrySet().size(), "the sound lists name 17 events");
        for (java.util.Map.Entry<String, List<BiomeMusic.Entry>> list : BiomeMusic.lists().entrySet()) {
            int present = 0;
            for (BiomeMusic.Entry e : list.getValue()) {
                if (BiomeMusic.file(e.track) != null) {
                    present++;
                }
            }
            c.eq(present, json.getAsJsonObject("music.overworld." + list.getKey()).getAsJsonArray("sounds").size(),
                    list.getKey() + ": every installed track and none missing");
        }
        JsonObject first = json.getAsJsonObject("music.overworld.cherry_grove").getAsJsonArray("sounds").get(0).getAsJsonObject();
        c.eq(TrailsAndTalesBackport.MOD_ID + ":music/game/bromeliad", first.get("name").getAsString(), "grove: Bromeliad first");
        c.eq(3, first.get("weight").getAsInt(), "grove: Bromeliad weight 3");
        c.eq(0.4F, first.get("volume").getAsFloat(), "grove: Bromeliad volume 0.4");
        c.ok(first.get("stream").getAsBoolean(), "grove: streamed");
    }

    private static void deathMessages(Check c) {
        c.ok(DeathMessages.enabled(), "death messages are on");
        EntityZombie zombie = new EntityZombie(c.world);
        BlockPos at = c.area();
        zombie.setPosition(at.getX() + 0.5D, at.getY() + 1, at.getZ() + 0.5D);
        c.world.spawnEntity(zombie);
        DeathWatch watch = new DeathWatch(zombie);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(watch);
        try {
            zombie.onKillCommand();
        } finally {
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(watch);
        }
        // Read as the death happens: onDeath clears the tracker after the message is made.
        ITextComponent message = watch.message;
        c.ok(message instanceof TextComponentTranslation && "death.attack.genericKill".equals(((TextComponentTranslation) message).getKey()),
                "/kill: was killed (" + key(message) + ")");
        c.ok(zombie.isDead || zombie.getHealth() <= 0.0F, "/kill still kills");

        EntityZombie fall = new EntityZombie(c.world);
        fall.setPosition(at.getX() + 0.5D, at.getY() + 1, at.getZ() + 0.5D);
        c.world.spawnEntity(fall);
        fall.attackEntityFrom(DamageSource.OUT_OF_WORLD, 4.0F);
        c.eq("death.attack.outOfWorld", key(fall.getCombatTracker().getDeathMessage()), "void damage keeps fell out of the world");
        fall.setDead();

        FakePlayer player = c.player("border", at.up());
        WorldBorder border = c.world.getWorldBorder();
        double size = border.getDiameter();
        double cx = border.getCenterX();
        double cz = border.getCenterZ();
        double buffer = border.getDamageBuffer();
        double per = border.getDamageAmount();
        try {
            border.setCenter(at.getX() + 0.5D, at.getZ() + 0.5D);
            border.setTransition(2.0D);
            border.setDamageBuffer(0.0D);
            border.setDamageAmount(0.2D);
            player.setPosition(at.getX() + 20.5D, at.getY() + 1, at.getZ() + 0.5D);
            float amount = (float) Math.max(1, Math.floor(-(border.getClosestDistance(player)) * 0.2D));
            c.ok(DeathMessages.replacement(player, DamageSource.IN_WALL, amount) == DeathMessages.OUTSIDE_BORDER,
                    "the border's hit is recorded as outsideBorder (" + amount + ")");
            c.ok(DeathMessages.replacement(player, DamageSource.IN_WALL, amount + 1.0F) == null, "another amount stays inWall");
            player.setPosition(at.getX() + 0.5D, at.getY() + 1, at.getZ() + 0.5D);
            c.ok(DeathMessages.replacement(player, DamageSource.IN_WALL, 1.0F) == null, "inside the border: inWall");
        } finally {
            border.setCenter(cx, cz);
            border.setTransition(size);
            border.setDamageBuffer(buffer);
            border.setDamageAmount(per);
        }
        c.ok(DeathMessages.replacement(player, DamageSource.OUT_OF_WORLD, 4.0F) == null, "void damage is not /kill");
    }

    /** The death message of one entity, read in LivingDeathEvent (before onDeath clears the combat tracker). */
    public static final class DeathWatch {
        private final net.minecraft.entity.EntityLivingBase entity;
        ITextComponent message;

        DeathWatch(net.minecraft.entity.EntityLivingBase entity) {
            this.entity = entity;
        }

        @net.minecraftforge.fml.common.eventhandler.SubscribeEvent(priority = net.minecraftforge.fml.common.eventhandler.EventPriority.LOWEST)
        public void death(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
            if (event.getEntityLiving() == this.entity) {
                this.message = this.entity.getCombatTracker().getDeathMessage();
            }
        }
    }

    private static String key(ITextComponent message) {
        return message instanceof TextComponentTranslation ? ((TextComponentTranslation) message).getKey() : String.valueOf(message);
    }

    private static void stepSounds(Check c) {
        c.ok(StepSounds.enabled(), "step sounds are on");
        boolean p2 = false;
        for (String applied : Asm.APPLIED) {
            p2 |= applied.startsWith("P2 ");
        }
        c.ok(p2, "P2 is applied (Entity.move): " + Asm.APPLIED);
        c.ok(StepSounds.isCombination(Blocks.CARPET), "carpet is a combination block");
        c.ok(StepSounds.isCombination(Blocks.SNOW_LAYER), "snow layers are");
        c.ok(!StepSounds.isCombination(Blocks.SNOW), "the snow block is not");
        c.ok(StepSounds.isInside(Blocks.WATERLILY), "a lily pad is an inside block");
        c.ok(!StepSounds.isCombination(Blocks.STONE) && !StepSounds.isInside(Blocks.STONE), "stone is neither");
        BlockPos at = c.area().up(3);
        c.world.setBlockState(at, Blocks.STONE.getDefaultState());
        c.world.setBlockState(at.up(), Blocks.CARPET.getDefaultState());
        FakePlayer player = c.player("steps", at.up());
        StepSounds.lastCombination = null;
        StepSounds.playStepSound(player, at, Blocks.STONE);
        c.eq("minecraft:carpet>minecraft:stone", StepSounds.lastCombination, "carpet on stone plays both");
        c.world.setBlockToAir(at.up());
        StepSounds.lastCombination = null;
        EntityZombie zombie = new EntityZombie(c.world);
        c.world.setBlockState(at.up(), Blocks.CARPET.getDefaultState());
        StepSounds.playStepSound(zombie, at, Blocks.STONE);
        c.eq(null, StepSounds.lastCombination, "a zombie plays its own step sound (no combination)");
        c.world.setBlockToAir(at.up());
        c.world.setBlockToAir(at);
    }

    private static void signs(Check c) throws Exception {
        c.eq(32, SignNetwork.WAX_ON, "the wax-on packet is id 32");
        Scoreboard board = c.world.getScoreboard();
        ScoreObjective objective = board.getObjective("ttbtest");
        if (objective == null) {
            objective = board.addScoreObjective("ttbtest", IScoreCriteria.DUMMY);
        }
        board.getOrCreateScore("Alex", objective).setScorePoints(7);
        BlockPos at = c.area().up(6);
        c.world.setBlockState(at, Blocks.STANDING_SIGN.getDefaultState());
        TileEntity tile = c.world.getTileEntity(at);
        c.ok(tile instanceof TileEntitySign, "a sign tile");
        if (!(tile instanceof TileEntitySign)) {
            return;
        }
        SignData data = tile.getCapability(SignData.CAP, null);
        c.ok(data != null, "the sign has its 1.20 data");
        if (data == null) {
            return;
        }
        data.back[0] = new TextComponentScore("Alex", "ttbtest");
        NBTTagCompound saved = tile.writeToNBT(new NBTTagCompound());
        int before = SignData.resolvedLoads;
        TileEntity loaded = TileEntity.create(c.world, saved);
        c.ok(SignData.resolvedLoads > before, "the back face is resolved as the sign loads");
        SignData back = loaded == null ? null : loaded.getCapability(SignData.CAP, null);
        c.eq("7", back == null ? null : back.back[0].getUnformattedText(), "the back's score reads 7");
        board.removeObjective(objective);
        c.world.setBlockToAir(at);
    }
}
