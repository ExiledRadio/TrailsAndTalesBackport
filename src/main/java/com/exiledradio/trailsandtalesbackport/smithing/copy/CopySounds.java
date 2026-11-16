package com.exiledradio.trailsandtalesbackport.smithing.copy;

import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.common.Content;

import net.minecraft.block.SoundType;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The copies' sound events (as Caves &amp; Cliffs' {@code ModSounds}, under our id): made while the blocks register,
 * registered in the sound event, which comes after the block and item events.
 */
@Mod.EventBusSubscriber(modid = TrailsAndTalesBackport.MOD_ID)
public final class CopySounds {

    private static final Map<String, SoundEvent> EVENTS = new LinkedHashMap<String, SoundEvent>();
    private static final List<SoundEvent> ORDER = new ArrayList<SoundEvent>();

    private CopySounds() {
    }

    static synchronized SoundEvent event(String name) {
        SoundEvent e = EVENTS.get(name);
        if (e == null) {
            e = new SoundEvent(Content.id(name));
            e.setRegistryName(Content.id(name));
            EVENTS.put(name, e);
            ORDER.add(e);
        }
        return e;
    }

    /** A block sound type from {@code <group>.break/.step/.place/.hit/.fall}. */
    static SoundType type(String group) {
        return new SoundType(1.0F, 1.0F, event(group + ".break"), event(group + ".step"), event(group + ".place"),
                event(group + ".hit"), event(group + ".fall"));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static synchronized void register(RegistryEvent.Register<SoundEvent> event) {
        for (SoundEvent e : ORDER) {
            event.getRegistry().register(e);
        }
    }
}
