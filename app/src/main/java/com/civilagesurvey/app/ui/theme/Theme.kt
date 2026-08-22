package com.civilagesurvey.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

private val LightColorScheme = lightColorScheme(
    primary = PrimaryAccent,
    secondary = ButtonBlue,
    background = BackgroundWhite,
    surface = SurfaceSlate,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = BorderGrey
)

private val WarmSepiaColorScheme = lightColorScheme(
    primary = Color(0xFF9A3412),
    secondary = Color(0xFFC2410C),
    background = Color(0xFFFDFBF7),
    surface = Color(0xFFF5EFE6),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF27272A),
    onSurface = Color(0xFF27272A),
    outline = Color(0xFFD6CEC2)
)

private val ModernIndigoColorScheme = lightColorScheme(
    primary = Color(0xFF4F46E5),
    secondary = Color(0xFF0D9488),
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFEEF2FF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    outline = Color(0xFFC7D2FE)
)

private val BlackWhiteColorScheme = lightColorScheme(
    primary = Color.Black,
    secondary = Color.Black,
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
    outline = Color.Black
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF60A5FA),
    secondary = Color(0xFF3B82F6),
    background = Color(0xFF0B0F14),
    surface = Color(0xFF111827),
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color(0xFFF9FAFB),
    onSurface = Color(0xFFF9FAFB),
    outline = Color(0xFF374151)
)

@Composable
fun DCATheme(
    appearanceMode: String = "Professional Light",
    fontSizeMode: String = "Standard",
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        appearanceMode.contains("Sepia", ignoreCase = true) -> WarmSepiaColorScheme
        appearanceMode.contains("Indigo", ignoreCase = true) -> ModernIndigoColorScheme
        appearanceMode.contains("Black", ignoreCase = true) ||
        appearanceMode.contains("B&W", ignoreCase = true) ||
        appearanceMode.contains("Contrast", ignoreCase = true) -> BlackWhiteColorScheme
        appearanceMode.contains("Dark", ignoreCase = true) -> DarkColorScheme
        else -> LightColorScheme
    }

    val baseSize = 16f
    val scale = when (fontSizeMode) {
        "Large" -> 4f
        "Extra Large" -> 8f
        else -> 0f
    }

    val typography = Typography(
        bodyLarge = Typography().bodyLarge.copy(fontSize = (baseSize + scale).sp),
        bodyMedium = Typography().bodyMedium.copy(fontSize = (baseSize - 2 + scale).sp),
        titleLarge = Typography().titleLarge.copy(fontSize = (baseSize + 6 + scale).sp),
        titleMedium = Typography().titleMedium.copy(fontSize = (baseSize + 2 + scale).sp),
        labelLarge = Typography().labelLarge.copy(fontSize = (baseSize + scale).sp)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
