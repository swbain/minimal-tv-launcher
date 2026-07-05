package com.pavlovsfrog.minimaltvlauncher.theme

import androidx.compose.ui.graphics.Color

/** Nocturne palette. Amber/SunDot are sRGB conversions of oklch(0.78 0.15 75) / (0.82 0.15 78). */
object NocturneColors {
  /** Screen base under the wallpaper. */
  val BaseBlack = Color(0xFF08090C)

  /** Tint for the vertical scrim and radial vignette — rgb(7, 8, 11). */
  val ScrimTint = Color(0xFF07080B)

  /** Clock time. */
  val TextPrimary = Color(0xFFF6F7F9)

  /** AM/PM, card labels. */
  val TextSecondary = Color(0xFFCDD2DA)

  /** "72° Sunny" weather text. */
  val TextWeather = Color(0xFFE6E9EE)

  /** Date (italic). */
  val TextDate = Color(0xFFD2D6DD)

  /** 3dp separator dot between weather and date. */
  val DotSeparator = Color(0xFFB6BCC6)

  /** Loading / empty-state messages. */
  val TextMuted = Color(0xFF7C7F88)

  /** Focus ring + glow. */
  val Amber = Color(0xFFEFA831)

  /** Weather sun dot. */
  val SunDot = Color(0xFFF9B73F)

  /** Bannerless card background (over the scrimmed wallpaper). */
  val CardFill = Color.White.copy(alpha = 0.035f)

  /** 1dp unfocused card border. */
  val CardBorder = Color.White.copy(alpha = 0.06f)
}
