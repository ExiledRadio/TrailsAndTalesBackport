# Stream Sg: 1.20's sign changes

Package `sign` (client parts in `sign.client`). Config `signs` (`enabled`). No entities; packet ids 30 and 31 of
the block 30-34. Dev-server port 25931.

## Changelog section

*   **Edit signs after placing them**: right-click a sign to change its text (not in Adventure mode). Every
    sign gets this: oak, our cherry and bamboo signs, hanging signs, Village & Pillage's five woods' signs and
    other mods' signs built on vanilla's.
*   **Text on both sides**: right-click a sign from behind to write on its back. Both sides show in the world.
*   **Dye and glow ink per side**: dye colors the side you click; glow ink (with Caves & Cliffs installed)
    makes that side's text glow, and an ink sac stops the glow. Only a side with text takes them.
*   **Waxing**: honeycomb (with Buzzy Bees installed) waxes a sign, so nobody can edit, dye or glow it any
    more. Clicking a waxed sign makes it wobble (its sound); click commands on it still run.
*   **The new edit screen**: 1.20's "Edit Sign Message" screen with the sign drawn large, the text in its
    color, a cursor you can move, selection, copy and paste; hanging signs have their own screen, as in 1.20.

## How it works

*   **Source**: 1.20's `SignBlock.use`, `SignBlockEntity` (`isFacingFrontText`, `updateSignText`,
    `playerWhoMayEdit`, `executeClickCommandsIfPresent`), `SignText`, the `SignApplicator` items (`DyeItem`,
    `GlowInkSacItem`, `InkSacItem`, `HoneycombItem`), `SignRenderer`, `HangingSignRenderer`,
    `AbstractSignEditScreen`, `SignEditScreen`, `HangingSignEditScreen`, next to 1.12.2's `TileEntitySign`,
    `BlockSign`, `GuiEditSign`, `TileEntitySignRenderer`, `NetHandlerPlayServer.processUpdateSign`.
*   **No patches.** Nothing of vanilla's needed changing, so there is no ASM (and so nothing for the frame
    verifier): the data is a capability, the click a Forge event, the screen swapped through `GuiOpenEvent`,
    the renderers re-bound.
*   **Data** (`SignData`): a capability on every `TileEntitySign` (vanilla's, ours, hanging signs,
    Village & Pillage's, any mod's that extends it). Forge saves it under `ForgeCaps`, and a sign's update
    packet is its whole save, so it reaches clients with no packet of ours. The tag is 1.20's shape:
    `trailsandtalesbackport:sign {front_text:{has_glowing_text}, back_text:{messages[4], color,
    has_glowing_text}, is_waxed}`. The front's lines stay vanilla's `Text1-4`. **Front color** is
    Village & Pillage's `villagerbackport:Color` in the tile's Forge data (its 1.14 sign dyeing), so a sign it
    dyed keeps its color and both mods' renderers agree. Colors are saved by 1.14 name (`light_gray`). The
    editor (`playerWhoMayEdit`) is not saved, as in 1.20. `SignSides` reads and changes both faces.
*   **Which face**: 1.20's rule - the player is within 90 degrees of the front's facing, measured from the
    outline's center for wall signs and wall hanging signs, the block's center otherwise.
*   **Right-click** (`SignInteraction`, at high priority, cancelled on both sides so Village & Pillage's
    front-only dyeing and vanilla's `BlockSign` - which would run the front's commands again - never run):
    the face's click commands; waxed: the fail sound, nothing else; an applicator (if the player may build and
    nobody else is editing): dye, glow ink, ink sac (each only on a face with text and only if it changes
    something, with its 1.17 sound), honeycomb (any sign); else, unless there were commands, the editor for
    that face, if the player may build, nobody else is editing and every line is plain text (1.20:
    literal contents). Sneaking with an item in either hand skips it all, as for any block. Applicators are
    used up outside creative and count as item use.
*   **Dye items**: 1.12.2's sixteen dye metadatas and any ore-dictionary dye. Without 1.14's plain dyes
    (Village & Pillage's `black_dye`), the ink sac is also black dye: it stops the glow on a glowing face,
    else dyes black. With them, the ink sac, cocoa beans, lapis and bone meal are only what 1.20's are: the
    ink sac stops the glow, the others open the editor like any item.
*   **Editing** (`SignEditing`): opening marks the player as the one who may edit, sends them the sign, then
    packet 31. Packet 30 brings four lines (at most 384 characters each) for one face, taken only from that
    player, within 8 blocks, while the sign is not waxed; formatting codes are stripped and each line keeps its
    old style; editing then ends. Someone else editing blocks others only while they are still in the world
    and within 8 blocks (1.20's tick clears a gone editor; here the check does). A sign placed from an item
    opens vanilla's editor (vanilla marks the placer); our screen sends a front update, which that mark
    allows once.
*   **Screen** (`SignEditScreen`): 1.20's layout (title at 40, the sign at 62.5x around y 90, Done at
    `height / 4 + 144`; hanging: the `gui/hanging_signs/<wood>` picture at 4.5x around y 125), text in the
    face's color (full when glowing), "_" or a bar for the cursor, the selection; up, down and Enter change
    line; left, right, Home, End (Shift selects), Backspace, Delete, Ctrl+A/C/X/V. Changes show on the sign in
    the world as you type; closing sends them. It closes by itself if the sign goes or you move away.
    Vanilla's `GuiEditSign` is swapped for it whenever anything opens one.
*   **Renderers** (bound in client post-init, after every mod's own): `StandardSignRenderer` for vanilla's
    tile, our wood sign tile and Village & Pillage's `TileEntityWoodSign` (by class name), with each one's
    texture (Village & Pillage's from `villagerbackport:textures/entity/signs/<wood>.png`, the wood read from
    the block name); `HangingSignSidesRenderer`, stream H's renderer with both faces. Text as 1.20's
    `SignRenderer`: 40% of the color, glowing at full color and full brightness with an outline in the dark
    color (cream for black) within 16 blocks of the camera (always for black). The back is turned half a turn
    about the board's middle.
*   **Hanging signs** (`hangingsign`, touched as stream H left it for us): its renderer's text moved into an
    overridable `renderText`, which `HangingSignSidesRenderer` replaces. Hanging signs are recognized by name
    (`*_hanging_sign`) elsewhere, so the sign stream does not link against stream H's blocks.
*   **Sounds** (ours, files from 1.20.6): `block.sign.waxed_interact_fail`, `item.dye.use`, `item.ink_sac.use`,
    `item.glow_ink_sac.use`, `item.honeycomb.wax_on`, with their subtitles (`tools/sign/generate.py`).

## Tested headlessly

`./gradlew runServer -Pselftest=all` (port 25931), all streams, on a fresh test world: **`pass=3781 fail=0`**;
`-Pselftest=sign` alone: **`pass=97 fail=0`**; `check_assets.py`: **80 blocks, 1145 states, 91 item models, 745
sound files, 0 errors**. Release jar (`build --rerun-tasks`): no `/selftest/` entries, reobfuscated (`func_`
names).

*   The data on vanilla's, our wood and hanging sign tiles (not on other tiles); its NBT in 1.20's shape
    (back color by 1.14 name, `is_waxed`, four messages; front color in Village & Pillage's key); a full round
    trip of both faces' text, colors, glow and wax; the same through the update tag clients read; an old sign
    loading black, unwaxed and empty behind.
*   Both packets through their bytes; lines capped at 384 characters.
*   Which face: standing signs at rotation 0 and 4, a wall sign from the front and from behind its board, a
    ceiling hanging sign.
*   Through the server's real right-click path: opening the editor for the clicker; front text taken (codes
    stripped) once; back text from behind leaving the front; text refused from another player, from 9+
    blocks, and without an open editor; a nearby editor holding the sign and a gone one freeing it; nothing in
    Adventure mode; translated text never opened; a placed sign's first text taken once; sneaking with dye
    doing nothing.
*   Dye per face, used once, free in creative, nothing on a face without text (the editor opens instead), a
    same-color dye opening the editor; glow ink (a stand-in for Caves & Cliffs') per face and once; the ink
    sac stopping the glow, then (without 1.14's dyes) dyeing black.
*   Waxing (a stand-in for Buzzy Bees' honeycomb): used once, blank signs too; a waxed sign refusing the
    editor, text and dye; no waxing without honeycomb; honeycomb found iff Buzzy Bees is installed.
*   Click commands: the front's from the front, the back's from behind, instead of the editor, and still on
    a waxed sign.
*   A hanging sign's back edited and saved.

The test world is reused between runs: `bambooplant`'s "broken piece drops one bamboo" counts an item left
from the previous run's world and fails on every run after the first (not this stream's; seen here on runs
2 and 3, gone on a fresh world).

## Not tested

*   Anything drawn: both renderers, the glow and its outline, the edit screens, the GUI swap.
*   With Buzzy Bees, Caves & Cliffs or Village & Pillage installed: honeycomb and glow ink were stand-ins
    (the lookup by name is checked only for being absent); Village & Pillage's sign renderer being replaced.
*   The client's half of the packets (opening the screen from packet 31; sending packet 30 on close).

## Built after the first pass (branch `tt-leftovers`)

*   **Wax-on particles** (level event 3003): packet 32 (`SignNetwork.WaxOn`) goes with the wax sound to everyone
    within 64 blocks; `sign.client.WaxParticles` spawns 1.20.6's `ParticleUtils.spawnParticlesOnBlockFaces(...,
    WAX_ON, UniformInt.of(3, 5))`: three to five on each face, as `GlowParticle.WaxOnProvider` makes them (color
    0.91, 0.55, 0.08; 1.20.6's `glow` sprite; 10 to 39 ticks; brightening to full block light).
*   **Other mods' signs with their own renderer**: on the first client tick, every `TileEntitySign` subclass bound to
    a renderer that is not this mod's gets `sign.client.OtherSignRenderer` around it: theirs draws the board and
    the front, then the back text and, on a glowing front, the glowing front text are drawn in vanilla's sign
    layout. A sign whose renderer lays its board out differently gets the text in vanilla's place.
*   **Selectors and scores on the back** resolve as the sign loads, as the front's do (same sender: the sign,
    permission level 2; server worlds only, as 1.20.6's `loadLines`).
*   **Request to Village & Pillage** (Decided 6) stands: nothing is needed; if it binds its own renderer again,
    `OtherSignRenderer` draws the back and glow over it.
