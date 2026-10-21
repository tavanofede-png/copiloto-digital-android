package ar.com.copiloto.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Colors = lightColorScheme(
    primary = Color(0xFF275F52),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE1EBE4),
    onPrimaryContainer = Color(0xFF163F35),
    background = Color(0xFFF6F5F0),
    onBackground = Color(0xFF222C28),
    surface = Color(0xFFFFFEFA),
    onSurface = Color(0xFF222C28),
    onSurfaceVariant = Color(0xFF52635A),
)

@Composable
fun CopilotoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = Typography(), content = content)
}
