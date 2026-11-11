#!/usr/bin/env python3
"""Genera los iconos pixel-art de la barra inferior (rejilla de 32x32, ampliados x8 sin suavizar).
Uso: python3 tools/make_icons.py   ->   app/src/main/res/drawable-nodpi/px_*.png"""
from PIL import Image, ImageDraw
import os

INK = "#1B1740"; WHITE = "#FFFFFF"; PAPER = "#FFF3CF"; PAPER_D = "#E9D79E"
GOLD = "#F2B705"; GOLD_D = "#A66F00"; GOLD_L = "#FFE27A"
BLUE = "#2F6FE4"; BLUE_L = "#8DB6FF"; BLUE_D = "#1A3F94"
RED = "#E5484D"; RED_D = "#A32A31"; RED_L = "#FF8C8F"
HAIR = "#4A3426"; HAIR_L = "#85624A"; SKIN = "#FFD6AE"; SKIN_D = "#E4A877"; GREEN = "#2EAD6B"; GREY = "#C9C5DE"; GREY_D = "#8F8AB0"
S = 8  # factor de ampliación

def canvas():
    im = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    return im, ImageDraw.Draw(im)

def box(d, x0, y0, x1, y1, fill, line=INK):
    d.rectangle([x0, y0, x1, y1], fill=line)
    d.rectangle([x0 + 1, y0 + 1, x1 - 1, y1 - 1], fill=fill)

def save(im, name):
    out = os.path.join("app/src/main/res/drawable-nodpi", f"px_{name}.png")
    im.resize((32 * S, 32 * S), Image.NEAREST).save(out)

def mail():
    im, d = canvas()
    d.rectangle([5, 25, 29, 26], fill=GREY)                       # sombra
    box(d, 2, 7, 28, 24, WHITE)
    d.rectangle([3, 21, 27, 23], fill=GREY)                        # base sombreada
    d.polygon([(3, 8), (27, 8), (15, 18)], fill=RED)               # solapa
    d.polygon([(3, 8), (27, 8), (15, 18)], outline=INK)
    d.line([(4, 9), (26, 9)], fill=RED_L)
    d.line([(3, 23), (12, 15)], fill=INK); d.line([(27, 23), (18, 15)], fill=INK)   # pliegues
    d.line([(15, 18), (15, 18)], fill=RED_D)
    d.rectangle([13, 12, 17, 13], fill=RED_D)
    return im

def calendar():
    im, d = canvas()
    d.rectangle([6, 28, 28, 29], fill=GREY)
    box(d, 3, 5, 27, 27, WHITE)
    d.rectangle([4, 6, 26, 12], fill=RED); d.rectangle([4, 6, 26, 7], fill=RED_L)
    d.line([(4, 12), (26, 12)], fill=RED_D)
    for x in (9, 20):                                              # anillas
        d.rectangle([x, 2, x + 2, 8], fill=INK); d.rectangle([x + 1, 3, x + 1, 7], fill=GREY)
    ys = (15, 19, 23)
    xs = (6, 11, 16, 21)
    for yi, y in enumerate(ys):
        for xi, x in enumerate(xs):
            if (yi, xi) == (1, 2):
                d.rectangle([x - 1, y - 1, x + 3, y + 3], fill=INK); d.rectangle([x, y, x + 2, y + 2], fill=GOLD)
            else:
                d.rectangle([x, y, x + 2, y + 2], fill=BLUE if (yi + xi) % 2 == 0 else BLUE_L)
    return im

def notes():
    im, d = canvas()
    d.rectangle([8, 29, 26, 30], fill=GREY)
    box(d, 4, 3, 24, 28, PAPER)
    d.rectangle([5, 4, 23, 9], fill=BLUE); d.rectangle([5, 4, 23, 5], fill=BLUE_L)
    d.line([(5, 9), (23, 9)], fill=BLUE_D)
    for x in (8, 13, 18):
        d.rectangle([x, 2, x + 1, 7], fill=INK); d.point((x, 3), fill=GREY)
    for i, y in enumerate((13, 17, 21, 25)):
        d.line([(7, y), (20 if i < 3 else 13, y)], fill=GREY_D)
    d.line([(5, 27), (23, 27)], fill=PAPER_D)
    # lápiz
    d.polygon([(17, 26), (26, 17), (29, 20), (20, 29)], fill=GOLD, outline=INK)
    d.line([(19, 26), (27, 18)], fill=GOLD_L)
    d.polygon([(17, 26), (20, 29), (15, 30)], fill=SKIN, outline=INK); d.point((15, 30), fill=INK)
    d.polygon([(26, 17), (28, 15), (31, 18), (29, 20)], fill=RED, outline=INK)
    return im

def contacts():
    im, d = canvas()
    d.rectangle([8, 29, 28, 30], fill=GREY)
    for y, c in ((6, RED), (12, GOLD), (18, GREEN)):               # pestañas
        box(d, 25, y, 29, y + 4, c)
    box(d, 3, 2, 26, 28, BLUE)
    d.rectangle([4, 3, 25, 4], fill=BLUE_L); d.rectangle([4, 26, 25, 27], fill=BLUE_D)
    d.rectangle([6, 4, 6, 26], fill=BLUE_D)                        # lomo
    box(d, 9, 7, 22, 23, WHITE)
    d.ellipse([12, 9, 19, 16], fill=SKIN, outline=INK)             # cabeza
    d.rectangle([14, 12, 14, 12], fill=INK); d.rectangle([17, 12, 17, 12], fill=INK)
    d.pieslice([10, 16, 21, 28], 180, 360, fill=BLUE_L, outline=INK)  # hombros
    d.rectangle([10, 22, 21, 22], fill=WHITE)
    return im

def alfred():
    im, d = canvas()
    # chaqueta y pajarita
    d.polygon([(3, 31), (4, 24), (11, 20), (21, 20), (28, 24), (29, 31)], fill=BLUE, outline=INK)
    d.polygon([(12, 20), (20, 20), (16, 31)], fill=WHITE, outline=INK)            # camisa
    d.line([(11, 21), (13, 27)], fill=BLUE_D); d.line([(21, 21), (19, 27)], fill=BLUE_D)
    d.rectangle([15, 25, 16, 26], fill=GOLD); d.rectangle([15, 28, 16, 29], fill=GOLD)
    # cuello y cabeza
    d.rectangle([13, 17, 19, 20], fill=SKIN_D, outline=INK)
    d.ellipse([8, 3, 24, 20], fill=SKIN, outline=INK)
    d.rectangle([9, 14, 10, 15], fill=SKIN_D); d.rectangle([22, 14, 23, 15], fill=SKIN_D)
    # pelo
    d.pieslice([7, 1, 25, 15], 180, 360, fill=HAIR, outline=INK)
    d.rectangle([9, 6, 23, 7], fill=HAIR); d.rectangle([9, 8, 10, 9], fill=HAIR); d.rectangle([22, 8, 23, 9], fill=HAIR)
    d.line([(12, 4), (18, 4)], fill=HAIR_L); d.line([(11, 5), (14, 6)], fill=HAIR_L)
    # cara
    for x in (12, 19): d.rectangle([x, 11, x + 1, 13], fill=INK)
    d.rectangle([12, 9, 14, 9], fill=HAIR); d.rectangle([18, 9, 20, 9], fill=HAIR)       # cejas
    d.ellipse([17, 9, 22, 15], outline=GOLD_D)                                         # monóculo
    d.line([(22, 14), (24, 19)], fill=GOLD)                                            # cadena
    d.rectangle([13, 16, 19, 16], fill=INK)                                           # bigote
    d.rectangle([14, 17, 18, 17], fill=RED_D)
    # pajarita
    d.polygon([(9, 21), (15, 23), (9, 26)], fill=RED, outline=INK)
    d.polygon([(23, 21), (17, 23), (23, 26)], fill=RED, outline=INK)
    d.rectangle([15, 22, 17, 24], fill=RED_D, outline=INK)
    return im

if __name__ == "__main__":
    os.makedirs("app/src/main/res/drawable-nodpi", exist_ok=True)
    for n, f in (("mail", mail), ("calendar", calendar), ("notes", notes), ("contacts", contacts), ("alfred", alfred)):
        save(f(), n)
    print("ok")
