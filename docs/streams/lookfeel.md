# Stream Lf: look and feel

Package `lookfeel` (client side in `lookfeel.client`). Config `lookAndFeel` (`enabled`, `gameMusic`). Packet ids
90-94 (none used). Dev-server port 25918.

1.20 to 1.20.6's changes beyond content, handed to Backport Core's `presentation` module as versions 1.20, 1.20.2,
1.20.3 and 1.20.5. Core shows the newest installed version's single-slot pieces and merges the lists, so with a
newer backport installed (Tricky Trials) its pieces win where it has them.

## Changelog section

*   **Title screen:** the cherry grove panorama, 1.20's logo with the larger "Java Edition" under it, its own
    Minceraft logo, and the splash where 1.20 puts it. Six new splashes ("This sand is sus", "Sniff sniff..." and
    the rest), and "Verlet integration!" spelled right.
*   **Menu music:** A Familiar Room, Bromeliad, Crescent Dunes and Echo in the Wind play on the title screen; Aerie,
    Firebugs and Labyrinthine no longer do. A Familiar Room also plays in every overworld biome.
*   **Loading a world:** the loading screen shows the blurred panorama behind the chunk map, in 1.20's colors.
*   **Options:** Menu Background Blur (Accessibility Settings and Video Settings). Hide Splash Texts appears in
    Accessibility Settings. Hold Ctrl and scroll in Video Settings to change GUI Scale.
*   **F3:** F3 + 1 shows the profiler chart and F3 + 2 the FPS and TPS charts; Shift + F3 and Alt + F3 no longer do.
*   **Menus:** "Back" instead of "Cancel" on the Multiplayer and World Selection screens. Vanilla text reads as in
    1.20.6: "Game Over!", "Left Button", death messages with "while" instead of "whilst", and the rest.
*   **Window icon:** a grass block.

## How it works

*   `LookAndFeel.preInit` (both sides) registers each version with `Presentation.version`: the six faces and
    1.20.6's overlay, `texts/splashes_added.txt` / `splashes_removed.txt` (1.19.4's `splashes.txt` against
    1.20.6's; Core drops the Java version splash), the logo, `edition.png` and their `.mcmeta` (smooth scaling, as
    1.20.6's 4x textures have) and `menu_background.png` as vanilla assets Core serves, the four menu tracks as
    files (`trailsandtalesbackport:music/game/<track>`) and the three removals, and `vanilla_text/`.
*   The logo: 1.12.2's `GuiMainMenu` draws `minecraft.png` as two 155 by 44 pieces, from (0, 0) and (0, 45), at
    width / 2 - 137. `tools/lookfeel/generate.py` puts 1.20.6's logo into that layout (9 pixels in, at its 4x
    size), so vanilla's own draw shows 1.20's logo at 1.20's place. The rest is `LookAndFeelClient`'s, only while
    Core serves this mod's logo: "Java Edition" drawn after the screen (`DrawScreenEvent.Post`, before Core's fade
    overlay); the splash taken out of the screen in `Pre` and drawn after "Java Edition" at width / 2 + 123, 69
    (`SplashRenderer`); a Minceraft roll (1 in 10,000) bound to 1.20's `minceraft.png`. Core requests 10.1.
*   Menu music is Core's pool. The removals name `minecraft:` and `wildupdatebackport:` paths (Core request 10.3).
*   `GameMusicPack` (client, pre-init, behind `gameMusic`): a built-in pack serving `minecraft:sounds.json` from
    `trailsandtalesbackport_packs/game_music/`, adding A Familiar Room to `music.game` without `replace`, so
    1.12.2's tracks and any other pack's stay.
*   `LoadingScreen1205` is the drawer of Core's `PANORAMA_1_20_5` revision: the panorama and overlay drawn into a
    framebuffer, blurred by `shaders/post/menu_blur.json` (six passes of 1.12.2's own `blur` program, radius = Menu
    Background Blur x 10 x 1, 1/2, 1/4, as 1.20.6's `box_blur` passes), drawn back, then `menu_background.png`
    tiled at 32. Before the spawn area starts, "Reading world data..." in the middle (`GenericMessageScreen`); then
    Core's grid (1.12.2's spawn area, 43 by 43) centered with the percentage just above, colored by
    `LookRules.gridColor` (23w16a). Without framebuffers or shaders, or at 0% blur, the panorama is drawn unblurred.
*   Menu Background Blur: a `SliderOption`, 0 to 100% (OFF at 0), 50% default, after Auto-Jump on Accessibility
    Settings and also on Video Settings (1.20.6's lists). It changes only the loading screen until Core draws
    1.20.5's menu backgrounds (Core request 10.5).
*   F3 + 1 / F3 + 2 are Core debug keys of 1.20.2 that set `showDebugProfilerChart` / `showLagometer`; on F3's
    release (`KeyInputEvent`) the two are set back to what the keys chose, undoing 1.12.2's Shift and Alt. Their
    F3 + Q lines are this mod's own text until Core can register a key with no line (Core request 10.2).
*   Ctrl + scroll (`GuiScreenEvent.MouseInputEvent.Pre` on `GuiVideoSettings`): `LookRules.nextGuiScale`, 1.20.6's
    rule (Auto counts as one above the most the window fits; never below 1 or above that), then the screen is laid
    out again at the new scale.
*   Back: on `InitGuiEvent.Post` of `GuiMultiplayer` and `GuiWorldSelection`, button 0 reading "Cancel" reads
    `gui.back`.
*   Vanilla text: 77 of 1.12.2's keys whose 1.20.6 English differs and that 1.12.2 shows the same way (no key that
    1.12.2 follows with ": ON", none whose placeholders changed), written by `generate.py` in the family's 19
    language files from Mojang's 1.20.6 files. Cumulative from 1.12.2, so with Nether Update installed both agree
    and the newest wins key by key.
*   Window icon: `Display.setIcon` with 1.20's 16 and 32 pixel icons in pre-init (not on macOS, as vanilla).

`tools/lookfeel/generate.py` writes every asset above from Mojang's files (1.20.6's jar and asset index, 1.19.4's
jar, 1.20.6's language files), with the paths in its header.

## Tested

*   `./gradlew runServer -Pselftest=lookfeel` (port 25918): **`pass=117 fail=0`**. All streams,
    `-Pselftest=all`: **`pass=525 fail=0`**. `check_assets.py`: **0 errors**.
    *   The four versions registered by this mod; 1.20.5 the newest; the loading revision 1.20.5's; Hide Splash
        Texts shown.
    *   The six faces (1024 by 1024) and the overlay; the logo, `.mcmeta`, edition and menu background served by
        Core from this mod; the logo's layout (first piece 9 pixels in, second at row 45); Minceraft; the icons;
        the blur's six passes.
    *   Splash pool: the seven additions in, the Java one and the misspelling out, the size exact.
    *   Menu pool: the four tracks in (and their files), 1.12.2's in, Aerie and Firebugs (both namespaces) out; the
        game music pack's `music.game` adds without replacing.
    *   Menu Background Blur's screen, second screen, version, place, default, OFF label, radius 5.
    *   `nextGuiScale` (six cases) and the grid colors.
    *   Vanilla text: 1.20.6's English for four keys, every language with en_us's 77 keys; the lang keys.
*   `./gradlew runClient -Pselftest=client` (a real client, then it quits): **`pass=28 fail=0`**. The cherry
    panorama drawn, Core serving this mod's logo with its `.mcmeta`, "Java Edition" and the splash drawn each
    frame, the icon set, Minceraft bound and unbound, A Familiar Room in `music.game`, F3 + 1 and F3 + 2 in the
    key map and toggling both charts, five texts through `I18n`, Menu Background Blur shown, World Selection's
    Back; then a new world loaded through 1.20.5's screen: frames drawn, with the grid, over the blurred panorama,
    and one frame saved. The saved title and loading frames (`eclipse/lookfeel/`) were looked at: 1.20's logo,
    "Java Edition", the splash at 1.20's place over it; the loading screen's blurred, darkened cherry panorama
    behind the grid.

## Not tested

*   Whether `music.game` keeps 1.12.2's tracks: the dev client has no vanilla `sounds.json` (FG2's asset download
    is refused), so only the addition could be seen. The pack has no `replace`, which is what keeps them.
*   Ctrl + scroll and the F3 release reset with real input (the rules are tested, the events are not).
*   The menu tracks playing (Core's player picks them; the pool is tested).
*   Other languages' vanilla text in game.

## Built after the first pass (branch `tt-leftovers`)

*   **1.20's biome music** (`music.BiomeMusic`, client `music.client`; config `lookAndFeel.biomeMusic`): all 17 of
    1.20.6's `music.overworld.*` lists, track for track with their weights and volumes, as Backport Core's "1.20"
    music step: jungles, sparse jungles, bamboo jungles, deserts, badlands, forests (birch and dark forests too),
    flower forests, old growth taigas and the cherry grove play their music where the game music would. In Caves &
    Cliffs' eight 1.18 biomes (its "1.18" step chooses them) the same biome's 1.20 list plays. The sound lists are
    written at load from the tracks installed: 1.12.2's own under their old names (Clark is `calm2` ...), this mod's
    five, Caves & Cliffs' 1.18 tracks, Wild Update's swamp tracks; a missing mod's tracks are left out. The cherry
    grove's own step is gone (this replaces it). Swamp and deep dark music are 1.19's (Wild Update).
*   **Narrator Hotkey** (1.20.2): Backport Core's, on the `core-tt` branch (patch P32 and the option; request 10.8).
*   **Dimension-change screen** (1.20.5, `lookfeel.client.DimensionScreen`): to or from the Nether the portal's
    animated texture fills "Loading terrain...", to or from the End the end portal's effect (1.20.6's
    `rendertype_end_portal` shader in fixed function, with 1.20.6's textures); a respawn after death and every
    other change keep the menu background.
*   **Ctrl + pick block** (1.20.5, patch P1): no "(+NBT)" line, and a named block gives a named item.
*   **Third-person shield arm** (1.20.5, `lookfeel.client.ArmPoses` on Core's player model hook): the blocking arm
    follows the head (pitch clamped to -80..25 degrees, yaw to -30..30), as `HumanoidModel.poseBlockingArm`.
*   Combined step sounds and the `/kill` and world border death messages turned out to be 1.20's (not 1.20.5's):
    they are stream X's (`misc.StepSounds`, patch P2; `misc.DeathMessages`), config `misc.stepSounds` and
    `misc.deathMessages`.
*   The Failed to Load World screen, the chunk and disk-space toasts and Viossa: `docs/streams/presentation1205.md`.
    Per-effect potion particles: `docs/streams/effectparticles.md`.

## In Backport Core (branch `core-tt`)

*   1.20.5's menu look on every screen and the panorama motion: Core's (requests 10.5, 10.6), on `core-tt`.
*   F3 + 3 (network charts): built in Backport Core (`core-tt`, request 10.9).
*   Buttons, check boxes and the Accessibility and Language icons: 1.20.2's sprites match 1.15's textures pixel for
    pixel, so nothing to replace.
