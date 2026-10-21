package ar.com.copiloto.capture

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager

data class ScreenGeometry(val width: Int, val height: Int, val densityDpi: Int) {
    companion object {
        @Suppress("DEPRECATION")
        fun read(context: Context): ScreenGeometry {
            val windowManager = context.getSystemService(WindowManager::class.java)
            val (width, height) = if (Build.VERSION.SDK_INT >= 30) {
                val bounds = windowManager.maximumWindowMetrics.bounds
                bounds.width() to bounds.height()
            } else {
                val metrics = DisplayMetrics()
                windowManager.defaultDisplay.getRealMetrics(metrics)
                metrics.widthPixels to metrics.heightPixels
            }
            return ScreenGeometry(width, height, context.resources.configuration.densityDpi)
        }
    }
}
