"""Signs stream assets: the edit screens' titles and 1.20's sign interaction sounds.

    python tools/sign/generate.py

Every sound here is one 1.12.2 lacks, so each is ours, with its files: waxed sign interact fail (1.20),
dye use, ink sac use, glow ink sac use and honeycomb wax on (1.17).
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID

lang = [
    "gui.%s.sign.edit=%s" % (M, assets.name("sign.edit")),
    "gui.%s.hanging_sign.edit=%s" % (M, assets.name("hanging_sign.edit")),
]

events = {}
for e in ["block.sign.waxed_interact_fail", "item.dye.use", "item.ink_sac.use", "item.glow_ink_sac.use",
          "item.honeycomb.wax_on"]:
    events[e] = assets.copy_sound(e)
assets.merge_sounds("sign", events)
assets.merge_lang("sign", lang + assets.subtitle_lines(events))
print("sign: %d lines, %d sound events" % (len(lang), len(events)))
