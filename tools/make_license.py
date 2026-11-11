#!/usr/bin/env python3
"""Genera un código de licencia de Alfred Premium firmado con tu clave privada.

  python3 tools/make_license.py --key alfred_license_private.pem --email cliente@correo.com --days 365
  python3 tools/make_license.py --key alfred_license_private.pem --email cliente@correo.com --lifetime

El código (ALFRED1.…) se lo envías al cliente; la app lo verifica sin conexión con la clave pública.
Necesita `openssl` instalado."""
import argparse, base64, json, subprocess, sys, uuid
from datetime import date, timedelta

def b64u(b: bytes) -> str:
    return base64.urlsafe_b64encode(b).rstrip(b"=").decode()

ap = argparse.ArgumentParser()
ap.add_argument("--key", required=True, help="clave privada .pem (NO la subas al repositorio)")
ap.add_argument("--email", required=True)
g = ap.add_mutually_exclusive_group(required=True)
g.add_argument("--days", type=int, help="días de validez desde hoy")
g.add_argument("--lifetime", action="store_true", help="sin caducidad")
ap.add_argument("--plan", default="premium")
a = ap.parse_args()

data = {"v": 1, "id": uuid.uuid4().hex[:10], "e": a.email.strip(), "p": a.plan,
        "x": None if a.lifetime else (date.today() + timedelta(days=a.days)).isoformat()}
payload = b64u(json.dumps(data, separators=(",", ":")).encode())
msg = f"ALFRED1.{payload}"
sig = subprocess.run(["openssl", "dgst", "-sha256", "-sign", a.key], input=msg.encode(), capture_output=True)
if sig.returncode != 0:
    sys.exit("openssl falló: " + sig.stderr.decode())
print(f"{msg}.{b64u(sig.stdout)}")
print(f"# para {data['e']} · caduca: {data['x'] or 'nunca'} · id {data['id']}", file=sys.stderr)
