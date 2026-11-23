package com.narezany.appletree.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Спокойная палитра: приглушённая зелень, тёплый крем, мягкое небо.
val Ink = Color(0xFF33473B)
val InkSoft = Color(0xFF6B7F72)
val Muted = Color(0xFF97A79A)
val Cream = Color(0xFFFFFDF7)
val CreamDeep = Color(0xFFF6F1E5)
val CardLine = Color(0xFFE3E9DC)
val AppleRed = Color(0xFFD2665A)
val Gold = Color(0xFFE0AE55)
val LeafGreen = Color(0xFF7FAE6B)
val LeafDark = Color(0xFF5F8B51)
val SkyBlue = Color(0xFFA9D8F2)
val Water = Color(0xFFBFE3F2)
val Sand = Color(0xFFE7D9B8)

private val scheme = lightColorScheme(
    primary = LeafGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEBD2),
    onPrimaryContainer = Ink,
    secondary = AppleRed,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF6DFDA),
    onSecondaryContainer = Ink,
    tertiary = Gold,
    onTertiary = Color(0xFF4A3620),
    background = Color(0xFFF7FAF2),
    onBackground = Ink,
    surface = Cream,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEFF4E9),
    onSurfaceVariant = InkSoft,
    outline = CardLine,
    outlineVariant = Color(0xFFEDF1E7),
    error = Color(0xFFB4554C)
)

private val typography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 36.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontSize = 11.sp, letterSpacing = 0.3.sp)
)

@Composable
fun AppleTreeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}
