package com.pavlovsfrog.minimaltvlauncher.theme

import androidx.compose.ui.graphics.Color

/** Nocturne palette. Focus/SunDot are sRGB conversions of oklch(0.75 0.12 255) / (0.82 0.15 78). */
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
  val Focus = Color(0xFF79B1F9)

  /** Weather sun dot. */
  val SunDot = Color(0xFFF9B73F)

  /** Bannerless card background (over the scrimmed wallpaper). */
  val CardFill = Color.White.copy(alpha = 0.035f)

  /** 1dp unfocused card border. */
  val CardBorder = Color.White.copy(alpha = 0.06f)

  /** Full-screen scrim behind the long-press action menu — rgba(5,6,9,0.55). */
  val MenuScrim = Color(0xFF050609).copy(alpha = 0.55f)

  /** Anchored menu-card surface — rgba(13,14,19,0.96). */
  val MenuCardFill = Color(0xFF0D0E13).copy(alpha = 0.96f)

  /** Menu-card hairline border — rgba(255,255,255,0.12). */
  val MenuCardBorder = Color.White.copy(alpha = 0.12f)

  /** Focused menu-card item fill — rgba(255,255,255,0.10). */
  val MenuItemFocusFill = Color.White.copy(alpha = 0.10f)

  /** Menu-card header (uppercase app name) — #8a8e99. */
  val MenuHeader = Color(0xFF8A8E99)

  /** Menu-card item label — #f0f1f4. */
  val MenuLabel = Color(0xFFF0F1F4)

  /** Move-mode hint pill fill — rgba(13,14,19,0.92). */
  val HintPillFill = Color(0xFF0D0E13).copy(alpha = 0.92f)

  /** Action-menu circle fill — rgba(20,21,27,0.92). */
  val MenuCircle = Color(0xFF14151B).copy(alpha = 0.92f)

  /** Focused action-menu circle fill. */
  val MenuCircleFocused = Color(0xFFF4F4F5)

  /** Glyph on a focused (light) action-menu circle. */
  val MenuGlyphFocused = Color(0xFF101116)

  /** Destructive glyph (Uninstall ✕). */
  val DangerText = Color(0xFFFF9D97)

  /** Header gear circle fill — rgba(14,15,20,0.55). */
  val GearFill = Color(0xFF0E0F14).copy(alpha = 0.55f)

  /** Header gear circle border. */
  val GearBorder = Color.White.copy(alpha = 0.14f)

  /** Full-screen settings takeover surface — rgba(8,9,12,0.97). */
  val SettingsSurface = Color(0xFF08090C).copy(alpha = 0.97f)

  /** Unstarred (hidden) star glyph ☆. */
  val StarOff = Color(0xFF5A5E68)

  /** Focused settings-row background. */
  val RowFocusFill = Color.White.copy(alpha = 0.09f)

  /** App name on a hidden settings row. */
  val TextDimmed = Color(0xFF8A8E99)
}
