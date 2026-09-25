package com.movealarm.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.movealarm.app.R

/** UI 시안(무브알람 UI 시안.html)의 색상 */
object MoveColors {
    val Background = Color(0xFFFFF6DE)
    val Hero = Color(0xFFFFC947)
    val HeroBorder = Color(0xFFF4B627)
    val HeroSub = Color(0xFF5C4A12)
    val Card = Color(0xFFFFFFFF)
    val CardBorder = Color(0xFFF0DFAE)
    val Ink = Color(0xFF3B2A12)
    val Sub = Color(0xFF6E5A34)
    val Accent = Color(0xFFC2660A)
    val PillBg = Color(0xFFFFF1CE)
    val TrackBg = Color(0xFFF3E6BE)
    val Off = Color(0xFFEADFC0)
    val Ok = Color(0xFF6E9B6E)
    val OffDot = Color(0xFFB9A77A)
    val ClockCenter = Color(0xFFFFF7E3)
    val ClockEdge = Color(0xFFFFE9AC)
    val RingTop = Color(0xFF2A2013)
    val RingBottom = Color(0xFF241C10)
    val Popup = Color(0xFFFFF9EA)
    val PhotoBg = Color(0xFFFFE6A6)
}

val Jua = FontFamily(Font(R.font.jua_regular))
val GowunDodum = FontFamily(Font(R.font.gowun_dodum_regular))

private fun TextStyle.body() = copy(fontFamily = GowunDodum)

@Composable
fun MoveTheme(content: @Composable () -> Unit) {
    val base = Typography()
    val typography = Typography(
        displayLarge = base.displayLarge.body(),
        displayMedium = base.displayMedium.body(),
        displaySmall = base.displaySmall.body(),
        headlineLarge = base.headlineLarge.body(),
        headlineMedium = base.headlineMedium.body(),
        headlineSmall = base.headlineSmall.body(),
        titleLarge = base.titleLarge.body(),
        titleMedium = base.titleMedium.body(),
        titleSmall = base.titleSmall.body(),
        bodyLarge = base.bodyLarge.body(),
        bodyMedium = base.bodyMedium.body(),
        bodySmall = base.bodySmall.body(),
        labelLarge = base.labelLarge.body(),
        labelMedium = base.labelMedium.body(),
        labelSmall = base.labelSmall.body(),
    )
    val colors = lightColorScheme(
        primary = MoveColors.Accent,
        onPrimary = Color.White,
        background = MoveColors.Background,
        onBackground = MoveColors.Ink,
        surface = MoveColors.Card,
        onSurface = MoveColors.Ink,
        onSurfaceVariant = MoveColors.Sub,
        outline = MoveColors.CardBorder,
    )
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}
