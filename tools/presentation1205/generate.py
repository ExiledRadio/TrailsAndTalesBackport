#!/usr/bin/env python3
"""
Stream presentation1205: the language files of 1.20.5's chunk toasts, low disk space warning, world recovery screen
and Viossa.

Sources, all Mojang's own:
  G:/MCDownloads/trails-1.20.6/client.jar         1.20.6's en_us.json (assets.VANILLA_LANG)
  G:/MCDownloads/trails-1.20.6/assetindex.json    1.20.6's asset index (16); every other language, vp_vl.json
                                                   included, is an object of it, fetched once into objects/
  ~/.gradle/caches/minecraft/.../minecraft-1.12.2.jar   1.12.2's en_us.lang (assets.OLD_LANG)

Writes:
  assets/trailsandtalesbackport/vanilla_text_1205/<code>.lang
      1.20.5's new vanilla keys this stream shows, in the family's languages (Mojang's text per language) and in
      Viossa. Core composes the folder into minecraft:lang/<code>.lang (Contribution.vanillaText).
      vp_vl.lang also holds 1.12.2's own vanilla keys in Viossa: a 1.12.2 key gets 1.20.6's Viossa text when 1.20.6
      has the same key with the same English (or the key is one Lf's vanilla_text rewords), or else when every
      1.20.6 key with that exact English has one Viossa text; placeholders, line breaks and formatting codes must
      agree. Anything else falls back to English, as 1.12.2 always loads en_us first.
  assets/trailsandtalesbackport/lang/vp_vl.lang
      this mod's own keys whose English is a vanilla name (tools/lang/vanilla_keys.py's matching), in Viossa.
  en_us.lang block "presentation1205": the config category's names.

  python -I tools/presentation1205/generate.py
"""

import ast
import hashlib
import io
import json
import os
import re
import sys
import urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
TOOLS = os.path.dirname(HERE)
sys.path.insert(0, TOOLS)
sys.path.insert(0, os.path.join(TOOLS, "lang"))
import assets  # noqa: E402
import vanilla_keys  # noqa: E402

SOURCE = assets.SOURCE
INDEX = json.load(open(SOURCE + "/assetindex.json", encoding="utf-8"))["objects"]

# The family's languages, as Lf's vanilla_text (tools/lookfeel/generate.py) and Backport Core ship them.
CODES = ["de_de", "en_us", "es_ar", "es_es", "es_mx", "es_uy", "es_ve", "fr_fr", "it_it", "ja_jp", "ko_kr",
         "nl_nl", "pl_pl", "pt_br", "ru_ru", "tr_tr", "uk_ua", "zh_cn", "zh_tw"]
VIOSSA = "vp_vl"

# 1.20.6's keys of the pieces this stream draws that 1.12.2 lacks. selectWorld.edit.backup* (the backup toast)
# are Backport Core's, which ships them in every language.
NEW_KEYS = [
    "chunk.toast.checkLog",
    "chunk.toast.loadFailure",
    "chunk.toast.lowDiskSpace",
    "chunk.toast.lowDiskSpace.description",
    "chunk.toast.saveFailure",
    "gui.continue",
    "recover_world.bug_tracker",
    "recover_world.button",
    "recover_world.done.failed",
    "recover_world.done.success",
    "recover_world.done.title",
    "recover_world.issue.missing_file",
    "recover_world.issue.none",
    "recover_world.message",
    "recover_world.no_fallback",
    "recover_world.restore",
    "recover_world.restoring",
    "recover_world.state_entry",
    "recover_world.state_entry.unknown",
    "recover_world.title",
    "recover_world.warning",
    "selectWorld.warning.lowDiskSpace.description",
    "selectWorld.warning.lowDiskSpace.title",
]


def mojang(code):
    """@return 1.20.6's language file of this code, from its asset index (fetched once)."""
    if code == "en_us":
        return assets.VANILLA_LANG
    h = INDEX["minecraft/lang/%s.json" % code]["hash"]
    local = "%s/objects/%s/%s" % (SOURCE, h[:2], h)
    if not os.path.exists(local):
        data = urllib.request.urlopen("https://resources.download.minecraft.net/%s/%s" % (h[:2], h), timeout=60).read()
        if hashlib.sha1(data).hexdigest() != h:
            raise IOError("hash mismatch for " + code)
        os.makedirs(os.path.dirname(local), exist_ok=True)
        with open(local, "wb") as f:
            f.write(data)
    return json.load(io.open(local, encoding="utf-8"))


def to_lang(value):
    return value.replace("\r", "").replace("\n", "\\n")


def write_lang(path, lines):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(lines) + "\n")


SPEC = re.compile(r"%(?:(\d+)\$)?([sd])|%%")
ANY_PERCENT = re.compile(r"%")


def args(text):
    """@return the arguments a format string reads, as sorted (index, conversion), or None if it has a stray %"""
    found = []
    seq = 0
    covered = 0
    for m in SPEC.finditer(text):
        covered += len(re.findall("%", m.group(0)))
        if m.group(0) == "%%":
            continue
        if m.group(1):
            found.append((int(m.group(1)), m.group(2)))
        else:
            seq += 1
            found.append((seq, m.group(2)))
    if covered != len(ANY_PERCENT.findall(text)):
        return None
    return sorted(set(found))


def compatible(english, translated):
    """True when a translation can stand for 1.12.2's English: same arguments, line breaks and formatting codes."""
    a, b = args(english), args(translated)
    if a is None or b is None or a != b:
        return False
    if ("\n" in translated) != ("\n" in english) or "\\n" in translated:
        return False
    return sorted(re.findall("\u00a7.", english)) == sorted(re.findall("\u00a7.", translated))


def lookfeel_reworded():
    """The 1.12.2 keys Lf's vanilla_text gives 1.20.6's English (read from its generator, not imported)."""
    tree = ast.parse(open(os.path.join(TOOLS, "lookfeel", "generate.py"), encoding="utf-8").read())
    for node in tree.body:
        if isinstance(node, ast.Assign) and any(getattr(t, "id", None) == "VANILLA_TEXT_KEYS" for t in node.targets):
            return set(ast.literal_eval(node.value))
    return set()


def vanilla_viossa(vp):
    """1.12.2's vanilla keys in Viossa. @return (lines, report)"""
    en = assets.VANILLA_LANG
    reworded = lookfeel_reworded()
    by_text = {}
    for k, v in en.items():
        by_text.setdefault(v, []).append(k)
    lines, same, text, skipped = [], 0, 0, 0
    for key in sorted(assets.OLD_LANG):
        old = assets.OLD_LANG[key]
        value = None
        if key in vp and (en.get(key) == old or key in reworded):
            value = vp[key]
            same += 1
        else:
            values = {vp[c] for c in by_text.get(old, []) if c in vp}
            if len(values) == 1:
                value = values.pop()
                text += 1
        if value is None:
            continue
        english = en[key] if key in reworded and key in en else old
        if not compatible(english, value):
            skipped += 1
            continue
        lines.append("%s=%s" % (key, to_lang(value)))
    return lines, "%d by key, %d by English text, %d left English (arguments differ)" % (same, text, skipped)


def our_viossa(vp):
    """This mod's own keys named after vanilla names, in Viossa."""
    en = assets.VANILLA_LANG
    lines = []
    path = os.path.join(assets.ASSETS, "lang", "en_us.lang")
    for line in open(path, encoding="utf-8").read().splitlines():
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, ours = line.split("=", 1)
        for cand in vanilla_keys.candidates(key):
            if cand in en:
                if en[cand] == ours and cand in vp and compatible(ours, vp[cand]):
                    lines.append("%s=%s" % (key, to_lang(vp[cand])))
                break
    return sorted(lines)


def main():
    folder = os.path.join(assets.ASSETS, "vanilla_text_1205")
    en = assets.VANILLA_LANG
    for code in CODES:
        moj = mojang(code)
        write_lang(os.path.join(folder, code + ".lang"), ["%s=%s" % (k, to_lang(moj.get(k, en[k]))) for k in NEW_KEYS])
    vp = mojang(VIOSSA)
    old, report = vanilla_viossa(vp)
    new = ["%s=%s" % (k, to_lang(vp.get(k, en[k]))) for k in NEW_KEYS]
    write_lang(os.path.join(folder, VIOSSA + ".lang"), ["# 1.12.2's vanilla text in Viossa"] + old
               + ["", "# 1.20.5's keys this mod shows"] + new)
    ours = our_viossa(vp)
    write_lang(os.path.join(assets.ASSETS, "lang", VIOSSA + ".lang"), ours)
    print("new keys: %d in %d languages and Viossa" % (len(NEW_KEYS), len(CODES)))
    print("Viossa, 1.12.2's keys: %d of %d (%s)" % (len(old), len(assets.OLD_LANG), report))
    print("Viossa, this mod's vanilla names: %d" % len(ours))

    assets.merge_lang("presentation1205", [
        "config.trailsandtalesbackport.presentation1205=1.20.5 World Warnings and Viossa",
        "config.trailsandtalesbackport.presentation1205.chunkToasts=Chunk Load and Save Failure Toasts",
        "config.trailsandtalesbackport.presentation1205.lowDiskSpace=Low Disk Space Warnings",
        "config.trailsandtalesbackport.presentation1205.recoverWorld=World Recovery Screen",
        "config.trailsandtalesbackport.presentation1205.viossa=Viossa in the Language List",
    ])
    print("done")


if __name__ == "__main__":
    main()
