package ar.com.copiloto.voice

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ar.com.copiloto.ui.theme.CopilotoTheme

/** A visible activity owns microphone access, including on Android 14–16. */
class VoiceCaptureActivity : ComponentActivity() {
    private lateinit var recorder: PcmRecorder
    private val main = Handler(Looper.getMainLooper())
    private var listening by mutableStateOf(false)
    private var finishingAudio by mutableStateOf(false)
    private var message by mutableStateOf("Solo escucho mientras hacés tu consulta.")
    private var pending: Result<ByteArray>? = null
    private var delivered = false
    private var requestId = 0L
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) record() else { message = "No habilitaste el micrófono. Podés volver y escribir tu consulta." }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        recorder = PcmRecorder(this)
        requestId = intent.getLongExtra("voiceRequestId", -1)
        VoiceInputBus.attach(requestId) { recorder.close(); finish() }
        setContent {
            CopilotoTheme {
                BackHandler { finish() }
                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.widthIn(max = 400.dp).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text(if (listening) "Te escucho…" else "¿Qué necesitás hacer?", style = MaterialTheme.typography.headlineSmall)
                        Text(message, style = MaterialTheme.typography.bodyLarge)
                        if (listening) LinearProgressIndicator(Modifier.fillMaxWidth())
                        Button(onClick = {
                            if (listening) { finishingAudio = true; listening = false; recorder.stop() }
                            else if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) record()
                            else permission.launch(Manifest.permission.RECORD_AUDIO)
                        }, enabled = !finishingAudio, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                            Text(if (finishingAudio) "Preparando consulta…" else if (listening) "Terminar y preguntar" else "Comenzar a hablar")
                        }
                        TextButton(onClick = { finish() }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Cancelar") }
                    }
                }
            }
        }
        if (savedInstanceState == null) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) main.post { record() }
            else permission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun record() {
        if (listening || finishingAudio || isFinishing || isDestroyed) return
        message = "Contame qué querés hacer. Tocá “Terminar y preguntar” cuando termines. Máximo 20 segundos."
        runCatching {
            listening = true
            recorder.start { result ->
                pending = result
                finish()
            }
        }.onFailure {
            listening = false
            message = it.message ?: "No pude abrir el micrófono. Probá escribiendo."
        }
    }

    override fun onStop() {
        super.onStop()
        recorder.close()
        if (!delivered) {
            delivered = true
            val result = pending ?: Result.failure(IllegalStateException("Consulta por voz cancelada."))
            // The previous app must be visible again before the service takes its screenshot.
            main.postDelayed({ VoiceInputBus.complete(requestId, result) }, 350)
        }
        if (!isFinishing) finish()
    }

    override fun onDestroy() {
        VoiceInputBus.detach(requestId)
        recorder.close()
        super.onDestroy()
    }
}
