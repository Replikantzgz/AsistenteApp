package com.iatualcance.alfred.ai

import android.util.Base64
import com.iatualcance.alfred.data.Settings
import com.iatualcance.alfred.google.Http
import com.iatualcance.alfred.google.arr
import com.iatualcance.alfred.google.obj
import com.iatualcance.alfred.google.str
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
class GeminiError(val code: Int, message: String) : Exception(message)

/** Qué modelos de Gemini tienen capa gratuita. Según Google: Flash y Flash-Lite, sí; los Pro, de pago. Los límites exactos se ven en AI Studio. */
object ModelInfo {
    fun isPaid(name: String) = Regex("(^|-)pro(-|$)").containsMatchIn(name.lowercase())
    fun isFree(name: String) = !isPaid(name) && ("flash" in name.lowercase() || "lite" in name.lowercase())
    fun label(name: String) = when { isPaid(name) -> "de pago"; isFree(name) -> "gratis"; else -> "" }
    private fun rank(n: String) = when { isPaid(n) -> 3; "lite" in n -> 0; "flash" in n -> 1; else -> 2 }
    /** Primero los gratuitos más generosos (Flash-Lite), luego Flash, luego el resto y al final los de pago. */
    fun sorted(models: List<String>) = models.sortedWith(compareBy({ rank(it.lowercase()) }, { it }))
}

class Gemini(private val settings: Settings) {
    private val base = "https://generativelanguage.googleapis.com/v1beta"

    private fun key() = settings.apiKey.value.ifBlank {
        throw Exception("Falta la clave de Gemini. Añádela en Ajustes (el engranaje de la pantalla de Alfred).")
    }

    private suspend fun http(req: Request): JsonObject = withContext(Dispatchers.IO) {
        Http.client.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            val j = runCatching { Http.json.parseToJsonElement(text).jsonObject }.getOrNull()
            if (!resp.isSuccessful) {
                val msg = j?.obj("error")?.str("message") ?: text.take(200)
                throw GeminiError(resp.code, when (resp.code) {
                    400, 403 -> if ("API key" in msg) "La clave de Gemini no es válida. Revísala en Ajustes." else "Gemini: $msg"
                    404 -> "El modelo «${settings.model.value}» no existe o no está disponible. Elige otro en Ajustes."
                    429 -> "Se ha agotado el cupo gratuito de Gemini por ahora (hay un límite por minuto y otro por día; el diario se renueva a medianoche, hora del Pacífico). Espera un poco o prueba otro modelo en Ajustes."
                    500, 502, 503, 504 -> "Gemini está saturado en este momento (error ${resp.code}). Lo he reintentado varias veces; vuelve a probar en un minuto."
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
        val j = withRetry { model ->
            http(Request.Builder().url("$base/models/$model:generateContent")
                .header("x-goog-api-key", key()).post(body.toString().toRequestBody(Http.JSON_TYPE)).build())
        }
        val cand = j.arr("candidates").firstOrNull()?.jsonObject
        val content = cand?.obj("content")
        if (content == null || content.arr("parts").isEmpty()) {
            val why = cand?.str("finishReason") ?: j.obj("promptFeedback")?.str("blockReason") ?: "sin respuesta"
            throw Exception("Gemini no devolvió respuesta ($why).")
        }
        return content
    }

    /**
     * Gemini devuelve 503/429 cuando está saturado, y suele ser cosa de segundos. Se reintenta con espera creciente
     * y, si el modelo elegido sigue saturado, se prueba con uno más ligero antes de rendirse.
     */
    private suspend fun <T> withRetry(call: suspend (model: String) -> T): T {
        val chosen = settings.model.value
        val models = listOf(chosen, FALLBACK).distinct()
        var last: GeminiError? = null
        for ((mi, model) in models.withIndex()) {
            val attempts = if (mi == 0) 3 else 2
            var attempt = 0
            while (attempt < attempts) {
                try {
                    return call(model)
                } catch (e: GeminiError) {
                    last = e
                    // Cupo agotado: no se insiste con este modelo, se pasa al siguiente (Flash-Lite tiene más cupo gratuito).
                    if (e.code == 429) break
                    if (e.code !in RETRYABLE) { if (e.code == 404 && mi < models.lastIndex) break else throw e }
                    delay(1200L * (attempt + 1)); attempt++
                }
            }
        }
        throw last ?: Exception("Gemini no responde")
    }

    /** Búsqueda en internet con "Grounding with Google Search". Devuelve el texto y los títulos de las fuentes. */
    suspend fun searchWeb(query: String): Pair<String, List<String>> {
        val body = buildJsonObject {
            put("contents", buildJsonArray { add(userContent("Busca en internet y responde en español, con datos actuales y concretos y de forma breve: $query")) })
            putJsonArray("tools") { add(buildJsonObject { putJsonObject("google_search") {} }) }
            putJsonObject("generationConfig") { put("temperature", 0.2) }
        }
        val j = withRetry { model ->
            http(Request.Builder().url("$base/models/$model:generateContent").header("x-goog-api-key", key()).post(body.toString().toRequestBody(Http.JSON_TYPE)).build())
        }
        val cand = j.arr("candidates").firstOrNull()?.jsonObject
        val text = cand?.obj("content")?.let { textOf(it) }.orEmpty()
        val sources = cand?.obj("groundingMetadata")?.arr("groundingChunks")?.mapNotNull { c -> c.jsonObject.obj("web")?.let { it.str("title") ?: it.str("uri") } }?.distinct()?.take(5).orEmpty()
        return text to sources
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
        private val RETRYABLE = setOf(500, 502, 503, 504)
        private const val FALLBACK = "gemini-flash-lite-latest"

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
