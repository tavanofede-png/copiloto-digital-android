package ar.com.copiloto.ui.session

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ar.com.copiloto.session.SessionState

@Composable
fun ActiveSessionScreen(session: SessionState, onOpenBrowser: () -> Unit, onStop: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.safeDrawingPadding().fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Text("Copiloto · Sesión", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(28.dp))
            Text("Acompañamiento\nactivo", style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold)
            Text("Podés salir de esta aplicación.", style = MaterialTheme.typography.titleLarge)
            Text("Cuando necesites ayuda, tocá la burbuja. Podés arrastrarla para que no tape lo que querés ver.",
                style = MaterialTheme.typography.bodyLarge)
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Un paso a la vez", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Abrí otra app, tocá la burbuja y preguntá por voz o escribiendo. Copiloto te explica el próximo paso y señala dónde tocar.",
                        style = MaterialTheme.typography.bodyLarge)
                    if (session.capturing) LinearProgressIndicator(Modifier.fillMaxWidth())
                    session.status?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    session.lastMessage?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
                    session.captureSummary?.let { Text("Última captura: $it", style = MaterialTheme.typography.bodyMedium) }
                }
            }
            session.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = onOpenBrowser, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = RoundedCornerShape(18.dp)) {
                Text("Abrir navegador", style = MaterialTheme.typography.titleMedium)
            }
            OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = RoundedCornerShape(18.dp)) {
                Text("Finalizar sesión", style = MaterialTheme.typography.titleMedium)
            }
            Text("Solo capturamos cuando lo pedís. No guardamos un historial de imágenes.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
