# 1.20.5's effect particles

Package `effectparticles` (`EffectParticles`, `EffectParticlesMessage`, `client/EffectParticlesClient`), patch P20
(`asm/EffectParticlesTransformer`, `docs/PATCHES.md`). Option `misc.effectParticles` (on; client only, no restart),
shown while `Presentation.isAtLeast("1.20.5")`. Packet 90 (block 90-94).

## Changelog section

*   1.20.5: effect particles are no longer one blended color. Each visible effect gives particles of its own color
    (an ambient one, such as a beacon's, fainter), and fewer of them.

## 1.20.6's code (remapped 1.20.6 client, Vineflower)

*   `LivingEntity.updateSynchronizedMobEffectParticles`: `DATA_EFFECT_PARTICLES` is the list of
    `MobEffectInstance.getParticleOptions()` of every effect with `isVisible()`; `DATA_EFFECT_AMBIENCE_ID` is
    `areAllEffectsAmbient` (true unless a visible effect is not ambient). Set from `updateInvisibilityStatus` when the
    effects change (`effectsDirty`); an empty effect map empties the list.
*   `MobEffect.createParticleOptions`: `ColorParticleOption.create(ENTITY_EFFECT, ARGB32.color(alpha, color))`,
    alpha 255, or `AMBIENT_ALPHA = Mth.floor(38.25F)` = 38 for an ambient effect.
*   `LivingEntity.tickEffects`, both sides (the server's `addParticle` does nothing): with a non-empty list,
    `random.nextInt((isInvisible() ? 15 : 4) * (ambient ? 5 : 1)) == 0` draws `Util.getRandom(list, random)` at
    `getRandomX(0.5), getRandomY(), getRandomZ(0.5)` with speed (1, 1, 1).
*   `SpellParticle.MobEffectProvider`: a `SpellParticle` with the option's red, green, blue and alpha.

1.12.2: `EntityLivingBase.updatePotionMetadata` syncs one blended color (`POTION_EFFECTS`, through Forge's
`PotionColorCalculationEvent`) and `HIDE_PARTICLES` (which is the ambient flag); `updatePotionEffects` draws a
`mobSpell`/`mobSpellAmbient` one tick in 2 (15 invisible, another 1 in 5 ambient), passing the color as the speed.

## How it works

*   **Server.** Forge's `PotionColorCalculationEvent` (every recompute of the blended color: an effect added, changed,
    removed), `PotionRemoveEvent` and `PotionExpiryEvent` (the last effect going leaves no color to compute) mark the
    entity; at the end of the world tick its list is computed as 1.20.6's (`EffectParticles.compute`: one ARGB color per
    visible effect, sorted, and the all-ambient flag) and sent with `sendToTrackingAndSelf` when it differs from what
    was sent last. Once a second the entities with particles are looked at again, for a mod that takes an effect away
    without an event. A player who starts tracking an entity is sent its list; a player is sent their own on log-in,
    respawn and dimension change. No list is sent for an entity without particles.
*   **Client.** Lists by entity id; dropped when a new entity joins under that id, when the world unloads, and every
    five seconds for entities no longer in the world.
*   **P20.** In `updatePotionEffects`, `int i = dataManager.get(POTION_EFFECTS)` becomes
    `EffectParticles.effectColor(dataManager.get(POTION_EFFECTS), this)`. For a client entity with a list, while active,
    the hook rolls 1.20.6's roll on the entity's random, draws the particle and answers 0, so 1.12.2 draws nothing.
    Otherwise it answers the color unchanged: the server, the option off, Presentation below 1.20.5, or no list (a
    server without this mod) all keep 1.12.2's swirl.
*   **The particle.** `mobSpell` made through the particle manager with speed (1, 1, 1), so it moves as 1.20.6's (the
    speed is not the color), then given the effect's color and alpha (38/255 ambient). 1.12.2's `RenderGlobal` rule for
    which particles are made (within 32 blocks, the particle setting) is applied first, as `addParticle` does.
*   **Beacons and conduits.** Their effects are ambient, so they come out at alpha 38 and, with nothing else active,
    five times less often, as in 1.20.6.

## Other family mods

No other family mod patches `EntityLivingBase.updatePotionEffects` (Backport Core's P-patches, Wild Update, Tricky
Trials, Nether Update, Update Aquatic, Caves & Cliffs and Buzzy Bees checked; Core's `WaterTravelTransformer` patches
`travel` in the same class, which this patch leaves alone). Tricky Trials draws its four 1.21 effects (wind charged,
weaving, oozing, infested) with their own particles on 1.21's same roll, and sets `POTION_EFFECTS` to the other
effects' blend. With both mods, its effects stay in this mod's list with alpha 0 (so the pick is among all visible
effects, as 1.21's): a pick that lands on one draws nothing here, and Tricky Trials draws its own on its roll. The
expected rate stays 1.21's one in 4.

## Differences from 1.20.6

*   1.20.6's `SpellParticle` hides itself within 3 blocks of a player looking through a spyglass in first person; 1.12.2
    has no spyglass, so this is not done.
*   1.20.6's spell particle has no collision (`hasPhysics = false`); 1.12.2's `ParticleSpell` keeps its collision with
    blocks. Its texture frames, size, life, gravity and friction are the same.
*   Area effect clouds, tipped arrows and potion splashes keep 1.12.2's particles (1.20.5 changed only their particle
    type, keeping one blended color).
*   The roll uses 1.12.2's `java.util.Random` (the entity's own), as 1.12.2's swirl does, not 1.20.6's `RandomSource`.

## Tested

*   Headless, `-Pselftest=effectparticles` (with `futuremc`, port 25887): part of **353 checks, 0 failed**. P20 in
    `Asm.APPLIED`; the list for speed + ambient regeneration + hidden night vision (2 colors, alpha 255 and 38, not all
    ambient); one message per change through 1.12.2's own `updatePotionEffects`, none while unchanged; all ambient once
    speed goes; the empty list sent and the entity forgotten; a new tracker sent the list; the server hook leaves the
    color alone; Tricky Trials' weaving at alpha 0; the packet's round trip; 1.20.6's rates over 60000 rolls each (1 in
    4, 20, 15, 75) with an even pick.
*   Client, `runClient -Pselftest=liveeffects` (`ClientEffectParticlesTest`; not "client", which runs the look-and-feel
    check): **12 checks, 0 failed**. A flat world, speed and ambient regeneration on the player, strength on a pig:
    the lists arrive, P20 applied on the client, 400 ticks of drawing at about one in 4 per entity (196 particles for
    the two), only the three effects' own colors, the option off brings 1.12.2's color back; a third-person frame in
    `eclipse/effectparticles/screenshots/particles.png`.
*   Frame verifier (`family-notes/tools/verify/ScanAll`) over the real 1.12.2 client bytes: only
    `EntityLivingBase` touched, vanilla and patched both verify.

## Not tested

*   A dedicated server with a separate client, and Tricky Trials installed alongside.
