@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.example.minimaltvlauncher

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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val COLUMNS = 4

@Composable
fun LauncherScreen(
  uiState: LauncherUiState,
  onAppClick: (AppInfo) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 32.dp)) {
    Header()

    when (uiState) {
      LauncherUiState.Loading -> CenteredMessage("Loading apps…")
      is LauncherUiState.Ready ->
        if (uiState.apps.isEmpty()) {
          CenteredMessage("No apps installed")
        } else {
          AppGrid(apps = uiState.apps, onAppClick = onAppClick)
        }
    }
  }
}

@Composable
private fun Header() {
  var now by remember { mutableStateOf(System.currentTimeMillis()) }
  LaunchedEffect(Unit) {
    while (true) {
      now = System.currentTimeMillis()
      delay(10_000)
    }
  }
  val time = remember(now / 60_000) { SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(now)) }

  Row(
    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(text = "Apps", style = MaterialTheme.typography.headlineMedium)
    Text(
      text = time,
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun AppGrid(apps: List<AppInfo>, onAppClick: (AppInfo) -> Unit) {
  val firstItemFocus = remember { FocusRequester() }
  // Move focus onto the first app so the remote can drive the grid immediately.
  LaunchedEffect(apps.isNotEmpty()) {
    if (apps.isNotEmpty()) firstItemFocus.requestFocus()
  }

  LazyVerticalGrid(
    columns = GridCells.Fixed(COLUMNS),
    horizontalArrangement = Arrangement.spacedBy(24.dp),
    verticalArrangement = Arrangement.spacedBy(24.dp),
    contentPadding = PaddingValues(bottom = 48.dp),
  ) {
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
  Column(modifier = modifier) {
    Card(
      onClick = onClick,
      shape = CardDefaults.shape(shape = MaterialTheme.shapes.medium),
      modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
    ) {
      Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
      ) {
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
            // Square icon: keep it small and centered on the card.
            Image(
              bitmap = bitmap.asImageBitmap(),
              contentDescription = app.label,
              contentScale = ContentScale.Fit,
              modifier = Modifier.size(64.dp),
            )
          }
        } else {
          Text(text = app.label.take(1).uppercase(), style = MaterialTheme.typography.headlineLarge)
        }
      }
    }

    Text(
      text = app.label,
      style = MaterialTheme.typography.bodyMedium,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      textAlign = TextAlign.Center,
      modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
  }
}

@Composable
private fun CenteredMessage(message: String) {
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Text(text = message, style = MaterialTheme.typography.titleLarge)
  }
}
