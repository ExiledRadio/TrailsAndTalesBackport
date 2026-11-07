# Stream B, part 2: the bamboo plant copy

Package `bamboo.plant`. Config `bamboo` (`enabled`, new `jungleBamboo`). Decided by the user 2026-11-07
(PLAN.md *Decided* 5).

## Changelog section

*   Without Village & Pillage, this mod adds bamboo itself: plant it on grass or dirt, grow it with bone meal,
    find it scattered through jungles. Nine bamboo make a block of bamboo.

## How it works

*   Family Rule 1: bamboo is 1.14's, so Village & Pillage owns it. When it is absent (and no other installed
    mod provides `bamboo` in Backport Core's table), this mod registers the identical stalk (`bamboo`), shoot
    (`bamboo_sapling`) and item, under its own namespace, so the owner can rename them in place later.
*   The block classes and assets are the owner's, copied by `tools/bamboo/copy_from_owner.py` (rerun it after the
    owner changes them); `BambooPlant` holds what the owner's `BambooContent` held. Sounds and names come from
    1.20.6's files, as the owner's came from Mojang's.
*   Only what the block of bamboo needs: no pandas, scaffolding or bamboo jungle biome (the owner's own
    features). The jungle scattering is the owner's 1.14 rule, off while Future MC adds its own.

## Tested headlessly

`./gradlew runServer -Pselftest=all` (port 25889):

*   **Alone**: the copy registered under our namespace, used by the block of bamboo recipe (9 make one); every
    state's metadata round trip; a real player planting a shoot on grass; bone meal turning it into a stalk that
    grows; breaking the bottom dropping one bamboo, the stalk above coming down on its scheduled tick.
*   **With Village & Pillage 2.4.0** (its dev jar in `eclipse/mods` for that run): no copy registered, V&P's
    bamboo making the block of bamboo; every other stream's test passing beside it (hanging signs from V&P's
    stripped logs, V&P narrowing the vanilla recipes instead of us).

## Not tested

*   Drawing (the owner's models, copied as they are), jungle generation in real chunks.
