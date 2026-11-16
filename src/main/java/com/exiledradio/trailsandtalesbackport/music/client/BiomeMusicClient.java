package com.exiledradio.trailsandtalesbackport.music.client;

import com.exiledradio.backportcore.presentation.client.GameMusic;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.music.BiomeMusic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Client side of 1.20's biome music: Backport Core's "1.20" music step, and the built-in pack that writes the
 * biomes' sound lists ({@link BiomeMusicPack}).
 *
 * <p>The step follows the last line of 1.20.6's {@code Minecraft.getSituationalMusic},
 * {@code biome.getBackgroundMusic().orElse(Musics.GAME)}: only where the game music would play (not flying in
 * creative, not under water, not in the Nether or the End or a dimension with its own music, all of which the steps
 * before it chose), the biome at the player's feet gives its music, 10 to 20 minutes apart and never cutting off a
 * track already playing ({@code Musics.createGameMusic}). Where Caves &amp; Cliffs' "1.18" step chose one of 1.18's
 * eight biomes, the same biome's 1.20 list plays instead.
 */
@SideOnly(Side.CLIENT)
public final class BiomeMusicClient {

    public static final int MIN_DELAY = 12000;
    public static final int MAX_DELAY = 24000;
    private static final String CC = "cavesandcliffsbackport";
    private static final String CC_PREFIX = "music.overworld.";

    /** For the client self-test: the last music biome the step chose, or null. */
    public static volatile String lastBiome;

    private BiomeMusicClient() {
    }

    /** From {@code ClientSetup.init}, in pre-init: before the sound handler first reads the sound lists. */
    public static void init() {
        if (!BiomeMusic.enabled()) {
            return;
        }
        BiomeMusicPack.install();
        GameMusic.add("1.20", "biome music", new GameMusic.Step() {
            @Override
            public void choose(GameMusic.Situation s) {
                GameMusic.Track track = pick(s);
                if (track != null) {
                    s.choose(track);
                }
            }
        });
    }

    /** @return 1.20's choice, or null to leave the choice as it is */
    @Nullable
    static GameMusic.Track pick(GameMusic.Situation s) {
        Minecraft mc = s.mc;
        lastBiome = null;
        if (mc.player == null || mc.world == null) {
            return null;
        }
        SoundEvent chosen = s.choice().event;
        String region = region18(chosen);
        boolean game = chosen == SoundEvents.MUSIC_GAME;
        if (!game && region == null) {
            return null;
        }
        SoundEvent underWater = GameMusic.underWater();
        if (underWater != null && s.isPlaying(underWater)) {
            return null;
        }
        String biome = region != null ? region : BiomeMusic.musicOf(mc.world.getBiome(new BlockPos(mc.player)));
        SoundEvent music = biome == null ? null : BiomeMusic.event(biome);
        if (music != null) {
            lastBiome = biome;
            return new GameMusic.Track(music, MIN_DELAY, MAX_DELAY, false);
        }
        if (game && playingOurs(s)) {
            // Walking out of the biome: 1.20's game music lets the playing track finish (1.12.2's cuts it off).
            return new GameMusic.Track(SoundEvents.MUSIC_GAME, MIN_DELAY, MAX_DELAY, false);
        }
        return null;
    }

    /** @return the 1.18 music biome Caves &amp; Cliffs chose ({@code cavesandcliffsbackport:music.overworld.<id>}), or null */
    @Nullable
    static String region18(@Nullable SoundEvent event) {
        if (event == null) {
            return null;
        }
        ResourceLocation name = event.getSoundName();
        if (!CC.equals(name.getNamespace()) || !name.getPath().startsWith(CC_PREFIX)) {
            return null;
        }
        String id = name.getPath().substring(CC_PREFIX.length());
        for (String r : BiomeMusic.REGIONS_1_18) {
            if (r.equals(id)) {
                return id;
            }
        }
        return null;
    }

    private static boolean playingOurs(GameMusic.Situation s) {
        List<SoundEvent> events = BiomeMusic.events();
        for (SoundEvent e : events) {
            if (s.isPlaying(e)) {
                return true;
            }
        }
        return false;
    }

    /** Adds a built-in pack for every later reload and to the live resource manager now. */
    static boolean addPack(IResourcePack pack) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            @SuppressWarnings("unchecked")
            List<IResourcePack> defaults = (List<IResourcePack>) ReflectionHelper.findField(Minecraft.class,
                    "defaultResourcePacks", "field_110449_ao").get(mc);
            defaults.add(pack);
            IReloadableResourceManager manager = (IReloadableResourceManager) mc.getResourceManager();
            if (manager instanceof SimpleReloadableResourceManager) {
                ((SimpleReloadableResourceManager) manager).reloadResourcePack(pack);
            }
            return true;
        } catch (Exception e) {
            TrailsAndTalesBackport.LOGGER.warn("Biome music: the sound lists could not be added; biomes keep the game music.", e);
            return false;
        }
    }
}
