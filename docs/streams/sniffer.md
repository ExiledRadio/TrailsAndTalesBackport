# Stream N: the sniffer

Package `sniffer` (client parts in `sniffer.client`). Config `sniffer` (`enabled`). Entity id 5 (`sniffer`); ids
6-9 and packet ids 50-54 are unused (the stream sends no packets of its own).

## Changelog section

*   **Sniffer**: a slow, gentle mob, 14 health. It sniffs the air, sniffs the ground and walks off with its nose
    down to a spot of grass, dirt, coarse dirt or podzol it has not dug before, then digs up a **torchflower
    seed** or a **pitcher pod**. It digs again after 8 minutes. Breed two with torchflower seeds and they lay a
    **sniffer egg**; they follow you while you hold the seeds.
*   **Sniffer egg**: place it and it cracks twice, then hatches a **snifflet** after about 20 minutes (10 on
    moss). A snifflet takes 40 minutes to grow up, faster if fed torchflower seeds. Sniffer eggs are also found
    in warm ocean ruins.
*   **Torchflower** and **pitcher plant**: plant the seed or pod on farmland. A torchflower crop flowers after two
    stages; a pitcher crop grows over five stages, two blocks tall from the fourth, and breaks into a pitcher
    plant. A torchflower makes orange dye, a pitcher plant two cyan dye. A torchflower goes in a flower pot.
*   Chickens and parrots eat both new seeds.
*   Advancements *Smells Interesting*, *Little Sniffs* and *Planting the Past*; the sniffer counts for *Two by
    Two* and both seeds for *A Seedy Place*.
*   With Caves & Cliffs Backport, sniffers also dig moss and rooted dirt, and eggs on moss hatch twice as fast.
    With Wild Update Backport, they dig mud and muddy mangrove roots. With Buzzy Bees Backport, bees visit
    torchflowers and pitcher plants. With Village & Pillage Backport, the seeds, pods and flowers compost.

## How it works

*   **Source**: 1.20.1's `Sniffer`, `SnifferAi`, `SnifferEggBlock`, `TorchflowerCropBlock`, `PitcherCropBlock`,
    `DoublePlantBlock`, `SnifferModel`, `SnifferAnimation`, `SnifferRenderer` and `SnifferSoundInstance`,
    decompiled with Mojang's names (`G:/MCDownloads/tt-sniffer-decomp`), checked against 26.3's (current Java).
    The only change current Java made to these numbers: walking to a target gives up after 500-700 ticks
    (1.20.1: 10000-15000). Animation time is read from the tick count, as current Java does.
*   **`EntitySniffer`** (`EntityAnimal`): 1.9 x 1.75, 0.4 lower while digging, eyes at 0.6 of its height, speed
    0.1, floats, avoids water unless burning or in it, rider offset 1.8. Its state (`SnifferState`, 1.20's seven)
    is synced; the client starts the matching `AnimationClock` and resizes. Digging sets the seed-drop tick 120
    ticks ahead and sends entity event 63 (the client starts the digging sound); the drop comes from the loot
    table `trailsandtalesbackport:gameplay/sniffer_digging` (1.20's: torchflower seeds or a pitcher pod, even
    odds) at the head block, 2.25 blocks ahead. Client-side, from 1.7 s to 6 s into the dig, 30 block-crack
    particles a tick and the ground's hit sound every 10 ticks. Breeding drops an egg item instead of a child,
    with 1.20's `finalizeSpawnChildFromBreeding`: stat, `bred_animals` (no child), 6000-tick cooldown, hearts,
    1-7 experience.
*   **`SnifferBrain`**: 1.20's brain, ported. 1.12.2 has no `Brain`, so this is one: the sniffer's memories as
    fields, sensors every 20 ticks (nearest visible living within 16, hurt-by, the player within 10 holding
    seeds), and 1.20's behaviors with their exact run times and conditions - core (swim, panic 100-120 at 2x,
    move to target, temptation cooldown), idle (make love, follow temptation at 1.25x stopping 2.5/3.5 away,
    look at target 45-90, feeling happy 40-100, and a weighted `RunOne` of walk-to-look-target, scenting
    40-80, sniffing 40-80, look at a player within 6, random stroll, do nothing 5-20), sniff (searching 600)
    and dig (digging 160-180, finished digging 40). Each tick it starts what can start in the core activity
    and the active one (dig, sniff, idle - the first whose conditions hold), then ticks everything running,
    side by side as 1.20 does (no mutex). Ticked from `updateAITasks`, before 1.12.2's move and look helpers.
    Saved: `SniffCooldown` and `ExploredPositions` (up to 21, newest first), the two memories 1.20 saves.
*   **Two 1.12.2 adaptations in the brain**, both in `MoveToTarget`: (1) 1.12.2's navigator parks a mob at
    `node + (int)(width + 1) / 2`, a whole block off the node for a 1.9-wide sniffer, so "reached" measures from
    the node it walked to; (2) a path that ends on the node the sniffer already stands on lets the walk target
    go, except the sniff target (Searching times that out at 600 ticks as 1.20 does). Without these, the
    self-test showed a sniffer walking to a stroll target for ever and never sniffing again.
*   **`BlockSnifferEgg`**: hatch 0-2; each stage is a third of 24000 ticks (12000 on a hatch-boost block) plus
    0-299. 1.12.2 runs `onBlockAdded` only when the block itself changes, so `updateTick` schedules the next stage
    and plays the crack; the last hatches a baby (age -48000) at the block's center facing a random way, with
    the hatch sound. On moss, 1.20's level event 3009 (sparkles on every face, 1.20's `glint` sprite) is a block
    event, so every nearby client shows it. Hardness 0.5, metal sounds, red on maps, blocks paths, uncommon.
*   **Crops** (`BlockTorchflowerCrop`, `BlockPitcherCrop`): 1.20's `CropBlock` growth speed (`CropGrowth`), on
    farmland only, light 8 or the sky to stay. Torchflower: ages 0-1, then the torchflower block; skips two
    random ticks in three; bone meal +1; drops one seed. Pitcher: ages 0-4 with `half`; only the lower half
    ticks; grows with the crop chance and no light-9 minimum, from age 3 into the block above (air or itself);
    bone meal on either half +1; the bulb's and crop's collision boxes, none on top; only the lower half drops
    (a pod, the plant at 4); breaking either half takes the other (a creative player gets no drop).
*   **Flowers**: `BlockTorchflower` (small flower, offset), `BlockPitcherPlant` (two-block `DoublePlantBlock`,
    not a tall flower: bone meal does not copy it), potted torchflower through `common/BlockPotted`.
*   **Advancements** (`SnifferAdvancements`): our three files under husbandry, hidden until earned. *Little
    Sniffs* is granted from `processInteract` (1.12.2 has no mob-interaction trigger). *Two by Two* and *A Seedy
    Place* are vanilla files, so the loaded objects get the criteria by reflection after every advancement
    load (as Buzzy Bees adds the bee); criteria other mods add the same way are kept.
*   **Family**: diggable and hatch-boost blocks by registry path under Caves & Cliffs' or Wild Update's id
    (`rooted_dirt`, `moss_block`, `mud`, `muddy_mangrove_roots`; `moss_block`); bee flowers through Buzzy Bees'
    `BeeFlowers.addFlower`/`addFlowerItem` by reflection; composter chances (0.3 seeds and pods, 0.85
    flowers) through `common/Compat`. Chickens' and parrots' 1.12.2 seed sets get both items.
*   **For the archaeology and structure streams**: the egg is `Owners.ours("sniffer_egg")`
    (`trailsandtalesbackport:sniffer_egg`), null when this stream is off. Warm ocean ruins' table is theirs.
*   **Client**: `ModelSniffer` is 1.20's model part for part (192 x 192 texture, the same cubes, pivots and
    0.5 inflation), built from `AnimatedPart`s - 1.12.2 parts with 1.20's reset pose and scale. `KeyframeAnimation`
    is 1.20's keyframe player (linear and Catmull-Rom, looping, walk scaled by the limb swing x 100);
    `SnifferAnimations` is generated from 1.20's keyframes by `tools/sniffer/animations.py`. A snifflet gets
    1.20's baby head and half size 24 px down. `SnifferClient.DiggingSound` follows a digging or searching
    sniffer; the searching sound plays every 20 ticks.
*   Assets: `tools/sniffer/generate.py` (blockstates, models, textures, 15 sound events, names, advancements).

## Tested headlessly

`./gradlew runServer -Pselftest=sniffer` (port 25890): **`pass=113 fail=0`**, eight runs in a row (the dig is
random: first seed after 420-4160 ticks). `-Pselftest=all`: **`pass=521 fail=0`**. With Buzzy Bees 1.0.1 and Caves
& Cliffs 0.1.0 dev jars in `eclipse/mods` (and Core 1.2.0, which Caves & Cliffs needs): **`pass=528 fail=0`**.
`check_assets.py`: **76 blocks, 1124 states, 64 item models, 480 sound files, 0 errors**. Release jar built with
`--rerun-tasks`: reobfuscated (`func_` names), no self-test classes.

*   Registration, names, the egg found by name, uncommon; the spawn egg and its color; chickens and parrots
    take both seeds; both dye recipes.
*   Torchflower crop: planted by a real player only on farmland (counts for *Planting the Past*), shapes, bone
    meal to the flower, drops, random-tick growth.
*   Pitcher crop: planting, bone meal through every age with the top half from 3, no growth into stone,
    collision, breaking the top as a survival player (one plant, both halves gone), pod drops, random-tick
    growth to 4. Pitcher plant: placed on podzol with its top, breaking the top drops one, no room no place.
*   Egg: schedules, cracks, hatches one baby at -48000 and half size, drops itself when broken, hardness.
*   Sniffer: health, speed, size, eyes, food, diggable blocks; feeding a snifflet (age, *Little Sniffs*);
    saved memories.
*   Breeding: two sniffers fed by a player lay one egg, get the 6000 cooldown, leave love; no baby.
*   Digging, end to end: a sniffer on grass sniffs, searches, digs, drops a seed or pod, stands up, remembers the
    spot, gets the 9600 cooldown and is happy, and does not sniff or dig again in its cooldown.
*   *Two by Two* gets `minecraft:sniffer` as a requirement; *A Seedy Place* both seeds in its any-of list.
*   With the family: moss and rooted dirt dug, moss boosts an egg, torchflowers and pitcher plants are bee flowers.

## Not tested

*   Anything drawn: the model, the animations (generated keyframe for keyframe, but worth a look in game, the
    nose's scale above all), the snifflet's size and head, the texture, the egg sparkles, the dig particles.
*   Sounds as heard: the digging sound following the sniffer, the searching sound, step volume.
*   Panic, swimming, temptation following a real player, looking at players, a whole hatch on scheduled ticks
    over 20 minutes, the composter with Village & Pillage, Wild Update's mud.

## Left out, and why

*   **Game events**: 1.20's digging shake and the egg's block-place event wait for Core's vibrations
    (`CORE_REQUESTS.md` 3).
*   **Bees growing the crops**: 1.20's bees grow both crops (the pitcher crop through its own grow, which places
    the top half). Buzzy Bees' growable hook only raises an `age` property, which would leave a torchflower
    crop at age 1 for ever and a pitcher crop without its top, so neither crop is registered with it. Needs a
    grow callback in Buzzy Bees' `BeeFlowers` (its maintainer's call).
*   **Suspicious stew** from torchflowers (night vision, 5 s): suspicious stew is Village & Pillage's; nothing
    to hook into yet.
*   **Head turn limit**: 1.20 limits the sniffer's head to 50 degrees from its body; 1.12.2's look helper has its
    own fixed limit and no hook.
