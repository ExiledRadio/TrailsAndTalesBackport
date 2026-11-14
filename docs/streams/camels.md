# Stream Ca: camels

Package `camel`. Config `camels` (`enabled`). Entity id 10 (`camel`). Packet ids 55-59 reserved, none used:
everything the camel syncs goes through entity data and 1.12.2's own riding packets.

## Changelog section

*   **Camels**: tall, slow desert mounts with 32 health that carry two riders. Put a saddle on one by
    right-clicking it; the first rider steers. The jump key **dashes** forward instead of jumping, charged like
    a horse's jump; the jump bar shows the cooldown until the next dash is ready. Camels sit down now and then
    and get up again; a sitting camel won't move until its rider presses forward. They step up a block and a
    half, breed and heal with **cactus**, follow players holding cactus, and panic when hurt. Sneak and
    right-click to open a camel's saddle slot.
*   Every desert village has a camel at its center.
*   *Two by Two* now needs camels.
*   Superflat worlds get only what their preset lists: cherry trees, petals and jungle bamboo need `decoration`, and a desert village's camel comes with the preset's villages.

## How it works

*   `EntityCamel` extends 1.12.2's `AbstractHorse`, as 1.20's camel extends 1.20's. The saddle slot and its
    screen, riding, the jump bar (`IJumpingMount`), eating grass, fall damage and dropping the saddle on death
    come from it. Everything the camel changes is 1.20.6's numbers (decompiled with Mojang's names into
    `G:/MCDownloads/tt-camel-decomp`):
    *   32 health, speed 0.09, jump strength 0.42, step height 1.5; 1.7 x 2.375 (0.945 tall sitting), eyes 2.275
        (0.845 sitting); a baby is 0.45 of that and grows in 20 minutes. Spawned camels are grown (1.12.2's
        horse makes a foal one time in five; 1.20 doesn't). Always tame, never rears.
    *   **Sitting**: one synced value, the game time of the last sit or stand, negated while sitting (1.20's
        `LAST_POSE_CHANGE_TICK`; saved as `LastPoseTick`). Sitting down takes 40 ticks, standing up 52; in
        between and while sitting the camel refuses to move, and its head stays within 30 degrees of its body.
        It stands at once when hurt or in water, when a leash pulls it more than 6 blocks, when a walk order
        comes, and when the rider presses forward. 1.12.2 has no long data serializer, so the mod registers one
        (`trailsandtalesbackport:long`, a Forge data serializer entry, registered even with camels off).
    *   **Dash**: only saddled, on the ground, standing, with the dash ready. A push of 22.2222 x speed along
        the facing (x 0.4 on soul sand) and 1.4285 x jump strength (plus Jump Boost) up, times the jump bar
        (0.4-1.0, as a horse's jump). Then 55 ticks of cooldown; the dash-ready sound when it ends. A sprinting
        rider goes 0.1 faster while the dash is ready. The client steering the camel dashes (1.12.2 moves a
        steered mount on its rider's client, as 1.20 does); the server plays the dash sound and sets the synced
        dashing flag, which starts the cooldown on every side. 1.20's client never sends the dash key during the
        cooldown; 1.12.2's player does, so the server ignores it then.
    *   **Two riders**: the driver 0.5 forward, the second 0.7 back (an animal 0.2 further forward), at the body's
        height as the sit and stand animations move it (1.20's `getBodyAnchorAnimationYOffset`); a player's feet
        0.6 below the seat (1.20's player vehicle attachment). Only the first rider steers, and only when
        saddled (1.12.2's horse lets any first rider). Babies carry nobody and take no saddle.
    *   **Interaction** (1.20's `mobInteract`): sneaking opens the inventory; a saddle goes straight on (1.12.2's
        horse opens its screen instead); cactus heals 2, makes a grown camel fall in love and grows a baby by
        10 seconds; otherwise the player gets on.
    *   Its sounds are 1.20's eleven camel events; steps on sand, red sand, suspicious sand and concrete powder
        use the sand step (`#camel_sand_step_sound_blocks`).
*   `CamelBrain`: 1.20's `CamelAi` - the core and idle behaviors in 1.20's priority order (swim, panic at 4x
    speed after standing up, look at a target, walk to the walk target, the two cooldowns; look at players now
    and then, make love, follow cactus or a grown camel, glance around, and one of stroll / walk to what it
    looks at / sit or stand after 20 seconds in a pose / do nothing), with the sensors every 20 ticks. The same
    machine as the sniffer's: one list, every behavior free to run beside the others, timings drawn as 1.20
    draws them. Reached checks allow for 1.12.2 parking a wide mob a block off its path node.
*   Body and look: 1.20's body rotation control with the camel's 30 degrees, still while it refuses to move;
    the look helper does nothing while it is steered.
*   Walk animation: 1.20's camel - six times the distance moved, eased by 0.2, none while sitting or dashing.
*   **Village camels** (`CamelVillages`, the one place this stream generates anything): 1.20 puts one camel at
    every desert town center, through a jigsaw in each of its three `desert_meeting_point` templates. Village &
    Pillage's 1.14 town centers are the same templates without that jigsaw, so on chunk population the camel is
    placed exactly where 1.20 would put it, read from the world's saved village data (its pieces save pool,
    element, position and rotation) and the element's name from Village & Pillage's own `pools.json`, both by
    name; zombie villages get none. Without it, vanilla 1.12.2 desert villages get one camel at the first
    corner of the well where it fits. No random draws, so seeds give the same camels. Any other terrain
    generator calls `CamelVillages.spawnInChunk(world, chunkX, chunkZ)` for the chunks it populates.
*   *Two by Two*: the loaded `minecraft:husbandry/bred_all_animals` gets 1.20's `minecraft:camel` criterion
    (bred two camels) as one more requirement, by reflection, rechecked each server tick so `/reload` keeps it.
    The same way the sniffer stream adds the sniffer; each adds only its own criterion.
*   Client (`camel/client`): 1.20's `CamelModel` (every cube, texture offset, pivot and the 0.05 grown saddle and
    bridle; saddle and bridle shown when saddled, reins when ridden too; the head tipped back by up to 45 degrees
    as the dash cools down), its six keyframe animations generated from 1.20.6's `CamelAnimation` by
    `tools/camel/animations.py` (walk, sit, sit pose, stand up, idle, dash; 261 keyframes), 1.20's texture,
    shadow 0.7. The jump bar: 1.20 draws the cooldown sprite and does not charge while the dash cools down; here
    the charge is reset after each player tick on a cooling camel and the cooldown sprite (1.20.6's
    `hud/jump_bar_cooldown`) is drawn over the bar.
*   Assets: `python tools/camel/generate.py` (texture, cooldown sprite, sounds, name, subtitles).

## Tested headlessly

`./gradlew runServer -Pselftest=all` (port 25920), all streams on this branch: **`pass=500 fail=0`**;
`-Pselftest=camels` five runs in a row **`pass=92 fail=0`**; with Village & Pillage 2.5.1 in `eclipse/mods`
**`pass=93 fail=0`**. `check_assets.py`: **0 errors**. Release jar built with `--rerun-tasks`: `func_` names,
no self-test classes.

*   Registration, spawn egg, attributes, size and eye height standing, sitting and as a baby; spawned camels are
    grown.
*   Riding: saddling by right-click (saddle used up); two players get on, a third doesn't; only the first steers;
    seats at 1.20's points standing and sitting, a player's feet 0.6 below; an animal's back seat; the rider's
    forward key stands a sitting camel up; an unsaddled camel carries but isn't steered and can't dash; babies
    take no saddle or riders; the saddle is saved.
*   Dash: ridden speed with and without sprinting; the push and lift at a full bar; 55-tick cooldown; the dash key
    ignored during the cooldown; the dashing flag ends on the ground; the server's dash starting the cooldown.
*   Sitting: 40 ticks down, 52 up, no sliding while sitting, standing when hurt and in water, saved sitting; an
    idle camel in a pen sits down by itself (not before 20 seconds) and stands up again.
*   Breeding: cactus eaten, love, one baby from two camels, parents' 6000-tick wait, cactus growing a baby by
    10 seconds and healing 2; *Two by Two* has the camel criterion.
*   Villages: 1.20's camel spots for all three meeting points and a turned one; none for a zombie town center;
    one camel from a saved Village & Pillage desert village and from a vanilla desert village's well, none for a
    zombie or plains village; Village & Pillage's real `pools.json` read by name when it is installed.

## Not tested

*   Anything drawn: the model, its animations, saddle/bridle/reins, the baby, the cooldown bar over the jump bar.
    The dev client was not run.
*   A real client steering and dashing (the dash's push, charging the bar, the cooldown freeze on the bar), the
    second rider's view, the inventory screen.
*   A camel in a desert village generated by the game (only saved village data built by the test); villages from
    Village & Pillage's terrain adapters (OTG and the others) beyond the `Village`/`OTGVillage` names.
*   Leash offset: 1.12.2 draws the leash at its own fixed point, not 1.20's `getLeashOffset`.
*   1.20 lets camels path over fences (`setCanWalkOverFences`); 1.12.2's pathfinder has no such option.
*   Game events (vibrations) for sitting, standing, dashing and eating: Wild Update's, not hooked here.
