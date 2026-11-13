# Stream Ar: the armadillo

Package `armadillo` (client parts in `armadillo.client`). Config `armadillos` (`enabled`). Entity id 15
(`armadillo`); ids 16-19 and packet ids 65-69 are unused (the stream sends no packets of its own). Wolf armor is
stream W's; this stream only provides the scute.

## Changelog section

*   **Armadillo**: a small, shy mob, 12 health, found in savannas and badlands. When something scares it - an
    undead mob, a sprinting or riding player, or whoever hurt it - it rolls up into its shell, peeks out now and
    then, and unrolls a few seconds after the danger is gone. Rolled up it takes much less damage. Fire, lava,
    cacti and the like make it unroll and run. Spiders keep away from it unless it is rolled up.
*   **Armadillo scute**: an adult sheds one every 5 to 10 minutes. Brush an armadillo to get one at once (a
    dispenser holding a brush does it too). Breed armadillos with spider eyes.
*   Advancement *Isn't It Scute?*; the armadillo counts for *Two by Two*.

## How it works

*   **Source**: 1.20.6's `Armadillo`, `ArmadilloAi`, `ArmadilloModel`, `ArmadilloAnimation`, `ArmadilloRenderer`,
    the brush's dispenser behavior and the spider's avoid goal, decompiled from the client jar remapped to
    Mojang's names (`G:/MCDownloads/tt-armadillo-decomp`, `out`), checked against 26.3's (`out26`, current Java).
    Current Java's changes that are followed: shedding and brushing go through loot tables
    (`gameplay/armadillo_shed`, `brush/armadillo`, one scute each); the scute timer always counts down but sheds
    only for an adult with `doMobLoot` on (a baby whose timer ran out sheds as soon as it grows up); environmental
    panic is 26.3's `panic_environmental_causes` (cactus, freezing, magma, fire, lava, lightning) read from the
    hurt-by memory; a rolled-up armadillo refuses every interaction but the brush; a baby's eyes are at 0.21875.
    The model, animations, sounds and texture are 1.20.6's (26.x's new baby model is not part of 1.20).
*   **`EntityArmadillo`** (`EntityAnimal`): 0.7 x 0.65, eyes 0.26; a baby is 0.6 the size (`setScaleForAge`).
    Speed 0.14, 12 health, floats. Its `ArmadilloState` (idle, rolling 10 ticks, scared 50, unrolling 30) is
    synced; `inStateTicks` restarts on every change on both sides, and the shell hides the body after 5 ticks of
    rolling and until 26 ticks into unrolling. `rollUp` stops it in place, forgets love and plays the roll sound;
    `rollOut` plays the unroll-finish sound. Rolled up: damage `(d - 1) / 2` (hurt-reduced sound), no ambient
    sound, the head locked to the body (`clampHeadRotationToBody`, head limit 0), and the body does not turn.
    Hurt by a living thing it remembers danger for 80 ticks and rolls up (if it can stay rolled up: not
    panicking, in water or lava, leashed, riding or ridden). Saved: `state` (by name) and `scute_time`, as 1.20.
*   **`ArmadilloBrain`**: 1.20.5's brain, ported the way the sniffer's is. Sensors (each starting at a random
    point of its scan): nearest living within the follow range and the visible ones (20 ticks), hurt-by (20),
    temptation by spider eyes within 10 (20), nearest visible adult (20), and the scare detector (5 ticks: any
    nearby entity `isScaredBy` - its box within 7 across and 2 up or down - refreshes the 80-tick danger memory;
    when the armadillo cannot stay rolled up the memory is dropped). Core: swim, panic (2x, unrolling first),
    look at target 45-90, move to target 150-250 (not while rolled up; 40-tick retry cooldown when stuck), the
    temptation and gaze cooldowns, and rolling out when no danger is remembered. Idle: look at a player within 6
    every 30-60 ticks, make love (1.0x, close enough 1), a run-one of follow temptation (1.25x, stop at 1 block
    for a baby, 2 for an adult) and a baby following an adult 5-16 away, look around (150-250 cooldown, 30
    degrees), and a run-one of stroll, walk to the look target and do nothing 30-60. Panic (while danger is
    remembered and it is not panicking): ball up - on the ground it rolls up; after more than 10 ticks of rolling
    it is rolled up (the land sound on the ground); it peeks (entity event 64: peek sound and animation) every
    150-450 ticks while the danger memory has more than 75 ticks left, starts unrolling (unroll-start sound)
    under 30, and rolls back up if danger returns while unrolling; running behaviors of any activity keep
    ticking, as 1.20's do. Within one priority, core goes before idle before panic.
*   **Head and body**: 1.20's `LookControl` (2-tick look, the head kept within `getMaxHeadYRot` of the body while
    walking) replaces 1.12.2's look helper through its field (`field_70749_g`); 1.20's `BodyRotationControl`
    is the body helper (`createBodyHelper`). Both use the armadillo's 32-degree limit (0 rolled up), where
    1.12.2's have 75 fixed.
*   **Brushing**: the brush is the archaeology stream's, recognized by registry name only
    (`ArmadilloContent.isBrush`: `trailsandtalesbackport:brush`), so this stream builds and works with or without
    that branch. `processInteract`: an adult drops `brush/armadillo` with the brush sound and the brush takes 16
    damage; a baby cannot be brushed. A player who brushes one gets *Isn't It Scute?*. In init, if the brush
    exists, the dispenser behavior for it is 1.20.5's: the first adult armadillo in the block in front is
    brushed (16 damage; a used-up brush breaks); with none the dispenser clicks and keeps the brush. A behavior
    another mod registered for the brush earlier still runs when there is no armadillo.
*   **Spawning** (`ArmadilloSpawns`, the one place): 1.20.5's biome files. Savanna, savanna plateau and windswept
    savanna weight 10, groups 2-3; badlands, wooded and eroded badlands weight 6, groups 1-2 (1.12.2's mesas have
    no other animals, as 1.20's badlands). Biomes are matched by registry path in any namespace, 1.12.2's names
    (`savanna`, `savanna_rock`, `mutated_savanna(_rock)`, `mesa`, `mesa_rock`, `mesa_clear_rock`,
    `mutated_mesa(_rock|_clear_rock)`) and 1.18's (`savanna_plateau`, `windswept_savanna`, `badlands`,
    `wooded_badlands`, `eroded_badlands`). **For Caves & Cliffs' 1.18 terrain**: biomes with 1.18 names get the
    spawns with no code; anything else calls `ArmadilloSpawns.addSpawns(Biome)` or reads
    `ArmadilloSpawns.forBiomePath(path)` ({weight, min, max}). Spawn rule: `armadillo_spawnable_on` (grass,
    terracotta and the white, yellow, orange, red, brown and light gray terracottas, red sand, coarse dirt) with
    light above 8; chunk-generation spawns are held to it too (`LivingSpawnEvent.CheckSpawn`), as 1.20 does.
*   **Spiders**: every spider and cave spider gets 1.20.5's `AvoidEntityGoal(Armadillo, 6, 1.0, 1.2)` at
    priority 2, for armadillos that are not rolled up (added on joining the world).
*   **Advancements** (`ArmadilloAdvancements`): *Isn't It Scute?* is our file under Adventure (granted by code).
    *Two by Two* gets `minecraft:armadillo` (bred_animals, parent our armadillo) as a requirement of its own, by
    reflection whenever the server loads advancements, as the sniffer stream does for the sniffer.
*   **Client**: `ModelArmadillo` is 1.20.5's model part for part (64 x 64, the same cubes, pivots, rotations and
    the body's 0.3 inflated shell), built from `AnimatedPart`s - 1.12.2 parts with 1.20's reset pose, scale and
    `skipDraw`. Rolled up the body's own cubes, hind legs and tail are hidden and the ball shown; the head
    follows the look within 25 down, 22.5 up and 32.5 aside. `KeyframeAnimation` is 1.20's keyframe player;
    `ArmadilloAnimations` (walk, roll up, peek, roll out: 362 keyframes) is generated from 1.20.6's by
    `tools/armadillo/animations.py`. The walk runs at 16.5 x the limb swing, strength 2.5 x the swing amount;
    the peek clock is fast-forwarded 50 ticks on becoming scared, as 1.20 does, so the shell holds still until
    the first peek. A baby is drawn 0.6 size, 16.02 pixels down. Shadow 0.4.
*   Assets: `tools/armadillo/generate.py` (texture, the scute's model and texture, 13 sound events with 1.20.6's
    files and subtitles, names, the two loot tables, the advancement).
*   **Shared code**: `AnimationClock`, `AnimatedPart` and `KeyframeAnimation` are copies of the sniffer stream's
    (with a fast-forward, a reset rotation and `skipDraw` added). When the sniffer, camel and armadillo branches
    are all merged they can become one copy in `common`.

## Tested headlessly

`./gradlew runServer -Pselftest=armadillo` (port 25921): **`pass=117 fail=0`** (and three earlier runs at 115 checks, before the look-control checks were added).
`-Pselftest=all`: **`pass=525 fail=0`**. `check_assets.py`:
**70 blocks, 1105 states, 60 item models, 466 sound files, 0 errors**. Release jar built with `--rerun-tasks`:
reobfuscated (`func_` names in the armadillo classes), no self-test classes.

The archaeology stream is not on this branch, so the test registers a stand-in `trailsandtalesbackport:brush`
(test builds only, and only when `archaeology.ItemBrush` is absent; left out of the asset dump).

*   Registration: the scute (name, key, found by name, stacks to 64), entity id 15, spawn egg colors, the 13
    sound events, both loot tables giving one scute, the brush known by name.
*   Spawns: weights and group sizes in every savanna and badlands biome, none in plains or desert, 1.18 names,
    added once; the spawnable blocks and the spawn check on red sand and stone.
*   The armadillo: health, speed, size, eyes, the baby's size and eyes, the look control in place, the first scute
    time, spider eyes; saving and loading `state` and `scute_time`, an unknown state loading as idle.
*   Feeding: an adult takes a spider eye and falls in love; a rolled-up one takes nothing; rolling up forgets love.
*   Scare, end to end: a husk 3 blocks away makes it roll up within the scans, rolled up on the 11th tick, stays
    rolled up while the husk is there, starts unrolling with under 30 ticks of danger left and is idle again
    70-90 ticks after the husk is gone. Players: walking does not scare it, sprinting does, not from 9 blocks
    across or 4 above, never a spectator.
*   Damage: 5 when idle; hurt by a mob it rolls up and remembers danger 80 ticks; rolled up 5 becomes 2; fire
    unrolls it and it panics.
*   Shedding: one scute when the timer runs out, the next in 5-10 minutes; a baby sheds none but its timer runs
    on and it sheds when grown; none without `doMobLoot`.
*   Brushing: an adult (also rolled up) gives a scute, 16 durability, *Isn't It Scute?*; a baby gives nothing and
    the brush takes no wear. Dispenser: no armadillo, the brush stays unused; with one, a scute and 16 damage; a
    worn-out brush breaks.
*   Spiders and cave spiders get the avoid goal at priority 2.
*   Breeding, end to end: two armadillos fed spider eyes have one baby (-24000), parents wait 6000.
*   *Two by Two* needs `minecraft:armadillo` as its own requirement; *Isn't It Scute?* sits under Adventure.

## Not tested

*   Anything drawn: the model, the shell and ball switch, the four animations (generated keyframe for keyframe,
    worth a look in game: the roll up and the peek above all), the baby's size, the texture, the scute icon.
*   Sounds as heard (step volume, peek), the peek's timing, temptation following a real player, babies following
    adults, panic running, spiders actually fleeing, natural spawning in a real savanna, the dispenser with the
    archaeology stream's real brush (the stand-in has the same name and durability).

## Left out, and why

*   **Game events** (roll up, unroll, shed, brush): they wait for Core's vibrations (`CORE_REQUESTS.md` 3).
*   **Wolf armor** and the scute's use in it: stream W.
*   **26.x's baby armadillo model**: a 26.x change, not 1.20's.
*   **1.20.5 renamed the turtle's scute to "Turtle Scute"**: that item is Update Aquatic's (and vanilla 1.12.2's
    has no scute at all), so nothing here.
