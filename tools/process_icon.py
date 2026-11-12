#!/usr/bin/env python3
"""Prepara el icono de la app a partir de una imagen cuadrada con borde negro (el fondo puede ser un tablero pintado).

  python3 tools/process_icon.py icono.jpg

Aísla la silueta (el borde negro cierra el icono; lo de fuera se descarta) y genera el icono adaptable y los iconos clásicos."""
import sys
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

RES = "app/src/main/res"

def silhouette(path, inset=5):
    """Máscara geométrica del icono. Se mide sobre los bordes limpios (izquierdo y superior) y la simetría respecto al anillo central:
    la sombra proyectada abajo a la derecha se confunde con el borde negro, así que no se puede separar por color."""
    im = Image.open(path).convert("RGB")
    a = np.asarray(im).astype(int); H, W = a.shape[:2]
    dark = a.max(axis=2) < 16
    cy, cx = H // 2, W // 2
    sub = dark[cy - 330:cy + 330, cx - 330:cx + 330]
    ys, xs = np.where(sub)
    rcx = (xs.min() + xs.max()) / 2 + cx - 330; rcy = (ys.min() + ys.max()) / 2 + cy - 330     # centro del anillo
    left = int(np.argmax(dark[int(rcy)])); top = int(np.argmax(dark[:, int(rcx)]))
    # recorta la esquina al tamaño real (simétrico respecto al centro) y estima el radio de las esquinas por la diagonal
    x0, x1, y0, y1 = left, 2 * rcx - left, top, 2 * rcy - top
    d = next(i for i in range(0, 900) if dark[i, i])
    # Resolviendo la ecuación de la esquina sobre la diagonal sale un radio de ~520 px (esquinas muy redondeadas).
    r = max(120.0, min(float(min(x1 - x0, y1 - y0)) / 2, 520.0))
    print(f"icono: x {x0:.0f}..{x1:.0f}  y {y0:.0f}..{y1:.0f}  radio {r:.0f}")
    S = 4
    big = Image.new("L", (W * S // 2, H * S // 2), 0)
    ImageDraw.Draw(big).rounded_rectangle([(x0 + inset) * S / 2, (y0 + inset) * S / 2, (x1 - inset) * S / 2, (y1 - inset) * S / 2], radius=r * S / 2, fill=255)
    alpha = big.resize((W, H), Image.LANCZOS)
    out = im.convert("RGBA"); out.putalpha(alpha)
    return out.crop(alpha.getbbox())

def square(im, size, fill):
    k = size * fill / max(im.size)
    r = im.resize((round(im.width * k), round(im.height * k)), Image.LANCZOS)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0)); out.paste(r, ((size - r.width) // 2, (size - r.height) // 2), r); return out

if __name__ == "__main__":
    art = silhouette(sys.argv[1])
    print("icono", art.size)
    square(art, 432, 0.62).save(f"{RES}/drawable-nodpi/ic_launcher_foreground.png", optimize=True)    # zona segura del icono adaptable
    for d, px in (("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)):
        bg = Image.new("RGBA", (px, px), "#FFFFFF"); bg.alpha_composite(square(art, px, 0.92)); bg.convert("RGB").save(f"{RES}/mipmap-{d}/ic_launcher.png")
    square(art, 512, 1.0).save("/tmp/icon_preview.png")
