# Mechanics: 1.20's changes to existing behavior (branch `tt-mechanics`)

From the family's mechanics audit (2026-11-16, owner T&T). Each change has its own setting on the **Mechanics** page
of this mod in Backport Core's settings screen (`ModConfig.mechanics`), on by default; off, the game behaves as
1.12.2. Code in `mechanics/`, patches P8 to P12 in `docs/PATCHES.md`.

## Changelog

- **Mob melee reach (1.20.2, 23w31a; P8, `meleeReach`).** A melee mob hits when its box, widened by
  `sqrt(2.04) - 0.6` (0.83) to each side and not at all up or down, touches the target's hitbox, and it can see the
  target (`Mob.isWithinMeleeAttackRange`, `getAttackBoundingBox`, `LivingEntity.getHitbox`,
  `MeleeAttackGoal.canPerformAttack`, read from 1.20.2's decompiled client). So: wide mobs no longer reach through
  walls (an iron golem's reach drops from 2.9 blocks to 1.83), nothing hits through glass panes or fences it cannot
  see through (MC-1297, MC-2310), a rider is reached only from its seat up (a baby zombie cannot reach a horse's
  rider), and a tall mob reaches up to its own height (an enderman reaches a player
  standing 2 blocks up; the safe height goes from 1.5 to 3, as minecraft.wiki's 1.20.2 page says; no enderman-only
  code, it follows from the box). A riding attacker's box is joined with its mount's. Covers vanilla's
  `EntityAIAttackMelee` and its subclasses (zombie, polar bear, spider, rabbit) and other mods' AIs that extend
  `EntityAIAttackMelee` or `EntityAIZombieAttack`, whether or not they override `getAttackReachSqr` or
  `checkAndPerformAttack`.
- **Shields at any pitch (1.20.3, 23w40a; P9, `shieldPitch`).** Blocking compares the attack's direction with the
  way the player faces, not with the pitched look vector, so a shield blocks while looking straight up or down.
- **Spawn chunks (1.20.5, 24w03a; P10, `spawnChunks`).** The area kept loaded around the world spawn is the spawn's
  chunk and 2 chunks around it (5 by 5; 1.12.2: 17 by 17), and the server prepares that same area when it starts
  (1.12.2: 25 by 25). `SpawnChunks.radius` is the one value: Tricky Trials Backport sets it to 0 for 1.21.9 (none
  kept, none prepared).
- **Re-dyeing (1.20, 23w12a; `redyeing`, restart).** `dye_<color>_wool` and `dye_<color>_bed` (one dye with a wool
  block or bed of any other color) and `dye_<color>_carpet` (one dye surrounded by eight carpets of other colors,
  mixed). The dye is the color's ore name, as 1.12.2's other dye recipes. 1.12.2's white-only recipes stay (same
  result).
- **Jump Boost on slimes and magma cubes (1.20; P11, `slimeJumpBoost`).** Their jump adds Jump Boost's
  `0.1 * (level)`, as 1.20's `Slime.jumpFromGround` uses `getJumpPower()`.
- **Chorus fruit resets fall distance (1.20; `chorusFallDistance`).** After eating teleports the eater (1.20's
  `ChorusFruitItem.finishUsingItem` calls `resetFallDistance()`).
- **Arrows keep custom names (1.20.3, 23w43a; P12, `arrowNames`).** An arrow shot from a bow with a named arrow item
  takes the name, keeps the item (`trailsandtalesbackport:item` in the arrow's Forge data, as 1.20.3's
  `pickupItemStack`) and gives it back when picked up.


### The low-impact 1.20.x items (second round)

- **Enchanting through replaceable blocks (1.20, 23w12a, MC-2474; P13, `enchantingGaps`).** A bookshelf at one of
  the 32 places two blocks out counts when the one block halfway to it, at its own height, is air or a replaceable
  block (grass, ferns, dead bushes, vines, fire, snow layers, water, lava: 1.20's `#replaceable`, read as 1.12.2's
  replaceable materials). Forge's per-shelf power is kept. Particles follow the same test (client).
- **Block sounds (1.20.2, 23w31a; 1.20.5, 24w10a; P15, `blockSounds`).** Jukeboxes and tripwire hooks sound like
  wood, levers, repeaters and comparators like stone; sponges and wet sponges (per state) and cobwebs get 1.20's
  own sounds; a sponge soaking up water plays `block.sponge.absorb`.
- **The 1.20.3 bat (`batModel`, client).** `BatModel`'s parts and 32x32 texture, posed by `BatAnimation`'s flying
  and resting keyframes (`tools/mechanics/bat_animations.py`); off, 1.12.2's renderer.
- **Hoppers under full blocks (1.20.5, 24w04a; P14, `hopperPickup`).** A hopper block whose block above has a full
  collision box (not a beehive or bee nest) picks up no items; hopper minecarts are unchanged. In 1.12.2 this only
  shows for items inside that block, as 1.12.2's pickup box already stops at the top of the block above.
- **Ender pearl landing sound (1.20.3, 23w43a; `pearlSound`).** `entity.player.teleport` where a player's pearl lands.
- **Names of looked-at entities (1.20.3; P20, `entityNames`, client).** A custom name of an entity that is not a mob
  shows while it is under the crosshair.
- **Game rules (`gameRules`).** `enderPearlsVanishOnDeath` (1.20.2, P17), `projectilesCanBreakBlocks` (1.20.3,
  decorated pots), `playersNetherPortalDefaultDelay` / `playersNetherPortalCreativeDelay` (1.20.3, P18),
  `spawnChunkRadius` (1.20.5, read by `SpawnChunks.effectiveRadius`; Tricky Trials' radius 0 still wins), and
  `randomTickSpeed` for snow, ice and cauldrons in the rain (1.20.2, P19).
- **Barrier landings (1.20.2; P16, `barrierParticles`).** No dust when landing on a barrier.
- **Wither's color (1.20; `witherColor`).** 7561558 instead of 3484199.

Checked and left alone: re-dyeing (done in the first round); camels and sniffers panic while leashed (1.20.2) already
(their panic behaviors ignore the lead); spiders fear armadillos (1.20.5) already in the armadillo stream; shulker
bullets and llama spit never broke armor stands in 1.12.2 (their damage is not "player" or "arrow"); special arrows
already pick up as themselves. Not done: `maxCommandForkCount` (1.12.2 has no command forks); 1.20.5's mob item
pickup change (a side effect of item components); 1.20.5's bees and parrots overshooting and the shulker bullet's
melee hitbox (no 1.12.2 code to match found); end portal arrival tickets (1.12.2 has no chunk tickets).

## How it works

- P8 sets `checkAndPerformAttack`'s distance argument on its first line to a value that answers 1.20.2's question
  against the method's own reach (`MeleeReach.distance`), so the method's comparisons follow 1.20.2 without the
  transformer knowing what they are. Only classes whose direct superclass is one of the two are found (the
  transformer reads each class's superclass name; it loads nothing).
- The chorus fruit change records the eater's position in `LivingEntityUseItemEvent.Tick`, which runs just before the
  fruit is eaten in the same tick, and compares it in `Finish`.
- Named arrows: a bow spawns its arrow from `onPlayerStoppedUsing` while the bow is still the active item and before
  the ammunition shrinks, so `EntityJoinWorldEvent` finds the ammunition as `ItemBow.findAmmo` does.

## Tested

`-Pselftest=mechanics` (`MechanicsSelfTest`), each with the setting on and off where 1.12.2 differs:

- P8 through the patched `checkAndPerformAttack` of real AIs: a zombie one block away hits; a zombie behind a glass
  pane post 0.8 blocks away hits only when off; an iron golem 2.5 blocks away hits only when off, 1.7 blocks away
  always; an enderman hits a villager standing 2 blocks up only when on, 3 blocks up never; a horse's rider's
  hitbox starts at the seat, a baby zombie cannot reach it and an adult can; a zombie reaches exactly its height up;
  P8 reaches the polar bear's own method.
- P9: with the shield raised, pitch -90, 0 and 90 block a zombie in front and not one behind.
- P10: radius 2 keeps chunks 2 away, not 3; the overworld drops chunks 5 away; the start area is 25 chunks; radius 0
  keeps and prepares none; off, 1.12.2's 128-block area and 192-block start area.
- P11: slime 0.42, with Jump Boost II 0.62; magma cube with Jump Boost I; off, no change.
- Chorus fruit teleports the test player; fall distance 0 after (off: kept).
- A bow shot from named arrows makes a named arrow; picking it up gives back the named item.
- Re-dyeing: blue wool to red, lime bed to red, mixed carpets to black, white wool still dyes, red on red is no
  recipe.

- `-Pselftest=mechanicslow` (`MechanicsLowSelfTest`): a real `ContainerEnchantment` offers level 30 through grass,
  snow and vines (off: at most 8), stone halfway blocks one shelf only; `pullItems` skips an item inside a stone block
  above (off: takes it), not under air or a slab, minecart-aligned positions unchanged; every swapped sound type and
  the sponge's states, and the absorb sound heard by a world listener; barrier landings; each game rule (pearls of a
  dead player, portal delays, the snow/ice chance at 0, 3 and 300, `spawnChunkRadius` and Tricky Trials' 0); the
  pearl sound (and none for a teleport without a pearl); Wither's color. Each with its setting off.

## Not done, not tested

- `EntityAIAttackMelee.shouldExecute`'s case with no path (1.20.2 starts the goal when already in melee range) keeps
  1.12.2's distance check. A class extending another mod's melee AI (two levels below `EntityAIAttackMelee`) that
  overrides `checkAndPerformAttack` without calling `super` is not patched. The polar bear's warning range stays
  1.12.2's (twice its reach); 1.20.2 measures it from the bear's center.
- Arrows shot by dispensers do not take a name (1.20.3's dispensers do); only bows that find ammunition as vanilla's.
- Not tested in a client: shields and named arrows were only checked on the server.
- Not tested in a client: the bat, P13's particles, P20, and hearing the new sounds; P19 allows at most one snow or
  ice try per chunk per tick, so very high `randomTickSpeed` values are slower than 1.20.2's; raising
  `spawnChunkRadius` keeps new chunks loaded only once something else loads them (1.12.2 has no chunk tickets).
