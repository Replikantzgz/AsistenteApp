#!/usr/bin/env python3
"""Recorta el logo de Drive (sobre una tarjeta blanca) y genera app/src/main/res/drawable-nodpi/art_drive.png.
Uso: python3 tools/process_drive.py logo_drive.jpg"""
import sys
from PIL import Image
sys.path.insert(0, "tools")
from process_art import cutout, square

im = Image.open(sys.argv[1]).convert("RGB")
k = im.width / 1932                                           # las coordenadas se midieron sobre una vista de 1932 px
box = tuple(int(v * k) for v in (400, 440, 1760, 1450))       # alrededor del logo, sin la tarjeta
crop = im.crop(box)
hole = (int(980 * k) - box[0], int(1100 * k) - box[1])        # hueco blanco del triángulo
art = cutout(crop, extra_seeds=(hole,))
square(art, 384, 0.94).save("app/src/main/res/drawable-nodpi/art_drive.png", optimize=True)
print("ok", art.size)
