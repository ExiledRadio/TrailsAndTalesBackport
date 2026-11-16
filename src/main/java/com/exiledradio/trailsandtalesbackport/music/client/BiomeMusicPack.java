package com.exiledradio.trailsandtalesbackport.music.client;

import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.music.BiomeMusic;
import com.google.common.collect.ImmutableSet;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * A built-in pack serving one more {@code trailsandtalesbackport:sounds.json}, which the sound handler merges with
 * the mod's own: the music biomes' lists, written when read from the tracks installed ({@link BiomeMusic#soundsJson}),
 * so a list never names a file that is not there.
 */
@SideOnly(Side.CLIENT)
public final class BiomeMusicPack implements IResourcePack {

    /** For the client self-test. */
    public static volatile boolean installed;

    static void install() {
        installed = BiomeMusicClient.addPack(new BiomeMusicPack());
    }

    @Override
    public InputStream getInputStream(ResourceLocation location) throws IOException {
        if (!resourceExists(location)) {
            throw new FileNotFoundException(location.toString());
        }
        return new ByteArrayInputStream(BiomeMusic.soundsJson().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public boolean resourceExists(ResourceLocation location) {
        return TrailsAndTalesBackport.MOD_ID.equals(location.getNamespace()) && "sounds.json".equals(location.getPath());
    }

    @Override
    public Set<String> getResourceDomains() {
        return ImmutableSet.of(TrailsAndTalesBackport.MOD_ID);
    }

    @Nullable
    @Override
    public <T extends IMetadataSection> T getPackMetadata(MetadataSerializer serializer, String section) {
        return null;
    }

    @Override
    public BufferedImage getPackImage() throws IOException {
        throw new FileNotFoundException("pack.png");
    }

    @Override
    public String getPackName() {
        return TrailsAndTalesBackport.MOD_ID + ":biome_music";
    }
}
