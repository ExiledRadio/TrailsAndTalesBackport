package com.exiledradio.trailsandtalesbackport.camel;

import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.common.Content;

import net.minecraft.util.SoundEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.registries.DataSerializerEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Stream Ca: camels.
 *
 * <ul>
 *   <li>Entity {@code camel}, id 10 of this mod's block 10-14, with its spawn egg (1.20's colors). Tracked
 *       80 blocks out, every 3 ticks, as 1.12.2 tracks horses.</li>
 *   <li>Its eleven 1.20 sounds; the saddle sound plays 1.12.2's horse saddle, as 1.20's camel saddle event does.</li>
 *   <li>The long data serializer the camel's pose tick needs ({@code trailsandtalesbackport:long}).</li>
 *   <li>Camels in desert villages ({@link CamelVillages}) and in <i>Two by Two</i> ({@link CamelAdvancements}).</li>
 * </ul>
 * Camels never spawn naturally, as in 1.20.
 */
@Mod.EventBusSubscriber(modid = TrailsAndTalesBackport.MOD_ID)
public final class CamelContent {

    public static final int CAMEL_ID = 10;
    /** 1.20's spawn egg colors. */
    public static final int EGG_PRIMARY = 16565097;
    public static final int EGG_SECONDARY = 13341495;

    private static final List<SoundEvent> SOUNDS = new ArrayList<SoundEvent>();
    public static final SoundEvent AMBIENT = sound("entity.camel.ambient");
    public static final SoundEvent DASH = sound("entity.camel.dash");
    public static final SoundEvent DASH_READY = sound("entity.camel.dash_ready");
    public static final SoundEvent DEATH = sound("entity.camel.death");
    public static final SoundEvent EAT = sound("entity.camel.eat");
    public static final SoundEvent HURT = sound("entity.camel.hurt");
    public static final SoundEvent SADDLE = sound("entity.camel.saddle");
    public static final SoundEvent SIT = sound("entity.camel.sit");
    public static final SoundEvent STAND = sound("entity.camel.stand");
    public static final SoundEvent STEP = sound("entity.camel.step");
    public static final SoundEvent STEP_SAND = sound("entity.camel.step_sand");

    private CamelContent() {
    }

    public static boolean enabled() {
        return ModConfig.camels.enabled;
    }

    private static SoundEvent sound(String name) {
        SoundEvent e = new SoundEvent(Content.id(name)).setRegistryName(Content.id(name));
        SOUNDS.add(e);
        return e;
    }

    /** Pre-init: the village and advancement hooks. */
    public static void preInit() {
        if (enabled()) {
            MinecraftForge.EVENT_BUS.register(new CamelVillages());
            MinecraftForge.EVENT_BUS.register(new CamelAdvancements());
        }
    }

    // ------------------------------------------------------------------ registry events

    /** Registered whether or not camels are on, so a world's synced entity data never misses it. */
    @SubscribeEvent
    public static void registerSerializers(RegistryEvent.Register<DataSerializerEntry> event) {
        event.getRegistry().register(new DataSerializerEntry(EntityCamel.LONG).setRegistryName(Content.id("long")));
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        if (!enabled()) {
            return;
        }
        event.getRegistry().register(EntityEntryBuilder.create().entity(EntityCamel.class)
                .id(Content.id("camel"), CAMEL_ID).name(TrailsAndTalesBackport.MOD_ID + ".camel")
                .tracker(80, 3, true).egg(EGG_PRIMARY, EGG_SECONDARY).build());
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        if (!enabled()) {
            return;
        }
        for (SoundEvent s : SOUNDS) {
            event.getRegistry().register(s);
        }
    }
}
