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

/** Hace falta que el usuario apruebe los permisos en pantalla. */
class ConsentRequired(val pendingIntent: PendingIntent) : Exception("Conecta tu cuenta de Google en Ajustes")

/**
 * Inicio de sesión y permisos con Google Identity Services (AuthorizationClient).
 * No hay ningún ID de cliente en el código: Google reconoce la app por su
 * nombre de paquete + huella SHA-1, registrados en Google Cloud como cliente OAuth de Android.
 */
class GoogleAuth(private val ctx: Context) {
    companion object {
        val SCOPES = listOf(
            "https://www.googleapis.com/auth/userinfo.email",
            "https://www.googleapis.com/auth/userinfo.profile",
            "https://www.googleapis.com/auth/calendar.events",
            "https://www.googleapis.com/auth/gmail.modify",
            "https://www.googleapis.com/auth/contacts",
            "https://www.googleapis.com/auth/drive",   // Drive completo: explorar, compartir y trabajar en proyectos con otras personas
        )
    }

    @Volatile private var token: String? = null
    @Volatile private var tokenAt = 0L

    private fun request() = AuthorizationRequest.builder().setRequestedScopes(SCOPES.map { Scope(it) }).build()

    /** Devuelve un token válido o lanza [ConsentRequired] si hay que mostrar la pantalla de Google. */
    suspend fun token(): String {
        token?.let { if (System.currentTimeMillis() - tokenAt < 40 * 60_000) return it }
        val res = try {
            Identity.getAuthorizationClient(ctx).authorize(request()).await()
        } catch (e: ApiException) {
            throw Exception(describe(e))
        }
        if (res.hasResolution()) throw ConsentRequired(res.pendingIntent!!)
        return store(res.accessToken ?: throw Exception("Google no devolvió un token de acceso"))
    }

    /** Resultado de la pantalla de consentimiento. */
    fun fromIntent(data: Intent?): String {
        val res = try {
            Identity.getAuthorizationClient(ctx).getAuthorizationResultFromIntent(data)
        } catch (e: ApiException) {
            throw Exception(describe(e))
        }
        return store(res.accessToken ?: throw Exception("Google no devolvió un token de acceso"))
    }

    private fun store(t: String): String { token = t; tokenAt = System.currentTimeMillis(); return t }

    /** Descarta el token en caché (caducado o revocado) para pedir uno nuevo. */
    suspend fun invalidate() {
        val t = token ?: return
        token = null
        runCatching { Identity.getAuthorizationClient(ctx).clearToken(ClearTokenRequest.builder().setToken(t).build()).await() }
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
