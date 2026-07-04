@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.example.minimaltvlauncher

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.example.minimaltvlauncher.theme.MinimalTvLauncherTheme

class MainActivity : ComponentActivity() {

  private val viewModel: LauncherViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      MinimalTvLauncherTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          val state by viewModel.uiState.collectAsStateWithLifecycle()
          LauncherScreen(uiState = state, onAppClick = ::launchApp)
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    // Pick up apps installed or removed while the launcher was in the background.
    viewModel.refresh()
  }

  private fun launchApp(app: AppInfo) {
    val intent = Intent(Intent.ACTION_MAIN).apply {
      component = app.componentName
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
    }
    try {
      startActivity(intent)
    } catch (_: ActivityNotFoundException) {
      // The app was uninstalled since we last loaded; refresh the list.
      viewModel.refresh()
    }
  }
}
