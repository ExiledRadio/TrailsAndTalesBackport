"""Assets for a common/wood WoodSet: blockstates, models, textures, item models, names and sounds.

Used by tools/cherry/generate.py and tools/bamboo/generate.py. Every model and texture is 1.20.6's own,
through assets.port_blockstate / port_item_model; this file only says which 1.12.2 states each block has
and how they map to 1.20's.
"""
import assets

HORIZONTAL = ["north", "south", "west", "east"]
BOOL = ["true", "false"]


def _button(state):
    f = state.pop("facing")
    if f == "up":
        state.update(face="floor", facing="north")
    elif f == "down":
        state.update(face="ceiling", facing="north")
    else:
        state.update(face="wall", facing=f)
    return state


def _slab_half(state):
    state["type"] = state.pop("half")
    state.pop("variant")
    return state


def _slab_double(state):
    state.pop("variant")
    state["type"] = "double"
    return state


def blocks(wood, bamboo=False):
    """@return [(our name, vanilla name, props, convert)] for every block of the set."""
    n = lambda s: "%s_%s" % (wood, s)  # noqa: E731
    out = []
    pillar = {"axis": ["x", "y", "z"]}
    if bamboo:
        out += [(n("block"), n("block"), pillar, None),
                ("stripped_%s_block" % wood, "stripped_%s_block" % wood, pillar, None)]
    else:
        for s in ("log", "wood"):
            out += [(n(s), n(s), pillar, None), ("stripped_" + n(s), "stripped_" + n(s), pillar, None)]
    planks = [n("planks")] + ([n("mosaic")] if bamboo else [])
    for p in planks:
        base = p[:-len("_planks")] if p.endswith("_planks") else p
        out += [(p, p, {}, None),
                (base + "_stairs", base + "_stairs",
                 {"facing": HORIZONTAL, "half": ["top", "bottom"],
                  "shape": ["straight", "inner_left", "inner_right", "outer_left", "outer_right"]}, None),
                (base + "_slab", base + "_slab", {"half": ["top", "bottom"], "variant": ["default"]}, _slab_half),
                (base + "_double_slab", base + "_slab", {"variant": ["default"]}, _slab_double)]
    out += [
        (n("fence"), n("fence"), None, None),
        (n("fence_gate"), n("fence_gate"), {"facing": HORIZONTAL, "in_wall": BOOL, "open": BOOL, "powered": BOOL}, None),
        (n("door"), n("door"), {"facing": HORIZONTAL, "half": ["upper", "lower"], "hinge": ["left", "right"],
                                "open": BOOL, "powered": BOOL}, None),
        (n("trapdoor"), n("trapdoor"), {"facing": HORIZONTAL, "half": ["top", "bottom"], "open": BOOL}, None),
        (n("pressure_plate"), n("pressure_plate"), {"powered": BOOL}, None),
        (n("button"), n("button"), {"facing": ["down", "up"] + HORIZONTAL, "powered": BOOL}, _button),
        (n("sign"), n("sign"), {"rotation": [str(i) for i in range(16)]}, None),
        (n("wall_sign"), n("wall_sign"), {"facing": HORIZONTAL}, None),
    ]
    return out


# 1.12.2 names the items of these blocks differently from their 1.20 item models: none, today.
ITEMS_FROM_BLOCK = ("log", "wood", "planks", "stairs", "slab", "fence", "fence_gate", "trapdoor", "pressure_plate",
                    "button", "block", "mosaic", "mosaic_stairs", "mosaic_slab")


def generate(wood, sound_set, bamboo=False):
    """Writes every asset of the set; @return (lang lines, sound events)."""
    M = assets.MODID
    lang = []
    for ours, vanilla, props, convert in blocks(wood, bamboo):
        assets.port_blockstate(vanilla, ours, props or {}, convert)
        lang.append("tile.%s.%s.name=%s" % (M, ours, assets.name("block.minecraft." + vanilla)))
    items = []
    for ours, vanilla, props, convert in blocks(wood, bamboo):
        if ours.endswith("_double_slab") or ours.endswith("_wall_sign") or ours.endswith("_sign") or ours.endswith("_door"):
            continue
        items.append(ours)
    for name in items:
        assets.port_item_model(name)
    for name in ("%s_door" % wood, "%s_sign" % wood):
        assets.port_item_model(name)
        lang.append("item.%s.%s.name=%s" % (M, name, assets.name("item.minecraft." + name)
                                            if ("item.minecraft." + name) in assets.VANILLA_LANG
                                            else assets.name("block.minecraft." + name)))
    assets.texture("entity/signs/" + wood)

    b = "block." + sound_set
    events = {}
    for e in [b + ".break", b + ".step", b + ".place", b + ".hit", b + ".fall",
              b + "_door.open", b + "_door.close", b + "_trapdoor.open", b + "_trapdoor.close",
              b + "_button.click_on", b + "_button.click_off", b + "_pressure_plate.click_on",
              b + "_pressure_plate.click_off", b + "_fence_gate.open", b + "_fence_gate.close"]:
        events[e] = assets.copy_sound(e)
    return lang, events
