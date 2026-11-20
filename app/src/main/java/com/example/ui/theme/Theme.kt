package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = SafetyAmber,
    onPrimary = Color.White,
    primaryContainer = SafetyAmberContainer,
    onPrimaryContainer = OnSafetyAmberContainer,
    secondary = SlateNavy,
    onSecondary = Color.White,
    secondaryContainer = VerifiedBlueContainer,
    onSecondaryContainer = VerifiedBlue,
    tertiary = PaidEmerald,
    onTertiary = Color.White,
    tertiaryContainer = PaidEmeraldContainer,
    onTertiaryContainer = PaidEmerald,
    background = BlueprintPaper,
    onBackground = SlateInk,
    surface = CrispCardWhite,
    onSurface = SlateInk,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = SlateMuted,
    outline = SlateBorder,
    error = DangerCrimson,
    onError = Color.White,
    errorContainer = DangerCrimsonContainer,
    onErrorContainer = DangerCrimson
)

private val DarkColorScheme = darkColorScheme(
    primary = SafetyAmberBright,
    onPrimary = SlateInk,
    primaryContainer = OnSafetyAmberContainer,
    onPrimaryContainer = SafetyAmberContainer,
    secondary = Color(0xFF93C5FD),
    onSecondary = SlateInk,
    secondaryContainer = Color(0xFF1E3A5F),
    onSecondaryContainer = Color(0xFFDBEAFE),
    tertiary = Color(0xFF34D399),
    onTertiary = SlateInk,
    tertiaryContainer = Color(0xFF064E3B),
    onTertiaryContainer = Color(0xFFD1FAE5),
    background = SlateInk,
    onBackground = Color(0xFFF8FAFC),
    surface = SlateNavy,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = SlateSurfaceDark,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF475569),
    error = Color(0xFFF87171),
    onError = SlateInk,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2)
)

private val QuoteVoiceShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun QuoteVoiceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = QuoteVoiceTypography,
        shapes = QuoteVoiceShapes,
        content = content
    )
}
