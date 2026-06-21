package com.sillygirl.client.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight

// ===== 品牌配色 =====
val Purple40 = Color(0xFF6750A4)
val PurpleGrey40 = Color(0xFF625B71)
val Pink40 = Color(0xFF7D5260)

val Purple70 = Color(0xFF371E73)
val Purple80 = Color(0xFF8067B2)
val Pink80 = Color(0xFFEBA1CC)

val PrimaryGradientColors = listOf(
    Color(0xFF667EEA),
    Color(0xFF764BA2),
)

val SecondaryGradientColors = listOf(
    Color(0xFF5CC3FF),
    Color(0xFF4FACFE),
)

val SuccessColor = Color(0xFF22C55E)
val WarningColor = Color(0xFFF59E0B)
val DangerColor = Color(0xFFEF4444)

val CardBackgroundLight = Color(0xFFFFFFFF)
val CardBackgroundDark = Color(0xFF1E1E2E)

val SurfaceLight = Color(0xFFF8F9FC)
val SurfaceDark = Color(0xFF121218)

@OptIn(ExperimentalMaterial3Api::class)
private val LightColorScheme = lightColorScheme(
    primary = Purple70,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8DEF8),
    secondary = Color(0xFF5B6770),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDEE2E6),
    tertiary = Pink40,
    background = SurfaceLight,
    surface = CardBackgroundLight,
    onBackground = Color(0xFF1A1A2E),
    onSurface = Color(0xFF1A1A2E),
    surfaceVariant = Color(0xFFF1F2F6),
    outline = Color(0xFFE0E0E0),
)

@OptIn(ExperimentalMaterial3Api::class)
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    onPrimary = Color(0xFF1A1A2E),
    primaryContainer = Color(0xFF2D1B4E),
    secondary = PurpleGrey40,
    onSecondary = Color(0xFF1A1A2E),
    secondaryContainer = Color(0xFF2D2D3A),
    tertiary = Pink80,
    background = SurfaceDark,
    surface = CardBackgroundDark,
    onBackground = Color.White,
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF1E1E2E),
    outline = Color(0xFF3A3A4A),
)

@Composable
fun SillyGirlTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(
            displayLarge = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
            headlineLarge = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            titleLarge = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            titleMedium = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            titleSmall = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
            bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal),
            bodyMedium = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
            labelLarge = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
        ),
        content = content
    )
}
