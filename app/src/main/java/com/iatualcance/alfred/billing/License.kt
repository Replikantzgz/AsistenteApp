package com.iatualcance.alfred.billing

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.time.LocalDate
import java.util.Base64

/** Contenido firmado de un código de licencia. */
@Serializable
data class LicenseData(
    val v: Int = 1,
    val id: String = "",
    val e: String = "",          // correo del cliente
    val p: String = "premium",   // plan
    val x: String? = null,       // caduca (AAAA-MM-DD); null = de por vida
)

sealed interface LicenseResult {
    data class Valid(val data: LicenseData) : LicenseResult
    data class Invalid(val reason: String) : LicenseResult
}

/**
 * Códigos de licencia: `ALFRED1.<datos en base64url>.<firma en base64url>`.
 * La firma es ECDSA P-256 con SHA-256 sobre el texto `ALFRED1.<datos>`. Solo quien tiene la clave privada puede
 * generarlos; la app solo guarda la pública, así que no se pueden falsificar y no hace falta servidor.
 */
object Licenses {
    const val PREFIX = "ALFRED1"
    private val json = Json { ignoreUnknownKeys = true }

    fun verify(code: String, publicKeyB64: String = BillingConfig.PUBLIC_KEY_B64, today: LocalDate = LocalDate.now()): LicenseResult {
        val clean = code.filterNot { it.isWhitespace() }
        val parts = clean.split('.')
        if (parts.size != 3 || parts[0] != PREFIX) return LicenseResult.Invalid("El código no tiene el formato correcto.")
        val data = runCatching {
            val signature = Base64.getUrlDecoder().decode(parts[2])
            val key = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyB64)))
            val ok = Signature.getInstance("SHA256withECDSA").run {
                initVerify(key); update("${parts[0]}.${parts[1]}".toByteArray(Charsets.US_ASCII)); verify(signature)
            }
            if (!ok) return LicenseResult.Invalid("El código no es válido.")
            json.decodeFromString(LicenseData.serializer(), String(Base64.getUrlDecoder().decode(parts[1])))
        }.getOrElse { return LicenseResult.Invalid("El código no es válido.") }
        data.x?.let { exp ->
            val day = runCatching { LocalDate.parse(exp) }.getOrNull() ?: return LicenseResult.Invalid("El código no es válido.")
            if (today.isAfter(day)) return LicenseResult.Invalid("Este código caducó el $exp.")
        }
        return LicenseResult.Valid(data)
    }
}
