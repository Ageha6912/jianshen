package com.jianshen.fitness.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jianshen.fitness.R

/**
 * v3 白金风(向 LibreFit 看齐):暖黑/暖白双底 + 唯一香槟铂金强调色 + 大圆角 +
 * Roboto Flex 宽体数字(拉丁/数字走 Flex,中文回退系统字体)。
 * 完成态、选中态全部用白金;红色仅用于错误/删除。
 */

// Roboto Flex 可变字体:钉死 opsz/GRAD 等轴,保留 wght+wdth;wdth=125 宽体是观感关键。
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun flexFont(weight: FontWeight, axisWeight: Int) = Font(
    resId = R.font.roboto_flex,
    weight = weight,
    variationSettings = FontVariation.Settings(
        FontVariation.width(125f),
        FontVariation.weight(axisWeight),
    ),
)

val PlatinumFontFamily = FontFamily(
    flexFont(FontWeight.Normal, 400),
    flexFont(FontWeight.Medium, 500),
    flexFont(FontWeight.SemiBold, 600),
    flexFont(FontWeight.Bold, 700),
)

private fun platinumTypography() = Typography(
    displayLarge = PlatinumTypographyTokens.displayLarge,
    displayMedium = PlatinumTypographyTokens.displayMedium,
    displaySmall = PlatinumTypographyTokens.displaySmall,
    headlineLarge = PlatinumTypographyTokens.headlineLarge,
    headlineMedium = PlatinumTypographyTokens.headlineMedium,
    headlineSmall = PlatinumTypographyTokens.headlineSmall,
    titleLarge = PlatinumTypographyTokens.titleLarge,
    titleMedium = PlatinumTypographyTokens.titleMedium,
    titleSmall = PlatinumTypographyTokens.titleSmall,
    bodyLarge = PlatinumTypographyTokens.bodyLarge,
    bodyMedium = PlatinumTypographyTokens.bodyMedium,
    bodySmall = PlatinumTypographyTokens.bodySmall,
    labelLarge = PlatinumTypographyTokens.labelLarge,
    labelMedium = PlatinumTypographyTokens.labelMedium,
    labelSmall = PlatinumTypographyTokens.labelSmall,
)

private object PlatinumTypographyTokens {
    val displayLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = PlatinumFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 57.sp, letterSpacing = 0.sp,
    )
    val displayMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = PlatinumFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 45.sp, letterSpacing = 0.sp,
    )
    val displaySmall = androidx.compose.ui.text.TextStyle(
        fontFamily = PlatinumFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 36.sp, letterSpacing = 0.sp,
    )
    val headlineLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = PlatinumFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, letterSpacing = 0.sp,
    )
    val headlineMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = PlatinumFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, letterSpacing = 0.sp,
    )
    val headlineSmall = androidx.compose.ui.text.TextStyle(
        fontFamily = PlatinumFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, letterSpacing = 0.sp,
    )
    val titleLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = PlatinumFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, letterSpacing = 0.sp,
    )
    val titleMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = PlatinumFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, letterSpacing = 0.1.sp,
    )
    val titleSmall = androidx.compose.ui.text.TextStyle(
        fontFamily = PlatinumFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.1.sp,
    )
    val bodyLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = PlatinumFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, letterSpacing = 0.3.sp,
    )
    val bodyMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = PlatinumFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, letterSpacing = 0.2.sp,
    )
    val bodySmall = androidx.compose.ui.text.TextStyle(
        fontFamily = PlatinumFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, letterSpacing = 0.3.sp,
    )
    val labelLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = PlatinumFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.1.sp,
    )
    val labelMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = PlatinumFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.4.sp,
    )
    val labelSmall = androidx.compose.ui.text.TextStyle(
        fontFamily = PlatinumFontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.4.sp,
    )
}

private val PlatinumDark = darkColorScheme(
    primary = Color(0xFFE8E1D5),
    onPrimary = Color(0xFF1F1B16),
    primaryContainer = Color(0xFF3B362E),
    onPrimaryContainer = Color(0xFFE8E1D5),
    secondary = Color(0xFFCDC5B8),
    onSecondary = Color(0xFF332F28),
    secondaryContainer = Color(0xFF4A4438),
    onSecondaryContainer = Color(0xFFE8E1D3),
    tertiary = Color(0xFFD8C8B4),
    onTertiary = Color(0xFF3B3222),
    tertiaryContainer = Color(0xFF4E4433),
    onTertiaryContainer = Color(0xFFEFE2CE),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF141210),
    onBackground = Color(0xFFEAE6DF),
    surface = Color(0xFF141210),
    onSurface = Color(0xFFEAE6DF),
    surfaceVariant = Color(0xFF262320),
    onSurfaceVariant = Color(0xFFA9A29A),
    outline = Color(0xFF555049),
    outlineVariant = Color(0xFF3A362F),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFEAE6DF),
    inverseOnSurface = Color(0xFF36312B),
    inversePrimary = Color(0xFF7E735F),
    surfaceDim = Color(0xFF141210),
    surfaceBright = Color(0xFF3A3630),
    surfaceContainerLowest = Color(0xFF0E0C0A),
    surfaceContainerLow = Color(0xFF1B1916),
    surfaceContainer = Color(0xFF201D1A),
    surfaceContainerHigh = Color(0xFF262320),
    surfaceContainerHighest = Color(0xFF2C2926),
)

private val PlatinumLight = lightColorScheme(
    primary = Color(0xFF2A2724),
    onPrimary = Color(0xFFFAF9F7),
    primaryContainer = Color(0xFFEFE9DE),
    onPrimaryContainer = Color(0xFF1F1B16),
    secondary = Color(0xFF6B6459),
    onSecondary = Color(0xFFFAF9F7),
    secondaryContainer = Color(0xFFE8E1D3),
    onSecondaryContainer = Color(0xFF26221B),
    tertiary = Color(0xFF7A6A50),
    onTertiary = Color(0xFFFAF9F7),
    tertiaryContainer = Color(0xFFEDE2CE),
    onTertiaryContainer = Color(0xFF26200F),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFAF9F7),
    onBackground = Color(0xFF262320),
    surface = Color(0xFFFAF9F7),
    onSurface = Color(0xFF262320),
    surfaceVariant = Color(0xFFEFE9DE),
    onSurfaceVariant = Color(0xFF6D675E),
    outline = Color(0xFFC9C4BC),
    outlineVariant = Color(0xFFE2DDD4),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF302C26),
    inverseOnSurface = Color(0xFFF8F1E8),
    inversePrimary = Color(0xFFE8E1D5),
    surfaceDim = Color(0xFFDBD8D3),
    surfaceBright = Color(0xFFFAF9F7),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF4F2EF),
    surfaceContainer = Color(0xFFEEECE8),
    surfaceContainerHigh = Color(0xFFE8E5E1),
    surfaceContainerHighest = Color(0xFFE2DFDA),
)

// LibreFit 式大圆角:卡片 20 / 弹层与对话框 28 / 控件 12-16。
private val PlatinumShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** 胶囊(pill)形状:全宽按钮、页签、分类 chip 用。 */
val PillShape = RoundedCornerShape(50)

@Composable
fun FitnessTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) PlatinumDark else PlatinumLight,
        shapes = PlatinumShapes,
        typography = platinumTypography(),
        content = content,
    )
}
