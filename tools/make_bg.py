#!/usr/bin/env python3
"""Genera la textura de fondo (baldosa repetible de 512x512) con iconos de juego casi invisibles.
Uso: python3 tools/make_bg.py   ->   app/src/main/res/drawable-nodpi/bg_pattern.png"""
from PIL import Image, ImageDraw
import math

T = 512; K = 4; ALPHA = 0.075            # opacidad final de los trazos
INK = (31, 29, 43)

def icon(kind, s=160):
    im = Image.new("L", (s * K // 2, s * K // 2), 0); d = ImageDraw.Draw(im); n = s * K // 2; w = max(6, n // 22)
    c = n // 2
    if kind == "ball":
        d.ellipse([w, w, n - w, n - w], outline=255, width=w); d.line([(w, c), (n - w, c)], fill=255, width=w)
        d.ellipse([c - n // 7, c - n // 7, c + n // 7, c + n // 7], fill=0, outline=255, width=w)
    elif kind == "star":
        pts = []
        for i in range(10):
            r = (n // 2 - w) if i % 2 == 0 else (n // 5)
            a = -math.pi / 2 + i * math.pi / 5; pts.append((c + r * math.cos(a), c + r * math.sin(a)))
        d.polygon(pts, outline=255, width=w)
    elif kind == "bolt":
        d.polygon([(c + n // 10, w), (c - n // 4, c + n // 12), (c, c + n // 12), (c - n // 8, n - w), (c + n // 3, c - n // 8), (c + n // 28, c - n // 8)], outline=255, width=w)
    elif kind == "pad":
        d.rounded_rectangle([w, n // 4, n - w, n - n // 4], radius=n // 5, outline=255, width=w)
        d.line([(n // 4, c - n // 12), (n // 4, c + n // 12)], fill=255, width=w); d.line([(n // 4 - n // 12, c), (n // 4 + n // 12, c)], fill=255, width=w)
        d.ellipse([n - n // 3 - w, c - w * 2, n - n // 3 + w, c], outline=255, width=w // 2 + 1); d.ellipse([n - n // 4 - w * 2, c, n - n // 4, c + w * 2], outline=255, width=w // 2 + 1)
    elif kind == "cup":
        d.polygon([(n // 4, w), (n - n // 4, w), (n - n // 4 - n // 16, c + n // 8), (c + n // 12, c + n // 4), (c - n // 12, c + n // 4), (n // 4 + n // 16, c + n // 8)], outline=255, width=w)
        d.arc([w, w * 2, n // 3, c], 90, 270, fill=255, width=w); d.arc([n - n // 3, w * 2, n - w, c], 270, 90, fill=255, width=w)
        d.line([(c, c + n // 4), (c, n - n // 6)], fill=255, width=w); d.rectangle([n // 3, n - n // 6, n - n // 3, n - w], outline=255, width=w)
    elif kind == "heart":
        d.ellipse([w, n // 5, c, c + n // 6], outline=255, width=w); d.ellipse([c, n // 5, n - w, c + n // 6], outline=255, width=w)
        d.polygon([(w * 2, c - n // 20), (c, n - w), (n - w * 2, c - n // 20)], outline=255, width=w)
    return im

layout = [("ball", 40, 30, -15), ("star", 250, 20, 10), ("bolt", 430, 60, 12), ("pad", 120, 170, 8), ("cup", 330, 190, -8),
          ("bolt", 20, 330, -10), ("ball", 230, 360, 20), ("star", 410, 340, -14), ("bolt", 90, 450, -10), ("pad", 330, 440, 6)]
mask = Image.new("L", (T * K, T * K), 0)
for kind, x, y, rot in layout:
    ic = icon(kind, 110).rotate(rot, expand=True, resample=Image.BICUBIC)
    for dx in (-T, 0, T):                      # repetible: lo que se sale por un borde entra por el opuesto
        for dy in (-T, 0, T):
            mask.paste(255, ((x + dx) * K, (y + dy) * K), ic)
mask = mask.resize((T, T), Image.LANCZOS).point(lambda v: int(v * ALPHA))
out = Image.new("RGBA", (T, T), INK + (0,)); out.putalpha(mask)
out.save("app/src/main/res/drawable-nodpi/bg_pattern.png", optimize=True)
print("ok")
