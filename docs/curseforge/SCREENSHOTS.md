# Trails & Tales Backport: screenshots

The twelve images for `DESCRIPTION.md`, one per `PENDING image` marker, in `screenshots/` as JPEG (1920x1080, all
under 1 MB). Every scene is hand-built in generated terrain: the mod's blocks and items sit where they would be used,
and every mob is posed (no AI, silent, invulnerable). The settings screen (`tt-settings`) is the user's own screenshot,
as on the other pages.

## Where they were taken

* **World:** `TrailsAndTalesShowcase`, seed 7, Caves & Cliffs world type, made with 1.20's biomes (Tricky Trials held
  out at creation, added back after). Time, weather and mob spawning are frozen; `randomTickSpeed` is 0.
* **Build server:** `G:\VBPTest\showcase120` (Forge 2860, ports 25698/25699, the ShowcaseHelper mod). Scene scripts:
  `tools\scenes\tt*.py` (re-running one rebuilds its scene; `tt3_trailruins.py` digs from the current surface, so it
  runs once, then `tt3b_dress.py`). `tools\shot.py` takes a picture in the connected client.
* **Client:** Prism instance `BackportShowcaseTT`, a copy of Backport Showcase at
  `G:\VBPTest\showcase120\client\BackportShowcaseTT` (OptiFine, Complementary Reimagined r5.7.1, FOV 70, 1920x1080).
* **Jars:** Backport Core 1.4.0, Trails & Tales 0.2.0, Caves & Cliffs 1.1.0, Village & Pillage 2.6.0, Update Aquatic
  1.1.1, Buzzy Bees 1.0.1, Nether Update 1.0.0, Tricky Trials 0.1.0 from their `build/libs` (2026-11-17), and the
  Wild Update 1.0.0 jar from the Backport Showcase instance (the new build fails a dedicated server boot, see the
  report).
* Each camera stands on a barrier block, so the teleport puts you exactly there.

## Shots

| # | File | Description section | Teleport | Time | In frame |
| --- | --- | --- | --- | --- | --- |
| 1 | `tt-cherry-grove` | Cherry groves | `/tp @p 597.5 95 -600.5 -88 10` | `/time set 10800` | A cherry-wood pavilion terraced into the slope of a generated cherry grove: stripped cherry posts, cherry stairs roof, fence rails, benches, a hanging lantern, a potted cherry sapling, pink petals everywhere, two rabbits |
| 2 | `tt-bamboo` | Bamboo wood | `/tp @p -948.5 66 -1819.5 50 6` | `/time set 11500` | A fisher's hut on blocks-of-bamboo stilts at a jungle lake: bamboo planks and mosaic, stripped bamboo corners, door, open trapdoor shutters, a mosaic-slab dock with a fence rail and lantern, a bamboo chest raft and raft, bamboo along the shore, late sun through the jungle |
| 3 | `tt-trail-ruins` | Archaeology, trail ruins | `/tp @p -1243.5 71 -1881.5 135 36` | `/time set 4000` | The generated trail ruins tower in a jungle clearing, its fill dug out (only the ruin's own terracotta, glazed terracotta and suspicious gravel left standing), one suspicious gravel half-brushed, scaffolding into the pit, a tent with decorated pots of trail ruins sherds and a hanging sign |
| 4 | `tt-dig-site` | Archaeology, decorated pots | `/tp @p -1862.5 81 -1384.5 96 15` | `/time set 3000` | A dig beside a desert well outside the village: the archaeologist (a posed armor stand in leather) brushing suspicious sand in the pit wall with a sherd showing, an emerald in the floor, a camp of decorated pots with sherd faces, a chest and a "Dig site" hanging sign |
| 5 | `tt-sniffer` | The sniffer | `/tp @p 663.5 88 -813.5 160 22` | `/time set 3000` | A fenced sniffer garden beside the smithy: the sniffer nosing a moss bed with dug patches, a snifflet facing you, a sniffer egg in a hay nest, azaleas, beds of torchflowers and pitcher plants in bloom and growing, a torchflower seed on the moss |
| 6 | `tt-camel` | Camels | `/tp @p -1804.5 85 -1413.5 88 3` | `/time set 2200` | A saddled camel carrying a wandering trader on the desert village road, a desert farmer walking beside it, the village and the badlands behind |
| 7 | `tt-armadillo` | Armadillos and wolves, first paragraph | `/tp @p -1804.5 97 -1428.5 -160 18` | `/time set 2200` | A badlands ledge above the village: an armadillo walking on red sand, one rolled up, a baby on the step behind, a scute on the ground, dead bushes, a cactus, terracotta bands |
| 8 | `tt-wolves` | Armadillos and wolves, second paragraph | `/tp @p 947.5 79 -317.5 88 6` | `/time set 2500` | A camp in an old-growth pine taiga clearing: a tamed wolf in teal-dyed wolf armor sitting by the campfire, the local pack of black wolves at the trees, a wool tent, bedroll, chest and lantern |
| 9 | `tt-trims` | Armor trims | `/tp @p 649.5 87 -812.5 176 4` | `/time set 6000` | The village smithy: iron armor with a gold coast trim and diamond armor with an emerald wild trim on posed armor stands, the netherite upgrade and coast templates framed over the smithing table, a campfire hearth, anvil, grindstone |
| 10 | `tt-shop` | Blocks, first image | `/tp @p 643.5 87 -811.5 -60 2` | `/time set 10200` | The village lane: the "Bakery" cherry hanging sign on chains under a beam, the "Smithy" spruce hanging sign across the lane, a decorated pot, potted torchflower and barrels by the cherry-plank bakery, a lamp post, hay and oaks at the lane's end |
| 11 | `tt-study` | Blocks, second image | `/tp @p 635.5 87 -803.5 -47 6` | `/time set 5000` | A study: chiseled bookshelves with gaps among plain shelves around the window, a desk with a skeleton skull and lit candles, a brick fireplace with a lit campfire and a piglin head on the mantel |
| 12 | `tt-sensor` | Blocks, third image | `/tp @p 636.5 81 -803.5 -12 13` | `/time set 6000` | The study's cellar: a calibrated sculk sensor (active, its amethyst crown) set by a comparator reading a chiseled bookshelf, redstone dust lighting floor and wall lamps, amethyst and a note block beside it |

## Markers in DESCRIPTION.md

Each image replaces its `<!-- PENDING image: tt-... -->` line once it is on the project's Media page.

## Known limits

* Frozen (NoAI) camels keep facing south whatever their saved yaw, and a camel summoned with two riders drops the
  second, so scene 6 has one rider and a villager walking beside it.
* A NoAI sniffer can't be posed mid-dig (its state isn't saved), so scene 5 shows it standing over the moss it has
  dug, with the seed beside it.
