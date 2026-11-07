# Stream K: chiseled bookshelf

Package `bookshelf`. Config `chiseledBookshelf` (`enabled`).

## Changelog section

*   **Chiseled bookshelf** (6 planks, 3 wooden slabs): holds six books, book and quills, written books,
    enchanted books or knowledge books. Click a slot on its front with a book to put it in, click a full slot
    to take the book back. A comparator reads which slot was last used (1-6). Hoppers put books in and take
    them out. Drops its books when broken, and itself only with Silk Touch. It gives no enchanting power.
*   Advancement **The Power of Books**: read the signal of a chiseled bookshelf with a comparator.

## How it works

*   Only the facing is saved in metadata; the six `slot_N_occupied` properties come from the tile entity
    (`getActualState`) and have 1.20's names, so 1.20.6's multipart blockstate is used with only its `AND`
    conditions merged (1.12.2's multipart has no `AND`).
*   Slot under the cursor as 1.20's `getHitSlot`: columns split at 6/16 and 11/16 across the face, rows at half
    height. Clicking an empty slot with anything else does nothing and places nothing.
*   Hoppers through Forge's item handler; books only (this Forge's `ItemStackHandler` does not check
    `isItemValid` on insert, so the handler does), one per slot, a copy stored.
*   Books are in the update packet; each change re-renders the block on clients.
*   Other mods can make an item shelvable with the `bookshelfBook` ore name.
*   The advancement uses an `impossible` criterion granted on placing a comparator that reads a shelf, or a
    shelf a comparator reads (1.20's location condition, as an event).

## Tested headlessly

`./gradlew runServer -Pselftest=all` (port 25887), all streams: **`pass=254 fail=0`**; `check_assets.py`:
**39 blocks, 753 states (all 256 of the shelf's), 33 item models, 262 sound files, 0 errors**.

*   States, facing round trip, recipe from birch planks and oak slabs, fuel, no enchanting power.
*   Slot mapping on all four faces and the column boundaries; side faces have none.
*   A real player placing it (facing them), putting books in and taking them out by clicking, an enchanted
    book going in, a stick refused and not used; the comparator signal after each; the actual state per slot.
*   The item handler: one book per slot, the caller's stack untouched, a stick refused; a ticking hopper above
    emptying its books into the free slots.
*   Saving and loading every slot and the last slot. Breaking: all books drop, no shelf; Silk Touch gives the
    shelf.
*   The advancement granted to a real player placing a comparator that reads the shelf.

## Not tested

*   Anything drawn: the books shown in the front, re-rendering on change, sounds playing.
*   A hopper underneath pulling books out (the handler's extraction is Forge's own).
