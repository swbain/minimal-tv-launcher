package com.pavlovsfrog.minimaltvlauncher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.pavlovsfrog.minimaltvlauncher.R
import com.pavlovsfrog.minimaltvlauncher.theme.NocturneColors

/**
 * The Nocturne stage: wallpaper over a near-black base, dimmed by a vertical scrim (darkest at
 * the very top and bottom so the header and labels stay legible) and a radial vignette.
 * Gradient stops mirror the design source (TV Launcher.dc.html) exactly.
 */
@Composable
fun NocturneBackground(
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  Box(modifier = modifier.fillMaxSize().background(NocturneColors.BaseBlack)) {
    Image(
      painter = painterResource(R.drawable.wallpaper),
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize(),
    )
    Box(
      modifier = Modifier.fillMaxSize().background(
        Brush.verticalGradient(
          0.00f to NocturneColors.ScrimTint.copy(alpha = 0.62f),
          0.34f to NocturneColors.ScrimTint.copy(alpha = 0.34f),
          0.70f to NocturneColors.ScrimTint.copy(alpha = 0.50f),
          1.00f to NocturneColors.ScrimTint.copy(alpha = 0.82f),
        )
      )
    )
    // Design: radial-gradient(120% 90% at 50% 50%, transparent 55%, rgba(7,8,11,0.55) 100%).
    // Compose radial brushes are circular, so draw a circle of radius ry scaled out to rx.
    Box(
      modifier = Modifier.fillMaxSize().drawWithCache {
        val radiusY = size.height * 0.9f
        val brush = Brush.radialGradient(
          0.55f to Color.Transparent,
          1.00f to NocturneColors.ScrimTint.copy(alpha = 0.55f),
          center = size.center,
          radius = radiusY,
        )
        val scaleX = (size.width * 1.2f) / radiusY
        onDrawBehind {
          if (size.minDimension > 0f) {
            scale(scaleX = scaleX, scaleY = 1f, pivot = center) { drawRect(brush) }
          }
        }
      }
    )
    content()
  }
}
