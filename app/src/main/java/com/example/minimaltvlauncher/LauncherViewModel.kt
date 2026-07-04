package com.example.minimaltvlauncher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Screen state for the launcher. */
sealed interface LauncherUiState {
  data object Loading : LauncherUiState

  data class Ready(val apps: List<AppInfo>) : LauncherUiState
}

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = AppsRepository(application)

  private val _uiState = MutableStateFlow<LauncherUiState>(LauncherUiState.Loading)
  val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

  init {
    refresh()
  }

  /** Reloads the installed apps. Called on start and whenever the launcher resumes. */
  fun refresh() {
    viewModelScope.launch {
      _uiState.value = LauncherUiState.Ready(repository.loadApps())
    }
  }
}
