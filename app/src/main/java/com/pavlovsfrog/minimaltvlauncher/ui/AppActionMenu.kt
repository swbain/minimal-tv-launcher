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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
private val CardWidth = 190.dp
private val CardRadius = 7.dp
private val CardGap = 8.dp        // anchor.right → card, and the flip offset
private val EdgeMargin = 12.dp
private val ConfirmKeys = setOf(Key.Enter, Key.NumPadEnter, Key.DirectionCenter)

/**
 * The long-press overlay: full-screen scrim, a lifted ghost of the pressed tile, and the anchored
 * menu card (Move · Hide from favorites · Open app settings · Uninstall · Cancel) placed to the
 * right of the tile — flipping to the left when it would overflow, clamped so it never clips an
 * edge. Focus opens on Move; Back, Cancel, and the scrim all dismiss.
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
  val cardAlpha by animateFloatAsState(if (entered) 1f else 0f, tween(160), label = "cardAlpha")
  val cardRise by animateFloatAsState(if (entered) 0f else 1f, tween(160), label = "cardRise")

  // The OK press that opened the menu may still be held: its auto-repeat downs and its release
  // must not "click" the freshly-focused Move item. A fresh press is recognized by repeatCount ==
  // 0; only its key-up may click.
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

    // Custom layout: the anchor is in root pixels, so place the card in pixels too.
    Layout(
      content = { MenuCard(app = app, onAction = onAction) },
      modifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
          alpha = cardAlpha
          translationY = cardRise * 10.dp.toPx()
        },
    ) { measurables, constraints ->
      val card = measurables.first().measure(Constraints())
      layout(constraints.maxWidth, constraints.maxHeight) {
        val margin = EdgeMargin.roundToPx()
        val gap = CardGap.roundToPx()
        val maxX = (constraints.maxWidth - margin - card.width).coerceAtLeast(margin)
        // Prefer the right of the tile; flip left if the card would overflow the right edge.
        val rightX = anchorBounds.right.roundToInt() + gap
        val x =
          if (rightX + card.width <= constraints.maxWidth - margin) rightX
          else anchorBounds.left.roundToInt() - gap - card.width
        val y = anchorBounds.top.roundToInt()
          .coerceIn(margin, (constraints.maxHeight - margin - card.height).coerceAtLeast(margin))
        card.place(x.coerceIn(margin, maxX), y)
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
      .shadow(24.dp, shape, ambientColor = NocturneColors.Focus, spotColor = NocturneColors.Focus)
      .clip(shape)
      .background(NocturneColors.BaseBlack)
      .background(NocturneColors.CardFill)
      .border(3.dp, NocturneColors.Focus, shape),
  ) {
    AppTileArt(app)
  }
}

/**
 * The anchored menu card. Deliberately sans-serif — it reads as a utility surface, breaking from
 * the Newsreader launcher type. Five items, no wrap; focus opens on Move.
 */
@Composable
private fun MenuCard(app: AppInfo, onAction: (LauncherAction) -> Unit) {
  val moveFocus = remember { FocusRequester() }
  LaunchedEffect(Unit) { runCatching { moveFocus.requestFocus() } }

  Column(
    modifier = Modifier
      .width(CardWidth)
      .clip(RoundedCornerShape(CardRadius))
      .background(NocturneColors.MenuCardFill)
      .border(1.dp, NocturneColors.MenuCardBorder, RoundedCornerShape(CardRadius))
      .padding(5.dp),
    verticalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    Text(
      text = app.label.uppercase(),
      fontFamily = FontFamily.SansSerif,
      fontSize = 9.sp,
      fontWeight = FontWeight.Medium,
      letterSpacing = 1.sp,
      color = NocturneColors.MenuHeader,
      modifier = Modifier.padding(start = 7.dp, top = 5.dp, bottom = 3.dp),
    )
    MenuCardItem("Move", focusRequester = moveFocus) { onAction(LauncherAction.MoveApp(app)) }
    MenuCardItem("Hide from favorites") { onAction(LauncherAction.HideApp(app)) }
    MenuCardItem("Open app settings") { onAction(LauncherAction.OpenAppSettings(app)) }
    MenuCardItem("Uninstall", danger = true) { onAction(LauncherAction.UninstallApp(app)) }
    MenuCardItem("Cancel") { onAction(LauncherAction.MenuDismissed) }
  }
}

@Composable
private fun MenuCardItem(
  label: String,
  danger: Boolean = false,
  focusRequester: FocusRequester? = null,
  onClick: () -> Unit,
) {
  val labelColor = if (danger) NocturneColors.DangerText else NocturneColors.MenuLabel
  Surface(
    onClick = onClick,
    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(5.dp)),
    colors = ClickableSurfaceDefaults.colors(
      containerColor = Color.Transparent,
      contentColor = labelColor,
      focusedContainerColor = NocturneColors.MenuItemFocusFill,
      focusedContentColor = labelColor,
      pressedContainerColor = NocturneColors.MenuItemFocusFill,
      pressedContentColor = labelColor,
    ),
    border = ClickableSurfaceDefaults.border(
      focusedBorder = Border(BorderStroke(1.dp, NocturneColors.Focus)),
    ),
    scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    modifier = Modifier
      .fillMaxWidth()
      .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
  ) {
    Text(
      text = label,
      fontFamily = FontFamily.SansSerif,
      fontSize = 13.sp,
      fontWeight = FontWeight.Medium,
      color = labelColor,
      modifier = Modifier.padding(horizontal = 7.dp, vertical = 7.dp),
    )
  }
}
