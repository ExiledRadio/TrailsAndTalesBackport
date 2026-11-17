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

    @Setting.Feature(icon = "minecraft:comparator")
    @Config.LangKey("config.trailsandtalesbackport.compat")
    @Config.Comment("How this mod behaves alongside other mods.")
    public static final Compat compat = new Compat();

    @Setting.Feature(icon = "minecraft:compass")
    @Config.LangKey("config.trailsandtalesbackport.presentation1205")
    @Config.Comment("1.20.5's world warnings and Viossa: chunk failure and low disk space toasts, the low disk space and world recovery screens, and the Viossa language.")
    public static final Presentation1205 presentation1205 = new Presentation1205();

    @Setting.Feature(icon = "minecraft:book")
    @Config.LangKey("config.trailsandtalesbackport.misc")
    @Config.Comment("1.20's smaller changes to existing blocks, items and mobs.")
    public static final Misc misc = new Misc();

    @Setting.Feature(icon = "minecraft:iron_sword")
    @Config.LangKey("config.trailsandtalesbackport.mechanics")
    @Config.Comment("1.20's changes to how mobs fight, shields block and the spawn area stays loaded, and smaller 1.20 rules.")
    public static final Mechanics mechanics = new Mechanics();

    @Setting.Feature(icon = "minecraft:painting")
    @Config.LangKey("config.trailsandtalesbackport.lookAndFeel")
    @Config.Comment("1.20's look: the cherry grove panorama, the title logo, splashes, menu music and the rest.")
    public static final LookAndFeel lookAndFeel = new LookAndFeel();

    // ------------------------------------------------------------------------------------------
    // Categories. Each stream owns exactly one.
    // ------------------------------------------------------------------------------------------

    public static final class Compat {
        @Config.RequiresMcRestart
        @Config.Comment({"When another installed mod adds the same blocks and items (stripped logs, bamboo, chain): OURS hides the",
                "other mod's, THEIRS hides ours, BOTH hides nothing."})
        @Config.LangKey("config.trailsandtalesbackport.compat.duplicates")
        public com.exiledradio.backportcore.duplicate.DuplicateBlocks.Keep duplicates = com.exiledradio.backportcore.duplicate.DuplicateBlocks.Keep.OURS;

        @Config.Comment("Convert blocks and items left behind by another mod that added them, when that mod is removed from a world.")
        @Config.LangKey("config.trailsandtalesbackport.compat.convertRemoved")
        public boolean convertRemoved = true;
    }

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
        @Config.Comment("The calibrated sculk sensor, the sculk sensor when Wild Update Backport is absent, and 1.20's vibrations (amethyst resonance, the game events of this mod's blocks and mobs). Off, nothing is registered.")
        @Config.LangKey("config.trailsandtalesbackport.calibratedSculkSensor.enabled")
        public boolean enabled = true;
    }

    public static final class Heads {
        @Config.RequiresMcRestart
        @Config.Comment("The piglin head and mob heads playing their mob's sound on note blocks.")
        @Config.LangKey("config.trailsandtalesbackport.heads.enabled")
        public boolean enabled = true;
    }

    public static final class Presentation1205 {
        @Config.Comment("In singleplayer, a toast when a chunk fails to load or save (1.20.5).")
        @Config.LangKey("config.trailsandtalesbackport.presentation1205.chunkToasts")
        public boolean chunkToasts = true;

        @Config.Comment("A warning screen when opening a singleplayer world with less than 64 MB free, and a toast when the game saves with that little space left (1.20.5).")
        @Config.LangKey("config.trailsandtalesbackport.presentation1205.lowDiskSpace")
        public boolean lowDiskSpace = true;

        @Config.Comment("Worlds whose level.dat cannot be read show in the world list and open the Failed to Load World screen, which can restore level.dat_old, instead of 1.12.2 using level.dat_old silently (1.20.5).")
        @Config.LangKey("config.trailsandtalesbackport.presentation1205.recoverWorld")
        public boolean recoverWorld = true;

        @Config.RequiresMcRestart
        @Config.Comment("Viossa in the language list (1.20.5).")
        @Config.LangKey("config.trailsandtalesbackport.presentation1205.viossa")
        public boolean viossa = true;
    }

    public static final class Misc {
        @Config.Comment({"1.20.5's effect particles: one particle per effect in its own color (ambient ones fainter), and fewer of",
                "them, in place of one blended color. Shown while 1.20.5 or later is the newest installed version. Client only."})
        @Setting.Client
        @Config.LangKey("config.trailsandtalesbackport.misc.effectParticles")
        public boolean effectParticles = true;

        @Config.RequiresMcRestart
        @Config.Comment("1.20's smaller changes to existing blocks, items and mobs.")
        @Config.LangKey("config.trailsandtalesbackport.misc.enabled")
        public boolean enabled = true;

        @Config.Comment("1.20's death messages: /kill reads \"was killed\" and the world border \"left the confines of this world\".")
        @Config.LangKey("config.trailsandtalesbackport.misc.deathMessages")
        public boolean deathMessages = true;

        @Config.Comment("1.20's step sounds: walking on a carpet, moss carpet, snow layer or roots plays it and, muffled, the block below; on petals, a lily pad or glow lichen it plays that block.")
        @Config.LangKey("config.trailsandtalesbackport.misc.stepSounds")
        public boolean stepSounds = true;
    }

    public static final class Mechanics {
        @Config.Comment({"1.20.2's mob melee reach: a mob hits only what its own box, widened by 0.83 blocks to each side (not up",
                "or down), touches, and only what it can see. No hits through thin walls or from below a mounted rider; tall",
                "mobs like endermen reach higher. Applies to every melee AI built on vanilla's."})
        @Config.LangKey("config.trailsandtalesbackport.mechanics.meleeReach")
        public boolean meleeReach = true;

        @Config.Comment("1.20.3: a raised shield blocks by the way the player faces, whatever the pitch, so it also blocks while looking straight up or down.")
        @Config.LangKey("config.trailsandtalesbackport.mechanics.shieldPitch")
        public boolean shieldPitch = true;

        @Config.Comment({"1.20.5's spawn chunks: the area kept loaded around the world spawn is 5 by 5 chunks (radius 2) instead",
                "of 1.12.2's 17 by 17, and the game prepares only that area when a world starts."})
        @Config.LangKey("config.trailsandtalesbackport.mechanics.spawnChunks")
        public boolean spawnChunks = true;

        @Config.RequiresMcRestart
        @Config.Comment("1.20: beds, wool and carpets of any color can be dyed another color in the crafting grid (1.12.2: only white ones).")
        @Config.LangKey("config.trailsandtalesbackport.mechanics.redyeing")
        public boolean redyeing = true;

        @Config.Comment("1.20: Jump Boost makes slimes and magma cubes jump higher.")
        @Config.LangKey("config.trailsandtalesbackport.mechanics.slimeJumpBoost")
        public boolean slimeJumpBoost = true;

        @Config.Comment("1.20: teleporting by eating chorus fruit resets fall distance, so the landing does no fall damage built up before.")
        @Config.LangKey("config.trailsandtalesbackport.mechanics.chorusFallDistance")
        public boolean chorusFallDistance = true;

        @Config.Comment("1.20.3: an arrow item with a custom name shot from a bow keeps the name, and picking the arrow up gives the named item back.")
        @Config.LangKey("config.trailsandtalesbackport.mechanics.arrowNames")
        public boolean arrowNames = true;
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

        @Config.RequiresMcRestart
        @Config.Comment("1.20's biome music: Bromeliad, Crescent Dunes, Echo in the Wind and the older tracks in jungles, deserts, badlands, forests, old growth taigas and the cherry grove, and 1.20's lists in Caves & Cliffs' 1.18 biomes.")
        @Config.LangKey("config.trailsandtalesbackport.lookAndFeel.biomeMusic")
        public boolean biomeMusic = true;
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
