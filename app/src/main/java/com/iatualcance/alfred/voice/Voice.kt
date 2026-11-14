package com.iatualcance.alfred.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Locale

/** Limpieza del texto antes de leerlo en voz alta (sin dependencias de Android, para poder probarla). */
object SpeechText {
    fun clean(s: String): String = s
        .replace(Regex("https?://\\S+"), "un enlace")
        .replace(Regex("[\\x{1F000}-\\x{1FFFF}\\x{2600}-\\x{27BF}\\x{2B00}-\\x{2BFF}\\x{FE0F}\\x{200D}]"), "")
        .replace(Regex("[*_`#>~|]"), "")
        .replace(Regex("\\s+"), " ").trim().take(1500)
}

/**
 * Modo conversación: escucha (SpeechRecognizer), manda lo dicho al asistente, lee la respuesta (TextToSpeech) y vuelve a
 * escuchar, hasta que se pulsa Terminar o hay dos silencios seguidos. Necesita el permiso de micrófono.
 */
class VoiceController(private val ctx: Context) {
    enum class State { IDLE, LISTENING, THINKING, SPEAKING }

    val state = MutableStateFlow(State.IDLE)
    /** Lo que el usuario ha dicho. Se asigna desde el chat. */
    var onText: ((String) -> Unit)? = null
    @Volatile var active = false; private set
    /** Lo entendido hasta ahora (mientras se espera por si sigues hablando). */
    val heard = MutableStateFlow("")
    private var oneShot = false
    private var buffer = ""
    private val flush = Runnable { flushNow() }

    private val main = Handler(Looper.getMainLooper())
    private var rec: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private val ttsQueue = mutableListOf<(TextToSpeech?) -> Unit>()
    private var silences = 0

    companion object { const val PATIENCE_MS = 2500L }

    fun available() = SpeechRecognizer.isRecognitionAvailable(ctx)

    /** [oneShot]: una sola frase (botón del micro); si no, conversación continua. */
    fun startConversation(oneShot: Boolean = false) { this.oneShot = oneShot; active = true; silences = 0; buffer = ""; heard.value = ""; listen() }

    fun stop() {
        active = false
        main.post {
            main.removeCallbacks(flush); buffer = ""; heard.value = ""
            runCatching { rec?.cancel(); rec?.destroy() }; rec = null
            runCatching { tts?.stop() }
            state.value = State.IDLE
        }
    }

    /** Corta lo que Alfred esté pensando o leyendo y se pone a escuchar de nuevo (sin salir del modo voz). */
    fun interrupt() {
        main.post {
            main.removeCallbacks(flush); buffer = ""; heard.value = ""
            runCatching { rec?.cancel(); rec?.destroy() }; rec = null
            runCatching { tts?.stop() }
            if (active) listen() else state.value = State.IDLE
        }
    }

    /** Vuelve a escuchar (tras leer la respuesta) si el modo conversación sigue activo; en una sola frase, termina. */
    fun relisten() { if (active) { if (oneShot) stop() else listen() } }

    /** Pasa lo dicho al asistente (tras la pausa de paciencia). */
    private fun flushNow() {
        main.removeCallbacks(flush)
        val said = buffer.trim(); buffer = ""; heard.value = ""
        if (said.isBlank() || !active) return
        runCatching { rec?.cancel(); rec?.destroy() }; rec = null
        silences = 0; state.value = State.THINKING; onText?.invoke(said)
    }

    private fun listen() { main.post {
        if (!active) return@post
        runCatching { rec?.destroy() }
        val r = SpeechRecognizer.createSpeechRecognizer(ctx).also { rec = it }
        r.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val said = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty().trim()
                if (said.isBlank()) { if (buffer.isNotBlank()) flushNow() else silence(); return }
                // Paciencia: no se envía hasta que pasen PATIENCE_MS sin que sigas hablando; mientras, se sigue escuchando.
                silences = 0; buffer = (buffer + " " + said).trim(); heard.value = buffer
                main.removeCallbacks(flush); main.postDelayed(flush, PATIENCE_MS)
                // Se sigue escuchando por si continúas, pero tras una pausa: crear otro reconocedor al instante suele dar error.
                main.postDelayed({ if (active && buffer.isNotBlank()) listen() }, 400)
            }
            override fun onError(error: Int) {
                // Si ya había algo dicho, un fallo al seguir escuchando NO debe perderlo: se envía tal cual.
                if (buffer.isNotBlank()) { flushNow(); return }
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> silence()
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY, SpeechRecognizer.ERROR_CLIENT -> main.postDelayed({ if (active) listen() }, 600)
                    else -> stop()                         // sin permiso, sin red, servicio no disponible…
                }
            }
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() { main.removeCallbacks(flush) }   // sigues hablando: se espera
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        r.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES").putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, ctx.packageName)
                .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, PATIENCE_MS)
                .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, PATIENCE_MS)
                .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1),
        )
        state.value = State.LISTENING
    } }

    private fun silence() { silences++; if (silences >= 2) stop() else listen() }

    /** Lee [text] en voz alta; al terminar llama a [then]. */
    fun speak(text: String, then: () -> Unit = {}) {
        val clean = SpeechText.clean(text)
        if (clean.isBlank()) { then(); return }
        main.post {
            withTts { t ->
                if (t == null) { state.value = if (active) State.THINKING else State.IDLE; then(); return@withTts }
                state.value = State.SPEAKING
                t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) { main.post { if (!active) state.value = State.IDLE; then() } }
                    @Deprecated("Deprecated in Java") override fun onError(utteranceId: String?) { main.post { if (!active) state.value = State.IDLE; then() } }
                })
                t.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "alfred-" + System.nanoTime())
            }
        }
    }

    private fun withTts(run: (TextToSpeech?) -> Unit) {
        val t = tts
        if (t != null && ttsReady) { run(t); return }
        ttsQueue += run
        if (t == null) {
            tts = TextToSpeech(ctx) { status ->
                main.post {
                    val queued = ttsQueue.toList(); ttsQueue.clear()
                    if (status == TextToSpeech.SUCCESS) {
                        tts?.language = Locale("es", "ES"); ttsReady = true
                        queued.forEach { it(tts) }
                    } else queued.forEach { it(null) }
                }
            }
        }
    }
}
