# Stream W: wolves

Package `wolf` (client parts in `wolf.client`). Config `wolves` (`enabled`). Entity ids 20-24 are unused (the stream
adds no entity: 1.12.2's own wolf stays the wolf). Packet ids 70 (wolf state) and 71 (armor broke); 72-74 unused.
The armadillo scute comes from stream Ar by name (`Owners.ours("armadillo_scute")`).

## Changelog section

*   **Wolf armor**: crafted from six armadillo scutes. The owner of a tamed adult wolf puts it on by using it on
    the wolf. It takes every hit in the wolf's place - except drowning, starving, suffocating, magic, wither,
    thorns, freezing and the void - until it breaks, cracking in three stages as it wears. It gives the wolf 11
    armor points. With the wolf sitting, its owner repairs it with a scute, an eighth of its durability each (or
    in an anvil); shears take it off again. It drops when the wolf dies. Dye it like leather armor, in the
    crafting grid, and wash the dye off in a cauldron.
*   **Eight new wolf variants**, by biome: ashen (snowy taiga), black (mega taiga), chestnut (mega spruce
    taiga), rusty (jungles), spotted (savannas), striped (badlands), woods (forest) and snowy (the grove, with
    Caves & Cliffs). Taiga wolves and wolves anywhere else are the familiar pale wolf, as are wolves already in a
    world. Pups take one parent's variant.
*   **Wolves spawn** in savanna plateaus, jungle edges and wooded badlands plateaus too, and on podzol, coarse
    dirt and snow as well as grass.
*   **Tamed wolves**: 40 health (tamed wolves already in a world have their health doubled once), food heals
    twice as much, whining below half health, and damage from mobs is no longer halved - so a snow golem's
    snowballs now only push them.
*   Advancements *Good as New*, *Shear Brilliance* and *The Whole Pack*.

## How it works

*   **Source**: 1.20.6's `Wolf`, `WolfVariant(s)`, `AnimalArmorItem`, `Crackiness`, `WolfRenderer`,
    `WolfArmorLayer`, `WolfModel`, `ArmorDyeRecipe`, `DyedItemColor`, `CauldronInteraction`, `OverworldBiomes`,
    `BiomeData`, `FixWolfHealth`, the item and damage-type tags, the recipe and the three advancements, decompiled
    from the client jar remapped to Mojang's names (`G:/MCDownloads/tt-wolves-decomp`, `out*`; the remapped jar
    is stream Ar's `G:/MCDownloads/tt-armadillo-decomp/client-named.jar`). 1.12.2's `EntityWolf` and `RenderWolf`
    for what is already there.
*   **No new wolf class.** 1.12.2's `EntityWolf` stays; what 1.20.5 adds lives in a capability, **`WolfData`**
    (`ForgeCaps/trailsandtalesbackport:wolf`: `variant` as 1.20's id, `body_armor`, `health40`), and the rest is
    done from Forge events in **`WolfContent`**. Nothing is patched.
*   **Variants** (`WolfVariant`): 1.20.5's nine, with 1.20's biome sets matched by registry path in any namespace,
    under 1.12.2's names and 1.18's: pale `taiga(_hills)`, `mutated_taiga`; ashen `taiga_cold(_hills)`,
    `mutated_taiga_cold`; black `redwood_taiga(_hills)`; chestnut `mutated_redwood_taiga(_hills)`; woods
    `forest(_hills)`; spotted every savanna; rusty every jungle (and `bamboo_jungle(_hills)`); striped every mesa;
    snowy `grove` (Caves & Cliffs' biome, by path only - null-safe, no link to that mod). Anything else is pale.
    A wolf gets its variant when it joins the world: one that is not yet in a chunk is being spawned (natural,
    chunk generation, spawn egg, `/summon`), and takes its biome's (1.20.5's `finalizeSpawn`); one loaded from
    its chunk without a variant was saved before this mod and stays pale. A pup takes one parent's at random
    (`BabyEntitySpawnEvent`, before it joins).
*   **Client**: the server sends a wolf's variant and armor (packet 70) to each player who starts tracking it and
    to every tracker when either changes. **`RenderWolfVariant`** replaces 1.12.2's wolf renderer: the variant's
    wild, tame and angry textures (pale uses vanilla's own files), 1.20's tail height from the share of health
    lost, and the armor layer before the collar. **The armor layer** (`ModelWolfArmor`: 1.12.2's wolf model with
    every cube inflated by 0.2, as 1.20's `WOLF_ARMOR` layer) draws the armor, then its overlay in the dyed color
    (only when dyed), then the crack texture for the armor's crack level, blended.
*   **Wolf armor** (`ItemWolfArmor`): 64 durability, one per stack, not enchantable at the table or with books,
    repaired with scutes in an anvil. Color in `display.color` as 1.12.2's leather (1.12.2's tooltip shows "Dyed",
    or the hex color with advanced tooltips). The item model shows the overlay layer only when dyed (an item
    property `dyed` picks `wolf_armor_dyed`), because 1.20.5 hides it with a transparent tint that 1.12.2's item
    tinting cannot express. Recipe: 1.20's shape (`X  /XXX/X X`), registered only when the scute exists.
    `RecipeWolfArmorDye` is 1.20.5's `ArmorDyeRecipe` + `DyedItemColor.applyDyes` (1.20.6's dye colors; any ore
    dictionary dye counts). A cauldron with water washes the color off, one level, *Armor cleaned* stat; not
    while sneaking.
*   **Putting it on, taking it off** (`PlayerInteractEvent.EntityInteract`, before 1.12.2's own handling): the
    owner of a tamed adult wolf without armor; the equip sound. Shears: the owner, any wolf wearing armor, unless
    it has Curse of Binding (creative ignores that); 1 shears durability, the unequip sound, the armor dropped at
    the wolf, *Shear Brilliance*. Scute: the owner, the wolf sitting, the armor damaged; one scute (even in
    creative, as 1.20.5), an eighth of the durability (8), the repair sound, *Good as New* when it reaches full.
    Each worn armor gives 1.20's body-slot modifier, 11 armor.
*   **Damage** (`LivingHurtEvent`, highest priority, in `damageEntity`):
    *   1.12.2's wolf first turns damage from any mob that is not a player or an arrow into `(d + 1) / 2`; 1.20.5
        has no such step. This handler is the first point after it and turns it back: on a fresh hit 1.12.2 has
        just stored the halved amount as `lastDamage` and reset the hurt timer, so the damage is `2h - 1`; inside
        the hurt cooldown only the excess over the last hit arrives (the difference of two halved amounts), so it
        is doubled. A snow golem's snowball (0) arrives as 0.5 and becomes 0 again: knockback only.
    *   With armor on, damage not in 1.20.5's `#bypasses_wolf_armor` (by damage type name: `outOfWorld`,
        `genericKill`, `cramming`, `drown`, `dryout`, `freeze`, `inWall`, `indirectMagic`, `magic`,
        `outsideBorder`, `starve`, `thorns`, `wither`) goes to the armor: `ceil(damage)` durability, none to the
        wolf. Crossing into another crack level (1.20.5's `Crackiness.WOLF_ARMOR`: below 95%, 69% and 32% left)
        plays the crack sound with 20 scute particles. Used up, it breaks: gone, and every tracker plays 1.20's
        entity event 65 (break sound, five item particles; packet 71). The wolf's hurt sound becomes the armor's
        damage sound (swapped in `PlaySoundAtEntityEvent`, which 1.12.2 fires without the entity, so the
        absorbing wolf is remembered for that one sound).
    *   Damage the armor does not take but armor blocks (thorns) wears it by a quarter, at least 1 (1.20.5's
        `hurtArmor`), and the 11 armor points apply.
*   **Death**: the armor drops with the other loot (`LivingDropsEvent`, with `doMobLoot`).
*   **Tamed wolves**: 1.12.2 sets a tamed wolf's max health to 20 whenever it is tamed or loaded; on joining and
    every update it is set to 40, and the first time a wolf gets it its health is doubled (`health40` keeps it
    from happening twice): 1.12.2's taming leaves it at 20, 1.20.5 sets 40; a wolf from an older world keeps its
    share, as 1.20.5's `FixWolfHealth`. A tamed pup has 40 of 40 and one parent's collar color (1.20.5's
    `getBreedOffspring`). Feeding meat heals twice its hunger value and works up to full health (1.12.2 stopped
    at 20 and healed its hunger value). Its pant becomes a whine below 20 health (1.12.2: below 10), decided while
    the wolf updates. The tail is drawn from the share of health lost.
*   **Spawning** (`WolfSpawns`): 1.12.2 already has 1.20's wolves in every taiga (8, 4-4) and the forest (5, 4-4).
    Added: the savanna plateau (8, 4-8), the jungle edge and jungle edge M - 1.18's sparse jungle (8, 2-4) - and
    the mesa plateau F and its M (wooded badlands, 2, 4-8); the grove (1, 1-1) and 1.18-named taiga, snowy taiga,
    old growth taigas and forest get 1.20's entry when they have no wolves. Spawn rule: 1.20.5's
    `#wolves_spawnable_on` (grass, snow, snow block, coarse dirt, podzol) with raw light above 8, for natural,
    chunk-generation and spawner spawns; the rest of 1.12.2's check (path weight, the block's own rule, room)
    stays, as 1.20's is the same.
*   **Advancements** (our files under husbandry, parent *Best Friends Forever*, granted by code because 1.12.2 has
    no trigger for using an item on a mob and its tame trigger knows no variants): *Good as New*, *Shear
    Brilliance*, and *The Whole Pack* (challenge, 50 experience, one criterion per variant, `minecraft:ashen`
    and so on, granted on `AnimalTameEvent` at the lowest priority, when nothing has stopped the taming).
*   **Assets** (`tools/wolf/generate.py`): the 24 variant textures, the 5 armor layer textures, the item textures,
    the 6 sound events with their files and subtitles, the advancements and names, from 1.20.6.

## Tested headlessly

`./gradlew runServer -Pselftest=wolf` (port 25922): **`pass=141 fail=0`** (the first run's 3 failures were test
mistakes: 1.20's shaped recipes do match mirrored, feeding at 30 clamps at 40, a client-only method).
`-Pselftest=all`: **`pass=3588 fail=0`** (all 14 streams). `check_assets.py`: **79 blocks, 1140 states, 90 item
models, 703 sound files, 0 errors**; every texture the renderer names exists, entity textures 64 x 32. Release jar
built last with `--rerun-tasks`: reobfuscated (`func_` names in the wolf classes, no dev names), no self-test
classes.

*   Registration: name, key, one per stack, 64 durability, not enchantable at the table or with books, repaired
    with scutes (not leather), the 6 sound events.
*   Recipes: six scutes in 1.20's shape (and mirrored, as 1.20's); red dye gives 1.20's red (`B02E26`); red and
    yellow mix as `applyDyes` does; a dyed armor's color counts as one dye; armor and a stick make nothing. The
    cauldron washes a dyed armor (one level used), not an undyed one.
*   Variants for every 1.12.2 biome group (taiga and hills pale, cold taiga ashen, mega taiga black, mega spruce
    chestnut, forest woods, birch forest pale, savannas spotted, jungles rusty, mesas striped, plains pale), the
    grove by name (and Caves & Cliffs' when present), 1.18 names, ids and lookup.
*   Spawns: the three added biomes with 1.20's numbers, taiga and forest keeping their one entry, none in plains,
    the plain savanna or plain badlands, added once; the spawnable blocks; the spawn check on lit podzol (allowed)
    and stone (denied).
*   Joining: a new wolf takes its biome's variant; a wolf saved without the data and loaded from its chunk is
    pale; an old tamed wolf (20 max, 15 health) loads as 40 and 30, and saved and loaded again stays 30 of 40.
*   Taming with bones through the player's interaction: 40 of 40; *The Whole Pack* counts the variant. Feeding:
    cooked beef heals 16, also above 20 health, up to full; a full wolf is not fed.
*   Damage: a zombie's 6 is 6; within the hurt cooldown a 9 deals 3 more; a player's 5 is 5; fall unchanged; a
    snow golem's snowball does nothing.
*   Armor: only the owner puts it on, not a second one, not on a pup or a wild wolf; 11 armor points; a zombie's
    3.5 costs 4 durability and no health, low cracks below 95%; falls absorbed; magic and drowning go past;
    thorns goes past and wears it by 2; the crack thresholds at 95/69/32%; which damage types go past. Repair:
    not while standing, 8 a scute, to full, *Good as New*, no scute used on an undamaged armor. Shears: off,
    1 wear, armor points gone, dropped, *Shear Brilliance*; not with Curse of Binding. Breaking: the last hit is
    still absorbed, then no armor and no armor points. Dropped on death.
*   Sounds (listened for): the wolf's hurt sound without armor, the armor's damage sound with it, the crack
    sound on crossing a level, the wolf's own sound for magic; a pant at 15 health becomes a whine, at 25 stays.
*   Breeding: over 40 pups, both parents' variants appear and only theirs; tamed pups have 40 of 40 and a parent's
    collar.
*   Saving: variant (as `minecraft:chestnut`), armor (worn and dyed) and armor points survive a save and load.
    The state packet round-trips; an unknown variant number reads as pale.
*   Advancements: all three under *Best Friends Forever*; *The Whole Pack* has nine criteria, each required, and a
    reward.

## Not tested

*   Anything drawn: the variant textures on wolves, the tail height, the armor layer's fit, overlay color and
    crack textures, the item icon dyed and undyed, the break particles. Sounds as heard.
*   Packets reaching a real client (only their encoding is tested), natural spawning in real biomes, taming by a
    real player in game.

## Left out, and why

*   **Pack variant**: 1.20 gives a whole spawned pack the first wolf's variant (`WolfPackData`); 1.12.2 spawns
    each wolf of a pack on its own, so each takes the variant of the biome it stands in. They differ only for a
    pack straddling a biome border.
*   **`/summon` with a variant tag** or a variant in a spawn egg's data: 1.12.2 cannot pass 1.20's `variant` tag
    to the capability; a summoned wolf takes its biome's.
*   **Wooded badlands' creature spawn chance** (0.04, 1.12.2's mesa plateaus use the default): biome-wide, not the
    wolf's.
*   **Wild wolves' attack damage** (1.12.2: 2, 1.20: 4) and the dye-the-collar-owner-only check, the
    on-fire/freezing panic goal: older than 1.20.5, not in this stream's plan.
*   **Dispensers** do not put wolf armor on or shear it off: 1.20.5's do neither (only horse armor and saddles).
