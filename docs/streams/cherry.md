# Stream C: cherry wood, leaves, saplings, pink petals

Package `cherry`, shared wood code in `common/wood`. Config `cherry` (`enabled`). Built in the first session.

## Changelog section

*   **Cherry wood**: cherry logs, wood, stripped logs and stripped wood, planks, stairs, slabs, fences, fence
    gates, doors, trapdoors, pressure plates, buttons and signs. Every cherry block has 1.20's own cherry wood
    sounds, doors and buttons included. Strip logs and wood with any axe. Cherry planks work in every recipe
    that takes any planks (crafting tables, chests, sticks and the rest).
*   **Cherry leaves** drop cherry saplings and sticks, never apples, and let pink petals drift down from under
    them. Like 1.20's leaves they stay on as long as a log is within 6 blocks.
*   **Cherry saplings** grow 1.20's cherry trees: a short trunk splitting into up to three branches under wide
    blossom canopies with leaves hanging from their edges. Saplings go into flower pots.
*   **Pink petals**: place up to four on one block, each turned the way you face. Bone meal adds a petal, or
    drops one from a full block. One petal crafts into pink dye.
*   Vanilla's trapdoor, button, pressure plate and sign recipes now need oak planks, as in 1.14, so the other
    woods make their own.
*   With a composter installed, cherry leaves, saplings and pink petals compost at 30%.

## How it works

*   **`common/wood`** is the wood-set code bamboo will share: `WoodKind` (name, map colors, 1.20's per-wood
    sounds), `WoodBlocks` (each block extends vanilla's block of that shape, so every `instanceof` check
    holds), `WoodSign` (standing, wall, our own `TileEntitySign` subclass `trailsandtalesbackport:sign`, item),
    `WoodSet` (registration, 1.20.6's recipes, fuel times, ore names, flammability), `AxeStripping`,
    `VanillaWoodRecipes` and the client `WoodSignRenderer`.
*   **Wood sounds**: doors, trapdoors, fence gates, buttons and pressure plates play the wood's events directly
    instead of vanilla's hard-wired world events (1006/1012, 1008/1014), on click and on redstone.
*   **Leaf decay at distance 7**: our logs tell leaves within 7 to check when broken (vanilla logs reach 4), and
    a checking leaf stays if a log is reachable through leaves in 6 steps.
*   **Fancy/fast leaves** follow vanilla's own leaves, which the game switches with the graphics option.
*   **The tree** follows `configured_feature/cherry` from the 1.20.6 jar (trunk 7-8, 1-3 branches, foliage
    radius 4 height 5, the three hole and hanging chances); the trunk and canopy walks are written from what the
    placers produce, since the placers are code, not data.
*   **Sign text color**: a sign dyed through Village & Pillage's sign dyeing keeps its color under
    `villagerbackport:Color` in the tile's Forge data; our renderer reads the same key.
*   **Narrowing vanilla recipes**: Forge widens 1.12.2's trapdoor, button, plate and sign recipes to any
    `plankWood`, which made oak ones from cherry planks. Village & Pillage narrows them to oak when installed;
    without it, this mod does the same, in place (`VanillaWoodRecipes`).
*   **Ownership**: every block is declared to Backport Core's ownership table at pre-init.

## Tested headlessly

`./gradlew runServer -Pselftest=all` (port 25881), then `python tools/check_assets.py`:
**`SELFTEST DONE pass=164 fail=0`**; **20 blocks, 241 states, 17 item models, 137 sound files, 0 errors**.

*   Registration of every block, items only where 1.20 has them, the door and sign items; metadata round trip of
    every stored state; ore names (`logWood`, `plankWood`, `treeLeaves`); flammability, hardness, sound type.
*   Every recipe (planks from each log, wood, stairs, slabs, fence, gate, door, trapdoor, plate, button, sign,
    pink dye), cherry planks making a crafting table, sticks and a chest, oak planks still making the oak
    trapdoor and button, and the cherry ones not being made by vanilla's recipes. Fuel times for planks, log,
    slab, door, sign, button and sapling.
*   Axe stripping by a real player's right-click: log and wood strip keeping the axis, the axe wears, planks
    don't strip.
*   Door placed by its item (both halves), opened by clicking its top half, broken into one door; opened and
    closed by redstone. Fence gate opened by click and by redstone. Cherry fence joining an oak fence. Two slabs
    making the double slab, which drops two. Standing sign facing the player, wall sign, both with our tile, one
    sign item each when broken. Button powering when pressed.
*   Trees: nine trees grown with a fixed seed, each in open space, 8-30 logs, at least 40 leaves, every leaf
    within decay reach, some hanging leaves, grass under the trunk turned to dirt; no tree under a low roof; a
    sapling taking two growths to become a tree; no sapling on stone.
*   Leaves: a leaf 5 and 6 from a log stays and stops checking, a leaf 7 away decays; breaking a log reaches
    leaves 6 away; player-placed leaves are persistent; 20000 drop rolls give saplings at ~5%, sticks at ~2%,
    no apples; shears give the block.
*   Pink petals placed on grass by a real player, facing back toward them; three more added to the same block,
    none past four; the outline growing quadrant by quadrant for each facing; bone meal adding one and dropping
    one from a full block; breaking dropping as many as it held; not on stone.
*   Flower pot: a sapling into an empty vanilla pot, out again empty-handed, the potted block dropping the pot.
*   The server booting at all proves no client type is reached from server code.

## Not tested

*   Anything drawn (no client): block and item models, the sign renderer and text, the petal particles, the
    leaves' fancy and fast looks, the potted sapling.
*   Sounds playing (the events are registered and their files checked).
*   Composting (needs Village & Pillage installed; the calls are made at init when it is present).
*   Running beside Village & Pillage: with it installed this mod leaves the vanilla recipes to it.

## Left for other streams

*   Bee nests on cherry trees (`cherry_bees_005`): Buzzy Bees' nests; needs a hook from it.
*   Cherry groves in the world: stream G.
*   Waterlogging of signs, stairs, slabs, fences, gates and trapdoors: Core's adapter, later.
*   Hanging signs: stream H. Boats: stream Bo.
