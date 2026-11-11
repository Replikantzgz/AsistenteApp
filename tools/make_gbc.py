#!/usr/bin/env python3
"""Alfred en estilo Game Boy Color: 4 propuestas sobre fondo claro.
Uso: python3 tools/make_gbc.py <carpeta_salida>"""
from PIL import Image, ImageDraw
import os, sys

K = "#181818"; W = "#FFFFFF"; BG = "#FFFFFF"
HEAD = "#F0F0F8"; HEAD_S = "#A8B4D0"; HEAD_D = "#6C7898"; PLATE = "#FFF0D8"
BLUE = "#3868C8"; BLUE_L = "#78A0F0"; BLUE_D = "#203878"
GOLD = "#F8C028"; GOLD_D = "#B07818"; CYAN = "#50D8F0"; CYAN_D = "#1898C0"; PINK = "#F89088"; RED = "#E83030"
SHOE = "#804020"; SILVER = "#C8D0E0"; SCREEN = "#D8F0C0"; SCREEN_D = "#98C078"

def load_mascot():
    spec = importlib.util.spec_from_file_location("mm", os.path.join(os.path.dirname(__file__), "make_mascot.py"))
    m = importlib.util.module_from_spec(spec); spec.loader.exec_module(m); return m

def hexrgb(h): return tuple(int(h[i:i + 2], 16) for i in (1, 3, 5))

# ---------------------------------------------------------------- 1 · Sprite de batalla (frontal, 40x40)
def opt1():
    im = Image.new("RGB", (40, 40), BG); d = ImageDraw.Draw(im)
    def box(x0, y0, x1, y1, f): d.rectangle([x0, y0, x1, y1], fill=K); d.rectangle([x0 + 1, y0 + 1, x1 - 1, y1 - 1], fill=f)
    def cham(x0, y0, x1, y1, f, c=3):
        d.polygon([(x0 + c, y0), (x1 - c, y0), (x1, y0 + c), (x1, y1 - c), (x1 - c, y1), (x0 + c, y1), (x0, y1 - c), (x0, y0 + c)], fill=K)
        d.polygon([(x0 + c, y0 + 1), (x1 - c, y0 + 1), (x1 - 1, y0 + c), (x1 - 1, y1 - c), (x1 - c, y1 - 1), (x0 + c, y1 - 1), (x0 + 1, y1 - c), (x0 + 1, y0 + c)], fill=f)
    # piernas y zapatos
    for x in (13, 23): d.rectangle([x, 35, x + 3, 37], fill=BLUE_D); d.rectangle([x - 1, 38, x + 4, 39], fill=K); d.rectangle([x, 38, x + 3, 38], fill=SHOE)
    # cuerpo
    box(10, 25, 29, 35, BLUE); d.rectangle([11, 26, 12, 34], fill=BLUE_L); d.rectangle([27, 26, 28, 34], fill=BLUE_D)
    d.polygon([(16, 25), (23, 25), (19, 33)], fill=K); d.polygon([(17, 26), (22, 26), (19, 31)], fill=W)
    d.rectangle([19, 32, 19, 32], fill=GOLD); d.rectangle([19, 34, 19, 34], fill=GOLD)
    # brazo izquierdo
    box(7, 26, 10, 33, BLUE); d.rectangle([8, 27, 8, 32], fill=BLUE_L); box(6, 33, 10, 36, W)
    # brazo derecho + bandeja + orbe
    d.polygon([(29, 26), (32, 26), (37, 30), (35, 33), (29, 31)], fill=K); d.polygon([(30, 27), (31, 27), (35, 30), (34, 31), (30, 30)], fill=BLUE)
    box(34, 31, 38, 34, W)
    d.rectangle([30, 27, 39, 30], fill=K); d.rectangle([31, 28, 38, 29], fill=SILVER); d.line([(31, 28), (38, 28)], fill=W)
    d.rectangle([34, 19, 37, 26], fill=K); d.rectangle([33, 20, 38, 25], fill=K); d.rectangle([34, 20, 37, 25], fill=CYAN); d.rectangle([35, 21, 36, 22], fill=W); d.rectangle([34, 24, 37, 25], fill=CYAN_D)
    # cuello y cabeza
    d.rectangle([17, 23, 22, 25], fill=HEAD_D)
    box(3, 12, 6, 18, GOLD); box(33, 12, 36, 18, GOLD)
    cham(7, 6, 32, 24, HEAD, 4)
    d.rectangle([9, 8, 14, 9], fill=W); d.rectangle([9, 10, 10, 12], fill=W)
    d.line([(30, 12), (30, 21)], fill=HEAD_S); d.line([(10, 22), (29, 22)], fill=HEAD_S)
    d.rectangle([10, 11, 29, 20], fill=PLATE); d.line([(10, 20), (29, 20)], fill="#E8D4B0")
    for x in (13, 25):
        d.rectangle([x, 13, x + 1, 17], fill=K); d.point((x, 13), fill=W); d.point((x + 1, 14), fill="#7070A0")
    d.rectangle([11, 18, 12, 18], fill=PINK); d.rectangle([27, 18, 28, 18], fill=PINK)
    d.line([(18, 18), (21, 18)], fill=K); d.point((17, 17), fill=K); d.point((22, 17), fill=K)
    d.rectangle([19, 3, 20, 6], fill=K); d.rectangle([18, 0, 21, 3], fill=K); d.rectangle([19, 1, 20, 2], fill=CYAN); d.point((19, 1), fill=W)
    # pajarita
    d.polygon([(13, 25), (19, 27), (13, 31)], fill=K); d.polygon([(26, 25), (20, 27), (26, 31)], fill=K)
    d.polygon([(14, 26), (18, 27), (14, 29)], fill=GOLD); d.polygon([(25, 26), (21, 27), (25, 29)], fill=GOLD)
    d.rectangle([18, 26, 21, 28], fill=K); d.rectangle([19, 27, 20, 27], fill=GOLD_D)
    return im

# ---------------------------------------------------------------- 2 · Sprite de mapa (diminuto)
def opt2():
    im = Image.new("RGB", (24, 24), BG); d = ImageDraw.Draw(im)
    def box(x0, y0, x1, y1, f): d.rectangle([x0, y0, x1, y1], fill=K); d.rectangle([x0 + 1, y0 + 1, x1 - 1, y1 - 1], fill=f)
    d.rectangle([11, 1, 12, 4], fill=K); d.rectangle([10, 0, 13, 1], fill=K); d.rectangle([11, 0, 12, 0], fill=CYAN); d.point((11, 1), fill=CYAN_D); d.point((12, 1), fill=CYAN)
    box(4, 8, 6, 12, GOLD); box(17, 8, 19, 12, GOLD)                                  # orejas
    box(6, 4, 17, 14, HEAD)
    d.rectangle([7, 5, 8, 12], fill=W); d.rectangle([15, 8, 16, 13], fill=HEAD_S)
    d.rectangle([8, 8, 9, 10], fill=K); d.rectangle([14, 8, 15, 10], fill=K)           # ojos
    d.point((7, 11), fill=PINK); d.point((16, 11), fill=PINK); d.rectangle([11, 12, 12, 12], fill=K)
    box(6, 15, 17, 21, BLUE); d.rectangle([7, 16, 8, 20], fill=BLUE_L); d.rectangle([11, 16, 12, 20], fill=W)
    d.polygon([(8, 16), (11, 17), (8, 19)], fill=GOLD); d.polygon([(15, 16), (12, 17), (15, 19)], fill=GOLD)
    d.rectangle([4, 16, 5, 19], fill=BLUE); d.rectangle([18, 16, 19, 19], fill=BLUE); d.point((4, 20), fill=W); d.point((19, 20), fill=W)
    for x in (8, 13): d.rectangle([x, 22, x + 2, 23], fill=SHOE); d.line([(x, 23), (x + 2, 23)], fill=K)
    return im

def tiny_head(d, ox, oy):
    """Cabeza de Alfred de 16x14 px para etiquetas y pantallas."""
    d.rectangle([ox + 7, oy, ox + 8, oy + 2], fill=K); d.rectangle([ox + 7, oy, ox + 8, oy], fill=CYAN)
    d.rectangle([ox, oy + 5, ox + 1, oy + 8], fill=GOLD); d.rectangle([ox + 14, oy + 5, ox + 15, oy + 8], fill=GOLD)
    d.rectangle([ox + 1, oy + 2, ox + 14, oy + 12], fill=K); d.rectangle([ox + 2, oy + 3, ox + 13, oy + 11], fill=HEAD)
    d.rectangle([ox + 3, oy + 4, ox + 4, oy + 10], fill=W); d.rectangle([ox + 12, oy + 7, ox + 13, oy + 11], fill=HEAD_S)
    d.rectangle([ox + 4, oy + 6, ox + 5, oy + 8], fill=K); d.rectangle([ox + 10, oy + 6, ox + 11, oy + 8], fill=K)
    d.point((ox + 3, oy + 9), fill=PINK); d.point((ox + 12, oy + 9), fill=PINK); d.rectangle([ox + 7, oy + 9, ox + 8, oy + 9], fill=K)
    d.polygon([(ox + 3, oy + 13), (ox + 7, oy + 12), (ox + 3, oy + 11)], fill=GOLD); d.polygon([(ox + 12, oy + 13), (ox + 8, oy + 12), (ox + 12, oy + 11)], fill=GOLD)

# ---------------------------------------------------------------- 3 · Cartucho
def opt3():
    im = Image.new("RGB", (48, 48), BG); d = ImageDraw.Draw(im)
    d.polygon([(8, 4), (39, 4), (41, 6), (41, 41), (38, 44), (9, 44), (6, 41), (6, 6)], fill=K)
    d.polygon([(9, 5), (38, 5), (40, 7), (40, 40), (37, 43), (10, 43), (7, 40), (7, 7)], fill="#C4C8D8")
    d.rectangle([8, 6, 9, 38], fill=W); d.rectangle([38, 10, 39, 38], fill="#8890A8")
    d.rectangle([11, 8, 36, 34], fill=K); d.rectangle([12, 9, 35, 33], fill=W)
    d.rectangle([12, 9, 35, 13], fill=BLUE); d.line([(12, 13), (35, 13)], fill=BLUE_D)
    for x in range(14, 34, 4): d.rectangle([x, 11, x + 1, 11], fill=BLUE_L)         # franja
    tiny_head(d, 16, 17)
    d.rectangle([12, 31, 35, 33], fill=GOLD); d.line([(12, 33), (35, 33)], fill=GOLD_D)
    for y in (36, 38, 40): d.line([(13, y), (34, y)], fill="#8890A8")                 # agarre
    d.rectangle([14, 42, 33, 43], fill="#484858")
    return im

# ---------------------------------------------------------------- 4 · Consola con Alfred en la pantalla
def opt4():
    im = Image.new("RGB", (48, 48), BG); d = ImageDraw.Draw(im)
    d.rounded_rectangle([6, 2, 41, 45], radius=3, fill=K); d.rounded_rectangle([7, 3, 40, 44], radius=2, fill=BLUE)
    d.rectangle([8, 4, 9, 38], fill=BLUE_L); d.rectangle([38, 8, 39, 42], fill=BLUE_D)
    d.rounded_rectangle([10, 6, 37, 26], radius=2, fill=K); d.rectangle([11, 7, 36, 25], fill="#505868")
    d.rectangle([14, 9, 33, 24], fill=K); d.rectangle([15, 10, 32, 23], fill=SCREEN)
    d.line([(15, 22), (32, 22)], fill=SCREEN_D); d.rectangle([12, 14, 12, 16], fill=RED)   # led
    tiny_head(d, 16, 9)
    d.rectangle([15, 31, 21, 33], fill=K); d.rectangle([17, 29, 19, 35], fill=K); d.rectangle([18, 32, 18, 32], fill="#505868")  # cruceta
    for (x, y) in ((29, 33), (34, 30)): d.ellipse([x - 2, y - 2, x + 2, y + 2], fill=K); d.ellipse([x - 1, y - 1, x + 1, y + 1], fill=RED)
    d.line([(14, 40), (18, 38)], fill=BLUE_D); d.line([(19, 41), (23, 39)], fill=BLUE_D)
    for i in range(3): d.line([(29 + i * 3, 38 + (i % 1)), (31 + i * 3, 36)], fill=BLUE_D)
    return im

OPTS = [("1", "Sprite de batalla", opt1), ("2", "Sprite de mapa", opt2), ("3", "Cartucho", opt3), ("4", "Consola", opt4)]

def masked(im, shape, size, bg):
    big = im.resize((size, size), Image.NEAREST).convert("RGB")
    m = Image.new("L", (size, size), 0); dm = ImageDraw.Draw(m)
    if shape == "circle": dm.ellipse([0, 0, size - 1, size - 1], fill=255)
    else: dm.rounded_rectangle([0, 0, size - 1, size - 1], radius=size // 4, fill=255)
    base = Image.new("RGB", (size, size), bg); base.paste(big, (0, 0)); return base, m

if __name__ == "__main__":
    out = sys.argv[1] if len(sys.argv) > 1 else "."
    os.makedirs(out, exist_ok=True)
    cw = 296
    sheet = Image.new("RGB", (len(OPTS) * cw + 20, 560), "#FFFFFF"); dd = ImageDraw.Draw(sheet)
    for i, (k, name, f) in enumerate(OPTS):
        im = f(); big = im.resize((256, 256), Image.NEAREST); im.resize((im.width * 8, im.height * 8), Image.NEAREST).save(os.path.join(out, f"gbc_{k}.png"))
        x = 20 + i * cw
        sheet.paste(big, (x, 16)); dd.rectangle([x - 1, 15, x + 256, 272], outline="#181818")
        for j, (shape, size) in enumerate((("circle", 112), ("squircle", 112))):
            b, m = masked(im, shape, size, "#FFF4D6"); sheet.paste(b, (x + j * 128, 300), m)
        for j, size in enumerate((48, 72)):
            b, m = masked(im, "circle", size, "#FFF4D6"); sheet.paste(b, (x + j * 64, 430), m)
        dd.text((x, 500), f"{k} - {name}", fill="#181818")
    sheet.save(os.path.join(out, "gbc_hoja.png"))
