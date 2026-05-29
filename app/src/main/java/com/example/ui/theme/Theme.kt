package com.dhananan.civicage.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle

private val ProfessionalLightColorScheme = lightColorScheme(
    primary = Color(0xFF0F172A), // #0F172A Primary Accent
    secondary = Color(0xFF1D4ED8), // Civic blue #1D4ED8
    tertiary = Color(0xFFEFF6FF), // Soft blue #EFF6FF
    background = Color(0xFFFFFFFF), // Background #FFFFFF
    surface = Color(0xFFF8FAFC), // Surface/card #F8FAFC
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF111827), // Primary Text #111827
    onSurface = Color(0xFF111827),
    outline = Color(0xFFE5E7EB), // Border #E5E7EB
    error = Color(0xFFB91C1C) // Error #B91C1C
)

private val BlackAndWhiteColorScheme = lightColorScheme(
    primary = Color(0xFF000000),
    secondary = Color(0xFF000000),
    tertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF000000),
    onSurface = Color(0xFF000000),
    outline = Color(0xFF000000),
    error = Color(0xFF000000)
)

private val ProfessionalDarkColorScheme = darkColorScheme(
    primary = Color(0xFF60A5FA), // Accent #60A5FA
    secondary = Color(0xFF3B82F6),
    tertiary = Color(0xFF111827),
    background = Color(0xFF0B0F14), // Background #0B0F14
    surface = Color(0xFF111827), // Surface #111827
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color(0xFFF9FAFB), // Text #F9FAFB
    onSurface = Color(0xFFF9FAFB),
    outline = Color(0xFF374151), // Border #374151
    error = Color(0xFFEF4444)
)

@Composable
fun MyApplicationTheme(
    appearanceMode: String = "Professional Light",
    fontSizeMode: String = "Standard",
    content: @Composable () -> Unit,
) {
    val colorScheme = when (appearanceMode) {
        "Black & White" -> BlackAndWhiteColorScheme
        "Dark" -> ProfessionalDarkColorScheme
        else -> ProfessionalLightColorScheme
    }

    val scaleFactor = when (fontSizeMode) {
        "Large" -> 3f
        "Extra Large" -> 6f
        else -> 0f
    }

    val scaledTypography = androidx.compose.material3.Typography(
        bodyLarge = Typography.bodyLarge.copy(
            fontSize = (17f + scaleFactor).sp,
            lineHeight = (25f + scaleFactor).sp
        ),
        bodyMedium = Typography.bodyMedium.copy(
            fontSize = (15f + scaleFactor).sp,
            lineHeight = (22f + scaleFactor).sp
        ),
        titleLarge = Typography.titleLarge.copy(
            fontSize = (22f + scaleFactor).sp,
            lineHeight = (28f + scaleFactor).sp
        ),
        titleMedium = Typography.titleMedium.copy(
            fontSize = (20f + scaleFactor).sp,
            lineHeight = (26f + scaleFactor).sp
        ),
        displayMedium = Typography.displayMedium.copy(
            fontSize = (26f + scaleFactor).sp,
            lineHeight = (34f + scaleFactor).sp
        ),
        labelLarge = Typography.labelLarge.copy(
            fontSize = (17f + scaleFactor).sp,
            lineHeight = (24f + scaleFactor).sp
        )
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = scaledTypography,
        content = content
    )
}
