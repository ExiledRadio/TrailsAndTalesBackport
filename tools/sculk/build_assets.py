"""Generates Trails & Tales Backport's sculk sensor assets from 1.20.6's client jar and asset objects."""
import json
import os
import shutil
import sys
import zipfile

JAR = r"G:/MCDownloads/trails-1.20.6/client.jar"
OBJ = r"G:/MCDownloads/trails-1.20.6/objects"
IDX = r"G:/MCDownloads/trails-1.20.6/assetindex.json"
NS = "trailsandtalesbackport"
OUT = sys.argv[1]  # src/main/resources/assets/trailsandtalesbackport

jar = zipfile.ZipFile(JAR)


def jread(name):
    return json.loads(jar.read(name).decode("utf-8"))


def ns(ref):
    ref = ref.replace("minecraft:", "")
    if ref.startswith("block/") or ref.startswith("item/"):
        return NS + ":" + ref
    return ref


def write(rel, obj):
    p = os.path.join(OUT, rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


# --- models ---------------------------------------------------------------------------------------
for n in ["sculk_sensor", "sculk_sensor_active", "sculk_sensor_inactive", "calibrated_sculk_sensor",
          "calibrated_sculk_sensor_active", "calibrated_sculk_sensor_inactive"]:
    m = jread("assets/minecraft/models/block/%s.json" % n)
    if "parent" in m and m["parent"] not in ("block/block", "minecraft:block/block"):
        m["parent"] = ns(m["parent"] if "/" in m["parent"] else "block/" + m["parent"])
    m["textures"] = {k: ns(v) if not v.startswith("#") else v for k, v in m.get("textures", {}).items()}
    write("models/block/%s.json" % n, m)

for n in ["sculk_sensor", "calibrated_sculk_sensor"]:
    write("models/item/%s.json" % n, {"parent": NS + ":block/%s_inactive" % n})

# --- blockstates (1.12 names a model without "block/") -----------------------------------------------
for n in ["sculk_sensor", "calibrated_sculk_sensor"]:
    b = jread("assets/minecraft/blockstates/%s.json" % n)
    out = {}
    for key, v in b["variants"].items():
        # 1.12 sorts properties by name: facing, sculk_sensor_phase (waterlogged and power are not ours).
        props = dict(p.split("=") for p in key.split(","))
        props.pop("power", None)
        props.pop("waterlogged", None)
        k = ",".join("%s=%s" % (p, props[p]) for p in sorted(props))
        v = dict(v)
        v["model"] = NS + ":" + v["model"].replace("minecraft:", "").replace("block/", "")
        out[k] = v
    write("blockstates/%s.json" % n, {"variants": dict(sorted(out.items()))})

# --- textures -------------------------------------------------------------------------------------
for name in jar.namelist():
    base = os.path.basename(name)
    if name.startswith("assets/minecraft/textures/block/") and ("sculk_sensor" in base):
        p = os.path.join(OUT, "textures/block", base)
        os.makedirs(os.path.dirname(p), exist_ok=True)
        with open(p, "wb") as f:
            f.write(jar.read(name))

# --- sounds -----------------------------------------------------------------------------------------
idx = json.load(open(IDX))["objects"]


def obj(key):
    h = idx[key]["hash"]
    return os.path.join(OBJ, h[:2], h)


sounds_json = json.load(open(obj("minecraft/sounds.json")))
SUBTITLES = {
    "subtitles.block.sculk_sensor.clicking": "subtitles.%s.block.sculk_sensor.clicking" % NS,
    "subtitles.block.sculk_sensor.clicking_stop": "subtitles.%s.block.sculk_sensor.clicking_stop" % NS,
    "subtitles.block.amethyst_block.resonate": "subtitles.%s.block.amethyst_block.resonate" % NS,
}
events = {}
for key in ["block.sculk_sensor.break", "block.sculk_sensor.step", "block.sculk_sensor.place", "block.sculk_sensor.hit",
            "block.sculk_sensor.fall", "block.sculk_sensor.clicking", "block.sculk_sensor.clicking_stop",
            "block.amethyst_block.resonate"]:
    e = json.loads(json.dumps(sounds_json[key]))
    new = []
    for s in e["sounds"]:
        if isinstance(s, str):
            s = {"name": s}
        src = "minecraft/sounds/%s.ogg" % s["name"]
        dst = os.path.join(OUT, "sounds", s["name"] + ".ogg")
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        shutil.copyfile(obj(src), dst)
        s["name"] = NS + ":" + s["name"]
        new.append(s if len(s) > 1 else s["name"])
    e["sounds"] = new
    if "subtitle" in e:
        e["subtitle"] = SUBTITLES.get(e["subtitle"], e["subtitle"])
    events[key] = e

p = os.path.join(OUT, "sounds.json")
with open(p, encoding="utf-8") as f:
    allsounds = json.load(f)
allsounds.update(events)
with open(p, "w", encoding="utf-8", newline="\n") as f:
    json.dump(allsounds, f, indent=2)
    f.write("\n")
print("ok", len(events))
