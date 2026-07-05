package com.pavlovsfrog.minimaltvlauncher.weather

import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherRepositoryTest {

  private class FakeFetcher : JsonFetcher {
    val requests = mutableListOf<String>()
    var onFetch: (String) -> String = { url -> throw IOException("unexpected fetch: $url") }

    override suspend fun fetch(url: String): String {
      requests += url
      return onFetch(url)
    }

    fun respondNormally(forecast: String = OPEN_METEO_FIXTURE, geo: String = GEOJS_FIXTURE) {
      onFetch = { url -> if (url == GeoJsApi.URL) geo else forecast }
    }

    fun failEverything() {
      onFetch = { throw IOException("network down") }
    }
  }

  private var nowMillis = 0L
  private val fetcher = FakeFetcher()
  private val repository = WeatherRepository(fetcher, clock = { nowMillis })

  @Test
  fun `success maps geolocation and forecast into weather`() = runTest {
    fetcher.respondNormally()

    val weather = repository.currentWeather().getOrThrow()

    assertEquals(72, weather.temperature)
    assertEquals(TempUnit.Fahrenheit, weather.unit)
    assertEquals(0, weather.code)
    assertTrue(weather.isDay)
    assertEquals("Nashville", weather.city)
  }

  @Test
  fun `non-fahrenheit country requests celsius`() = runTest {
    fetcher.respondNormally(
      geo = """{"city":"Berlin","country_code":"DE","latitude":"52.52","longitude":"13.405"}""",
      forecast = """{"current":{"temperature_2m":21.6,"weather_code":2,"is_day":1}}""",
    )

    val weather = repository.currentWeather().getOrThrow()

    assertEquals(TempUnit.Celsius, weather.unit)
    assertEquals(22, weather.temperature)
    assertTrue(fetcher.requests.last().contains("temperature_unit=celsius"))
  }

  @Test
  fun `cold failure returns failure`() = runTest {
    fetcher.failEverything()

    val result = repository.currentWeather()

    assertTrue(result.isFailure)
  }

  @Test
  fun `failure after a success returns the stale cache`() = runTest {
    fetcher.respondNormally()
    val first = repository.currentWeather().getOrThrow()

    nowMillis += 31 * 60 * 1000L // past the TTL
    fetcher.failEverything()
    val second = repository.currentWeather().getOrThrow()

    assertEquals(first, second)
  }

  @Test
  fun `fresh cache short-circuits the network`() = runTest {
    fetcher.respondNormally()
    repository.currentWeather().getOrThrow()
    val requestsAfterFirst = fetcher.requests.size

    nowMillis += 29 * 60 * 1000L // still inside the TTL
    repository.currentWeather().getOrThrow()

    assertEquals(requestsAfterFirst, fetcher.requests.size)
  }

  @Test
  fun `expired cache refetches the forecast but not the geolocation`() = runTest {
    fetcher.respondNormally()
    repository.currentWeather().getOrThrow()

    nowMillis += 31 * 60 * 1000L // past the TTL
    repository.currentWeather().getOrThrow()

    assertEquals(1, fetcher.requests.count { it == GeoJsApi.URL })
    assertEquals(2, fetcher.requests.count { it.startsWith("https://api.open-meteo.com/") })
  }

  @Test
  fun `geolocation failure is retried on the next call`() = runTest {
    fetcher.failEverything()
    assertTrue(repository.currentWeather().isFailure)

    fetcher.respondNormally()
    val weather = repository.currentWeather().getOrThrow()

    assertEquals("Nashville", weather.city)
    assertEquals(2, fetcher.requests.count { it == GeoJsApi.URL })
  }
}
