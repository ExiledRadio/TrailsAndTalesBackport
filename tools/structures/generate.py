"""Stream St data: 1.20.1's trail ruins files, copied unchanged into this mod's assets.

    python -I tools/structures/generate.py [path to the 1.20.1 client jar]

Copies from the jar's data/minecraft/:
  structures/trail_ruins/**.nbt                      the 89 templates
  worldgen/template_pool/trail_ruins/**.json         the 7 pools
  worldgen/processor_list/trail_ruins_*.json         the 3 archaeology processor lists
  worldgen/structure/trail_ruins.json                start pool, size, start height, projection, distance, bury
  worldgen/structure_set/trail_ruins.json            the random_spread grid
  tags/worldgen/biome/has_structure/trail_ruins.json the biomes
  tags/blocks/trail_ruins_replaceable.json           what the archaeology rules turn into suspicious gravel
into src/main/resources/assets/trailsandtalesbackport/ at the same paths. Backport Core reads the pools, lists and
templates from there under their minecraft: names (the mod is a JigsawPools source); the structure code reads the
rest. 1.20.1, not 1.20.6, because Chunkbase's "1.20" and Caves & Cliffs' 1.20 terrain rules are 1.20.1's.

Then the music disc Relic (trail ruins' rare loot) from the family's 1.20.6 jar and asset index (tools/assets.py):
its texture, item model, sound (music_disc.relic, streamed) and names.
"""
import os
import sys
import zipfile

DEFAULT_JAR = r"G:\MCDownloads\minecraft-1.20.1\client.jar"
ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "trailsandtalesbackport")


def wanted(name):
    if not name.startswith("data/minecraft/"):
        return False
    rel = name[len("data/minecraft/"):]
    return (rel.startswith("structures/trail_ruins/") and rel.endswith(".nbt")) \
        or (rel.startswith("worldgen/template_pool/trail_ruins/") and rel.endswith(".json")) \
        or (rel.startswith("worldgen/processor_list/trail_ruins_") and rel.endswith(".json")) \
        or rel in ("worldgen/structure/trail_ruins.json", "worldgen/structure_set/trail_ruins.json",
                   "tags/worldgen/biome/has_structure/trail_ruins.json", "tags/blocks/trail_ruins_replaceable.json")


def main():
    jar = zipfile.ZipFile(sys.argv[1] if len(sys.argv) > 1 else DEFAULT_JAR)
    n = 0
    for name in sorted(jar.namelist()):
        if not wanted(name):
            continue
        dst = os.path.join(OUT, *name[len("data/minecraft/"):].split("/"))
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        with open(dst, "wb") as f:
            f.write(jar.read(name))
        n += 1
    print("copied", n, "files")
    relic()


def relic():
    sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    import assets  # noqa: E402
    m = assets.MODID
    assets.texture("item/music_disc_relic")
    assets.write_json("models/item/music_disc_relic.json",
                      {"parent": "item/generated", "textures": {"layer0": "%s:item/music_disc_relic" % m}})
    events = {"music_disc.relic": assets.copy_sound("music_disc.relic")}
    assets.merge_sounds("structures", events)
    # 1.12.2's ItemRecord reads "item.record.<name>.desc"; the item's name is vanilla's own "item.record.name",
    # repeated here in the same words so the asset check (which reads only this mod's file) finds it.
    lang = ["item.record.relic.desc=%s" % assets.name("item.minecraft.music_disc_relic.desc"),
            "item.record.name=%s" % assets.name("item.minecraft.music_disc_relic")]
    assets.merge_lang("structures", lang + assets.subtitle_lines(events))
    print("relic:", events)


if __name__ == "__main__":
    main()
