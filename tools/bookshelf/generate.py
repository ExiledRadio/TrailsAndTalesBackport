"""Chiseled bookshelf stream assets: blockstate, models, item model, name, sounds, The Power of Books.

    python tools/bookshelf/generate.py
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID


def flatten(when):
    """1.12.2 multipart conditions have no AND: merge its terms into one condition (all must hold)."""
    if "AND" in when:
        out = {}
        for term in when["AND"]:
            out.update(flatten(term))
        return out
    return when


assets.port_blockstate("chiseled_bookshelf", "chiseled_bookshelf", None)
path = os.path.join(assets.ASSETS, "blockstates", "chiseled_bookshelf.json")
data = json.load(open(path, encoding="utf-8"))
for part in data["multipart"]:
    if "when" in part:
        part["when"] = flatten(part["when"])
assets.write_json("blockstates/chiseled_bookshelf.json", data)
assets.port_item_model("chiseled_bookshelf")

lang = ["tile.%s.chiseled_bookshelf.name=%s" % (M, assets.name("block.minecraft.chiseled_bookshelf"))]
lang += assets.advancement("adventure/read_power_of_chiseled_bookshelf", "%s:chiseled_bookshelf" % M,
                           {"chiseled_bookshelf": {"trigger": "minecraft:impossible"}},
                           "adventure.read_power_from_chiseled_bookshelf")
events = {}
for e in ["break", "step", "place", "hit", "fall", "insert", "insert.enchanted", "pickup", "pickup.enchanted"]:
    events["block.chiseled_bookshelf." + e] = assets.copy_sound("block.chiseled_bookshelf." + e)
assets.merge_sounds("bookshelf", events)
assets.merge_lang("bookshelf", lang + assets.subtitle_lines(events))
print("bookshelf: %d lines, %d sound events" % (len(lang), len(events)))
