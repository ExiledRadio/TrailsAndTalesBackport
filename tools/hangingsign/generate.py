"""Hanging sign stream assets: every wood's hanging and wall hanging sign, item, textures; the chain copy.

    python tools/hangingsign/generate.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID
WOODS = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "cherry", "bamboo", "crimson", "warped", "mangrove"]
SETS = ["hanging_sign", "cherry_wood_hanging_sign", "bamboo_wood_hanging_sign", "nether_wood_hanging_sign"]
HORIZONTAL = ["north", "south", "west", "east"]

lang = []
for w in WOODS:
    c, wall = w + "_hanging_sign", w + "_wall_hanging_sign"
    assets.port_blockstate(c, c, {"rotation": [str(i) for i in range(16)], "attached": ["true", "false"]})
    assets.port_blockstate(wall, wall, {"facing": HORIZONTAL})
    assets.port_item_model(c)
    assets.texture("entity/signs/hanging/" + w)
    assets.texture("gui/hanging_signs/" + w)
    lang.append("tile.%s.%s.name=%s" % (M, c, assets.name("block.minecraft." + c)))
    lang.append("tile.%s.%s.name=%s" % (M, wall, assets.name("block.minecraft." + wall)))
    lang.append("item.%s.%s.name=%s" % (M, c, assets.name("block.minecraft." + c)))

assets.port_blockstate("chain", "chain", {"axis": ["x", "y", "z"]})
assets.port_item_model("chain")
lang.append("tile.%s.chain.name=%s" % (M, assets.name("block.minecraft.chain")))

events = {}
for s in SETS + ["chain"]:
    for part in ("break", "step", "place", "hit", "fall"):
        events["block.%s.%s" % (s, part)] = assets.copy_sound("block.%s.%s" % (s, part))
assets.merge_sounds("hangingsign", events)
assets.merge_lang("hangingsign", sorted(set(lang)) + assets.subtitle_lines(events))
print("hangingsign: %d names, %d sound events" % (len(lang), len(events)))
