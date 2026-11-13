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

const val DEFAULT_MODEL = "gemini-flash-lite-latest"

/** Ajustes de la app. La clave de Gemini se guarda cifrada con el almacén de claves de Android. */
class Settings(ctx: Context) {
    private val p = ctx.getSharedPreferences("alfred", Context.MODE_PRIVATE)

    val account = MutableStateFlow(
        p.getString("email", null)?.let { Account(p.getString("name", "") ?: "", it, p.getString("photo", null)) }
    )
    val apiKey = MutableStateFlow(p.getString("gemini", null)?.let { Crypto.decrypt(it) } ?: "")
    val model = MutableStateFlow(p.getString("model", DEFAULT_MODEL) ?: DEFAULT_MODEL)
    val driveAuto = MutableStateFlow(p.getBoolean("driveAuto", true))
    val notifyEvents = MutableStateFlow(p.getBoolean("notifyEvents", true))
    val notifyTasks = MutableStateFlow(p.getBoolean("notifyTasks", true))
    val leadMin = MutableStateFlow(p.getInt("leadMin", 15))
    val dailySummary = MutableStateFlow(p.getBoolean("dailySummary", true))
    val summaryMinutes = MutableStateFlow(p.getInt("summaryMinutes", 8 * 60))
    val skin = MutableStateFlow(p.getString("skin", "pokemon") ?: "pokemon").also { com.iatualcance.alfred.ui.Skins.current = com.iatualcance.alfred.ui.Skins.byId(it.value) }
    fun setSkin(id: String) { skin.value = id; p.edit().putString("skin", id).apply(); com.iatualcance.alfred.ui.Skins.current = com.iatualcance.alfred.ui.Skins.byId(id) }
    val sumEvents = MutableStateFlow(p.getBoolean("sumEvents", true))
    val sumTasks = MutableStateFlow(p.getBoolean("sumTasks", true))
    val sumNotes = MutableStateFlow(p.getBoolean("sumNotes", true))
    val sumMail = MutableStateFlow(p.getBoolean("sumMail", true))
    val sumWeather = MutableStateFlow(p.getBoolean("sumWeather", true))
    val weatherCity = MutableStateFlow(p.getString("weatherCity", "") ?: "")
    fun setSumEvents(v: Boolean) { sumEvents.value = v; p.edit().putBoolean("sumEvents", v).apply() }
    fun setSumTasks(v: Boolean) { sumTasks.value = v; p.edit().putBoolean("sumTasks", v).apply() }
    fun setSumNotes(v: Boolean) { sumNotes.value = v; p.edit().putBoolean("sumNotes", v).apply() }
    fun setSumMail(v: Boolean) { sumMail.value = v; p.edit().putBoolean("sumMail", v).apply() }
    fun setSumWeather(v: Boolean) { sumWeather.value = v; p.edit().putBoolean("sumWeather", v).apply() }
    fun setWeatherCity(v: String) { weatherCity.value = v.trim(); p.edit().putString("weatherCity", v.trim()).apply() }
    val speakReplies = MutableStateFlow(p.getBoolean("speakReplies", false))
    val autoUpdate = MutableStateFlow(p.getBoolean("autoUpdate", true))
    val license = MutableStateFlow(p.getString("license", "") ?: "")
    private val aiDay get() = p.getString("aiDay", "") ?: ""
    /** Mensajes de IA usados hoy. */
    val aiToday = MutableStateFlow(if (aiDay == java.time.LocalDate.now().toString()) p.getInt("aiCount", 0) else 0)

    fun setAccount(a: Account?) {
        account.value = a
        p.edit().apply {
            if (a == null) { remove("email"); remove("name"); remove("photo") }
            else { putString("email", a.email); putString("name", a.name); putString("photo", a.photo) }
        }.apply()
    }

    fun setModel(m: String) { model.value = m; p.edit().putString("model", m).apply() }
    fun setApiKey(k: String) {
        apiKey.value = k.trim()
        p.edit().apply { if (k.isBlank()) remove("gemini") else putString("gemini", Crypto.encrypt(k.trim())) }.apply()
    }

    fun setDriveAuto(v: Boolean) { driveAuto.value = v; p.edit().putBoolean("driveAuto", v).apply() }
    fun setNotifyEvents(v: Boolean) { notifyEvents.value = v; p.edit().putBoolean("notifyEvents", v).apply() }
    fun setNotifyTasks(v: Boolean) { notifyTasks.value = v; p.edit().putBoolean("notifyTasks", v).apply() }
    fun setLeadMin(v: Int) { leadMin.value = v; p.edit().putInt("leadMin", v).apply() }
    fun setDailySummary(v: Boolean) { dailySummary.value = v; p.edit().putBoolean("dailySummary", v).apply() }
    fun setSummaryMinutes(v: Int) { summaryMinutes.value = v; p.edit().putInt("summaryMinutes", v).apply() }
    fun setSpeakReplies(v: Boolean) { speakReplies.value = v; p.edit().putBoolean("speakReplies", v).apply() }
    fun setAutoUpdate(v: Boolean) { autoUpdate.value = v; p.edit().putBoolean("autoUpdate", v).apply() }
    fun setLicense(code: String) { license.value = code; p.edit().putString("license", code).apply() }
    fun addAiUse() {
        val today = java.time.LocalDate.now().toString()
        val n = (if (aiDay == today) p.getInt("aiCount", 0) else 0) + 1
        p.edit().putString("aiDay", today).putInt("aiCount", n).apply(); aiToday.value = n
    }
    fun aiUsedToday(): Int = if (aiDay == java.time.LocalDate.now().toString()) p.getInt("aiCount", 0) else 0
    /** Vuelve a los valores de fábrica de los ajustes, sin tocar la clave de Gemini, la licencia ni la cuenta. */
    fun resetPreferences() {
        p.edit().remove("notifyEvents").remove("notifyTasks").remove("leadMin").remove("driveAuto").remove("autoUpdate").remove("speakReplies").remove("dailySummary").remove("summaryMinutes").remove("model").apply()
        notifyEvents.value = true; notifyTasks.value = true; leadMin.value = 15; driveAuto.value = true; autoUpdate.value = true; speakReplies.value = false; dailySummary.value = true; summaryMinutes.value = 8 * 60; model.value = DEFAULT_MODEL
    }
    fun clear() { val keep = license.value; p.edit().clear().putString("license", keep).apply(); account.value = null; apiKey.value = ""; model.value = DEFAULT_MODEL }
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
