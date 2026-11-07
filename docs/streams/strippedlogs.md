# Stripped log copies (for stream H)

Package `woodcopy`. No config of its own (on with `hangingSigns`). Follows the user's 2026-11-07 rule: every
mod works fully on its own, with copies instead of crafting-grid stand-ins.

## Changelog section

*   Without Village & Pillage (or Update Aquatic), this mod adds the six woods' stripped logs and stripped wood
    itself: strip vanilla logs with any axe. Hanging signs are made from them, as in 1.20.

## How it works

*   Village & Pillage owns stripped logs (1.13 blocks it shipped first); Update Aquatic copies them when V&P
    is absent. This mod registers its own copies only when neither is installed, from the owner's classes
    and assets, copied by `tools/woodcopy/copy_from_owner.py` (an adaptation of Update Aquatic's tool), same
    names and states, so the owner can rename them in place.
*   Registration, recipes (planks, stripped wood, charcoal) and axe stripping are ours: the owner's rule, through
    a new state rule hook in `common/wood/AxeStripping` (vanilla logs keep their wood in metadata).
*   `HangingWood.log()`: V&P's stripped log, else Update Aquatic's, else ours. The plain-log stand-in is gone.
*   `AxeStripping` now also respects spectators, `canPlayerEdit` and a denied item use, as V&P's does.

## Tested headlessly

Port 25890. **Alone: `pass=408 fail=0`**, 70 blocks, 1105 states, 0 asset errors. **With Village & Pillage 2.4.0:
`pass=368 fail=0`** (no copies; hanging signs from V&P's stripped logs).

*   Twelve copies, owner's names, state round trips; planks and stripped wood recipes; charcoal; a real player
    stripping a dark oak log (axis kept) and an all-bark jungle log (into stripped wood); hanging signs made
    from our copy.

## Not tested

*   With Update Aquatic installed (its copies should be used: same check as V&P's, by name).
*   Drawing (the owner's models, copied as they are).
