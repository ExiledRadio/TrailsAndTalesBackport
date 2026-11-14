# Stream G: the cherry grove

Package `biome` (client part in `biome.client`). Config `cherryGrove` (`enabled`). Branch `tt-grove`. No entities;
packet ids 20-24 are unused. Dev-server port 25884. Generator: `tools/biome/generate.py`.

## Changelog section

*   **Cherry groves**: a new mountain biome of pale green grass, pink-blossomed cherry trees, carpets of pink petals
    and clear light-blue water. Pigs, sheep and rabbits live there, and (with Buzzy Bees Backport) bee nests hang
    from some of the trees.
*   Cherry groves take part of the extreme hills, about as often as you meet a grove in 1.20 (0.3% of the world).
    The land is the same as it was: a seed keeps its shape and every other biome stays where it was.
*   With Caves & Cliffs Backport's 1.18 terrain, groves sit on the mountain slopes exactly where 1.20 puts them.
*   The grove has 1.20's music: Bromeliad, Echo in the Wind, Left to Bloom, Clark, Minecraft and Sweden.

## How it works

*   **Source**: 1.20.6's `worldgen/biome/cherry_grove.json`, `configured_feature/cherry_bees_005`, `flower_cherry`,
    `patch_grass`, `patch_tall_grass` and `placed_feature/trees_cherry`, `flower_cherry`, `patch_grass_plain`,
    `patch_tall_grass_2`. The client jar does not ship them, so they were generated with 1.20.6's own data
    generator (`net.minecraft.data.Main --server`, Java 21, the jar's libraries). 1.20.6's numbers equal 1.20.1's,
    which Caves & Cliffs ported.
*   **Biome** (`BiomeCherryGrove`, `trailsandtalesbackport:cherry_grove`, registered while `cherryGrove` and
    `cherry` are both on): temperature 0.5, downfall 0.8, rain; grass and foliage `0xB6DB61`; sky `0x7BA4FF`
    (1.12.2's own formula gives it for 0.5); fog 1.12.2's default `0xC0D8FF`; water and underwater fog `0x5DB7EF`.
    1.12.2's water texture is blue and the biome color multiplies it. Every channel of `0x5DB7EF` is above 1.13's
    blue `0x3F76E4`, so the tint over the blue texture is white. When something replaces the tint (Update Aquatic's
    1.13 texture), a `GetWaterColor` listener at `LOWEST` gives the exact color. Height is 1.12.2's extreme hills'
    (base 1.0, variation 0.5). Surface grass over dirt (1.20's surface rules have nothing for the grove). Forge
    dictionary `MOUNTAIN` (1.20's `#is_mountain`).
*   **Spawns** as the JSON: pig (1, 1-2), rabbit (2, 2-6), sheep (2, 2-4); the eight usual monsters and bats are
    1.12.2's defaults already; no squid. 1.20's glow squid entry is Caves & Cliffs' to add.
*   **Decoration in 1.12.2 terrain** (`CherryGroveDecorator`), 1.20.6's step order:

    | 1.20.6 | Here |
    | --- | --- |
    | default ores; `disk_sand` 3, `disk_clay` 1, `disk_gravel` 1; `ore_emerald` | 1.12.2's ores and disks (same counts); 1.12.2's mountain emeralds (3-8, y 4-31). Tuff and copper are Caves & Cliffs' |
    | `ore_infested` | 1.12.2's mountain silverfish stone (7 veins of 9, y 0-63) |
    | default springs | 1.12.2's (50 water, 20 lava) |
    | `patch_tall_grass_2`: 7 if the biome noise is at least -0.8, else 0; 1 in 32; motion blocking | 96 tries, 7 across, 3 up or down: tall grass |
    | `patch_grass_plain`: 10, else 5; `WORLD_SURFACE_WG` | 32 tries, 7 / 3: grass |
    | `flower_cherry`: 10, else 5; motion blocking | 96 tries, 6 / 2: pink petals, 16 weighted states (4 facings x 1-4 flowers) |
    | `trees_cherry`: 10 (weight 9) or 11 (weight 1); no water (`_WG` heightmaps); `OCEAN_FLOOR`; cherry sapling survives | `cherry_bees_005`: the cherry stream's `CherryTree` |

    *   The biome noise is 1.20's `BIOME_INFO_NOISE` (simplex, seed 2345), which is 1.12.2's `GRASS_COLOR_NOISE`.
        It is read at the chunk's corner, as 1.20 reads it at the placement origin. The self-test checks five values
        against the 1.20.6 class itself.
    *   Each position passes 1.20's `biome` filter (the grove at that block). Patches are 1.20's
        `RandomPatchFeature`: offsets `nextInt(n + 1) - nextInt(n + 1)`, air first, then the state drawn, then
        survival.
    *   Heightmaps are 1.20's. The `_WG` ones (grass patches, the trees' water check) stop updating in 1.20 once
        features start, so they skip plants, leaves and logs here.
    *   1.20's `TreeFeature` also checks the trunk's own block (air or `#replaceable_by_trees`). `CherryTree` skips
        that block, because the sapling has cleared it. So the decorator checks it, and pink petals there stop a
        tree, as in 1.20. Without that check, 1.12.2 groves grew more than twice C&C's trees.
    *   Blocks are set without neighbor or observer updates (flags 2 | 16). 1.12.2's flag 2 still pokes observers,
        so an emerald at a chunk edge loaded the chunk to the north. Vanilla's own mountain emeralds do that too.
    *   1.12.2's extreme hills decoration (oaks, spruces, flowers) does not run in the grove. Each step fires its
        Forge decorate event; steps a world generator says it places itself (Core 1.2.0's `DecorationSource`, by
        name) are skipped.

### Where it generates

*   **1.12.2 terrain** (*Decided* 1, `CherryGroveLayer`). In 1.20 the cherry grove took the meadow's place in the
    plateau table's variant row, in the cold and temperate columns at the two driest humidities. That is a
    mountain-slope biome of the cool and temperate latitudes. 1.12.2's high land in exactly those bands (Forge's
    WARM and COOL) is the extreme hills. So a mask of patches about 256 blocks across is laid over the biome map,
    1 cell in 38, and extreme hills or wooded hills (`extreme_hills_with_trees`) under a patch become grove. The
    gravelly variants and the edge stay. 1 in 38 makes the grove 0.308% of 1.12.2's 1:4 map over five seeds. 1.20
    gives 0.30%, measured with Caves & Cliffs' exact 1.20 biome source (`Editions.CHERRY`) over three seeds at
    8000 blocks square. The cherry grove took 17% of the meadow and nothing else.
*   **Same terrain, same seeds.** The grove has the extreme hills' height numbers, and 1.12.2's terrain reads only
    those from a biome, so terrain is identical block for block. No other biome cell changes, so a seed's map
    matches Chunkbase's 1.12 map apart from the groves. Village, stronghold and spawn biome lists are unaffected:
    the extreme hills were in none of them.
*   **How the layers are wrapped.** Forge's `InitBiomeGens` (at `LOW`) replaces the 1:4 map with the grove layer.
    The 1:1 voronoi zoom gets that layer as its parent, so the 1:1 map is the zoom of the map with groves. The zoom
    object stays in place. That matters: Backport Core runs on the same event at `LOWEST`. It re-seeds the hills
    layer's river noise once a 1.13+ backport is installed, and swaps the voronoi zoom for 1.15's fuzzy zoom once
    a 1.15+ one is. It only finds the zoom by its class, so wrapping the 1:1 layer, as Tricky Trials' pale garden
    does, would silently keep 1.12.2's zoom.
*   **`cherryGrove.enabled`** off (restart): the biome is not registered and no new grove appears. Chunks made
    before keep their biome id, which then reads as plains.
*   **Caves & Cliffs' 1.18 terrain**: C&C already has the 1.20 grove (`Editions.CHERRY`, since `cc118-all`). A
    world made with our `cherry_log`, `cherry_leaves` and `pink_petals` registered is "1.18 terrain with
    1.20-cherry's biomes". It places groves by 1.20's climate (`PLATEAU_BIOMES_VARIANT`) and decorates them with its
    1.20.1 features (`trees_cherry` with `cherry_bees_005`, `flower_cherry`, the grass patches), our blocks by name.
    Its `BiomeMap` takes a biome a mod registers under the modern name, so its grove columns are this biome. So
    **no C&C hook was needed** and there is no `cc-grove` branch. Our decorator does not run there (checked).
    Without Trails & Tales, C&C's world is the same as before: its edition comes from the blocks, not from us.

### Integration

*   **Buzzy Bees Backport** (by name, init): `BeeNests.addTree` (cherry logs, any axis) and `setBiomeChance(grove,
    0.05)`, 1.20's `cherry_bees_005`. In 1.12.2 terrain Buzzy Bees' populate pass then gives grove trees their
    nests, and its sapling rule (5% with a flower within 2) gives the cherry sapling's: 1.20's `CherryTreeGrower`
    picks `cherry_bees_005` near flowers. `BeeFlowers.addFlower` / `addFlowerItem` for cherry leaves and pink
    petals, as 1.20's `#flowers` block and item tags list them. In C&C's terrain its generator places the nests with
    the trees (Buzzy Bees steps aside through `DecorationSource`).
*   **Music** (`CherryGroveClient`, client): a `GameMusic` step at "1.20". 1.20.6's `getSituationalMusic` gives the
    biome's music wherever the game music would play: overworld, not flying in creative, not while the underwater
    music plays. Here that is `music.overworld.cherry_grove`, 12000-24000 ticks, not replacing a track already
    playing. Its tracks are 1.20.6's: Bromeliad and Echo in the Wind (weight 3, volume 0.4) are the look and feel
    stream's files; Left to Bloom (0.4) is copied from 1.20.6's assets; Clark, Minecraft and Sweden are 1.12.2's
    `calm2`, `calm1` and `calm3` (the generator checks the hashes match).
*   **`/locate biome`**: Backport Core's `/locate` has no biome form (none on any Core branch). Asked for in
    `CORE_REQUESTS.md` 11.1; the grove is an ordinary registered biome, so it will be found with no work here.
*   **Adventuring Time**: 1.12.2's advancement lists its biomes in its JSON, so the plan's "through the biome
    registry on its own" does not hold. Asked for in `CORE_REQUESTS.md` 11.2.

## Tested headlessly

`./gradlew runServer -Pselftest=grove` (port 25884, seed 1013, Default world): **`SELFTEST DONE pass=95 fail=0`**.

*   Registration: id, temperature, downfall, rain, no snow even at y 200, both height numbers equal the extreme
    hills' and wooded hills', `MOUNTAIN`, grass over dirt; the music event registered.
*   Spawns: all 12 entries with weights and group sizes; no water creatures; spawn chance 0.1.
*   Colors: the constants; the white tint; the water event keeps the tint, gives `0x5DB7EF` once the tint is replaced,
    and leaves plains alone; the sky from 1.12.2's formula; the biome noise against 1.20.6's class at 5 points.
*   Layers: built through the real `InitBiomeGens` event (Core's steps included), grove off and on, five seeds, 8192
    blocks square at 1:4. 21.0M cells: extreme hills 10.6%, grove 0.308%. Nothing but hills changed, and only to
    grove; every grove cell has its hills' heights. The 1:1 map at 2048 blocks square per seed: the same, 0.54%
    grove. The zoom class is the same with and without the grove (Core's 1.15 swap still happens). The world's own
    1:4 and 1:1 maps equal the event's.
*   Heightmaps on a test plot: world surface, motion blocking, ocean floor and both `_WG` kinds over leaves, water
    and a plant. Sapling survival on grass, not stone. 1.20's trunk check: air and grass yes, petals no.
*   Patches on flat grass against a simulation of 1.20's `RandomPatchFeature`: 4 `flower_cherry` patches (27.8 each
    expected), at least 12 of the 16 petal states, all on grass; 10 `patch_grass` (7.8 each expected); tall grass
    both halves.
*   A real grove: the nearest whole grove chunk to 0, 0 (chunk -36, -36), its 4x4 generated. Cherry logs, more
    leaves than logs, petals, grass; no oak or other vanilla logs; grass, dirt or trees on every column; every tree
    try on grass or dirt; **no chunk loaded by the grove's decoration** (a chunk-load probe on the decorator's stack;
    the first version tripped it with emeralds).
*   Density over four groves (40 chunks apart or more), per 256 grove columns: 20.2 cherry logs, 302 leaves, 63
    petals, 27 grass.

With **Buzzy Bees Backport** 1.0.1's release jar in `eclipse/mods`: **101/0**. Chance 5% in the grove, cherry logs are nest trees, petals and leaves (and the petal item) are flowers.
Buzzy Bees' own nest pass with every trunk forced finds the cherry trees: 4 nests in the 3x3.

With **Caves & Cliffs** (`CavesAndCliffsBackport-playtest` 7311c71 release jar in `eclipse/mods`, Backport Core
playtest 1.2.0 via `-Pcore_version=playtest`), `level-type=cavesandcliffs`, seed 1013: **92/0**. The world chose
"1.18 terrain with 1.20-cherry's biomes"; whole grove chunks are this biome; our decorator did not run.
Density over four groves in C&C's 1.20.1 port: 18.5 logs, 243 leaves, 92.5 petals, 45 grass per 256 columns.
Trees match ours within 10%; leaves are 24% more here. Petals (-32%) and grass (-40%) are fewer in 1.12.2 terrain,
and the same in grove interiors (7+ blocks from an edge). Mean slope is equal (0.69 vs 0.68 blocks per step east),
and patch yields on flat ground match 1.20's simulated ones, so the patches themselves are right. The remaining
difference is the ground: in these 1.12.2 hills a patch places 4.2 grass and 10.9 petals, against 7.8 and 27.8 on
flat ground. Both runs were undone afterwards (mods folder empty, `level-type=DEFAULT`).

Full suite `-Pselftest=all` (Default world): **`SELFTEST DONE pass=3779 fail=0`**. `python tools/check_assets.py`: 0 errors. It now finds
1.12.2's own sounds (`calm1-3`) in 1.12.2's asset index, which the game downloads, as well as in its jar.

## Not tested

*   Anything drawn or heard: grass, foliage, sky and water colors on screen; the music step (it runs in the client's
    `GameMusic` ticker; a headless server has none).
*   Update Aquatic's water texture with the grove (the event is tested with a stand-in replaced tint).
*   Bees actually flying from grove nests; Buzzy Bees in a C&C world. Its 1.0.1 jar predates its `GeneratorNests`
    step-aside, so it would add second nests there; that is Buzzy Bees', fixed on its main branch.
*   A world made with an older C&C without `Editions.CHERRY`: it would place no groves in 1.18 terrain.

## Not done

*   `/locate biome` and Adventuring Time: Core requests 11.1 and 11.2.
*   1.20's underwater fog color: Core request 10 of Wild Update (the family has no fog hook yet).
*   The glow squid spawn entry is Caves & Cliffs'; amethyst geodes, glow lichen, tuff and copper in the grove come
    from C&C's own 1.12 generation where installed.
