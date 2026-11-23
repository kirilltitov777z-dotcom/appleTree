package com.narezany.appletree.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Спокойная природная палитра — без неона
val SkyTop = Color(0xFF8FCDF0)
val SkyMid = Color(0xFFB7E0F6)
val SkyBottom = Color(0xFFE8F5FC)
val Cloud = Color(0xFFF7FCFF)

val LeafDeep = Color(0xFF4F7F4A)
val Leaf = Color(0xFF6FA55F)
val LeafLight = Color(0xFF9BC98A)
val LeafPale = Color(0xFFC2DFB2)

val TrunkDark = Color(0xFF6B4F3A)
val Trunk = Color(0xFF8A6A4F)
val AppleRed = Color(0xFFC9564F)
val AppleLight = Color(0xFFDD7A6E)
val AppleGold = Color(0xFFD8A657)

val Soil = Color(0xFFB9A281)
val SoilLight = Color(0xFFCDBB9C)
val Grass = Color(0xFF94B97C)

val CardCream = Color(0xFFFBF8F1)
val Ink = Color(0xFF3B3A36)
val InkSoft = Color(0xFF6E6B63)
val Line = Color(0xFFE4DECF)

private val scheme = lightColorScheme(
    primary = LeafDeep,
    onPrimary = Color.White,
    primaryContainer = LeafPale,
    onPrimaryContainer = Color(0xFF2C4427),
    secondary = Color(0xFF5C8DBC),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD8E8F4),
    onSecondaryContainer = Color(0xFF274257),
    tertiary = AppleRed,
    onTertiary = Color.White,
    background = SkyBottom,
    onBackground = Ink,
    surface = CardCream,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF1ECDF),
    onSurfaceVariant = InkSoft,
    outline = Line,
    error = Color(0xFFB5544A),
)

private val type = Typography(
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 24.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp),
)

@Composable
fun AppleTreeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = type, content = content)
}
