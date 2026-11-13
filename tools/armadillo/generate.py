"""Armadillo stream assets: the armadillo (texture, 13 sound events, name), the scute, loot tables, advancement.

    python tools/armadillo/generate.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID
lang = []

# The armadillo's texture and name.
assets.texture("entity/armadillo")
lang.append("entity.%s.armadillo.name=%s" % (M, assets.name("entity.minecraft.armadillo")))

# The scute: 1.20.6's item model and texture.
assets.port_item_model("armadillo_scute")
lang.append("item.%s.armadillo_scute.name=%s" % (M, assets.name("item.minecraft.armadillo_scute")))

# Loot: 26.3's two scute tables (shedding, brushing), each one scute, in 1.12.2's format.
for path in ("gameplay/armadillo_shed", "brush/armadillo"):
    assets.write_json("loot_tables/%s.json" % path, {"pools": [{
        "name": "main", "rolls": 1,
        "entries": [{"type": "item", "name": "%s:armadillo_scute" % M, "weight": 1}]}]})

# Isn't It Scute?: 1.20.5's adventure advancement, granted by code when a player brushes an armadillo.
lang += assets.advancement("adventure/brush_armadillo", "%s:armadillo_scute" % M,
                           {"brush_armadillo": {"trigger": "minecraft:impossible"}},
                           "adventure.brush_armadillo", parent="minecraft:adventure/root")

events = {}
for e in ("ambient", "brush", "death", "eat", "hurt", "hurt_reduced", "land", "peek", "roll", "scute_drop", "step",
          "unroll_finish", "unroll_start"):
    events["entity.armadillo." + e] = assets.copy_sound("entity.armadillo." + e)
assets.merge_sounds("armadillo", events)
assets.merge_lang("armadillo", sorted(set(lang)) + assets.subtitle_lines(events))
print("armadillo: %d names, %d sound events" % (len(lang), len(events)))
