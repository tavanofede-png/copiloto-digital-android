package ar.com.copiloto.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.concurrent.Executors

class GuidanceSpeaker(context: Context, private val onSpeaking: (Boolean) -> Unit) {
    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private var track: AudioTrack? = null
    private var closed = false
    private var ready = false
    private var pending: String? = null
    private var generation = 0
    private val playbackTimeout = Runnable { stop() }
    private val tts = TextToSpeech(context.applicationContext) { status ->
        main.post {
            if (!closed && status == TextToSpeech.SUCCESS) {
                ready = true
                val preferred = Locale.forLanguageTag("es-AR")
                ttsLanguage(preferred)
                pending?.let { pending = null; speakText(it) }
            }
        }
    }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { reportSpeaking(utteranceId, true) }
            override fun onDone(utteranceId: String?) { reportSpeaking(utteranceId, false) }
            @Deprecated("Android API")
            override fun onError(utteranceId: String?) { reportSpeaking(utteranceId, false) }
        })
    }

    private fun reportSpeaking(utteranceId: String?, speaking: Boolean) {
        main.post { if (!closed && utteranceId == "guidance-$generation") onSpeaking(speaking) }
    }

    private fun ttsLanguage(preferred: Locale) {
        if (tts.setLanguage(preferred) < TextToSpeech.LANG_AVAILABLE) tts.setLanguage(Locale.forLanguageTag("es-ES"))
    }

    fun speak(message: String, pcm: ByteArray?) {
        stop()
        if (closed) return
        if (pcm == null) { speakText(message); return }
        val currentGeneration = generation
        worker.execute {
            val result = runCatching {
                AudioTrack.Builder().setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                    .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(24_000).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setTransferMode(AudioTrack.MODE_STATIC).setBufferSizeInBytes(pcm.size).build().also { audioTrack ->
                        try { check(audioTrack.write(pcm, 0, pcm.size) == pcm.size) }
                        catch (error: Exception) { audioTrack.release(); throw error }
                    }
            }
            main.post {
                if (closed || currentGeneration != generation) { result.getOrNull()?.release(); return@post }
                result.onSuccess { audioTrack ->
                    track = audioTrack
                    runCatching { audioTrack.play(); onSpeaking(true) }
                        .onSuccess { main.postDelayed(playbackTimeout, pcm.size * 1000L / 48_000 + 300) }
                        .onFailure { stop(); speakText(message) }
                }.onFailure { speakText(message) }
            }
        }
    }

    private fun speakText(message: String) {
        if (closed) return
        if (!ready) { pending = message; return }
        if (tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "guidance-$generation") == TextToSpeech.ERROR) onSpeaking(false)
    }

    fun stop() {
        generation++
        pending = null
        main.removeCallbacks(playbackTimeout)
        track?.let { runCatching { it.stop() }; it.release() }
        track = null
        tts.stop()
        if (!closed) onSpeaking(false)
    }

    fun close() { stop(); closed = true; tts.shutdown(); worker.shutdownNow() }
}
