package com.iatualcance.alfred.ai

import android.util.Base64
import com.iatualcance.alfred.data.Settings
import com.iatualcance.alfred.google.Http
import com.iatualcance.alfred.google.arr
import com.iatualcance.alfred.google.obj
import com.iatualcance.alfred.google.str
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** Cliente REST de la API de Gemini (Google AI Studio). La clave la pone el usuario en Ajustes. */
class Gemini(private val settings: Settings) {
    private val base = "https://generativelanguage.googleapis.com/v1beta"

    private fun key() = settings.apiKey.value.ifBlank {
        throw Exception("Falta la clave de Gemini. Añádela en Más → Ajustes.")
    }

    private suspend fun http(req: Request): JsonObject = withContext(Dispatchers.IO) {
        Http.client.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            val j = runCatching { Http.json.parseToJsonElement(text).jsonObject }.getOrNull()
            if (!resp.isSuccessful) {
                val msg = j?.obj("error")?.str("message") ?: text.take(200)
                throw Exception(when (resp.code) {
                    400, 403 -> if ("API key" in msg) "La clave de Gemini no es válida. Revísala en Ajustes." else "Gemini: $msg"
                    404 -> "El modelo «${settings.model.value}» no existe o no está disponible. Elige otro en Ajustes."
                    429 -> "Has alcanzado el límite de uso de Gemini. Espera un momento y vuelve a intentarlo."
                    else -> "Gemini (${resp.code}): $msg"
                })
            }
            j ?: throw Exception("Respuesta de Gemini no válida")
        }
    }

    /** Una llamada a generateContent. Devuelve el objeto `content` del modelo tal cual (hay que reenviarlo íntegro en el historial). */
    suspend fun generate(contents: List<JsonObject>, system: String? = null, tools: JsonArray? = null, jsonOut: Boolean = false): JsonObject {
        val body = buildJsonObject {
            system?.let { putJsonObject("systemInstruction") { putJsonArray("parts") { add(buildJsonObject { put("text", it) }) } } }
            put("contents", JsonArray(contents))
            tools?.let { putJsonArray("tools") { add(buildJsonObject { put("functionDeclarations", it) }) } }
            putJsonObject("generationConfig") {
                put("temperature", 0.3)
                if (jsonOut) put("responseMimeType", "application/json")
            }
        }
        val req = Request.Builder().url("$base/models/${settings.model.value}:generateContent")
            .header("x-goog-api-key", key()).post(body.toString().toRequestBody(Http.JSON_TYPE)).build()
        val j = http(req)
        val cand = j.arr("candidates").firstOrNull()?.jsonObject
        val content = cand?.obj("content")
        if (content == null || content.arr("parts").isEmpty()) {
            val why = cand?.str("finishReason") ?: j.obj("promptFeedback")?.str("blockReason") ?: "sin respuesta"
            throw Exception("Gemini no devolvió respuesta ($why).")
        }
        return content
    }

    suspend fun listModels(): List<String> {
        val req = Request.Builder().url("$base/models?pageSize=200").header("x-goog-api-key", key()).build()
        return http(req).arr("models").map { it.jsonObject }
            .filter { m -> m.arr("supportedGenerationMethods").any { it.jsonPrimitive.content == "generateContent" } }
            .mapNotNull { it.str("name")?.removePrefix("models/") }
            .filter { it.startsWith("gemini") && listOf("embedding", "image", "tts", "audio", "live", "robotics", "computer").none { x -> x in it } }
            .sorted()
    }

    /** Pregunta suelta con un fichero adjunto (audio, imagen o PDF) enviado en línea. */
    suspend fun withFile(prompt: String, bytes: ByteArray, mime: String, jsonOut: Boolean = false): String {
        if (bytes.size > 14 * 1024 * 1024) throw Exception("El archivo es demasiado grande (máximo 14 MB).")
        val content = generate(listOf(userContent(prompt, bytes, mime)), jsonOut = jsonOut)
        return textOf(content)
    }

    suspend fun transcribe(bytes: ByteArray, mime: String): String = withFile(
        "Transcribe este audio palabra por palabra en su idioma original, con puntuación correcta y párrafos. " +
            "Devuelve solo la transcripción, sin comentarios. Si no se entiende nada o está vacío, responde exactamente: (sin audio inteligible)",
        bytes, normalizeAudioMime(mime),
    ).trim()

    companion object {
        fun textPart(t: String) = buildJsonObject { put("text", t) }

        fun userContent(text: String, bytes: ByteArray? = null, mime: String? = null) = buildJsonObject {
            put("role", "user")
            putJsonArray("parts") {
                if (bytes != null && mime != null) add(buildJsonObject {
                    putJsonObject("inlineData") { put("mimeType", mime); put("data", Base64.encodeToString(bytes, Base64.NO_WRAP)) }
                })
                add(textPart(text))
            }
        }

        fun modelContent(text: String) = buildJsonObject { put("role", "model"); putJsonArray("parts") { add(textPart(text)) } }

        fun textOf(content: JsonObject): String =
            content.arr("parts").map { it.jsonObject }.filter { it.str("thought") != "true" }.mapNotNull { it.str("text") }.joinToString("").trim()

        fun functionCalls(content: JsonObject): List<JsonObject> = content.arr("parts").mapNotNull { it.jsonObject.obj("functionCall") }

        fun functionResponse(call: JsonObject, result: JsonElement) = buildJsonObject {
            putJsonObject("functionResponse") {
                call.str("id")?.let { put("id", it) }
                put("name", call.str("name").orEmpty())
                putJsonObject("response") { put("result", result) }
            }
        }

        /** Gemini admite wav, mp3, aiff, aac, ogg y flac; los audios de WhatsApp (opus) van como ogg. */
        fun normalizeAudioMime(m: String): String = when {
            "ogg" in m || "opus" in m -> "audio/ogg"
            "mpeg" in m || "mp3" in m -> "audio/mp3"
            "wav" in m -> "audio/wav"
            "flac" in m -> "audio/flac"
            "aac" in m -> "audio/aac"
            "mp4" in m || "m4a" in m -> "audio/mp4"
            "3gpp" in m || "amr" in m -> m
            else -> m
        }
    }
}
