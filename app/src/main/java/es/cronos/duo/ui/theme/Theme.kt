package es.cronos.duo.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = TextDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    onBackground = TextDark, // Texto principal en fondo
    onSurface = TextDark,     // Texto principal en superficie
    secondary = SecondaryTextDark, // Para texto secundario/ítems menos prominentes
    tertiary = SecondaryTextDark,  // Para texto menos prominente, como los grises de slate-400
    // Añadir más colores si son necesarios (ej. error, outline)
    outline = BorderDark // Usado a menudo para bordes (como ring-1 ring-white/5)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = TextDark, // El texto sobre el botón azul debe ser blanco/casi negro
    background = BackgroundLight,
    surface = SurfaceLight,
    onBackground = TextLight,
    onSurface = TextLight,
    secondary = SecondaryTextLight,
    tertiary = SecondaryTextLight,
    // Añadir más colores si son necesarios (ej. error, outline)
    outline = BorderLight
)

@Composable
fun DuoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            dynamicDarkColorScheme(context)
//            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}