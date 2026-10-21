package ar.com.copiloto.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PermissionScreen(overlayGranted: Boolean, busy: Boolean, error: String?, onContinue: () -> Unit, onBack: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.safeDrawingPadding().fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)) {
            TextButton(onClick = onBack, enabled = !busy) { Text("Volver") }
            Text("${if (overlayGranted) 2 else 1} de 2 · Preparar acompañamiento",
                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            LinearProgressIndicator(progress = { if (overlayGranted) 1f else .5f }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(32.dp))
            Text(if (overlayGranted) "Ver tu pantalla" else "Ayuda sobre otras apps",
                style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
            Text(if (overlayGranted)
                "Para poder indicarte dónde tocar, necesito ver la pantalla mientras dura la sesión."
                else "Voy a mostrar una pequeña burbuja mientras navegás. Podés moverla a un costado cuando lo necesites.",
                style = MaterialTheme.typography.titleMedium)
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Text(if (overlayGranted)
                    "Solo tomamos una imagen cuando pedís ayuda. No grabamos video ni guardamos capturas en tu galería. Podés finalizar cuando quieras."
                    else "En la siguiente pantalla, habilitá “Permitir mostrar sobre otras apps” para Copiloto y volvé acá.",
                    Modifier.padding(20.dp), style = MaterialTheme.typography.bodyLarge)
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge) }
            Button(onClick = onContinue, enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp), shape = RoundedCornerShape(18.dp)) {
                Text(if (busy) "Iniciando…" else if (overlayGranted) "Permitir y comenzar" else "Permitir burbuja",
                    Modifier.padding(vertical = 8.dp), style = MaterialTheme.typography.titleMedium)
            }
            Text("En esta primera prueba solo necesitamos burbuja y pantalla. El micrófono se habilitará cuando esté disponible la ayuda por voz.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
