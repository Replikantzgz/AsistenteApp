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

    private val main = Handler(Looper.getMainLooper())
    private var rec: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private val ttsQueue = mutableListOf<(TextToSpeech?) -> Unit>()
    private var silences = 0

    fun available() = SpeechRecognizer.isRecognitionAvailable(ctx)

    fun startConversation() { active = true; silences = 0; listen() }

    fun stop() {
        active = false
        main.post {
            runCatching { rec?.cancel(); rec?.destroy() }; rec = null
            runCatching { tts?.stop() }
            state.value = State.IDLE
        }
    }

    /** Vuelve a escuchar (tras leer la respuesta) si el modo conversación sigue activo. */
    fun relisten() { if (active) listen() }

    private fun listen() { main.post {
        if (!active) return@post
        runCatching { rec?.destroy() }
        val r = SpeechRecognizer.createSpeechRecognizer(ctx).also { rec = it }
        r.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val said = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty().trim()
                if (said.isBlank()) { silence(); return }
                silences = 0; state.value = State.THINKING; onText?.invoke(said)
            }
            override fun onError(error: Int) {
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> silence()
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> main.postDelayed({ if (active) listen() }, 600)
                    else -> stop()                         // sin permiso, sin red, servicio no disponible…
                }
            }
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        r.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES").putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, ctx.packageName),
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
