"""Copies the owners' classes this stream's copies are built from (family Rule 1: the owner's code, our ids).

    python -I tools/smithing/copy_from_owner.py

  * Caves & Cliffs Backport (E:/MincraftModding/Workspaces/CavesAndCliffsBackport, main): amethyst blocks, the
    buds and cluster, chimes, geodes and the noise geodes are shaped by; BlockBasic.
  * Tricky Trials Backport (E:/MincraftModding/Workspaces/TrickyTrialsBackport, main): its own copy of Caves &
    Cliffs' copper ore and vein generator.

Everything goes into smithing/copy (noise into smithing/copy/noise), with package names, the owner's
content class and its config settings rewritten to ours. Rerun after the owner changes these classes.
"""
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
WS = os.path.dirname(ROOT)
CC = WS + "/CavesAndCliffsBackport/src/main/java/com/exiledradio/cavesandcliffsbackport/"
TR = WS + "/TrickyTrialsBackport/src/main/java/com/exiledradio/trickytrialsbackport/"
OUT = ROOT + "/src/main/java/com/exiledradio/trailsandtalesbackport/smithing/copy/"
PKG = "com.exiledradio.trailsandtalesbackport.smithing.copy"

HEADER = "// Copied from %s by tools/smithing/copy_from_owner.py; edit the owner's file, not this one.\n"

COMMON = [
    ("com.exiledradio.cavesandcliffsbackport.common.BlockBasic", PKG + ".BlockBasic"),
    ("com.exiledradio.cavesandcliffsbackport.terrain.core.DoubleNoise", PKG + ".noise.DoubleNoise"),
    ("com.exiledradio.cavesandcliffsbackport.terrain.core.LegacyRandom", PKG + ".noise.LegacyRandom"),
    ("com.exiledradio.cavesandcliffsbackport.common.Owners", "com.exiledradio.trailsandtalesbackport.common.Owners"),
    ("com.exiledradio.cavesandcliffsbackport.ModConfig", "com.exiledradio.trailsandtalesbackport.ModConfig"),
    ("com.exiledradio.cavesandcliffsbackport.CavesAndCliffsBackport", "com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport"),
    ("com.exiledradio.trickytrialsbackport.ModConfig", "com.exiledradio.trailsandtalesbackport.ModConfig"),
    ("com.exiledradio.trickytrialsbackport.common.Owners", "com.exiledradio.trailsandtalesbackport.common.Owners"),
    ("CavesAndCliffsBackport.MOD_ID", "TrailsAndTalesBackport.MOD_ID"),
    ("AmethystContent", "AmethystCopies"),
    # The owner's settings; ours are in ModConfig.smithing, its numbers are the owner's defaults.
    ("ModConfig.amethyst.geodes", "ModConfig.smithing.geodes"),
    ("ModConfig.amethyst.geodeRarity", "24"),
    ("ModConfig.amethyst.geodeMinY", "6"),
    ("ModConfig.amethyst.geodeMaxY", "46"),
    ("ModConfig.copper.generate", "ModConfig.smithing.copperOre"),
    ("ModConfig.copper.veinsPerChunk", "6"),
    ("ModConfig.copper.veinSize", "10"),
    ("ModConfig.copper.minY", "1"),
    ("ModConfig.copper.maxY", "96"),
    # Its 1.18 terrain places its own geodes; without the owner there is none.
    ("com.exiledradio.cavesandcliffsbackport.terrain.ModernTerrain.owns(world)", "false"),
]


def strip_methods(text, word):
    """Removes every method (with its javadoc) whose signature line mentions `word`."""
    lines = text.split("\n")
    out = []
    i = 0
    while i < len(lines):
        line = lines[i]
        if word in line and re.match(r"\s+(public|private|static|protected).*\(.*", line) and "{" in line:
            # drop the javadoc/comment block just written
            while out and (out[-1].strip().startswith(("*", "/**", "//")) or out[-1].strip() == "*/"):
                out.pop()
            depth = line.count("{") - line.count("}")
            i += 1
            while depth > 0 and i < len(lines):
                depth += lines[i].count("{") - lines[i].count("}")
                i += 1
            if i < len(lines) and lines[i].strip() == "" and out and out[-1].strip() == "":
                i += 1
            continue
        out.append(line)
        i += 1
    return "\n".join(out)


def copy(src, dst, package, extra=()):
    text = open(src, encoding="utf-8").read().replace("\r\n", "\n")
    text = re.sub(r"^package [\w.]+;", "package %s;" % package, text, count=1, flags=re.M)
    for a, b in list(COMMON) + list(extra):
        text = text.replace(a, b)
    # Imports of classes now in the same package.
    text = re.sub(r"^import %s\.(\w+);\n" % re.escape(package), "", text, flags=re.M)
    rel = os.path.relpath(src, WS).replace("\\", "/")
    text = HEADER % rel + text
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    with open(dst, "w", encoding="utf-8", newline="\n") as f:
        f.write(text)
    return text


def main():
    copy(CC + "common/BlockBasic.java", OUT + "BlockBasic.java", PKG)
    copy(CC + "amethyst/BlockAmethyst.java", OUT + "BlockAmethyst.java", PKG)
    copy(CC + "amethyst/BlockAmethystCluster.java", OUT + "BlockAmethystCluster.java", PKG)
    copy(CC + "amethyst/AmethystEvents.java", OUT + "AmethystEvents.java", PKG,
         [("AmethystCopies.enabled()", "AmethystCopies.registering()")])
    copy(CC + "amethyst/GeodeGenerator.java", OUT + "GeodeGenerator.java", PKG)
    for name in ["LegacyRandom", "ImprovedNoise", "OctaveNoise", "DoubleNoise"]:
        path = OUT + "noise/%s.java" % name
        text = copy(CC + "terrain/core/%s.java" % name, path, PKG + ".noise")
        text = strip_methods(text, "Xoroshiro")
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            f.write(text)
    copy(TR + "copper/BlockCopperOre.java", OUT + "BlockCopperOre.java", PKG)
    copy(TR + "copper/CopperOreGenerator.java", OUT + "CopperOreGenerator.java", PKG)
    print("copied into", OUT)


if __name__ == "__main__":
    main()
