# Stream Sc: calibrated sculk sensor and game events

Package `sculk` (`common/Vibrations` for the other streams' calls). Config `calibratedSculkSensor` (`enabled`). No
entities or packets. Branch `tt-sensor`. Needs Backport Core 1.4.0 (`gameevent.*`, branch `core-game-events`), and
Wild Update Backport on its `wu-core-game-events` branch when both are installed.

## Changelog section

*   **Calibrated sculk sensor**: crafted from a sculk sensor and three amethyst shards. Hears vibrations 16 blocks
    away; a redstone signal into the side with the amethyst-colored input makes it hear only vibrations of that
    frequency (1-15). It gives no power out of that side.
*   **Amethyst resonance**: a sculk sensor (any sculk sensor) that activates next to a block of amethyst makes the
    amethyst resonate, sending the vibration's frequency on to sensors further away.
*   **Vibrations** from this mod's mobs and blocks: armadillos rolling up, unrolling, shedding a scute and being
    brushed; camels sitting, standing, dashing and eating; sniffers digging and sniffer eggs being placed; decorated
    pots taking an item, wobbling and shattering; suspicious sand and gravel breaking where they land; chiseled
    bookshelves taking and giving books; signs being dyed, inked or waxed; heads on note blocks being played; wolf
    armor going on and coming off. Mounting, dismounting and taking off armor make vibrations too, and wolves
    shaking themselves dry.
*   Without Wild Update Backport, this mod adds the **sculk sensor** itself (1.20's: active 30 ticks, then 10 of
    cooldown), so the calibrated one can be made.

## How it works

*   **Source**: 1.20.6's `SculkSensorBlock`, `CalibratedSculkSensorBlock`, their block entities,
    `VibrationSystem` (with `VibrationSelector`, `VibrationInfo`), `GameEventDispatcher`,
    `EuclideanGameEventListenerRegistry`, the game event tags and every `gameEvent` call of this mod's features,
    decompiled from `G:/MCDownloads/tt-camel-decomp/named.jar` (1.20.6, Mojang's names).
*   **Core's game events**: this stream requires them as "1.20" (`GameEvents.require`), which switches every
    backport's listeners to 1.20.6's `#vibrations` and Core's 1.20 emitters on (mount, dismount, 1.20's
    equip/unequip, wolf shake). The vibration rules (`VibrationSystem`) are Core's; see Core's `docs/API.md`.
*   **Sculk sensor** (`BlockSculkSensor`, `sculk_sensor`, only when `Owners.shouldRegisterCopy("sculk_sensor")`:
    Wild Update Backport owns it in Core's table): `sculk_sensor_phase`, metadata = phase, as Wild Update
    Backport's, so the owner can take a world's copies over. Power and `last_vibration_frequency` in the tile
    entity (`TileSculkSensor`, radius 8, `listener` tag as 1.20's). Inactive -> active `activeTicks()` (30) ->
    cooldown 10 -> inactive with `clicking_stop`. Power 15 at the sensor down to 1 at the radius
    (`redstoneStrengthForDistance`, block distance); strong power only into the block below; comparator: the last
    frequency while active. Its own block's place or destroy is not heard; only inactive sensors hear. Stepping
    on it schedules a step from the entity (`forceSchedule`), sneaking players included (a player-tick hook, as
    1.12.2's `onEntityWalk` skips them), never a warden. Hardness 1.5, light 1, Silk Touch drops it, else 5
    experience, hoes mine it faster. Waterloggable through Core (`Waterloggable`); waterlogged, it clicks
    silently. Blockstates as 1.20.6's (cooldown shows the active model).
*   **Calibrated sensor** (`BlockCalibratedSculkSensor`, `TileCalibratedSculkSensor`): adds `facing` (horizontal,
    where the placer looks; metadata bits 2-3), radius 16, active 10 ticks. The back (`facing.getOpposite()`, the
    input texture) reads `getRedstonePower(back neighbor, back)`; with 1-15 there, only that frequency is
    received. No weak power toward `facing`'s side (the back neighbor). Rotates and mirrors.
*   **Resonance** (`Resonance`, Core's `SensorActivation`): every activating sensor (this mod's, Wild Update
    Backport's) calls Core; each `#vibration_resonators` block beside it (Caves & Cliffs Backport's
    `amethyst_block`, or this mod's copy once it exists) posts `resonate_<frequency>` from itself and plays
    `block.amethyst_block.resonate` at 1.20's pitch for that frequency, before the tendrils click.
*   **Recipe**: `" # ", "#X#"`, `#` = Caves & Cliffs Backport's `amethyst_shard` (or this mod's copy by name), else
    any mod's `gemAmethyst`; `X` = Wild Update Backport's sculk sensor, else ours. Without either, no recipe and a
    warning.
*   **Assets**: models, blockstates, textures and sounds from 1.20.6 (`python -I tools/sculk/build_assets.py
    src/main/resources/assets/trailsandtalesbackport`; it reads `G:/MCDownloads/trails-1.20.6`'s client jar, asset
    index and objects, and appends to `sounds.json`, so run it on a clean copy). `en_us` only; the other languages wait for the family
    tool (PLAN, Order 4).
*   **Game events of the other streams** (`common/Vibrations`), each where 1.20.6 posts it:

    | Stream | What | Event, source, where |
    | --- | --- | --- |
    | Ar | `EntityArmadillo.rollUp`, `rollOut` | `entity_action`, the armadillo |
    | Ar | scute shed (`updateAITasks`) | `entity_place`, after the drop and sound |
    | Ar | `brushOffScute` (players and dispensers) | `entity_interact`, the armadillo |
    | Ca | `handleStartJump`, `sitDown`, `standUp`, `standUpInstantly` | `entity_action`, the camel |
    | Ca | `handleEating` | `eat`, the camel |
    | N | digging, every 10 ticks | `entity_action` at the head's block, the sniffer |
    | N | `BlockSnifferEgg.onBlockAdded` | `block_place`, the egg's state |
    | A | pot takes an item / any other click | `block_change`, the player |
    | A | projectile shatters a pot | `block_destroy`, the projectile (1.20's `destroyBlock`) |
    | A | falling suspicious block breaks on landing | `block_destroy`, the falling block |
    | K | any change of the books | `block_change`, the new state; taking a book also from the player |
    | Sg | dye, glow ink, ink sac, wax on a sign | `block_change`, the player and the sign |
    | Hd | a head on a note block played | `note_block_play`, the player (none for redstone) |
    | W | wolf armor on / sheared off | `equip` / `unequip`, the wolf |

    Boats and rafts are Core's `entity_place` (`addEntityPlacer`). Placing and breaking every block is Core's.

## Tested (`./gradlew runServer -Pselftest=sensor`, port 25614)

*   Without Wild Update Backport (67/0): 1.20 rules on; both blocks' states and metadata; the recipe (with
    test-only `amethyst_shard`/`amethyst_block` stand-ins, `TestAmethyst`); facing from the placer; a vibration 5
    blocks off arriving, power 11, comparator 8, no power out of the back, strong power only below, active ->
    cooldown -> inactive; a redstone block behind: eat (8) unheard, explode (15) heard; the listener saved; the
    sculk sensor deaf at 9 blocks and to its own placing; stepping on it: active next tick at 15, frequency 1, the
    amethyst beside it resonating `resonate_1` from itself before the clicking; armadillo, camel, sniffer egg,
    pot (insert, empty hand, projectile), bookshelf (in, out) and head-on-note-block events with their sources.
*   With Wild Update Backport's dev jar (56/0): no copy of ours, the recipe takes Wild's sensor, Wild's sensor
    resonates amethyst.
*   With Caves & Cliffs Backport's dev jar (67/0): the recipe takes its `amethyst_shard`, its `amethyst_block`
    resonates (the stand-ins step aside).
*   Whole suite: 3948/0. Wild Update Backport's vibration, sculk, warden and allay tests with this mod installed:
    405/0.

## Not tested

*   Anything drawn: the models (tendrils, the amethyst crystal, the input side's rotation), the animated tendril
    textures, the particles. Sounds as heard. Waterlogging (no Fluidlogged API in the dev run).
*   The sniffer's digging event and the camel's eating event in a running game; landing suspicious blocks; signs
    and wolf armor events (each is one call next to code those streams test); the 1.20 wolf shake and equip
    emitters (Core).
*   A comparator or redstone line actually driving the frequency filter in a game.

## Left out, and why

*   **Getting a sculk sensor without Wild Update Backport**: 1.20's come from the deep dark and ancient cities,
    which are Wild Update Backport's. Our copy is creative-only until a fallback source is decided (Decided 3
    covers items whose structure's owner is absent; the sensor is not on that list yet).
*   **Amethyst**: the shard and block copies (PLAN, "Copies this adds") are stream Sm's (`tt-smithing`), not
    built yet; with neither Caves &
    Cliffs Backport nor a `gemAmethyst` mod installed the calibrated sensor has no recipe and nothing resonates.
*   **Wild Update Backport's sensor keeps 1.19.2's timings** (40 ticks active, 1 of cooldown) with this mod
    installed; only its hearing (1.20's `#vibrations`) and resonance follow 1.20.
*   **Wolves' shake for 1.19.2 rules**: posted only with 1.20 rules on, so Wild Update Backport alone is unchanged.
