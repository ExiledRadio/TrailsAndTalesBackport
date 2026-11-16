package com.exiledradio.trailsandtalesbackport;

import com.exiledradio.backportcore.settings.Setting;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Configuration: one category per stream, each with an {@code enabled} switch.
 *
 * <h2>Rules for streams</h2>
 * <ul>
 *   <li>A stream edits only inside its own category class.</li>
 *   <li>Category classes extend {@code Object} directly. Forge 1.12 crashes at startup ("Unknown type")
 *       on a category class that extends another one.</li>
 *   <li>{@code enabled} is read once, during registration, and needs a restart: switching a stream off
 *       means its blocks and items are never registered, so a world that has them loses them.</li>
 *   <li>A changed default never reaches an installed game (Forge keeps the value in the file), so a
 *       default that must change gets a new key.</li>
 * </ul>
 */
@Config(modid = TrailsAndTalesBackport.MOD_ID, name = TrailsAndTalesBackport.MOD_ID)
public final class ModConfig {

    @Setting.Feature(icon = "trailsandtalesbackport:cherry_sapling")
    @Config.LangKey("config.trailsandtalesbackport.cherry")
    @Config.Comment("The cherry wood set, cherry leaves and saplings, and pink petals.")
    public static final Cherry cherry = new Cherry();

    @Setting.Feature(icon = "trailsandtalesbackport:bamboo_mosaic")
    @Config.LangKey("config.trailsandtalesbackport.bamboo")
    @Config.Comment("The bamboo wood set: bamboo blocks, planks, mosaic and the rest.")
    public static final Bamboo bamboo = new Bamboo();

    @Setting.Feature(icon = "trailsandtalesbackport:cherry_chest_boat")
    @Config.LangKey("config.trailsandtalesbackport.boats")
    @Config.Comment("Cherry boats, bamboo rafts and their chest forms.")
    public static final Boats boats = new Boats();

    @Setting.Feature(icon = "trailsandtalesbackport:pink_petals")
    @Config.LangKey("config.trailsandtalesbackport.cherryGrove")
    @Config.Comment("The cherry grove biome.")
    public static final CherryGrove cherryGrove = new CherryGrove();

    @Setting.Feature(icon = "trailsandtalesbackport:oak_hanging_sign")
    @Config.LangKey("config.trailsandtalesbackport.hangingSigns")
    @Config.Comment("Hanging signs for every wood.")
    public static final HangingSigns hangingSigns = new HangingSigns();

    @Setting.Feature(icon = "minecraft:sign")
    @Config.LangKey("config.trailsandtalesbackport.signs")
    @Config.Comment("1.20's sign changes: editing, text on both sides and waxing.")
    public static final Signs signs = new Signs();

    @Setting.Feature(icon = "trailsandtalesbackport:chiseled_bookshelf")
    @Config.LangKey("config.trailsandtalesbackport.chiseledBookshelf")
    @Config.Comment("The chiseled bookshelf.")
    public static final ChiseledBookshelf chiseledBookshelf = new ChiseledBookshelf();

    @Setting.Feature(icon = "trailsandtalesbackport:brush")
    @Config.LangKey("config.trailsandtalesbackport.archaeology")
    @Config.Comment("The brush, suspicious sand and gravel, pottery sherds and decorated pots.")
    public static final Archaeology archaeology = new Archaeology();

    @Setting.Feature(icon = "trailsandtalesbackport:suspicious_sand")
    @Config.LangKey("config.trailsandtalesbackport.structures")
    @Config.Comment("Trail ruins, and archaeology in desert pyramids and desert wells.")
    public static final Structures structures = new Structures();

    @Setting.Feature(icon = "trailsandtalesbackport:sniffer_egg")
    @Config.LangKey("config.trailsandtalesbackport.sniffer")
    @Config.Comment("The sniffer, sniffer eggs, torchflowers and pitcher plants.")
    public static final Sniffer sniffer = new Sniffer();

    @Setting.Feature(icon = "minecraft:cactus")
    @Config.LangKey("config.trailsandtalesbackport.camels")
    @Config.Comment("Camels.")
    public static final Camels camels = new Camels();

    @Setting.Feature(icon = "minecraft:iron_chestplate")
    @Config.LangKey("config.trailsandtalesbackport.smithing")
    @Config.Comment("Smithing templates and armor trims.")
    public static final Smithing smithing = new Smithing();

    @Setting.Feature(icon = "trailsandtalesbackport:armadillo_scute")
    @Config.LangKey("config.trailsandtalesbackport.armadillos")
    @Config.Comment("The armadillo and armadillo scutes.")
    public static final Armadillos armadillos = new Armadillos();

    @Setting.Feature(icon = "trailsandtalesbackport:wolf_armor")
    @Config.LangKey("config.trailsandtalesbackport.wolves")
    @Config.Comment("Wolf armor, the eight new wolf variants and 1.20.5's changes to tamed wolves.")
    public static final Wolves wolves = new Wolves();

    @Setting.Feature(icon = "minecraft:daylight_detector")
    @Config.LangKey("config.trailsandtalesbackport.calibratedSculkSensor")
    @Config.Comment("The calibrated sculk sensor.")
    public static final CalibratedSculkSensor calibratedSculkSensor = new CalibratedSculkSensor();

    @Setting.Feature(icon = "trailsandtalesbackport:piglin_head")
    @Config.LangKey("config.trailsandtalesbackport.heads")
    @Config.Comment("The piglin head, and mob heads on note blocks.")
    public static final Heads heads = new Heads();

    @Setting.Feature(icon = "minecraft:book")
    @Config.LangKey("config.trailsandtalesbackport.misc")
    @Config.Comment("1.20's smaller changes to existing blocks, items and mobs.")
    public static final Misc misc = new Misc();

    @Setting.Feature(icon = "minecraft:painting")
    @Config.LangKey("config.trailsandtalesbackport.lookAndFeel")
    @Config.Comment("1.20's look: the cherry grove panorama, the title logo, splashes, menu music and the rest.")
    public static final LookAndFeel lookAndFeel = new LookAndFeel();

    // ------------------------------------------------------------------------------------------
    // Categories. Each stream owns exactly one.
    // ------------------------------------------------------------------------------------------

    public static final class Cherry {
        @Config.RequiresMcRestart
        @Config.Comment("Cherry wood, leaves, saplings and pink petals. Off, nothing is registered.")
        @Config.LangKey("config.trailsandtalesbackport.cherry.enabled")
        public boolean enabled = true;
    }

    public static final class Bamboo {
        @Config.RequiresMcRestart
        @Config.Comment("The bamboo wood set. Off, nothing is registered.")
        @Config.LangKey("config.trailsandtalesbackport.bamboo.enabled")
        public boolean enabled = true;

        @Config.Comment({"Without Village & Pillage, this mod adds the bamboo plant itself; this scatters it",
                "through jungles in newly generated chunks, as 1.14 does."})
        @Setting.NewChunks
        @Config.LangKey("config.trailsandtalesbackport.bamboo.jungleBamboo")
        public boolean jungleBamboo = true;
    }

    public static final class Boats {
        @Config.RequiresMcRestart
        @Config.Comment("Cherry boats and bamboo rafts, with and without chests. Off, nothing is registered.")
        @Config.LangKey("config.trailsandtalesbackport.boats.enabled")
        public boolean enabled = true;
    }

    public static final class CherryGrove {
        @Config.RequiresMcRestart
        @Config.Comment("The cherry grove biome. Off, it never generates.")
        @Setting.NewChunks
        @Config.LangKey("config.trailsandtalesbackport.cherryGrove.enabled")
        public boolean enabled = true;
    }

    public static final class HangingSigns {
        @Config.RequiresMcRestart
        @Config.Comment("Hanging signs. Off, nothing is registered.")
        @Config.LangKey("config.trailsandtalesbackport.hangingSigns.enabled")
        public boolean enabled = true;
    }

    public static final class Signs {
        @Config.RequiresMcRestart
        @Config.Comment("Signs can be edited after placing, carry text on both sides, and can be waxed.")
        @Config.LangKey("config.trailsandtalesbackport.signs.enabled")
        public boolean enabled = true;
    }

    public static final class ChiseledBookshelf {
        @Config.RequiresMcRestart
        @Config.Comment("The chiseled bookshelf. Off, nothing is registered.")
        @Config.LangKey("config.trailsandtalesbackport.chiseledBookshelf.enabled")
        public boolean enabled = true;
    }

    public static final class Archaeology {
        @Config.RequiresMcRestart
        @Config.Comment("The brush, suspicious sand and gravel, pottery sherds and decorated pots. Off, nothing is registered.")
        @Config.LangKey("config.trailsandtalesbackport.archaeology.enabled")
        public boolean enabled = true;
    }

    public static final class Structures {
        @Config.RequiresMcRestart
        @Config.Comment("Trail ruins and the archaeology added to older structures. Off, none generate.")
        @Setting.NewChunks
        @Config.LangKey("config.trailsandtalesbackport.structures.enabled")
        public boolean enabled = true;
    }

    public static final class Sniffer {
        @Config.RequiresMcRestart
        @Config.Comment("The sniffer, its egg, torchflowers and pitcher plants. Off, nothing is registered.")
        @Config.LangKey("config.trailsandtalesbackport.sniffer.enabled")
        public boolean enabled = true;
    }

    public static final class Camels {
        @Config.RequiresMcRestart
        @Config.Comment("Camels. Off, nothing is registered.")
        @Config.LangKey("config.trailsandtalesbackport.camels.enabled")
        public boolean enabled = true;
    }

    public static final class Smithing {
        @Config.RequiresMcRestart
        @Config.Comment("Smithing templates and armor trims. Off, nothing is registered.")
        @Config.LangKey("config.trailsandtalesbackport.smithing.enabled")
        public boolean enabled = true;

        @Config.Comment({
                "Netherite upgrades need the netherite upgrade smithing template, as in 1.20, on whichever smithing",
                "table is in the game. Off: an upgrade also works with the template slot left empty."
        })
        @Config.LangKey("config.trailsandtalesbackport.smithing.netheriteNeedsTemplate")
        public boolean netheriteNeedsTemplate = true;

        @Config.RequiresMcRestart
        @Config.Comment({
                "Copper ore veins in the Overworld for this mod's copy of copper (only without Caves & Cliffs",
                "Backport and Tricky Trials Backport, which have their own)."
        })
        @Config.LangKey("config.trailsandtalesbackport.smithing.copperOre")
        public boolean copperOre = true;

        @Config.RequiresMcRestart
        @Config.Comment("Amethyst geodes for this mod's copy of amethyst (only without Caves & Cliffs Backport).")
        @Config.LangKey("config.trailsandtalesbackport.smithing.geodes")
        public boolean geodes = true;
    }

    public static final class Armadillos {
        @Config.RequiresMcRestart
        @Config.Comment("The armadillo and armadillo scutes. Off, nothing is registered.")
        @Config.LangKey("config.trailsandtalesbackport.armadillos.enabled")
        public boolean enabled = true;
    }

    public static final class Wolves {
        @Config.RequiresMcRestart
        @Config.Comment("Wolf armor, wolf variants and the tamed wolf changes.")
        @Config.LangKey("config.trailsandtalesbackport.wolves.enabled")
        public boolean enabled = true;
    }

    public static final class CalibratedSculkSensor {
        @Config.RequiresMcRestart
        @Config.Comment("The calibrated sculk sensor. Off, nothing is registered.")
        @Config.LangKey("config.trailsandtalesbackport.calibratedSculkSensor.enabled")
        public boolean enabled = true;
    }

    public static final class Heads {
        @Config.RequiresMcRestart
        @Config.Comment("The piglin head and mob heads playing their mob's sound on note blocks.")
        @Config.LangKey("config.trailsandtalesbackport.heads.enabled")
        public boolean enabled = true;
    }

    public static final class Misc {
        @Config.RequiresMcRestart
        @Config.Comment("1.20's smaller changes to existing blocks, items and mobs.")
        @Config.LangKey("config.trailsandtalesbackport.misc.enabled")
        public boolean enabled = true;
    }

    public static final class LookAndFeel {
        @Config.RequiresMcRestart
        @Config.Comment("1.20's title screen, splashes, menu music, options and loading screen through Backport Core.")
        @Config.LangKey("config.trailsandtalesbackport.lookAndFeel.enabled")
        public boolean enabled = true;

        @Config.RequiresMcRestart
        @Config.Comment("1.20 adds A Familiar Room to the music that plays in every overworld biome.")
        @Config.LangKey("config.trailsandtalesbackport.lookAndFeel.gameMusic")
        public boolean gameMusic = true;
    }

    // ------------------------------------------------------------------------------------------

    /** Saves edits made in the in-game config screen. */
    @Mod.EventBusSubscriber(modid = TrailsAndTalesBackport.MOD_ID)
    public static final class EventHandler {
        private EventHandler() {
        }

        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (TrailsAndTalesBackport.MOD_ID.equals(event.getModID())) {
                ConfigManager.sync(TrailsAndTalesBackport.MOD_ID, Config.Type.INSTANCE);
            }
        }
    }

    private ModConfig() {
    }
}
