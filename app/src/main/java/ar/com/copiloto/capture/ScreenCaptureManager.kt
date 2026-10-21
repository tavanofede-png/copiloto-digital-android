package ar.com.copiloto.capture

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

data class ScreenCapture(
    val jpeg: ByteArray,
    val imageWidth: Int,
    val imageHeight: Int,
    val screenWidth: Int,
    val screenHeight: Int,
    val likelyProtected: Boolean,
)

/** One projection and virtual display per consent; the surface is detached between requests. */
class ScreenCaptureManager(
    private val projection: MediaProjection,
    private var geometry: ScreenGeometry,
    private val onProjectionStopped: () -> Unit,
) {
    private val thread = HandlerThread("CopilotoCapture").apply { start() }
    private val worker = Handler(thread.looper)
    private val main = Handler(Looper.getMainLooper())
    private val closing = AtomicBoolean(false)
    private var virtualDisplay: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private var pending: ((Result<ScreenCapture>) -> Unit)? = null
    private var pendingGeometry: ScreenGeometry? = null
    private var released = false

    private val timeout = Runnable {
        finish(Result.failure(IllegalStateException("No llegó una imagen. Volvé a probar con la pantalla desbloqueada.")))
    }
    private val callback = object : MediaProjection.Callback() {
        override fun onStop() {
            if (closing.compareAndSet(false, true)) {
                release(stopProjection = false)
                main.post(onProjectionStopped)
                thread.quitSafely()
            }
        }
        override fun onCapturedContentResize(width: Int, height: Int) {
            if (width <= 0 || height <= 0 || released) return
            if (geometry.width != width || geometry.height != height) {
                if (pending != null) {
                    finish(Result.failure(IllegalStateException("La pantalla cambió de tamaño. Volvé a pedir la captura.")))
                }
                geometry = geometry.copy(width = width, height = height)
                virtualDisplay?.resize(width, height, geometry.densityDpi)
            }
        }
    }

    fun start(onReady: (Result<Unit>) -> Unit) {
        worker.post {
            val result = runCatching {
                check(!closing.get())
                projection.registerCallback(callback, worker)
                virtualDisplay = projection.createVirtualDisplay(
                    "CopilotoOnDemand", geometry.width, geometry.height, geometry.densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    null, null, worker,
                )
                checkNotNull(virtualDisplay)
                Unit
            }
            main.post { onReady(result) }
        }
    }

    fun capture(screen: ScreenGeometry, onResult: (Result<ScreenCapture>) -> Unit) {
        if (closing.get()) {
            main.post { onResult(Result.failure(IllegalStateException("La sesión terminó. Volvé a iniciarla."))) }
            return
        }
        worker.post {
            if (pending != null) {
                main.post { onResult(Result.failure(IllegalStateException("Ya hay una captura en curso."))) }
                return@post
            }
            pending = onResult
            pendingGeometry = screen
            runCatching {
                check(!released) { "La sesión terminó. Volvé a iniciarla." }
                if (geometry != screen) {
                    virtualDisplay?.resize(screen.width, screen.height, screen.densityDpi)
                    geometry = screen
                }
                val imageReader = ImageReader.newInstance(screen.width, screen.height, PixelFormat.RGBA_8888, 2)
                reader = imageReader
                imageReader.setOnImageAvailableListener({ source ->
                    var image: Image? = null
                    var converted: Result<ScreenCapture>? = null
                    try {
                        image = source.acquireLatestImage()
                        val availableImage = image
                        if (availableImage != null && pending != null) {
                            converted = runCatching { encode(availableImage, pendingGeometry ?: screen) }
                        }
                    } catch (error: Exception) {
                        converted = Result.failure(error)
                    } finally {
                        image?.close()
                    }
                    // Close the image before closing its reader and surface.
                    converted?.let(::finish)
                }, worker)
                worker.postDelayed(timeout, 5_000)
                checkNotNull(virtualDisplay).surface = imageReader.surface
            }.onFailure { finish(Result.failure(it)) }
        }
    }

    private fun encode(image: Image, screen: ScreenGeometry): ScreenCapture {
        val plane = image.planes[0]
        val paddedWidth = plane.rowStride / plane.pixelStride
        val padded = Bitmap.createBitmap(paddedWidth, image.height, Bitmap.Config.ARGB_8888)
        var cropped: Bitmap? = null
        var scaled: Bitmap? = null
        try {
            padded.copyPixelsFromBuffer(plane.buffer)
            val crop = image.cropRect
            val frame = Bitmap.createBitmap(padded, crop.left, crop.top, crop.width(), crop.height())
            cropped = frame
            val maxDimension = maxOf(frame.width, frame.height)
            val ratio = minOf(1f, 1920f / maxDimension, 1080f / frame.width)
            val output = if (ratio < 1f) Bitmap.createScaledBitmap(
                frame, (frame.width * ratio).roundToInt().coerceAtLeast(1),
                (frame.height * ratio).roundToInt().coerceAtLeast(1), true,
            ) else frame
            scaled = output
            val bytes = ByteArrayOutputStream().use {
                check(output.compress(Bitmap.CompressFormat.JPEG, 80, it))
                it.toByteArray()
            }
            return ScreenCapture(bytes, output.width, output.height, screen.width, screen.height,
                likelyProtected = looksUnavailable(output))
        } finally {
            scaled?.takeIf { it !== cropped && it !== padded }?.recycle()
            cropped?.takeIf { it !== padded }?.recycle()
            padded.recycle()
        }
    }

    private fun looksUnavailable(bitmap: Bitmap): Boolean {
        var dark = 0
        var samples = 0
        var darkest = 255
        var brightest = 0
        // Ignore system bars. This is a heuristic, never a guarantee of FLAG_SECURE detection.
        for (y in 2 until 18) for (x in 1 until 10) {
            val pixel = bitmap.getPixel(bitmap.width * x / 10, bitmap.height * y / 20)
            val red = Color.red(pixel)
            val green = Color.green(pixel)
            val blue = Color.blue(pixel)
            if (red < 18 && green < 18 && blue < 18) dark++
            val luminance = (red * 299 + green * 587 + blue * 114) / 1000
            darkest = minOf(darkest, luminance)
            brightest = maxOf(brightest, luminance)
            samples++
        }
        return dark.toFloat() / samples > 0.97f || brightest - darkest < 4
    }

    private fun finish(result: Result<ScreenCapture>) {
        val listener = pending ?: return
        pending = null
        pendingGeometry = null
        worker.removeCallbacks(timeout)
        runCatching { virtualDisplay?.surface = null }
        reader?.setOnImageAvailableListener(null, null)
        reader?.close()
        reader = null
        main.post { listener(result) }
    }

    fun close() {
        if (closing.compareAndSet(false, true)) {
            worker.post {
                release(stopProjection = true)
                thread.quitSafely()
            }
        }
    }

    private fun release(stopProjection: Boolean) {
        if (released) return
        finish(Result.failure(IllegalStateException("La sesión terminó.")))
        released = true
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
        runCatching { projection.unregisterCallback(callback) }
        if (stopProjection) runCatching { projection.stop() }
        worker.removeCallbacksAndMessages(null)
    }
}
