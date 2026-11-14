package com.exiledradio.trailsandtalesbackport.biome.client;

import com.exiledradio.backportcore.presentation.client.GameMusic;
import com.exiledradio.trailsandtalesbackport.biome.CherryGroveContent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MusicTicker;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;

/**
 * The cherry grove's music, through Backport Core's {@link GameMusic}: 1.20.6's
 * {@code Minecraft.getSituationalMusic} gives a biome's own music wherever the game music would play (in the
 * overworld, not flying in creative, and not while the underwater music plays), here 1.20's
 * {@code music.overworld.cherry_grove}: Bromeliad, Echo in the Wind (weight 3 each), Left to Bloom, Clark, Minecraft
 * and Sweden, 12000 to 24000 ticks apart, not cutting off a track already playing.
 */
public final class CherryGroveClient {

    public static final int MIN_DELAY = 12000;
    public static final int MAX_DELAY = 24000;

    private CherryGroveClient() {
    }

    /** From {@code ClientSetup.init}, in pre-init. */
    public static void init() {
        if (!CherryGroveContent.enabled()) {
            return;
        }
        GameMusic.add("1.20", "cherry grove music", new GameMusic.Step() {
            @Override
            public void choose(GameMusic.Situation s) {
                GameMusic.Track track = pick(s.mc, s.vanilla, s);
                if (track != null) {
                    s.choose(track);
                }
            }
        });
    }

    /** @return the grove's track where 1.20 plays it, else null (the choice stays as it is) */
    static GameMusic.Track pick(Minecraft mc, MusicTicker.MusicType vanilla, GameMusic.Situation s) {
        SoundEvent music = CherryGroveContent.music;
        if (music == null || mc.player == null || mc.world == null || vanilla != MusicTicker.MusicType.GAME) {
            return null;
        }
        SoundEvent underWater = GameMusic.underWater();
        if (underWater != null && s.isPlaying(underWater)) {
            return null;
        }
        if (mc.world.getBiome(new BlockPos(mc.player)) != CherryGroveContent.biome) {
            return null;
        }
        return new GameMusic.Track(music, MIN_DELAY, MAX_DELAY, false);
    }
}
