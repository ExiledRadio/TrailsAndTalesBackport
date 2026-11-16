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

- The armadillo (stream Ar) posts `ENTITY_ACTION` when it rolls up or unrolls, `ENTITY_PLACE` when it sheds a
  scute and `ENTITY_INTERACT` when brushed. None is posted yet: they wait for this API
  (`armadillo/EntityArmadillo.rollUp`, `rollOut`, `updateAITasks`, `brushOffScute`).

- Decided (`PLAN.md` *Decided* 4): the calibrated sensor always exists; when Wild Update is absent this mod
  also registers its copy of the sculk sensor. Both use this API; until Core has it, a local shim with the
  same shape in our `sculk` package (vibrations from the 1.20 event list this mod can see: steps, block
  place/break, item use, projectiles, eating, doors, containers...).
- Stream A's events, posted once this exists (none are posted today): `BLOCK_CHANGE` from a decorated pot
  taking an item or wobbling empty-handed (source the player), and `BLOCK_DESTROY` where a falling suspicious
  block breaks on landing.
- The sniffer (stream N) posts 1.20's `ENTITY_SHAKE` (current Java: `ENTITY_ACTION`) at its head block every
  10 ticks while digging, and a placed sniffer egg posts `BLOCK_PLACE`. Neither is posted yet: they wait for
  this API (`sniffer/EntitySniffer.emitDiggingParticles`, `sniffer/BlockSnifferEgg.onBlockAdded`).

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
  V&P's maintainer for one call. **Meanwhile** (stream Ca, `camel/CamelVillages`) this mod reads villages from
  the world's saved structure data on chunk population: V&P's desert town centers by its piece NBT (`Pool`,
  `Element`, `PosX/Y/Z`, `Rotation`) and its `pools.json` by resource name, vanilla 1.12.2's by `ViStart`. A hook
  would also need to pass the town center's template name, position and rotation, since 1.20's camel stands
  beside a jigsaw inside it.

## 7. Ownership table entries

Not a code request: this mod declares, from pre-init, the blocks it ships first so later mods find them:
`BlockOwnership.declare("trailsandtalesbackport", ...)` for the cherry and bamboo sets, hanging signs, chiseled
bookshelf, the brush, suspicious sand and gravel, the twenty pottery sherds, decorated pot, sniffer egg, torchflower, pitcher plant, pink petals,
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

## 10. Presentation: what 1.20's look needs from Core (stream Lf)

Stream Lf (`docs/streams/lookfeel.md`) hands Core 1.20, 1.20.2, 1.20.3 and 1.20.5 through `Presentation.version`.
These pieces are single slots, so they belong to Core; each has a small local shim until Core has it.

1. **Title logo layout revision (1.20).** 1.20's `LogoRenderer` draws `minecraft.png` as one 256 by 44 piece of a
   256 by 64 texture at width / 2 - 128, then `edition.png` (128 by 14 of 128 by 16) at width / 2 - 64, 37 below
   the logo's top, before the splash. The splash moved to width / 2 + 123, 69. Asked for: a `VanillaRevisions`
   title revision (`LOGO_1_20`) that draws both textures in that layout, the Minceraft roll using
   `minceraft.png` (1.20 gave it its own texture), and the splash at the revision's position.
   *Shim:* `tools/lookfeel/generate.py` lays 1.20.6's logo out the way 1.12.2 draws `minecraft.png` (two 155 by 44
   pieces); `LookAndFeelClient` draws "Java Edition", takes the splash out of `GuiMainMenu` for the frame and draws
   it at 1.20's place, and binds `minceraft.png` for a rolled Minceraft. All of it only while
   `Presentation.asset("textures/gui/title/minecraft.png")` is this mod's.
2. **Debug keys with no help line.** 1.20.2's F3 + 1 and F3 + 2 are not in vanilla's F3 + Q list. Asked for:
   `debugKey(keyCode, null, action)` registers a key with no F3 + Q line.
   *Shim:* the two keys print this mod's own lines ("F3 + 1 = Show profiler chart").
3. **Menu music removals by path.** `removeMenuMusic` matches the exact sound name, so 1.20's removal of 1.19's
   Aerie, Firebugs and Labyrinthine has to guess Wild Update's namespace. Asked for: a removal that matches any
   namespace by the vanilla path (`removeMenuMusicPath("music/game/swamp/aerie")`).
   *Shim:* removes both `minecraft:` and `wildupdatebackport:` names.
4. **Loading-grid colors per version.** 23w16a recolored the grid (carvers take liquid carvers' blue; light is
   0xFFE0A0; Initialize Light is 0xCCCCCC). Asked for: `LoadingStatus.color(GameVersion)`, used by Core's 1.14 and
   1.16 drawers too, so 1.20 to 1.20.4 (before the 1.20.5 drawer) show 1.20's colors.
   *Shim:* the 1.20.5 drawer (`LoadingScreen1205`) uses 1.20's colors itself.
5. **1.20.5's menu look (24w09a).** Menus over a blurred, darkened panorama outside a world and over the blurred
   world inside one, header and footer separators, a lighter pause background, all driven by Menu Background
   Blur. That is every screen, so Core's. Asked for: a menu-background owner that, from 1.20.5, draws
   `Screen.renderBackground` as 1.20.6 does, reading the blur from the option this mod registers
   (`trailsandtalesbackport:menu_background_blurriness`, or Core may take the option over).
   *Shim:* only the world-loading screen has it (Core's `PANORAMA_1_20_5` drawer, registered by this mod).
6. **Panorama motion (1.20.5).** 1.20.6's `PanoramaRenderer` holds the pitch at 10 degrees and only spins (1.13's
   bob between 20 and 30 degrees is gone). Asked for: the motion as a revision of `PanoramaRenderer`.
7. **Application icon (1.20).** The window icon changed from a crafting table to a grass block. Asked for: an icon
   slot (`Contribution.windowIcon(16px, 32px)`), newest wins.
   *Shim:* `LookAndFeelClient.setIcon` sets 1.20's on pre-init.
8. **Narrator Hotkey (1.20.2, Accessibility, default ON).** Turning it off stops Ctrl + B toggling the narrator;
   1.12.2 handles Ctrl + B in `Minecraft.dispatchKeypresses` with no event to cancel. Needs a patch, and this mod
   has no coremod, so it is Core's (it already patches `processKeyF3`).
   *Built* on Core's `core-tt` branch: `Presentation.NARRATOR_HOTKEY` (shown from 1.20.2, after Hide Splash Texts)
   and patch P32 on `Minecraft.dispatchKeypresses`. Nothing is needed from this mod (it registers 1.20.2).
9. **F3 + 3, the network charts (1.20.2).** 1.20.2's `DebugScreenOverlay` draws a bandwidth chart (bytes received a
   tick, `BandwidthDebugChart`) and a ping chart (`PingDebugChart`, fed by `ServerboundPingRequestPacket` /
   `ClientboundPongResponsePacket` every second while it shows) where F3 + 2's charts go. The debug screen and its
   keys are Core's. Asked for: the two charts, the byte count from a Netty handler on the client's
   `NetworkManager` channel and the ping from a Core packet (absent server side: the chart says so), behind a
   `debugKey(Keyboard.KEY_3, null, ...)` (10.2's no-help-line form) this mod registers for 1.20.2.
   *Meanwhile:* F3 + 3 does nothing.

## 11. Biomes: `/locate biome` and Adventuring Time (stream G)

1. **`/locate biome <id>`.** Today's `/locate` has a `biome` form: "The nearest minecraft:cherry_grove is at
   [x, y, z] (n blocks away)", with 1.18's spiral search (`BiomeSource.findClosestBiome3d`: radius 6400, horizontal
   step 32, the first match on each ring, the reply's coordinates green and clickable as the structure form's). Core's
   `LocateCommand` only knows structures. Asked for: the `biome` form over `ForgeRegistries.BIOMES` (ids in any
   namespace, `#tag` through Forge's biome dictionary or a small tag table backports fill), searching the world's
   own `BiomeProvider` so it works in 1.12.2's layers and in Caves &amp; Cliffs' 1.18 terrain alike.
   *Built* on Core branch `core-locate-biome` (51620e0, not merged): `locate.BiomeLocate`. It finds
   `trailsandtalesbackport:cherry_grove` as `minecraft:cherry_grove` with no call from here.
2. **Adventuring Time.** 1.12.2's `adventure/adventuring_time` lists its 1.12.2 biomes one criterion each; 1.20.6's
   lists every overworld biome, the cherry grove among them. A mod replacing the vanilla file would clash with every
   other backport that adds a biome (Wild Update's mangrove swamp, Tricky Trials' pale garden). Asked for: Core builds
   the advancement's criteria from a list the backports fill (`Advancements.adventuringTime(biome)`, in the modern
   order), re-applied after `/reload`. *Meanwhile:* the grove is not in Adventuring Time.
