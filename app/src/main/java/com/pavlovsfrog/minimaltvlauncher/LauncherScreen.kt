@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.pavlovsfrog.minimaltvlauncher

import android.content.ComponentName
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Glow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.pavlovsfrog.minimaltvlauncher.theme.MinimalTvLauncherTheme
import com.pavlovsfrog.minimaltvlauncher.theme.Newsreader
import com.pavlovsfrog.minimaltvlauncher.theme.NocturneColors
import com.pavlovsfrog.minimaltvlauncher.ui.NocturneBackground

private const val COLUMNS = 4

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

@Composable
fun LauncherScreen(
  state: LauncherState,
  onAction: (LauncherAction) -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.fillMaxSize()) {
    when (val apps = state.apps) {
      AppsUiState.Loading -> MessageScreen(state.clock, state.weather, "Loading apps…")
      is AppsUiState.Ready ->
        when {
          apps.apps.isNotEmpty() ->
            AppGrid(
              clock = state.clock,
              weather = state.weather,
              apps = apps.apps,
              onAppClick = { onAction(LauncherAction.AppClicked(it)) },
            )
          // Apps exist but every one is hidden — point at settings instead of "no apps".
          apps.allApps.isNotEmpty() ->
            MessageScreen(
              clock = state.clock,
              weather = state.weather,
              message = "No favorites on this screen",
              subline = "Open Settings (⚙) to star the apps you want here",
            )
          else -> MessageScreen(state.clock, state.weather, "No apps installed")
        }
    }
  }
}

/** Non-scrolling states: nothing can scroll away, so the header sits statically on top. */
@Composable
private fun MessageScreen(
  clock: ClockUiState,
  weather: WeatherUiState,
  message: String,
  subline: String? = null,
) {
  Column(modifier = Modifier.fillMaxSize().padding(horizontal = 50.dp, vertical = 37.dp)) {
    NocturneHeader(clock = clock, weather = weather)
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

/** "Big clock hero" header (Weather Layouts variant B): time dominant, weather tucked beneath. */
@Composable
private fun NocturneHeader(
  clock: ClockUiState,
  weather: WeatherUiState,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
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
) {
  val firstItemFocus = remember { FocusRequester() }
  // Move focus onto the first app so the remote can drive the grid immediately.
  LaunchedEffect(apps.isNotEmpty()) {
    if (apps.isNotEmpty()) firstItemFocus.requestFocus()
  }

  // The grid is the whole screen: design's full-screen scroll surface with
  // padding 74px 100px 120px (÷2 → dp), so cards slide under the screen edges.
  LazyVerticalGrid(
    columns = GridCells.Fixed(COLUMNS),
    horizontalArrangement = Arrangement.spacedBy(20.dp),
    verticalArrangement = Arrangement.spacedBy(20.dp),
    contentPadding = PaddingValues(start = 50.dp, top = 37.dp, end = 50.dp, bottom = 60.dp),
    modifier = Modifier.fillMaxSize(),
  ) {
    // The header rides in the scroll flow and scrolls away with the content.
    // 15dp + the 20dp row gap = the design's 70px (35dp) header→grid spacing.
    item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
      NocturneHeader(clock = clock, weather = weather, modifier = Modifier.padding(bottom = 15.dp))
    }
    itemsIndexed(apps, key = { _, app -> app.componentName.flattenToString() }) { index, app ->
      AppCard(
        app = app,
        onClick = { onAppClick(app) },
        modifier = if (index == 0) Modifier.focusRequester(firstItemFocus) else Modifier,
      )
    }
  }
}

@Composable
private fun AppCard(app: AppInfo, onClick: () -> Unit, modifier: Modifier = Modifier) {
  Card(
    onClick = onClick,
    shape = CardDefaults.shape(shape = RoundedCornerShape(8.dp)),
    scale = CardDefaults.scale(focusedScale = 1.07f),
    border = CardDefaults.border(
      border = Border(BorderStroke(1.dp, NocturneColors.CardBorder)),
      focusedBorder = Border(BorderStroke(3.dp, NocturneColors.Amber)),
    ),
    glow = CardDefaults.glow(
      focusedGlow = Glow(elevationColor = NocturneColors.Amber, elevation = 24.dp),
    ),
    colors = CardDefaults.colors(
      containerColor = NocturneColors.CardFill,
      focusedContainerColor = NocturneColors.CardFill,
      pressedContainerColor = NocturneColors.CardFill,
    ),
    modifier = modifier.fillMaxWidth().aspectRatio(16f / 9f),
  ) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
