package com.iatualcance.alfred.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class Account(val name: String, val email: String, val photo: String?)

const val DEFAULT_MODEL = "gemini-flash-latest"

/** Ajustes de la app. La clave de Gemini se guarda cifrada con el almacén de claves de Android. */
class Settings(ctx: Context) {
    private val p = ctx.getSharedPreferences("alfred", Context.MODE_PRIVATE)

    val account = MutableStateFlow(
        p.getString("email", null)?.let { Account(p.getString("name", "") ?: "", it, p.getString("photo", null)) }
    )
    val apiKey = MutableStateFlow(p.getString("gemini", null)?.let { Crypto.decrypt(it) } ?: "")
    val model = MutableStateFlow(p.getString("model", DEFAULT_MODEL) ?: DEFAULT_MODEL)
    val driveAuto = MutableStateFlow(p.getBoolean("driveAuto", true))

    fun setAccount(a: Account?) {
        account.value = a
        p.edit().apply {
            if (a == null) { remove("email"); remove("name"); remove("photo") }
            else { putString("email", a.email); putString("name", a.name); putString("photo", a.photo) }
        }.apply()
    }

    fun setApiKey(k: String) {
        apiKey.value = k.trim()
        p.edit().apply { if (k.isBlank()) remove("gemini") else putString("gemini", Crypto.encrypt(k.trim())) }.apply()
    }

    fun setModel(m: String) { model.value = m; p.edit().putString("model", m).apply() }
    fun setDriveAuto(v: Boolean) { driveAuto.value = v; p.edit().putBoolean("driveAuto", v).apply() }
    fun clear() { p.edit().clear().apply(); account.value = null; apiKey.value = ""; model.value = DEFAULT_MODEL }
}

private object Crypto {
    private const val ALIAS = "alfred_clave"
    private const val T = "AES/GCM/NoPadding"

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        kg.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return kg.generateKey()
    }

    fun encrypt(s: String): String {
        val c = Cipher.getInstance(T)
        c.init(Cipher.ENCRYPT_MODE, key())
        return Base64.encodeToString(c.iv + c.doFinal(s.toByteArray()), Base64.NO_WRAP)
    }

    fun decrypt(s: String): String? = runCatching {
        val b = Base64.decode(s, Base64.NO_WRAP)
        val c = Cipher.getInstance(T)
        c.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, b, 0, 12))
        String(c.doFinal(b, 12, b.size - 12))
    }.getOrNull()
}
