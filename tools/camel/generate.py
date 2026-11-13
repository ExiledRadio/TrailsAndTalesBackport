"""Camel stream assets: the camel's texture, the jump bar's cooldown sprite, its sounds and its name.

    python tools/camel/generate.py

The model and animations are code (camel/client/ModelCamel.java, and CamelAnimations.java from
tools/camel/animations.py).
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID

assets.texture("entity/camel/camel")
# 1.20.2+ keeps HUD pieces as sprites; 1.12.2 draws this one as a plain 182 x 5 texture.
assets.texture("gui/sprites/hud/jump_bar_cooldown", "gui/jump_bar_cooldown")

lang = ["entity.%s.camel.name=%s" % (M, assets.name("entity.minecraft.camel"))]

events = {}
for e in ("ambient", "dash", "dash_ready", "death", "eat", "hurt", "saddle", "sit", "stand", "step", "step_sand"):
    events["entity.camel." + e] = assets.copy_sound("entity.camel." + e)
assets.merge_sounds("camel", events)
assets.merge_lang("camel", lang + assets.subtitle_lines(events))
print("camel: %d names, %d sound events" % (len(lang), len(events)))
