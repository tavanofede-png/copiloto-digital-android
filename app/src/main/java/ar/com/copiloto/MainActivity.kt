package ar.com.copiloto

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ar.com.copiloto.capture.MediaProjectionService
import ar.com.copiloto.permissions.PermissionManager
import ar.com.copiloto.session.AssistanceSessionManager
import ar.com.copiloto.ui.home.HomeScreen
import ar.com.copiloto.ui.onboarding.PermissionScreen
import ar.com.copiloto.ui.session.ActiveSessionScreen
import ar.com.copiloto.ui.theme.CopilotoTheme

class MainActivity : ComponentActivity() {
    private val permissions by lazy { PermissionManager(this) }
    private var setup by mutableStateOf(false)
    private var overlayGranted by mutableStateOf(false)
    private var permissionPending by mutableStateOf(false)
    private var error by mutableStateOf<String?>(null)

    private val overlayPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        permissionPending = false
        overlayGranted = permissions.canShowOverlay()
        if (!overlayGranted) error = "La burbuja necesita ese permiso. Podés volver a intentarlo cuando quieras."
    }
    private val notificationsPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        requestScreenCapture()
    }
    private val captureConsent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        permissionPending = false
        val consent = result.data
        if (result.resultCode == Activity.RESULT_OK && consent != null) {
            AssistanceSessionManager.starting()
            runCatching {
                startForegroundService(Intent(this, MediaProjectionService::class.java).apply {
                    action = MediaProjectionService.ACTION_START
                    putExtra(MediaProjectionService.EXTRA_RESULT_CODE, result.resultCode)
                    putExtra(MediaProjectionService.EXTRA_RESULT_DATA, consent)
                })
            }.onFailure {
                AssistanceSessionManager.stopped("No pude iniciar la sesión. Volvé a intentarlo con la app abierta.")
            }
        } else {
            error = "No compartiste la pantalla. Podés volver a iniciar cuando quieras."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setup = savedInstanceState?.getBoolean("setup") ?: false
        permissionPending = savedInstanceState?.getBoolean("permissionPending") ?: false
        setContent {
            val session by AssistanceSessionManager.state.collectAsStateWithLifecycle()
            LaunchedEffect(session.active) { if (session.active) setup = false }
            BackHandler(enabled = setup && !permissionPending && !session.starting) { setup = false }
            CopilotoTheme {
                when {
                    session.active -> ActiveSessionScreen(session, ::openBrowser) {
                        stopService(Intent(this, MediaProjectionService::class.java))
                        setup = false
                    }
                    setup || session.starting -> PermissionScreen(
                        overlayGranted, permissionPending || session.starting, error ?: session.error,
                        ::continueSetup, { setup = false; error = null },
                    )
                    else -> HomeScreen(onStart = {
                        overlayGranted = permissions.canShowOverlay()
                        error = null
                        setup = true
                    }, error = error ?: session.error)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        overlayGranted = permissions.canShowOverlay()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("setup", setup)
        outState.putBoolean("permissionPending", permissionPending)
        super.onSaveInstanceState(outState)
    }

    private fun continueSetup() {
        error = null
        if (!permissions.canShowOverlay()) {
            permissionPending = true
            runCatching { overlayPermission.launch(permissions.overlaySettingsIntent()) }.onFailure {
                permissionPending = false
                error = "No pude abrir los permisos. Buscá Copiloto en Ajustes > Mostrar sobre otras apps."
            }
        } else if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissionPending = true
            notificationsPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else requestScreenCapture()
    }

    private fun requestScreenCapture() {
        permissionPending = true
        runCatching { captureConsent.launch(permissions.captureConsentIntent()) }.onFailure {
            permissionPending = false
            error = "Este dispositivo no permite compartir la pantalla. Probá con otro teléfono Android."
        }
    }

    private fun openBrowser() {
        val uri = Uri.parse("https://example.com")
        val chrome = Intent(Intent.ACTION_VIEW, uri).setPackage("com.android.chrome")
        runCatching { startActivity(chrome) }.recoverCatching {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }.onFailure { AssistanceSessionManager.error("Abrí tu navegador desde la pantalla de inicio del teléfono.") }
    }
}
