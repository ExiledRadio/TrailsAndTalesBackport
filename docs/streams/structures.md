# Stream St: structures

Package `structure`. Config `structures` (`enabled`; everything here also needs `archaeology`). No entities;
packet ids 45-49 unused. Branch `tt-structures`, dev-server port 25945. Code and numbers from the decompiled
1.20.1 sources (`G:/MCDownloads/minecraft-1.20.1/src`, Mojang's names); every structure file is 1.20.1's, copied
unchanged by `python -I tools/structures/generate.py`. 1.20.1 rather than 1.20.6 because Chunkbase's "1.20" and
Caves & Cliffs' 1.20 terrain rules are 1.20.1's; the trail ruins files only differ between the two in their saved
data version.

## Changelog section

*   **Trail ruins** generate in taigas, snowy taigas, old growth taigas, old growth birch forests and jungles, buried
    in the ground with their tower top showing, where 1.20 puts them for the same seed (Chunkbase 1.20). Their
    gravel hides **suspicious gravel** with the common and rare trail ruins loot.
*   `/locate structure minecraft:trail_ruins`.
*   **Desert pyramids** have 1.20's hidden cellar: a staircase from the floor down to a room of sand, with
    suspicious sand in it and one in its collapsed roof.
*   **Desert wells** are 1.20's, a block deeper, with two suspicious sand under the water.
*   With Update Aquatic Backport, **warm ocean ruins** hide suspicious sand and **cold ocean ruins** suspicious
    gravel, as in 1.20.
*   Music disc **Relic** (Aaron Cherof), found in trail ruins.

## How it works

### Trail ruins (`TrailRuins`, `TrailLayout`, `TrailRuinsStructure`, `TrailRuinsPiece`)

*   **Data**: 1.20.1's `structures/trail_ruins/**` (84 templates), `worldgen/template_pool/trail_ruins/**` (7 pools),
    the three `trail_ruins_*_archaeology` processor lists, `worldgen/structure/trail_ruins.json`,
    `worldgen/structure_set/trail_ruins.json`, the biome tag and the `trail_ruins_replaceable` block tag, under
    this mod's assets. The mod is a Backport Core `JigsawPools` and `ModernPalette` source, so the files' `minecraft:`
    names are read from this jar.
*   **Settings** (`TrailRuins`) are read from the copied files: grid 34/8, salt 83469867, linear; size 7; start
    height `absolute -15` projected to `WORLD_SURFACE_WG`; max distance 80; `bury`; the six biomes.
*   **Placement** (`TrailRuinsStructure`, a Core `ScatteredStructure`, saved as `TTB_TrailRuins`): Core's populate-step
    driver lays out and builds starts on any terrain (range 7 chunks). The start random is `StructureRandoms.start`.
    The stub's biome is checked as 1.20.1's `Structure.isValidBiome`: in Caves & Cliffs' 1.18 terrain its noise
    biome at the stub's quart (reached by reflection, as Tricky Trials' chambers and Wild Update's cities reach it);
    in 1.12.2 terrain the column's biome by today's id (`BiomeLocate.id`: `redwood_taiga` is
    `old_growth_pine_taiga`, `mutated_birch_forest` `old_growth_birch_forest`, and so on).
*   **Assembly** (`TrailLayout`): 1.20.1's `JigsawPlacement` draw for draw: the start rotation and element, the start
    piece's floor at the surface (its middle's `WORLD_SURFACE_WG`, from Core's `TerrainHeights`) minus 15, then
    breadth first to depth 7, each jigsaw (shuffled) trying its pool's elements and then the fallback's (shuffled),
    in shuffled rotations, inside the box 80 out from the start piece's middle or the parent's own box. Backport
    Core's `Jigsaw` is 1.16.5's and its pool shuffle is fastutil's (one draw more than 1.20's `Util.shuffledCopy`),
    and it keeps no junctions, so this is our own on Core's pools, elements and templates. Every trail ruins piece is
    rigid, so nothing else asks the ground; in 1.12.2 terrain the ruins are buried by the terrain itself.
*   **Pieces** (`TrailRuinsPiece`, a Core `JigsawPiece`, saved as `TTB_TrailRuinsPiece`): Core places the file with
    its `rule` processors (gravel to dirt 20% and coarse dirt 10%, mud bricks to packed mud 10%, each block's own
    `Mth.getSeed` random, as 1.20's). Core skips `capped` (it logs that once); the piece adds it (`Archaeology`). As
    1.20.1's `PoolElementStructurePiece` it keeps its jigsaw junctions and tells the terrain how it meets the ground
    (`TerrainAdaptation.AdaptingPiece`); the kind is registered with Core's `TerrainAdaptation` as `BURY`, with
    `startsNear` as the planner, so Caves & Cliffs' 1.18 generator beards the terrain around the ruins as 1.20 does.
*   **Archaeology** (`Archaeology`, `VanillaTemplate`): 1.20's `capped` processor shuffles the indices of the whole
    processed block list of a file (`RandomSource.create(seed).forkPositional().at(piece position)`) and lets its
    rule turn up to 6 (houses: common table, then 3 rare), 2 (roads) or 2 (tower top) gravel blocks into suspicious
    gravel with `append_loot` (`LootTable`, `LootTableSeed` from the block's random). So the picks depend on the
    file's block order, which `VanillaTemplate` rebuilds from the file as 1.20's `StructureTemplate` keeps it (full
    blocks, other blocks, blocks with data, each by y, x, z; "full" by today's block name), after the element's own
    processors (structure blocks out, jigsaws to their final state). The result is worked out once per piece and the
    blocks in each chunk are set by name (`trailsandtalesbackport:suspicious_gravel` and its tile's `LootTable` and
    `LootTableSeed`), with the tables renamed to this mod's `archaeology/trail_ruins_common` and `_rare`.
*   **Blocks other family mods own** come from the owner when installed (Wild Update's mud and mud bricks, Village &
    Pillage's workstations, brick wall and campfire). Without it, stand-ins (`StructuresContent.FALLBACKS`): mud to
    dirt, packed mud to brown terracotta, mud bricks to terracotta, their slabs and stairs to sandstone's, walls to
    cobblestone wall, blast furnace to furnace, cartography table, smithing table and loom to crafting table;
    grindstone, composter and campfire left out.

### Desert pyramids (`DesertPyramids`)

*   1.12.2's `ComponentScatteredFeaturePieces.DesertPyramid` is block for block 1.20.1's `DesertPyramidPiece` body
    (compared call by call), except one sandstone 1.12.2 runs a wall to (piece 4, 3, 18), which is cleared. Every
    generator that makes pyramids builds that piece: 1.12.2's, Backport Core's `ModernTemples`, Caves & Cliffs' 1.18
    `Temple` (at 1.18's height). So the cellar is added in 1.20's own piece coordinates to whichever pyramid is
    there: an `IWorldGenerator` (weight 1, after Core's structure driver) finds the scattered-feature generators of the
    chunk generator by type, and for each pyramid inside the populate step's box runs `addCellar` (stairs, room,
    terracotta, sand box, collapsed roof) and then `afterPlace` in that box.
*   Suspicious sand as 1.20's `afterPlace`: the collapsed roof's spot (`forkPositional` at the roof corner,
    `nextIntBetweenInclusive` x and z), then the 83 cellar spots sorted as `Vec3i.compareTo` (y, z, x), shuffled with
    `forkPositional` at the start's box middle, the first `nextInt(5, 8)` suspicious and the rest sand. Seeds are
    1.20's `BlockPos.asLong` (x, z, y packing, not 1.12.2's x, y, z).
*   Not vanilla's: the roof's sandstone-or-sand (`nextFloat < 0.33`) and the stair fill's `nextBoolean` draw from a
    per-chunk random; 1.20 draws its world-generation region's random there.

### Desert wells (`DesertWells`)

*   In 1.12.2's terrain, `BiomeDesert.decorate` asks the `DESERT_WELL` decoration event before its 1-in-1000 roll.
    The handler denies it and makes the same three draws itself (the roll, then x and z), so the rest of the chunk's
    decoration draws what it did; the well is then 1.20.1's `DesertWellFeature`: basin two deep, sand under the water
    cross, two suspicious sand (`Util.getRandom` of the cross, below 1 and below 2) with the desert well table. Those
    two picks come from the well position's own random, since 1.12.2's decoration has no feature random.
*   Caves & Cliffs' 1.18 terrain places its own wells: branch `tt-desert-well` of Caves & Cliffs Backport (commit
    `ea5ccee`, not merged) makes them 1.20.1's under its V120 rules (Trails & Tales installed), drawing the two picks
    from the feature random as vanilla does, with this mod's block by name and plain sand without it.

### Ocean ruins (`OceanRuins`)

*   Update Aquatic owns ocean ruins and builds them with Backport Core's `TemplatePiece`. Nothing here names its
    classes: its ruins are Core's `ocean_ruin` kind, a piece's settings are the keys it saves (template, origin,
    rotation, pivot, integrity, seed) and warm or cold its `BiomeType`. After Core's driver builds the chunk, each
    piece's file is run through 1.20.1's processors (its integrity, as Core rolls it; structure blocks and air out;
    `capped(rule(sand or gravel to suspicious, warm or cold table), 5)`), and the picks inside the box are set where the
    ruin really put sand or gravel.
*   Without Update Aquatic there are no ocean ruins (this mod does not copy them); their items come from fallback
    loot.

### Fallback loot (`FallbackLoot`, Decided 3)

An item whose only 1.20 source is a structure another family mod owns joins, while that owner is absent, the
archaeology table of the structure here most like it, at its 1.20 weight (entries `fallback_<item>`). With the owner
installed nothing is added.

| Item | Real source (owner) | Fallback source | Weight |
| --- | --- | --- | --- |
| Angler, shelter, snort pottery sherds | Warm ocean ruins' suspicious sand (Update Aquatic) | Desert pyramid's suspicious sand | 1 each |
| Sniffer egg (stream N) | Warm ocean ruins' suspicious sand (Update Aquatic) | Desert pyramid's suspicious sand | 1 |
| Blade, explorer, mourner, plenty pottery sherds | Cold ocean ruins' suspicious gravel (Update Aquatic) | Common trail ruins' suspicious gravel | 1 each |

The desert pyramid's own table weighs 8, so with the fallbacks one roll in three there is an ocean ruin's item;
common trail ruins weigh 35 with the blue and white dye (more with Caves & Cliffs' candles and the hanging signs), so the four cold-ruin sherds are 4 rolls in 39. Other streams add their
own owners' items (the smithing templates of ancient cities, bastions and shipwrecks) with `FallbackLoot.add`.

### Relic

*   `music_disc_relic`: 1.12.2's `ItemRecord` (named "Music Disc" with vanilla's `item.record.name`, "Aaron Cherof -
    Relic" from `item.record.relic.desc`), sound event `music_disc.relic` streamed from 1.20.6's `records/relic.ogg`,
    1.20.6's texture. Archaeology's loot adds it to `trail_ruins_rare` by name.

## Shared files touched

*   `TrailsAndTalesBackport.java`: one preInit and one init line. `SelfTestRunner`: one line.
*   `en_us.lang`: the `structures` block (1 key). `sounds.json`: `music_disc.relic` (via `tools/assets.merge_sounds`).

## Not done

*   The other 17 languages: the family's `build_langs.py` pass.

*   **Placed blocks get no tick** (`archaeology.Population`): 1.20's world generation places blocks into a
    `WorldGenRegion` without `onPlace` and without neighbor or shape updates, so a suspicious block it puts over air
    or water stays until the world runs and a neighbor changes. 1.12.2 populates straight into the world. While any
    chunk of a world populates (from Forge's `PopulateChunkEvent.Pre` to this mod's last `IWorldGenerator`, weight
    `Integer.MAX_VALUE`; its first, weight `Integer.MIN_VALUE`, marks the chunk for a generator that fires no event),
    the suspicious blocks' `onBlockAdded` and `neighborChanged` schedule nothing. That covers this mod's structures,
    other mods placing the blocks by name (Caves & Cliffs' 1.20 well) and later features in neighboring chunks setting
    blocks beside them with notifying flags. The set is emptied at the start of every server tick. A block set outside
    population by the structure code (`Archaeology.set`) still has its one tick taken back out (`Archaeology.unschedule`).
*   **The structures' plain sand and gravel get no tick either** (`Archaeology.unscheduleFalling`): 1.12.2's
    `onBlockAdded` schedules the fall of every sand and gravel block a structure sets from a world generator (Backport
    Core's scattered driver, this mod's generator), and one that falls takes the suspicious block beside it along. After
    a trail ruins piece, a desert pyramid with its cellar, a well and each ocean ruin piece (Update Aquatic's, whose sand
    and gravel this stream turns suspicious) is placed in a chunk, the pending ticks of the falling blocks inside it are
    taken back. Without this, Update Aquatic's test world lost 33 of 311 ocean-ruin suspicious blocks (and trail ruins 1
    of 86) once the world ran; with it, none.
*   **Relic's comparator signal** (P7, `docs/PATCHES.md`): a jukebox playing Relic gives 14, as 1.20. 1.12.2 counts
    the signal from the disc's item id; P7 turns exactly Relic's id offset into 14 at each return of
    `BlockJukebox.getComparatorInputOverride`, so it stacks with Nether Update's P2 (Pigstep 13) in either order.

## Not tested

*   The client: how the ruins, the cellar and the well look; the Relic record playing in a jukebox.
*   Caves & Cliffs' 1.20 well (branch `tt-desert-well`) in a generated world: the C&C world's test area has no
    desert near spawn (vanilla 1.20.1 has no desert pyramid within 4,000 blocks of seed 1013's spawn either). Its jar
    built and loaded in the C&C runs below.

## Known differences

*   A neighbor update after population still makes a suspicious block over air or water fall, as in 1.20 once the
    world runs (water still flowing when the chunk starts ticking, a player).

## Tested headlessly

**Gaps closed after 0.1.0** (generation ticks, Relic's comparator), `-Pselftest=all` on fresh worlds, seed 1013:
alone **5117/0**; Caves & Cliffs 1.0.0 dev jar in `eclipse/mods` **5110/0** (as is, its height coremod off), the same
jar with its coremod manifest **5110/0**, and in its 1.18 world type (`level-type=cavesandcliffs`) **5101/0**.
`-Pselftest=structures` with Update Aquatic 1.1.1: 369/0, and a second run in the same world 369/0 (all 311 ocean-ruin
and 86 trail-ruins suspicious blocks still there; without the fix 33 and 1 were gone, which the test now reports).
With Nether Update 1.0.0 and Wild Update 1.0.0 (dev jars given their release manifests, so their P2 and P5 load
beside P7): 366/0, Relic 14, Pigstep 13.

`./gradlew runServer -Pselftest=all -PdeployDir= -PcoreDeployDir=` (fresh Default world, seed 1013, port 25945):
**`SELFTEST DONE pass=4217 fail=0`**. `python -I tools/check_assets.py`: **80 blocks, 1145 states, 92 item models,
752 sound files, 0 errors**. Release jar built last with `--rerun-tasks`: 0 `/selftest/` entries.

*   **Against vanilla 1.20.1** (`tools/structures/oracle120/<seed>.json`): a real 1.20.1 server
    (`tools/structures/oracle/oracle.py`; `max-tick-time=-1`, or the watchdog stops it during the force-loads)
    force-loaded the trail ruins and desert pyramid candidate chunks of regions -4..3 for seeds 1013, 12345 and 42,
    then every chunk of each valid start, and the saved starts and brushable block entities were read from the region
    files. 28 trail ruins: **524 of 524 pieces** (template, position, rotation, box) and **524 of 524 junction
    lists** as vanilla's, laid out from vanilla's start height; **2873 of 2873 suspicious gravel** (position, table,
    seed) worked out from the pieces, plus 2 more of ours in one ruin, where a later vanilla feature (a water spring
    under the ruin) hollowed the ground and vanilla's fell. 1 desert pyramid: **7 of 7 suspicious sand** from its box
    and facing. Since every Chunkbase "1.20" position comes from these same rules (grid, biome at the stub), the
    vanilla server is the stronger check: it also fixes the start height and the layout piece for piece.
*   **Caves & Cliffs world** (C&C `tt-desert-well` jar in `eclipse/mods`, `level-type=cavesandcliffs`,
    `level-name=selftest-cc`, removed afterwards): structures **332/0**. Seed 1013's **64 of 64** trail ruins
    candidates start or not exactly as in vanilla 1.20.1 (its noise biomes), and the located ruins are vanilla's piece
    for piece **at vanilla's height** (0 up); 88 suspicious gravel built. In the Default world (1.12.2 biomes) 48 of the
    64 agree, as expected from different biomes; the located ruins there are vanilla's layout at that terrain's height.
*   **Update Aquatic world** (UA 1.1.1's release jar, `level-name=selftest-ua`, removed afterwards): structures
    **340/0**, twice on the same world: 6 ocean ruins (64 pieces) near spawn, 320 suspicious blocks worked out, 311
    in the world after the first build (110 warm sand, 201 cold gravel; the rest where the ruin's integrity left no
    sand or gravel). The fallback loot is absent with UA and present without it.
*   This world: `/locate structure minecraft:trail_ruins` and Core's ring order; the start saved with the layout's
    pieces; 86 suspicious gravel exactly where worked out (65 common, 21 rare), each with its table and seed, none
    waiting for a tick, no other suspicious gravel in the box; the start 15 under the bare terrain. A desert pyramid:
    the cellar's blue terracotta and stairs, 7 suspicious sand (the roof's and 6) with the table and 1.20's seed. A
    well on sand: 1.20's basin, sand under the cross, two suspicious sand with the well table; the `DESERT_WELL`
    event denied and 1.20's well where 1.12.2's would be.
*   Data: the settings read from the files (34/8/83469867, size 7, y -15, 80, bury, six biomes), 7 pools and their
    templates load and read in 1.20's order, every block of the ruins resolves, the three processor lists,
    `Mth.getSeed` and 1.20's `BlockPos.asLong` against values worked out by hand, the terrain adaptation registered.
