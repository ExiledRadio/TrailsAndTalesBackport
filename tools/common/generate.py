"""Language lines shared by every stream: the creative tab and every config category and field name.

Names are read from ModConfig.java's @Config.LangKey annotations, so a stream's new config field gets
its name line here without editing this file; give the field's English name in NAMES below if the
field name alone does not read well. The tooltips (<langKey>.tooltip) come from the @Config comments
through the family's language tool (tools/lang/build_langs.py, `config` step).

    python tools/common/generate.py
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

CONFIG = os.path.join(assets.ROOT, "src", "main", "java", "com", "exiledradio", assets.MODID, "ModConfig.java")

# English names for keys whose last word does not read well on its own.
NAMES = {
    "cherry": "Cherry Wood",
    "bamboo": "Bamboo Wood",
    "boats": "Boats and Rafts",
    "cherryGrove": "Cherry Grove",
    "hangingSigns": "Hanging Signs",
    "signs": "Signs",
    "chiseledBookshelf": "Chiseled Bookshelf",
    "archaeology": "Archaeology",
    "structures": "Structures",
    "sniffer": "Sniffer",
    "camels": "Camels",
    "smithing": "Smithing Templates and Armor Trims",
    "armadillos": "Armadillos",
    "wolves": "Wolves",
    "calibratedSculkSensor": "Calibrated Sculk Sensor",
    "heads": "Mob Heads",
    "misc": "Other Changes",
    "torchflowerLight": "Glowing Torchflowers",
    "enabled": "Enabled",
}


def words(name):
    """'axeStripping' -> 'Axe stripping'."""
    spaced = re.sub(r"(?<=[a-z0-9])([A-Z])", r" \1", name).lower()
    return spaced[:1].upper() + spaced[1:]


lines = ["itemGroup.%s=Trails & Tales" % assets.MODID]
for key in re.findall(r'@Config\.LangKey\("([^"]+)"\)', open(CONFIG, encoding="utf-8").read()):
    last = key.split(".")[-1]
    lines.append("%s=%s" % (key, NAMES.get(last, words(last))))
assets.merge_lang("config", lines)

# Sounds every stream may use (common/wood/AxeStripping).
events = {"item.axe.strip": assets.copy_sound("item.axe.strip")}
assets.merge_sounds("common", events)
assets.merge_lang("common", assets.subtitle_lines(events))
print("common: %d lines" % len(lines))
