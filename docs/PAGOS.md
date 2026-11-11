# Pagos de Alfred (preparados, apagados)

Todo está construido pero **invisible**: mientras `BillingConfig.ENABLED` sea `false`, la app va como siempre
(todo desbloqueado, sin límites, sin ninguna pantalla ni texto de pago).

## Cómo funciona

1. El usuario toca **Comprar** en la pantalla Premium → se abre en el navegador la **página de pago de tu pasarela**
   (Stripe Payment Link, Lemon Squeezy, Paddle…). Alfred no toca tarjetas ni datos de pago.
2. Cuando pagan, tú (o una automatización) generas un **código** con `tools/make_license.py` y se lo envías por correo.
3. El usuario lo pega en la app. La app **verifica la firma sin conexión** con una clave pública incluida en el código.
   Nadie puede inventarse un código sin tu clave privada, y no hace falta servidor.

Plan gratuito: `FREE_AI_PER_DAY` usos de IA al día (chat, transcripción de audios y lectura de documentos).
Calendario, correo, notas, tareas, contactos, Drive y notificaciones **nunca** se limitan.

## Activarlo (3 pasos)

1. En `app/src/main/java/com/iatualcance/alfred/billing/BillingConfig.kt`:
   - `ENABLED = true`
   - `CHECKOUT_MONTHLY_URL` y `CHECKOUT_YEARLY_URL` = tus enlaces de pago
   - `PRICE_MONTHLY` / `PRICE_YEARLY` = los precios tal como los quieres mostrar
   - `SUPPORT_EMAIL` = tu correo de soporte
2. Guarda la **clave privada** (`alfred_license_private.pem`, te la entregué aparte) fuera del repositorio.
3. Compila y publica el APK.

Mientras `ENABLED` sea `true`, aparecen: la sección **Plan** en Ajustes, el aviso «Te quedan N mensajes gratis hoy»
en el chat y la pantalla **Premium** (que también salta sola al agotar la cuota).

## Generar un código para un cliente

```bash
python3 tools/make_license.py --key alfred_license_private.pem --email cliente@correo.com --days 365   # anual
python3 tools/make_license.py --key alfred_license_private.pem --email cliente@correo.com --days 31    # mensual
python3 tools/make_license.py --key alfred_license_private.pem --email cliente@correo.com --lifetime   # de por vida
```

## Importante

- **La clave privada es tu caja fuerte.** Quien la tenga puede regalar Premium. Si se pierde, hay que generar otro par
  y cambiar `PUBLIC_KEY_B64` (los códigos antiguos dejarían de valer).
- **Suscripciones**: con este sistema cada renovación requiere un código nuevo (el mensual caduca a los ~31 días).
  Para renovar solo y sin intervención hace falta un pequeño servidor que reciba el aviso de la pasarela (webhook),
  firme el código y lo envíe: es el siguiente paso natural, la app no cambia.
- **Google Play**: si algún día publicas en Play Store, las suscripciones digitales deben cobrarse con Google Play
  Billing (no con un enlace externo). Para APK propio o hasta 100 usuarios de prueba, este sistema vale.
- Las claves de licencia viajan en el móvil: «Borrar todos los datos» **no** borra la licencia.
