"""Shared helpers for the streams' asset generators: textures, sounds and names from modern Minecraft.

Sources: Minecraft 1.20.6, the last 1.20 release, which has everything 1.20.x added
(G:/MCDownloads/trails-1.20.6, from Mojang's piston-meta):
  * textures, models and en_us.json: the 1.20.6 client jar;
  * sounds and sounds.json: its asset index (16); objects are fetched once from Mojang's resource
    server into the objects/ folder beside it.

Every stream's generator (tools/<stream>/generate.py) imports this module, copies what it needs into
src/main/resources/assets/trailsandtalesbackport/, and merges its sound events and language lines into the
shared sounds.json and en_us.lang between markers of its own, so generators never overwrite each other.
"""
import hashlib
import json
import os
import shutil
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODID = "trailsandtalesbackport"
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", MODID)
SOURCE = "G:/MCDownloads/trails-1.20.6"
JAR = zipfile.ZipFile(SOURCE + "/client.jar")
INDEX = json.load(open(SOURCE + "/assetindex.json", encoding="utf-8"))["objects"]


def _object(path):
    """@return the bytes of an asset-index object such as 'minecraft/sounds.json', downloaded once."""
    h = INDEX[path]["hash"]
    local = "%s/objects/%s/%s" % (SOURCE, h[:2], h)
    if not os.path.exists(local):
        import urllib.request
        data = urllib.request.urlopen("https://resources.download.minecraft.net/%s/%s" % (h[:2], h), timeout=60).read()
        if hashlib.sha1(data).hexdigest() != h:
            raise IOError("hash mismatch for " + path)
        os.makedirs(os.path.dirname(local), exist_ok=True)
        with open(local, "wb") as f:
            f.write(data)
    with open(local, "rb") as f:
        return f.read()


VANILLA_SOUNDS = json.loads(_object("minecraft/sounds.json").decode("utf-8"))
# English is the one language shipped inside the client jar rather than the asset index.
VANILLA_LANG = json.loads(JAR.read("assets/minecraft/lang/en_us.json").decode("utf-8"))


def write(rel, data):
    """Writes bytes or text to a path under our assets folder, creating directories."""
    full = os.path.join(ASSETS, *rel.split("/"))
    os.makedirs(os.path.dirname(full), exist_ok=True)
    mode = "wb" if isinstance(data, bytes) else "w"
    with open(full, mode, **({} if mode == "wb" else {"encoding": "utf-8", "newline": "\n"})) as f:
        f.write(data)


def write_json(rel, obj):
    write(rel, json.dumps(obj, indent=2) + "\n")


def jar_bytes(path):
    return JAR.read(path)


def jar_json(path):
    return json.loads(JAR.read(path).decode("utf-8"))


def texture(src, dst=None):
    """Copies textures/<src>.png (and its .mcmeta, for animated ones) from the 1.20.6 jar to ours.

    @param src like 'block/kelp'; @param dst our path, default the same
    @return our texture name, 'trailsandtalesbackport:<dst>'
    """
    dst = dst or src
    write("textures/%s.png" % dst, jar_bytes("assets/minecraft/textures/%s.png" % src))
    meta = "assets/minecraft/textures/%s.png.mcmeta" % src
    if meta in JAR.namelist():
        write("textures/%s.png.mcmeta" % dst, jar_bytes(meta))
    return "%s:%s" % (MODID, dst)


def vanilla_sound(event):
    """@return 1.20.6's sounds.json entry for an event such as 'block.wet_grass.break'."""
    return VANILLA_SOUNDS[event]


def copy_sound(event, our_event=None):
    """Copies an event's .ogg files into our sounds/ and returns our sounds.json entry for it.

    Files keep their vanilla paths under our namespace; event references stay as they are.
    """
    spec = json.loads(json.dumps(VANILLA_SOUNDS[event]))
    out = []
    for sound in spec["sounds"]:
        entry = dict(sound) if isinstance(sound, dict) else {"name": sound}
        if entry.get("type") == "event":
            out.append(entry)
            continue
        name = entry["name"]
        ns, _, path = name.rpartition(":")
        data = _object("minecraft/sounds/%s.ogg" % path)
        write("sounds/%s.ogg" % path, data)
        entry["name"] = "%s:%s" % (MODID, path)
        out.append(entry if len(entry) > 1 else entry["name"])
    spec["sounds"] = out
    if "subtitle" in spec and spec["subtitle"] not in OLD_LANG:
        # A subtitle 1.12.2 lacks gets our own key; subtitle_lines() gives its English line.
        spec["subtitle"] = spec["subtitle"].replace("subtitles.", "subtitles.%s." % MODID, 1)
    return spec


def subtitle_lines(specs):
    """@return the en_us.lang lines for every subtitle of ours in these sounds.json entries."""
    lines = []
    for spec in specs.values():
        key = spec.get("subtitle")
        if key and key.startswith("subtitles.%s." % MODID):
            lines.append("%s=%s" % (key, VANILLA_LANG[key.replace("%s." % MODID, "", 1)]))
    return sorted(set(lines))


OLD_JAR = zipfile.ZipFile(os.path.join(os.path.expanduser("~"), ".gradle", "caches", "minecraft", "net", "minecraft",
                                       "minecraft", "1.12.2", "minecraft-1.12.2.jar"))
OLD_NAMES = set(OLD_JAR.namelist())
OLD_LANG = dict(l.split("=", 1) for l in OLD_JAR.read("assets/minecraft/lang/en_us.lang").decode("utf-8").splitlines()
                if "=" in l and not l.startswith("#"))

# Modern texture folders and their 1.12.2 names, for parents and textures that 1.12.2 already has.
_OLD_TEXTURE_DIRS = {"block/": "blocks/", "item/": "items/"}


def port_model(path, ours=None, textures_from_jar=True):
    """Ports models/<path>.json from the 1.20.6 jar into our namespace, ready for 1.12.2.

    * Textures named 'minecraft:block/x' are copied to ours and renamed 'trailsandtalesbackport:block/x'.
    * A parent 1.12.2 also has (block/cube_all, block/cross, item/generated, ...) is kept as vanilla's;
      any other vanilla parent is ported too, recursively.
    @return our model name, 'trailsandtalesbackport:<ours or path>'
    """
    ours = ours or path
    data = jar_json("assets/minecraft/models/%s.json" % path)
    parent = data.get("parent")
    if parent:
        pns, _, ppath = parent.rpartition(":")
        if (pns or "minecraft") == "minecraft":
            if ppath.startswith("builtin/") or ("assets/minecraft/models/%s.json" % ppath) in OLD_NAMES:
                data["parent"] = ppath
            else:
                data["parent"] = port_model(ppath)
    for key, value in list(data.get("textures", {}).items()):
        if value.startswith("#"):
            continue
        tns, _, tpath = value.rpartition(":")
        if (tns or "minecraft") == "minecraft" and textures_from_jar:
            data["textures"][key] = texture(tpath)
    write_json("models/%s.json" % ours, data)
    return "%s:%s" % (MODID, ours)


def name(key):
    """@return 1.20.6's English name for a translation key such as 'block.minecraft.kelp'."""
    return VANILLA_LANG[key]


def merge_sounds(section, events):
    """Replaces this generator's events in sounds.json (keys it wrote before are dropped first)."""
    path = os.path.join(ASSETS, "sounds.json")
    data = json.load(open(path, encoding="utf-8")) if os.path.exists(path) else {}
    marker = os.path.join(ROOT, "tools", section, ".sounds")
    if os.path.exists(marker):
        for old in open(marker, encoding="utf-8").read().split():
            data.pop(old, None)
    data.update(events)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(json.dumps(data, indent=2) + "\n")
    with open(marker, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(sorted(events)) + "\n")


def merge_lang(section, lines):
    """Replaces the block between '# --- <section> ---' and the next '# ---' (or the end) in en_us.lang."""
    path = os.path.join(ASSETS, "lang", "en_us.lang")
    text = open(path, encoding="utf-8").read().rstrip("\n").split("\n")
    head = "# --- %s ---" % section
    out = []
    skipping = False
    for line in text:
        if line == head:
            skipping = True
            continue
        if skipping and line.startswith("# --- "):
            skipping = False
        if not skipping:
            out.append(line)
    while out and out[-1] == "":
        out.pop()
    out += ["", head] + lines
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(out) + "\n")


def item_generated(name, texture_name):
    """Writes models/item/<name>.json as a flat item with one texture."""
    write_json("models/item/%s.json" % name, {"parent": "item/generated", "textures": {"layer0": texture_name}})


def item_from_block(name, model=None):
    """Writes models/item/<name>.json that shows the block model."""
    write_json("models/item/%s.json" % name, {"parent": "%s:block/%s" % (MODID, model or name)})


def bs_model(model):
    """'trailsandtalesbackport:block/x' -> 'trailsandtalesbackport:x': 1.12.2 blockstates add the block/ folder."""
    return model.replace(":block/", ":")


def _port_variant(v):
    """A 1.20 variant entry (dict, or a weighted list of them) with its model ported to ours."""
    if isinstance(v, list):
        return [_port_variant(x) for x in v]
    out = dict(v)
    ns, _, path = out["model"].rpartition(":")
    out["model"] = bs_model(port_model(path))
    return out


def states(props):
    """Every combination of a block's 1.12.2 properties: props is {name: [values]} -> list of dicts."""
    combos = [{}]
    for name in sorted(props):
        combos = [dict(c, **{name: v}) for c in combos for v in props[name]]
    return combos


def variant_string(state):
    """1.12.2's default state mapper string: properties sorted by name; 'normal' when there are none."""
    if not state:
        return "normal"
    return ",".join("%s=%s" % (k, state[k]) for k in sorted(state))


def port_blockstate(vanilla, ours, props, convert=None, extra_models=()):
    """Writes blockstates/<ours>.json from 1.20.6's blockstates/<vanilla>.json, with its models ported.

    * "variants" files: one 1.12.2 variant per state of ours (props, as for states()); convert(state)
      maps our state to 1.20's property names and values, and the 1.20 variant whose properties are all
      in it is used (1.20 keys name a subset; "" matches anything).
    * "multipart" files are kept as they are (their conditions must use our names; convert is ignored).
    @return the 1.20 data, for generators that want to look at it
    """
    data = jar_json("assets/minecraft/blockstates/%s.json" % vanilla)
    if "multipart" in data:
        parts = []
        for part in data["multipart"]:
            p = dict(part)
            p["apply"] = _port_variant(part["apply"])
            parts.append(p)
        write_json("blockstates/%s.json" % ours, {"multipart": parts})
        return data
    table = []
    for key, v in data["variants"].items():
        want = dict(kv.split("=", 1) for kv in key.split(",")) if key else {}
        table.append((want, v))
    out = {}
    for state in states(props):
        modern = convert(dict(state)) if convert else state
        modern = {k: str(v) for k, v in modern.items()}
        for want, v in table:
            if all(modern.get(k) == val for k, val in want.items()):
                out[variant_string(state)] = _port_variant(v)
                break
        else:
            raise KeyError("%s: no 1.20 variant for %s (as %s)" % (vanilla, state, modern))
    write_json("blockstates/%s.json" % ours, {"variants": out})
    return data


def port_item_model(vanilla, ours=None):
    """Ports models/item/<vanilla>.json (with its parents and textures) as our item model."""
    return port_model("item/" + vanilla, "item/" + (ours or vanilla))


def advancement(path, icon, criteria, vanilla_key, parent="minecraft:adventure/root", frame="task"):
    """Writes advancements/<path>.json with 1.20's title and description keys under our namespace.

    @param vanilla_key 1.20's key stem, e.g. 'adventure.read_power_from_chiseled_bookshelf'
    @param criteria {name: trigger dict}; 'minecraft:impossible' ones are granted by code
    @return the two en_us.lang lines
    """
    ours = "advancements.%s.%s" % (MODID, vanilla_key)
    write_json("advancements/%s.json" % path, {
        "parent": parent,
        "display": {"icon": {"item": icon}, "title": {"translate": ours + ".title"},
                    "description": {"translate": ours + ".description"}, "frame": frame},
        "criteria": criteria,
    })
    return ["%s.title=%s" % (ours, VANILLA_LANG["advancements.%s.title" % vanilla_key]),
            "%s.description=%s" % (ours, VANILLA_LANG["advancements.%s.description" % vanilla_key])]
