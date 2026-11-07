# Backport Core requests from Trails & Tales Backport

What this mod needs from Backport Core (`E:\MincraftModding\Workspaces\BackportCore`) that Core 1.0.0 lacks,
with the API asked for. Nothing here is built in Core by this mod: Core is not ours to edit. Until each
exists, this mod either waits (said per item) or carries a small local shim with the same shape in its own
package, to be deleted when Core has the real thing.

Requests 1, 5 and 6 also change what **Village & Pillage** does; the user decided on 2026-11-07 that they
go ahead (`PLAN.md`, *Decided* 2 and 6). The V&P side is written up for its maintainer in the family notes
(`trails-and-tales-requests-for-village-and-pillage.md`).

## 1. Smithing templates and armor trims (the delicate one)

### What 1.20 does

The smithing table has three inputs: **template**, **base**, **addition**. Netherite upgrades need the
netherite upgrade template (consumed, one per craft). Armor trims are template + any armor piece + a trim
material, and write the trim onto the armor's NBT. There is no two-slot smithing in 1.20.

### What exists

- Core's `smithing.SmithingUpgrades`: two-slot upgrades (`find(base, addition)`), gathered from every
  netherite mod; `configure(...)` belongs to the table's owner.
- Village & Pillage's `block.ContainerSmithingTable`: 1.16's two-slot table (`SLOT_BASE` 0, `SLOT_ADDITION` 1,
  `SLOT_RESULT` 2), its `GuiSmithingTable` and the JEI category `compat.JeiSmithing`. V&P's table, V&P's code.

### Asked for in Core: `smithing.SmithingTemplates`

```java
// Who brings templates (this mod, later Tricky Trials for bolt and flow).
SmithingTemplates.requireForUpgrades(String modId, Ingredient upgradeTemplate, BooleanSupplier on);

// Three-slot recipes. A transform copies the base's NBT onto the result (netherite-style);
// a trim hands base + addition to the applier, which returns the trimmed copy or EMPTY.
SmithingTemplates.addTransform(Ingredient template, Ingredient base, Ingredient addition,
                               ItemStack result, String source);
SmithingTemplates.addTrim(Ingredient template, Predicate<ItemStack> base, Ingredient addition,
                          TrimApplier applier, String source);

// The table owner says its table has the template slot (V&P's new version; our own copy when V&P is absent).
SmithingTemplates.declareTableSupport(String tableModId);

// What a table and a recipe viewer read.
boolean SmithingTemplates.templateSlot();          // a template source is on AND a table supports it
boolean SmithingTemplates.isTemplate(ItemStack);
@Nullable Result SmithingTemplates.find(ItemStack template, ItemStack base, ItemStack addition);
    // Result: ItemStack result(), boolean consumesTemplate() (true for every 1.20 recipe)
List<Recipe> SmithingTemplates.list();             // for JEI: template, base, addition, example result
```

**Rules inside Core**

- While `templateSlot()` is true, every `SmithingUpgrades` upgrade (from any netherite mod) is offered only
  as template + base + ingot, with the registered upgrade template. `SmithingUpgrades.find(base, addition)`
  keeps working for two-slot callers, but returns null while `templateSlot()` is true, so no table can skip
  the template.
- `templateSlot()` is false unless some table declared support. An older Village & Pillage (no slot) with this
  mod installed therefore keeps two-slot netherite and simply has no trims; this mod logs one line saying so.
  Nobody is ever left unable to make netherite.
- Config (owned by this mod, read by Core through the supplier): `smithing.netheriteNeedsTemplate`, default
  true, 1.20's rule.

### Asked for in Core: `smithing.ArmorTrims` (so Tricky Trials' bolt and flow work the same way)

```java
ArmorTrims.registerPattern(ResourceLocation id, Item template, ResourceLocation texture);   // trims/models/armor/<x>
ArmorTrims.registerMaterial(ResourceLocation id, Ingredient item, String paletteSuffix, int tooltipColor);
@Nullable ArmorTrims.Trim ArmorTrims.get(ItemStack armor);   // {pattern, material}
ItemStack ArmorTrims.apply(ItemStack armor, pattern, material);
```

- NBT: `{Trim: {pattern: "<modid>:<name>", material: "<modid>:<name>"}}` on the armor stack. Unknown ids are
  kept and ignored (a removed mod's trim comes back when the mod does).
- Client: one armor layer added to every `RenderLivingBase` that has a `LayerBipedArmor` (players, zombies,
  skeletons, armor stands, other mods' bipeds), drawing the trim over each worn piece; item icons through a
  wrapping baked model added in `ModelBakeEvent` for armor items (by exact model name, never by iterating
  the registry: VintageFix bakes lazily); the tooltip lines.
- Textures: this mod generates the paletted textures at build time (pattern x material x layer) and ships
  them; Core only needs the naming rule `<modid>:textures/trims/models/armor/<pattern>_<material>[_leggings].png`.

### Asked of Village & Pillage's maintainer (through the user)

- `ContainerSmithingTable`: when `SmithingTemplates.templateSlot()` is true, lay out 1.20's slots (template
  8,48; base 26,48; addition 44,48; result 98,48) and call `SmithingTemplates.find`; consume one template.
  Otherwise stay exactly as now. `declareTableSupport("villagerbackport")` in pre-init.
- `GuiSmithingTable`: 1.20's screen texture and the armor-stand preview when in template mode.
- `JeiSmithing`: a three-input category from `SmithingTemplates.list()`.

### Meanwhile here

- Without Village & Pillage and Nether Update: this mod registers its own copy of the smithing table (Rule 1),
  already in three-slot form, under `trailsandtalesbackport:smithing_table`, same block states as V&P's.
- The template requirement is on by default (`smithing.netheriteNeedsTemplate`, 1.20's rule).
- The `smithing` stream builds templates, trims and the client parts against a local `SmithingTemplates` /
  `ArmorTrims` shim with exactly the API above, used by our copied table. With V&P installed, nothing changes
  on its table until the request is done; the templates are still obtainable and duplicable.

## 2. Modern structure templates, jigsaw, and structure hooks

Shared with Update Aquatic's ocean structures (its stream S) and every later structure.

```java
StructureTemplate t = StructureTemplates.load(ResourceLocation nbtFile);  // 1.13+ NBT with a block palette
    // palette names ("minecraft:mud_bricks", "minecraft:suspicious_gravel") resolve through
    // BlockNames.resolve(...) to 1.12.2 states: vanilla flattening table + every family mod's names
    // (via BlockOwnership) + a per-call fallback
t.place(World, BlockPos, PlacementSettings, Random, BlockProcessor...);
JigsawAssembler.assemble(World, ResourceLocation startPool, BlockPos, int depth, Random)  // pools from json
StructureHooks.onPiecePlaced(String structureName, PieceCallback cb);    // for other mods' archaeology
StructureHooks.blockEntityData(String structureName, BlockEntityFiller f); // loot tables for suspicious blocks
```

- Trail ruins are 1.20 jigsaw (`trail_ruins/tower`, `roads`, `buildings`, `decor`), with archaeology
  processors that turn gravel into suspicious gravel with a loot table. We ship the NBT files, pools and
  processors; Core does the loading, palette mapping, jigsaw and terrain fitting (its `TerrainHeights`).
- `StructureHooks` lets this mod add suspicious sand and gravel to Update Aquatic's ocean ruins without either
  mod knowing the other's classes.
- **Waiting**: trail ruins and ocean-ruin archaeology wait for this. Desert pyramids and wells don't (they are
  built in code in 1.12.2 and edited after placement here).

## 3. Vibrations (game events)

Shared with Wild Update's sculk sensor, shrieker, warden and allay. The calibrated sculk sensor listens to the
same events.

```java
GameEvents.post(World, BlockPos, GameEvent event, @Nullable Entity source);   // the 1.19+ events and frequencies
GameEvents.listen(World, BlockPos, int range, VibrationListener listener);    // chunk-registered listeners
int GameEvent.frequency();                                                    // 1-15, as 1.20
```

- Decided (`PLAN.md` *Decided* 4): the calibrated sensor always exists; when Wild Update is absent this mod
  also registers its copy of the sculk sensor. Both use this API; until Core has it, a local shim with the
  same shape in our `sculk` package (vibrations from the 1.20 event list this mod can see: steps, block
  place/break, item use, projectiles, eating, doors, containers...).

## 4. Boats, rafts and chest boats

Every wood from 1.16 on has a boat; chest boats are Wild Update's (1.19).

```java
BoatTypes.register(String modId, String wood, Item planks, ResourceLocation texture,
                   boolean raft, @Nullable ResourceLocation chestTexture) -> BoatType
Item BoatTypes.boatItem(BoatType), chestBoatItem(BoatType)    // registered under the registering mod's id
```

- One boat entity and one chest-boat entity in Core (ids in Core), with the type saved by name, so a world
  keeps every boat whichever mods are installed.
- **Meanwhile**: our own `EntityBoat` subclass and chest-boat entity for cherry and bamboo only, with the same
  NBT field (`Type: "trailsandtalesbackport:cherry"`), so moving onto Core is a remap of two entity ids.

## 5. Signs: back text and waxing, one place for everyone's signs

1.20 signs have text on both faces, per-face dye and glow, and a waxed flag. Vanilla's, V&P's wood signs, ours
and every later wood's signs need the same data and the same renderer behavior.

```java
SignData SignData.of(TileEntitySign sign);   // capability on every TileEntitySign: back text (4 lines),
                                             // front/back color and glow, waxed
void SignData.drawBack(...)                  // client helper a sign renderer calls after drawing the front
```

- Decided (`PLAN.md` *Decided* 6): on every sign, V&P's included. With this in Core, V&P's
  `TileEntityWoodSignRenderer` and `TileEntityDyedSignRenderer` get the back face from one call.
  **Meanwhile** the capability lives in our `sign` package under the same name. Where a renderer can't be
  reached without it (vanilla's `TileEntitySignRenderer` when V&P's dyed-sign renderer replaced it), a small
  patch of our own `asm` coremod is allowed.

## 6. Village hooks

1.20's camels spawn in desert villages, which are Village & Pillage's villages (and Buzzy Bees' bee nests in
plains villages need the same).

```java
VillageHooks.onVillageGenerated(VillageCallback cb);   // (World, StructureBoundingBox, villageType, Random)
    // villageType: "plains", "desert", "savanna", "taiga", "snowy"; called by whichever mod built the village
```

- V&P would call it after placing a village; Core also calls it for vanilla 1.12.2 villages it can see. Asks
  V&P's maintainer for one call. **Meanwhile** this mod spawns camels by vanilla 1.12.2 desert villages only.

## 7. Ownership table entries

Not a code request: this mod declares, from pre-init, the blocks it ships first so later mods find them:
`BlockOwnership.declare("trailsandtalesbackport", ...)` for the cherry and bamboo sets, hanging signs, chiseled
bookshelf, suspicious sand and gravel, decorated pot, sniffer egg, torchflower, pitcher plant, pink petals,
calibrated sculk sensor and piglin head. Listed here so Core's maintainer can pre-fill them as V&P's are.

## 8. Textures and sounds for Core's packs

- 1.20.2: jukeboxes and tripwire hooks use wood sounds; levers, comparators and repeaters stone sounds;
  1.20.2 sponge sounds; 1.20.5 cobweb sounds. These are block sound *types* (code), done in this mod's `misc`
  stream, not pack entries.
- 1.20's nether wood sounds belong on Nether Update's crimson and warped blocks: that is Nether Update's (or a
  Core resound), not ours to set on its blocks. Noted here for whoever owns it.
- No 1.20 vanilla retextures of existing blocks found yet that Core's `modern` pack lacks; the bat (1.20.3) is a
  new model, drawn by our renderer.

## 9. Waterlogging

When Core's `Waterloggable` adapter exists: hanging signs and wall hanging signs (all woods), our chain copy,
the calibrated sculk sensor, the decorated pot, and our cherry and bamboo stairs, slabs, fences, fence gates,
trapdoors and signs.
