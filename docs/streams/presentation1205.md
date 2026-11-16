# Stream presentation1205: 1.20.5's world warnings and Viossa

Package `presentation1205` (client parts in `presentation1205.client`), patch P20 (`asm/ChunkFailureTransformer`).
Config `presentation1205` (`chunkToasts`, `lowDiskSpace`, `recoverWorld`, `viossa`; only `viossa` needs a restart).
Every piece works only while Backport Core's newest installed version is 1.20.5 or later
(`Presentation.isAtLeast("1.20.5")`; this stream registers 1.20.5 itself when any option is on). Language files from
`tools/presentation1205/generate.py`. Dev-server port 25951.

## Changelog section

*   **Chunk toasts** (24w03a): in singleplayer, a red "Failed to load chunk at [x, z]" or "Failed to save chunk at
    [x, z]" toast, "See log for more details", when a chunk cannot be read or written.
*   **Low disk space** (24w03a): opening a singleplayer world with less than 64 MB free shows "Warning! Low disk
    space!" (Continue / Back). While playing with that little space left, the game's saves (every 5 minutes and on
    pausing) and chunk failures show a "Low disk space!" toast.
*   **Failed to Load World** (24w05a): a world whose `level.dat` is missing or cannot be read is listed as "Failed
    to load world summary" with Attempt to Recover; opening it shows what is wrong with `level.dat` and
    `level.dat_old`, and Attempt to Restore (after a backup) brings the world back from `level.dat_old`.
*   **Viossa** (24w06a) in the language list, with Minecraft's own Viossa text.

## How it works

*   **Source**: 1.20.6's `SystemToast`, `ToastComponent`, `IntegratedServer` (`saveEverything`,
    `warnOnLowDiskSpace`, `reportChunkLoadFailure`, `reportChunkSaveFailure`, `tickServer`), `MinecraftServer`
    (autosave), `ChunkMap` (where the reports come from), `LevelStorageSource` (`checkForLowDiskSpace`, 64 MB;
    `getDataTag`, `restoreLevelDataFromOld`, `readLevelSummary`, `CorruptedLevelSummary`), `WorldOpenFlows`
    (`openWorld`, `openWorldLoadLevelData`, `openWorldCheckDiskSpace`), `RecoverWorldDataScreen`,
    `EditWorldScreen.makeBackupAndShowToast`, `ConfirmScreen`, `AlertScreen`, `GenericMessageScreen`,
    `WorldSelectionList`, `Util.safeReplaceOrMoveFile` and the 1.20.6 `pack.mcmeta`, decompiled from the remapped
    1.20.6 client jar (`G:/MCDownloads/tt-screens-decomp`, `G:/MCDownloads/tt-leftovers-decomp`), next to 1.12.2's
    `AnvilChunkLoader`, `ChunkProviderServer`, Forge's `ChunkIOExecutor`/`ChunkIOThreadPoolExecutor`,
    `SaveFormatOld`, `AnvilSaveConverter`, `GuiWorldSelection`, `GuiListWorldSelection(Entry)`,
    `FMLClientHandler.tryLoadExistingWorld`, `GuiToast`, `SystemToast` and `LanguageManager`.
*   **Toasts** (`SystemToast1205`): 1.20.6's `SystemToast` drawn on 1.12.2's toast list: as wide as its text (at
    least 160), the background built in rows from the 160 x 32 system toast of `toasts.png`, the title yellow
    (red for the chunk toasts, the title's own style), the message white, 5 s (low disk space 10 s), and
    `addOrUpdate` (a second failure restarts the shown toast with the new position). 1.12.2's list slides every
    toast by 160 pixels; a wider one moves itself the rest, so it slides by its own width as in 1.20.6. The backup
    toast of a restore is the same class (1.20.6's `WORLD_BACKUP`).
*   **Finding chunk failures** (`ChunkFailures`): 1.12.2 catches the same failures 1.20.6 reports and only logs
    them, so a log4j appender on the three loggers involved hears each one, on the thread that caught it, as it
    happens: Forge's chunk loading threads ("Unhandled exception loading chunk:" followed by the `QueuedChunk`,
    whose text gives the position: every chunk read from disk while playing), `AnvilChunkLoader` ("Failed to save
    chunk", on the file IO thread when a region write fails, where the loader's `chunksBeingSaved` holds the chunk
    being written, or on the server thread when a chunk cannot become NBT) and `ChunkProviderServer` ("Couldn't
    load chunk", "Couldn't save chunk ..."). Those server-thread lines name no chunk, so patch P20 puts a call at
    the first line of `ChunkProviderServer.saveChunkData(chunk)` and `loadChunkFromFile(x, z)` that remembers the
    position for that thread (a Forge-patched class, javac frames; parameters only). An appender was chosen over
    patching the catch blocks: it needs no change to the code that fails, and a log4j reconfiguration that drops
    it is undone at the next world load. Failures that are an `Error` are not reported (1.20.6 crashes on those).
    The appender is installed on a client only; 1.20.6's dedicated server reports nothing.
*   **Reporting** (`Presentation1205Client`): as `IntegratedServer.reportChunk*Failure`: the disk check first
    (the low disk space toast), then the chunk toast, on the client thread.
*   **When the disk is checked** (`LowDiskSpaceTicker`): 1.20.6 checks at the end of every `saveEverything`: the
    autosave every 6000 ticks (counted only while the game runs), `/save-all` (after it ran; 1.12.2 registers
    `/save-all` on dedicated servers only, so this matters only for a mod that adds it) and pausing (the client's
    pause, `Minecraft.isGamePaused`, as `IntegratedServer.tickServer` reads it). `DiskSpace` is 1.20.6's
    check: `Files.getFileStore(worldFolder).getUsableSpace() < 67108864`, and a store that cannot be read counts as
    plenty.
*   **World list** (`WorldListHandler`, `WorldEntry`): every entry 1.12.2 built becomes a `WorldEntry` (an
    entry of another mod's own class is left alone), whose `joinWorld` runs 1.20.6's checks (`WorldOpening`) and
    then 1.12.2's own opening (its version question, Forge's checks). With `recoverWorld` on, a folder whose
    `level.dat` is missing or does not read (gzip NBT, its `Data` through the data fixer into a `WorldInfo`; the
    result cached by the file's time and size) is listed as 1.20.6's `CorruptedLevelSummary`: the folder's name,
    the folder and level.dat's time, red "Failed to load world summary"; Play Selected World reads "Attempt to
    Recover"; Edit and Re-Create are off; Delete works. Folders 1.12.2 left out (neither file reads) are added, as
    1.20.6 lists every folder with either file, and the list is sorted by 1.20.6's order (last played, then name).
*   **Opening a world** (`WorldOpening`): `level.dat` must read, or the Failed to Load World screen opens; then,
    under 64 MB free, 1.20.6's `ConfirmScreen` with the red "Warning! Low disk space!", its two-line description,
    Continue and Back. Back returns to a reloaded world list. Creating a world is not checked (nor in 1.20.6).
*   **Failed to Load World** (`RecoverWorldDataScreen`): 1.20.6's layout (one centered column, 8 px apart: the
    bold title, the message naming the folder in gray, the two state lines, Report a Bug and Attempt to Restore
    side by side at 120 px, Back), the dates in 1.20.6's short localized format. A state line is "State from
    <time>: " and "No issues" (green) or what reading the file threw (red). Report a Bug opens 1.12.2's link
    screen for 1.20.6's `https://aka.ms/snapshotbugs?ref=game`. Attempt to Restore is on only when level.dat fails
    and level.dat_old reads (else its tooltip, "No state to recover from available"); it shows "Attempting to
    restore world..." for a frame, makes a backup with Backport Core's Make Backup (`WorldBackups.backup`, zip in
    `.minecraft/backups`) and its toast, then moves level.dat to `level.dat_corrupted_<yyyy-MM-dd_HH-mm-ss>` and
    level.dat_old to level.dat (1.20.6's `safeReplaceOrMoveFile`, 10 tries a step). 1.12.2's own readers
    (`SaveFormatOld.getWorldData`, Forge's `tryLoadExistingWorld`) leave a level.dat that is not gzip open until it
    is collected, and Windows refuses to move an open file, so the restore first runs a collection and the
    finalizers (and again between tries): "Recovery done" / "Recovery
    was successful!" with Continue (the world opens, from the start of the checks) and Back, or "Failed to
    recover from previous state.".
*   **Viossa** (`ViossaPack`): 1.12.2 builds the language list from the "language" section of every default and
    selected pack's metadata; a pack with no files, added to the client's default packs, answers it with 1.20.6's
    entry (`vp_vl`, "Viossa", region "Vilant", left to right), and the running list gets it at once (the startup
    load already ran). With the option off, a game set to Viossa reads English, as a version without it would.
    Vanilla's 1.12.2 keys in Viossa and this stream's new keys are `vanilla_text_1205/vp_vl.lang`, which Backport
    Core composes into `minecraft:lang/vp_vl.lang` (the composed file is the only `vp_vl` in the `minecraft`
    domain, so nothing hides it; 1.12.2 loads en_us first, so anything untranslated is English). This mod's own
    vanilla-named keys are `lang/vp_vl.lang`.
*   **Text** (`tools/presentation1205/generate.py`): the 23 new keys (`chunk.toast.*`, `recover_world.*`,
    `selectWorld.warning.lowDiskSpace.*`, `gui.continue`) in the family's 19 files and Viossa, Mojang's 1.20.6
    text per language from the 1.20.6 asset index (`selectWorld.edit.backup*` are Backport Core's). 1.12.2's keys
    in Viossa: a key gets 1.20.6's Viossa when 1.20.6 has the same key with the same English (or the key is one Lf's
    `vanilla_text` gives 1.20.6's English), else when every 1.20.6 key with exactly that English has one Viossa
    text, and only when the arguments, line breaks and formatting codes agree: 2568 of 1.12.2's 3303 keys (962 by
    key, 1608 by text; 2 more left English because their arguments differ). This mod's keys: those `tools/lang/vanilla_keys.py` matches to a vanilla name with the same
    English (213). Rerun after any en_us change.

## Tested

*   `./gradlew runServer -Pselftest=presentation1205` (port 25951): **93 pass, 0 fail**. Registration and Core's
    vanilla text folder; the 23 keys in all 20 files, with English's arguments; vp_vl (1.12.2's keys and
    arguments, this mod's file); P20 applied to both methods; the appender against real failures: a chunk that
    cannot become NBT (server thread, P20's position), a region write that throws on the file IO thread
    (`chunksBeingSaved`), stored chunk data that is not NBT read by the server thread (`loadChunkFromFile`) and by
    Forge's chunk loading threads (the `QueuedChunk`), each reported once with its position, and nothing with the
    option off; the 64 MB threshold; no warning on a dedicated server.
*   `./gradlew runClient -Pselftest=screens1205`: **61 pass, 0 fail**. At the title screen: Viossa in the list ("Viossa
    (Vilant)"), vanilla's text, the new keys and this mod's names in Viossa, English fallback, and back; the three
    toasts (width, update in place, text). In a new world: a real chunk save failure on the integrated server shows
    the toast with its position; with 1 MB free (a test override of the drive's space), pausing checks the disk and
    shows the low disk space toast, and the autosave cadence checks on its 6000th tick and not before. At the title screen again: a world with a broken level.dat
    (1.12.2 lists it from level.dat_old) and one with both files broken (1.12.2 leaves it out) both list as
    corrupted, sorted; Attempt to Recover, Edit and Re-Create off; the Failed to Load World screen for each
    (restore on / off); a restore (backup zip, backup toast, `level.dat_corrupted_*`, level.dat reads again,
    Recovery done); the restored world lists normally; the low disk space screen when opening a world, and Back.
    Frames in `eclipse/presentation1205/screenshots/` (toasts, low disk toast, world list, both recovery screens,
    Recovery done, low disk screen).
*   Regressions: `runServer -Pselftest=all` 3974 pass, 0 fail; `tools/check_assets.py` 0 errors;
    `runClient -Pselftest=client` (Lf) 27 pass, 0 fail.
*   Not tested: a real full disk, a real failed region write in a played world, Report a Bug opening a browser,
    and a world opened with Continue after a restore (the restore and the opening are each tested).

## What differs from 1.20.6

*   **Corrupted worlds**: 1.20.6 lists a world from level.dat alone and opens nothing it cannot read; 1.12.2 lists
    a world from level.dat_old when level.dat fails and opens it from level.dat_old without a word (and leaves out
    a world with neither readable). With `recoverWorld` on, this stream does as 1.20.6 for the list and for opening
    from the list; worlds opened another way (another mod's world list, a Realms-like launcher) keep 1.12.2's
    silent fallback, which happens inside `SaveHandler.loadWorldInfo`.
*   **Order of the checks**: 1.20.6 checks the disk after its version and backup prompts; here the disk check comes
    before 1.12.2's own version question (`GuiListWorldSelectionEntry.loadWorld` is private, with no event).
*   **Missing files**: 1.20.6 reads level.dat with `Files.newInputStream`, which throws `NoSuchFileException`,
    not the `FileNotFoundException` its screen checks for, so a missing level.dat_old shows as that exception's
    text rather than "Missing file" and its line is not left empty. This stream reads the files the same way, so it
    shows the same.
*   **Chunk load failures 1.12.2 crashes on**: a chunk that fails to read on the server thread through Forge's
    synchronous path (`ChunkIOExecutor.syncChunkLoad`, a chunk asked for before its asynchronous load) is a crash
    in 1.12.2, where 1.20.6 makes an empty chunk and shows the toast. Turning that crash into an empty chunk is a
    behavior change beyond the toast and is not done. Forge's chunk loading thread also throws
    `IllegalMonitorStateException` in its own `afterExecute` after logging such a failure (its own bug, unchanged).
*   **`/save-all`**: 1.12.2 has it on dedicated servers only, so in singleplayer the low disk space toast comes from
    the autosave cadence, pausing and chunk failures.
*   **Autosave**: 1.12.2 autosaves every 900 ticks; the disk check keeps 1.20.6's 6000-tick cadence rather than
    following 1.12.2's saves, so the low disk space toast comes as often as in 1.20.6.
*   **Notification Time**: 1.20.6 multiplies toast times by its Notification Time option, which 1.12.2 lacks; the
    times are its default.
*   **Backup**: Backport Core's Make Backup (1.13's `createBackup`) names a second backup in the same second
    `<name>_1.zip`, where 1.20.6 uses `<name> (1).zip`.
*   **Backgrounds**: the screens use 1.12.2's menu background (as the other menus do here), not 1.20.5's blurred
    panorama.
