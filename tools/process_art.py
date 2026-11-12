#!/usr/bin/env python3
"""Prepara las siete ilustraciones de la app: recorta, quita el fondo blanco y genera los recursos Android.

  python3 tools/process_art.py pikachu.jpg correo.jpg agenda.jpg ajustes.jpg contactos.jpg notas.jpg pokeball.jpg

El orden es fijo: asistente, correo, agenda, ajustes, contactos, notas, icono de la app."""
import sys, os
import numpy as np
from PIL import Image, ImageDraw

RES = "app/src/main/res"
NAMES = ["pikachu", "correo", "agenda", "ajustes", "contactos", "notas", "pokeball"]

def cutout(path, extra_seeds=()):
    """Quita el fondo blanco. [extra_seeds]: puntos (x, y) de zonas blancas cerradas (p. ej. el hueco de un triángulo) que también deben desaparecer."""
    im = path.convert("RGB") if isinstance(path, Image.Image) else Image.open(path).convert("RGB")
    a = np.asarray(im)
    near_white = (a.min(axis=2) >= 236).astype(np.uint8) * 255
    mask = Image.fromarray(near_white, "L").copy()
    ImageDraw.floodfill(mask, (0, 0), 128)                      # solo el blanco conectado con el borde
    for pt in ((im.width - 1, 0), (0, im.height - 1), (im.width - 1, im.height - 1)):
        if mask.getpixel(pt) == 255: ImageDraw.floodfill(mask, pt, 128)
    for pt in extra_seeds:
        if mask.getpixel(pt) == 255: ImageDraw.floodfill(mask, pt, 128)
    bg = np.asarray(mask) == 128
    # comer un píxel de halo blanquecino del JPEG pegado al fondo
    pad = np.pad(bg, 1, constant_values=True)
    near = (pad[:-2, 1:-1] | pad[2:, 1:-1] | pad[1:-1, :-2] | pad[1:-1, 2:])
    halo = near & ~bg & (a.min(axis=2) >= 205)
    alpha = np.where(bg | halo, 0, 255).astype(np.uint8)
    rgba = np.dstack([a, alpha])
    out = Image.fromarray(rgba, "RGBA")
    return out.crop(out.getchannel("A").getbbox())

def square(im, size, fill=0.94):
    """Centra el recorte en un cuadrado transparente, ocupando `fill` del lado."""
    s = max(im.size); k = size * fill / s
    r = im.resize((max(1, round(im.width * k)), max(1, round(im.height * k))), Image.LANCZOS)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0)); out.paste(r, ((size - r.width) // 2, (size - r.height) // 2), r); return out

if __name__ == "__main__":
    srcs = sys.argv[1:]
    assert len(srcs) == 7, "hacen falta las 7 imágenes, en este orden: " + ", ".join(NAMES)
    os.makedirs(f"{RES}/drawable-nodpi", exist_ok=True)
    for name, src in zip(NAMES, srcs):
        art = cutout(src)
        if name == "pokeball":
            # icono adaptable: el dibujo ocupa ~62% del lienzo (la zona segura es el 66% central)
            square(art, 432, 0.62).save(f"{RES}/drawable-nodpi/ic_launcher_foreground.png")
            for d, px in (("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)):
                os.makedirs(f"{RES}/mipmap-{d}", exist_ok=True)
                bg = Image.new("RGBA", (px, px), "#FFFFFF"); bg.alpha_composite(square(art, px, 0.9))
                bg.convert("RGB").save(f"{RES}/mipmap-{d}/ic_launcher.png")
        if name != "pokeball": square(art, 384).save(f"{RES}/drawable-nodpi/art_{name}.png", optimize=True)
        print(name, art.size)
