#!/usr/bin/env python3
"""Propuestas de logo de Alfred: pixel-art 32x32 sobre fondo claro, ampliado x8 sin suavizar.
Uso: python3 tools/make_logo.py <carpeta_salida>"""
from PIL import Image, ImageDraw
import sys, os

INK = "#1B1740"; CREAM = "#FFF4D6"; CREAM_D = "#F1E2B5"; WHITE = "#FFFFFF"
STEEL = "#D5DDEE"; STEEL_D = "#9AA7C7"
GOLD = "#F2B705"; GOLD_L = "#FFE27A"; GOLD_D = "#B27A00"
BLUE = "#2F6FE4"; BLUE_L = "#8DB6FF"; BLUE_D = "#1A3F94"
CYAN = "#62E8FF"; CYAN_D = "#139AC0"; PINK = "#FFB3B3"; RED = "#E5484D"; HAT = "#33307A"; HAT_L = "#6C66C4"
N = 32; S = 8

def new():
    im = Image.new("RGBA", (N, N), CREAM); d = ImageDraw.Draw(im)
    for y in range(2, N, 4):                                    # trama de puntos muy tenue
        for x in range(2 + (y // 4 % 2) * 2, N, 4): d.point((x, y), fill=CREAM_D)
    return im, d

def chamfer(d, x0, y0, x1, y1, fill, c=2):
    pts = lambda i: [(x0 + c - i, y0 - i), (x1 - c + i, y0 - i), (x1 + i, y0 + c - i), (x1 + i, y1 - c + i),
                     (x1 - c + i, y1 + i), (x0 + c - i, y1 + i), (x0 - i, y1 - c + i), (x0 - i, y0 + c - i)]
    d.polygon(pts(1), fill=INK); d.polygon(pts(0), fill=fill)

def rect(d, x0, y0, x1, y1, fill):
    d.rectangle([x0, y0, x1, y1], fill=INK); d.rectangle([x0 + 1, y0 + 1, x1 - 1, y1 - 1], fill=fill)

def eye(d, x, y):
    d.ellipse([x, y, x + 4, y + 4], fill=CYAN, outline=INK); d.point((x + 1, y + 1), fill=WHITE); d.point((x + 2, y + 2), fill=INK)

def bowtie(d, cx, cy, w=5, h=3, c=GOLD):
    d.polygon([(cx - w - 1, cy - h), (cx, cy), (cx - w - 1, cy + h)], fill=c, outline=INK)
    d.polygon([(cx + w + 1, cy - h), (cx, cy), (cx + w + 1, cy + h)], fill=c, outline=INK)
    d.line([(cx - w, cy - h + 1), (cx - 2, cy - 1)], fill=GOLD_L); d.line([(cx + w, cy - h + 1), (cx + 2, cy - 1)], fill=GOLD_L)
    rect(d, cx - 1, cy - 1, cx + 1, cy + 1, GOLD_D)

def shoulders(d, top=24):
    d.polygon([(3, 31), (4, top + 3), (10, top), (21, top), (27, top + 3), (28, 31)], fill=BLUE, outline=INK)
    d.polygon([(12, top), (19, top), (15, 31)], fill=WHITE, outline=INK)
    d.line([(6, top + 4), (9, top + 2)], fill=BLUE_L)

def robot_head(d, top=6):
    d.line([(15, top - 4), (15, top)], fill=INK); d.line([(16, top - 4), (16, top)], fill=INK)
    d.ellipse([13, top - 6, 18, top - 1], fill=GOLD, outline=INK); d.point((14, top - 5), fill=GOLD_L)
    for x in (3, 25): rect(d, x, top + 4, x + 3, top + 10, GOLD)                       # tornillos laterales
    chamfer(d, 6, top, 25, top + 16, WHITE, 3)
    d.line([(9, top + 15), (22, top + 15)], fill=STEEL_D); d.line([(24, top + 4), (24, top + 12)], fill=STEEL)
    rect(d, 13, top + 17, 18, top + 19, STEEL_D)                                      # cuello

# ----------------------------------------------------------------------------- 1 · Mayordomo
def concept_1():
    im, d = new(); shoulders(d, 25); robot_head(d, 6)
    eye(d, 9, 11); eye(d, 18, 11)
    d.line([(13, 18), (18, 18)], fill=INK); d.point((12, 17), fill=INK); d.point((19, 17), fill=INK)
    d.rectangle([8, 16, 9, 17], fill=PINK); d.rectangle([22, 16, 23, 17], fill=PINK)
    bowtie(d, 15, 26)
    return im

# ----------------------------------------------------------------------------- 2 · Pajarita con lente
def concept_2():
    im, d = new()
    d.line([(15, 4), (15, 9)], fill=INK); d.line([(16, 4), (16, 9)], fill=INK)
    d.ellipse([13, 1, 18, 6], fill=GOLD, outline=INK); d.point((14, 2), fill=GOLD_L)
    d.polygon([(2, 9), (14, 17), (2, 26)], fill=GOLD, outline=INK)
    d.polygon([(29, 9), (17, 17), (29, 26)], fill=GOLD, outline=INK)
    d.line([(4, 12), (11, 16)], fill=GOLD_L); d.line([(27, 12), (20, 16)], fill=GOLD_L)
    d.line([(4, 23), (11, 19)], fill=GOLD_D); d.line([(27, 23), (20, 19)], fill=GOLD_D)
    for p in ((6, 15), (6, 20), (25, 15), (25, 20)): d.point(p, fill=INK)              # remaches
    d.ellipse([10, 11, 21, 22], fill=INK)                                              # lente
    d.ellipse([11, 12, 20, 21], fill=BLUE_D); d.ellipse([12, 13, 19, 20], fill=CYAN)
    d.ellipse([14, 15, 17, 18], fill=INK); d.rectangle([12, 13, 13, 14], fill=WHITE)
    return im

# ----------------------------------------------------------------------------- 3 · Monóculo
def concept_3():
    im, d = new()
    d.ellipse([3, 3, 25, 25], fill=GOLD, outline=INK); d.ellipse([6, 6, 22, 22], fill=INK)
    d.arc([4, 4, 24, 24], 200, 250, fill=GOLD_L, width=1)
    d.ellipse([7, 7, 21, 21], fill=CYAN); d.ellipse([9, 9, 19, 19], fill=CYAN_D); d.ellipse([11, 11, 17, 17], fill=INK)
    d.ellipse([12, 12, 14, 14], fill=WHITE); d.point((16, 16), fill=CYAN)
    d.rectangle([7, 8, 9, 9], fill=WHITE)
    d.line([(22, 23), (25, 26), (26, 29)], fill=INK); d.line([(23, 23), (26, 26), (27, 29)], fill=GOLD)
    d.rectangle([13, 0, 14, 2], fill=INK)
    return im

# ----------------------------------------------------------------------------- 4 · Bombín
def concept_4():
    im, d = new(); shoulders(d, 26); robot_head(d, 9)
    rect(d, 9, 15, 22, 20, INK)                                                        # visor
    d.rectangle([11, 16, 13, 18], fill=CYAN); d.rectangle([18, 16, 20, 18], fill=CYAN); d.point((11, 16), fill=WHITE); d.point((18, 16), fill=WHITE)
    d.line([(14, 22), (17, 22)], fill=INK)
    d.ellipse([8, 1, 23, 11], fill=INK); d.ellipse([9, 2, 22, 10], fill=HAT); d.rectangle([9, 6, 22, 10], fill=HAT)
    d.arc([10, 3, 20, 9], 200, 280, fill=HAT_L, width=1)
    d.rectangle([9, 7, 22, 8], fill=GOLD); d.line([(9, 8), (22, 8)], fill=GOLD_D)
    d.rectangle([5, 10, 26, 11], fill=INK); d.line([(6, 10), (25, 10)], fill=HAT)
    bowtie(d, 15, 27, w=4, h=2)
    return im

CONCEPTS = [("1", "Mayordomo", concept_1), ("2", "Pajarita-lente", concept_2), ("3", "Monoculo", concept_3), ("4", "Bombin", concept_4)]

def masked(im, shape, size):
    big = im.resize((size, size), Image.NEAREST)
    m = Image.new("L", (size, size), 0); dm = ImageDraw.Draw(m)
    if shape == "circle": dm.ellipse([0, 0, size - 1, size - 1], fill=255)
    else: dm.rounded_rectangle([0, 0, size - 1, size - 1], radius=size // 4, fill=255)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0)); out.paste(big, (0, 0), m); return out

if __name__ == "__main__":
    out = sys.argv[1] if len(sys.argv) > 1 else "."
    os.makedirs(out, exist_ok=True)
    cw = 296
    sheet = Image.new("RGB", (len(CONCEPTS) * cw + 20, 560), "#FFFFFF"); d = ImageDraw.Draw(sheet)
    for i, (k, name, f) in enumerate(CONCEPTS):
        im = f(); im.resize((256, 256), Image.NEAREST).save(os.path.join(out, f"logo_{k}.png")); im.resize((N * S, N * S), Image.NEAREST).save(os.path.join(out, f"logo_{k}_grande.png"))
        x = 20 + i * cw
        big = im.resize((256, 256), Image.NEAREST); sheet.paste(big, (x, 16))
        d.rectangle([x - 1, 15, x + 256, 272], outline=INK)
        c = masked(im, "circle", 112); sheet.paste(c, (x, 300), c)
        q = masked(im, "squircle", 112); sheet.paste(q, (x + 128, 300), q)
        s1 = masked(im, "circle", 48); sheet.paste(s1, (x, 430), s1)
        s2 = masked(im, "circle", 72); sheet.paste(s2, (x + 64, 430), s2)
        d.text((x, 500), f"{k} - {name}", fill=INK)
    sheet.save(os.path.join(out, "logos_hoja.png"))
