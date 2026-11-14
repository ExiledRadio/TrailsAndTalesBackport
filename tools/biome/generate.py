"""Cherry grove stream assets: 1.20.6's music.overworld.cherry_grove.

    python tools/biome/generate.py

Bromeliad and Echo in the Wind are the look and feel stream's files (1.20's menu tracks); Left to Bloom is copied
from 1.20.6's asset index; Clark, Minecraft and Sweden are 1.12.2's own calm2, calm1 and calm3 (checked to be the
same files by hash).
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID

OLD_INDEX = json.load(open(os.path.join(os.path.expanduser("~"), ".gradle", "caches", "minecraft", "assets", "indexes",
                                        "1.12.json"), encoding="utf-8"))["objects"]
# 1.20.5 renamed the three calm tracks after their titles; 1.12.2 has them under their old names.
OLD_NAME = {"music/game/clark": "music/game/calm2", "music/game/minecraft": "music/game/calm1",
            "music/game/sweden": "music/game/calm3"}
OURS = {"music/game/bromeliad", "music/game/echo_in_the_wind"}
COPY = {"music/game/left_to_bloom"}

spec = json.loads(json.dumps(assets.vanilla_sound("music.overworld.cherry_grove")))
for sound in spec["sounds"]:
    path = sound["name"]
    if path in OLD_NAME:
        new_hash = assets.INDEX["minecraft/sounds/%s.ogg" % path]["hash"]
        old_hash = OLD_INDEX["minecraft/sounds/%s.ogg" % OLD_NAME[path]]["hash"]
        assert new_hash == old_hash, path
        sound["name"] = "minecraft:" + OLD_NAME[path]
    elif path in OURS:
        assert os.path.exists(os.path.join(assets.ASSETS, "sounds", path + ".ogg")), path
        sound["name"] = "%s:%s" % (M, path)
    elif path in COPY:
        assets.write("sounds/%s.ogg" % path, assets._object("minecraft/sounds/%s.ogg" % path))
        sound["name"] = "%s:%s" % (M, path)
    else:
        raise SystemExit("unplaced track " + path)

events = {"music.overworld.cherry_grove": spec}
assets.merge_sounds("biome", events)
print("biome: %d sound events, %d tracks" % (len(events), len(spec["sounds"])))
