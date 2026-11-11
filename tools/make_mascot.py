#!/usr/bin/env python3
"""Alfred, mascota en pixel-art suave (rejilla 64x64, ampliada sin suavizar).
Uso: python3 tools/make_mascot.py <carpeta_salida>"""
from PIL import Image, ImageDraw, ImageFont, ImageChops
import sys, os

OL = "#2B2540"
HEAD_L = "#F4F7FB"; HEAD = "#D9E1EC"; HEAD_D = "#AAB6C9"; HEAD_DD = "#808DA6"
PLATE = "#FFF8EC"; PLATE_D = "#EEDFC4"; BLUSH = "#F7A9A2"
GOLD = "#E9AE2F"; GOLD_L = "#FFDB80"; GOLD_D = "#A86F12"
SLATE = "#4E6FA8"; SLATE_L = "#7F9DD3"; SLATE_D = "#34497A"
LEG = "#2F2A4A"; SHOE = "#5A3B2E"; SHOE_L = "#8F5E45"
GLOW = "#7EE8F7"; GLOW_L = "#DCFCFF"; GLOW_D = "#2AB5CF"; GLOW_BG = "#D5F6FB"
SILVER = "#C5CEDC"; SILVER_D = "#8F9BB3"
BG = "#FFFFFF"
N = 64

def ell(d, box, fill, outline=None):
    x0, y0, x1, y1 = box
    if outline: d.ellipse([x0 - 1, y0 - 1, x1 + 1, y1 + 1], fill=outline)
    d.ellipse(box, fill=fill)

def rr(d, box, fill, outline=OL, r=1):
    x0, y0, x1, y1 = box
    d.rounded_rectangle([x0 - 1, y0 - 1, x1 + 1, y1 + 1], radius=r + 1, fill=outline)
    d.rounded_rectangle(box, radius=r, fill=fill)

def poly(d, pts, fill, outline=OL):
    d.polygon(pts, fill=fill); d.line(pts + [pts[0]], fill=outline, width=1)

def mascot(bg=BG, glow_bg=GLOW_BG):
    im = Image.new("RGBA", (N, N), bg); d = ImageDraw.Draw(im)
    # --- cola del frac y piernas
    poly(d, [(21, 52), (28, 52), (26, 59), (19, 58)], SLATE_D); poly(d, [(36, 52), (43, 52), (45, 58), (38, 59)], SLATE_D)
    for x in (24, 35): rr(d, [x, 56, x + 4, 60], LEG, OL, 0)
    for (x0, x1) in ((20, 29), (34, 43)):
        ell(d, [x0, 59, x1, 63], SHOE, OL); d.line([(x0 + 2, 60), (x1 - 3, 60)], fill=SHOE_L)
    # --- cuerpo (frac)
    poly(d, [(19, 40), (45, 40), (47, 54), (17, 54)], SLATE)
    d.polygon([(20, 41), (25, 41), (23, 53), (19, 53)], fill=SLATE_L); d.line([(44, 42), (46, 53)], fill=SLATE_D)
    d.polygon([(27, 40), (37, 40), (32, 53)], fill=PLATE); d.line([(27, 40), (32, 53), (37, 40)], fill=OL)
    d.line([(27, 41), (24, 46)], fill=SLATE_D); d.line([(37, 41), (40, 46)], fill=SLATE_D)    # solapas
    for y in (47, 51): d.rectangle([31, y, 32, y + 1], fill=GOLD); d.point((31, y), fill=GOLD_L)
    # --- brazo izquierdo (colgando) con guante
    poly(d, [(15, 42), (19, 41), (19, 51), (15, 52)], SLATE); d.line([(16, 43), (16, 50)], fill=SLATE_L)
    d.rectangle([15, 50, 19, 51], fill=PLATE)
    ell(d, [14, 52, 19, 56], HEAD_L, OL)
    # --- brazo derecho con bandeja y orbe luminoso
    poly(d, [(44, 42), (48, 42), (54, 49), (51, 52), (45, 47)], SLATE); d.line([(46, 43), (51, 49)], fill=SLATE_L)
    ell(d, [51, 50, 57, 55], HEAD_L, OL)
    rr(d, [46, 47, 62, 49], SILVER, OL, 0); d.line([(47, 48), (61, 48)], fill=HEAD_L)
    d.rectangle([52, 49, 56, 49], fill=SILVER_D)
    ell(d, [49, 33, 59, 45], GLOW_BG)                                                          # halo
    for (x, y) in ((50, 34), (58, 34), (49, 40), (59, 40), (54, 32), (54, 46)): d.point((x, y), fill=GLOW_D)
    ell(d, [51, 36, 58, 44], GLOW_D, OL); ell(d, [52, 37, 57, 43], GLOW); ell(d, [53, 38, 55, 40], GLOW_L); d.point((56, 42), fill=GLOW_L)
    # --- cuello
    rr(d, [29, 38, 35, 41], HEAD_DD, OL, 0)
    # --- orejas (discos dorados)
    for (x0, x1) in ((8, 14), (50, 56)):
        ell(d, [x0, 20, x1, 28], GOLD, OL); ell(d, [x0 + 1, 21, x1 - 2, 25], GOLD_L); d.point(((x0 + x1) // 2, 24), fill=GOLD_D)
    # --- cabeza (tres tonos)
    ell(d, [14, 11, 50, 39], HEAD_DD, OL)
    ell(d, [14, 11, 49, 38], HEAD_D)
    ell(d, [14, 11, 47, 36], HEAD)
    ell(d, [16, 12, 42, 31], HEAD_L)
    ell(d, [18, 13, 38, 25], "#FFFFFF")
    # placa de la cara
    rr(d, [19, 18, 44, 35], PLATE, PLATE_D, 6)
    d.line([(22, 34), (41, 34)], fill=PLATE_D)
    # ojos de punto, rubor y boca
    for x in (24, 38):
        d.rectangle([x, 24, x + 2, 28], fill=OL); d.point((x, 24), fill="#FFFFFF"); d.point((x + 1, 25), fill="#6E6A95")
    ell(d, [20, 29, 23, 31], BLUSH); ell(d, [41, 29, 44, 31], BLUSH)
    d.line([(30, 30), (33, 30)], fill=OL); d.point((29, 29), fill=OL); d.point((34, 29), fill=OL)
    # junta y tornillos
    d.line([(32, 12), (32, 15)], fill=HEAD_D); d.point((19, 15), fill=HEAD_DD); d.point((44, 15), fill=HEAD_DD)
    # --- pajarita
    poly(d, [(23, 41), (31, 44), (23, 48)], GOLD); poly(d, [(41, 41), (33, 44), (41, 48)], GOLD)
    d.line([(25, 42), (29, 44)], fill=GOLD_L); d.line([(39, 42), (35, 44)], fill=GOLD_L)
    d.line([(25, 46), (29, 45)], fill=GOLD_D); d.line([(39, 46), (35, 45)], fill=GOLD_D)
    rr(d, [30, 43, 34, 45], GOLD_D, OL, 0); d.point((31, 43), fill=GOLD_L)
    # --- antena con luz
    d.line([(31, 5), (31, 11)], fill=OL); d.line([(32, 5), (32, 11)], fill=OL); d.line([(31, 6), (31, 10)], fill=HEAD_D)
    ell(d, [28, 0, 35, 6], GLOW_D, OL); ell(d, [29, 1, 34, 5], GLOW); d.point((30, 2), fill=GLOW_L); d.point((31, 2), fill=GLOW_L)
    return ImageChops.offset(im, -2, 0)

def wordmark(width, scale):
    f = ImageFont.truetype("app/src/main/res/font/pressstart2p.ttf", 8 * scale)
    im = Image.new("RGB", (width, 12 * scale), BG); d = ImageDraw.Draw(im); d.fontmode = "1"
    t = "ALFRED"; w = d.textlength(t, font=f)
    d.text(((width - w) / 2 + scale, scale), t, font=f, fill="#4B2F2A")                   # sombra
    d.text(((width - w) / 2, 0), t, font=f, fill=OL)
    return im

if __name__ == "__main__":
    out = sys.argv[1] if len(sys.argv) > 1 else "."
    os.makedirs(out, exist_ok=True)
    m = mascot()
    S = 8
    m.resize((N * S, N * S), Image.NEAREST).save(os.path.join(out, "mascota.png"))
    # lámina de presentación
    sheet = Image.new("RGB", (760, 820), BG)
    sheet.paste(m.resize((N * 7, N * 7), Image.NEAREST), (156, 20))
    sheet.paste(wordmark(520, 5), (120, 490)); d = ImageDraw.Draw(sheet)
    cream = mascot(bg="#FFF4D6", glow_bg="#E6F8F3")
    for i, size in enumerate((96, 64, 48)):
        for j, (bgc, shape) in enumerate((("#FFF4D6", "circle"), ("#FFFFFF", "squircle"))):
            im = mascot(bg=bgc, glow_bg="#E6F8F3" if bgc != "#FFFFFF" else GLOW_BG).resize((size, size), Image.NEAREST)
            mk = Image.new("L", (size, size), 0); dm = ImageDraw.Draw(mk)
            if shape == "circle": dm.ellipse([0, 0, size - 1, size - 1], fill=255)
            else: dm.rounded_rectangle([0, 0, size - 1, size - 1], radius=size // 4, fill=255)
            sheet.paste(im.convert("RGB"), (60 + i * 120 + j * 360, 620), mk)
    sheet.save(os.path.join(out, "mascota_hoja.png"))
