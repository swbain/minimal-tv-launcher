@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.pavlovsfrog.minimaltvlauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.pavlovsfrog.minimaltvlauncher.AppInfo
import com.pavlovsfrog.minimaltvlauncher.AppTileArt
import com.pavlovsfrog.minimaltvlauncher.LauncherAction
import com.pavlovsfrog.minimaltvlauncher.theme.NocturneColors
import kotlin.math.roundToInt

// Design §2, px ÷ 2 = dp/sp (1080p TV = xhdpi 2.0).
private val CircleSize = 33.dp
private val ItemWidth = 48.dp
private val ItemGap = 10.dp
private val RowOffsetFromTile = 9.dp
private val EdgeMargin = 12.dp
private val ConfirmKeys = setOf(Key.Enter, Key.NumPadEnter, Key.DirectionCenter)

/**
 * The long-press overlay: full-screen scrim, a lifted ghost of the pressed tile, and the
 * Hide · Uninstall · Cancel action row anchored below the tile (above when it would clip
 * the bottom edge). Focus opens on Hide; Back, Cancel, and the scrim all dismiss.
 */
@Composable
fun AppActionMenu(
  app: AppInfo,
  anchorBounds: Rect,
  onAction: (LauncherAction) -> Unit,
) {
  var entered by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) { entered = true }
  val scrimAlpha by animateFloatAsState(if (entered) 1f else 0f, tween(180), label = "scrim")
  val rowAlpha by animateFloatAsState(if (entered) 1f else 0f, tween(160), label = "rowAlpha")
  val rowRise by animateFloatAsState(if (entered) 0f else 1f, tween(160), label = "rowRise")

  // The OK press that opened the menu may still be held: its auto-repeat downs and its
  // release must not "click" the freshly-focused Hide button. A fresh press is recognized
  // by repeatCount == 0; only its key-up may click.
  var sawFreshDown by remember { mutableStateOf(false) }

  BackHandler { onAction(LauncherAction.MenuDismissed) }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .onPreviewKeyEvent { event ->
        if (event.key !in ConfirmKeys) return@onPreviewKeyEvent false
        when {
          event.type == KeyEventType.KeyDown && event.nativeKeyEvent.repeatCount > 0 -> true
          event.type == KeyEventType.KeyDown -> {
            sawFreshDown = true
            false
          }
          event.type == KeyEventType.KeyUp && !sawFreshDown -> true
          else -> false
        }
      }
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .graphicsLayer { alpha = scrimAlpha }
        .background(NocturneColors.MenuScrim)
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
        ) { onAction(LauncherAction.MenuDismissed) }
    )

    GhostTile(app = app, bounds = anchorBounds)

    // Custom layout: the anchor is in root pixels, so place the row in pixels too.
    Layout(
      content = { ActionRow(app = app, onAction = onAction) },
      modifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
          alpha = rowAlpha
          translationY = rowRise * 5.dp.toPx()
        },
    ) { measurables, constraints ->
      val row = measurables.first().measure(Constraints())
      layout(constraints.maxWidth, constraints.maxHeight) {
        val margin = EdgeMargin.roundToPx()
        val gap = RowOffsetFromTile.roundToPx()
        val x = (anchorBounds.center.x - row.width / 2f).roundToInt()
          .coerceIn(margin, (constraints.maxWidth - margin - row.width).coerceAtLeast(margin))
        var y = (anchorBounds.bottom + gap).roundToInt()
        if (y + row.height > constraints.maxHeight - margin) {
          // Would clip the bottom edge — flip above the tile.
          y = (anchorBounds.top - gap - row.height).roundToInt()
        }
        row.place(x, y)
      }
    }
  }
}

/** A duplicate of the pressed tile, drawn over the scrim with the amber ring + lifted glow. */
@Composable
private fun GhostTile(app: AppInfo, bounds: Rect) {
  val shape = RoundedCornerShape(8.dp)
  val density = LocalDensity.current
  val tileWidth = with(density) { bounds.width.toDp() }
  val tileHeight = with(density) { bounds.height.toDp() }
  Box(
    modifier = Modifier
      .offset { IntOffset(bounds.left.roundToInt(), bounds.top.roundToInt()) }
      .size(width = tileWidth, height = tileHeight)
      .shadow(24.dp, shape, ambientColor = NocturneColors.Amber, spotColor = NocturneColors.Amber)
      .clip(shape)
      .background(NocturneColors.BaseBlack)
      .background(NocturneColors.CardFill)
      .border(3.dp, NocturneColors.Amber, shape),
  ) {
    AppTileArt(app)
  }
}

@Composable
private fun ActionRow(app: AppInfo, onAction: (LauncherAction) -> Unit) {
  val hideFocus = remember { FocusRequester() }
  LaunchedEffect(Unit) { hideFocus.requestFocus() }

  Row(horizontalArrangement = Arrangement.spacedBy(ItemGap)) {
    MenuItem(
      glyph = "⊘",
      label = "Hide",
      onClick = { onAction(LauncherAction.HideApp(app)) },
      focusRequester = hideFocus,
    )
    MenuItem(
      glyph = "✕",
      label = "Uninstall",
      glyphColor = NocturneColors.DangerText,
      onClick = { onAction(LauncherAction.UninstallApp(app)) },
    )
    MenuItem(
      glyph = "‹",
      label = "Cancel",
      onClick = { onAction(LauncherAction.MenuDismissed) },
    )
  }
}

@Composable
private fun MenuItem(
  glyph: String,
  label: String,
  onClick: () -> Unit,
  glyphColor: Color = NocturneColors.TextWeather,
  focusRequester: FocusRequester? = null,
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.width(ItemWidth),
  ) {
    Surface(
      onClick = onClick,
      shape = ClickableSurfaceDefaults.shape(CircleShape),
      colors = ClickableSurfaceDefaults.colors(
        containerColor = NocturneColors.MenuCircle,
        contentColor = glyphColor,
        focusedContainerColor = NocturneColors.MenuCircleFocused,
        focusedContentColor = NocturneColors.MenuGlyphFocused,
        pressedContainerColor = NocturneColors.MenuCircleFocused,
        pressedContentColor = NocturneColors.MenuGlyphFocused,
      ),
      border = ClickableSurfaceDefaults.border(
        focusedBorder = Border(BorderStroke(2.dp, NocturneColors.Amber)),
      ),
      scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
      modifier = Modifier
        .size(CircleSize)
        .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
    ) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = glyph, fontSize = 13.sp)
      }
    }
    Text(
      text = label,
      fontSize = 9.sp,
      color = NocturneColors.TextSecondary,
      modifier = Modifier.padding(top = 4.dp),
    )
  }
}
