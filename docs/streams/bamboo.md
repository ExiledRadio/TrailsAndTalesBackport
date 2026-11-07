# Stream B: bamboo wood set

Package `bamboo`, built on `common/wood` (see `cherry.md`). Config `bamboo` (`enabled`).

## Changelog section

*   **Bamboo wood**: blocks of bamboo (9 bamboo) and stripped bamboo, bamboo planks (2 per block), bamboo
    mosaic (2 bamboo slabs) with its own stairs and slab, and bamboo stairs, slabs, fences, fence gates, doors,
    trapdoors, pressure plates, buttons and signs, all with 1.20's bamboo wood sounds. Strip blocks of bamboo
    with an axe.

## How it works

*   `WoodSet.bamboo(...)`: the bamboo shape of the shared wood set (block of bamboo in place of logs, no wood,
    the mosaic as a second plank).
*   The bamboo itself is Village & Pillage's (`villagerbackport:bamboo`). Without it, the set is registered but
    the block of bamboo has no recipe, until the copy of the plant is built (PLAN.md, Still open 5).

## Tested headlessly

`./gradlew runServer -Pselftest=all` (port 25882) with cherry: **`pass=201 fail=0`**; `check_assets.py`:
**38 blocks, 497 states, 32 item models, 193 sound files, 0 errors**. Registration, items, sounds, mosaic slab
states, recipes (planks from both blocks, mosaic, mosaic stairs and slabs, crafting table), ore names, fuel,
flammability, no block-of-bamboo recipe without bamboo, axe stripping keeping the axis.

## Not tested

*   **With Village & Pillage installed** (its dev jar in `eclipse/mods`): the test has the branch for it
    (9 bamboo make a block), but that run was not made yet.
*   Anything drawn (models, sign renderer), sounds playing.
