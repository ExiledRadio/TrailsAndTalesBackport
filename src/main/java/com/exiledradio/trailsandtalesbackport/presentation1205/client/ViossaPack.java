package com.exiledradio.trailsandtalesbackport.presentation1205.client;

import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.presentation1205.Presentation1205;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.Language;
import net.minecraft.client.resources.LanguageManager;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.LanguageMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 1.20.5's Viossa ({@code vp_vl}) in the language list: 1.12.2 builds the list from every default and selected
 * resource pack's {@code pack.mcmeta} "language" section, so this pack (no files, in the client's default packs)
 * answers that section with 1.20.6's entry ("Viossa", region "Vilant", left to right). The text itself is in this
 * mod's {@code lang/vp_vl.lang} and, for vanilla's keys, {@code vanilla_text_1205/vp_vl.lang}.
 *
 * <p>With the option off, a game that was set to Viossa reads English, as a version without Viossa would.
 */
@SideOnly(Side.CLIENT)
public final class ViossaPack implements IResourcePack {

    public static final String CODE = "vp_vl";
    /** 1.20.6's pack.mcmeta entry. */
    public static final Language VIOSSA = new Language(CODE, "Vilant", "Viossa", false);

    private static boolean installed;

    @SuppressWarnings("unchecked")
    public static void install(Minecraft mc) {
        LanguageManager languages = mc.getLanguageManager();
        try {
            Map<String, Language> map = (Map<String, Language>) ReflectionHelper.findField(LanguageManager.class,
                    "languageMap", "field_135046_d").get(languages);
            if (!Presentation1205.viossa()) {
                if (CODE.equals(mc.gameSettings.language)) {
                    mc.gameSettings.language = "en_us";
                    languages.setCurrentLanguage(map.get("en_us"));
                }
                return;
            }
            List<IResourcePack> packs = (List<IResourcePack>) ReflectionHelper.findField(Minecraft.class,
                    "defaultResourcePacks", "field_110449_ao").get(mc);
            if (!installed) {
                packs.add(new ViossaPack());
                installed = true;
            }
            // The startup resource load already built the list; later reloads read this pack.
            if (!map.containsKey(CODE)) {
                map.put(CODE, VIOSSA);
            }
        } catch (Exception e) {
            TrailsAndTalesBackport.LOGGER.error("Viossa: could not add it to the language list", e);
        }
    }

    public static boolean isInstalled() {
        return installed;
    }

    @Override
    public InputStream getInputStream(ResourceLocation location) throws IOException {
        throw new FileNotFoundException(location.toString());
    }

    @Override
    public boolean resourceExists(ResourceLocation location) {
        return false;
    }

    @Override
    public Set<String> getResourceDomains() {
        return Collections.emptySet();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends IMetadataSection> T getPackMetadata(MetadataSerializer serializer, String section) {
        if ("language".equals(section) && Presentation1205.viossa()) {
            return (T) new LanguageMetadataSection(Collections.singletonList(VIOSSA));
        }
        return null;
    }

    @Override
    public BufferedImage getPackImage() throws IOException {
        throw new FileNotFoundException("pack.png");
    }

    @Override
    public String getPackName() {
        return TrailsAndTalesBackport.MOD_ID + ":viossa";
    }
}
