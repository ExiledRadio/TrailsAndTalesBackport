"""Writes assets/trailsandtalesbackport/migration/futuremc_states.json: what each metadata value of the other mod's
blocks that this mod also has (its copies of other family mods' blocks) meant, in the format of Backport Core's
recorded state tables (`/futuremc dump`).

Every entry is the owner's recorded one, as written by `/futuremc dump` on a real server with Future MC 0.2.21:
  * stripped logs (`axis` y 0, x 4, z 8, none 12) and stripped wood (y 0, x 4, z 8), `bamboo` (`leaves`, `thick`,
    `mature` in meta = mature * 6 + thick * 3 + leaves) and `smithing_table` (`facing` 2-5): Village & Pillage's
    migration/futuremc_states.json;
  * `chain` (no properties: the other mod's chain is vertical only): Nether Update's.
This mod's blocks are copies of the owners' (same states, same metadata), so the owners' rules apply unchanged.

    python -I tools/futuremc/states.py
"""
import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "trailsandtalesbackport", "migration",
                   "futuremc_states.json")

WOODS = ("oak", "spruce", "birch", "jungle", "acacia", "dark_oak")
LEAVES = ("none", "small", "large")

states = {}
for wood in WOODS:
    states["stripped_%s_log" % wood] = {"0": {"axis": "y"}, "4": {"axis": "x"}, "8": {"axis": "z"},
                                        "12": {"axis": "none"}}
    states["stripped_%s_wood" % wood] = {"0": {"axis": "y"}, "4": {"axis": "x"}, "8": {"axis": "z"}}
states["bamboo"] = {}
for mature in (0, 1):
    for thick in (0, 1):
        for leaves in range(3):
            states["bamboo"][str(mature * 6 + thick * 3 + leaves)] = {
                "leaves": LEAVES[leaves], "mature": "true" if mature else "false", "thick": "true" if thick else "false"}
states["chain"] = {"0": {}}
states["smithing_table"] = {str(meta): {"facing": facing}
                            for meta, facing in ((2, "north"), (3, "south"), (4, "west"), (5, "east"))}

os.makedirs(os.path.dirname(OUT), exist_ok=True)
with open(OUT, "w", encoding="utf-8", newline="\n") as f:
    f.write(json.dumps(states, indent=2) + "\n")
print("futuremc_states.json: %d blocks" % len(states))
