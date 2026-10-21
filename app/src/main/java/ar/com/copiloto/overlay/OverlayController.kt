package ar.com.copiloto.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import ar.com.copiloto.capture.ScreenCapture
import ar.com.copiloto.capture.ScreenGeometry
import kotlin.math.abs
import kotlin.math.roundToInt

/** All windows belong to the session service and are removed together on stop. */
class OverlayController(
    private val context: Context,
    private val onCapture: () -> Unit,
    private val onStop: () -> Unit,
) {
    private val windows = context.getSystemService(WindowManager::class.java)
    private val bubble = BubbleView(context)
    private var attached = false
    private var panel: View? = null
    private var preview: Bitmap? = null
    private var animator: ValueAnimator? = null
    private var busy = false
    private val bubbleParams = WindowManager.LayoutParams(
        dp(64), dp(64), WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
    ).apply {
        // Absolute pixel origin matches screenshots, including on RTL-configured devices.
        gravity = Gravity.TOP or Gravity.LEFT
        x = ScreenGeometry.read(context).width - dp(76)
        y = dp(200)
    }

    init { enableDrag() }

    fun show() {
        if (attached) return
        windows.addView(bubble, bubbleParams)
        attached = true
    }

    private fun enableDrag() {
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var dragging = false
        val slop = ViewConfiguration.get(context).scaledTouchSlop
        bubble.setOnClickListener {
            if (!busy) {
                if (panel != null) hidePanel() else showPanel()
            }
        }
        bubble.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    animator?.cancel()
                    downX = event.rawX
                    downY = event.rawY
                    startX = bubbleParams.x
                    startY = bubbleParams.y
                    dragging = false
                    view.animate().scaleX(.94f).scaleY(.94f).setDuration(80).start()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (abs(dx) > slop || abs(dy) > slop) dragging = true
                    if (dragging) {
                        bubbleParams.x = startX + dx.roundToInt()
                        bubbleParams.y = startY + dy.roundToInt()
                        clampPosition()
                        updatePosition()
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    view.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    if (dragging) snapToSide()
                    else if (event.actionMasked == MotionEvent.ACTION_UP) view.performClick()
                    true
                }
                else -> false
            }
        }
    }

    private fun clampPosition() {
        val size = ScreenGeometry.read(context)
        bubbleParams.x = bubbleParams.x.coerceIn(dp(8), (size.width - dp(72)).coerceAtLeast(dp(8)))
        bubbleParams.y = bubbleParams.y.coerceIn(dp(40), (size.height - dp(112)).coerceAtLeast(dp(40)))
    }

    private fun updatePosition() {
        if (attached) runCatching { windows.updateViewLayout(bubble, bubbleParams) }
    }

    private fun snapToSide() {
        val size = ScreenGeometry.read(context)
        val end = if (bubbleParams.x + dp(32) < size.width / 2) dp(8) else size.width - dp(72)
        animator = ValueAnimator.ofInt(bubbleParams.x, end).apply {
            duration = 180
            addUpdateListener {
                bubbleParams.x = it.animatedValue as Int
                clampPosition()
                updatePosition()
            }
            start()
        }
    }

    fun onConfigurationChanged() {
        animator?.cancel()
        clampPosition()
        updatePosition()
        hidePanel()
    }

    fun prepareCapture() {
        busy = true
        hidePanel()
        bubble.state = BubbleState.THINKING
        bubble.visibility = View.INVISIBLE
    }

    fun showCapture(capture: ScreenCapture) {
        busy = false
        bubble.visibility = View.VISIBLE
        bubble.state = if (capture.likelyProtected) BubbleState.ERROR else BubbleState.IDLE
        showPanel(capture = capture)
    }

    fun showError(message: String) {
        busy = false
        bubble.visibility = View.VISIBLE
        bubble.state = BubbleState.ERROR
        showPanel(error = message)
    }

    private fun showPanel(capture: ScreenCapture? = null, error: String? = null) {
        hidePanel()
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(20), dp(24), dp(20))
            background = GradientDrawable().apply {
                setColor(Color.rgb(255, 254, 250))
                cornerRadius = dp(24).toFloat()
                setStroke(dp(1), Color.rgb(219, 226, 219))
            }
        }
        content.addView(text(if (capture != null) "Tu pantalla, a pedido" else "Estoy acá para ayudarte", 23f, true))
        val detail = when {
            error != null -> error
            capture?.likelyProtected == true -> "Esta pantalla parece estar protegida o vacía. Probá en otra aplicación."
            capture != null -> "¿Reconocés la pantalla que estabas viendo? La imagen se borra al cerrar este panel."
            else -> "Abrí otra app y comprobá que puedo ver la pantalla cuando lo pedís."
        }
        content.addView(text(detail, 17f).apply { setPadding(0, dp(12), 0, dp(12)) })
        if (capture != null && !capture.likelyProtected) {
            preview = BitmapFactory.decodeByteArray(capture.jpeg, 0, capture.jpeg.size,
                BitmapFactory.Options().apply { inSampleSize = 2 })
            content.addView(ImageView(context).apply {
                setImageBitmap(preview)
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.FIT_CENTER
                contentDescription = "Vista previa de la pantalla capturada"
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(220)))
            content.addView(text("${capture.screenWidth} × ${capture.screenHeight} → ${capture.imageWidth} × ${capture.imageHeight} · ${capture.jpeg.size / 1024} KB", 14f)
                .apply { setPadding(0, dp(8), 0, dp(8)) })
        }
        content.addView(button(if (capture == null) "Probar captura" else "Capturar de nuevo") { onCapture() })
        content.addView(button("Cerrar panel") { hidePanel() })
        content.addView(button("Finalizar acompañamiento") { onStop() })
        val geometry = ScreenGeometry.read(context)
        val scroll = ScrollView(context).apply {
            isFillViewport = false
            addView(content)
        }
        val params = WindowManager.LayoutParams(
            minOf(geometry.width - dp(32), dp(400)), WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_SECURE,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.CENTER }
        // Bound height so controls remain reachable with large text and in landscape.
        scroll.measure(View.MeasureSpec.makeMeasureSpec(params.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        if (scroll.measuredHeight > geometry.height - dp(120)) params.height = geometry.height - dp(120)
        windows.addView(scroll, params)
        panel = scroll
    }

    fun hidePanel() {
        panel?.let { runCatching { windows.removeViewImmediate(it) } }
        panel = null
        preview?.recycle()
        preview = null
    }

    fun close() {
        animator?.cancel()
        animator = null
        hidePanel()
        if (attached) runCatching { windows.removeViewImmediate(bubble) }
        attached = false
    }

    private fun text(value: String, size: Float, bold: Boolean = false) = TextView(context).apply {
        text = value
        textSize = size
        setTextColor(Color.rgb(34, 44, 40))
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun button(label: String, action: () -> Unit) = Button(context).apply {
        text = label
        textSize = 16f
        isAllCaps = false
        minHeight = dp(52)
        setOnClickListener { action() }
    }

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).roundToInt()
}
