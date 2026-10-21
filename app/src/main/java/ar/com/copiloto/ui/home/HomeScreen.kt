package ar.com.copiloto.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ar.com.copiloto.AppBrand

@Composable
fun HomeScreen(onStart: () -> Unit, error: String? = null, onConnection: () -> Unit = {}) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.safeDrawingPadding().fillMaxSize()
                .verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Text(AppBrand.name, style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold)
            Text("Voz y pantalla · Realtime", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Box(Modifier.size(88.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center) {
                Text("↗", style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary)
            }
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Tu copiloto\ndigital", style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold)
                Text("Te acompaño paso a paso mientras usás tu celular.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Benefit("01", "Preguntá hablando", "Contame qué querés hacer.")
                Benefit("02", "Te mostramos dónde tocar", "Una indicación, un paso a la vez.")
                Benefit("03", "Vos mantenés el control", "Cada acción la hacés vos.")
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge) }
            Button(onClick = onStart,
                modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                shape = RoundedCornerShape(18.dp)) {
                Text("Iniciar acompañamiento", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp))
            }
            Text("Cuando necesites ayuda, tocá la burbuja. Solo analizamos la pantalla cuando pedís ayuda.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onConnection, modifier = Modifier.heightIn(min = 48.dp)) { Text("Configurar conexión") }
        }
    }
}

@Composable
private fun Benefit(number: String, title: String, detail: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
        Text(number, style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
