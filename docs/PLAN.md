# Trails & Tales Backport: plan

Everything Java Edition 1.20.x added or changed (1.20 Trails & Tales through 1.20.6, so 1.20.5's armadillo,
wolf armor and wolf variants are in), grouped into streams, each in its own package, config category and
branch.

Source: minecraft.wiki's *Java Edition 1.20*, *1.20.2*, *1.20.3* and *1.20.5* pages and the pages they link.
Where current Java behaves differently from 1.20, current Java wins (marked **now:**). Numbers the wiki leaves
out come from the 1.20.6 client jar's own code (Mojang's mappings ship beside it). Assets come from the 1.20.6
client jar and asset index 16 in `G:\MCDownloads\trails-1.20.6` through `tools/assets.py`.

Anything behind 1.20.x's *Update 1.21* experiment (crafter, trial chambers, breeze, tuff and copper blocks,
mace, ominous trials, the 1.20.5 effects Infested/Oozing/Weaving/Wind Charged) is Tricky Trials', not ours.
The 1.20.2 villager trade rebalance is still an experiment in current Java and stays out.

## Scope at a glance

| In | Out (and why) |
| --- | --- |
| Every new block, item, mob, biome, structure, advancement and music disc of 1.20.x; the changes to existing blocks, items and mobs that a player sees | Commands, data packs, data components, display entities, the debug and options screens: 1.12.2 can't take them and players never see them |
| Archaeology in the older structures we can reach (desert pyramids, desert wells; ocean ruins through Update Aquatic) | Sculk sensor vibration-frequency rework: Wild Update's sensor, its job (we only tell its owner) |
| Hanging signs for every wood, ours and the family's (Rule 2) | Diamond ore distribution in deepslate (1.20.2): Caves & Cliffs' deepslate |
| Smithing templates on Village & Pillage's smithing table (Rule 2, through Backport Core) | Ambient music tracks (nice to have; listed under *Later*) |

## Who owns what

Family Rule 1, as made exact in the family brief: content belongs to the mod for the version line it first
shipped in outside experiments. Ours: everything 1.20.x added, including the bamboo wood set, hanging signs
(also for other mods' woods), the piglin head and 1.20.5's armadillo, scutes, wolf armor and wolf variants.

What we need from other family mods, and what happens without them:

| Thing | Owner | Without the owner |
| --- | --- | --- |
| Bamboo plant and item (bamboo block, planks, scaffolding recipes) | Village & Pillage (1.14) | We register an identical copy of the plant, sapling and item, and bamboo in jungles (no bamboo jungle biome, no pandas). **Still open 5.** |
| Smithing table | Village & Pillage (1.14) | We register an identical copy, with 1.20's three slots |
| Stripped logs (hanging sign recipes) | Village & Pillage (1.13 content) | Update Aquatic's copies if installed; otherwise the recipe takes the log itself |
| The five woods' standing signs | Village & Pillage | Not needed by us |
| Honeycomb (sign waxing) | Buzzy Bees (1.15) | Waxing is off |
| Chains (hanging sign recipe) | Nether Update (1.16) | We register an identical copy (`chain`, axis property only until waterlogging) |
| Crimson and warped stems, stripped stems | Nether Update | No crimson or warped hanging signs |
| Netherite ingot and gear; bastions; piglins | Nether Update (or any netherite mod Core finds) | Netherite upgrade template unused; trim material netherite only with some netherite mod; piglin head absent |
| Amethyst shard, copper ingot, glow ink sac, moss block, grove | Caves & Cliffs (1.17/1.18) | Those trim materials absent; calibrated sensor recipe absent; sniffer eggs hatch at the normal rate everywhere; glow ink on hanging signs absent |
| Sculk sensor and the vibration system | Wild Update (1.19) | No calibrated sculk sensor (**Still open 4**) |
| Mangrove stem, stripped mangrove log | Wild Update | No mangrove hanging sign |
| Chest boats | Wild Update | Our cherry chest boat and bamboo chest raft use our own chest-boat entity (see `CORE_REQUESTS.md` 4) |
| Ancient cities (ward, silence trims) | Wild Update | Those two templates come from our fallback loot (**Still open 3**) |
| Ocean ruins and shipwrecks (sherds, sniffer egg, coast trim) | Update Aquatic (1.13) | Fallback loot (**Still open 3**) |
| Mob ambient sounds for heads (piglin) | Nether Update | — |

Each lookup goes through `common/Owners` (`block`, `item`, `theirs`, `shouldRegisterCopy`), backed by Backport
Core's `BlockOwnership`. The blocks this mod ships first are declared there (`BlockOwnership.declare`), so
Tricky Trials can find our sherds, brush and decorated pot.

## Streams

Each stream owns one package under `com.exiledradio.trailsandtalesbackport`, one config category with an
`enabled` switch (already in `ModConfig`), one block of five packet ids and, for mobs, one block of entity
ids. No stream calls another's classes; another stream's item is looked up by name with `Owners.ours(name)`,
which returns null when that stream is off.

| # | Stream | Package | Config | Entity ids | Packet ids | Needs |
| --- | --- | --- | --- | --- | --- | --- |
| C | Cherry wood, leaves, sapling, pink petals | `cherry` | `cherry` | none | 5-9 | nothing |
| B | Bamboo wood set (+ bamboo copy) | `bamboo` | `bamboo` | none | 10-14 | Still open 5 (default: copy) |
| Bo | Boats and rafts | `boat` | `boats` | 1-4 | 15-19 | C, B by name |
| G | Cherry grove biome | `biome` | `cherryGrove` | none | 20-24 | **Still open 1** |
| H | Hanging signs (+ chain copy) | `hangingsign` | `hangingSigns` | none | 25-29 | nothing |
| Sg | 1.20 sign changes | `sign` | `signs` | none | 30-34 | **Still open 6** |
| K | Chiseled bookshelf | `bookshelf` | `chiseledBookshelf` | none | 35-39 | nothing |
| A | Archaeology: brush, suspicious blocks, sherds, decorated pot | `archaeology` | `archaeology` | none | 40-44 | nothing |
| St | Structures: trail ruins, desert pyramid and well archaeology, Relic | `structure` | `structures` | none | 45-49 | Core's template loader for trail ruins |
| N | Sniffer, egg, torchflower, pitcher plant | `sniffer` | `sniffer` | 5-9 | 50-54 | nothing |
| Ca | Camels | `camel` | `camels` | 10-14 | 55-59 | nothing |
| Sm | Smithing templates and armor trims | `smithing` | `smithing` | none | 60-64 | **Still open 2**, Core (`CORE_REQUESTS.md` 1) |
| Ar | Armadillo and scutes | `armadillo` | `armadillos` | 15-19 | 65-69 | nothing |
| W | Wolf armor, variants, tamed wolf changes | `wolf` | `wolves` | 20-24 | 70-74 | Ar by name |
| Sc | Calibrated sculk sensor | `sculk` | `calibratedSculkSensor` | none | 75-79 | **Still open 4** |
| Hd | Piglin head, heads on note blocks | `heads` | `heads` | none | 80-84 | nothing |
| X | Other changes | `misc` | `misc` | none | 85-89 | nothing |

Packet ids 0-4 are `common`'s. Potions, enchantments and sounds have no numbers to collide on.

### C. Cherry wood, leaves, sapling, pink petals

- **Cherry wood set**: log, wood, stripped log, stripped wood, planks, stairs, slab, fence, fence gate, door,
  trapdoor, pressure plate, button, sign (standing and wall). Same recipes, fuel, flammability and ore names
  (`logWood`, `plankWood`, `slabWood`, `stairWood`, `fenceWood`, `fenceGateWood`, `doorWood`, `trapdoorWood`)
  as the vanilla woods, so other mods' wood recipes take it. Axe stripping (our own handler; V&P's and Update
  Aquatic's handlers only know their own logs).
- **Cherry wood sound type** (1.20's `cherry_wood` sounds) for every cherry block.
- **Cherry leaves** (decay, Silk Touch/shears, saplings 1 in 20, sticks; **now:** no apples) dropping the
  `cherry_leaves` falling-petal particles below them (client); **cherry sapling** growing a cherry tree
  (1.20's cherry trunk and blossom placer, with the bee-nest chance left to Buzzy Bees if it registers a
  hook) and **potted cherry sapling**.
- **Pink petals**: 1-4 petals, 4 facings, bone meal adds one (at 4 drops one), placed on grass/dirt; hitbox
  grows with petals (1.20.2); compost 30% (Village & Pillage's composter).
- Future MC pairs for its cherry blocks if Future MC has them (checked against its registry when built).

### B. Bamboo wood set

- **Block of bamboo** (9 bamboo, axis), **block of stripped bamboo**, **bamboo planks** (2 per block), **bamboo
  mosaic** (2 bamboo slabs), stairs and slabs of both, fence, fence gate, door, trapdoor, pressure plate,
  button, sign. Bamboo wood sounds.
- Bamboo comes from Village & Pillage. Without it, an identical copy of its plant, sapling and item, and its
  jungle generation, registered only when it is absent (default; **Still open 5**).

### Bo. Boats and rafts

- **Cherry boat**, **cherry chest boat**, **bamboo raft**, **bamboo chest raft** (raft: its own model,
  sits higher). Our own boat entity subclassing vanilla's `EntityBoat` with a wood field, renderer and
  models from 1.20.6; the chest forms with 27 slots, opened by sneaking, dropping their contents.
- Chest boats are Wild Update's mechanic: until Backport Core has the shared boat registry
  (`CORE_REQUESTS.md` 4) ours is a local shim with the same behavior.

### G. Cherry grove biome (Still open 1)

- 1.20's cherry grove: grass and foliage color, sky and fog, water color `#5DB7EF`, cherry trees, pink petals,
  pigs, sheep, rabbits and (with Buzzy Bees) bees, hilly terrain (1.12.2 has no 1.18 mountains; the extreme
  hills height variation is the nearest). *Adventuring Time* gets it through the biome registry on its own.
- How it gets into the world is the user's call (see *Still open*).

### H. Hanging signs

- A hanging sign and a wall hanging sign per wood: oak, spruce, birch, jungle, acacia, dark oak, cherry,
  bamboo always; crimson and warped with Nether Update; mangrove with Wild Update (Rule 2: other mods' woods,
  our signs). 2 chains + 6 stripped logs (bamboo: stripped bamboo blocks) make 6.
- Hanging under a block (attached by chains or V-shaped when the block above is not full), on a wall bracket,
  16 rotations when attached; not placed unsupported; smaller text area; editing, dyeing and (with Caves &
  Cliffs) glow ink like signs; front and back text as 1.20 signs. Tile entity and renderer of our own.
- **Chain** copy when Nether Update is absent.
- Waterlogging later (list at the end).

### Sg. 1.20 sign changes (Still open 6)

- Right-clicking a placed sign edits it (not in Adventure mode, not while waxed); text on the back face,
  edited from behind; dye and glow ink per side; **waxing** with honeycomb (Buzzy Bees) stops editing.
- Applies to vanilla's signs, our cherry and bamboo signs and hanging signs, and (Rule 2) Village & Pillage's
  five wood signs. Needs our renderer in place of vanilla's sign renderer and the back text stored in a
  capability on the sign's tile entity. How far to go on other mods' signs is **Still open 6**.

### K. Chiseled bookshelf

- 6 planks + 3 wooden slabs; six slots (books, written and writable books, enchanted books, knowledge book);
  clicking a slot's face places or takes that book; comparator output is the last slot touched (1-6); hoppers
  and droppers insert and extract (1.20); no enchanting power; drops itself only with Silk Touch, else its books
  (**now:** as 1.20.x). Advancement *The Power of Books*.

### A. Archaeology

- **Brush** (stick, copper ingot, feather; **without Caves & Cliffs' copper**, the recipe takes an iron nugget:
  **Still open 3** has the alternatives), 64 durability, brushing animation (client), dispenser brushing
  armadillos (with stream Ar).
- **Suspicious sand** and **suspicious gravel**: tile entity with a loot table and a dusted stage 0-3; brushing
  for 96 ticks reveals the item, finishes into sand/gravel; falls like sand when unsupported but breaks when it
  lands; drops nothing when mined.
- **20 pottery sherds** (angler, archer, arms up, blade, brewer, burn, danger, explorer, friend, heart,
  heartbreak, howl, miner, mourner, plenty, prize, sheaf, shelter, skull, snort; flow, guster and scrape are
  1.21's).
- **Decorated pot**: four faces from sherds or bricks (crafted in a diamond), drawn with the sherd patterns;
  holds one stack of one item (1.20.3), hoppers in and out, comparator output; wobbles when used; breaks into
  its sherds unless Silk Touch or empty-handed; arrows and other projectiles shatter it; pushed by pistons it
  drops.
- Loot tables for every archaeology site as 1.20.6 has them (`archaeology/desert_pyramid`, `desert_well`,
  `ocean_ruin_warm`, `ocean_ruin_cold`, `trail_ruins_common`, `trail_ruins_rare`).
- Advancements *Respecting the Remnants*, *Careful Restoration*.

### St. Structures

- **Desert pyramid**: 1.20 added an underground sand room with suspicious sand. 1.12.2's pyramid is built in
  code, so this is done after the pyramid's piece is placed, by finding it through the structure start and
  digging the 1.20 room under it; no template loader needed.
- **Desert well**: one suspicious sand under the water, placed right after vanilla's well feature.
- **Trail ruins**: 1.20's jigsaw structure (tower, roads, buildings) in taiga, birch forest, jungle and (with
  Caves & Cliffs) old-growth birch, half buried in gravel, with suspicious gravel. Needs Backport Core's modern
  structure template and jigsaw loader (`CORE_REQUESTS.md` 2) and its terrain adapters.
- **Ocean ruins** (Update Aquatic's): their suspicious sand and gravel through a structure hook on Core's
  loader (`CORE_REQUESTS.md` 2); the sniffer egg is in warm ruins' table.
- **Music disc "Relic"** (trail ruins rare loot) with its sound.
- **Smithing template loot** in every structure the 1.12.2 game has: desert pyramid (dune), jungle temple
  (wild), nether fortress (rib), end city (spire), stronghold library (eye), woodland mansion (vex), and through
  `LootTableLoadEvent` in the family's structures by table name when installed: pillager outpost (sentry,
  V&P), shipwreck (coast, UA), bastion (snout and netherite upgrade, NU), ancient city (ward, silence, WU),
  trail ruins (host, raiser, shaper, wayfinder, ours). Elder guardians drop the tide template.

### N. Sniffer

- **Sniffer** (14 health, slow, sniffs and digs grass, dirt, podzol, coarse dirt, mud and moss (family
  blocks by name) for torchflower seeds or pitcher pods, 8-minute cooldown, remembers dug spots), breeding with
  torchflower seeds lays a **sniffer egg**; the **snifflet** grows up in 20 minutes.
- **Sniffer egg** hatches in 3 stages, 20 minutes (10 on Caves & Cliffs' moss), cracks louder on moss.
- **Torchflower seeds** → torchflower crop (2 stages) → **torchflower** (flower, orange dye, potted form).
- **Pitcher pod** → pitcher crop (5 stages, two blocks tall from stage 3) → **pitcher plant** (tall flower,
  cyan dye, 2 cyan dye). Bees pollinate (Buzzy Bees by name).
- Advancements *Smells Interesting*, *Little Sniffs*, *Planting the Past*. 1.20's additions to *Two by Two*
  and *A Seedy Place* go into vanilla 1.12.2's own advancements of those names through their criteria.

### Ca. Camels

- **Camel** (32 health), saddled with a saddle, two riders,
  sitting at random, **dash** (jump key, cooldown, shown on the jump bar), tall enough that zombies don't reach
  the rider (1.20.2 reach change), breeding with cactus, panic on lead (1.20.2), spawning in desert villages
  (Village & Pillage's: through `CORE_REQUESTS.md` 6, else one camel by a vanilla 1.12.2 desert village's well).

### Sm. Smithing templates and armor trims (Still open 2)

- **Netherite upgrade** template and the 16 1.20 **armor trim templates** (coast, dune, eye, host, raiser, rib,
  sentry, shaper, silence, snout, spire, tide, vex, ward, wayfinder, wild; bolt and flow are 1.21's). Each
  duplicates with 7 diamonds and its block, as 1.20.6's recipe files say (read from the jar by
  `tools/smithing`); a block another family mod owns (cobbled deepslate, blackstone) is looked up by name and
  the recipe is left out without it.
- **Trims** on any armor: pattern from the template, material from the addition: iron, gold, diamond,
  emerald, lapis, redstone, quartz, and with the family's items amethyst, copper and netherite. Stored as NBT
  (`Trim: {pattern, material}`), shown in the tooltip, drawn on the worn armor through our own armor layer
  added to every biped renderer (client), and on the item icon through a wrapped item model (client). Trim
  textures are generated at build time from 1.20.6's grayscale trims and palettes (`tools/smithing`).
- **Netherite upgrades need the template** when this mod is installed (1.20), on whichever smithing table is
  in the game. This changes Village & Pillage's table and Core's registry: design in `CORE_REQUESTS.md` 1;
  nothing is built against them until the user says yes.
- Advancements *Crafting a New Look*, *Smithing with Style*.

### Ar. Armadillo

- **Armadillo** (12 health) in savanna and badlands (mesa) on red sand, coarse dirt and terracotta; rolls up at
  threats (sprinting players, mounted players, undead, whoever hurt it) and unrolls 3 s after; rolled up it takes
  reduced damage; drops a **scute** every 5-10 minutes; brushing gives a scute (16 brush durability); breeds
  with spider eyes; spiders and cave spiders avoid it unless rolled up.
- Dispensers brush armadillos. Advancement *Isn't It Scute?*; *Two by Two* needs armadillos.

### W. Wolves

- **Wolf armor** (6 scutes): equipped on a tamed adult wolf by its owner, absorbs damage until broken, cracks
  in 3 stages, repaired with scutes, dyeable, removed with shears, drops on death; drawn on the wolf (client).
- **Wolf variants**: pale (the existing wolf) and ashen, black, chestnut, rusty, snowy, spotted, striped and
  woods, chosen by biome with the closest 1.12.2 biomes (cold taiga → ashen, mega spruce taiga → chestnut,
  mega taiga → black, jungle edge → rusty, savanna plateau → spotted, mesa plateau F → striped, forest → woods,
  taiga → pale; grove → snowy with Caves & Cliffs). Stored on the wolf, kept by its pups by parent, drawn by
  our wolf renderer in place of vanilla's (client). Existing wolves stay pale.
- **Tamed wolf changes** (1.20.5): 40 health, no halving of environmental damage, food heals twice as much,
  snow golem snowballs do only knockback; 1.20.5 wolf spawning on coarse dirt and podzol.
- Advancements *Good as New*, *Shear Brilliance*, *The Whole Pack*.

### Sc. Calibrated sculk sensor (Still open 4)

- 1 sculk sensor + 3 amethyst shards; redstone into its back side sets the one vibration frequency it listens
  to; range 16, active 10 ticks. It is built on Wild Update's vibration system.

### Hd. Heads

- **Piglin head** (charged creeper kills a piglin, with Nether Update), standing and wall forms, ears flapping
  when powered or worn walking.
- **Mob heads on note blocks** (1.20): placed on top without sneaking, the note block plays the mob's ambient
  sound (skeleton, wither skeleton, zombie, creeper, ender dragon, piglin; player heads silent). Vanilla 1.12.2
  heads, so this is a note block event handler.
- **Powered dragon and piglin heads** animate (1.20.2's `powered`): client renderer for our piglin head; the
  vanilla dragon head through the same renderer replacement only if the user agrees (not a priority).

### X. Other changes

- Beds, carpets and wool dyeable to any color in the crafting grid (1.20).
- Bat's new model and texture (1.20.3): our renderer in place of vanilla's (client).
- Smaller visible 1.20.x rules that need no patches, collected as they are found: polished blackstone button
  break time (NU's block, Rule 2 note only), slimes and magma cubes affected by Jump Boost, shields blocking
  while looking straight up, chorus fruit resetting fall distance, arrows keeping custom names, sponge sounds.
  Each is checked against what 1.12.2 can do without patching before it is built.

## Rule 2 integrations (all soft: `Loader.isModLoaded` and names)

- **Village & Pillage**: composter (cherry leaves 30%, cherry sapling 30%, pink petals 30%, torchflower seeds
  30%, pitcher pod 30%, torchflower 65%, pitcher plant 85%); bamboo; smithing table and its template slot
  (Core); hanging signs for its woods are ours anyway; sentry template in pillager outposts; camels in desert
  villages; its trades and wandering trader (1.20's: none new outside the experiment).
- **Update Aquatic**: archaeology in ocean ruins, coast template in shipwrecks, sniffer eggs in warm ruins.
- **Buzzy Bees**: honeycomb waxing; bees pollinating torchflowers, pitcher plants and cherry leaves; bee nests
  on cherry trees (its hook, if it has one).
- **Nether Update**: chain, crimson and warped hanging signs, netherite (also any other netherite mod Core finds),
  snout and netherite-upgrade templates in bastions, piglin head.
- **Caves & Cliffs**: copper (brush), amethyst (calibrated sensor, trim), copper trim, glow ink on signs, moss
  (sniffer egg), grove (snowy wolves), deepslate in template duplication.
- **Wild Update**: sculk sensor (calibrated sensor), mangrove hanging signs, ward and silence templates in
  ancient cities, chest boats (Core registry), mud as a sniffer digging block.
- **Tricky Trials** reads ours: sherds, brush, decorated pot, smithing templates (declared in Core's table).

## Shared files and rules

Exactly as Update Aquatic Backport's plan:

- **`ModConfig.java`**: every category already exists; a stream edits only inside its own class. Category
  classes extend `Object`.
- **`NetworkHandler.java`**: register packets only with ids from your block, in id order.
- **`TrailsAndTalesBackport.java`**: at most one line per lifecycle hook, calling your package.
- **`client/ClientSetup.java`**: at most one line in `init`, calling your `client` class.
- **`selftest/SelfTestRunner.java`**: one line in `TESTS`.
- **`en_us.lang`, `sounds.json`**: written only by your generator through `tools/assets.py` (`merge_lang` /
  `merge_sounds`), which replaces your own `# --- <stream> ---` block and nothing else.
- **`common/`**: read freely; changing it is the integrator's job (this session).
- **Registration**: your own `@Mod.EventBusSubscriber` class, through `common/Content`.
- **Languages (Rule 4)**: every user-facing string is an `en_us.lang` key written by a generator; vanilla names
  through `assets.name(...)`; `python tools/lang/vanilla_keys.py` lists them as vanilla. The 18 languages come
  from the family's tool (`tools/lang/build_langs.py`, from Backport Core's `languages` branch) once it is
  merged; until then streams leave them to the integrator.
- **Docs**: `docs/streams/<stream>.md` with a changelog section, how it works, tested, not tested.

## Build and test

- `./gradlew build` never deploys unless `ttbDeployDir` is set in `~/.gradle/gradle.properties`.
- Backport Core: `deps/BackportCore-1.0.0-dev.jar`, copied from `BackportCore/build/libs` (not committed).
- Headless test: `./gradlew runServer -Pselftest=<stream>` on port 25880 (`eclipse/server.properties`); a
  stream worktree uses 25880 + its row number (1-17). Then `python tools/check_assets.py` with 0 errors, then
  `./gradlew build` last.
- Before every Gradle run: at least 8 GB free RAM. Gradle `-Xmx2G`, no daemon. Kill only our own server, by
  PID. Never `cleanRunServer`.
- Integration runs with other family mods: their release jars in `eclipse/mods` (FG2 remaps SRG jars at
  dev runtime), removed again afterwards.
- No mixins, no coremod, no ASM. A part that cannot be done without patching stops and goes to the user.

## Waterlogging (later pass, through Core's `Waterloggable` adapter)

Blocks 1.20 makes waterloggable: every hanging sign and wall hanging sign, the chain copy, the calibrated
sculk sensor, the decorated pot, cherry/bamboo signs, stairs, slabs, fences, trapdoors (like every wood),
pink petals no, sniffer egg no.

## Still open (the user's call)

1. **Cherry grove biome.** Adding a biome changes world generation, and RLCraft Revamped runs without modded
   biomes. Options: (a) a real biome added to the overworld list (weight configurable, default like extreme
   hills); (b) no biome: cherry groves as patches of cherry trees and pink petals inside extreme hills and
   forests; (c) both, (a) on by default. Recommended: (c).
2. **Smithing templates on Village & Pillage's table.** 1.20 makes netherite upgrades need a template. Doing
   that touches V&P's table (a third slot and 1.20's screen) and Core's registry. The design is in
   `CORE_REQUESTS.md` 1. Yes/no to: the template requirement on by default when this mod is installed
   (config to turn it off), and asking V&P's maintainer for the slot.
3. **Things that only come from structures we don't have.** Without Update Aquatic, the warm and cold ocean
   ruin sherds (7), the sniffer egg and the coast template; without Wild Update, ward and silence; without
   Nether Update, snout and the netherite upgrade; without Caves & Cliffs, the brush's copper. Recommended:
   a config `fallbackLoot` (on) that puts each orphan in the nearest 1.12.2 structure (sniffer egg and ocean
   sherds in desert pyramid and well archaeology, coast in vanilla 1.12.2 ocean monument chests? none exist, so
   desert pyramid; ward and silence in stronghold chests; snout in nether fortresses) and the brush taking an
   iron nugget without copper.
4. **Calibrated sculk sensor.** It is a sculk sensor variant; copying Wild Update's whole vibration system
   when it is absent is a lot of code doing Wild Update's job. Recommended: the calibrated sensor exists only
   with Wild Update installed, through a shared vibration API in Core (`CORE_REQUESTS.md` 3).
5. **Bamboo without Village & Pillage.** Recommended: copy its bamboo plant, sapling, item and jungle
   generation when V&P is absent (Rule 1's copy), nothing else (no pandas, no bamboo jungle).
6. **Sign changes on signs we don't own.** Editing, back text and waxing need our renderer in place of vanilla's
   sign renderer, and the same on V&P's five wood signs (it has its own renderer). Recommended: vanilla's and
   ours here; V&P's through a small request to its maintainer (its renderer reads the back text from the same
   capability).

## Order

1. Scaffold, this plan, `CORE_REQUESTS.md`: **done**.
2. Streams that wait on nothing: C, B (default copy), Bo, H, K, A, N, Ar, W, Hd, X, one at a time on a `tt-<stream>`
   branch, each merged into `main` when tested.
3. G, Sg, Sm, Sc as the user answers Still open 1, 6, 2, 4; St's trail ruins and ocean ruins when Core has the
   template loader (desert pyramid and well and the template loot can go before it).
4. Languages through the family tool; waterlogging when Core's adapter exists.

## Later

- 1.20's ambient music tracks (Aaron Cherof) by biome, through a music event: nice to have.
- Future MC pairs for whatever of ours Future MC also adds (cherry? none in Future MC 0.2.x; checked per stream).
