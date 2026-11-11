#!/usr/bin/env python3
"""Icono provisional de Drive: triángulo dividido en tres colores, con contorno. Se sustituirá por la ilustración definitiva.
Uso: python3 tools/make_drive_icon.py   ->   app/src/main/res/drawable-nodpi/art_drive.png"""
from PIL import Image, ImageDraw

K = 4; N = 384; S = N * K
INK = "#1F1D2B"; GREEN = "#1FA463"; YELLOW = "#FFCB2E"; BLUE = "#3B82F6"
def P(x, y): return (x * S / 400, y * S / 400)
A, B, C = P(200, 52), P(48, 322), P(352, 322)
G = ((A[0] + B[0] + C[0]) / 3, (A[1] + B[1] + C[1]) / 3)
im = Image.new("RGBA", (S, S), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
for tri, col in (((A, B, G), GREEN), ((A, C, G), YELLOW), ((B, C, G), BLUE)):
    d.polygon(tri, fill=col)
w = int(8 * K)
for tri in ((A, B, G), (A, C, G), (B, C, G)): d.line(list(tri) + [tri[0]], fill=INK, width=w, joint="curve")
d.line([A, B, C, A], fill=INK, width=int(11 * K), joint="curve")
for p in (A, B, C): d.ellipse([p[0] - 5.5 * K, p[1] - 5.5 * K, p[0] + 5.5 * K, p[1] + 5.5 * K], fill=INK)   # esquinas redondeadas
im.resize((N, N), Image.LANCZOS).save("app/src/main/res/drawable-nodpi/art_drive.png", optimize=True)
print("ok")
