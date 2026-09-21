package dev.psychocat.catclicker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Palette taken from the web version's style.css so both versions feel the same.
val Cream = Color(0xFFF7EFE3)
val CreamCard = Color(0xFFFFFAF2)
val CardBorder = Color(0xFFECDDCA)
val ArtBackground = Color(0xFFF7ECDC)
val Brown = Color(0xFF422B21)
val BrownSoft = Color(0xFF806150)
val Terracotta = Color(0xFFB85E35)
val Sand = Color(0xFFF3E6D5)
val FishCard = Color(0xFFFFF2D9)
val FishBorder = Color(0xFFE9C78B)
val HungerFill = Color(0xFFE29B4E)
val HungerTrack = Color(0xFFEBD9C4)
val Danger = Color(0xFF9C3B2A)
val Success = Color(0xFF3F7A45)

private val colors = lightColorScheme(
    primary = Terracotta,
    onPrimary = Color.White,
    background = Cream,
    onBackground = Brown,
    surface = CreamCard,
    onSurface = Brown,
    surfaceVariant = Sand,
    onSurfaceVariant = BrownSoft,
    secondaryContainer = Sand,
    onSecondaryContainer = Brown,
    outline = CardBorder,
    error = Danger,
)

private val Serif = FontFamily.Serif
private val Sans = FontFamily.SansSerif

// Deliberately large: the game is meant for an elderly player. `sp` also follows the system font-size setting.
private val typography = Typography(
    displayLarge = TextStyle(fontFamily = Serif, fontSize = 40.sp, lineHeight = 48.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontFamily = Serif, fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontFamily = Serif, fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontFamily = Sans, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontFamily = Sans, fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontFamily = Sans, fontSize = 20.sp, lineHeight = 28.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontSize = 18.sp, lineHeight = 25.sp),
    bodySmall = TextStyle(fontFamily = Sans, fontSize = 16.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold),
    labelMedium = TextStyle(fontFamily = Sans, fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
)

@Composable
fun CatClickerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}
