"""Generates Splatball's item textures, item models, recipes and the empty game-test structure.

Run from anywhere:  python tools/gen_assets.py
Edit the pixel grids below to redraw a marker, then re-run. Needs only the Python standard library.
"""
import gzip
import json
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src" / "main" / "resources"
ASSETS = ROOT / "assets" / "splatball"
DATA = ROOT / "data" / "splatball"

BASE = {
    ".": (0, 0, 0, 0),
    "B": (58, 62, 70, 255),     # body
    "L": (112, 118, 130, 255),  # body highlight
    "D": (32, 34, 38, 255),     # barrel / dark metal
    "G": (92, 64, 42, 255),     # grip
    "T": (20, 20, 22, 255),     # trigger
    "W": (245, 245, 245, 255),
    "w": (200, 200, 205, 255),
    "S": (150, 150, 158, 255),
}

# 16x16, barrel pointing right. H/h = paint hopper colour for that marker.
SPRITES = {
    "splat_pistol": ((250, 204, 40), """
................
................
................
................
....HHHH........
....HhhH........
....HHHH........
..BBBBBBBBBB....
..BLLLLLLLLBDDD.
..BLLLLLLLLBDDD.
..BBBBBBBBBB....
...GG.T.........
...GG.T.........
...GG...........
...GG...........
................
"""),
    "splatter_smg": ((120, 220, 50), """
................
................
.....HHHH.......
.....HhhH.......
.....HHHH.......
.BBBBBBBBBBBB...
.BLLLLLLLLLLBDDD
.BLLLLLLLLLLBDDD
.BBBBBBBBBBBB...
.BB.GG.T.DD.....
.B..GG.T.DD.....
....GG...DD.....
....GG...DD.....
.........DD.....
................
................
"""),
    "scatter_blaster": ((245, 130, 30), """
................
................
................
....HHHH........
....HhhH........
GGBBBBBBBBBBBBBB
GGBLLLLLLLLLLLLD
GGBBBBBBBBBBBBBB
GGBLLLLLLLLLLLLD
GGBBBBBBBBBBBBBB
G..GG.T.SSSSS...
...GG.T.SSSSS...
...GG...........
................
................
................
"""),
    "long_shot": ((90, 190, 250), """
................
................
................
....DDDDDDD.....
....DHhhhHD.....
.....D...D......
GGBBBBBBBBBB....
GGBLLLLLLLLBDDDD
GGBBBBBBBBBBDDDD
GG.GG.T.........
G..GG.T.....S...
...GG......S.S..
...GG...........
................
................
................
"""),
    "paint_launcher": ((225, 70, 200), """
................
................
................
..BBBBBBBBBBBB..
.BBLLLLLLLLLLHH.
.BBLLLLLLLLLLHhH
.BBLLLLLLLLLLHhH
.BBLLLLLLLLLLHhH
.BBLLLLLLLLLLHH.
..BBBBBBBBBBBB..
....GG.T.GG.....
....GG.T.GG.....
....GG..........
................
................
................
"""),
    # white so the item colour handler can tint it to the shooter's colour
    "paintball": ((255, 255, 255), """
................
................
................
................
................
.....wwww.......
....wWWWWw......
...wWWWWWWw.....
...wWWWWWWw.....
...wWWWWwww.....
...wWWWwwww.....
....wwwwww......
.....wwww.......
................
................
................
"""),
}

RECIPES = {
    "splat_pistol": None,
    "splatter_smg": "minecraft:redstone",
    "scatter_blaster": "minecraft:piston",
    "long_shot": "minecraft:spyglass",
    "paint_launcher": "minecraft:dispenser",
}


STATION = """
WWWWWWWWWWWWWWWW
WRRRRRRRbbbbbbbW
WRRRRRRRbbbbbbbW
WRRWWRRRbbbWWbbW
WRRWWWRRbbWWWbbW
WRRRWWWWWWWWbbbW
WRRRRWWWWWWbbbbW
WRRRRWWTTWWbbbbW
WRRRRWWTTWWbbbbW
WRRRRWWWWWWbbbbW
WRRRWWWWWWWWbbbW
WRRWWWRRbbWWWbbW
WRRWWRRRbbbWWbbW
WRRRRRRRbbbbbbbW
WRRRRRRRbbbbbbbW
WWWWWWWWWWWWWWWW
"""


def station_pixels():
    palette = dict(BASE, R=(208, 48, 48, 255), b=(48, 80, 208, 255))
    return [[palette[ch] for ch in line] for line in STATION.strip("\n").split("\n")]


def png(rows):
    def chunk(tag, body):
        return struct.pack(">I", len(body)) + tag + body + struct.pack(">I", zlib.crc32(tag + body) & 0xFFFFFFFF)

    raw = b"".join(b"\x00" + b"".join(bytes(px) for px in row) for row in rows)
    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", len(rows[0]), len(rows), 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


def pixels(name):
    (r, g, b), art = SPRITES[name]
    palette = dict(BASE, H=(r, g, b, 255), h=(min(r + 50, 255), min(g + 50, 255), min(b + 50, 255), 255))
    lines = art.strip("\n").split("\n")
    assert len(lines) == 16 and all(len(line) == 16 for line in lines), f"{name} must be 16x16"
    return [[palette[ch] for ch in line] for line in lines]


def write(path, content):
    path.parent.mkdir(parents=True, exist_ok=True)
    if isinstance(content, bytes):
        path.write_bytes(content)
    else:
        path.write_text(json.dumps(content, indent=2) + "\n", encoding="utf-8")


def marker_model(name):
    # sprite +x is "forward" for a held flat item, so these keep the barrel pointing where you aim
    return {
        "parent": "item/generated",
        "textures": {"layer0": f"splatball:item/{name}"},
        "display": {
            "firstperson_righthand": {"rotation": [0, -90, 5], "translation": [1.13, 3.2, 1.13], "scale": [0.9, 0.9, 0.9]},
            "firstperson_lefthand": {"rotation": [0, 90, -5], "translation": [1.13, 3.2, 1.13], "scale": [0.9, 0.9, 0.9]},
            "thirdperson_righthand": {"rotation": [0, -90, 100], "translation": [0, 1.5, 2.5], "scale": [0.85, 0.85, 0.85]},
            "thirdperson_lefthand": {"rotation": [0, 90, -100], "translation": [0, 1.5, 2.5], "scale": [0.85, 0.85, 0.85]},
        },
    }


def recipe(name, extra):
    key = {"I": {"item": "minecraft:iron_ingot"}, "S": {"item": "minecraft:slime_ball"}}
    pattern = ["IIS", "I  "]
    if extra:
        key["X"] = {"item": extra}
        pattern = ["IIS", "IX "]
    return {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": pattern, "key": key,
            "result": {"item": f"splatball:{name}"}}


def nbt_empty_structure():
    """A 1x1x1 air structure in gzipped NBT, used as the template for game tests."""
    def name(s):
        raw = s.encode()
        return struct.pack(">H", len(raw)) + raw

    def tag_int(n, v):
        return b"\x03" + name(n) + struct.pack(">i", v)

    def tag_int_list(n, values):
        return b"\x09" + name(n) + b"\x03" + struct.pack(">i", len(values)) + b"".join(struct.pack(">i", v) for v in values)

    def tag_compound_list(n, payloads):
        return b"\x09" + name(n) + b"\x0a" + struct.pack(">i", len(payloads)) + b"".join(p + b"\x00" for p in payloads)

    palette = [b"\x08" + name("Name") + name("minecraft:air")]
    blocks = [tag_int_list("pos", [0, 0, 0]) + tag_int("state", 0)]
    body = (tag_int("DataVersion", 3465) + tag_int_list("size", [1, 1, 1]) + tag_compound_list("palette", palette)
            + tag_compound_list("blocks", blocks) + b"\x09" + name("entities") + b"\x00" + struct.pack(">i", 0))
    return gzip.compress(b"\x0a" + name("") + body + b"\x00")


def main():
    for sprite in SPRITES:
        write(ASSETS / "textures" / "item" / f"{sprite}.png", png(pixels(sprite)))
    write(ASSETS / "models" / "item" / "paintball.json",
          {"parent": "item/generated", "textures": {"layer0": "splatball:item/paintball"}})
    for marker, extra in RECIPES.items():
        # item models for the markers are written by tools/gun_models.py
        write(DATA / "recipes" / f"{marker}.json", recipe(marker, extra))
    write(DATA / "structures" / "empty.nbt", nbt_empty_structure())

    block = "splatball:block/paintball_station"
    write(ASSETS / "textures" / "block" / "paintball_station.png", png(station_pixels()))
    write(ASSETS / "blockstates" / "paintball_station.json", {"variants": {"": {"model": block}}})
    write(ASSETS / "models" / "block" / "paintball_station.json", {"parent": "block/cube_all", "textures": {"all": block}})
    write(ASSETS / "models" / "item" / "paintball_station.json", {"parent": block})
    write(DATA / "loot_tables" / "blocks" / "paintball_station.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "splatball:paintball_station"}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    write(DATA / "recipes" / "paintball_station.json", {
        "type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["WRW", "WSW", "WBW"],
        "key": {"W": {"item": "minecraft:white_wool"}, "R": {"item": "minecraft:red_dye"},
                "B": {"item": "minecraft:blue_dye"}, "S": {"item": "minecraft:slime_ball"}},
        "result": {"item": "splatball:paintball_station"}})

    # contact sheet so the sprites can be eyeballed without launching the game
    scale, names = 12, list(SPRITES)
    sheet = [[(40, 44, 52, 255)] * (len(names) * 17 * scale) for _ in range(16 * scale)]
    for i, sprite in enumerate(names):
        for y, row in enumerate(pixels(sprite)):
            for x, px in enumerate(row):
                if px[3]:
                    for dy in range(scale):
                        for dx in range(scale):
                            sheet[y * scale + dy][(i * 17 + x) * scale + dx] = px
    write(Path(__file__).resolve().parent / "sprites_preview.png", png(sheet))
    print("assets written to", ROOT)


if __name__ == "__main__":
    main()
