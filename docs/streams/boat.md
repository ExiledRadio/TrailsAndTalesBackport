# Stream Bo: boats and rafts

Package `boat`. Config `boats` (`enabled`). Entity ids 1 (`boat`) and 2 (`chest_boat`).

## Changelog section

*   **Cherry boats** and **bamboo rafts** (five planks in a U), and the **cherry boat with chest** and **bamboo
    raft with chest** (a boat or raft and a chest). They row, float, carry riders and break like any boat; a
    raft seats its riders higher. A boat with a chest holds 27 stacks: sneak and right-click it to open the
    chest. It takes one rider, and spills its chest when broken.

## How it works

*   `EntityModBoat` extends vanilla's `EntityBoat`, so every boat rule is vanilla's; the wood is synced data and
    saved as `Wood` (by name), the item and fall-crash planks are the wood's own (vanilla's crash drops oak
    planks by metadata; `entityDropItem` swaps them).
*   `EntityModChestBoat`: 27-slot chest (a chest's container; closes past 8 blocks or when the boat goes), one
    rider sitting 0.15 forward (1.19's), contents dropped on breaking but kept through a dimension change.
    Chest boats are Wild Update's mechanic; this is the local shim until Backport Core's shared boat registry
    (`CORE_REQUESTS.md` 4).
*   `ItemModBoat`: vanilla's boat placement (look ray that sees water, refused when blocked), burns 1200.
*   Client: 1.20's boat, raft and chest models in 1.12.2 parts (`ModelWoodBoat`), drawn the way vanilla's boat
    renderer draws (rocking when hit, the water mask in a boat's hull; rafts have none), with 1.20's textures.

## Tested headlessly

`./gradlew runServer -Pselftest=all` (port 25883), all streams: **`pass=364 fail=0`**; `check_assets.py`:
**46 item models, 0 errors**.

*   The four items, names, stack size, fuel; all four recipes.
*   A real player placing a cherry boat on water by looking at it; the boat cherry, picked as the cherry boat,
    floating 40 ticks, broken by a hit and dropping the cherry boat.
*   A raft's rider offset above a boat's.
*   A chest raft: one rider fits, a second does not; 27 slots; sneak-click opening a chest container; saved and
    loaded by entity id with its wood and items; broken, dropping its contents and itself.

## Not tested

*   Anything drawn: models (the raft and chest parts are rebuilt from 1.20's model layers; positions worth a look
    in game), textures, paddle animation, the water mask.
*   Rowing by a real client, riding with two passengers in a plain boat, the fall-crash planks.
*   Moving a chest boat between dimensions.
