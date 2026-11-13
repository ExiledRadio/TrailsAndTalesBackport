"""Archaeology stream assets: brush, suspicious sand and gravel, pottery sherds, decorated pot, loot tables,
advancements, names and sounds.

    python tools/archaeology/generate.py

The six archaeology loot tables are read from 1.20.6's data and written with the entries 1.12.2 has
(vanilla items, with their 1.12.2 metadata, and this stream's sherds). Entries for items another stream or
family mod owns are printed as OPTIONAL lines: those are added in code (archaeology/ArchaeologyLoot.java)
when the item exists, so a missing item never breaks a table.
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID

SHERDS = ["angler", "archer", "arms_up", "blade", "brewer", "burn", "danger", "explorer", "friend", "heart",
          "heartbreak", "howl", "miner", "mourner", "plenty", "prize", "sheaf", "shelter", "skull", "snort"]

lang = []

# --- brush -------------------------------------------------------------------------------------------
assets.texture("item/brush")
for model in ("brush", "brush_brushing_0", "brush_brushing_1", "brush_brushing_2"):
    data = assets.jar_json("assets/minecraft/models/item/%s.json" % model)
    data["textures"] = {"layer0": "%s:item/brush" % M}
    for o in data.get("overrides", []):
        o["model"] = "%s:item/%s" % (M, o["model"].split("/")[-1])
    assets.write_json("models/item/%s.json" % model, data)
lang.append("item.%s.brush.name=%s" % (M, assets.name("item.minecraft.brush")))

# --- suspicious sand and gravel ----------------------------------------------------------------------
for block in ("suspicious_sand", "suspicious_gravel"):
    assets.port_blockstate(block, block, {"dusted": ["0", "1", "2", "3"]})
    assets.item_from_block(block, block + "_0")
    lang.append("tile.%s.%s.name=%s" % (M, block, assets.name("block.minecraft." + block)))

# --- sherds ------------------------------------------------------------------------------------------
for s in SHERDS:
    name = s + "_pottery_sherd"
    assets.item_generated(name, assets.texture("item/" + name))
    lang.append("item.%s.%s.name=%s" % (M, name, assets.name("item.minecraft." + name)))

# --- decorated pot -----------------------------------------------------------------------------------
assets.texture("entity/decorated_pot/decorated_pot_base")
assets.texture("entity/decorated_pot/decorated_pot_side")
for s in SHERDS:
    assets.texture("entity/decorated_pot/%s_pottery_pattern" % s)
assets.port_blockstate("decorated_pot", "decorated_pot",
                       {"cracked": ["false", "true"], "facing": ["north", "south", "west", "east"]})
pot = assets.jar_json("assets/minecraft/models/item/decorated_pot.json")
pot.pop("gui_light", None)
pot["textures"] = {"particle": assets.texture("entity/decorated_pot/decorated_pot_side")}
assets.write_json("models/item/decorated_pot.json", pot)
lang.append("tile.%s.decorated_pot.name=%s" % (M, assets.name("block.minecraft.decorated_pot")))

# --- sounds ------------------------------------------------------------------------------------------
events = {}
for prefix in ("block.suspicious_sand", "block.suspicious_gravel", "block.decorated_pot"):
    for part in ("break", "step", "place", "hit", "fall"):
        events[prefix + "." + part] = assets.copy_sound(prefix + "." + part)
for e in ("block.decorated_pot.insert", "block.decorated_pot.insert_fail", "block.decorated_pot.shatter",
          "item.brush.brushing.generic", "item.brush.brushing.sand", "item.brush.brushing.gravel",
          "item.brush.brushing.sand.complete", "item.brush.brushing.gravel.complete"):
    events[e] = assets.copy_sound(e)

# --- loot tables -------------------------------------------------------------------------------------
# 1.20 name -> (1.12.2 item, metadata); None = owned elsewhere, added in code.
VANILLA = {
    "diamond": ("minecraft:diamond", 0), "tnt": ("minecraft:tnt", 0), "gunpowder": ("minecraft:gunpowder", 0),
    "emerald": ("minecraft:emerald", 0), "brick": ("minecraft:brick", 0), "stick": ("minecraft:stick", 0),
    "iron_axe": ("minecraft:iron_axe", 0), "wheat": ("minecraft:wheat", 0),
    "wooden_hoe": ("minecraft:wooden_hoe", 0), "coal": ("minecraft:coal", 0),
    "gold_nugget": ("minecraft:gold_nugget", 0), "clay": ("minecraft:clay_ball", 0),
    "yellow_dye": ("minecraft:dye", 11), "light_blue_dye": ("minecraft:dye", 12),
    "orange_dye": ("minecraft:dye", 14),
    "magenta_stained_glass_pane": ("minecraft:stained_glass_pane", 2),
    "pink_stained_glass_pane": ("minecraft:stained_glass_pane", 6),
    "blue_stained_glass_pane": ("minecraft:stained_glass_pane", 11),
    "light_blue_stained_glass_pane": ("minecraft:stained_glass_pane", 3),
    "red_stained_glass_pane": ("minecraft:stained_glass_pane", 14),
    "yellow_stained_glass_pane": ("minecraft:stained_glass_pane", 4),
    "purple_stained_glass_pane": ("minecraft:stained_glass_pane", 10),
    "wheat_seeds": ("minecraft:wheat_seeds", 0), "beetroot_seeds": ("minecraft:beetroot_seeds", 0),
    "dead_bush": ("minecraft:deadbush", 0), "flower_pot": ("minecraft:flower_pot", 0),
    "string": ("minecraft:string", 0), "lead": ("minecraft:lead", 0),
}
for s in SHERDS:
    VANILLA[s + "_pottery_sherd"] = ("%s:%s_pottery_sherd" % (M, s), 0)

for table in ("desert_pyramid", "desert_well", "ocean_ruin_cold", "ocean_ruin_warm", "trail_ruins_common",
              "trail_ruins_rare"):
    data = assets.jar_json("data/minecraft/loot_tables/archaeology/%s.json" % table)
    (pool,) = data["pools"]
    entries = []
    for e in pool["entries"]:
        name = e["name"].split(":")[1]
        weight = e.get("weight", 1)
        if name in VANILLA and not e.get("functions"):
            item, meta = VANILLA[name]
            entry = {"type": "item", "name": item, "weight": weight, "entryName": name}
            if meta:
                entry["functions"] = [{"function": "set_data", "data": meta}]
            entries.append(entry)
        else:
            print("OPTIONAL %s %s weight=%d functions=%s" % (table, name, weight, json.dumps(e.get("functions"))))
    assets.write_json("loot_tables/archaeology/%s.json" % table,
                      {"pools": [{"name": "main", "rolls": pool["rolls"], "entries": entries}]})

# --- advancements ------------------------------------------------------------------------------------
tables = ["desert_pyramid", "desert_well", "ocean_ruin_cold", "ocean_ruin_warm", "trail_ruins_rare",
          "trail_ruins_common"]
criteria = {t: {"trigger": "minecraft:impossible"} for t in tables}
for s in SHERDS:
    criteria["has_" + s] = {"trigger": "minecraft:inventory_changed",
                            "conditions": {"items": [{"item": "%s:%s_pottery_sherd" % (M, s)}]}}
lang += assets.advancement("adventure/salvage_sherd", "%s:brush" % M, criteria, "adventure.salvage_sherd")
path = os.path.join(assets.ASSETS, "advancements", "adventure", "salvage_sherd.json")
adv = json.load(open(path, encoding="utf-8"))
adv["requirements"] = [tables, ["has_" + s for s in SHERDS]]
assets.write_json("advancements/adventure/salvage_sherd.json", adv)

lang += assets.advancement("adventure/craft_decorated_pot_using_only_sherds", "%s:decorated_pot" % M,
                           {"pot_crafted_using_only_sherds": {"trigger": "minecraft:impossible"}},
                           "adventure.craft_decorated_pot_using_only_sherds",
                           parent="%s:adventure/salvage_sherd" % M)
path = os.path.join(assets.ASSETS, "advancements", "adventure", "craft_decorated_pot_using_only_sherds.json")
adv = json.load(open(path, encoding="utf-8"))
adv["display"]["icon"]["nbt"] = ('{BlockEntityTag:{sherds:["minecraft:brick","%s:heart_pottery_sherd",'
                                 '"minecraft:brick","%s:explorer_pottery_sherd"]}}' % (M, M))
assets.write_json("advancements/adventure/craft_decorated_pot_using_only_sherds.json", adv)

assets.merge_sounds("archaeology", events)
assets.merge_lang("archaeology", sorted(set(lang)) + assets.subtitle_lines(events))
print("archaeology: %d lang lines, %d sound events" % (len(lang), len(events)))
