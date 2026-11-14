package com.exiledradio.trailsandtalesbackport.sign;

import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.common.Content;
import com.exiledradio.trailsandtalesbackport.common.Owners;

import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Stream Sg: 1.20's sign changes, on every sign whose tile is a {@code TileEntitySign}.
 *
 * <ul>
 *   <li>{@link SignData}: back text, per-face glow, waxing, as a capability on each sign tile.</li>
 *   <li>{@link SignInteraction}: right-clicking a placed sign edits the face toward the player, or dyes,
 *       glows, un-glows or waxes it.</li>
 *   <li>{@link SignNetwork}: the editor's packets (ids 30 and 31 of the block 30-34).</li>
 *   <li>{@code sign.client}: the 1.20 edit screens, and renderers that draw both faces for vanilla's, our
 *       and Village & Pillage's signs and our hanging signs.</li>
 * </ul>
 * Honeycomb (Buzzy Bees) and glow ink (Caves & Cliffs) are found by name: without them there is no waxing
 * or glowing text, because nothing could be got to do it with.
 */
@Mod.EventBusSubscriber(modid = TrailsAndTalesBackport.MOD_ID)
public final class SignsContent {

    private static final List<SoundEvent> SOUNDS = new ArrayList<SoundEvent>();
    public static final SoundEvent WAXED_INTERACT_FAIL = event("block.sign.waxed_interact_fail");
    public static final SoundEvent DYE_USE = event("item.dye.use");
    public static final SoundEvent GLOW_INK_SAC_USE = event("item.glow_ink_sac.use");
    public static final SoundEvent INK_SAC_USE = event("item.ink_sac.use");
    public static final SoundEvent HONEYCOMB_WAX_ON = event("item.honeycomb.wax_on");

    /** Buzzy Bees' honeycomb, found in init; null without it. Tests may swap in a stand-in. */
    @Nullable
    public static Item honeycomb;
    /** Caves & Cliffs' glow ink sac, found in init; null without it. */
    @Nullable
    public static Item glowInkSac;
    /**
     * True when 1.14's plain dyes exist (Village & Pillage's {@code black_dye}): 1.12.2's ink sac, cocoa
     * beans, lapis and bone meal are then only themselves, as in 1.20, not also dyes.
     */
    public static boolean modernDyes;

    private SignsContent() {
    }

    private static SoundEvent event(String name) {
        SoundEvent e = new SoundEvent(Content.id(name));
        e.setRegistryName(Content.id(name));
        SOUNDS.add(e);
        return e;
    }

    public static boolean enabled() {
        return ModConfig.signs.enabled;
    }

    public static void preInit() {
        if (enabled()) {
            SignData.register();
            MinecraftForge.EVENT_BUS.register(SignInteraction.class);
        }
    }

    public static void init() {
        if (enabled()) {
            honeycomb = Owners.theirs(Owners.BUZZY_BEES, "honeycomb");
            glowInkSac = Owners.theirs(Owners.CAVES_AND_CLIFFS, "glow_ink_sac");
            modernDyes = Owners.theirs(Owners.VILLAGE_AND_PILLAGE, "black_dye") != null;
        }
    }

    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<TileEntity> event) {
        if (enabled() && event.getObject() instanceof TileEntitySign) {
            event.addCapability(SignData.KEY, new SignData.Provider());
        }
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        if (enabled()) {
            for (SoundEvent e : SOUNDS) {
                event.getRegistry().register(e);
            }
        }
    }
}
