"""Copies 26.1's baby textures from the 26.3 client jar (G:/MCDownloads/minecraft-26.3) for the baby models.

Run: python -I tools/babies/textures.py   (idempotent)
"""
import io
import os
import zipfile

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
TEXTURES = os.path.join(ROOT, "src", "main", "resources", "assets", "trailsandtalesbackport", "textures")
JAR = zipfile.ZipFile("G:/MCDownloads/minecraft-26.3/client.jar")

# 26.3's path -> ours (the armadillo's textures sit in entity/, not entity/armadillo/, as the stream has them).
COPIES = {
    "entity/camel/camel_baby.png": "entity/camel/camel_baby.png",
    "entity/sniffer/snifflet.png": "entity/sniffer/snifflet.png",
    "entity/armadillo/armadillo_baby.png": "entity/armadillo_baby.png",
    "entity/wolf/wolf_collar_baby.png": "entity/wolf/wolf_collar_baby.png",
}
# Every wolf variant's puppy, pale's ("wolf") too: 1.12.2 has no baby textures to fall back on.
for _variant in ("wolf", "wolf_ashen", "wolf_black", "wolf_chestnut", "wolf_rusty", "wolf_snowy", "wolf_spotted",
                 "wolf_striped", "wolf_woods"):
    for _state in ("", "_tame", "_angry"):
        _name = "entity/wolf/%s%s_baby.png" % (_variant, _state)
        COPIES[_name] = _name


def main():
    for src, dst in COPIES.items():
        data = JAR.read("assets/minecraft/textures/" + src)
        out = os.path.join(TEXTURES, dst)
        os.makedirs(os.path.dirname(out), exist_ok=True)
        image = Image.open(io.BytesIO(data))
        if image.mode not in ("P", "RGBA"):
            # 1.12.2's ImageIO reads a gray or RGB PNG's tRNS transparency as opaque (26.3's wolf_collar_baby is gray
            # with tRNS): saved as RGBA instead. Palette PNGs keep their tRNS and are copied as they are.
            buf = io.BytesIO()
            image.convert("RGBA").save(buf, "PNG", optimize=True)
            data = buf.getvalue()
        with open(out, "wb") as f:
            f.write(data)
        print("%s (%d bytes)" % (dst, len(data)))


if __name__ == "__main__":
    main()
