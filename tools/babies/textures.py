"""Copies 26.1's baby textures from the 26.3 client jar (G:/MCDownloads/minecraft-26.3) for the baby models.

Run: python -I tools/babies/textures.py   (idempotent)
"""
import os
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
TEXTURES = os.path.join(ROOT, "src", "main", "resources", "assets", "trailsandtalesbackport", "textures")
JAR = zipfile.ZipFile("G:/MCDownloads/minecraft-26.3/client.jar")

# 26.3's path -> ours (the armadillo's textures sit in entity/, not entity/armadillo/, as the stream has them).
COPIES = {
    "entity/camel/camel_baby.png": "entity/camel/camel_baby.png",
    "entity/sniffer/snifflet.png": "entity/sniffer/snifflet.png",
    "entity/armadillo/armadillo_baby.png": "entity/armadillo_baby.png",
}


def main():
    for src, dst in COPIES.items():
        data = JAR.read("assets/minecraft/textures/" + src)
        out = os.path.join(TEXTURES, dst)
        os.makedirs(os.path.dirname(out), exist_ok=True)
        with open(out, "wb") as f:
            f.write(data)
        print("%s (%d bytes)" % (dst, len(data)))


if __name__ == "__main__":
    main()
