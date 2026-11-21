"""Game Drops Backport's poplar hanging sign (its request 1): blockstates, models, item and GUI textures, names, and
the hanging sign's entity texture.

26.3 draws hanging signs as block models from a 32x32 block/<wood>_hanging_sign.png; our renderer uses 1.20's 64x32
entity layout (entity/signs/hanging/<wood>.png). The layout change is found from vanilla's own woods: every pixel of
1.21.11's entity texture is matched with the 26.3 block texture pixels that have its color in every wood that both
versions have; where several do, the one that keeps it next to its neighbors wins. Poplar's 26.3 texture is then laid
out by that map.

    python -I tools/hangingsign/poplar.py <1.21.11 client.jar> <26.3 client.jar>
"""
import io
import json
import os
import sys
import zipfile

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "trailsandtalesbackport")
WOODS = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo", "crimson", "warped", "pale_oak"]


def image(jar, path):
    return Image.open(io.BytesIO(jar.read(path))).convert("RGBA")


def layout_map(old, new):
    pairs = [(image(old, "assets/minecraft/textures/entity/signs/hanging/%s.png" % w),
              image(new, "assets/minecraft/textures/block/%s_hanging_sign.png" % w)) for w in WOODS]
    o0, n0 = pairs[0]
    every = [(x, y) for y in range(n0.height) for x in range(n0.width)]
    cand = {}
    for y in range(o0.height):
        for x in range(o0.width):
            if any(o.getpixel((x, y))[3] for o, _ in pairs):
                cand[(x, y)] = [q for q in every if all(n.getpixel(q) == o.getpixel((x, y)) for o, n in pairs)]
    missing = [p for p, s in cand.items() if not s]
    if missing:
        raise SystemExit("no 26.3 pixel matches %d entity pixels, e.g. %s" % (len(missing), missing[:5]))
    chosen = {p: s[0] for p, s in cand.items() if len(s) == 1}
    # Propagate: a pixel takes the candidate its mapped neighbor's offset points at.
    for _ in range(64):
        changed = False
        for p, s in cand.items():
            if p in chosen:
                continue
            for d in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                q = (p[0] + d[0], p[1] + d[1])
                if q in chosen:
                    want = (chosen[q][0] - d[0], chosen[q][1] - d[1])
                    if want in s:
                        chosen[p] = want
                        changed = True
                        break
        if not changed:
            break
    for p, s in cand.items():
        chosen.setdefault(p, s[0])
    return chosen, o0.size


def write(path, data):
    full = os.path.join(ASSETS, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, "wb") as f:
        f.write(data)


def write_json(path, data):
    write(path, (json.dumps(data, indent=2) + "\n").encode("utf-8"))


def png(img):
    buf = io.BytesIO()
    img.save(buf, "PNG")
    return buf.getvalue()


def merge_lang(section, lines):
    """Replaces the block between '# --- <section> ---' and the next '# ---' (or the end) in en_us.lang."""
    path = os.path.join(ASSETS, "lang", "en_us.lang")
    text = open(path, encoding="utf-8").read().rstrip("\n").split("\n")
    head = "# --- %s ---" % section
    out, skipping = [], False
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


def main(old_path, new_path):
    old, new = zipfile.ZipFile(old_path), zipfile.ZipFile(new_path)
    mapping, size = layout_map(old, new)
    src = image(new, "assets/minecraft/textures/block/poplar_hanging_sign.png")
    out = Image.new("RGBA", size, (0, 0, 0, 0))
    for p, q in mapping.items():
        out.putpixel(p, src.getpixel(q))
    write("textures/entity/signs/hanging/poplar.png", png(out))
    # As RGBA: the jar's keyed (tRNS) transparency draws black in 1.12.2.
    write("textures/gui/hanging_signs/poplar.png", png(image(new, "assets/minecraft/textures/gui/hanging_signs/poplar.png")))
    write("textures/item/poplar_hanging_sign.png", png(image(new, "assets/minecraft/textures/item/poplar_hanging_sign.png")))
    # The same files as every other wood's (tools/hangingsign/generate.py), the particle from Game Drops' log.
    for kind in ("poplar_hanging_sign", "poplar_wall_hanging_sign"):
        mangrove = json.load(open(os.path.join(ASSETS, "blockstates", kind.replace("poplar", "mangrove") + ".json"), encoding="utf-8"))
        text = json.dumps(mangrove, indent=2).replace("mangrove", "poplar")
        write("blockstates/%s.json" % kind, (text + "\n").encode("utf-8"))
    write_json("models/block/poplar_hanging_sign.json", {"textures": {"particle": "gamedropsbackport:block/stripped_poplar_log"}})
    write_json("models/item/poplar_hanging_sign.json", {"parent": "item/generated",
                                                        "textures": {"layer0": "trailsandtalesbackport:item/poplar_hanging_sign"}})
    en = json.loads(new.read("assets/minecraft/lang/en_us.json").decode("utf-8"))
    lang = []
    for kind in ("poplar_hanging_sign", "poplar_wall_hanging_sign"):
        lang.append("tile.trailsandtalesbackport.%s.name=%s" % (kind, en["block.minecraft." + kind]))
    lang.append("item.trailsandtalesbackport.poplar_hanging_sign.name=%s" % en["block.minecraft.poplar_hanging_sign"])
    merge_lang("hangingsign-poplar", sorted(lang))
    unique = len(set(mapping.values()))
    print("poplar hanging sign: %d entity pixels from %d block pixels" % (len(mapping), unique))


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
