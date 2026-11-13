#!/usr/bin/env python3
"""
Stream Lf (look and feel): writes 1.20's title-screen, splash, music and vanilla-text assets.

Every file comes from Mojang's own 1.20.6 files (and 1.19.4's, to know what 1.20 changed):
  G:/MCDownloads/trails-1.20.6/client.jar            the client jar (textures, splashes.txt)
  G:/MCDownloads/tt-lookfeel/assets/                 objects from 1.20.6's asset index (panorama, music, icons)
  G:/MCDownloads/tt-lookfeel/client-1.19.4/client.jar   1.19.4's jar (splashes.txt before 1.20)
  G:/MojangLang/1.20.6/lang/                         1.20.6's language files (vanilla text)

  python -I tools/lookfeel/generate.py
"""

import io
import json
import os
import sys
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.dirname(HERE))
import assets  # noqa: E402

from PIL import Image  # noqa: E402

DL = "G:/MCDownloads/tt-lookfeel"
OBJ = DL + "/assets"
JAR = zipfile.ZipFile("G:/MCDownloads/trails-1.20.6/client.jar")
JAR_1194 = zipfile.ZipFile(DL + "/client-1.19.4/client.jar")
LANG = "G:/MojangLang/1.20.6/lang"
RES = os.path.join(assets.ROOT, "src", "main", "resources")
OURS = assets.ASSETS

BLUR = {"texture": {"blur": True}}

# The languages of the family (Rule 4), as Backport Core's other backports ship vanilla text in.
CODES = ["de_de", "en_us", "es_ar", "es_es", "es_mx", "es_uy", "es_ve", "fr_fr", "it_it", "ja_jp", "ko_kr",
         "nl_nl", "pl_pl", "pt_br", "ru_ru", "tr_tr", "uk_ua", "zh_cn", "zh_tw"]

# 1.12.2's keys whose English 1.20.6 words differently, where 1.12.2 shows the text the same way (no key whose
# 1.12.2 screen appends ": ON" to it, or formats it differently). Cumulative from 1.12.2: the newest installed
# backport's text wins key by key, so this is the text as 1.20.6 has it.
VANILLA_TEXT_KEYS = [
    "build.tooHigh",
    "chat.link.open",
    "container.enchant.level.requirement",
    "createWorld.customize.presets.share",
    "death.attack.cactus.player",
    "death.attack.dragonBreath",
    "death.attack.drown.player",
    "death.attack.hotFloor",
    "death.attack.hotFloor.player",
    "death.attack.inFire.player",
    "death.attack.onFire.player",
    "death.attack.thorns",
    "deathScreen.spectate",
    "deathScreen.title",
    "deathScreen.title.hardcore",
    "deathScreen.titleScreen",
    "debug.creative_spectator.error",
    "debug.creative_spectator.help",
    "demo.day.1",
    "demo.help.fullWrapped",
    "demo.reminder",
    "disconnect.loginFailed",
    "disconnect.loginFailedInfo",
    "disconnect.loginFailedInfo.serversUnavailable",
    "gui.recipebook.moreRecipes",
    "gui.recipebook.toggleRecipes.all",
    "gui.recipebook.toggleRecipes.craftable",
    "gui.toMenu",
    "gui.toTitle",
    "inventory.hotbarInfo",
    "inventory.hotbarSaved",
    "itemGroup.hotbar",
    "key.loadToolbarActivator",
    "key.mouse.left",
    "key.mouse.middle",
    "key.mouse.right",
    "key.saveToolbarActivator",
    "mount.onboard",
    "multiplayer.disconnect.banned",
    "multiplayer.disconnect.ip_banned",
    "multiplayer.disconnect.kicked",
    "multiplayer.disconnect.outdated_client",
    "multiplayer.disconnect.outdated_server",
    "multiplayer.downloadingStats",
    "multiplayer.downloadingTerrain",
    "options.chat.opacity",
    "options.chat.scale",
    "options.narrator.all",
    "options.narrator.chat",
    "options.narrator.notavailable",
    "options.narrator.off",
    "options.narrator.system",
    "options.realmsNotifications",
    "options.vsync",
    "record.nowPlaying",
    "selectServer.direct",
    "selectWorld.edit.openFolder",
    "selectWorld.edit.save",
    "selectWorld.gameMode.adventure.line1",
    "selectWorld.gameMode.hardcore.line1",
    "selectWorld.gameMode.survival.line1",
    "selectWorld.mapFeatures.info",
    "selectWorld.tooltip.snapshot1",
    "selectWorld.versionJoinButton",
    "sign.edit",
    "subtitles.block.iron_trapdoor.close",
    "subtitles.block.iron_trapdoor.open",
    "subtitles.entity.generic.small_fall",
    "subtitles.entity.llama.angry",
    "subtitles.entity.painting.break",
    "subtitles.entity.parrot.imitate.elder_guardian",
    "subtitles.entity.shulker_bullet.hit",
    "subtitles.entity.shulker_bullet.hurt",
    "subtitles.entity.zombie_villager.converted",
    "subtitles.entity.zombie_villager.cure",
    "subtitles.item.armor.equip",
    "subtitles.item.armor.equip_elytra",
]

MENU_TRACKS = ["a_familiar_room", "bromeliad", "crescent_dunes", "echo_in_the_wind"]


def obj(name):
    return os.path.join(OBJ, name.replace("/", "__"))


def save_png(img, rel):
    full = os.path.join(OURS, *rel.split("/"))
    os.makedirs(os.path.dirname(full), exist_ok=True)
    img.save(full, optimize=True)


def copy_file(src, rel):
    data = open(src, "rb").read()
    assets.write(rel, data)


def panorama():
    for i in range(6):
        copy_file(obj("minecraft/textures/gui/title/background/panorama_%d.png" % i),
                  "textures/gui/title/background/panorama_%d.png" % i)
    copy_file(obj("minecraft/textures/gui/title/background/panorama_overlay.png"),
              "textures/gui/title/background/panorama_overlay.png")


def logo_in_112_layout(src):
    """
    1.20's logo (1.20.6's 1024 by 256 file, drawn as 256 by 44 of a 256 by 64 texture at width / 2 - 128) laid
    out the way 1.12.2's GuiMainMenu draws minecraft.png: two 155 by 44 pieces of a 256 by 256 texture, from
    (0, 0) at width / 2 - 137 and from (0, 45) at width / 2 + 18. Kept at the file's 4x resolution.
    """
    k = src.size[0] // 256
    logo = src.crop((0, 0, 256 * k, 44 * k))
    out = Image.new("RGBA", (256 * k, 256 * k), (0, 0, 0, 0))
    # Screen x of the 1.20 logo's left edge is 9 right of 1.12.2's first piece.
    first = 155 - 9  # logo columns in the first piece
    out.paste(logo.crop((0, 0, first * k, 44 * k)), (9 * k, 0))
    out.paste(logo.crop((first * k, 0, 256 * k, 44 * k)), (0, 45 * k))
    return out


def title():
    for name in ("minecraft", "minceraft"):
        src = Image.open(io.BytesIO(JAR.read("assets/minecraft/textures/gui/title/%s.png" % name))).convert("RGBA")
        save_png(logo_in_112_layout(src), "textures/gui/title/%s.png" % name)
        assets.write_json("textures/gui/title/%s.png.mcmeta" % name, BLUR)
    assets.write("textures/gui/title/edition.png", JAR.read("assets/minecraft/textures/gui/title/edition.png"))
    assets.write_json("textures/gui/title/edition.png.mcmeta", BLUR)
    # 1.20.5's menu background tint (16 by 16, drawn tiled 32 to a tile): the loading screen's darkening.
    assets.write("textures/gui/menu_background.png", JAR.read("assets/minecraft/textures/gui/menu_background.png"))
    # 1.20's application icon (a grass block).
    for size in (16, 32):
        copy_file(obj("icons/icon_%dx%d.png" % (size, size)), "textures/gui/icon/icon_%dx%d.png" % (size, size))


def blur_shader():
    """
    1.20.5's shaders/post/blur.json is six box_blur passes (across, then down, at the radius times 1, 1/2 and
    1/4). 1.12.2 has no box_blur program, but its own "blur" program is a box blur with a Radius uniform; the
    loading screen sets each pass's radius from Menu Background Blur (5, 2.5, 1.25 here are 50%'s).
    """
    passes = []
    for i, scale in enumerate([1.0, 1.0, 0.5, 0.5, 0.25, 0.25]):
        across = i % 2 == 0
        passes.append({"name": "blur",
                       "intarget": "minecraft:main" if across else "swap",
                       "outtarget": "swap" if across else "minecraft:main",
                       "uniforms": [{"name": "BlurDir", "values": [1.0, 0.0] if across else [0.0, 1.0]},
                                    {"name": "Radius", "values": [5.0 * scale]}]})
    assets.write_json("shaders/post/menu_blur.json", {"targets": ["swap"], "passes": passes})


def lines(data):
    return [l.rstrip("\r") for l in data.decode("utf-8").split("\n") if l.strip()]


def splashes():
    old = lines(JAR_1194.read("assets/minecraft/texts/splashes.txt"))
    new = lines(JAR.read("assets/minecraft/texts/splashes.txt"))
    added = [s for s in new if s not in old]
    removed = [s for s in old if s not in new]
    assets.write("texts/splashes_added.txt", ("\n".join(added) + "\n").encode("utf-8"))
    assets.write("texts/splashes_removed.txt", ("\n".join(removed) + "\n").encode("utf-8"))
    print("splashes: %d added, %d removed" % (len(added), len(removed)))


def music():
    for t in MENU_TRACKS:
        copy_file(obj("minecraft/sounds/music/game/%s.ogg" % t), "sounds/music/game/%s.ogg" % t)
    # 1.20 put A Familiar Room in the game music (music.game); the built-in pack merges this into 1.12.2's.
    pack = {"music.game": {"sounds": [{"name": "trailsandtalesbackport:music/game/a_familiar_room", "stream": True}]}}
    path = os.path.join(RES, "trailsandtalesbackport_packs", "game_music", "assets", "minecraft", "sounds.json")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(json.dumps(pack, indent=2) + "\n")


def to_lang(value):
    return value.replace("\r", "").replace("\n", "\\n")


def vanilla_text():
    folder = os.path.join(OURS, "vanilla_text")
    os.makedirs(folder, exist_ok=True)
    en = json.load(io.open(os.path.join(LANG, "en_us.json"), encoding="utf-8"))
    for code in CODES:
        path = os.path.join(LANG, code + ".json")
        moj = json.load(io.open(path, encoding="utf-8")) if os.path.isfile(path) else {}
        out = []
        for k in VANILLA_TEXT_KEYS:
            out.append("%s=%s" % (k, to_lang(moj.get(k, en[k]))))
        with open(os.path.join(folder, code + ".lang"), "w", encoding="utf-8", newline="\n") as f:
            f.write("\n".join(out) + "\n")
    print("vanilla text: %d keys in %d languages" % (len(VANILLA_TEXT_KEYS), len(CODES)))


def lang():
    v = assets.VANILLA_LANG
    assets.merge_lang("lookfeel", [
        "config.trailsandtalesbackport.lookAndFeel=Look and Feel",
        "config.trailsandtalesbackport.lookAndFeel.enabled=Enabled",
        "config.trailsandtalesbackport.lookAndFeel.gameMusic=A Familiar Room in the Game Music",
        "options.accessibility.menu_background_blurriness=" + v["options.accessibility.menu_background_blurriness"],
        "options.accessibility.menu_background_blurriness.tooltip=" + v["options.accessibility.menu_background_blurriness.tooltip"],
        "selectWorld.data_read=" + v["selectWorld.data_read"],
        "debug.profiler_chart.help=F3 + 1 = Show profiler chart",
        "debug.fps_charts.help=F3 + 2 = Show FPS and TPS charts",
    ])


if __name__ == "__main__":
    panorama()
    title()
    blur_shader()
    splashes()
    music()
    vanilla_text()
    lang()
    print("done")
