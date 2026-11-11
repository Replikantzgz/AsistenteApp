#!/usr/bin/env python3
"""Propuestas de logo de Alfred (pixel-art 48x48, ampliado x8 sin suavizar).
Uso: python3 tools/make_logo.py <carpeta_salida>"""
from PIL import Image, ImageDraw
import sys, os

INK = "#1B1740"; NAVY2 = "#2B2760"; WHITE = "#FFFFFF"
STEEL_L = "#EEF2FB"; STEEL = "#BCC7E0"; STEEL_D = "#7C89AD"; STEEL_DD = "#55618A"
GOLD = "#F2B705"; GOLD_L = "#FFE27A"; GOLD_D = "#A66F00"
BLUE = "#2F6FE4"; BLUE_L = "#8DB6FF"; BLUE_D = "#1A3F94"
CYAN = "#62E8FF"; CYAN_D = "#139AC0"; RED = "#E5484D"; RED_D = "#A32A31"
N = 48; S = 8

def new():
    im = Image.new("RGBA", (N, N), INK); d = ImageDraw.Draw(im)
    for y in range(1, N, 6):
        for x in range(1 + (y // 6 % 2) * 3, N, 6): d.point((x, y), fill=NAVY2)
    return im, d

def halo(d):
    d.arc([3, 3, 44, 44], 90, 270, fill=GOLD, width=2)
    d.arc([3, 3, 44, 44], 270, 90, fill=BLUE, width=2)
    d.arc([5, 5, 42, 42], 100, 260, fill=GOLD_D, width=1)
    d.arc([5, 5, 42, 42], 280, 80, fill=BLUE_D, width=1)

def rect(d, x0, y0, x1, y1, fill, line=INK):
    d.rectangle([x0, y0, x1, y1], fill=line); d.rectangle([x0 + 1, y0 + 1, x1 - 1, y1 - 1], fill=fill)

def chamfer(d, x0, y0, x1, y1, fill, c=3, line=INK):
    """Rectángulo con esquinas cortadas en diagonal (cabezas de robot más amables que un monitor)."""
    pts = lambda i: [(x0 + c - i, y0 - i), (x1 - c + i, y0 - i), (x1 + i, y0 + c - i), (x1 + i, y1 - c + i),
                     (x1 - c + i, y1 + i), (x0 + c - i, y1 + i), (x0 - i, y1 - c + i), (x0 - i, y0 + c - i)]
    d.polygon(pts(1), fill=line); d.polygon(pts(0), fill=fill)

def bowtie(d, cx, cy, c=GOLD, hi=GOLD_L, lo=GOLD_D):
    d.polygon([(cx - 8, cy - 4), (cx - 1, cy), (cx - 8, cy + 4)], fill=c, outline=INK)
    d.polygon([(cx + 8, cy - 4), (cx + 1, cy), (cx + 8, cy + 4)], fill=c, outline=INK)
    d.line([(cx - 7, cy - 2), (cx - 3, cy - 1)], fill=hi); d.line([(cx + 7, cy - 2), (cx + 3, cy - 1)], fill=hi)
    rect(d, cx - 2, cy - 2, cx + 2, cy + 2, lo)
    d.point((cx, cy), fill=hi)

def suit(d, top=33):
    d.polygon([(7, 47), (9, top + 3), (17, top - 1), (31, top - 1), (39, top + 3), (41, 47)], fill=BLUE, outline=INK)
    d.polygon([(18, top - 1), (30, top - 1), (24, 47)], fill=WHITE, outline=INK)
    d.line([(16, top), (21, 44)], fill=BLUE_D); d.line([(32, top), (27, 44)], fill=BLUE_D)
    d.line([(10, top + 4), (14, top + 2)], fill=BLUE_L)
    # circuitos sutiles en la solapa
    for (x, y) in ((12, 41), (36, 41)): d.point((x, y), fill=CYAN)
    d.line([(12, 42), (12, 45)], fill=BLUE_L); d.line([(36, 42), (36, 45)], fill=BLUE_L)
    d.rectangle([23, 41, 24, 42], fill=GOLD); d.rectangle([23, 45, 24, 46], fill=GOLD)

def led(d, x, y, big=False):
    if big:
        d.rectangle([x - 1, y - 1, x + 2, y + 2], fill=CYAN_D); d.rectangle([x, y, x + 1, y + 1], fill=CYAN); d.point((x, y), fill=WHITE)
    else:
        d.rectangle([x, y, x + 1, y + 1], fill=CYAN); d.point((x, y), fill=WHITE)

# ----------------------------------------------------------------------------- A · Monóculo
def concept_a():
    im, d = new(); halo(d); suit(d, 34)
    d.line([(24, 6), (24, 10)], fill=STEEL_DD); d.rectangle([23, 6, 23, 10], fill=INK); d.rectangle([25, 6, 25, 10], fill=INK)
    d.ellipse([21, 2, 26, 7], fill=GOLD, outline=INK); d.point((23, 3), fill=GOLD_L)
    rect(d, 21, 30, 27, 35, STEEL_D); d.line([(22, 32), (26, 32)], fill=STEEL_DD)
    for x0 in (10, 36):                                                                # orejas
        rect(d, x0, 17, x0 + 2, 24, GOLD); d.line([(x0 + 1, 19), (x0 + 1, 22)], fill=GOLD_D)
    chamfer(d, 12, 9, 35, 31, STEEL, c=4)
    d.line([(16, 10), (31, 10)], fill=STEEL_L); d.line([(13, 14), (13, 27)], fill=STEEL_L)
    d.line([(34, 15), (34, 27)], fill=STEEL_D); d.line([(16, 30), (31, 30)], fill=STEEL_D)
    d.line([(24, 11), (24, 13)], fill=STEEL_D)
    rect(d, 15, 15, 32, 24, "#10102E")
    led(d, 18, 18, big=True)
    d.ellipse([24, 15, 31, 23], outline=GOLD, width=1); d.arc([24, 15, 31, 23], 200, 250, fill=GOLD_L, width=1)
    led(d, 27, 18, big=True)
    d.line([(31, 23), (33, 28), (33, 34)], fill=GOLD)
    # bigote clásico, fino
    d.line([(17, 27), (22, 27)], fill=INK); d.point((16, 26), fill=INK); d.point((23, 26), fill=INK)
    d.line([(25, 27), (30, 27)], fill=INK); d.point((24, 26), fill=INK); d.point((31, 26), fill=INK)
    d.line([(21, 28), (26, 28)], fill=STEEL_DD)
    bowtie(d, 24, 37)
    return im

# ----------------------------------------------------------------------------- B · Bombín
def concept_b():
    im, d = new(); halo(d); suit(d, 36)
    rect(d, 21, 32, 27, 37, STEEL_D); d.line([(22, 34), (26, 34)], fill=STEEL_DD)
    d.ellipse([11, 14, 36, 34], fill=STEEL, outline=INK)                               # cabeza
    d.arc([13, 16, 34, 32], 170, 250, fill=STEEL_L, width=2); d.arc([13, 16, 34, 32], 340, 60, fill=STEEL_D, width=2)
    # bombín: cúpula baja y redondeada, ala ancha, cinta dorada
    HAT = "#3A3578"; HAT_L = "#6C66C4"
    d.ellipse([13, 8, 34, 19], fill=INK); d.ellipse([14, 9, 33, 18], fill=HAT)
    d.rectangle([14, 13, 33, 17], fill=HAT)
    d.arc([15, 10, 31, 17], 200, 280, fill=HAT_L, width=2)
    d.rectangle([14, 14, 33, 16], fill=GOLD); d.line([(15, 14), (32, 14)], fill=GOLD_L); d.line([(14, 16), (33, 16)], fill=GOLD_D)
    d.rectangle([8, 17, 39, 19], fill=INK); d.line([(9, 17), (38, 17)], fill=HAT)      # ala
    rect(d, 14, 21, 33, 27, "#10102E")                                                 # visor
    led(d, 18, 23); led(d, 28, 23)
    d.line([(21, 24), (25, 24)], fill=CYAN_D)
    d.rectangle([13, 29, 14, 30], fill=RED_D); d.rectangle([33, 29, 34, 30], fill=RED_D)
    d.line([(20, 30), (27, 30)], fill=INK); d.point((19, 29), fill=INK); d.point((28, 29), fill=INK)
    bowtie(d, 24, 39)
    return im

# ----------------------------------------------------------------------------- C · Monograma A
def concept_c():
    im, d = new(); halo(d)
    left = [(21, 9), (27, 9), (20, 41), (13, 41)]; right = [(21, 9), (27, 9), (35, 41), (28, 41)]
    for poly in (left, right): d.polygon(poly, fill=GOLD, outline=INK)
    d.line([(22, 10), (14, 40)], fill=GOLD_L); d.line([(23, 10), (16, 40)], fill=GOLD_L)
    d.line([(26, 12), (33, 40)], fill=GOLD_D); d.line([(25, 12), (31, 40)], fill=GOLD_D)
    d.rectangle([21, 8, 27, 9], fill=INK)
    # ojo robot en el vértice
    d.ellipse([19, 12, 29, 22], fill=INK); d.ellipse([20, 13, 28, 21], fill="#10102E")
    led(d, 23, 16, big=True); d.ellipse([19, 12, 29, 22], outline=GOLD_L, width=1)
    # travesaño: pajarita azul
    bowtie(d, 24, 33, c=BLUE, hi=BLUE_L, lo=BLUE_D)
    d.line([(10, 44), (38, 44)], fill=GOLD_D); d.line([(14, 46), (34, 46)], fill=BLUE_D)
    return im

def save(im, path, scale=S):
    im.resize((im.width * scale, im.height * scale), Image.NEAREST).save(path)

def masked(im, shape, size):
    big = im.resize((size, size), Image.NEAREST)
    m = Image.new("L", (size, size), 0); dm = ImageDraw.Draw(m)
    if shape == "circle": dm.ellipse([0, 0, size - 1, size - 1], fill=255)
    else: dm.rounded_rectangle([0, 0, size - 1, size - 1], radius=size // 4, fill=255)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0)); out.paste(big, (0, 0), m); return out

if __name__ == "__main__":
    out = sys.argv[1] if len(sys.argv) > 1 else "."
    os.makedirs(out, exist_ok=True)
    concepts = [("A", "Monoculo", concept_a()), ("B", "Bombin", concept_b())]
    for k, _, im in concepts: save(im, os.path.join(out, f"logo_{k}.png"))
    # hoja de contacto: grande + círculo + cuadrado redondeado + tamaño real de lanzador
    W = 2 * 420 + 40; H = 700
    sheet = Image.new("RGB", (W, H), "#F6F1E6"); d = ImageDraw.Draw(sheet)
    for i, (k, name, im) in enumerate(concepts):
        x = 20 + i * 420
        sheet.paste(im.resize((384, 384), Image.NEAREST), (x, 20))
        sheet.paste(masked(im, "circle", 150), (x, 420), masked(im, "circle", 150))
        sheet.paste(masked(im, "squircle", 150), (x + 170, 420), masked(im, "squircle", 150))
        sm = masked(im, "circle", 48); sheet.paste(sm, (x + 340, 450), sm)
        sm2 = masked(im, "circle", 96); sheet.paste(sm2, (x + 300, 520), sm2)
        d.text((x, 590), f"{k} · {name}", fill=INK)
    sheet.save(os.path.join(out, "logos_hoja.png"))
