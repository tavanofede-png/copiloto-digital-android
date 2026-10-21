package ar.com.copiloto.capture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import ar.com.copiloto.MainActivity
import ar.com.copiloto.R
import ar.com.copiloto.overlay.OverlayController
import ar.com.copiloto.session.AssistanceSessionManager
import ar.com.copiloto.assistant.ConnectionSettings
import ar.com.copiloto.assistant.HttpAssistantApi
import ar.com.copiloto.assistant.ConversationContext
import ar.com.copiloto.assistant.GuidanceResponse
import ar.com.copiloto.overlay.BubbleState
import ar.com.copiloto.voice.GuidanceSpeaker
import ar.com.copiloto.voice.VoiceCaptureActivity
import ar.com.copiloto.voice.VoiceInputBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

/** Owns the entire session: projection, notification, overlays and pending work. */
class MediaProjectionService : Service() {
    private val main = Handler(Looper.getMainLooper())
    private var capture: ScreenCaptureManager? = null
    private var overlay: OverlayController? = null
    private var stopping = false
    private var stopMessage: String? = null
    private var captureInProgress = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var api: HttpAssistantApi
    private lateinit var speaker: GuidanceSpeaker
    private val conversation = ConversationContext()
    private var request: Job? = null
    private var generation = 0
    private var pendingStart: Runnable? = null
    private var lastGuidance: GuidanceResponse? = null

    override fun onCreate() {
        super.onCreate()
        api = HttpAssistantApi(ConnectionSettings(this))
        speaker = GuidanceSpeaker(this) { speaking ->
            if (!stopping && !captureInProgress) overlay?.setState(if (speaking) BubbleState.SPEAKING else BubbleState.IDLE)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP || intent == null) {
            stopSession()
            return START_NOT_STICKY
        }
        if (capture != null || stopping) return START_NOT_STICKY
        try {
            val notification = createNotification()
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
            } else startForeground(NOTIFICATION_ID, notification)

            check(Settings.canDrawOverlays(this)) { "Falta el permiso para mostrar la burbuja. Volvé a concederlo." }
            val consentData = if (Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(EXTRA_RESULT_DATA)
            }
            checkNotNull(consentData) { "Falta el permiso de pantalla. Volvé a iniciar el acompañamiento." }
            val projection = getSystemService(MediaProjectionManager::class.java).getMediaProjection(
                intent.getIntExtra(EXTRA_RESULT_CODE, 0), consentData,
            )
            capture = ScreenCaptureManager(requireNotNull(projection), ScreenGeometry.read(this)) {
                stopSession("Android detuvo la captura. Podés iniciar otra sesión cuando quieras.")
            }.also { manager ->
                manager.start { result ->
                    if (stopping) return@start
                    result.onSuccess {
                        runCatching {
                            overlay = OverlayController(this, onCapture = ::captureOne, onStop = { stopSession() },
                                onVoice = ::askByVoice, onQuestion = { ask(question = it) },
                                onCancel = { cancelAssistance(); overlay?.showInput() },
                                onRepeat = { lastGuidance?.let { speaker.speak(it.message, it.audioPcm) } }).also { it.show() }
                            AssistanceSessionManager.started()
                            Log.i(TAG, "session_started")
                        }.onFailure { stopSession("No pude mostrar la burbuja. Revisá el permiso y volvé a probar.") }
                    }.onFailure { stopSession("No pude iniciar la captura. Volvé a conceder el permiso de pantalla.") }
                }
            }
        } catch (error: Exception) {
            Log.w(TAG, "session_start_failed: ${error.javaClass.simpleName}")
            stopSession("No pude iniciar el acompañamiento. Revisá los permisos y volvé a probar.")
        }
        return START_NOT_STICKY
    }

    private fun captureOne() {
        if (captureInProgress || stopping) return
        if (!Settings.canDrawOverlays(this)) {
            stopSession("Se desactivó el permiso de la burbuja. Podés volver a concederlo desde la app.")
            return
        }
        captureInProgress = true
        AssistanceSessionManager.capturing()
        overlay?.prepareCapture()
        // Let SurfaceFlinger remove our windows before attaching the capture surface.
        main.postDelayed({
            if (stopping) return@postDelayed
            capture?.capture(ScreenGeometry.read(this)) { result ->
                captureInProgress = false
                if (stopping) return@capture
                result.onSuccess { frame ->
                    val summary = "${frame.screenWidth} × ${frame.screenHeight} · ${frame.jpeg.size / 1024} KB"
                    AssistanceSessionManager.captured(summary)
                    Log.i(TAG, "capture_success ${frame.imageWidth}x${frame.imageHeight} bytes=${frame.jpeg.size} protected=${frame.likelyProtected}")
                    runCatching { overlay?.showCapture(frame) }
                        .onFailure { stopSession("No pude mostrar el panel. Revisá el permiso de la burbuja.") }
                }.onFailure {
                    val message = "No pude ver esta pantalla. Probá nuevamente con el celular desbloqueado."
                    AssistanceSessionManager.error(message)
                    runCatching { overlay?.showError(message) }
                        .onFailure { stopSession(message) }
                    Log.w(TAG, "capture_failed: ${it.javaClass.simpleName}")
                }
            }
        }, 350)
    }

    private fun askByVoice() {
        if (captureInProgress || stopping) return
        speaker.stop()
        val id = VoiceInputBus.await { result ->
            captureInProgress = false
            if (stopping) return@await
            result.onSuccess { ask(audio = it) }.onFailure {
                AssistanceSessionManager.idle()
                overlay?.showInput()
            }
        }
        runCatching {
            startActivity(Intent(this, VoiceCaptureActivity::class.java)
                .putExtra("voiceRequestId", id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            captureInProgress = true
            AssistanceSessionManager.processing("Escuchando tu consulta…")
            overlay?.prepareVoice()
        }.onFailure {
            VoiceInputBus.clear()
            captureInProgress = false
            overlay?.showError("No pude abrir el micrófono. Podés escribir tu consulta.")
            AssistanceSessionManager.error("No pude abrir el micrófono.")
        }
    }

    private fun ask(question: String? = null, audio: ByteArray? = null) {
        if (captureInProgress || stopping) return
        if (!Settings.canDrawOverlays(this)) { stopSession("Falta el permiso de la burbuja."); return }
        speaker.stop()
        captureInProgress = true
        val current = ++generation
        overlay?.prepareCapture()
        AssistanceSessionManager.processing("Estoy mirando tu pantalla…")
        val geometry = ScreenGeometry.read(this)
        pendingStart = Runnable {
            if (stopping || generation != current) return@Runnable
            capture?.capture(geometry) { result ->
                if (stopping || generation != current) return@capture
                val screen = result.getOrNull()
                overlay?.showLoading()
                request = scope.launch {
                    try {
                        val guidance = api.getGuidance(screen, geometry.width, geometry.height, question, audio, conversation.snapshot())
                        if (stopping || generation != current) return@launch
                        captureInProgress = false
                        val now = ScreenGeometry.read(this@MediaProjectionService)
                        if (now.width != geometry.width || now.height != geometry.height) {
                            overlay?.showError("La pantalla cambió de orientación. Volvé a preguntar para ubicar la indicación.")
                            AssistanceSessionManager.error("La pantalla cambió. Volvé a consultar.")
                            return@launch
                        }
                        lastGuidance = guidance
                        conversation.add(question ?: guidance.heardQuestion, guidance.message)
                        AssistanceSessionManager.answered(guidance.message)
                        overlay?.showGuidance(guidance)
                        speaker.speak(guidance.message, guidance.audioPcm)
                        Log.i(TAG, "guidance_received risk=${guidance.risk} target=${guidance.canHighlight}")
                    } catch (_: CancellationException) {
                        // Cancellation is an intentional user action.
                    } catch (error: Exception) {
                        if (stopping || generation != current) return@launch
                        captureInProgress = false
                        val message = when (error) {
                            is java.net.UnknownHostException, is java.net.ConnectException -> "No tengo conexión en este momento. Revisá internet y volvé a intentar."
                            is java.net.SocketTimeoutException -> "La respuesta está tardando demasiado. Probá nuevamente."
                            else -> error.message ?: "No pude entender esta pantalla. Intentemos otra vez."
                        }
                        AssistanceSessionManager.error(message)
                        overlay?.showError(message)
                        speaker.speak(message, null)
                    }
                }
            }
        }.also { main.postDelayed(it, 400) }
    }

    private fun cancelAssistance() {
        generation++
        pendingStart?.let { main.removeCallbacks(it) }
        pendingStart = null
        capture?.cancelCapture()
        api.cancel()
        request?.cancel()
        request = null
        captureInProgress = false
        speaker.stop()
        AssistanceSessionManager.idle()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        overlay?.onConfigurationChanged()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopSession()
        super.onTaskRemoved(rootIntent)
    }

    private fun stopSession(message: String? = null) {
        if (stopping) return
        stopping = true
        stopMessage = message
        cleanup()
        stopSelf()
    }

    private fun cleanup() {
        VoiceInputBus.clear()
        cancelAssistance()
        main.removeCallbacksAndMessages(null)
        overlay?.close()
        overlay = null
        capture?.close()
        capture = null
        captureInProgress = false
        lastGuidance = null
        conversation.clear()
        AssistanceSessionManager.stopped(stopMessage)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        stopping = true
        cleanup()
        speaker.close()
        scope.cancel()
        Log.i(TAG, "session_stopped")
        super.onDestroy()
    }

    private fun createNotification(): Notification {
        val notifications = getSystemService(NotificationManager::class.java)
        notifications.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Acompañamiento digital",
            NotificationManager.IMPORTANCE_LOW).apply {
            description = "Se muestra mientras Copiloto te acompaña."
        })
        val open = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1,
            Intent(this, MediaProjectionService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_copiloto)
            .setContentTitle("Copiloto digital activo")
            .setContentText("Tocá la burbuja cuando necesites ayuda.")
            .setContentIntent(open)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .addAction(Notification.Action.Builder(null, "Finalizar", stop).build())
            .build()
    }

    companion object {
        const val ACTION_START = "ar.com.copiloto.START"
        const val ACTION_STOP = "ar.com.copiloto.STOP"
        const val EXTRA_RESULT_CODE = "projectionResultCode"
        const val EXTRA_RESULT_DATA = "projectionResultData"
        private const val CHANNEL_ID = "digital_assistant"
        private const val NOTIFICATION_ID = 1001
        private const val TAG = "Copiloto"
    }
}
