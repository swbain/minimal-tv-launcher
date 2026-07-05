package com.pavlovsfrog.minimaltvlauncher.weather

import java.util.Locale
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class TempUnit(internal val queryValue: String) {
  Fahrenheit("fahrenheit"),
  Celsius("celsius"),
}

/** What the sky is doing right now. */
data class CurrentConditions(val temperature: Double, val weatherCode: Int, val isDay: Boolean)

/** Open-Meteo current-weather forecast — keyless, free non-commercial tier. */
object OpenMeteoApi {

  /**
   * Coordinates are formatted with [Locale.US]: a German-locale TV would otherwise emit
   * `36,17` and break the query string.
   */
  fun url(latitude: Double, longitude: Double, unit: TempUnit): String =
    String.format(
      Locale.US,
      "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f" +
        "&current=temperature_2m,weather_code,is_day&temperature_unit=%s&timezone=auto",
      latitude,
      longitude,
      unit.queryValue,
    )

  @Serializable
  internal data class ForecastResponse(val current: Current)

  @Serializable
  internal data class Current(
    @SerialName("temperature_2m") val temperature: Double,
    @SerialName("weather_code") val weatherCode: Int,
    @SerialName("is_day") val isDay: Int,
  )

  fun parse(json: String): CurrentConditions {
    val current = WeatherJson.decodeFromString<ForecastResponse>(json).current
    return CurrentConditions(current.temperature, current.weatherCode, current.isDay == 1)
  }
}
