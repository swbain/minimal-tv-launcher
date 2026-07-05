package com.pavlovsfrog.minimaltvlauncher.weather

import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Current weather, ready for display. */
data class Weather(
  val temperature: Int,
  val unit: TempUnit,
  val code: Int,
  val isDay: Boolean,
  val city: String?,
)

/** The ViewModel's seam onto the weather pipeline — fake-able in tests. */
fun interface WeatherProvider {
  suspend fun currentWeather(): Result<Weather>
}

/**
 * Orchestrates GeoJS geolocation → Open-Meteo forecast.
 *
 * Geolocation is cached for the process lifetime; the forecast for [TTL_MILLIS]. Concurrent
 * callers are deduped by the [Mutex]. Any failure falls back to the last cached success when
 * one exists, so transient outages never blank an already-populated header.
 */
class WeatherRepository(
  private val fetcher: JsonFetcher,
  private val clock: () -> Long = System::currentTimeMillis,
) : WeatherProvider {

  private val mutex = Mutex()
  private var location: GeoLocation? = null
  private var cached: Weather? = null
  private var cachedAtMillis = 0L

  override suspend fun currentWeather(): Result<Weather> = mutex.withLock {
    val now = clock()
    cached?.let { fresh -> if (now - cachedAtMillis < TTL_MILLIS) return Result.success(fresh) }

    try {
      val location = this.location
        ?: GeoJsApi.parse(fetcher.fetch(GeoJsApi.URL)).also { this.location = it }
      val unit =
        if (location.countryCode?.uppercase() in FAHRENHEIT_COUNTRIES) TempUnit.Fahrenheit
        else TempUnit.Celsius
      val conditions =
        OpenMeteoApi.parse(fetcher.fetch(OpenMeteoApi.url(location.latitude, location.longitude, unit)))

      val weather = Weather(
        temperature = conditions.temperature.roundToInt(),
        unit = unit,
        code = conditions.weatherCode,
        isDay = conditions.isDay,
        city = location.city,
      )
      cached = weather
      cachedAtMillis = now
      Result.success(weather)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      cached?.let { Result.success(it) } ?: Result.failure(e)
    }
  }

  private companion object {
    const val TTL_MILLIS = 30 * 60 * 1000L

    /** The three countries still on Fahrenheit. */
    val FAHRENHEIT_COUNTRIES = setOf("US", "LR", "MM")
  }
}
