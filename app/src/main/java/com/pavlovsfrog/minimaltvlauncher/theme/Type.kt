package com.pavlovsfrog.minimaltvlauncher.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.tv.material3.Typography
import com.pavlovsfrog.minimaltvlauncher.R

/**
 * Newsreader (SIL Open Font License) — the Nocturne serif identity. Static instances are
 * bundled because minSdk 24 predates variable-font support (API 26).
 */
val Newsreader = FontFamily(
  Font(R.font.newsreader_regular, FontWeight.Normal),
  Font(R.font.newsreader_medium, FontWeight.Medium),
  Font(R.font.newsreader_semibold, FontWeight.SemiBold),
  Font(R.font.newsreader_italic, FontWeight.Normal, FontStyle.Italic),
  Font(R.font.newsreader_medium_italic, FontWeight.Medium, FontStyle.Italic),
)

/**
 * TV type scale carried entirely by Newsreader. Component-specific sizes from the design
 * (60sp clock, 13sp weather row, 14sp card labels) are set at the point of use; this scale
 * provides the serif default everywhere else.
 */
val NocturneTypography: Typography = Typography().run {
  Typography(
    displayLarge = displayLarge.copy(fontFamily = Newsreader),
    displayMedium = displayMedium.copy(fontFamily = Newsreader),
    displaySmall = displaySmall.copy(fontFamily = Newsreader),
    headlineLarge = headlineLarge.copy(fontFamily = Newsreader),
    headlineMedium = headlineMedium.copy(fontFamily = Newsreader),
    headlineSmall = headlineSmall.copy(fontFamily = Newsreader),
    titleLarge = titleLarge.copy(fontFamily = Newsreader),
    titleMedium = titleMedium.copy(fontFamily = Newsreader),
    titleSmall = titleSmall.copy(fontFamily = Newsreader),
    bodyLarge = bodyLarge.copy(fontFamily = Newsreader),
    bodyMedium = bodyMedium.copy(fontFamily = Newsreader),
    bodySmall = bodySmall.copy(fontFamily = Newsreader),
    labelLarge = labelLarge.copy(fontFamily = Newsreader),
    labelMedium = labelMedium.copy(fontFamily = Newsreader),
    labelSmall = labelSmall.copy(fontFamily = Newsreader),
  )
}
