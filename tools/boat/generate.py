"""Boat stream assets: cherry boats and bamboo rafts with and without chests (item models, textures, names).

    python tools/boat/generate.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID
lang = []
for wood, kind in (("cherry", "boat"), ("bamboo", "raft")):
    for chest in ("", "chest_"):
        item = "%s_%s%s" % (wood, chest, kind)
        assets.port_item_model(item)
        lang.append("item.%s.%s.name=%s" % (M, item, assets.name("item.minecraft." + item)))
    assets.texture("entity/boat/" + wood)
    assets.texture("entity/chest_boat/" + wood)
lang.append("entity.%s.boat.name=%s" % (M, assets.name("entity.minecraft.boat")))
lang.append("entity.%s.chest_boat.name=%s" % (M, assets.name("entity.minecraft.chest_boat")))
assets.merge_lang("boat", sorted(lang))
print("boat: %d names" % len(lang))
