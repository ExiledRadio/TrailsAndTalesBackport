"""Copies the owner's stripped logs and stripped wood (six woods) from Village & Pillage Backport into this mod,
so our copies match the owner's exactly. Trails & Tales needs them for 1.20's hanging sign recipes.

Rule 1 of the family plan: when the owner is absent we register our own copy with the same states and
metadata. Copying the owner's block classes and assets - instead of rewriting them - is what keeps them the
same. Rerun after the owner changes these blocks. Registration and axe stripping are ours
(woodcopy/StrippedLogCopies), not copied: they differ. Registered only when neither Village & Pillage nor
Update Aquatic (which has its own copies) is installed.

    python tools/woodset/copy_from_owner.py [path to the Village & Pillage workspace]
"""
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

OWNER = sys.argv[1] if len(sys.argv) > 1 else "E:/MincraftModding/Workspaces/RLCraftVillagerTrading"
OWNER_ID = "villagerbackport"
OWNER_JAVA = OWNER + "/src/main/java/com/exiledradio/villagerbackport/woodset"
OWNER_ASSETS = OWNER + "/src/main/resources/assets/villagerbackport"
OUR_JAVA = os.path.join(assets.ROOT, "src", "main", "java", "com", "exiledradio", assets.MODID, "woodcopy")

CLASSES = ["Wood", "BlockStrippedLog"]
WOODS = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak"]


def names():
    out = []
    for w in WOODS:
        out += ["stripped_%s_log" % w, "stripped_%s_wood" % w]
    return out


def rename(text):
    return text.replace(OWNER_ID + ":", assets.MODID + ":")


def copy_classes():
    os.makedirs(OUR_JAVA, exist_ok=True)
    for c in CLASSES:
        src = open("%s/%s.java" % (OWNER_JAVA, c), encoding="utf-8").read()
        src = src.replace("package com.exiledradio.villagerbackport.woodset;",
                          "package com.exiledradio.%s.woodcopy;" % assets.MODID)
        header = ("// Copied from the owning mod by tools/woodcopy/copy_from_owner.py. Do not edit here: the copy must\n"
                  "// stay identical to the owner's block, so the owner can take our blocks over by renaming them.\n")
        with open(os.path.join(OUR_JAVA, c + ".java"), "w", encoding="utf-8", newline="\n") as f:
            f.write(header + src)


def models_of(blockstate):
    found = set()

    def walk(node):
        if isinstance(node, dict):
            for k, v in node.items():
                if k == "model" and isinstance(v, str):
                    found.add(v)
                else:
                    walk(v)
        elif isinstance(node, list):
            for v in node:
                walk(v)
    walk(blockstate)
    return found


def copy_model(name, seen):
    """Copies an owner model and, recursively, its owner parents and owner textures."""
    ns, _, path = name.rpartition(":")
    if ns not in ("", OWNER_ID) or name in seen:
        return
    if ns == "":
        return  # vanilla model
    seen.add(name)
    data = json.load(open("%s/models/%s.json" % (OWNER_ASSETS, path), encoding="utf-8"))
    parent = data.get("parent", "")
    if parent.startswith(OWNER_ID + ":"):
        copy_model(parent, seen)
    for tex in data.get("textures", {}).values():
        if tex.startswith(OWNER_ID + ":"):
            tpath = tex.split(":", 1)[1]
            for ext in (".png", ".png.mcmeta"):
                src = "%s/textures/%s%s" % (OWNER_ASSETS, tpath, ext)
                if os.path.exists(src):
                    assets.write("textures/%s%s" % (tpath, ext), open(src, "rb").read())
    assets.write("models/%s.json" % path, rename(json.dumps(data, indent=2)) + "\n")


def copy_assets():
    seen = set()
    for n in names():
        state = json.load(open("%s/blockstates/%s.json" % (OWNER_ASSETS, n), encoding="utf-8"))
        assets.write("blockstates/%s.json" % n, rename(json.dumps(state, indent=2)) + "\n")
        for m in models_of(state):
            # A vanilla-format blockstate's model names always live under models/block/.
            ns, _, path = m.rpartition(":")
            copy_model("%s:block/%s" % (ns or "minecraft", path), seen)
        copy_model("%s:item/%s" % (OWNER_ID, n), seen)

    lang = open(OWNER_ASSETS + "/lang/en_us.lang", encoding="utf-8").read().split("\n")
    wanted = set("tile.%s.%s.name" % (OWNER_ID, n) for n in names())
    lines = []
    for line in lang:
        key = line.split("=", 1)[0]
        if key in wanted:
            lines.append(line.replace("." + OWNER_ID + ".", "." + assets.MODID + ".", 1))
    assets.merge_lang("woodcopy", ["# Copied from the owning mod by tools/woodcopy/copy_from_owner.py."] + sorted(set(lines)))
    print("copied %d blocks, %d models" % (len(names()), len(seen)))


if __name__ == "__main__":
    copy_classes()
    copy_assets()
