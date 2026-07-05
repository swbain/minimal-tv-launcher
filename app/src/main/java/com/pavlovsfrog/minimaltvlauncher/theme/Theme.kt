package com.pavlovsfrog.minimaltvlauncher.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

/**
 * Nocturne theme: always dark (TV UIs sit in dim living rooms; we don't follow the system
 * light/dark setting), Newsreader serif typography, amber accent.
 */
@Composable
fun MinimalTvLauncherTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme =
      darkColorScheme(
        background = NocturneColors.BaseBlack,
        surface = NocturneColors.BaseBlack,
        surfaceVariant = NocturneColors.CardFill,
        onSurface = NocturneColors.TextPrimary,
        onSurfaceVariant = NocturneColors.TextSecondary,
        border = NocturneColors.CardBorder,
        borderVariant = NocturneColors.CardBorder,
        primary = NocturneColors.Amber,
      ),
    typography = NocturneTypography,
    content = content,
  )
}
