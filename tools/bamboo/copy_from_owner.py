"""Copies Village & Pillage Backport's bamboo plant into this mod (family Rule 1), so our copy matches exactly.

The block of bamboo is made from bamboo, which is 1.14's and so Village & Pillage's. When that mod is absent,
this mod registers the identical plant: the bamboo stalk, the bamboo shoot ("bamboo_sapling"), the bamboo item
and the scattering of bamboo in jungles. Copying the owner's classes and assets, instead of rewriting them, is
what keeps the states the same, so the owner can rename our copies in place. No pandas, scaffolding or bamboo
jungle biome: they are Village & Pillage's own features, not what our recipes need.

    python tools/bamboo/copy_from_owner.py [path to the Village & Pillage workspace]

Rerun after the owner changes these classes. The copies are written to the `bamboo.plant` package with a
"copied, do not edit" header; `BambooPlant` (ours, not copied) holds what the owner's `BambooContent` held.
"""
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

OWNER = sys.argv[1] if len(sys.argv) > 1 else "E:/MincraftModding/Workspaces/RLCraftVillagerTrading"
OWNER_ID = "villagerbackport"
OWNER_JAVA = OWNER + "/src/main/java/com/exiledradio/villagerbackport/bamboo"
OWNER_ASSETS = OWNER + "/src/main/resources/assets/villagerbackport"
OUR_PACKAGE = "com.exiledradio.%s.bamboo.plant" % assets.MODID
OUR_JAVA = os.path.join(assets.ROOT, "src", "main", "java", *OUR_PACKAGE.split("."))
CLASSES = ["BlockBamboo", "BlockBambooSapling", "BambooFeature", "JungleBamboo"]
BLOCKS = ["bamboo", "bamboo_sapling"]


def java(text):
    text = text.replace("package com.exiledradio.villagerbackport.bamboo;", "package %s;" % OUR_PACKAGE)
    text = text.replace("import com.exiledradio.villagerbackport.ModConfig;",
                        "import com.exiledradio.%s.ModConfig;" % assets.MODID)
    text = text.replace("BambooContent.", "BambooPlant.")
    text = text.replace("BambooFutureMc.generatesJungleBamboo()", "BambooPlant.futureMcBamboo()")
    header = ("// Copied from Village & Pillage Backport by tools/bamboo/copy_from_owner.py (family Rule 1).\n"
              "// Do not edit: rerun the tool instead.\n")
    return header + text


os.makedirs(OUR_JAVA, exist_ok=True)
for name in CLASSES:
    src = open(os.path.join(OWNER_JAVA, name + ".java"), encoding="utf-8").read()
    with open(os.path.join(OUR_JAVA, name + ".java"), "w", encoding="utf-8", newline="\n") as f:
        f.write(java(src))

# Assets: the owner's blockstates, every model they reach, every texture those use, and the item model.
seen_models = set()


def ours(name):
    return name.replace(OWNER_ID + ":", assets.MODID + ":")


def copy_model(ref):
    """ref like 'villagerbackport:block/bamboo1_age0' (or a blockstate's 'villagerbackport:bamboo1_age0')."""
    ns, _, path = ref.rpartition(":")
    if ns != OWNER_ID or path in seen_models:
        return
    seen_models.add(path)
    data = json.load(open("%s/models/%s.json" % (OWNER_ASSETS, path), encoding="utf-8"))
    if "parent" in data:
        copy_model(data["parent"])
        data["parent"] = ours(data["parent"])
    for key, tex in list(data.get("textures", {}).items()):
        tns, _, tpath = tex.rpartition(":")
        if tns == OWNER_ID:
            png = "%s/textures/%s.png" % (OWNER_ASSETS, tpath)
            assets.write("textures/%s.png" % tpath, open(png, "rb").read())
            if os.path.exists(png + ".mcmeta"):
                assets.write("textures/%s.png.mcmeta" % tpath, open(png + ".mcmeta", "rb").read())
            data["textures"][key] = ours(tex)
    assets.write_json("models/%s.json" % path, data)


def walk(node):
    if isinstance(node, dict):
        if "model" in node:
            ref = node["model"]
            ns, _, path = ref.rpartition(":")
            copy_model("%s:%s" % (ns, path if path.startswith(("block/", "item/")) else "block/" + path))
            node["model"] = ours(ref)
        for v in node.values():
            walk(v)
    elif isinstance(node, list):
        for v in node:
            walk(v)


for b in BLOCKS:
    data = json.load(open("%s/blockstates/%s.json" % (OWNER_ASSETS, b), encoding="utf-8"))
    walk(data)
    assets.write_json("blockstates/%s.json" % b, data)
copy_model(OWNER_ID + ":item/bamboo")

# Names and sounds: the owner's are 1.14's vanilla ones, so ours come from Mojang's files as theirs did.
M = assets.MODID
lang = ["tile.%s.bamboo.name=%s" % (M, assets.name("block.minecraft.bamboo")),
        "tile.%s.bamboo_sapling.name=%s" % (M, assets.name("block.minecraft.bamboo_sapling")),
        "item.%s.bamboo.name=%s" % (M, assets.name("block.minecraft.bamboo"))]
events = {}
for e in ["block.bamboo.break", "block.bamboo.fall", "block.bamboo.hit", "block.bamboo.place", "block.bamboo.step",
          "block.bamboo_sapling.break", "block.bamboo_sapling.hit", "block.bamboo_sapling.place"]:
    events[e] = assets.copy_sound(e)
os.makedirs(os.path.join(assets.ROOT, "tools", "bambooplant"), exist_ok=True)
assets.merge_sounds("bambooplant", events)
assets.merge_lang("bambooplant", lang + assets.subtitle_lines(events))
print("bamboo plant: %d classes, %d models, %d sounds" % (len(CLASSES), len(seen_models), len(events)))
