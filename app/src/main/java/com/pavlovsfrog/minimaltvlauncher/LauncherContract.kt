package com.pavlovsfrog.minimaltvlauncher

import android.content.ComponentName

/**
 * MVI-lite contract: [LauncherState] is the only render input, [LauncherAction]s carry user
 * intent into the ViewModel, and [LauncherEvent]s are one-shot effects owned by the Activity.
 */
data class LauncherState(
  val apps: AppsUiState = AppsUiState.Loading,
  val clock: ClockUiState = ClockUiState("", "", ""),
  val weather: WeatherUiState = WeatherUiState.Hidden,
  val overlay: Overlay = Overlay.None,
)

/** At most one overlay owns the screen (and TV focus) at a time. */
sealed interface Overlay {
  data object None : Overlay

  /** The long-press action row (Hide · Uninstall · Cancel), anchored to [app]'s tile. */
  data class AppMenu(val app: AppInfo) : Overlay

  /** The full-screen all-apps settings list (opened by the header gear). */
  data object Settings : Overlay
}

sealed interface AppsUiState {
  data object Loading : AppsUiState

  /**
   * @param apps the home grid: favorites only, precomputed so composables stay dumb.
   * @param allApps every installed app with its favorite flag (the settings list's input).
   */
  data class Ready(
    val apps: List<AppInfo>,
    val allApps: List<AppEntry> = emptyList(),
  ) : AppsUiState
}

/** One installed app plus whether it is shown on the home grid. */
data class AppEntry(val app: AppInfo, val isFavorite: Boolean)

/** e.g. time = "9:41", amPm = "PM", date = "Sat · Jul 4". */
data class ClockUiState(val time: String, val amPm: String, val date: String)

sealed interface WeatherUiState {
  /** Loading, or failed with no cache — the header shows just the date. */
  data object Hidden : WeatherUiState

  /** e.g. tempText = "72°", condition = "Sunny" ("" hides the condition word). */
  data class Ready(val tempText: String, val condition: String) : WeatherUiState
}

sealed interface LauncherAction {
  data class AppClicked(val app: AppInfo) : LauncherAction

  data class AppLongPressed(val app: AppInfo) : LauncherAction

  data object MenuDismissed : LauncherAction

  data class HideApp(val app: AppInfo) : LauncherAction

  data class UninstallApp(val app: AppInfo) : LauncherAction

  data object OpenSettings : LauncherAction

  data object CloseSettings : LauncherAction

  data class ToggleFavorite(val packageName: String) : LauncherAction

  data object ScreenResumed : LauncherAction

  data object LaunchFailed : LauncherAction
}

sealed interface LauncherEvent {
  data class LaunchApp(val componentName: ComponentName) : LauncherEvent

  /** Fires the platform ACTION_DELETE flow — the system owns the confirmation dialog. */
  data class RequestUninstall(val packageName: String) : LauncherEvent
}
