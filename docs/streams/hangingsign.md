# Stream H: hanging signs and the chain copy

Package `hangingsign`. Config `hangingSigns` (`enabled`).

## Changelog section

*   **Hanging signs** for oak, spruce, birch, jungle, acacia, dark oak, cherry and bamboo, and for crimson,
    warped and mangrove when the mods that add those woods are installed. Two chains over six stripped logs
    make six. Hang one under a block, a fence, a wall, iron bars or a chain, or on the side of a wall; they
    never stand on the ground. Under a block with a full bottom they hang straight and turn to face you; sneak,
    or hang one under something thinner, and it hangs from a V of chains and turns to any angle.
*   **Chains** (nugget, ingot, nugget), when no other installed mod adds them.

## How it works

*   `HangingWood`: each wood, its 1.20 sound set and where its stripped log comes from (Village & Pillage's,
    Update Aquatic's copy, ours, Nether Update's, Wild Update's). A vanilla wood with no stripped log in the
    game takes its plain log; a wood without its mod gets no sign at all.
*   `BlockHangingSign.Ceiling` saves its rotation (0-15) in metadata; whether it hangs from a V of chains is in
    the tile (`TileHangingSign`, a `TileEntitySign`, so the editor and sign packets work as for any sign) and
    shown through the actual state. Support is 1.20's "holds the middle of its bottom face", read from the
    block's collision box. `Wall` attaches to a full face on its left or right, or to another wall hanging
    sign on the same line.
*   `ItemHangingSign`: 1.20's placement order (wall first on a side face, then ceiling), 4 or 16 directions,
    burns 800, stacks to 16, opens the editor.
*   `HangingSignRenderer`: 1.20's model rebuilt in 1.12.2 model parts, 1.20's texture, text placement and
    scale.
*   The chain copy registers only if Backport Core's table has no other provider (Nether Update declares it),
    with 1.16's states (axis), upright by default.
*   Also fixed here for every pillar block: `BlockRotatedPillar` defaults to the x axis; our logs and chains
    now default to upright.

## Tested headlessly

`./gradlew runServer -Pselftest=all` (port 25885), all streams: **`pass=336 fail=0`**; `check_assets.py`:
**56 blocks, 1044 states, 42 item models, 368 sound files, 0 errors**.

*   Which woods get signs (none for crimson, warped, mangrove in this run), the chain copy present; state round
    trips; the chain recipe; oak and dark oak signs from plain logs, cherry from stripped cherry logs; fuel
    800, stacks of 16.
*   Placing by a real player: under stone (straight chains, facing the player, the editor opened for them,
    one sign used); text set through the server's sign packet; sneaking (V of chains, 16 directions); under an
    oak fence, a glass pane and a chain (V of chains); refused on the ground; dropping when its block goes.
*   A wall hanging sign facing the player along the wall, held by the wall, dropping when the wall goes.
*   Saving and loading the chains; the actual state showing them.

## Not tested

*   Anything drawn: the renderer, its text, the destroy-stage overlay, the item icons.
*   With Nether Update or Wild Update installed (their crimson, warped, mangrove and chain names are guessed as
    1.16's and 1.19's own: `stripped_crimson_stem`, `stripped_warped_stem`, `stripped_mangrove_log`, `chain`).
*   With Village & Pillage or Update Aquatic installed (their stripped logs in the recipe).

## Left for other streams

*   Text on the back face, dyeing and glow ink, waxing: stream Sg (1.20's hanging sign edit screen too).
*   Waterlogging (every hanging sign and the chain): done (`waterlog.md`).
