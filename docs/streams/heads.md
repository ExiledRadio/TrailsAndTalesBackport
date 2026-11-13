# Stream Hd: heads

Package `heads` (client parts in `heads.client`). Config `heads` (`enabled`). No entities; packet ids 80-84
are unused (the stream sends no packets of its own). Dev-server port 25923.

## Changelog section

*   **Piglin head**: dropped by a piglin killed by a charged creeper (with Nether Update installed, since the
    piglin is its mob). Placed standing or on a wall, and worn like any other head. Its ears flap while it is
    powered by redstone, and while a mob wearing it walks.
*   **Mob heads on note blocks**: a head on top of a note block makes it play that mob's sound when it is
    played (clicked, hit or powered): skeleton, wither skeleton, zombie, creeper, ender dragon and piglin.
    Player heads play nothing. Heads go onto a note block's top without sneaking.
*   **Dispensers** put a piglin head on the mob or armor stand in front of them.

## How it works

*   **Source**: 1.20.6's `SkullBlock`, `WallSkullBlock`, `PiglinWallSkullBlock`, `AbstractSkullBlock`,
    `SkullBlockEntity`, `SkullBlockRenderer`, `PiglinHeadModel`, `PiglinModel.addHead`, `CustomHeadLayer`,
    `Piglin.dropCustomDeathLoot`, `Creeper.canDropMobsSkull`, `NoteBlock`, `NoteBlockInstrument` and the head
    dispenser behavior, decompiled from the client jar remapped to Mojang's names
    (`G:/MCDownloads/tt-heads-decomp/out`), next to 1.12.2's `BlockSkull`, `ItemSkull`, `TileEntitySkull`,
    `TileEntitySkullRenderer`, `LayerCustomHead`, `BlockNote` and `TileEntityNote`.
*   **Block** (`BlockPiglinHead`, `piglin_head`): one block for standing and wall heads, as 1.12.2's skull is
    (not vanilla's skull block: its type is a 0-5 number in `TileEntitySkull` and its renderer a fixed switch).
    `facing` is `up` for a standing head or the direction a wall head looks; the standing head's 16-step
    rotation is in `TilePiglinHead` (`Rot`, as 1.12.2's skull saves it). 1.20's numbers: strength 1, stone
    sounds, destroyed by pistons, shapes 10 x 8 x 10 standing and 10 x 8 x 8 on a wall, no occlusion, soul sand
    break particles. Rotating or mirroring a structure turns both forms. It drops itself.
*   **Item** (`ItemPiglinHead`): placed by 1.12.2's skull rules (not on a bottom face; a replaceable block is
    placed into; turned to the player, `floor(yaw * 16 / 360 + 0.5) & 15`), with 1.20's place sound, which
    1.12.2's skull item lacks. Worn in the head slot (`getEquipmentSlot`), so mobs, armor stands and the
    inventory's helmet slot take it.
*   **The drop** (`HeadsContent.onDrops`): Nether Update's piglin is found by registry name
    (`netherupdatebackport:piglin`; a null name is no piglin), so nothing links against that mod. As 1.20: an
    adult piglin (babies drop no loot) killed by a charged creeper - not itself - that has not yet caused a
    head drop drops one piglin head, and the creeper counts it (1.12.2's `ableToCauseSkullDrop` also wants
    `doMobLoot`). Piglin brutes and zombified piglins drop none, as in 1.20.
*   **Powered animation**: 1.20.2's `SkullBlockEntity.animation`, client side only: while the block is powered
    (`isBlockPowered`, 1.20's `hasNeighborSignal`) the counter steps once a tick, the partial tick added while
    it runs; unpowered it holds. 1.20 keeps the power in a `powered` state property; here it is read where the
    tile ticks, as 1.12.2's dragon head already does. **The dragon head needs nothing**: 1.12.2 already animates
    a powered dragon head with the same jaw formula (`(sin(t * pi * 0.2) + 1) * 0.2`), standing, on a wall and
    worn.
*   **Client**: `ModelPiglinHead` is `PiglinHeadModel` (64 x 64 piglin texture, our copy of 1.20.6's): head
    10 x 8 x 8, snout, two tusks and two ears whose z rotation is `-(cos(t * pi * 0.24) + 2.5) * 0.2` and
    `(cos(t * pi * 0.2) + 2.5) * 0.2`. `RenderPiglinHead` draws it as `renderSkull` does: standing at the block
    center, on a wall a quarter block out from the wall at 0.25 up, turned away from it; 1.12.2's skull GL setup
    and its break-progress texture matrix (scaled for a 64 x 64 texture). The item renderer draws a standing
    head at 180 degrees, as 1.12.2's skull items; the item model is 1.20's `template_skull` (1.12.2's
    `item/skull` is the same). **Worn**: `LayerPiglinHead` is 1.12.2's `LayerCustomHead` skull branch line for
    line (sneak offset, baby scale, villager offset, 1.1875 scale) with the wearer's limb swing as the
    animation, as `CustomHeadLayer` passes the walk animation. `HeadsClient` adds it beside each
    `LayerCustomHead` the first time that renderer draws an entity (so renderers other mods register are
    covered), on the same head part; the item renderer skips the stack an entity being drawn wears on its
    head, so `LayerCustomHead`'s generic worn-item path shows nothing.
*   **Dispenser**: 1.20's head behavior: equip the first mob, player or armor stand in front that can wear it,
    else fail (the failure click; the head stays). 1.12.2's own skulls are left as 1.12.2 dispenses them.
*   **Note blocks** (`NoteBlockHeads`): 1.20's mob-head instruments on 1.12.2's note block, which plays nothing
    at all with any block above it, so every case is added and none replaced. The sound is
    `block.note_block.imitate.<mob>`: volume 3, record category, pitch 1 whatever the note, no note particle,
    at the block's center. The events are ours and point at 1.12.2's mob events (`entity.skeleton.ambient`,
    `entity.wither_skeleton.ambient`, `entity.zombie.ambient`, `entity.creeper.primed` at pitch 0.5,
    `entity.enderdragon.ambient`); the piglin's carries 1.20.6's piglin idle sounds itself, since 1.12.2 has no
    piglin. A player head is 1.20's custom-head instrument with no sound set (1.12.2's heads have no
    `note_block_sound`), so it is silent. Heads above count standing or on a wall, ours and vanilla's.
    *   *Right click*: vanilla still tunes the block (1.20 tunes it too) and the head's sound plays, when the
        click reaches the block (not sneaking, or sneaking empty-handed).
    *   *Hit* (survival only; creative breaks the block, as 1.20): the head's sound plays.
    *   *Redstone*: note blocks with a head on top are tracked per world: found when a chunk loads (its tile
        entities) and when a head or note block is placed (`BlockEvent.PlaceEvent`), read the next tick. At
        the end of every world tick each one's power is read, and a rising edge plays the head's sound - 1.20's
        `POWERED` property, kept beside the block. A head placed on a powered note block is silent until the
        next edge, as in 1.20. A note block whose head is gone, or whose chunk unloads, is dropped.
    *   *Placing*: a head item used on a note block's top face is placed there without sneaking (1.20's
        `note_block_top_instruments` tag skips the note block's own use); from a side face it is an ordinary
        click.

## Tested (`./gradlew runServer -Pselftest=heads`, port 25923)

`HeadsSelfTest`: registration (ids, translation key, five states and their metadata, strength, piston
reaction, the three shapes, rotation and mirroring of both forms, head slot, own drop, six sound events);
the tile's rotation saved, rotated and mirrored; the placement rotation formula; placement by a player
standing (turned to the player), on a wall (facing out), not on a bottom face. Note blocks: the sound for each
vanilla skull type and for our head standing and on a wall, none for a player head or a plain block; a zombie
head and our head placed on a note block's top without sneaking and without tuning it; a side-face click with a
head item tuning it; right clicks tuning and playing (volume 3, records, pitch 1), sneaking with an item doing
nothing, a survival hit playing; redstone tracking from placement, a sound on each rising edge only, none while
powered or on losing power, none for a player head, untracking when the head is removed, a head put on an
already powered note block silent until the next edge; no mob sound without a head. The drop: only for the
piglin's name, not for a null name, an uncharged creeper, no killer, a baby or with `doMobLoot` off; one head per
creeper. The dispenser: nothing in front keeps the head (not thrown); an armor stand in front gets it.

## Not tested

*   Anything drawn: the block, item and worn renderers, the ear animation (powered and walking), the item
    model's transforms, break particles. These need the client.
*   A real Nether Update piglin dying to a real charged creeper (the drop's rules are tested on a stand-in
    entity with the piglin's name; Nether Update's dev jar was not in `eclipse/mods`).
*   Note blocks set by commands or structures after their chunk loaded (not tracked until the chunk reloads, or
    a head or note block is placed there); a power pulse that rises and falls within one tick.
*   Sculk sensors and allays hearing a head-played note block: 1.20 sends a `note_block_play` game event; this
    stream sends none (no family game-event bus to post it on yet).
