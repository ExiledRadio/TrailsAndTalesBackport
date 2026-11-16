"""Smithing stream assets: templates, armor trims, the smithing table and its 1.20 screen, the copper and
amethyst copies, the two trim advancements, names and sounds.

    python -I tools/smithing/generate.py

From 1.20.6's client jar (through tools/assets.py), except the copies' blockstates, models and textures,
which are their owners' (Caves & Cliffs' amethyst, Tricky Trials' copy of Caves & Cliffs' copper), with
the owner's namespace changed to ours.
"""
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID
WS = os.path.dirname(assets.ROOT)
CC = WS + "/CavesAndCliffsBackport/src/main/resources/assets/cavesandcliffsbackport/"
TR = WS + "/TrickyTrialsBackport/src/main/resources/assets/trickytrialsbackport/"

PATTERNS = ["coast", "dune", "eye", "host", "raiser", "rib", "sentry", "shaper", "silence", "snout", "spire",
            "tide", "vex", "ward", "wayfinder", "wild"]
MATERIALS = ["quartz", "iron", "netherite", "redstone", "copper", "gold", "emerald", "diamond", "lapis", "amethyst"]
PALETTES = MATERIALS + ["iron_darker", "gold_darker", "diamond_darker", "netherite_darker"]

lang = []

# --- Templates: items, names (all "Smithing Template"), tooltip text ---------------------------------------
templates = ["netherite_upgrade_smithing_template"] + ["%s_armor_trim_smithing_template" % p for p in PATTERNS]
for t in templates:
    assets.port_item_model(t)
    lang.append("item.%s.%s.name=%s" % (M, t, assets.name("item.minecraft." + t)))
lang.append("item.%s.smithing_template.name=%s" % (M, assets.name("item.minecraft.smithing_template")))
for key in ["item.minecraft.smithing_template.applies_to", "item.minecraft.smithing_template.ingredients",
            "item.minecraft.smithing_template.armor_trim.applies_to",
            "item.minecraft.smithing_template.armor_trim.ingredients",
            "item.minecraft.smithing_template.armor_trim.base_slot_description",
            "item.minecraft.smithing_template.armor_trim.additions_slot_description",
            "item.minecraft.smithing_template.netherite_upgrade.applies_to",
            "item.minecraft.smithing_template.netherite_upgrade.ingredients",
            "item.minecraft.smithing_template.netherite_upgrade.base_slot_description",
            "item.minecraft.smithing_template.netherite_upgrade.additions_slot_description",
            "upgrade.minecraft.netherite_upgrade",
            "container.upgrade", "container.upgrade.error_tooltip", "container.upgrade.missing_template_tooltip"]:
    lang.append("%s=%s" % (key, assets.name(key)))

# --- Trims: patterns (grayscale), palettes, names ------------------------------------------------------------
for p in PATTERNS:
    for suffix in ["", "_leggings"]:
        assets.write("textures/trims/models/armor/%s%s.png" % (p, suffix),
                     assets.jar_bytes("assets/minecraft/textures/trims/models/armor/%s%s.png" % (p, suffix)))
    lang.append("trim_pattern.%s.%s=%s" % (M, p, assets.name("trim_pattern.minecraft." + p)))
for c in PALETTES:
    assets.write("textures/trims/color_palettes/%s.png" % c,
                 assets.jar_bytes("assets/minecraft/textures/trims/color_palettes/%s.png" % c))
for m in MATERIALS:
    lang.append("trim_material.%s.%s=%s" % (M, m, assets.name("trim_material.minecraft." + m)))

# --- The smithing table copy and the 1.20 screen ----------------------------------------------------------
assets.port_blockstate("smithing_table", "smithing_table", {})
assets.port_item_model("smithing_table")
lang.append("tile.%s.smithing_table.name=%s" % (M, assets.name("block.minecraft.smithing_table")))
assets.write("textures/gui/container/smithing.png", assets.jar_bytes("assets/minecraft/textures/gui/container/smithing.png"))
assets.write("textures/gui/container/smithing_error.png",
             assets.jar_bytes("assets/minecraft/textures/gui/sprites/container/smithing/error.png"))
for icon in ["smithing_template_armor_trim", "smithing_template_netherite_upgrade", "sword", "pickaxe", "axe", "hoe",
             "shovel", "ingot", "redstone_dust", "lapis_lazuli", "quartz", "diamond", "emerald", "amethyst_shard"]:
    assets.write("textures/items/empty_slot_%s.png" % icon,
                 assets.jar_bytes("assets/minecraft/textures/item/empty_slot_%s.png" % icon))

events = {"block.smithing_table.use": assets.copy_sound("block.smithing_table.use")}

# --- Advancements (criteria granted by code: 1.12.2 has no recipe_crafted trigger) ----------------------
lang += assets.advancement("adventure/trim_with_any_armor_pattern", "%s:dune_armor_trim_smithing_template" % M,
                           {"trimmed": {"trigger": "minecraft:impossible"}}, "adventure.trim_with_any_armor_pattern")
EXCLUSIVE = ["spire", "snout", "rib", "ward", "silence", "vex", "tide", "wayfinder"]
ours = "advancements.%s.adventure.trim_with_all_exclusive_armor_patterns" % M
criteria = {"%s_armor_trim_smithing_template" % p: {"trigger": "minecraft:impossible"} for p in EXCLUSIVE}
assets.write_json("advancements/adventure/trim_with_all_exclusive_armor_patterns.json", {
    "parent": "%s:adventure/trim_with_any_armor_pattern" % M,
    "display": {"icon": {"item": "%s:silence_armor_trim_smithing_template" % M},
                "title": {"translate": ours + ".title"}, "description": {"translate": ours + ".description"},
                "frame": "challenge"},
    "criteria": criteria,
    "requirements": [[c] for c in sorted(criteria)],
    "rewards": {"experience": 150},
})
for part in ["title", "description"]:
    lang.append("%s.%s=%s" % (ours, part, assets.name("advancements.adventure.trim_with_all_exclusive_armor_patterns." + part)))


# --- Copies: the owners' blockstates, models and textures under our namespace -------------------------------
def owner_asset(owner_root, owner_ns, rel):
    """Copies one of the owner's files (json rewritten to our namespace), and every texture a model names."""
    src = owner_root + rel
    if rel.endswith(".json"):
        text = open(src, encoding="utf-8").read().replace(owner_ns + ":", M + ":")
        assets.write(rel, text)
        data = json.loads(text)
        for tex in (data.get("textures") or {}).values():
            if tex.startswith(M + ":"):
                path = tex.split(":", 1)[1]
                owner_asset(owner_root, owner_ns, "textures/%s.png" % path)
        parent = data.get("parent", "")
        if parent.startswith(M + ":"):
            owner_asset(owner_root, owner_ns, "models/%s.json" % parent.split(":", 1)[1])
        for variant in (data.get("variants") or {}).values():
            for v in variant if isinstance(variant, list) else [variant]:
                model = v.get("model", "")
                if model.startswith(M + ":"):
                    owner_asset(owner_root, owner_ns, "models/block/%s.json" % model.split(":", 1)[1])
    else:
        with open(src, "rb") as f:
            assets.write(rel, f.read())
        if os.path.exists(src + ".mcmeta"):
            with open(src + ".mcmeta", "rb") as f:
                assets.write(rel + ".mcmeta", f.read())


AMETHYST = ["amethyst_block", "budding_amethyst", "small_amethyst_bud", "medium_amethyst_bud", "large_amethyst_bud",
            "amethyst_cluster", "calcite", "smooth_basalt"]
for b in AMETHYST:
    owner_asset(CC, "cavesandcliffsbackport", "blockstates/%s.json" % b)
    owner_asset(CC, "cavesandcliffsbackport", "models/item/%s.json" % b)
    lang.append("tile.%s.%s.name=%s" % (M, b, assets.name("block.minecraft." + b)))
owner_asset(CC, "cavesandcliffsbackport", "models/item/amethyst_shard.json")
lang.append("item.%s.amethyst_shard.name=%s" % (M, assets.name("item.minecraft.amethyst_shard")))

owner_asset(TR, "trickytrialsbackport", "blockstates/copper_ore.json")
for i in ["copper_ore", "copper_ingot", "raw_copper"]:
    owner_asset(TR, "trickytrialsbackport", "models/item/%s.json" % i)
lang.append("tile.%s.copper_ore.name=%s" % (M, assets.name("block.minecraft.copper_ore")))
for i in ["copper_ingot", "raw_copper"]:
    lang.append("item.%s.%s.name=%s" % (M, i, assets.name("item.minecraft." + i)))

for group in ["block.amethyst_block", "block.amethyst_cluster", "block.calcite", "block.basalt"]:
    for part in ["break", "step", "place", "hit", "fall"]:
        events[group + "." + part] = assets.copy_sound(group + "." + part)
events["block.amethyst_block.chime"] = assets.copy_sound("block.amethyst_block.chime")
for size in ["small", "medium", "large"]:
    for part in ["break", "place"]:
        e = "block.%s_amethyst_bud.%s" % (size, part)
        events[e] = assets.copy_sound(e)

assets.merge_sounds("smithing", events)
assets.merge_lang("smithing", sorted(set(lang)) + assets.subtitle_lines(events))
print("smithing: %d lines, %d sound events" % (len(lang), len(events)))
