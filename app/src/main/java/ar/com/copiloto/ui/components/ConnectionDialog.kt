package ar.com.copiloto.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import ar.com.copiloto.assistant.ConnectionSettings
import ar.com.copiloto.assistant.HttpAssistantApi
import kotlinx.coroutines.launch

@Composable
fun ConnectionDialog(settings: ConnectionSettings, onDismiss: () -> Unit, onSaved: () -> Unit) {
    var url by remember { mutableStateOf(settings.baseUrl) }
    var code by remember { mutableStateOf(settings.token) }
    var status by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    AlertDialog(onDismissRequest = { if (!testing) onDismiss() }, title = { Text("Conectar Copiloto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Pegá el código de conexión recibido con el APK. La clave de OpenAI queda en el servidor.")
                OutlinedTextField(value = code, onValueChange = { code = it }, singleLine = true,
                    label = { Text("Código de conexión") }, visualTransformation = PasswordVisualTransformation(),
                    enabled = !testing, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = url, onValueChange = { url = it }, singleLine = true,
                    label = { Text("Servidor HTTPS") }, enabled = !testing, modifier = Modifier.fillMaxWidth())
                status?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                if (testing) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(enabled = !testing, onClick = {
            runCatching { settings.save(url, code) }.onSuccess {
                testing = true
                scope.launch {
                    runCatching { HttpAssistantApi(settings).testConnection() }
                        .onSuccess { status = it; onSaved() }
                        .onFailure { status = it.message ?: "No pude conectar. Revisá el código y probá otra vez." }
                    testing = false
                }
            }.onFailure { status = it.message }
        }) { Text(if (testing) "Comprobando…" else "Guardar y probar") } },
        dismissButton = { TextButton(enabled = !testing, onClick = onDismiss) { Text("Cerrar") } })
}
