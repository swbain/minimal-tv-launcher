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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
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
import com.pavlovsfrog.minimaltvlauncher.theme.Newsreader

private val TitleStyle = TextStyle(
  fontFamily = Newsreader,
  fontWeight = FontWeight.SemiBold,
  fontSize = 17.sp,
  color = NocturneColors.TextPrimary,
)

private val SubtitleStyle = TextStyle(
  fontFamily = Newsreader,
  fontStyle = FontStyle.Italic,
  fontSize = 11.sp,
  color = NocturneColors.TextMuted,
)

private val RowLabelStyle = TextStyle(
  fontFamily = Newsreader,
  fontSize = 15.sp,
)

/**
 * Design §4: full-screen takeover listing every installed app in two columns; the star
 * toggles home visibility in place (no confirm), the back button / Back key close.
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
      BackButton(onClick = { onAction(LauncherAction.CloseSettings) })
      Text(text = "Apps", style = TitleStyle)
      Text(
        text = "$favoriteCount of ${allApps.size} on home · press to star or unstar",
        style = SubtitleStyle,
      )
    }

    LazyVerticalGrid(
      columns = GridCells.Fixed(2),
      horizontalArrangement = Arrangement.spacedBy(32.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
      contentPadding = PaddingValues(vertical = 4.dp),
      modifier = Modifier.fillMaxSize().padding(top = 14.dp),
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

/** Icon-only circular back button, styled after the home screen's gear button. */
@Composable
private fun BackButton(onClick: () -> Unit) {
  Surface(
    onClick = onClick,
    shape = ClickableSurfaceDefaults.shape(CircleShape),
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
    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
    modifier = Modifier
      .size(32.dp)
      .semantics { contentDescription = "Back" },
  ) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text(text = "←", fontSize = 15.sp)
    }
  }
}

@Composable
private fun AppRow(entry: AppEntry, onToggle: () -> Unit, modifier: Modifier = Modifier) {
  val app = entry.app
  var focused by remember { mutableStateOf(false) }
  Surface(
    onClick = onToggle,
    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
    colors = ClickableSurfaceDefaults.colors(
      containerColor = Color.Transparent,
      contentColor = NocturneColors.TextSecondary,
      focusedContainerColor = NocturneColors.RowFocusFill,
      focusedContentColor = NocturneColors.TextPrimary,
      pressedContainerColor = NocturneColors.RowFocusFill,
      pressedContentColor = NocturneColors.TextPrimary,
    ),
    scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    modifier = modifier
      .fillMaxWidth()
      .onFocusChanged { focused = it.isFocused },
  ) {
    Box {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
      ) {
        AppChip(entry = entry)
        Text(
          text = app.label,
          style = RowLabelStyle,
          color = if (entry.isFavorite) NocturneColors.TextSecondary else NocturneColors.TextDimmed,
          modifier = Modifier.weight(1f),
        )
        Text(
          text = if (entry.isFavorite) "★" else "☆",
          fontSize = 14.sp,
          color = if (entry.isFavorite) NocturneColors.Amber else NocturneColors.StarOff,
        )
      }
      // Focus indicator: amber accent bar hugging the row's leading edge.
      Box(
        modifier = Modifier
          .align(Alignment.CenterStart)
          .padding(start = 3.dp)
          .width(3.dp)
          .height(18.dp)
          .clip(RoundedCornerShape(1.5.dp))
          .background(if (focused) NocturneColors.Amber else Color.Transparent),
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
