# Waterlogging pass

Backport Core's `waterlog` API (`Waterloggable`, `Waterlogging`) over Fluidlogged API 3.3.3, which is optional. No
block has a `waterlogged` property: the water is Fluidlogged API's, and Core's patch makes our `Waterloggable`
classes Fluidlogged API's `IFluidloggable`. Core's `WaterRules` lets them take water sources only, never flowing
water or lava, and keeps every other block of ours dry. No package of its own (each stream's blocks say it
themselves), no config, no packets. Branch `tt-waterlog`. Test server port 25941 (`eclipse/server.properties`).

## Changelog section

*   With **Fluidlogged API** installed, these hold water as in 1.20: every hanging sign and wall hanging sign, the
    chain, the decorated pot, cherry and bamboo signs, stairs (bamboo mosaic too), slabs, fences and trapdoors, and
    cherry leaves.
*   Cherry trees growing into water keep it in their leaves.
*   Without Fluidlogged API nothing changes: these blocks stay dry.

## How it works

| Block | 1.20.6 rule kept |
| --- | --- |
| `common/wood/WoodBlocks` `Stairs`, `Slab`, `Fence`, `TrapDoor` (cherry, bamboo, bamboo mosaic) | plain `Waterloggable`; slab `canHoldWater = !isDouble()`; door, fence gate, button, pressure plate, planks and logs stay dry (1.20.6's are) |
| `common/wood/WoodSign` `Standing`, `Wall` (cherry, bamboo) | plain `Waterloggable` |
| `hangingsign/BlockHangingSign` (`Ceiling`, `Wall`, every wood) | plain `Waterloggable`; one that loses its support drops and leaves its water |
| `hangingsign/BlockChain` (the copy) | plain `Waterloggable`, as 1.16's and Nether Update's copy; states unchanged (axis only) |
| `archaeology/BlockDecoratedPot` | plain `Waterloggable`; broken, it leaves its water |
| `cherry/BlockCherryLeaves` | plain `Waterloggable` (every 1.19+ leaf); Fluidlogged API's built-in list would take any `BlockLeaves` anyway |
| `cherry/CherryTree` (saplings and the cherry grove) | 1.20's `validTreePos` includes water (`#replaceable_by_trees`): logs replace it; leaves go in with `placeKeepingWater` (1.20's `FoliagePlacer.tryPlaceLeaf`: a source only), with `setBlockAndNotifyAdequately`'s flags |

Every one goes through Core: without Fluidlogged API, `Waterlogging` answers "no water" and `placeKeepingWater`
places dry, so trees replace water as 1.12.2's always did.

Item placement needs nothing: the sign, hanging sign, pot and block items set the block into the water's
position, and Fluidlogged API keeps the source for a block that can hold it.

Worldgen: cherry trees (above). Nothing else this mod generates is waterloggable (petals, bamboo, suspicious
blocks); structure templates (stream St) place through Core's `ModernTemplate`, which already keeps a template's
waterlogged blocks with `placeWithWater`.

`build.gradle`: `-PnoFluidlogged` runs `runServer`/`runClient` without Fluidlogged API on the classpath (compiled
against it, as the release jar is), as Wild Update's, Caves & Cliffs' and Nether's builds do.

**Not here:** the calibrated sculk sensor and the sculk sensor copy are stream Sc's (Decided 4). 1.20 waterlogs
both, and a waterlogged one makes no clicking sound (Wild Update's `BlockSculkSensor` does exactly that). The test
skips their names (`WaterlogSelfTest.OTHERS`), so Sc's blocks are checked by Sc's own test.

## Tested

`-Pselftest=waterlog` (`WaterlogSelfTest`, `WaterlogFluidChecks`), fresh world each run:

With Fluidlogged API (151 pass, 0 fail):

*   Exactly the blocks 1.20.6 waterlogs are `Waterloggable` (door, fence gate, button, pressure plate, planks, log,
    sapling, petals, sniffer egg, bookshelf, suspicious sand, piglin head, bamboo, torchflower, pitcher crop and
    potted sapling are not).
*   All 1,145 states of ours (no Nether Update or Wild Update installed): 533 take a water source, every other
    state none, and none takes flowing water or lava.
*   Placed into a source with `setBlockState`, each kind keeps it (cherry, bamboo and mosaic stairs, both fences
    and trapdoors, both signs and wall signs, four hanging and wall hanging signs, the pot, leaves, the chain, top
    and bottom slabs); double slabs, the fence gate, door, planks, bookshelf, sniffer egg and log replace it.
*   Placed by a player's item into water: cherry sign, decorated pot, stairs, a hanging sign under a submerged
    ceiling, a wall hanging sign and a wall sign on a submerged wall, and a chain all keep the source.
*   Breaking waterlogged stairs, pot, sign, chain, leaves or slab leaves the source; a hanging sign whose ceiling
    goes drops and leaves its water.
*   Placed into flowing water, stairs, fence, hanging sign, pot, sign, leaves and chain replace it; a spring does
    not run into a dry fence, pot or trapdoor; a waterlogged slab and pot let their water flow out (1.20's
    non-full shapes).
*   Buckets fill and empty stairs, a slab, fence, trapdoor, pot, sign, leaves and chain; a hanging sign takes and
    gives up a source; on a double slab the water goes on top.
*   Six cherry trees grown on grass at the bottom of a 16-deep pool: 1,706 leaves, every one holding a source; no
    log holds water.

Without Fluidlogged API (`-PnoFluidlogged`, 60 pass, 0 fail): the same blocks are `Waterloggable` but no state of
ours holds water; every kind placed into water replaces it; `placeKeepingWater` places leaves dry; nothing can be
given water; the six trees in the pool grow with 1,706 dry leaves.

Whole suite, fresh world each run (`-Pselftest=all`): **4032 pass, 0 fail** with Fluidlogged API; **3941 pass, 0
fail** with `-PnoFluidlogged`. The release jar was rebuilt after the last test run (0 `/selftest/` entries).

## Not tested

*   Client: water drawn inside our models (Fluidlogged API draws it).
*   A projectile shattering a waterlogged pot (the shatter path is `setBlockToAir`, the same call the
    support-loss test covers) and a piston breaking one.
*   A generated cherry grove with water in it: the tree is tested in a pool, not in a generated region.
*   With Nether Update installed (its chain replaces ours; it is `Waterloggable` there too).
