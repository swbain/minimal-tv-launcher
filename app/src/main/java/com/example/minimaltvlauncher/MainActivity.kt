@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.example.minimaltvlauncher

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import com.example.minimaltvlauncher.theme.MinimalTvLauncherTheme
import com.example.minimaltvlauncher.theme.NocturneColors
import com.example.minimaltvlauncher.ui.NocturneBackground
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

  private val viewModel: LauncherViewModel by viewModels { LauncherViewModel.Factory }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // The Activity is the only event consumer: it owns startActivity.
    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.events.collect { event ->
          when (event) {
            is LauncherEvent.LaunchApp -> launchApp(event.componentName)
          }
        }
      }
    }

    setContent {
      MinimalTvLauncherTheme {
        NocturneBackground {
          // Transparent Surface keeps TV-Material content locals without hiding the wallpaper.
          Surface(
            modifier = Modifier.fillMaxSize(),
            colors = SurfaceDefaults.colors(
              containerColor = androidx.compose.ui.graphics.Color.Transparent,
              contentColor = NocturneColors.TextPrimary,
            ),
          ) {
            val state by viewModel.state.collectAsStateWithLifecycle()
            LauncherScreen(state = state, onAction = viewModel::onAction)
          }
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    // Pick up apps installed or removed while the launcher was in the background.
    viewModel.onAction(LauncherAction.ScreenResumed)
  }

  private fun launchApp(componentName: ComponentName) {
    val intent = Intent(Intent.ACTION_MAIN).apply {
      component = componentName
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
    }
    try {
      startActivity(intent)
    } catch (_: ActivityNotFoundException) {
      // The app was uninstalled since we last loaded; refresh the list.
      viewModel.onAction(LauncherAction.LaunchFailed)
    }
  }
}
