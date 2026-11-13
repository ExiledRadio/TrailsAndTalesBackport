"""Wolves stream assets: the eight new variants' textures, wolf armor (item, entity layers, sounds), advancements.

    python tools/wolf/generate.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID
lang = []

# The eight variants that 1.12.2 lacks: wild, tame and angry textures each (pale is vanilla's own wolf).
for v in ("ashen", "black", "chestnut", "rusty", "snowy", "spotted", "striped", "woods"):
    for suffix in ("", "_tame", "_angry"):
        assets.texture("entity/wolf/wolf_%s%s" % (v, suffix))

# The armor on the wolf: base, dyed overlay and the three crack stages.
for t in ("wolf_armor", "wolf_armor_overlay", "wolf_armor_crackiness_low", "wolf_armor_crackiness_medium",
          "wolf_armor_crackiness_high"):
    assets.texture("entity/wolf/" + t)

# The item. 1.20.5 tints the overlay with a transparent color when undyed; 1.12.2's item tint has no alpha, so
# the overlay layer is its own model, chosen by the "dyed" property.
base = assets.texture("item/wolf_armor")
overlay = assets.texture("item/wolf_armor_overlay")
assets.write_json("models/item/wolf_armor_dyed.json",
                  {"parent": "item/generated", "textures": {"layer0": base, "layer1": overlay}})
assets.write_json("models/item/wolf_armor.json", {
    "parent": "item/generated", "textures": {"layer0": base},
    "overrides": [{"predicate": {"%s:dyed" % M: 1}, "model": "%s:item/wolf_armor_dyed" % M}]})
lang.append("item.%s.wolf_armor.name=%s" % (M, assets.name("item.minecraft.wolf_armor")))

# Advancements: 1.20.5's three under husbandry, granted by code (1.12.2 has no item-on-entity trigger and its
# tame trigger knows no variants).
VARIANTS = ("ashen", "black", "chestnut", "pale", "rusty", "snowy", "spotted", "striped", "woods")
ADV = [
    ("repair_wolf_armor", "%s:wolf_armor" % M, {"repair_wolf_armor": {"trigger": "minecraft:impossible"}}, "task", None),
    ("remove_wolf_armor", "minecraft:shears", {"remove_wolf_armor": {"trigger": "minecraft:impossible"}}, "task", None),
    ("whole_pack", "minecraft:bone", dict(("minecraft:" + v, {"trigger": "minecraft:impossible"}) for v in VARIANTS),
     "challenge", 50),
]
for path, icon, criteria, frame, xp in ADV:
    key = "advancements.%s.husbandry.%s" % (M, path)
    adv = {
        "parent": "minecraft:husbandry/tame_an_animal",
        "display": {"icon": {"item": icon}, "title": {"translate": key + ".title"},
                    "description": {"translate": key + ".description"}, "frame": frame},
        "criteria": criteria,
        "requirements": [[c] for c in sorted(criteria)],
    }
    if xp:
        adv["rewards"] = {"experience": xp}
    assets.write_json("advancements/husbandry/%s.json" % path, adv)
    for part in ("title", "description"):
        lang.append("%s.%s=%s" % (key, part, assets.name("advancements.husbandry.%s.%s" % (path, part))))

events = {}
for e in ("item.armor.equip_wolf", "item.armor.unequip_wolf", "item.wolf_armor.break", "item.wolf_armor.crack",
          "item.wolf_armor.damage", "item.wolf_armor.repair"):
    events[e] = assets.copy_sound(e)
assets.merge_sounds("wolf", events)
assets.merge_lang("wolf", sorted(set(lang)) + assets.subtitle_lines(events))
print("wolf: %d names, %d sound events" % (len(lang), len(events)))
