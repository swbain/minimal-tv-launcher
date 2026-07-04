package com.example.minimaltvlauncher.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

/**
 * Minimal dark theme for the TV launcher. TV UIs are almost always dark so the content sits
 * comfortably in a dim living room, so we don't follow the system light/dark setting here.
 */
@Composable
fun MinimalTvLauncherTheme(content: @Composable () -> Unit) {
  MaterialTheme(colorScheme = darkColorScheme(), content = content)
}
