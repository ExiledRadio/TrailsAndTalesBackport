package com.exiledradio.trailsandtalesbackport.mechanics;

import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.common.Content;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSponge;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.Potion;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * The 1.20.x sound and color swaps of existing things:
 * <ul>
 *   <li>1.20.2 (23w31a): jukeboxes and tripwire hooks use wood sounds, levers, repeaters and comparators stone
 *       sounds (1.12.2: the other way round).</li>
 *   <li>1.20.2 (23w31a): sponges and wet sponges get their own sounds ({@code SoundType.SPONGE} and
 *       {@code WET_SPONGE}; 1.12.2: grass), and a sponge soaking up water plays {@code block.sponge.absorb}
 *       ({@code SpongeBlock.tryAbsorbWater}). The wet state's sound is picked by the method P15 adds to
 *       {@code BlockSponge}.</li>
 *   <li>1.20.5 (24w10a): cobwebs get their own sounds ({@code SoundType.COBWEB}; 1.12.2: stone).</li>
 *   <li>1.20 (23w12a): Wither's color is 7561558 (1.12.2: 3484199), the color of its particles.</li>
 * </ul>
 * The sound types and the color are set on the vanilla objects when the game starts and again whenever the setting
 * changes, so turning it off puts back 1.12.2's.
 */
@Mod.EventBusSubscriber(modid = TrailsAndTalesBackport.MOD_ID)
public final class BlockSounds {

    static final List<SoundEvent> SOUNDS = new ArrayList<SoundEvent>();

    public static final SoundEvent SPONGE_BREAK = event("block.sponge.break");
    public static final SoundEvent SPONGE_STEP = event("block.sponge.step");
    public static final SoundEvent SPONGE_PLACE = event("block.sponge.place");
    public static final SoundEvent SPONGE_HIT = event("block.sponge.hit");
    public static final SoundEvent SPONGE_FALL = event("block.sponge.fall");
    public static final SoundEvent SPONGE_ABSORB = event("block.sponge.absorb");
    public static final SoundEvent WET_SPONGE_BREAK = event("block.wet_sponge.break");
    public static final SoundEvent WET_SPONGE_STEP = event("block.wet_sponge.step");
    public static final SoundEvent WET_SPONGE_PLACE = event("block.wet_sponge.place");
    public static final SoundEvent WET_SPONGE_HIT = event("block.wet_sponge.hit");
    public static final SoundEvent WET_SPONGE_FALL = event("block.wet_sponge.fall");
    public static final SoundEvent COBWEB_BREAK = event("block.cobweb.break");
    public static final SoundEvent COBWEB_STEP = event("block.cobweb.step");
    public static final SoundEvent COBWEB_PLACE = event("block.cobweb.place");
    public static final SoundEvent COBWEB_HIT = event("block.cobweb.hit");
    public static final SoundEvent COBWEB_FALL = event("block.cobweb.fall");
    /** 1.20.3's {@code entity.player.teleport}, played where a player's ender pearl lands. */
    public static final SoundEvent PLAYER_TELEPORT = event("entity.player.teleport");

    public static final SoundType SPONGE = new SoundType(1.0F, 1.0F, SPONGE_BREAK, SPONGE_STEP, SPONGE_PLACE, SPONGE_HIT, SPONGE_FALL);
    public static final SoundType WET_SPONGE = new SoundType(1.0F, 1.0F, WET_SPONGE_BREAK, WET_SPONGE_STEP, WET_SPONGE_PLACE,
            WET_SPONGE_HIT, WET_SPONGE_FALL);
    public static final SoundType COBWEB = new SoundType(1.0F, 1.0F, COBWEB_BREAK, COBWEB_STEP, COBWEB_PLACE, COBWEB_HIT, COBWEB_FALL);

    /** 1.20's {@code MobEffects.WITHER} color, and 1.12.2's. */
    public static final int WITHER_COLOR = 7561558;
    public static final int WITHER_COLOR_1122 = 3484199;

    private static final Map<Block, SoundType> ORIGINAL = new IdentityHashMap<Block, SoundType>();
    private static Field soundType;
    private static Field liquidColor;

    private BlockSounds() {
    }

    private static SoundEvent event(String name) {
        SoundEvent e = new SoundEvent(Content.id(name));
        e.setRegistryName(Content.id(name));
        SOUNDS.add(e);
        return e;
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        for (SoundEvent e : SOUNDS) {
            event.getRegistry().register(e);
        }
    }

    /** Called from init and after the settings change. */
    public static void apply() {
        boolean on = ModConfig.mechanics.blockSounds;
        set(Blocks.JUKEBOX, on ? SoundType.WOOD : null);
        set(Blocks.TRIPWIRE_HOOK, on ? SoundType.WOOD : null);
        set(Blocks.LEVER, on ? SoundType.STONE : null);
        set(Blocks.UNPOWERED_REPEATER, on ? SoundType.STONE : null);
        set(Blocks.POWERED_REPEATER, on ? SoundType.STONE : null);
        set(Blocks.UNPOWERED_COMPARATOR, on ? SoundType.STONE : null);
        set(Blocks.POWERED_COMPARATOR, on ? SoundType.STONE : null);
        set(Blocks.SPONGE, on ? SPONGE : null);
        set(Blocks.WEB, on ? COBWEB : null);
        setColor(MobEffects.WITHER, ModConfig.mechanics.witherColor ? WITHER_COLOR : WITHER_COLOR_1122);
    }

    /** Sets a block's sound type; null puts back the one it had before this class first changed it. */
    private static void set(Block block, SoundType type) {
        try {
            if (soundType == null) {
                soundType = ObfuscationReflectionHelper.findField(Block.class, "field_149762_H");
            }
            SoundType current = (SoundType) soundType.get(block);
            if (!ORIGINAL.containsKey(block)) {
                ORIGINAL.put(block, current);
            }
            soundType.set(block, type != null ? type : ORIGINAL.get(block));
        } catch (Exception e) {
            TrailsAndTalesBackport.LOGGER.error("Could not set the sound type of {}", block.getRegistryName(), e);
        }
    }

    private static void setColor(Potion potion, int color) {
        try {
            if (liquidColor == null) {
                liquidColor = ObfuscationReflectionHelper.findField(Potion.class, "field_76414_N");
            }
            liquidColor.setInt(potion, color);
        } catch (Exception e) {
            TrailsAndTalesBackport.LOGGER.error("Could not set Wither's color", e);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void configChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (TrailsAndTalesBackport.MOD_ID.equals(event.getModID())) {
            apply();
        }
    }

    /** P15: the {@code getSoundType(state, world, pos, entity)} added to {@code BlockSponge}: a wet sponge's own sounds. */
    public static SoundType spongeSound(Block block, IBlockState state) {
        if (ModConfig.mechanics.blockSounds && block == Blocks.SPONGE && state.getBlock() == block
                && state.getValue(BlockSponge.WET)) {
            return WET_SPONGE;
        }
        return block.getSoundType();
    }

    /**
     * P15: {@code BlockSponge.tryAbsorb}'s {@code worldIn.playEvent(2001, pos, id(water))}, the splash of a sponge that
     * just soaked up water; 1.20.2 also plays {@code block.sponge.absorb} there.
     */
    public static void absorbed(World world, int type, BlockPos pos, int data) {
        world.playEvent(type, pos, data);
        if (ModConfig.mechanics.blockSounds) {
            world.playSound(null, pos, SPONGE_ABSORB, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
    }

    /** For the self-test: the sound a block makes now. */
    public static SoundType soundOf(Block block, IBlockState state, World world, BlockPos pos, Entity entity) {
        return block.getSoundType(state, world, pos, entity);
    }
}
