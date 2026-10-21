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

/** Owns the entire session: projection, notification, overlays and pending work. */
class MediaProjectionService : Service() {
    private val main = Handler(Looper.getMainLooper())
    private var capture: ScreenCaptureManager? = null
    private var overlay: OverlayController? = null
    private var stopping = false
    private var stopMessage: String? = null
    private var captureInProgress = false

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
                            overlay = OverlayController(this, ::captureOne) { stopSession() }.also { it.show() }
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
        main.removeCallbacksAndMessages(null)
        overlay?.close()
        overlay = null
        capture?.close()
        capture = null
        captureInProgress = false
        AssistanceSessionManager.stopped(stopMessage)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        stopping = true
        cleanup()
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
