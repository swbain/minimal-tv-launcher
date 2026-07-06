package com.pavlovsfrog.minimaltvlauncher

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pavlovsfrog.minimaltvlauncher.data.OrderRepository
import com.pavlovsfrog.minimaltvlauncher.data.VisibilityRepository
import com.pavlovsfrog.minimaltvlauncher.weather.WeatherProvider
import com.pavlovsfrog.minimaltvlauncher.weather.WmoWeatherCode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LauncherViewModel @Inject constructor(
  private val appsLoader: AppsLoader,
  private val weatherRepository: WeatherProvider,
  private val timeSource: TimeSource,
  private val visibilityRepository: VisibilityRepository,
  private val orderRepository: OrderRepository,
) : ViewModel() {

  private val _state = MutableStateFlow(LauncherState())
  val state: StateFlow<LauncherState> = _state.asStateFlow()

  // replay = 0 on purpose: a replayed LaunchApp would relaunch an app on Activity recreate.
  // Clicks can only happen while the Activity is started, so a collector is always live.
  private val _events = MutableSharedFlow<LauncherEvent>(extraBufferCapacity = 1)
  val events: SharedFlow<LauncherEvent> = _events.asSharedFlow()

  // null until the first PackageManager load; combine() below waits for it.
  private val installedApps = MutableStateFlow<List<AppInfo>?>(null)

  init {
    deriveAppsState()
    reloadApps()
    startClockTicker()
    startWeatherLoop()
  }

  fun onAction(action: LauncherAction) {
    when (action) {
      is LauncherAction.AppClicked ->
        _events.tryEmit(LauncherEvent.LaunchApp(action.app.componentName))
      is LauncherAction.AppLongPressed ->
        _state.update { it.copy(overlay = Overlay.AppMenu(action.app)) }
      LauncherAction.MenuDismissed -> _state.update { it.copy(overlay = Overlay.None) }
      is LauncherAction.HideApp -> {
        _state.update { it.copy(overlay = Overlay.None) }
        // The Phase-2 combine() propagates the write back into the grid.
        viewModelScope.launch {
          visibilityRepository.setHidden(action.app.packageName, hidden = true)
        }
      }
      is LauncherAction.UninstallApp -> {
        _state.update { it.copy(overlay = Overlay.None) }
        _events.tryEmit(LauncherEvent.RequestUninstall(action.app.packageName))
      }
      is LauncherAction.OpenAppSettings -> {
        _state.update { it.copy(overlay = Overlay.None) }
        _events.tryEmit(LauncherEvent.OpenAppInfo(action.app.packageName))
      }
      is LauncherAction.MoveApp ->
        _state.update { it.copy(overlay = Overlay.None, reordering = action.app) }
      LauncherAction.CommitMove -> _state.update { it.copy(reordering = null) }
      is LauncherAction.ReorderStep -> reorder(action.direction)
      LauncherAction.OpenSettings -> _state.update { it.copy(overlay = Overlay.Settings) }
      LauncherAction.CloseSettings -> _state.update { it.copy(overlay = Overlay.None) }
      is LauncherAction.ToggleFavorite -> {
        val entry = (state.value.apps as? AppsUiState.Ready)
          ?.allApps?.firstOrNull { it.app.packageName == action.packageName }
          ?: return
        viewModelScope.launch {
          // Favoriting means un-hiding: hidden is the inverse of isFavorite.
          visibilityRepository.setHidden(action.packageName, hidden = entry.isFavorite)
        }
      }
      LauncherAction.ScreenResumed -> {
        // Snap the clock immediately (the TV may have slept past many ticks or changed zone).
        _state.update { it.copy(clock = ClockFormatter.format(timeSource.nowMillis())) }
        reloadApps()
        refreshWeather()
      }
      LauncherAction.LaunchFailed -> reloadApps()
    }
  }

  /**
   * Installed apps × hidden set × saved order → state, so a DB toggle or reorder updates home
   * without a reload. Apps are sorted by their position in the saved order; unknown packages sort
   * last, and since [installedApps] arrives alphabetically, stable-sort keeps them alphabetical.
   */
  private fun deriveAppsState() {
    viewModelScope.launch {
      combine(
        installedApps.filterNotNull(),
        visibilityRepository.hiddenPackages(),
        orderRepository.order(),
      ) { installed, hidden, order ->
        val rank = order.withIndex().associate { (index, pkg) -> pkg to index }
        installed
          .sortedBy { rank[it.packageName] ?: Int.MAX_VALUE }
          .map { AppEntry(app = it, isFavorite = it.packageName !in hidden) }
      }.collect { entries ->
        _state.update {
          it.copy(
            apps = AppsUiState.Ready(
              apps = entries.filter(AppEntry::isFavorite).map(AppEntry::app),
              allApps = entries,
            )
          )
        }
      }
    }
  }

  /**
   * One Move-mode step: reorder the visible favorites, thread the change back through the full
   * order (keeping hidden apps pinned), and persist. A blocked edge move writes nothing.
   */
  private fun reorder(direction: MoveDirection) {
    val moving = state.value.reordering?.packageName ?: return
    val ready = state.value.apps as? AppsUiState.Ready ?: return
    val visible = ready.apps.map(AppInfo::packageName)
    val newVisible = moveWithinVisible(visible, moving, direction, GRID_COLUMNS)
    if (newVisible == visible) return
    val newFull = threadVisibleIntoFull(ready.allApps.map { it.app.packageName }, newVisible)
    viewModelScope.launch { orderRepository.setOrder(newFull) }
  }

  private fun reloadApps() {
    viewModelScope.launch {
      val apps = appsLoader.loadApps()
      installedApps.value = apps
      // Keep the tables honest: uninstall → reinstall behaves like a fresh install (visible).
      val packages = apps.map(AppInfo::packageName)
      visibilityRepository.prune(packages)
      orderRepository.prune(packages)
    }
  }

  /** Ticks aligned to minute boundaries — the design shows minutes only. */
  private fun startClockTicker() {
    viewModelScope.launch {
      while (true) {
        val now = timeSource.nowMillis()
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
  }
}
