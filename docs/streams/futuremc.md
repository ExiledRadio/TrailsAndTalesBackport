# Pairs with the other mod that adds some of the same blocks

Package `migrate` (`FutureMcPairs`), generator `tools/futuremc/states.py`, table `migration/futuremc_states.json`.
Config `compat` (`duplicates`, `convertRemoved`). Quiet compatibility (Rule 3): no user-facing text names the other mod.
Done the family way, as Buzzy Bees' stream F and Nether Update's stream Fm: Backport Core's `FutureMcTable` contributor
and `DuplicateBlocks`.

## Changelog section

*   Worlds that used another mod's stripped logs and wood, bamboo or chain keep them when that mod is removed: they
    turn into this mod's (when this mod has its own copies, i.e. without Village & Pillage or Nether Update).
*   With both installed, the duplicates are hidden (`compat.duplicates`: OURS, the default, hides theirs; THEIRS hides
    ours; BOTH hides nothing). With ours kept, an axe strips vanilla logs into ours.

## What the other mod has of ours

Read from its 0.2.21 registry classes (`G:\MCDownloads\futuremc-0.2.21\src\futuremc\registry`: `FBlocks`, `FItems`,
`FEntities`) and checked against Village & Pillage's and Nether Update's recorded tables (written by `/futuremc dump`
on real servers with it installed).

**1.20 content: nothing.** None of these exists in it: cherry wood and leaves, pink petals, the cherry grove, bamboo
wood (planks, mosaic, rafts), hanging signs, the 1.20 sign changes, the chiseled bookshelf, the brush, suspicious sand
and gravel, sherds, decorated pots, trail ruins, the sniffer, its egg, torchflowers, pitcher plants, camels, smithing
templates and trims, the calibrated sculk sensor, the piglin head, armadillos and their scute (its `scute` item is the
1.13 turtle scute), wolf armor and variants. Its entities (bee, cod, panda, pufferfish, salmon, trident, tropical fish)
are none of ours.

**This mod's copies of other family mods' blocks** (`docs/PLAN.md` "Copies"):

| Copy (owner) | The other mod | Pair |
| --- | --- | --- |
| Stripped logs and stripped wood, six woods (Village & Pillage) | `stripped_<wood>_log`, `stripped_<wood>_wood` | same names, `axis` as recorded (log: y 0, x 4, z 8, none 12; wood: y 0, x 4, z 8) |
| Bamboo plant (Village & Pillage) | `bamboo` (and its block item) | its `thick` / `mature` are our `age` / `stage`, counted; `leaves` the same. Our `bamboo_sapling` has no counterpart (its shoot is a state of the stalk) |
| Chain (Nether Update) | `chain` | no properties there (vertical only): our vertical chain |
| Smithing table, 1.20 form (Village & Pillage), for stream Sm | `smithing_table` | `facing` ignored, as Village & Pillage declares. Registered now; a pair once the smithing stream's copy exists (not in this build) |
| Copper, amethyst, sculk sensor | none | it has none of them |

Each pair counts only while both halves exist: with an owner installed this mod registers no copy, and the owner's own
contributor pairs its blocks.

## How it works

*   `FutureMcTable.contributor("trailsandtalesbackport", compat.convertRemoved)`: same-name counterparts, the bamboo
    property renames, the smithing table's `facing` ignored, and the recorded states (`tools/futuremc/states.py`: the
    owners' entries, identical to Village & Pillage's and Nether Update's tables). Items need no rule: every one is a
    block's item of the same name.
*   `DuplicateBlocks.register(..., compat.duplicates, blockPairs)`: the 15 pairs. Its bamboo item is a Kotlin
    `ModeledItemBlock`, which Backport Core 1.3.0 hides through the tab field (Nether Update's Core request 6).
*   Stripping: its axe handler runs at normal priority and this mod's at low, so with both installed its stripped logs
    would come out of every vanilla log. With ours kept, `FutureMcPairs.stripFirst` runs this mod's stripping at high
    priority; it cancels the click when it strips, so theirs never runs. With theirs kept, theirs strips (its blocks are
    the visible ones).

## Tested

*   Headless, `-Pselftest=futuremc` (dev, without the other mod; with `effectparticles`, port 25887): part of **353
    checks, 0 failed**. Every recorded state of all 14 pairs present converts with nothing dropped; the stripped log axes
    and metadata; all 12 bamboo states (thick to age, mature to stage, leaves); the chain; four block items become ours
    keeping their count; the smithing table pair is inert without our copy; nothing of ours is hidden without the other
    mod.

## Not tested

*   A real server with the other mod installed (`/futuremc check`, `/futuremc dump`, removing it from a world). The
    states are the owners' recorded ones, so `/futuremc check` should report 0 mismatches; Nether Update's kit is at
    `G:\NetherTest\fmserver`.
*   The strip-first handler with the other mod installed.
