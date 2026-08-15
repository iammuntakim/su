package su.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

val AppPrimary: Color = Color(0xFF007AFF)

private val LightColors = lightColorScheme(
    primary = AppPrimary,
    primaryContainer = Color(0xFF5D9BFF),
    secondary = AppPrimary,
    secondaryContainer = Color(0xFF5D9BFF),
    background = Color(0xFFF7F7F7),
    surface = Color(0xFFF7F7F7),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF7F7F7),
    surfaceContainerHighest = Color(0xFFF2F2F2),
    onBackground = Color.Black,
    onSurface = Color.Black,
    outline = Color(0xFFD9D9D9),
    dividerLine = Color(0xFFE0E0E0),
)

private val DarkColors = darkColorScheme(
    primary = AppPrimary,
    primaryContainer = Color(0xFF5D9BFF),
    secondary = Color(0xFF5D9BFF),
    secondaryContainer = Color(0xFF5D9BFF),
    background = Color(0xFF121212),
    surface = Color(0xFF121212),
    surfaceContainer = Color(0xFF1E1E1E),
    surfaceContainerHigh = Color(0xFF252525),
    surfaceContainerHighest = Color(0xFF2D2D2D),
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0),
    outline = Color(0xFF3A3A3A),
    dividerLine = Color(0xFF333333),
)

@Composable
fun MiuixAppTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MiuixTheme(colors = colors, content = content)
}
