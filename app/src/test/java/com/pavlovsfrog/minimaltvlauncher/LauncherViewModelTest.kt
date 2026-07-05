package com.pavlovsfrog.minimaltvlauncher

import android.content.ComponentName
import androidx.lifecycle.viewModelScope
import com.pavlovsfrog.minimaltvlauncher.weather.TempUnit
import com.pavlovsfrog.minimaltvlauncher.weather.Weather
import com.pavlovsfrog.minimaltvlauncher.weather.WeatherProvider
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LauncherViewModelTest {

  private val dispatcher = StandardTestDispatcher()

  @Before
  fun setMainDispatcher() {
    Dispatchers.setMain(dispatcher)
  }

  @After
  fun resetMainDispatcher() {
    Dispatchers.resetMain()
  }

  private class FakeAppsLoader : AppsLoader {
    var apps: List<AppInfo> = emptyList()
    var loadCount = 0

    override suspend fun loadApps(): List<AppInfo> {
      loadCount++
      return apps
    }
  }

  private class FakeWeatherProvider : WeatherProvider {
    var result: Result<Weather> = Result.failure(IOException("no weather yet"))

    override suspend fun currentWeather(): Result<Weather> = result
  }

  private fun appInfo(label: String) = AppInfo(
    label = label,
    packageName = "com.pavlovsfrog.$label",
    componentName = ComponentName("com.pavlovsfrog.$label", "com.pavlovsfrog.$label.Main"),
    image = null,
    hasBanner = false,
  )

  private val loader = FakeAppsLoader()
  private val weather = FakeWeatherProvider()
  private val fixedNow = 1_782_255_660_000L

  private fun buildViewModel() =
    LauncherViewModel(loader, weather, timeSource = { fixedNow })

  private fun runVmTest(block: suspend TestScope.(LauncherViewModel) -> Unit) =
    runTest(dispatcher) {
      val viewModel = buildViewModel()
      try {
        block(viewModel)
      } finally {
        // The clock ticker and weather loop reschedule forever; cancel them so the
        // test scheduler can go idle and runTest can complete.
        viewModel.viewModelScope.cancel()
      }
    }

  @Test
  fun `initial load flips apps from Loading to Ready`() = runVmTest { viewModel ->
    val streamly = appInfo("streamly")
    loader.apps = listOf(streamly)

    assertEquals(AppsUiState.Loading, viewModel.state.value.apps)
    dispatcher.scheduler.runCurrent()

    val ready = viewModel.state.value.apps as AppsUiState.Ready
    assertSame(streamly, ready.apps.single())
  }

  @Test
  fun `app click emits exactly one LaunchApp event`() = runVmTest { viewModel ->
    val streamly = appInfo("streamly")
    loader.apps = listOf(streamly)
    dispatcher.scheduler.runCurrent()

    val events = mutableListOf<LauncherEvent>()
    val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.events.toList(events)
    }
    viewModel.onAction(LauncherAction.AppClicked(streamly))
    dispatcher.scheduler.runCurrent()
    collector.cancel()

    val event = events.single() as LauncherEvent.LaunchApp
    assertSame(streamly.componentName, event.componentName)
  }

  @Test
  fun `LaunchFailed reloads the apps list`() = runVmTest { viewModel ->
    loader.apps = listOf(appInfo("stale"))
    dispatcher.scheduler.runCurrent()

    val fresh = appInfo("fresh")
    loader.apps = listOf(fresh)
    viewModel.onAction(LauncherAction.LaunchFailed)
    dispatcher.scheduler.runCurrent()

    val ready = viewModel.state.value.apps as AppsUiState.Ready
    assertSame(fresh, ready.apps.single())
  }

  @Test
  fun `weather success populates Ready with temp and condition`() = runVmTest { viewModel ->
    weather.result = Result.success(
      Weather(temperature = 72, unit = TempUnit.Fahrenheit, code = 0, isDay = true, city = "Nashville")
    )

    dispatcher.scheduler.runCurrent()

    assertEquals(WeatherUiState.Ready(tempText = "72°", condition = "Sunny"), viewModel.state.value.weather)
  }

  @Test
  fun `unknown weather code yields empty condition`() = runVmTest { viewModel ->
    weather.result = Result.success(
      Weather(temperature = 21, unit = TempUnit.Celsius, code = 42, isDay = true, city = null)
    )

    dispatcher.scheduler.runCurrent()

    assertEquals(WeatherUiState.Ready(tempText = "21°", condition = ""), viewModel.state.value.weather)
  }

  @Test
  fun `weather failure leaves the header Hidden`() = runVmTest { viewModel ->
    weather.result = Result.failure(IOException("offline"))

    dispatcher.scheduler.runCurrent()

    assertEquals(WeatherUiState.Hidden, viewModel.state.value.weather)
  }

  @Test
  fun `failed fetch retries after a minute instead of waiting the full interval`() = runVmTest { viewModel ->
    weather.result = Result.failure(IOException("network still booting"))
    dispatcher.scheduler.runCurrent()
    assertEquals(WeatherUiState.Hidden, viewModel.state.value.weather)

    weather.result = Result.success(
      Weather(temperature = 72, unit = TempUnit.Fahrenheit, code = 0, isDay = true, city = null)
    )
    dispatcher.scheduler.advanceTimeBy(60_000)
    dispatcher.scheduler.runCurrent()

    assertEquals(WeatherUiState.Ready(tempText = "72°", condition = "Sunny"), viewModel.state.value.weather)
  }

  @Test
  fun `clock formats the injected instant`() = runVmTest { viewModel ->
    dispatcher.scheduler.runCurrent()

    assertEquals(ClockFormatter.format(fixedNow), viewModel.state.value.clock)
  }

  @Test
  fun `ScreenResumed reloads apps and refreshes weather`() = runVmTest { viewModel ->
    dispatcher.scheduler.runCurrent()
    assertEquals(WeatherUiState.Hidden, viewModel.state.value.weather)
    val loadsBeforeResume = loader.loadCount

    weather.result = Result.success(
      Weather(temperature = 60, unit = TempUnit.Fahrenheit, code = 3, isDay = false, city = null)
    )
    viewModel.onAction(LauncherAction.ScreenResumed)
    dispatcher.scheduler.runCurrent()

    assertEquals(loadsBeforeResume + 1, loader.loadCount)
    assertEquals(WeatherUiState.Ready(tempText = "60°", condition = "Overcast"), viewModel.state.value.weather)
  }
}
