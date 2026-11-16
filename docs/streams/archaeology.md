# Stream A: archaeology

Package `archaeology`. Config `archaeology` (`enabled`). Packets 40-41 (42-44 free). Tile entities
`brushable_block` and `decorated_pot`. Behavior and numbers from the decompiled 1.20.1 and 1.21.11 sources and
the 1.20.6 jar's data (loot tables, tags, recipes, advancements).

## Changelog section

*   **Brush** (feather, copper ingot, stick; 64 durability): hold use on a block to brush it. Dust flies off the
    face every half second with the block's brushing sound.
*   **Suspicious sand** and **suspicious gravel**: brush one for about five seconds to uncover what is buried in
    it; it shows more of the item as it is dusted, then drops it and turns into sand or gravel. Stop brushing and
    the dust settles back. They fall when nothing is under them and break where they land. Mining one gives
    nothing.
*   **20 pottery sherds**: angler, archer, arms up, blade, brewer, burn, danger, explorer, friend, heart,
    heartbreak, howl, miner, mourner, plenty, prize, sheaf, shelter, skull and snort.
*   **Decorated pot**: four bricks or sherds in a diamond; each sherd's pattern is drawn on its side. It holds one
    stack of one item: use an item on it to put one in. Hoppers fill and empty it, and a comparator reads how full
    it is. It wobbles when used. Broken with a tool or hit by a projectile it shatters into its sherds and bricks;
    broken by hand, with Silk Touch or by a piston it drops itself with its patterns. What it holds spills out.
*   Archaeology loot for desert pyramids, desert wells, cold and warm ocean ruins and trail ruins (common and rare).
*   Advancements **Respecting the Remnants** and **Careful Restoration**.

## How it works

### Brush (`ItemBrush`)

*   1.20's `BrushItem`: using it on a block starts a 200-tick use (`EnumAction.NONE`, so no bow or eating pose).
    Each tick it ray-traces the player's view within block reach (4.5, creative 5), entities included; an entity in
    the way or no block stops the use. Elapsed ticks 5, 15, 25... are strokes: 7-11 block-crack particles swept
    along the face (client), the block's brushing sound (`item.brush.brushing.sand`/`gravel`, else `generic`),
    and on a suspicious block one brush of its tile entity. Finishing a block costs 1 durability.
*   `onItemUse` returns FAIL (vanilla's CONSUME: no arm swing); while brushing, the other hand's right-clicks are
    cancelled so it cannot place or use its item.
*   The `brushing` item property (stroke progress) picks 1.20.6's `brush_brushing_0..2` item models; first-person
    the brush sweeps with 1.20's `applyBrushTransform` (`RenderSpecificHandEvent`).

### Suspicious sand and gravel (`BlockSuspicious`, `TileSuspicious`)

*   `dusted` 0-3 in metadata. Vanilla's `BrushableBlockEntity` numbers: 10-tick stroke cooldown, reset 40 ticks
    after the last stroke, 10 strokes finish it (the tenth on tick 95 of brushing, the "96 ticks" of the plan);
    dusted 1 from 1 stroke, 2 from 3, 3 from 6; left alone it loses 2 strokes every 4 ticks.
*   The first stroke rolls the loot table (with the brushing player's luck) and keeps the one item. The finish drops
    it on the brushed face (half a block out, no motion), sends vanilla's level event 3008 as packet 40 (the
    brush-completed sound and the break particles), and sets sand or gravel.
*   Falling: every placement and neighbor change schedules a 2-tick tick; with air (or anything sand falls through)
    below it spawns a falling block that drops nothing. 1.12.2's falling entity places the block where it lands,
    so `onEndFalling` removes it at once with the 2001 break effect - vanilla's falling suspicious block never
    becomes a block again. Worldgen's instant falling is skipped (vanilla has none for these).
*   Drops nothing mined, with Silk Touch, pushed (piston: destroy) or blown up. Falling dust in the block's map
    color, as 1.20 (they are not falling blocks there).
*   Sounds: 1.20.6's `block.suspicious_sand/gravel.*` and the completed sounds `item.brush.brushing.*.complete`.
    1.21.11 plays the plain brushing sound on completion; this follows 1.20.6, which still has the complete sounds.

### Structures and other mods: how to place them

Stream St (trail ruins, desert pyramids and wells, ocean ruins) and Caves & Cliffs' 1.18 desert pyramids and
wells place them by name, with no link to this code:

1.  Set `trailsandtalesbackport:suspicious_sand` or `trailsandtalesbackport:suspicious_gravel` (default state).
2.  In its tile entity, write `LootTable` (string) and `LootTableSeed` (long, 0 for random) into the NBT
    (`te.writeToNBT`, set the two keys, `te.readFromNBT`), or put them in a structure template's block NBT.

The tables are `trailsandtalesbackport:archaeology/desert_pyramid`, `desert_well`, `ocean_ruin_cold`,
`ocean_ruin_warm`, `trail_ruins_common` and `trail_ruins_rare`; any other table works too. Inside this mod,
`ArchaeologyContent.placeSuspicious(world, pos, gravel, table, seed, flags)` does the same. The self-test places
them the by-name way.

### Loot tables (`ArchaeologyLoot`, `tools/archaeology/generate.py`)

*   1.20.6's six tables, one roll, the same weights. The JSON holds every entry 1.12.2 always has (1.12.2's
    metadata for dyes and panes: yellow 11, light blue 12, orange 14; the 1.20 names stay as entry names). Entries
    another stream or mod owns are added on `LootTableLoadEvent` only when the item exists:
    *   desert well: Village & Pillage's suspicious stew with 1.20.6's `set_stew_effect` (one of night vision,
        jump boost, weakness, blindness, poison, saturation; seconds from 1.20.6's ranges, ticks for saturation),
        in 1.14's stew NBT;
    *   warm ocean ruins: the sniffer egg (stream N);
    *   common trail ruins: blue and white dye (Village & Pillage's, else 1.12.2's own blue and white dye, lapis
        lazuli and bone meal), the red, green, purple and brown candles (Caves & Cliffs), the spruce and oak
        hanging signs (stream H);
    *   rare trail ruins: the wayfinder, raiser, shaper and host templates (stream Sm) and the disc Relic (St).
*   With this mod alone the sniffer egg, templates, Relic, candles and stew are not in the tables yet; each joins
    its table as soon as its stream or mod exists (the self-test counts the entries either way).

### Sherds

*   20 plain items, uncommon (yellow names, as current Java), each in the `potterySherd` ore name. The pot recipe
    and renderer take any `potterySherd` item, so a later update's sherds (flow, guster, scrape) need no code here:
    a sherd `<ns>:<x>_pottery_sherd` is drawn with `<ns>:textures/entity/decorated_pot/<x>_pottery_pattern.png`
    when that texture exists.

### Decorated pot (`BlockDecoratedPot`, `TileDecoratedPot`, `ItemDecoratedPot`, `RecipeDecoratedPot`)

*   States: horizontal `facing` (the player's facing when placed) and `cracked`, in metadata. Hardness 0, push
    reaction destroy, 14-wide shape; drawn only by its renderer (`ENTITYBLOCK_ANIMATED`).
*   NBT as 1.20.4: `sherds` (back, left, right, front item ids; left out when all bricks), `item`, and
    `LootTable`/`LootTableSeed` for a structure's pot. The item keeps `sherds` under `BlockEntityTag`, which 1.12.2
    copies into the tile entity on placement; a plain pot item has no NBT, so plain pots stack. Tooltip as 1.20:
    a blank line, then front, left, right, back in gray, unless all bricks.
*   Recipes: 1.20's `decorated_pot_simple` (four bricks in a diamond) and `crafting_decorated_pot` (four bricks
    or sherds in a diamond: top = back, left, right, bottom = front; exactly four items on a 3x3 grid).
*   Use (1.20.4): an item goes in if the pot is empty or holds the same item (and NBT) below a full stack: positive
    wobble, use statistic, `block.decorated_pot.insert` at pitch 0.7 + 0.5 x fill, seven `dust_plume` particles
    1.2 above (packet 41; 1.20.3's particle rebuilt on 1.12.2's generic smoke sprites). Anything else, or an empty
    hand: `insert_fail` and the negative wobble. Creative keeps the item. Wobbles are block event 1 (positive 7
    ticks, negative 10), drawn with 1.21.11's renderer math.
*   Hoppers: Forge item handler, one slot, same-item stacking to the item's stack size, the caller's stack never
    stored. Comparator: vanilla's container signal for one slot.
*   Breaking: vanilla's `playerWillDestroy` is a `BreakEvent` (lowest priority): with a sword, axe, pickaxe,
    shovel, hoe or trident (1.20's `breaks_decorated_pots`; tools by tool class) and no Silk Touch, the pot is
    set cracked before the break, so the break effect is the shatter sound and the drops (`getDrops`, from the
    tile entity kept until after harvesting, as the flower pot) are its four faces. Otherwise it drops itself with
    its faces. The breaking player's own client hears the shatter too (its break effect asks the block with no
    entity; the client checks the looked-at pot and the held tool). Contents spill in `breakBlock`, however it
    goes.
*   Projectiles (1.20's `impact_projectiles`: arrows, snowballs, eggs, fireballs including wither skulls and
    dragon fireballs, tridents by name) shatter it through `ProjectileImpactEvent`, as `mayInteract`: a player's
    projectile where that player may edit, a mob's only with `mobGriefing`, an ownerless one always.
*   Renderer: 1.20's `DecoratedPotRenderer` model part for part on 1.12.2 `ModelRenderer`s (neck and rim with
    their -0.1/+0.2 inflation, top, bottom, four single-face sides); the item uses the same model through a
    `TileEntityItemStackRenderer` and 1.20.6's `builtin/entity` item model and display transforms.

### Advancements

*   *Respecting the Remnants* (`adventure/salvage_sherd`): one `impossible` criterion per archaeology table, granted
    when a player's brushing rolls that table (1.20's `player_generates_container_loot`), and one
    `inventory_changed` criterion per sherd, in 1.20's two requirement groups.
*   *Careful Restoration* (`adventure/craft_decorated_pot_using_only_sherds`): granted on `ItemCraftedEvent` for a
    pot whose four faces are all sherds (1.20's `recipe_crafted` with four sherd ingredients). Its icon is 1.20's
    heart and explorer pot.

## Not in this stream

*   Brushing armadillos for scutes (with a dispenser too): stream Ar.
*   The structures themselves (St) and Caves & Cliffs' own placement.
*   Without Caves & Cliffs (or another mod's `ingotCopper`) the brush has no recipe: the copper ingot copy is one
    of *Decided*'s copies, not built yet.
*   Vibrations from pots and landing suspicious blocks: Core's game events (`CORE_REQUESTS.md` 3).
*   Waterlogged pots: the later waterlogging pass.
*   The third-person brushing arm pose (1.20's `ArmPose.BRUSH`): built on `tt-leftovers`
    (`lookfeel.client.ArmPoses`, on Core's player model hook).

## Tested headlessly

`./gradlew runServer -Pselftest=all` (port 25888), all streams: **`pass=3007 fail=0`**; `check_assets.py`:
**73 blocks, 1121 states, 83 item models, 521 sound files, 0 errors**.

*   States and metadata round trips, hardness, harvest tool, push reactions, 20 uncommon sherds in `potterySherd`,
    brush durability, stack size and use duration.
*   Recipes: brush (with a test-only `ingotCopper` stand-in, since the dev environment has no copper mod), the
    plain pot, a sherd pot with each face in the right place, and three grids that must not craft.
*   All six tables load and roll exactly one item, 400 rolls each; their entry counts with and without the
    optional owners; every sherd is in some table.
*   A real player brushing suspicious sand placed by name with `LootTable`/`LootTableSeed` NBT, tick by tick:
    the first stroke on tick 5, the loot rolled then (NBT switches from the table to the item), dusted 1/2/3 at
    1/3/6 strokes, still suspicious on tick 94, sand on tick 95, one durability used, the item dropped centered one
    block out on the brushed side with no motion; the advancement criterion of that table.
*   The same seed giving the same item; `placeSuspicious`; mined with a shovel and with Silk Touch: nothing.
*   The reset: nothing lost before 40 ticks, then 2 strokes per 4 ticks, the dusted state following, the face
    forgotten at 0, the rolled item kept.
*   Falling: unsupported, it becomes a falling block with drops off, falls ten blocks, breaks on landing, leaves
    no block and no item.
*   Pot: placed by a player facing them with the item's faces; pick block keeps them; inserting by hand (one at
    a time, another item refused, empty hand), comparator 1 and 15, the wobble event; the hopper handler (fills to
    64, hands back the rest, leaves the caller's stack alone, refuses other items, extracts); NBT round trip;
    broken by hand: drops itself with its faces and spills 63 diamonds.
*   Breaking: pickaxe and sword shatter it into its sherds and bricks; Silk Touch and a stick drop the pot; an
    arrow's impact event shatters it; the tool list; *Careful Restoration* granted for four sherds, not with a
    brick.

## Not tested

*   Everything drawn or heard on a client: the pot and buried-item renderers, the pot item, the brush models and
    first-person sweep, the dust and dust-plume particles, the wobble animation, the 3008 effect, sounds playing,
    the breaking player hearing the shatter.
*   A real arrow flying into a pot (the impact event was posted directly), a piston pushing one, a hopper block
    ticking into one.
*   With Village & Pillage, Caves & Cliffs or Update Aquatic installed (their stew, dyes, candles; ocean ruins).
