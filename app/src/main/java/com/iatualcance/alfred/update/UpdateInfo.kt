package com.iatualcance.alfred.update

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Contenido de update.json, que el workflow publica junto a cada versión. */
@Serializable
data class UpdateInfo(
    val versionCode: Int,
    val versionName: String = "",
    val url: String,
    val sha256: String? = null,
    val notes: String = "",
) {
    /** ¿Es más nueva que la versión instalada? Se compara por versionCode, que solo sube. */
    fun isNewerThan(installed: Int) = versionCode > installed

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(text: String): UpdateInfo? = runCatching {
            json.decodeFromString(serializer(), text).takeIf { it.versionCode > 0 && it.url.startsWith("https://") }
        }.getOrNull()
    }
}
