package ar.com.copiloto.permissions

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.provider.Settings

class PermissionManager(private val context: Context) {
    fun canShowOverlay(): Boolean = Settings.canDrawOverlays(context)

    fun overlaySettingsIntent(): Intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}"),
    )

    fun captureConsentIntent(): Intent {
        val manager = context.getSystemService(MediaProjectionManager::class.java)
        // Full-display consent keeps coordinates consistent when the user switches apps.
        return if (Build.VERSION.SDK_INT >= 34) {
            manager.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay())
        } else {
            manager.createScreenCaptureIntent()
        }
    }
}
