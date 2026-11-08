# Creative tabs

Trails & Tales Backport has no creative tab of its own. Each block and item is in the vanilla tab Minecraft 1.19.2 gives it, next to its vanilla neighbor in 1.19.2's order, through Backport Core's creative placement (`creative.CreativePlacement`; the rule and the table are in Backport Core's `docs/CREATIVE_TABS.md`). Items are placed by their vanilla name: `common/Content` places every block with an item and every item, and the main class's pre-init the spawn eggs. Its cherry and bamboo sets come after dark oak (after mangrove once the Wild Update adds it), as today's tabs order the woods.

"After" and "Before" are the stacks either side of it with this mod and Backport Core alone, from a dev client's `-Dbackportcore.creativeDump` (an item is put after its nearest 1.19.2 neighbor before it, or before the one after it, whichever is of its kind). With other backports installed an item moves to stay by its 1.19.2 neighbor: stripped logs follow the warped stem once the Nether Update adds it, and so on.

## Building Blocks

| Item | After | Before |
|---|---|---|
| `cherry_planks` | `dark_oak_planks` | `bamboo_planks` (this mod) |
| `bamboo_planks` | `cherry_planks` (this mod) | `bamboo_mosaic` (this mod) |
| `bamboo_mosaic` | `bamboo_planks` (this mod) | `bedrock` |
| `chiseled_bookshelf` | `bookshelf` | `mossy_cobblestone` |
| `cherry_slab` | `dark_oak_slab` | `bamboo_slab` (this mod) |
| `bamboo_slab` | `cherry_slab` (this mod) | `bamboo_mosaic_slab` (this mod) |
| `bamboo_mosaic_slab` | `bamboo_slab` (this mod) | `sandstone_stairs` |
| `cherry_log` | `dark_oak_log` | `bamboo_block` (this mod) |
| `bamboo_block` | `cherry_log` (this mod) | `stripped_oak_log` (this mod) |
| `stripped_oak_log` | `bamboo_block` (this mod) | `stripped_spruce_log` (this mod) |
| `stripped_spruce_log` | `stripped_oak_log` (this mod) | `stripped_birch_log` (this mod) |
| `stripped_birch_log` | `stripped_spruce_log` (this mod) | `stripped_jungle_log` (this mod) |
| `stripped_jungle_log` | `stripped_birch_log` (this mod) | `stripped_acacia_log` (this mod) |
| `stripped_acacia_log` | `stripped_jungle_log` (this mod) | `stripped_dark_oak_log` (this mod) |
| `stripped_dark_oak_log` | `stripped_acacia_log` (this mod) | `stripped_cherry_log` (this mod) |
| `stripped_cherry_log` | `stripped_dark_oak_log` (this mod) | `stripped_bamboo_block` (this mod) |
| `stripped_bamboo_block` | `stripped_cherry_log` (this mod) | `stripped_oak_wood` (this mod) |
| `stripped_oak_wood` | `stripped_bamboo_block` (this mod) | `stripped_spruce_wood` (this mod) |
| `stripped_spruce_wood` | `stripped_oak_wood` (this mod) | `stripped_birch_wood` (this mod) |
| `stripped_birch_wood` | `stripped_spruce_wood` (this mod) | `stripped_jungle_wood` (this mod) |
| `stripped_jungle_wood` | `stripped_birch_wood` (this mod) | `stripped_acacia_wood` (this mod) |
| `stripped_acacia_wood` | `stripped_jungle_wood` (this mod) | `stripped_dark_oak_wood` (this mod) |
| `stripped_dark_oak_wood` | `stripped_acacia_wood` (this mod) | `stripped_cherry_wood` (this mod) |
| `stripped_cherry_wood` | `stripped_dark_oak_wood` (this mod) | `cherry_wood` (this mod) |
| `cherry_wood` | `stripped_cherry_wood` (this mod) | `acacia_stairs` |
| `cherry_stairs` | `dark_oak_stairs` | `bamboo_stairs` (this mod) |
| `bamboo_stairs` | `cherry_stairs` (this mod) | `bamboo_mosaic_stairs` (this mod) |
| `bamboo_mosaic_stairs` | `bamboo_stairs` (this mod) | `prismarine` |

## Decoration Blocks

| Item | After | Before |
|---|---|---|
| `cherry_sapling` | `dark_oak_sapling` | `oak_leaves` |
| `pink_petals` | `red_mushroom` | `bamboo` (this mod) |
| `bamboo` | `pink_petals` (this mod) | `torch` |
| `chain` | `iron_bars` | `glass_pane` |
| `cherry_leaves` | `dark_oak_leaves` | `slime_block` |
| `cherry_fence` | `dark_oak_fence` | `bamboo_fence` (this mod) |
| `bamboo_fence` | `cherry_fence` (this mod) | `acacia_fence` |
| `cherry_sign` | `oak_sign` | `bamboo_sign` (this mod) |
| `bamboo_sign` | `cherry_sign` (this mod) | `oak_hanging_sign` (this mod) |
| `oak_hanging_sign` | `bamboo_sign` (this mod) | `spruce_hanging_sign` (this mod) |
| `spruce_hanging_sign` | `oak_hanging_sign` (this mod) | `birch_hanging_sign` (this mod) |
| `birch_hanging_sign` | `spruce_hanging_sign` (this mod) | `jungle_hanging_sign` (this mod) |
| `jungle_hanging_sign` | `birch_hanging_sign` (this mod) | `acacia_hanging_sign` (this mod) |
| `acacia_hanging_sign` | `jungle_hanging_sign` (this mod) | `dark_oak_hanging_sign` (this mod) |
| `dark_oak_hanging_sign` | `acacia_hanging_sign` (this mod) | `cherry_hanging_sign` (this mod) |
| `cherry_hanging_sign` | `dark_oak_hanging_sign` (this mod) | `bamboo_hanging_sign` (this mod) |
| `bamboo_hanging_sign` | `cherry_hanging_sign` (this mod) | `white_bed` |

## Redstone

| Item | After | Before |
|---|---|---|
| `cherry_pressure_plate` | `oak_pressure_plate` | `bamboo_pressure_plate` (this mod) |
| `bamboo_pressure_plate` | `cherry_pressure_plate` (this mod) | `redstone_torch` |
| `cherry_trapdoor` | `oak_trapdoor` | `bamboo_trapdoor` (this mod) |
| `bamboo_trapdoor` | `cherry_trapdoor` (this mod) | `oak_fence_gate` |
| `cherry_button` | `oak_button` | `bamboo_button` (this mod) |
| `bamboo_button` | `cherry_button` (this mod) | `trapped_chest` |
| `cherry_fence_gate` | `dark_oak_fence_gate` | `bamboo_fence_gate` (this mod) |
| `bamboo_fence_gate` | `cherry_fence_gate` (this mod) | `acacia_fence_gate` |
| `cherry_door` | `dark_oak_door` | `bamboo_door` (this mod) |
| `bamboo_door` | `cherry_door` (this mod) | (end of the tab) |

## Transportation

| Item | After | Before |
|---|---|---|
| `cherry_boat` | `dark_oak_boat` | `cherry_chest_boat` (this mod) |
| `cherry_chest_boat` | `cherry_boat` (this mod) | `bamboo_raft` (this mod) |
| `bamboo_raft` | `cherry_chest_boat` (this mod) | `bamboo_chest_raft` (this mod) |
| `bamboo_chest_raft` | `bamboo_raft` (this mod) | (end of the tab) |
