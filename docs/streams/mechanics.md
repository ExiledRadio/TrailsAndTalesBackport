# Mechanics: 1.20's changes to existing behavior (branch `tt-mechanics`)

From the family's mechanics audit (2026-11-16, owner T&T). Each change has its own setting on the **Mechanics** page
of this mod in Backport Core's settings screen (`ModConfig.mechanics`), on by default; off, the game behaves as
1.12.2. Code in `mechanics/`, patches P7 to P11 in `docs/PATCHES.md`.

## Changelog

- **Mob melee reach (1.20.2, 23w31a; P7, `meleeReach`).** A melee mob hits when its box, widened by
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
- **Shields at any pitch (1.20.3, 23w40a; P8, `shieldPitch`).** Blocking compares the attack's direction with the
  way the player faces, not with the pitched look vector, so a shield blocks while looking straight up or down.
- **Spawn chunks (1.20.5, 24w03a; P9, `spawnChunks`).** The area kept loaded around the world spawn is the spawn's
  chunk and 2 chunks around it (5 by 5; 1.12.2: 17 by 17), and the server prepares that same area when it starts
  (1.12.2: 25 by 25). `SpawnChunks.radius` is the one value: Tricky Trials Backport sets it to 0 for 1.21.9 (none
  kept, none prepared).
- **Re-dyeing (1.20, 23w12a; `redyeing`, restart).** `dye_<color>_wool` and `dye_<color>_bed` (one dye with a wool
  block or bed of any other color) and `dye_<color>_carpet` (one dye surrounded by eight carpets of other colors,
  mixed). The dye is the color's ore name, as 1.12.2's other dye recipes. 1.12.2's white-only recipes stay (same
  result).
- **Jump Boost on slimes and magma cubes (1.20; P10, `slimeJumpBoost`).** Their jump adds Jump Boost's
  `0.1 * (level)`, as 1.20's `Slime.jumpFromGround` uses `getJumpPower()`.
- **Chorus fruit resets fall distance (1.20; `chorusFallDistance`).** After eating teleports the eater (1.20's
  `ChorusFruitItem.finishUsingItem` calls `resetFallDistance()`).
- **Arrows keep custom names (1.20.3, 23w43a; P11, `arrowNames`).** An arrow shot from a bow with a named arrow item
  takes the name, keeps the item (`trailsandtalesbackport:item` in the arrow's Forge data, as 1.20.3's
  `pickupItemStack`) and gives it back when picked up.

## How it works

- P7 sets `checkAndPerformAttack`'s distance argument on its first line to a value that answers 1.20.2's question
  against the method's own reach (`MeleeReach.distance`), so the method's comparisons follow 1.20.2 without the
  transformer knowing what they are. Only classes whose direct superclass is one of the two are found (the
  transformer reads each class's superclass name; it loads nothing).
- The chorus fruit change records the eater's position in `LivingEntityUseItemEvent.Tick`, which runs just before the
  fruit is eaten in the same tick, and compares it in `Finish`.
- Named arrows: a bow spawns its arrow from `onPlayerStoppedUsing` while the bow is still the active item and before
  the ammunition shrinks, so `EntityJoinWorldEvent` finds the ammunition as `ItemBow.findAmmo` does.

## Tested

`-Pselftest=mechanics` (`MechanicsSelfTest`), each with the setting on and off where 1.12.2 differs:

- P7 through the patched `checkAndPerformAttack` of real AIs: a zombie one block away hits; a zombie behind a glass
  pane post 0.8 blocks away hits only when off; an iron golem 2.5 blocks away hits only when off, 1.7 blocks away
  always; an enderman hits a villager standing 2 blocks up only when on, 3 blocks up never; a horse's rider's
  hitbox starts at the seat, a baby zombie cannot reach it and an adult can; a zombie reaches exactly its height up;
  P7 reaches the polar bear's own method.
- P8: with the shield raised, pitch -90, 0 and 90 block a zombie in front and not one behind.
- P9: radius 2 keeps chunks 2 away, not 3; the overworld drops chunks 5 away; the start area is 25 chunks; radius 0
  keeps and prepares none; off, 1.12.2's 128-block area and 192-block start area.
- P10: slime 0.42, with Jump Boost II 0.62; magma cube with Jump Boost I; off, no change.
- Chorus fruit teleports the test player; fall distance 0 after (off: kept).
- A bow shot from named arrows makes a named arrow; picking it up gives back the named item.
- Re-dyeing: blue wool to red, lime bed to red, mixed carpets to black, white wool still dyes, red on red is no
  recipe.

## Not done, not tested

- `EntityAIAttackMelee.shouldExecute`'s case with no path (1.20.2 starts the goal when already in melee range) keeps
  1.12.2's distance check. A class extending another mod's melee AI (two levels below `EntityAIAttackMelee`) that
  overrides `checkAndPerformAttack` without calling `super` is not patched. The polar bear's warning range stays
  1.12.2's (twice its reach); 1.20.2 measures it from the bear's center.
- Arrows shot by dispensers do not take a name (1.20.3's dispensers do); only bows that find ammunition as vanilla's.
- Not tested in a client: shields and named arrows were only checked on the server.
