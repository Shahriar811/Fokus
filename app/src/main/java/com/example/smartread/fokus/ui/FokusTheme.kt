package com.example.smartread.fokus.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartread.R

@Immutable
internal data class FokusTokens(
    val focusAccent: Color,
    val focusAccentSoft: Color,
    val focusGlow: Color,
    val onFocusAccent: Color,
    val breakAccent: Color,
    val breakAccentSoft: Color,
    val breakGlow: Color,
    val onBreakAccent: Color,
    val heroSurface: Color,
    val heroBorder: Color,
    val bentoSurface: Color,
    val dialTrack: Color,
    val dialTickActive: Color,
    val dialTickMuted: Color,
    val navSurface: Color,
    val streakTint: Color,
    val streakContainer: Color
)

private val lightTokens = FokusTokens(
    focusAccent = Color(0xFF165B40),
    focusAccentSoft = Color(0xFFD6EDE0),
    focusGlow = Color(0xFF2E8561),
    onFocusAccent = Color.White,
    breakAccent = Color(0xFFB35824),
    breakAccentSoft = Color(0xFFF7E2D0),
    breakGlow = Color(0xFFD97D42),
    onBreakAccent = Color.White,
    heroSurface = Color(0xFFFFFDF8),
    heroBorder = Color(0xFFE3DDD0),
    bentoSurface = Color(0xFFFFFDF8),
    dialTrack = Color(0xFFE8E4D8),
    dialTickActive = Color(0xFF165B40),
    dialTickMuted = Color(0xFFC9C3B4),
    navSurface = Color(0xFFFFFDF9),
    streakTint = Color(0xFFA64F1E),
    streakContainer = Color(0xFFF9E6D6)
)

private val darkTokens = FokusTokens(
    focusAccent = Color(0xFF7ED6A3),
    focusAccentSoft = Color(0xFF1B3A2B),
    focusGlow = Color(0xFF56BA82),
    onFocusAccent = Color(0xFF062918),
    breakAccent = Color(0xFFEBB470),
    breakAccentSoft = Color(0xFF3E2A16),
    breakGlow = Color(0xFFD8974B),
    onBreakAccent = Color(0xFF351D04),
    heroSurface = Color(0xFF141D18),
    heroBorder = Color(0xFF25342C),
    bentoSurface = Color(0xFF141D18),
    dialTrack = Color(0xFF233029),
    dialTickActive = Color(0xFF7ED6A3),
    dialTickMuted = Color(0xFF38473E),
    navSurface = Color(0xFF111814),
    streakTint = Color(0xFFEBB470),
    streakContainer = Color(0xFF382716)
)

private val lightColors = lightColorScheme(
    primary = Color(0xFF165B40),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4EBDD),
    onPrimaryContainer = Color(0xFF0C3825),
    secondary = Color(0xFF2B5C47),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCECE0),
    onSecondaryContainer = Color(0xFF123324),
    tertiary = Color(0xFFA64F1E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF7E3D2),
    onTertiaryContainer = Color(0xFF4A2208),
    background = Color(0xFFF5F3EC),
    onBackground = Color(0xFF161D19),
    surface = Color(0xFFFFFDF8),
    onSurface = Color(0xFF161D19),
    onSurfaceVariant = Color(0xFF545E58),
    surfaceVariant = Color(0xFFEAE6DC),
    surfaceDim = Color(0xFFDDD8CC),
    surfaceBright = Color(0xFFFFFDF9),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF1EFE7),
    surfaceContainer = Color(0xFFEBE7DD),
    surfaceContainerHigh = Color(0xFFE4DFD3),
    surfaceContainerHighest = Color(0xFFDCD6C9),
    inverseSurface = Color(0xFF1B2721),
    inverseOnSurface = Color(0xFFF2EFE7),
    inversePrimary = Color(0xFF7ED6A3),
    error = Color(0xFFB33A2E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DED9),
    onErrorContainer = Color(0xFF5C1711),
    outline = Color(0xFF7A827B),
    outlineVariant = Color(0xFFDFDAD0)
)

private val darkColors = darkColorScheme(
    primary = Color(0xFF7FD6A4),
    onPrimary = Color(0xFF08331E),
    primaryContainer = Color(0xFF1D4432),
    onPrimaryContainer = Color(0xFFC9F2D9),
    secondary = Color(0xFF9CCFB0),
    onSecondary = Color(0xFF0F3322),
    secondaryContainer = Color(0xFF20392B),
    onSecondaryContainer = Color(0xFFD0ECDA),
    tertiary = Color(0xFFE9B373),
    onTertiary = Color(0xFF3D2204),
    tertiaryContainer = Color(0xFF452C14),
    onTertiaryContainer = Color(0xFFFCE0BD),
    background = Color(0xFF0D1310),
    onBackground = Color(0xFFEDEBE4),
    surface = Color(0xFF141C18),
    onSurface = Color(0xFFEDEBE4),
    onSurfaceVariant = Color(0xFF9EAAA2),
    surfaceVariant = Color(0xFF222E28),
    surfaceDim = Color(0xFF0D1310),
    surfaceBright = Color(0xFF2D3B33),
    surfaceContainerLowest = Color(0xFF090E0B),
    surfaceContainerLow = Color(0xFF121915),
    surfaceContainer = Color(0xFF19231E),
    surfaceContainerHigh = Color(0xFF212D27),
    surfaceContainerHighest = Color(0xFF293730),
    inverseSurface = Color(0xFFE7E4DC),
    inverseOnSurface = Color(0xFF16211B),
    inversePrimary = Color(0xFF165B40),
    error = Color(0xFFF29A8E),
    onError = Color(0xFF4A120D),
    errorContainer = Color(0xFF541F1A),
    onErrorContainer = Color(0xFFFFDAD4),
    outline = Color(0xFF6E7B73),
    outlineVariant = Color(0xFF27342D)
)

internal val panelShape = RoundedCornerShape(22.dp)
internal val featureShape = RoundedCornerShape(28.dp)

@OptIn(ExperimentalTextApi::class)
private fun variableFamily(resource: Int, vararg weights: FontWeight) = FontFamily(
    weights.map { weight ->
        Font(resource, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))
    }
)

private val interfaceFont = variableFamily(
    R.font.inter_variable, FontWeight.Light, FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold
)
private val headingFont = variableFamily(
    R.font.manrope_variable, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold, FontWeight.ExtraBold
)

internal val numberStyle = TextStyle(
    fontFamily = interfaceFont,
    fontWeight = FontWeight.Light,
    fontSize = 60.sp,
    letterSpacing = (-2.6).sp,
    fontFeatureSettings = "tnum",
    lineHeight = 66.sp
)

internal val tabularLabelStyle = TextStyle(
    fontFamily = interfaceFont,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    fontFeatureSettings = "tnum",
    lineHeight = 16.sp
)

private fun heading(size: Int, line: Int, spacing: Double, weight: FontWeight) = TextStyle(
    fontFamily = headingFont, fontWeight = weight, fontSize = size.sp,
    letterSpacing = spacing.sp, lineHeight = line.sp
)

private fun text(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = interfaceFont, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp
)

private val appTypography = Typography(
    displayLarge = heading(54, 58, -2.2, FontWeight.ExtraBold),
    displayMedium = heading(44, 48, -1.8, FontWeight.ExtraBold),
    displaySmall = heading(38, 43, -1.4, FontWeight.ExtraBold),
    headlineLarge = heading(34, 40, -1.2, FontWeight.ExtraBold),
    headlineMedium = heading(28, 34, -0.8, FontWeight.Bold),
    headlineSmall = heading(23, 29, -0.4, FontWeight.Bold),
    titleLarge = heading(19, 25, -0.25, FontWeight.Bold),
    titleMedium = text(16, 22, FontWeight.SemiBold),
    titleSmall = text(14, 20, FontWeight.SemiBold),
    bodyLarge = text(16, 24),
    bodyMedium = text(14, 21),
    bodySmall = text(12, 17),
    labelLarge = text(14, 20, FontWeight.SemiBold),
    labelMedium = text(12, 17, FontWeight.Medium),
    labelSmall = text(11, 15, FontWeight.Medium)
)

internal val LocalDarkTheme = staticCompositionLocalOf { false }
internal val LocalFokusTokens = staticCompositionLocalOf { lightTokens }

@Composable
fun FokusTheme(dark: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalDarkTheme provides dark,
        LocalFokusTokens provides if (dark) darkTokens else lightTokens
    ) {
        MaterialTheme(colorScheme = if (dark) darkColors else lightColors, typography = appTypography, content = content)
    }
}
