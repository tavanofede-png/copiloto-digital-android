package ar.com.copiloto.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** Push-to-talk PCM16 mono 24kHz; memory-only and hard-capped at 20 seconds. */
class PcmRecorder(private val context: Context) {
    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private val recording = AtomicBoolean(false)
    @Volatile private var recorder: AudioRecord? = null
    @Volatile private var cancelled = false
    @Volatile private var closed = false

    fun start(onComplete: (Result<ByteArray>) -> Unit) {
        check(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            "Falta el permiso del micrófono."
        }
        check(!closed && recording.compareAndSet(false, true))
        cancelled = false
        worker.execute {
            val result = runCatching {
                val minimum = AudioRecord.getMinBufferSize(24_000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
                check(minimum > 0) { "Este teléfono no permite grabar en este formato. Podés escribir tu consulta." }
                val device = AudioRecord.Builder().setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                    .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(24_000).setChannelMask(AudioFormat.CHANNEL_IN_MONO).build())
                    .setBufferSizeInBytes(maxOf(minimum * 2, 8192)).build()
                recorder = device
                try {
                    check(device.state == AudioRecord.STATE_INITIALIZED) { "No pude abrir el micrófono." }
                    if (!recording.get() || closed) error("Grabación cancelada.")
                    device.startRecording()
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(2048)
                    val maximum = 24_000 * 2 * 20
                    while (recording.get() && output.size() < maximum) {
                        val count = device.read(buffer, 0, minOf(buffer.size, maximum - output.size()), AudioRecord.READ_BLOCKING)
                        if (count > 0) output.write(buffer, 0, count)
                        else if (recording.get()) error("No pude escuchar el micrófono. Probá escribiendo.")
                    }
                    val bytes = output.toByteArray()
                    check(bytes.size >= 4800) { "No llegué a escucharte. Probá hablar un poquito más." }
                    bytes
                } finally {
                    runCatching { device.stop() }
                    device.release()
                    recorder = null
                    recording.set(false)
                }
            }
            if (!cancelled && !closed) main.post { if (!cancelled && !closed) onComplete(result) }
        }
    }

    fun stop() { recording.set(false); runCatching { recorder?.stop() } }
    fun close() {
        closed = true
        cancelled = true
        stop()
        worker.shutdownNow()
        main.removeCallbacksAndMessages(null)
    }
}
