@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.pavlovsfrog.minimaltvlauncher

import android.content.ComponentName
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Glow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.pavlovsfrog.minimaltvlauncher.theme.MinimalTvLauncherTheme
import com.pavlovsfrog.minimaltvlauncher.theme.Newsreader
import com.pavlovsfrog.minimaltvlauncher.theme.NocturneColors
import com.pavlovsfrog.minimaltvlauncher.ui.AllAppsSettings
import com.pavlovsfrog.minimaltvlauncher.ui.AppActionMenu
import com.pavlovsfrog.minimaltvlauncher.ui.NocturneBackground
import kotlinx.coroutines.delay

// Grid width lives in ReorderMath.kt (GRID_COLUMNS) so the ViewModel's reorder math and the grid
// layout can never disagree.

// Design canvas is 1920×1080 px; TV 1080p is xhdpi (2.0), so design px ÷ 2 = dp/sp.
// Text shadow matches the design's `text-shadow: 0 3px 18px rgba(0,0,0,0.7)` (raw px at 1080p).
private val HeaderShadow = Shadow(Color.Black.copy(alpha = 0.7f), Offset(0f, 3f), blurRadius = 18f)

private val TimeStyle = TextStyle(
  fontFamily = Newsreader,
  fontWeight = FontWeight.Normal,
  fontSize = 60.sp,
  lineHeight = 54.sp, // design line-height 0.9
  letterSpacing = 0.5.sp,
  color = NocturneColors.TextPrimary,
  shadow = HeaderShadow,
  platformStyle = PlatformTextStyle(includeFontPadding = false),
)

private val AmPmStyle = TextStyle(
  fontFamily = Newsreader,
  fontWeight = FontWeight.Medium,
  fontSize = 17.sp,
  color = NocturneColors.TextSecondary,
  shadow = HeaderShadow,
)

private val WeatherStyle = TextStyle(
  fontFamily = Newsreader,
  fontWeight = FontWeight.Medium,
  fontSize = 13.sp,
  color = NocturneColors.TextWeather,
  shadow = HeaderShadow,
)

private val DateStyle = TextStyle(
  fontFamily = Newsreader,
  fontStyle = FontStyle.Italic,
  fontSize = 13.sp,
  color = NocturneColors.TextDate,
  shadow = HeaderShadow,
)

private val MessageStyle = TextStyle(
  fontFamily = Newsreader,
  fontStyle = FontStyle.Italic,
  fontSize = 16.sp,
  color = NocturneColors.TextMuted,
)

// Move-mode hint pill: sans-serif utility type (design §3, 19px ÷ 2 ≈ 9.5sp).
private val HintPillStyle = TextStyle(
  fontFamily = FontFamily.SansSerif,
  fontSize = 9.5.sp,
  color = NocturneColors.TextSecondary,
)

@Composable
fun LauncherScreen(
  state: LauncherState,
  onAction: (LauncherAction) -> Unit,
  modifier: Modifier = Modifier,
) {
  // Written by tiles after layout, read by the overlay when it opens — never in composition,
  // so plain maps (not snapshot state) are correct here.
  val tileBounds = remember { HashMap<String, Rect>() }
  val tileFocus = remember { HashMap<String, FocusRequester>() }
  val gearFocus = remember { FocusRequester() }
  val homeApps = (state.apps as? AppsUiState.Ready)?.apps ?: emptyList()
  val allApps = (state.apps as? AppsUiState.Ready)?.allApps ?: emptyList()

  // With no favorites there is no grid to autofocus — start the remote on the gear. Guarded to
  // Ready so it can never fire during Loading (when the gear isn't even composed).
  LaunchedEffect(state.apps is AppsUiState.Ready, homeApps.isEmpty(), allApps.isEmpty()) {
    if (state.apps is AppsUiState.Ready && homeApps.isEmpty() && allApps.isNotEmpty()) {
      runCatching { gearFocus.requestFocus() }
    }
  }

  // Reveal the whole home surface (gear + clock + weather + grid) with one fade the moment apps
  // finish loading. Before that we render only the wallpaper — nothing focusable — so remote
  // focus can't flash onto the settings gear ahead of the first app card appearing.
  var revealed by remember { mutableStateOf(false) }
  LaunchedEffect(state.apps is AppsUiState.Ready) {
    if (state.apps is AppsUiState.Ready) revealed = true
  }
  val contentAlpha by animateFloatAsState(
    targetValue = if (revealed) 1f else 0f,
    animationSpec = tween(380),
    label = "contentReveal",
  )

  // Move mode: OK/Back drop the tile; auto-repeat D-pad is allowed for fast dragging.
  val reordering = state.reordering
  BackHandler(enabled = reordering != null) { onAction(LauncherAction.CommitMove) }
  // Keep the remote pinned to the moving tile as the grid reflows around it after each step.
  LaunchedEffect(reordering?.packageName, homeApps) {
    val moving = reordering?.packageName ?: return@LaunchedEffect
    withFrameNanos {} // let the reflow settle before touching focus
    runCatching { tileFocus[moving]?.requestFocus() }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .onPreviewKeyEvent { event ->
        if (state.reordering == null || event.type != KeyEventType.KeyDown) {
          return@onPreviewKeyEvent false
        }
        when (event.key) {
          Key.DirectionLeft -> onAction(LauncherAction.ReorderStep(MoveDirection.Left)).let { true }
          Key.DirectionRight -> onAction(LauncherAction.ReorderStep(MoveDirection.Right)).let { true }
          Key.DirectionUp -> onAction(LauncherAction.ReorderStep(MoveDirection.Up)).let { true }
          Key.DirectionDown -> onAction(LauncherAction.ReorderStep(MoveDirection.Down)).let { true }
          Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
            if (event.nativeKeyEvent.repeatCount == 0) onAction(LauncherAction.CommitMove)
            true // consume repeats too, so a held OK can't leak a click into the grid
          }
          else -> false
        }
      },
  ) {
    val onOpenSettings = { onAction(LauncherAction.OpenSettings) }
    Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = contentAlpha }) {
      when (val apps = state.apps) {
        // Wallpaper only while loading: no header, no focusable gear, nothing to flash focus onto.
        AppsUiState.Loading -> Unit
        is AppsUiState.Ready ->
          when {
            apps.apps.isNotEmpty() ->
              AppGrid(
                clock = state.clock,
                weather = state.weather,
                apps = apps.apps,
                onAppClick = { onAction(LauncherAction.AppClicked(it)) },
                onAppLongPress = { onAction(LauncherAction.AppLongPressed(it)) },
                onOpenSettings = onOpenSettings,
                gearFocusRequester = gearFocus,
                tileBounds = tileBounds,
                tileFocus = tileFocus,
                reordering = state.reordering,
              )
            // Apps exist but every one is hidden — point at settings instead of "no apps".
            apps.allApps.isNotEmpty() ->
              MessageScreen(
                clock = state.clock,
                weather = state.weather,
                message = "No favorites on this screen",
                subline = "Open Settings (⚙) to star the apps you want here",
                onOpenSettings = onOpenSettings,
                gearFocusRequester = gearFocus,
              )
            else ->
              MessageScreen(
                clock = state.clock,
                weather = state.weather,
                message = "No apps installed",
                onOpenSettings = onOpenSettings,
                gearFocusRequester = gearFocus,
              )
          }
      }
    }

    when (val overlay = state.overlay) {
      is Overlay.AppMenu ->
        AppActionMenu(
          app = overlay.app,
          anchorBounds = tileBounds[overlay.app.packageName] ?: Rect.Zero,
          onAction = onAction,
        )
      Overlay.Settings ->
        AllAppsSettings(allApps = allApps, onAction = onAction)
      Overlay.None -> Unit
    }

    if (reordering != null) {
      MoveHintPill(
        appLabel = reordering.label,
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 19.dp),
      )
    }

    MenuFocusRestorer(overlay = state.overlay, homeApps = homeApps, tileFocus = tileFocus)
    SettingsFocusRestorer(overlay = state.overlay, gearFocus = gearFocus)
  }
}

/** Puts the remote back on the gear when the settings takeover closes. */
@Composable
private fun SettingsFocusRestorer(overlay: Overlay, gearFocus: FocusRequester) {
  var wasOpen by remember { mutableStateOf(false) }
  LaunchedEffect(overlay) {
    if (overlay == Overlay.Settings) {
      wasOpen = true
    } else if (wasOpen && overlay == Overlay.None) {
      wasOpen = false
      withFrameNanos {}
      runCatching { gearFocus.requestFocus() }
    }
  }
}

/** Non-scrolling states: nothing can scroll away, so the header sits statically on top. */
@Composable
private fun MessageScreen(
  clock: ClockUiState,
  weather: WeatherUiState,
  message: String,
  onOpenSettings: () -> Unit,
  gearFocusRequester: FocusRequester,
  subline: String? = null,
) {
  Column(modifier = Modifier.fillMaxSize().padding(horizontal = 50.dp, vertical = 37.dp)) {
    NocturneHeader(
      clock = clock,
      weather = weather,
      onOpenSettings = onOpenSettings,
      gearFocusRequester = gearFocusRequester,
    )
    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = message, style = MessageStyle)
        if (subline != null) {
          Text(
            text = subline,
            style = MessageStyle.copy(fontSize = 13.sp),
            modifier = Modifier.padding(top = 6.dp),
          )
        }
      }
    }
  }
}

/**
 * Returns focus to the grid when the action menu closes: to the pressed tile after Cancel/Back,
 * or to the first remaining tile once a Hide reflow removes it. Re-keyed on [homeApps] because
 * the hide write lands asynchronously, a recomposition after the overlay is already gone.
 */
@Composable
private fun MenuFocusRestorer(
  overlay: Overlay,
  homeApps: List<AppInfo>,
  tileFocus: Map<String, FocusRequester>,
) {
  var pendingFocus by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(overlay) {
    if (overlay is Overlay.AppMenu) pendingFocus = overlay.app.packageName
  }

  LaunchedEffect(overlay, homeApps) {
    val packageName = pendingFocus ?: return@LaunchedEffect
    if (overlay != Overlay.None) return@LaunchedEffect
    withFrameNanos {} // let a pending grid reflow settle before touching focus
    val target =
      if (homeApps.any { it.packageName == packageName }) packageName
      else homeApps.firstOrNull()?.packageName
    target?.let { runCatching { tileFocus[it]?.requestFocus() } }
    // Keep watching briefly: the hide write may still be in flight. Then stop, so later
    // unrelated list changes don't yank focus around.
    delay(600)
    pendingFocus = null
  }
}

/**
 * Space-between header: settings gear top-left, "Big clock hero" block (Weather Layouts
 * variant B — time dominant, weather tucked beneath) top-right.
 */
@Composable
private fun NocturneHeader(
  clock: ClockUiState,
  weather: WeatherUiState,
  onOpenSettings: () -> Unit,
  gearFocusRequester: FocusRequester,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.Top,
  ) {
    GearButton(onClick = onOpenSettings, focusRequester = gearFocusRequester)

    Column(horizontalAlignment = Alignment.End) {
      Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(text = clock.time, style = TimeStyle, modifier = Modifier.alignByBaseline())
        Text(text = clock.amPm, style = AmPmStyle, modifier = Modifier.alignByBaseline())
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(top = 2.dp, end = 2.dp),
      ) {
        if (weather is WeatherUiState.Ready) {
          SunDot()
          Text(
            text = listOf(weather.tempText, weather.condition)
              .filter { it.isNotEmpty() }
              .joinToString(" "),
            style = WeatherStyle,
          )
          SeparatorDot()
        }
        Text(text = clock.date, style = DateStyle)
      }
    }
  }
}

/** 37dp circular ⚙ button (design: 74px circle, 32px glyph). */
@Composable
private fun GearButton(onClick: () -> Unit, focusRequester: FocusRequester) {
  Surface(
    onClick = onClick,
    shape = ClickableSurfaceDefaults.shape(CircleShape),
    colors = ClickableSurfaceDefaults.colors(
      containerColor = NocturneColors.GearFill,
      contentColor = NocturneColors.TextWeather,
      focusedContainerColor = NocturneColors.GearFill,
      focusedContentColor = NocturneColors.TextWeather,
      pressedContainerColor = NocturneColors.GearFill,
      pressedContentColor = NocturneColors.TextWeather,
    ),
    border = ClickableSurfaceDefaults.border(
      border = Border(BorderStroke(1.dp, NocturneColors.GearBorder)),
      focusedBorder = Border(BorderStroke(2.dp, NocturneColors.Focus)),
    ),
    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
    modifier = Modifier
      .size(37.dp)
      .focusRequester(focusRequester)
      .semantics { contentDescription = "Settings" },
  ) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text(text = "⚙", fontSize = 16.sp)
    }
  }
}

/** 10dp amber dot with a soft halo — design: box-shadow 0 0 14px oklch(0.82 0.15 78 / 0.8). */
@Composable
private fun SunDot() {
  Box(
    modifier = Modifier.size(10.dp).drawBehind {
      val dotRadius = size.minDimension / 2f
      val haloRadius = dotRadius * 2.4f
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            NocturneColors.SunDot.copy(alpha = 0.8f),
            NocturneColors.SunDot.copy(alpha = 0f),
          ),
          center = center,
          radius = haloRadius,
        ),
        radius = haloRadius,
      )
      drawCircle(color = NocturneColors.SunDot, radius = dotRadius)
    }
  )
}

@Composable
private fun SeparatorDot() {
  Box(
    modifier = Modifier
      .size(3.dp)
      .background(NocturneColors.DotSeparator, shape = RoundedCornerShape(percent = 50))
  )
}

@Composable
private fun AppGrid(
  clock: ClockUiState,
  weather: WeatherUiState,
  apps: List<AppInfo>,
  onAppClick: (AppInfo) -> Unit,
  onAppLongPress: (AppInfo) -> Unit,
  onOpenSettings: () -> Unit,
  gearFocusRequester: FocusRequester,
  tileBounds: MutableMap<String, Rect>,
  tileFocus: MutableMap<String, FocusRequester>,
  reordering: AppInfo?,
) {
  val firstItemFocus = remember { FocusRequester() }
  // Move focus onto the first app so the remote can drive the grid immediately.
  LaunchedEffect(apps.isNotEmpty()) {
    if (apps.isNotEmpty()) firstItemFocus.requestFocus()
  }

  // The grid is the whole screen: design's full-screen scroll surface with
  // padding 74px 100px 120px (÷2 → dp), so cards slide under the screen edges.
  LazyVerticalGrid(
    columns = GridCells.Fixed(GRID_COLUMNS),
    horizontalArrangement = Arrangement.spacedBy(20.dp),
    verticalArrangement = Arrangement.spacedBy(20.dp),
    contentPadding = PaddingValues(start = 50.dp, top = 37.dp, end = 50.dp, bottom = 60.dp),
    modifier = Modifier.fillMaxSize(),
  ) {
    // The header rides in the scroll flow and scrolls away with the content.
    // 15dp + the 20dp row gap = the design's 70px (35dp) header→grid spacing.
    item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
      NocturneHeader(
        clock = clock,
        weather = weather,
        onOpenSettings = onOpenSettings,
        gearFocusRequester = gearFocusRequester,
        modifier = Modifier.padding(bottom = 15.dp),
      )
    }
    itemsIndexed(apps, key = { _, app -> app.componentName.flattenToString() }) { index, app ->
      val focusRequester = remember { FocusRequester() }
      DisposableEffect(app.packageName) {
        tileFocus[app.packageName] = focusRequester
        onDispose {
          tileFocus.remove(app.packageName)
          tileBounds.remove(app.packageName)
        }
      }
      val isMoving = reordering?.packageName == app.packageName
      val reorderActive = reordering != null
      AppCard(
        app = app,
        // While reordering, the D-pad is captured for moves — tiles are inert to click/long-press.
        onClick = { if (!reorderActive) onAppClick(app) },
        onLongClick = { if (!reorderActive) onAppLongPress(app) },
        isMoving = isMoving,
        modifier = Modifier
          .animateItem()
          .focusRequester(focusRequester)
          .onGloballyPositioned { tileBounds[app.packageName] = it.boundsInRoot() }
          .then(if (index == 0) Modifier.focusRequester(firstItemFocus) else Modifier),
      )
    }
  }
}

@Composable
private fun AppCard(
  app: AppInfo,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  modifier: Modifier = Modifier,
  isMoving: Boolean = false,
) {
  // The held tile in Move mode lifts higher than an ordinary focus: bigger scale, thicker ring,
  // deeper glow — so "held for dragging" reads distinctly from plain focus.
  Card(
    onClick = onClick,
    onLongClick = onLongClick,
    shape = CardDefaults.shape(shape = RoundedCornerShape(8.dp)),
    scale = CardDefaults.scale(focusedScale = if (isMoving) 1.1f else 1.07f),
    border = CardDefaults.border(
      border = Border(BorderStroke(if (isMoving) 3.dp else 1.dp, NocturneColors.CardBorder)),
      focusedBorder = Border(BorderStroke(if (isMoving) 5.dp else 3.dp, NocturneColors.Focus)),
    ),
    glow = CardDefaults.glow(
      focusedGlow = Glow(
        elevationColor = NocturneColors.Focus,
        elevation = if (isMoving) 40.dp else 24.dp,
      ),
    ),
    colors = CardDefaults.colors(
      containerColor = NocturneColors.CardFill,
      focusedContainerColor = NocturneColors.CardFill,
      pressedContainerColor = NocturneColors.CardFill,
    ),
    modifier = modifier.fillMaxWidth().aspectRatio(16f / 9f),
  ) {
    AppTileArt(app)
  }
}

/** Bottom-center hint while a tile is held in Move mode. */
@Composable
private fun MoveHintPill(appLabel: String, modifier: Modifier = Modifier) {
  var entered by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) { entered = true }
  val alpha by animateFloatAsState(if (entered) 1f else 0f, tween(160), label = "hintAlpha")
  val rise by animateFloatAsState(if (entered) 0f else 1f, tween(160), label = "hintRise")

  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
      .graphicsLayer {
        this.alpha = alpha
        translationY = rise * 8.dp.toPx()
      }
      .clip(RoundedCornerShape(percent = 50))
      .background(NocturneColors.HintPillFill)
      .border(1.dp, NocturneColors.GearBorder, RoundedCornerShape(percent = 50))
      .padding(horizontal = 13.dp, vertical = 5.dp),
  ) {
    Text(text = "Moving ", style = HintPillStyle)
    Text(text = appLabel, style = HintPillStyle.copy(fontWeight = FontWeight.SemiBold, color = NocturneColors.TextPrimary))
    Text(text = " · arrows to reorder · OK to drop", style = HintPillStyle)
  }
}

/** The tile's visual — banner, small icon, or letter fallback. Shared with the menu's ghost. */
@Composable
internal fun AppTileArt(app: AppInfo, modifier: Modifier = Modifier) {
  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    val bitmap = app.image
    if (bitmap != null) {
      if (app.hasBanner) {
        Image(
          bitmap = bitmap.asImageBitmap(),
          contentDescription = app.label,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      } else {
        // Square icon: keep it small and centered on the quiet card fill.
        Image(
          bitmap = bitmap.asImageBitmap(),
          contentDescription = app.label,
          contentScale = ContentScale.Fit,
          modifier = Modifier.size(32.dp),
        )
      }
    } else {
      // No visible label under the card, so talkback needs the full name here.
      Text(
        text = app.label.take(1).uppercase(),
        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.SemiBold),
        color = NocturneColors.TextSecondary,
        modifier = Modifier.semantics { contentDescription = app.label },
      )
    }
  }
}

// ===== Previews (1080p TV) =====

private fun previewApps(count: Int) = List(count) { index ->
  AppInfo(
    label = listOf("Stream", "Cinema", "Music", "Live TV", "Photos", "Arcade", "Browser", "Podcasts")
      .getOrElse(index) { "App $index" },
    packageName = "com.pavlovsfrog.preview$index",
    componentName = ComponentName("com.pavlovsfrog.preview$index", "Main"),
    image = null,
    hasBanner = false,
  )
}

private val previewClock = ClockUiState(time = "9:42", amPm = "PM", date = "Sat · Jul 4")

@Preview(device = "id:tv_1080p")
@Composable
private fun LauncherScreenPreview() {
  MinimalTvLauncherTheme {
    NocturneBackground {
      LauncherScreen(
        state = LauncherState(
          apps = AppsUiState.Ready(previewApps(8)),
          clock = previewClock,
          weather = WeatherUiState.Ready(tempText = "72°", condition = "Sunny"),
        ),
        onAction = {},
      )
    }
  }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun LauncherScreenWeatherHiddenPreview() {
  MinimalTvLauncherTheme {
    NocturneBackground {
      LauncherScreen(
        state = LauncherState(
          apps = AppsUiState.Ready(previewApps(6)),
          clock = previewClock,
          weather = WeatherUiState.Hidden,
        ),
        onAction = {},
      )
    }
  }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun LauncherScreenEmptyPreview() {
  MinimalTvLauncherTheme {
    NocturneBackground {
      LauncherScreen(
        state = LauncherState(
          apps = AppsUiState.Ready(emptyList()),
          clock = previewClock,
          weather = WeatherUiState.Hidden,
        ),
        onAction = {},
      )
    }
  }
}
