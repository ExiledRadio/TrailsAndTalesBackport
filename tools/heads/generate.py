"""Heads stream assets: the piglin head (blockstate, models, texture, name) and the note block head sounds.

    python tools/heads/generate.py
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID

# Block: drawn by its tile entity renderer; the model only gives the break particles (1.20's block/skull:
# soul sand). One model for every facing.
assets.write_json("models/block/piglin_head.json", {"textures": {"particle": "blocks/soul_sand"}})
assets.write_json("blockstates/piglin_head.json", {
    "variants": {"facing=%s" % f: {"model": "%s:piglin_head" % M} for f in ["up", "north", "south", "west", "east"]}
})

# Item: 1.20's item/template_skull (the same as 1.12.2's item/skull), drawn by the item renderer.
template = assets.jar_json("assets/minecraft/models/item/template_skull.json")
template["textures"] = {"particle": "blocks/soul_sand"}
assets.write_json("models/item/piglin_head.json", template)

assets.texture("entity/piglin/piglin")

lang = ["tile.%s.piglin_head.name=%s" % (M, assets.name("block.minecraft.piglin_head"))]

# 1.20's block.note_block.imitate.* events: references to the mob's own event (1.12.2's names), the piglin's
# with its files, since 1.12.2 has no piglin.
OLD_EVENT = {
    "entity.ender_dragon.ambient": "entity.enderdragon.ambient",
}
OLD_SUBTITLE = {
    "subtitles.entity.ender_dragon.ambient": "subtitles.entity.enderdragon.ambient",
}
events = {}
for mob in ["skeleton", "wither_skeleton", "zombie", "creeper", "ender_dragon", "piglin"]:
    name = "block.note_block.imitate." + mob
    spec = json.loads(json.dumps(assets.VANILLA_SOUNDS[name]))
    if mob == "piglin":
        piglin = assets.copy_sound("entity.piglin.ambient")
        spec = {"sounds": piglin["sounds"], "subtitle": piglin["subtitle"]}
    else:
        for sound in spec["sounds"]:
            sound["name"] = "minecraft:" + OLD_EVENT.get(sound["name"], sound["name"])
        spec["subtitle"] = OLD_SUBTITLE.get(spec["subtitle"], spec["subtitle"])
        assert spec["subtitle"] in assets.OLD_LANG, spec["subtitle"]
    events[name] = spec

assets.merge_sounds("heads", events)
assets.merge_lang("heads", lang + assets.subtitle_lines(events))
print("heads: %d lines, %d sound events" % (len(lang), len(events)))
