package com.exiledradio.trailsandtalesbackport.lookfeel.client;

import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.google.common.collect.ImmutableSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

/**
 * 1.20 (23w17a) put A Familiar Room in {@code music.game}, the music of every overworld biome. A built-in pack
 * serves one file, {@code minecraft:sounds.json}, which the game merges with every other pack's: its {@code
 * music.game} lists the one track and replaces nothing, so 1.12.2's tracks and any other mod's or resource pack's
 * stay (and the creative music, which includes the game music, gains it too). The file sits outside {@code assets/}
 * so it is read only while {@code lookAndFeel.gameMusic} is on.
 */
@SideOnly(Side.CLIENT)
public final class GameMusicPack implements IResourcePack {

    static final String FILE = "/" + TrailsAndTalesBackport.MOD_ID + "_packs/game_music/assets/minecraft/sounds.json";

    /** For the client self-test. */
    public static volatile boolean installed;

    /** Adds the pack: for every later reload, and to the live resource manager now (before the sound handler reads). */
    static void install() {
        GameMusicPack pack = new GameMusicPack();
        try {
            if (GameMusicPack.class.getResource(FILE) == null) {
                throw new IllegalStateException(FILE + " is missing from the jar");
            }
            Minecraft mc = Minecraft.getMinecraft();
            @SuppressWarnings("unchecked")
            List<IResourcePack> defaults = (List<IResourcePack>) ReflectionHelper.findField(Minecraft.class, "defaultResourcePacks", "field_110449_ao").get(mc);
            defaults.add(pack);
            IReloadableResourceManager manager = (IReloadableResourceManager) mc.getResourceManager();
            if (manager instanceof SimpleReloadableResourceManager) {
                ((SimpleReloadableResourceManager) manager).reloadResourcePack(pack);
            }
            installed = true;
        } catch (Exception e) {
            TrailsAndTalesBackport.LOGGER.warn("Look and feel: A Familiar Room could not join the game music.", e);
        }
    }

    @Override
    public InputStream getInputStream(ResourceLocation location) throws IOException {
        InputStream in = resourceExists(location) ? GameMusicPack.class.getResourceAsStream(FILE) : null;
        if (in == null) {
            throw new FileNotFoundException(location.toString());
        }
        return in;
    }

    @Override
    public boolean resourceExists(ResourceLocation location) {
        return "minecraft".equals(location.getNamespace()) && "sounds.json".equals(location.getPath());
    }

    @Override
    public Set<String> getResourceDomains() {
        return ImmutableSet.of("minecraft");
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
        return TrailsAndTalesBackport.MOD_ID + ":game_music";
    }
}
