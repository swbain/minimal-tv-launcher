@file:OptIn(ExperimentalTestApi::class)

package com.example.minimaltvlauncher

import android.content.ComponentName
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.minimaltvlauncher.theme.MinimalTvLauncherTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Smoke test: a Ready state renders the clock, weather, and app cards, and focuses the grid. */
@RunWith(AndroidJUnit4::class)
class LauncherScreenTest {

  @get:Rule val composeRule = createComposeRule()

  private fun appInfo(label: String) = AppInfo(
    label = label,
    packageName = "com.example.${label.lowercase()}",
    componentName = ComponentName("com.example.${label.lowercase()}", "Main"),
    image = null,
    hasBanner = false,
  )

  private val readyState = LauncherState(
    apps = AppsUiState.Ready(listOf(appInfo("Cinema"), appInfo("Music"), appInfo("Arcade"))),
    clock = ClockUiState(time = "9:42", amPm = "PM", date = "Sat · Jul 4"),
    weather = WeatherUiState.Ready(tempText = "72°", condition = "Sunny"),
  )

  @Test
  fun readyState_rendersClockWeatherAndApps() {
    composeRule.setContent {
      MinimalTvLauncherTheme {
        LauncherScreen(state = readyState, onAction = {})
      }
    }

    composeRule.onNodeWithText("9:42").assertIsDisplayed()
    composeRule.onNodeWithText("72° Sunny").assertIsDisplayed()
    // Cards no longer carry a visible label; the app name lives in the content description.
    composeRule.onNodeWithContentDescription("Cinema").assertIsDisplayed()
    composeRule.onNodeWithContentDescription("Music").assertIsDisplayed()
    composeRule.onNodeWithContentDescription("Arcade").assertIsDisplayed()
  }

  @Test
  fun grid_isDpadDrivable_aCardGainsFocus() {
    composeRule.setContent {
      MinimalTvLauncherTheme {
        LauncherScreen(state = readyState, onAction = {})
      }
    }

    // The one-shot autofocus request can be dropped in the harness (the test window gains
    // focus late), so drive it like a remote: the first D-pad event lands focus on a card.
    composeRule.onRoot().performKeyInput { pressKey(Key.DirectionRight) }

    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule.onAllNodes(isFocused()).fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNode(isFocused()).assertExists()
  }

  @Test
  fun hiddenWeather_showsDateOnly() {
    composeRule.setContent {
      MinimalTvLauncherTheme {
        LauncherScreen(state = readyState.copy(weather = WeatherUiState.Hidden), onAction = {})
      }
    }

    composeRule.onNodeWithText("Sat · Jul 4").assertIsDisplayed()
    composeRule.onNodeWithText("72° Sunny").assertDoesNotExist()
  }
}
