"""Sniffer stream assets: the sniffer (texture, sounds, name), sniffer eggs, torchflowers and pitcher plants.

    python tools/sniffer/generate.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID
lang = []

# Blocks: 1.20.6's blockstates and models, with our property names (the same as 1.20's).
assets.port_blockstate("torchflower", "torchflower", {})
assets.port_blockstate("potted_torchflower", "potted_torchflower", {})
assets.port_blockstate("torchflower_crop", "torchflower_crop", {"age": ["0", "1"]})
assets.port_blockstate("pitcher_crop", "pitcher_crop", {"age": ["0", "1", "2", "3", "4"], "half": ["lower", "upper"]})
assets.port_blockstate("pitcher_plant", "pitcher_plant", {"half": ["lower", "upper"]})
assets.port_blockstate("sniffer_egg", "sniffer_egg", {"hatch": ["0", "1", "2"]})
for block in ("torchflower", "potted_torchflower", "torchflower_crop", "pitcher_crop", "pitcher_plant", "sniffer_egg"):
    lang.append("tile.%s.%s.name=%s" % (M, block, assets.name("block.minecraft." + block)))

# Items.
for item in ("torchflower", "pitcher_plant", "sniffer_egg", "torchflower_seeds", "pitcher_pod"):
    assets.port_item_model(item)
for item in ("torchflower_seeds", "pitcher_pod"):
    lang.append("item.%s.%s.name=%s" % (M, item, assets.name("item.minecraft." + item)))

# The sniffer: texture, name, the egg-crack particle's sprite.
assets.texture("entity/sniffer/sniffer")
assets.texture("particle/glint")
lang.append("entity.%s.sniffer.name=%s" % (M, assets.name("entity.minecraft.sniffer")))

# Advancements: 1.20's three sniffer advancements, under 1.12.2's husbandry tab.
lang += assets.advancement("husbandry/obtain_sniffer_egg", "%s:sniffer_egg" % M,
                           {"obtain_sniffer_egg": {"trigger": "minecraft:inventory_changed",
                                                   "conditions": {"items": [{"item": "%s:sniffer_egg" % M}]}}},
                           "husbandry.obtain_sniffer_egg", parent="minecraft:husbandry/root")
lang += assets.advancement("husbandry/feed_snifflet", "%s:torchflower_seeds" % M,
                           {"feed_snifflet": {"trigger": "minecraft:impossible"}},
                           "husbandry.feed_snifflet", parent="%s:husbandry/obtain_sniffer_egg" % M)
lang += assets.advancement("husbandry/plant_any_sniffer_seed", "%s:pitcher_pod" % M,
                           {"torchflower": {"trigger": "minecraft:placed_block",
                                            "conditions": {"block": "%s:torchflower_crop" % M}},
                            "pitcher_pod": {"trigger": "minecraft:placed_block",
                                            "conditions": {"block": "%s:pitcher_crop" % M}}},
                           "husbandry.plant_any_sniffer_seed", parent="%s:husbandry/feed_snifflet" % M)

# 1.20 hides all three until earned, and requires either seed for the last one.
import json  # noqa: E402

for path in ("husbandry/obtain_sniffer_egg", "husbandry/feed_snifflet", "husbandry/plant_any_sniffer_seed"):
    full = os.path.join(assets.ASSETS, "advancements", *path.split("/")) + ".json"
    data = json.load(open(full, encoding="utf-8"))
    data["display"]["hidden"] = True
    if path.endswith("plant_any_sniffer_seed"):
        data["requirements"] = [["torchflower", "pitcher_pod"]]
    assets.write_json("advancements/%s.json" % path, data)

events = {}
for e in ("death", "digging", "digging_stop", "drop_seed", "eat", "happy", "hurt", "idle", "scenting", "searching",
          "sniffing", "step"):
    events["entity.sniffer." + e] = assets.copy_sound("entity.sniffer." + e)
for e in ("crack", "hatch", "plop"):
    events["block.sniffer_egg." + e] = assets.copy_sound("block.sniffer_egg." + e)
assets.merge_sounds("sniffer", events)
assets.merge_lang("sniffer", sorted(set(lang)) + assets.subtitle_lines(events))
print("sniffer: %d names, %d sound events" % (len(lang), len(events)))
