"""Cherry stream assets: the cherry wood set, leaves, sapling, potted sapling, pink petals, petal particles.

    python tools/cherry/generate.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.insert(0, os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "common"))
import assets  # noqa: E402
import woodset  # noqa: E402

M = assets.MODID
lang, events = woodset.generate("cherry", "cherry_wood")

assets.port_blockstate("cherry_leaves", "cherry_leaves", {"check_decay": woodset.BOOL, "decayable": woodset.BOOL})
assets.port_item_model("cherry_leaves")
assets.port_blockstate("cherry_sapling", "cherry_sapling", {"stage": ["0", "1"]})
assets.port_item_model("cherry_sapling")
assets.port_blockstate("potted_cherry_sapling", "potted_cherry_sapling", {})
assets.port_blockstate("pink_petals", "pink_petals", None)
assets.port_item_model("pink_petals")
for name in ("cherry_leaves", "cherry_sapling", "potted_cherry_sapling", "pink_petals"):
    lang.append("tile.%s.%s.name=%s" % (M, name, assets.name("block.minecraft." + name)))
for i in range(12):
    assets.texture("particle/cherry_%d" % i)

for prefix in ("block.cherry_leaves", "block.cherry_sapling", "block.pink_petals"):
    for part in ("break", "step", "place", "hit", "fall"):
        events[prefix + "." + part] = assets.copy_sound(prefix + "." + part)

assets.merge_sounds("cherry", events)
assets.merge_lang("cherry", sorted(set(lang)) + assets.subtitle_lines(events))
print("cherry: %d names, %d sound events" % (len(lang), len(events)))
