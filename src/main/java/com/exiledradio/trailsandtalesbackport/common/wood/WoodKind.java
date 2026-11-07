package com.exiledradio.trailsandtalesbackport.common.wood;

import com.exiledradio.trailsandtalesbackport.common.Content;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.util.SoundEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One wood of 1.20's: its name, its map colors and its sounds (1.20's {@code WoodType} and
 * {@code BlockSetType} together).
 *
 * <p>1.20 gave cherry and bamboo their own sounds for every wooden block, doors and buttons included.
 * The events are made here, eagerly, because a block takes its {@link SoundType} in its constructor and
 * 1.12.2 fires the sound registry event after the block one; the stream registers {@link #soundEvents()}
 * in its own sound event handler.
 */
public final class WoodKind {

    /** "cherry", "bamboo": the start of every block's name. */
    public final String name;

    public final MapColor planksColor;
    public final MapColor barkColor;

    public final SoundType sound;

    public final SoundEvent doorOpen;
    public final SoundEvent doorClose;
    public final SoundEvent trapdoorOpen;
    public final SoundEvent trapdoorClose;
    public final SoundEvent buttonOn;
    public final SoundEvent buttonOff;
    public final SoundEvent plateOn;
    public final SoundEvent plateOff;
    public final SoundEvent gateOpen;
    public final SoundEvent gateClose;

    private final List<SoundEvent> events = new ArrayList<SoundEvent>();

    /**
     * @param soundSet 1.20's sound prefix: "cherry_wood" gives block.cherry_wood.break,
     *                 block.cherry_wood_door.open and the rest
     */
    public WoodKind(String name, String soundSet, MapColor planksColor, MapColor barkColor) {
        this.name = name;
        this.planksColor = planksColor;
        this.barkColor = barkColor;
        String b = "block." + soundSet;
        this.sound = new SoundType(1.0F, 1.0F, event(b + ".break"), event(b + ".step"), event(b + ".place"),
                event(b + ".hit"), event(b + ".fall"));
        this.doorOpen = event(b + "_door.open");
        this.doorClose = event(b + "_door.close");
        this.trapdoorOpen = event(b + "_trapdoor.open");
        this.trapdoorClose = event(b + "_trapdoor.close");
        this.buttonOn = event(b + "_button.click_on");
        this.buttonOff = event(b + "_button.click_off");
        this.plateOn = event(b + "_pressure_plate.click_on");
        this.plateOff = event(b + "_pressure_plate.click_off");
        this.gateOpen = event(b + "_fence_gate.open");
        this.gateClose = event(b + "_fence_gate.close");
    }

    private SoundEvent event(String name) {
        SoundEvent event = new SoundEvent(Content.id(name));
        event.setRegistryName(Content.id(name));
        this.events.add(event);
        return event;
    }

    /** @return every sound event of this wood, for the stream's sound registry handler */
    public List<SoundEvent> soundEvents() {
        return Collections.unmodifiableList(this.events);
    }

    /** A 1.12.2 sound type of the vanilla shape, from five events made by {@link Content#id}. */
    public static SoundType soundType(List<SoundEvent> sink, String prefix, float volume, float pitch) {
        SoundEvent[] e = new SoundEvent[5];
        String[] parts = {"break", "step", "place", "hit", "fall"};
        for (int i = 0; i < 5; i++) {
            e[i] = new SoundEvent(Content.id(prefix + "." + parts[i]));
            e[i].setRegistryName(Content.id(prefix + "." + parts[i]));
            sink.add(e[i]);
        }
        return new SoundType(volume, pitch, e[0], e[1], e[2], e[3], e[4]);
    }
}
