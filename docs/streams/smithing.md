# Stream Sm: smithing templates and armor trims

Package `smithing` (and `smithing.copy` for the copies), config `smithing`, branch `tt-smithing`. Needs Backport
Core 1.4.0 (branch `core-armor-trims`: `smithing.SmithingTemplates`, `smithing.ArmorTrims` and their client).

## Changelog

- 2026-11-16: the stream: 17 templates, 16 patterns x 10 materials, the 1.20 table and screen, template loot,
  advancements, copper and amethyst copies. Core 1.4.0 holds the shared smithing and trim code.

## What it adds

- **Templates**: the netherite upgrade and the sixteen 1.20 trim templates (coast, dune, eye, host, raiser, rib,
  sentry, shaper, silence, snout, spire, tide, vex, ward, wayfinder, wild), vanilla's item names. All named
  "Smithing Template" with 1.20's lines under the name (pattern or "Netherite Upgrade" in gray; "Applies to:" and
  "Ingredients:" with their answers in blue). Common rarity (1.20.6's). Ore name `armorTrimTemplate` (Tricky
  Trials' bolt and flow use it too).
- **Duplication**: 7 diamonds + the template + its block make two (1.20.6's recipes). Cobbled deepslate (silence,
  ward) is Caves & Cliffs', blackstone (snout) Nether Update's: by name, and the recipe waits for that mod.
- **Trims**: any armor piece (`ItemArmor`) + a trim template + a material. Materials in 1.20's order and colors:
  quartz, iron, netherite (any netherite mod's ingot, as Core finds them), redstone, copper (`ingotCopper`), gold,
  emerald, diamond, lapis lazuli, amethyst (`gemAmethyst`). Iron, gold, diamond and netherite trims use their
  `_darker` palette on armor of their own kind. Stored as `Trim: {pattern, material}` (1.20.4's tag; our ids).
  The same trim again makes nothing; another trim replaces it.
- **Netherite upgrades need the template** (`smithing.netheriteNeedsTemplate`, default true, 1.20's rule). The
  result keeps the base's enchantments, name, trim and damage.
- **The table**: 1.20's three-slot container and screen (`ContainerSmithing`, `GuiSmithing`): slots at 1.20's
  places, each taking only what fits, shift-click sorting template/base/addition, the red cross with its tooltip,
  empty-slot icons cycling every 1.5 s with 1.20's fade, the hover hints ("Add Smithing Template", "Add a piece of
  armor", ...), the armor stand preview at (141, 75) wearing or holding the result, the table's use sound.
  It opens on our copy of the smithing table (Village & Pillage's block, 1.14 recipe; only without Village &
  Pillage and Nether Update) and on Village & Pillage's and Nether Update's tables (see *How it works*).
- **Loot**, 1.20.6's pools and odds: desert pyramid (dune, 1/7, x2), jungle temple (wild, 1/3, x2), nether
  fortress (rib, 1/15), end city (spire, 1/15), stronghold corridor (eye, 1/10) and library (eye, always),
  woodland mansion (vex, 1/2), elder guardian (tide, 1/5); with their owners: pillager outpost (sentry, 1/4, x2,
  Village & Pillage), shipwrecks (coast, 1/6, x2, Update Aquatic), bastions (snout 1/12, netherite upgrade 1/10,
  treasure always, Nether Update), ancient city (ward 4/80, silence 1/80, Wild Update). Trail ruins' four are
  stream A's. **Fallback** (Decided 3, only while the owner is absent, same odds): sentry in woodland mansions,
  coast in dungeons, snout and the upgrade in nether fortresses, ward and silence in abandoned mineshafts.
- **Advancements**: *Crafting a New Look* (any trim), *Smithing with Style* (the eight exclusive patterns,
  challenge, 150 XP); criteria granted when a trimmed piece is taken from the table.
- **Copies** (Rule 1, owner Caves & Cliffs; Core's ownership table now lists them, so they are renamed onto the
  owner's when it is added):
  - **Copper** (when smithing or archaeology is on, and neither Caves & Cliffs nor Tricky Trials is installed):
    copper ore (2-5 raw copper, Fortune), raw copper, copper ingot, smelting (0.7 XP), veins (6 a chunk, up to
    10, y 1-96, stone; `smithing.copperOre`). Gives the brush its copper. Tricky Trials' classes (its copy of the
    owner's).
  - **Amethyst** (when smithing or the calibrated sculk sensor is on, without Caves & Cliffs): amethyst geodes
    (1 in 24 chunks, y 6-46, `smithing.geodes`), block of amethyst, budding amethyst, the three buds and the
    cluster, the shard, calcite and smooth basalt (the geodes' shell). The owner's classes, states, numbers and
    sounds; its chimes.
  - `tools/smithing/copy_from_owner.py` copies the owners' classes into `smithing.copy`; rerun when they change.

## How it works

- **Core** does the smithing and the trims (`docs/CORE_REQUESTS.md` 1; Core branch `core-armor-trims`, version
  1.4.0, three commits). `SmithingTemplates`: transforms, trims and the netherite upgrade template; while upgrades
  need the template, `SmithingUpgrades.find` answers null so no two-slot table skips it. `ArmorTrims`: patterns,
  materials, NBT. Its client builds 1.20's paletted textures at runtime from the grayscale patterns and the
  palettes this mod ships (`textures/trims/...`), replaces each renderer's plain biped armor layer with one that
  draws armor, trim, then glint per piece (vanilla's own `renderArmorLayer` still draws the armor), wraps armor
  item models in vanilla's shapes with the overlay, and adds the tooltip lines in the material's exact color.
- **This mod** registers the templates, patterns (`Content.id(pattern)`, textures under our namespace), materials
  and the upgrade template (`SmithingContent`), and declares its container as a table with the template slot.
- **Other tables** (`SmithingTables`): Village & Pillage's and Nether Update's tables are 1.16's two-slot table.
  Until their owner declares the slot (`SmithingTemplates.declareTableSupport`), right-clicking either opens our
  container on that block, so the template requirement and trims work with them installed. When the owner's code
  has the slot, its own screen is left alone. Sneaking with an item in hand places it, as vanilla.
- **GUI handler**: `SmithingGui` is this mod's one Forge GUI handler (id 0 = smithing); another stream needing a
  screen adds its id there.

## Tested

- `./gradlew runServer -Pselftest=smithing` (port 25892): **103/103**. Registration, ore names, 16 patterns and
  10 materials, duplication recipes (and none for a wrong block), our table copy and its recipe, trims (pattern,
  material, NBT, enchantment and damage kept, palette and darker palette, the same trim twice, a new material,
  leather, swords refused, invalid inputs refused, unknown pattern kept), taking the result (each input used,
  advancements), netherite upgrades with a stand-in upgrade (template required, two-slot find null, name and
  enchantments kept, setting off), shift-click sorting and returning inputs on close, loot rates over 2000 rolls
  per table (vanilla tables and fallbacks), copper copy (harvest level, drops, smelting, material, brush recipe,
  vein placement), amethyst copy (material, block recipe, cluster drops, a geode's three layers, budding growth).
- `python tools/check_assets.py`: 0 errors.
- `./gradlew runClient -Pselftest=trimrender` (`ClientSmithingTest`, **7/7**): in a superflat world, five armor
  stands in trimmed iron (iron trim, darker palette), diamond (gold silence, enchanted chestplate: glint over the
  trim), gold, leather and chainmail armor; trimmed icons of every vanilla armor material (darker palettes,
  netherite trim, leather); tooltips (the trim after the item's own lines and before enchantments, in each
  material's exact color; the template's 1.20 lines); the 1.20 screen with a rib/emerald trim in the result and
  the armor stand preview. Screenshots land in `eclipse/smithing/screenshots/` (worn, icons, screen); checked by
  eye. 11 armor layers replaced and 20 armor item models wrapped in a plain dev run.

## Not tested / not done

- With Village & Pillage, Nether Update, Update Aquatic, Wild Update or Caves & Cliffs installed (owners' loot
  tables, duplication with cobbled deepslate and blackstone, the table interception, copies switched off).
- A real netherite mod (the upgrade was tested with a stand-in upgrade).
- JEI: no smithing category for the three-slot recipes yet (Village & Pillage's JEI category is two-slot).
- Trims on armor with its own model (`getArmorModel`) are not drawn worn (its UVs are not vanilla's); their icons
  show no overlay unless the mod is listed with `ArmorTrims.drawOnIcon` (as 1.20 mod armor without trim models).
- Languages other than English (left to the integrator's lang tool, as the plan says); Core's "Upgrade:" line has
  all 19.
