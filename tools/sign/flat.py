"""26.2's flat sign pictures (gui/signs/<wood>.png) for this mod's own woods, cherry and bamboo, which the sign
editing screen draws instead of the 1.20 model from 26.2 on (sign.client.SignEditScreen.flatTexture).

    python -I tools/sign/flat.py <26.3 client.jar>
"""
import io
import os
import sys
import zipfile

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "trailsandtalesbackport", "textures", "gui", "signs")

if __name__ == "__main__":
    jar = zipfile.ZipFile(sys.argv[1])
    os.makedirs(OUT, exist_ok=True)
    for wood in ("cherry", "bamboo"):
        # As RGBA: the jar's keyed (tRNS) transparency draws black in 1.12.2.
        Image.open(io.BytesIO(jar.read("assets/minecraft/textures/gui/signs/%s.png" % wood))).convert("RGBA").save(
            os.path.join(OUT, wood + ".png"))
    print("flat signs: cherry, bamboo")
