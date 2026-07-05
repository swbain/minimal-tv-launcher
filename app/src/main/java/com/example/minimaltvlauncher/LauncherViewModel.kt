package com.example.minimaltvlauncher

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.minimaltvlauncher.weather.OkHttpJsonFetcher
import com.example.minimaltvlauncher.weather.WeatherProvider
import com.example.minimaltvlauncher.weather.WeatherRepository
import com.example.minimaltvlauncher.weather.WmoWeatherCode
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LauncherViewModel(
  private val appsLoader: AppsLoader,
  private val weatherRepository: WeatherProvider,
  private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) : ViewModel() {

  private val _state = MutableStateFlow(LauncherState())
  val state: StateFlow<LauncherState> = _state.asStateFlow()

  // replay = 0 on purpose: a replayed LaunchApp would relaunch an app on Activity recreate.
  // Clicks can only happen while the Activity is started, so a collector is always live.
  private val _events = MutableSharedFlow<LauncherEvent>(extraBufferCapacity = 1)
  val events: SharedFlow<LauncherEvent> = _events.asSharedFlow()

  init {
    reloadApps()
    startClockTicker()
    startWeatherLoop()
  }

  fun onAction(action: LauncherAction) {
    when (action) {
      is LauncherAction.AppClicked ->
        _events.tryEmit(LauncherEvent.LaunchApp(action.app.componentName))
      LauncherAction.ScreenResumed -> {
        // Snap the clock immediately (the TV may have slept past many ticks or changed zone).
        _state.update { it.copy(clock = ClockFormatter.format(currentTimeMillis())) }
        reloadApps()
        refreshWeather()
      }
      LauncherAction.LaunchFailed -> reloadApps()
    }
  }

  private fun reloadApps() {
    viewModelScope.launch {
      val apps = appsLoader.loadApps()
      _state.update { it.copy(apps = AppsUiState.Ready(apps)) }
    }
  }

  /** Ticks aligned to minute boundaries — the design shows minutes only. */
  private fun startClockTicker() {
    viewModelScope.launch {
      while (true) {
        val now = currentTimeMillis()
        _state.update { it.copy(clock = ClockFormatter.format(now)) }
        delay(60_000 - now % 60_000)
      }
    }
  }

  private fun startWeatherLoop() {
    viewModelScope.launch {
      while (true) {
        val succeeded = applyWeather()
        // Retry fast while failing: cold boot often races the network coming up, and
        // waiting the full refresh interval would leave the header blank for 30 minutes.
        delay(if (succeeded) 30.minutes else 1.minutes)
      }
    }
  }

  /** The repository's TTL cache makes resume-triggered refreshes effectively free. */
  private fun refreshWeather() {
    viewModelScope.launch { applyWeather() }
  }

  private suspend fun applyWeather(): Boolean {
    // Weather is strictly additive: failures never touch apps/clock, and a cold failure
    // just leaves the header Hidden until a later attempt succeeds.
    return weatherRepository.currentWeather()
      .onSuccess { weather ->
        _state.update {
          it.copy(
            weather = WeatherUiState.Ready(
              tempText = "${weather.temperature}°",
              condition = WmoWeatherCode.label(weather.code, weather.isDay) ?: "",
            )
          )
        }
      }
      .onFailure { Log.w(TAG, "Weather fetch failed", it) }
      .isSuccess
  }

  companion object {
    private const val TAG = "LauncherViewModel"

    val Factory: ViewModelProvider.Factory = viewModelFactory {
      initializer {
        val application =
          checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
        LauncherViewModel(
          appsLoader = AppsRepository(application),
          weatherRepository = WeatherRepository(OkHttpJsonFetcher()),
        )
      }
    }
  }
}
