package com.iatualcance.alfred.billing

/**
 * Pagos de Alfred. Está todo construido pero APAGADO.
 *
 * Mientras [ENABLED] sea false la app se comporta como siempre: todo desbloqueado, sin límites y sin ninguna
 * pantalla, texto ni botón de pago. Para activarlo:
 *   1. Pon [ENABLED] = true.
 *   2. Rellena los enlaces de pago de tu pasarela (los dos precios).
 *   3. Genera códigos con tools/make_license.py cuando alguien pague (ver docs/PAGOS.md).
 */
object BillingConfig {
    /** Interruptor general. */
    val ENABLED: Boolean = false

    /** Mensajes con IA gratis al día para quien no es Premium (chat, transcripciones y lectura de documentos). */
    const val FREE_AI_PER_DAY = 15

    /** Enlaces de pago alojados por la pasarela (Stripe Payment Link, Lemon Squeezy, Paddle, etc.). Vacío = "próximamente". */
    const val CHECKOUT_MONTHLY_URL = ""
    const val CHECKOUT_YEARLY_URL = ""

    /** Textos de los precios tal como se muestran. */
    const val PRICE_MONTHLY = "3,99 €/mes"
    const val PRICE_YEARLY = "29,99 €/año"

    /** Correo al que escriben los clientes si tienen un problema con el código. */
    const val SUPPORT_EMAIL = ""

    /** Clave pública que comprueba las firmas de los códigos (la privada NO está en el repositorio ni en la app). */
    const val PUBLIC_KEY_B64 = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEXq+yM6rREc/y1o18jk3zI/zlsj3lDo/ZFxAQkjeTnAz2Csl8ifmL6MqS/qYdbMUlW5EhdpIYp05Mpw16N2K2nw=="
}
