"""Bamboo stream assets: the bamboo wood set (blocks, mosaic, signs) and its sounds.

    python tools/bamboo/generate.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.insert(0, os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "common"))
import assets  # noqa: E402
import woodset  # noqa: E402

lang, events = woodset.generate("bamboo", "bamboo_wood", bamboo=True)
assets.merge_sounds("bamboo", events)
assets.merge_lang("bamboo", sorted(set(lang)) + assets.subtitle_lines(events))
print("bamboo: %d names, %d sound events" % (len(lang), len(events)))
