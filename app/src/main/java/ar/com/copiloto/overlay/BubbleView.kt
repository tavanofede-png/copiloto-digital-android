package ar.com.copiloto.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View

enum class BubbleState { IDLE, LISTENING, THINKING, SPEAKING, ERROR }

class BubbleView(context: Context) : View(context) {
    var state = BubbleState.IDLE
        set(value) {
            field = value
            contentDescription = when (value) {
                BubbleState.IDLE -> "Abrir Copiloto. Arrastrá para mover la burbuja."
                BubbleState.THINKING -> "Copiloto está preparando tu indicación"
                BubbleState.LISTENING -> "Escuchando tu consulta"
                BubbleState.SPEAKING -> "Copiloto está hablando"
                BubbleState.ERROR -> "Copiloto necesita tu atención"
            }
            invalidate()
        }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val arrow = Path()

    init {
        isClickable = true
        isFocusable = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        state = BubbleState.IDLE
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        arrow.reset()
        arrow.moveTo(w * .31f, h * .28f)
        arrow.lineTo(w * .73f, h * .46f)
        arrow.lineTo(w * .53f, h * .55f)
        arrow.lineTo(w * .44f, h * .75f)
        arrow.close()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val center = width / 2f
        paint.color = Color.WHITE
        canvas.drawCircle(center, center, center - 1, paint)
        paint.color = if (state == BubbleState.ERROR) Color.rgb(153, 69, 37) else Color.rgb(39, 95, 82)
        canvas.drawCircle(center, center, center - 4 * resources.displayMetrics.density, paint)
        paint.color = Color.WHITE
        if (state == BubbleState.IDLE) {
            canvas.drawPath(arrow, paint)
        } else {
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = height * .45f
            paint.isFakeBoldText = true
            val symbol = when (state) {
                BubbleState.ERROR -> "!"
                BubbleState.LISTENING -> "●"
                BubbleState.SPEAKING -> "≈"
                else -> "…"
            }
            canvas.drawText(symbol, center,
                center - (paint.ascent() + paint.descent()) / 2f, paint)
            paint.isFakeBoldText = false
        }
    }

    override fun performClick(): Boolean = super.performClick()
}
