@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.pavlovsfrog.minimaltvlauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.pavlovsfrog.minimaltvlauncher.AppEntry
import com.pavlovsfrog.minimaltvlauncher.LauncherAction
import com.pavlovsfrog.minimaltvlauncher.theme.NocturneColors

/**
 * Design §4: full-screen takeover listing every installed app in two columns; the star
 * toggles home visibility in place (no confirm), the back pill / Back key close.
 */
@Composable
fun AllAppsSettings(
  allApps: List<AppEntry>,
  onAction: (LauncherAction) -> Unit,
) {
  var entered by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) { entered = true }
  val surfaceAlpha by animateFloatAsState(if (entered) 1f else 0f, tween(200), label = "settings")

  val firstRowFocus = remember { FocusRequester() }
  LaunchedEffect(allApps.isNotEmpty()) {
    if (allApps.isNotEmpty()) runCatching { firstRowFocus.requestFocus() }
  }

  BackHandler { onAction(LauncherAction.CloseSettings) }

  val favoriteCount = allApps.count { it.isFavorite }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .graphicsLayer { alpha = surfaceAlpha }
      .background(NocturneColors.SettingsSurface)
      .padding(horizontal = 55.dp, vertical = 32.dp),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      BackPill(onClick = { onAction(LauncherAction.CloseSettings) })
      Text(
        text = "Apps",
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
        color = NocturneColors.TextPrimary,
      )
      Text(
        text = "$favoriteCount of ${allApps.size} on home · press to star or unstar",
        fontSize = 10.sp,
        fontStyle = FontStyle.Italic,
        color = NocturneColors.TextMuted,
      )
    }

    LazyVerticalGrid(
      columns = GridCells.Fixed(2),
      horizontalArrangement = Arrangement.spacedBy(32.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
      modifier = Modifier.fillMaxSize().padding(top = 18.dp),
    ) {
      itemsIndexed(allApps, key = { _, entry -> entry.app.packageName }) { index, entry ->
        AppRow(
          entry = entry,
          onToggle = { onAction(LauncherAction.ToggleFavorite(entry.app.packageName)) },
          modifier = if (index == 0) Modifier.focusRequester(firstRowFocus) else Modifier,
        )
      }
    }
  }
}

@Composable
private fun BackPill(onClick: () -> Unit) {
  Surface(
    onClick = onClick,
    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(percent = 50)),
    colors = ClickableSurfaceDefaults.colors(
      containerColor = NocturneColors.GearFill,
      contentColor = NocturneColors.TextSecondary,
      focusedContainerColor = NocturneColors.GearFill,
      focusedContentColor = NocturneColors.TextPrimary,
      pressedContainerColor = NocturneColors.GearFill,
      pressedContentColor = NocturneColors.TextPrimary,
    ),
    border = ClickableSurfaceDefaults.border(
      border = Border(BorderStroke(1.dp, NocturneColors.GearBorder)),
      focusedBorder = Border(BorderStroke(2.dp, NocturneColors.Amber)),
    ),
    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
  ) {
    Text(
      text = "‹ Back",
      fontSize = 11.sp,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
    )
  }
}

@Composable
private fun AppRow(entry: AppEntry, onToggle: () -> Unit, modifier: Modifier = Modifier) {
  val app = entry.app
  Surface(
    onClick = onToggle,
    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
    colors = ClickableSurfaceDefaults.colors(
      containerColor = androidx.compose.ui.graphics.Color.Transparent,
      contentColor = NocturneColors.TextSecondary,
      focusedContainerColor = NocturneColors.RowFocusFill,
      focusedContentColor = NocturneColors.TextPrimary,
      pressedContainerColor = NocturneColors.RowFocusFill,
      pressedContentColor = NocturneColors.TextPrimary,
    ),
    border = ClickableSurfaceDefaults.border(
      focusedBorder = Border(BorderStroke(1.dp, NocturneColors.Amber), inset = 1.dp),
    ),
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
      AppChip(entry = entry)
      Text(
        text = app.label,
        fontSize = 13.5.sp,
        color = if (entry.isFavorite) NocturneColors.TextSecondary else NocturneColors.TextDimmed,
        modifier = Modifier.weight(1f),
      )
      Text(
        text = if (entry.isFavorite) "★" else "☆",
        fontSize = 14.sp,
        color = if (entry.isFavorite) NocturneColors.Amber else NocturneColors.StarOff,
      )
    }
  }
}

/** 25dp app artwork chip — banner/icon, or a letter fallback; dimmed to 42% when hidden. */
@Composable
private fun AppChip(entry: AppEntry) {
  val app = entry.app
  Box(
    modifier = Modifier
      .height(25.dp)
      .aspectRatio(16f / 9f)
      .graphicsLayer { alpha = if (entry.isFavorite) 1f else 0.42f }
      .clip(RoundedCornerShape(6.dp))
      .background(NocturneColors.CardFill),
    contentAlignment = Alignment.Center,
  ) {
    val bitmap = app.image
    if (bitmap != null) {
      Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        contentScale = if (app.hasBanner) ContentScale.Crop else ContentScale.Fit,
        modifier = if (app.hasBanner) Modifier.fillMaxSize() else Modifier.size(16.dp),
      )
    } else {
      Text(
        text = app.label.take(1).uppercase(),
        fontSize = 11.sp,
        color = NocturneColors.TextSecondary,
      )
    }
  }
}
