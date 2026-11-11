package com.iatualcance.alfred.google

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.ClearTokenRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.tasks.await

/** Hace falta que el usuario apruebe los permisos en pantalla. [full]: el que falta es el de Drive completo. */
class ConsentRequired(val pendingIntent: PendingIntent, val full: Boolean = false) :
    Exception(if (full) "Falta el permiso de Drive completo" else "Conecta tu cuenta de Google en Ajustes")

/**
 * Inicio de sesión y permisos con Google Identity Services (AuthorizationClient).
 * No hay ningún ID de cliente en el código: Google reconoce la app por su
 * nombre de paquete + huella SHA-1, registrados en Google Cloud como cliente OAuth de Android.
 *
 * Hay dos niveles: el BASE (Calendar, Gmail, Contactos y los archivos que crea Alfred) y el de Drive completo, que solo se
 * pide al entrar en la pestaña Drive. Así, el resto de la app no depende de haber concedido el permiso grande.
 */
class GoogleAuth(private val ctx: Context) {
    companion object {
        val BASE_SCOPES = listOf(
            "https://www.googleapis.com/auth/userinfo.email",
            "https://www.googleapis.com/auth/userinfo.profile",
            "https://www.googleapis.com/auth/calendar.events",
            "https://www.googleapis.com/auth/gmail.modify",
            "https://www.googleapis.com/auth/contacts",
            "https://www.googleapis.com/auth/drive.file",
        )
        /** Drive completo: explorar, compartir y trabajar en proyectos con otras personas. */
        const val DRIVE_FULL = "https://www.googleapis.com/auth/drive"
        val SCOPES = BASE_SCOPES + DRIVE_FULL
    }

    @Volatile private var baseToken: String? = null
    @Volatile private var baseAt = 0L
    @Volatile private var fullToken: String? = null
    @Volatile private var fullAt = 0L

    private fun fresh(t: String?, at: Long) = t != null && System.currentTimeMillis() - at < 40 * 60_000

    private fun request(full: Boolean) =
        AuthorizationRequest.builder().setRequestedScopes((if (full) SCOPES else BASE_SCOPES).map { Scope(it) }).build()

    /** Devuelve un token válido o lanza [ConsentRequired] si hay que mostrar la pantalla de Google. */
    suspend fun token(full: Boolean = false): String {
        if (fresh(fullToken, fullAt)) return fullToken!!                 // el de Drive completo también vale para lo básico
        if (!full && fresh(baseToken, baseAt)) return baseToken!!
        val res = try {
            Identity.getAuthorizationClient(ctx).authorize(request(full)).await()
        } catch (e: ApiException) {
            throw Exception(describe(e))
        }
        if (res.hasResolution()) throw ConsentRequired(res.pendingIntent!!, full)
        return store(res.accessToken ?: throw Exception("Google no devolvió un token de acceso"), full)
    }

    /** Resultado de la pantalla de consentimiento. */
    fun fromIntent(data: Intent?): String {
        val res = try {
            Identity.getAuthorizationClient(ctx).getAuthorizationResultFromIntent(data)
        } catch (e: ApiException) {
            throw Exception(describe(e))
        }
        return store(res.accessToken ?: throw Exception("Google no devolvió un token de acceso"), DRIVE_FULL in res.grantedScopes)
    }

    private fun store(t: String, full: Boolean): String {
        baseToken = t; baseAt = System.currentTimeMillis()
        if (full) { fullToken = t; fullAt = baseAt }
        return t
    }

    /** Descarta los tokens en caché (caducados o revocados) para pedir otros nuevos. */
    suspend fun invalidate() {
        val tokens = listOfNotNull(baseToken, fullToken).distinct()
        baseToken = null; fullToken = null
        tokens.forEach { t -> runCatching { Identity.getAuthorizationClient(ctx).clearToken(ClearTokenRequest.builder().setToken(t).build()).await() } }
    }

    /** Retira en Google los permisos concedidos a la app para esa cuenta. */
    suspend fun revoke(email: String) {
        invalidate()
        runCatching {
            Identity.getAuthorizationClient(ctx).revokeAccess(
                RevokeAccessRequest.builder().setAccount(android.accounts.Account(email, "com.google")).setScopes(SCOPES.map { Scope(it) }).build()
            ).await()
        }
    }

    private fun describe(e: ApiException): String = when (e.statusCode) {
        10 -> "Google no reconoce esta app (error 10). Falta registrar el cliente OAuth de Android con el paquete y la huella SHA-1 en Google Cloud."
        7 -> "Sin conexión a internet."
        16 -> "Inicio de sesión cancelado."
        else -> "Error de Google (${e.statusCode}): ${e.message ?: ""}"
    }
}
