package ar.com.copiloto.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import ar.com.copiloto.assistant.GuidanceTarget
import ar.com.copiloto.capture.ScreenGeometry

/** Non-interactive, short-lived mark. It never injects a touch into another app. */
class HighlightOverlay(private val context: Context) {
    private val windows = context.getSystemService(WindowManager::class.java)
    private val main = Handler(Looper.getMainLooper())
    private var view: View? = null
    fun show(target: GuidanceTarget) {
        clear()
        if (!target.isUsable()) return
        val geometry = ScreenGeometry.read(context)
        val mark = object : View(context) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(0, 105, 75)
                style = Paint.Style.STROKE
                strokeWidth = 5 * resources.displayMetrics.density
                if (target.confidence < .8) pathEffect = DashPathEffect(floatArrayOf(14f, 10f), 0f)
            }
            val rectangle = RectF()
            val location = IntArray(2)
            override fun onDraw(canvas: Canvas) {
                getLocationOnScreen(location)
                val w = target.width!! * geometry.width
                val h = target.height!! * geometry.height
                val x = target.x!! * geometry.width - location[0]
                val y = target.y!! * geometry.height - location[1]
                rectangle.set((x - w / 2).toFloat().coerceAtLeast(4f),
                    (y - h / 2).toFloat().coerceAtLeast(4f),
                    (x + w / 2).toFloat().coerceAtMost(width - 4f),
                    (y + h / 2).toFloat().coerceAtMost(height - 4f))
                canvas.drawRoundRect(rectangle, 18f, 18f, paint)
            }
        }
        val parameters = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            // Android 12+ allows touches through an untrusted overlay only below the opacity limit.
            alpha = .65f
        }
        runCatching { windows.addView(mark, parameters); view = mark; main.postDelayed({ clear() }, 6_000) }
    }
    fun clear() {
        main.removeCallbacksAndMessages(null)
        view?.let { runCatching { windows.removeViewImmediate(it) } }
        view = null
    }
}
