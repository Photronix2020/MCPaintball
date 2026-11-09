"""Original 3D models for Splatball's five paint markers.

Run:  python tools/gun_models.py
Writes, for each marker, three item models:
  <name>.json     - picks the 3D model in the hand / on the ground and the flat sprite in inventories
  <name>_3d.json  - the box model built from the PARTS list below
  <name>_2d.json  - the flat inventory sprite (texture comes from gen_assets.py)
plus the shared colour palette texture and tools/guns_preview.png to eyeball the shapes.

Model space: 16 units = one block. The barrel points toward +Z, up is +Y, the gun is centred on x = 8,
and the grip sits just below the model centre (8, 8, 8), which is where the hand holds it.
Needs only the Python standard library.
"""
import json
import math
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src" / "main" / "resources" / "assets" / "splatball"

# palette: name -> (rgb, cell index in the 8x8 grid of 2x2-pixel swatches)
COLORS = {
    "body": (58, 62, 70), "lite": (112, 118, 130), "dark": (30, 32, 36), "grip": (92, 64, 42),
    "steel": (160, 164, 172), "black": (16, 16, 18), "wood": (124, 86, 52), "glass": (150, 215, 255),
    "yellow": (250, 204, 40), "yellow2": (255, 232, 120),
    "lime": (120, 220, 50), "lime2": (180, 245, 120),
    "orange": (245, 130, 30), "orange2": (255, 180, 100),
    "sky": (90, 190, 250), "sky2": (170, 225, 255),
    "magenta": (225, 70, 200), "magenta2": (250, 150, 235),
}
CELL = {name: i for i, name in enumerate(COLORS)}


def box(color, x1, y1, z1, x2, y2, z2):
    return (color, (x1, y1, z1), (x2, y2, z2))


def sym(color, half, y1, z1, y2, z2):
    """A box centred on x = 8 with the given half-width."""
    return box(color, 8 - half, y1, z1, 8 + half, y2, z2)


def hopper(color, z1, z2, size=1.5, y=10.5):
    """Feed neck plus a paint hopper sitting on top of the body."""
    mid = (z1 + z2) / 2
    return [
        sym("dark", 0.5, y, mid - 1, y + 1, mid + 1),
        sym(color, size, y + 1, z1, y + 4, z2),
        sym(color + "2", size - 0.5, y + 4, z1 + 1, y + 4.6, z2 - 1),
    ]


def grip(z1, z2, color="grip", top=8):
    return [sym(color, 0.8, 3.5, z1, top, z2)]


def trigger(z):
    return [sym("black", 0.4, 6.4, z, 6.9, z + 2.2), sym("black", 0.3, 6.9, z + 0.6, 8, z + 1.1)]


GUNS = {
    "splat_pistol": (
        [sym("body", 1, 8, 4, 10.5, 12), sym("lite", 1.05, 8.9, 5, 9.7, 11), sym("dark", 0.6, 8.8, 12, 10, 16.5),
         sym("steel", 0.7, 6.3, 9, 7.9, 13.5), sym("dark", 0.75, 6.2, 13.5, 8, 14)]
        + grip(4.5, 6.5) + trigger(6.5) + hopper("yellow", 4.5, 9.5)),
    "splatter_smg": (
        [sym("body", 1.1, 8, 2, 10.8, 13), sym("lite", 1.15, 9, 3, 9.9, 12), sym("dark", 0.6, 8.9, 13, 10.1, 19),
         sym("steel", 0.9, 8.6, 15, 10.4, 17.5), sym("dark", 0.7, 2.5, 9.5, 8, 11.5),
         sym("dark", 0.5, 8.8, -3, 9.8, 2), sym("body", 0.9, 6.5, -4, 10.6, -3)]
        + grip(4.5, 6.5) + trigger(6.5) + hopper("lime", 3.5, 9.5, size=1.7, y=10.8)),
    "scatter_blaster": (
        [sym("body", 1.5, 8, 2, 11, 10), sym("lite", 1.55, 9.1, 3, 9.9, 9),
         box("dark", 6.5, 9, 10, 7.8, 10.4, 21), box("dark", 8.2, 9, 10, 9.5, 10.4, 21),
         sym("body", 0.4, 9.3, 10, 10.1, 20), sym("steel", 1.7, 7, 12.5, 9, 17),
         sym("wood", 0.9, 7.2, -5, 10.2, 2), sym("wood", 1.0, 6.2, -6, 10.6, -5)]
        + grip(4, 6, color="wood") + trigger(6) + hopper("orange", 3, 8, y=11)),
    "long_shot": (
        [sym("body", 0.9, 8, 2, 10.5, 12), sym("lite", 0.95, 8.9, 3, 9.6, 11),
         sym("dark", 0.5, 9, 12, 10, 25), sym("steel", 0.7, 8.8, 24, 10.2, 26.5),
         sym("dark", 0.7, 11.6, 3, 13, 11.5), sym("dark", 0.9, 11.4, 2.2, 13.2, 3.4), sym("dark", 0.9, 11.4, 10.6, 13.2, 11.8),
         sym("glass", 0.6, 11.7, 11.8, 12.9, 12), sym("glass", 0.6, 11.7, 2, 12.9, 2.2),
         sym("dark", 0.3, 10.5, 4.5, 11.6, 5.3), sym("dark", 0.3, 10.5, 9, 11.6, 9.8),
         sym("wood", 0.8, 7.4, -6, 10, 2), sym("wood", 0.9, 6.2, -7, 10.4, -6),
         box("steel", 6.6, 4.5, 18, 7, 9, 18.6), box("steel", 9, 4.5, 18, 9.4, 9, 18.6),
         box("sky", 9, 8.6, 5, 10.6, 10.4, 8.5), box("sky2", 10.6, 9, 5.6, 11, 10, 7.9)]
        + grip(4.5, 6.5, color="wood") + trigger(6.5)),
    "paint_launcher": (
        [sym("lite", 2.5, 8, 0, 13, 15), sym("body", 2.7, 7.8, -0.5, 13.2, 1), sym("body", 2.7, 7.8, 7, 13.2, 8.2),
         sym("body", 2.7, 7.8, 14, 13.2, 15.5), sym("magenta", 2, 8.5, 15.5, 12.5, 16.3),
         sym("magenta2", 1.2, 9.3, 16.3, 11.7, 16.7), sym("black", 2, 8.5, -0.9, 12.5, -0.5),
         sym("dark", 0.4, 13, 3, 14.2, 4), sym("dark", 0.4, 13, 11, 13.8, 12)]
        + grip(3.5, 5.5) + grip(10.5, 12.5) + trigger(5.5)),
}

# How each view holds the 3D model. Tuned in-game from screenshots.
DISPLAY = {
    "firstperson_righthand": {"rotation": [0, 180, 0], "translation": [-0.5, 2, -3.5], "scale": [0.6, 0.6, 0.6]},
    "firstperson_lefthand": {"rotation": [0, 180, 0], "translation": [-0.5, 2, -3.5], "scale": [0.6, 0.6, 0.6]},
    "thirdperson_righthand": {"rotation": [0, 180, 0], "translation": [0, 0.5, 3.5], "scale": [0.8, 0.8, 0.8]},
    "thirdperson_lefthand": {"rotation": [0, 180, 0], "translation": [0, 0.5, 3.5], "scale": [0.8, 0.8, 0.8]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.6, 0.6, 0.6]},
    "head": {"rotation": [0, 0, 0], "translation": [0, 8, 0], "scale": [1, 1, 1]},
}


def png(rows):
    def chunk(tag, body):
        return struct.pack(">I", len(body)) + tag + body + struct.pack(">I", zlib.crc32(tag + body) & 0xFFFFFFFF)

    raw = b"".join(b"\x00" + b"".join(bytes(px) for px in row) for row in rows)
    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", len(rows[0]), len(rows), 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


def palette_png():
    rows = [[(0, 0, 0, 255)] * 16 for _ in range(16)]
    for name, (r, g, b) in COLORS.items():
        cx, cy = CELL[name] % 8 * 2, CELL[name] // 8 * 2
        for dy in range(2):
            for dx in range(2):
                rows[cy + dy][cx + dx] = (r, g, b, 255)
    return png(rows)


def element(color, lo, hi):
    u, v = CELL[color] % 8 * 2 + 0.5, CELL[color] // 8 * 2 + 0.5
    face = {"uv": [u, v, u + 1, v + 1], "texture": "#p"}
    return {"from": list(lo), "to": list(hi),
            "faces": {side: face for side in ("north", "south", "east", "west", "up", "down")}}


def write_json(path, content):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(content, indent=1) + "\n", encoding="utf-8")


def preview(path):
    """Shaded orthographic 3/4 and side views of every gun on one sheet."""
    cell_w, cell_h, scale = 300, 190, 8
    sheet = [[(40, 44, 52, 255)] * (cell_w * len(GUNS)) for _ in range(cell_h * 2)]
    corners = [(0, 0, 0), (1, 0, 0), (1, 1, 0), (0, 1, 0), (0, 0, 1), (1, 0, 1), (1, 1, 1), (0, 1, 1)]
    faces = [((0, 1, 2, 3), (0, 0, -1)), ((4, 5, 6, 7), (0, 0, 1)), ((0, 4, 7, 3), (-1, 0, 0)),
             ((1, 5, 6, 2), (1, 0, 0)), ((3, 2, 6, 7), (0, 1, 0)), ((0, 1, 5, 4), (0, -1, 0))]
    light = (0.4, 0.8, 0.45)
    for column, parts in enumerate(GUNS.values()):
        for row, (yaw, pitch) in enumerate(((math.radians(55), math.radians(20)), (math.radians(90), 0.0))):
            def project(p):
                x, y, z = p[0] - 8, p[1] - 8, p[2] - 8
                x, z = x * math.cos(yaw) - z * math.sin(yaw), x * math.sin(yaw) + z * math.cos(yaw)
                y, z = y * math.cos(pitch) - z * math.sin(pitch), y * math.sin(pitch) + z * math.cos(pitch)
                return (column * cell_w + cell_w / 2 - x * scale, row * cell_h + cell_h / 2 - y * scale + 10, z)

            quads = []
            for color, lo, hi in parts:
                pts = [project(tuple(lo[i] + c[i] * (hi[i] - lo[i]) for i in range(3))) for c in corners]
                for idx, normal in faces:
                    shade = 0.55 + 0.45 * max(0.0, sum(a * b for a, b in zip(normal, light)))
                    rgb = tuple(int(ch * shade) for ch in COLORS[color]) + (255,)
                    quad = [pts[i] for i in idx]
                    quads.append((sum(p[2] for p in quad) / 4, quad, rgb))
            for _, quad, rgb in sorted(quads, key=lambda q: q[0]):
                xs, ys = [p[0] for p in quad], [p[1] for p in quad]
                for py in range(max(0, int(min(ys))), min(len(sheet), int(max(ys)) + 1)):
                    for px in range(max(0, int(min(xs))), min(len(sheet[0]), int(max(xs)) + 1)):
                        signs = []
                        for i in range(4):
                            ax, ay, bx, by = xs[i], ys[i], xs[(i + 1) % 4], ys[(i + 1) % 4]
                            signs.append((bx - ax) * (py + 0.5 - ay) - (by - ay) * (px + 0.5 - ax))
                        if all(s >= 0 for s in signs) or all(s <= 0 for s in signs):
                            sheet[py][px] = rgb
    path.write_bytes(png(sheet))


def main():
    (ROOT / "textures" / "item").mkdir(parents=True, exist_ok=True)
    (ROOT / "textures" / "item" / "marker_palette.png").write_bytes(palette_png())
    models = ROOT / "models" / "item"
    for name, parts in GUNS.items():
        write_json(models / f"{name}_3d.json", {
            "textures": {"p": "splatball:item/marker_palette", "particle": "splatball:item/marker_palette"},
            "elements": [element(*part) for part in parts],
            "display": DISPLAY,
        })
        write_json(models / f"{name}_2d.json", {"parent": "item/generated", "textures": {"layer0": f"splatball:item/{name}"}})
        write_json(models / f"{name}.json", {
            "loader": "forge:separate_transforms",
            "gui_light": "front",
            "base": {"parent": f"splatball:item/{name}_3d"},
            "perspectives": {"gui": {"parent": f"splatball:item/{name}_2d"}, "fixed": {"parent": f"splatball:item/{name}_2d"}},
        })
    preview(Path(__file__).resolve().parent / "guns_preview.png")
    print("gun models written to", models)


if __name__ == "__main__":
    main()
