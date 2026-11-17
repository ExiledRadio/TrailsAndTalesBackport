"""Mechanics stream assets: 1.20.3's bat texture, and the sounds of 1.20.2's sponges, 1.20.5's cobwebs and
1.20.3's ender pearl landing (entity.player.teleport).

    python tools/mechanics/generate.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

assets.texture("entity/bat")

events = {}
for block, kinds in (("sponge", ("break", "step", "place", "hit", "fall", "absorb")),
                     ("wet_sponge", ("break", "step", "place", "hit", "fall")),
                     ("cobweb", ("break", "step", "place", "hit", "fall"))):
    for kind in kinds:
        event = "block.%s.%s" % (block, kind)
        events[event] = assets.copy_sound(event)
events["entity.player.teleport"] = assets.copy_sound("entity.player.teleport")
assets.merge_sounds("mechanics", events)
assets.merge_lang("mechanics", assets.subtitle_lines(events))
print("mechanics: %d sound events" % len(events))
