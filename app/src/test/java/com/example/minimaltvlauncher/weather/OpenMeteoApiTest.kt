package com.example.minimaltvlauncher.weather

import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Fixture matching the response shape captured during planning (2026-07-04). */
internal const val OPEN_METEO_FIXTURE = """
  {"current":{"time":"2026-07-04T18:15","temperature_2m":72.1,"weather_code":0,"is_day":1},
   "current_units":{"temperature_2m":"°F"}}
"""

class OpenMeteoApiTest {

  private val defaultLocale = Locale.getDefault()

  @After
  fun restoreLocale() {
    Locale.setDefault(defaultLocale)
  }

  @Test
  fun `builds forecast url with all current fields`() {
    val url = OpenMeteoApi.url(latitude = 36.1671, longitude = -86.7861, unit = TempUnit.Fahrenheit)

    assertEquals(
      "https://api.open-meteo.com/v1/forecast?latitude=36.1671&longitude=-86.7861" +
        "&current=temperature_2m,weather_code,is_day&temperature_unit=fahrenheit&timezone=auto",
      url,
    )
  }

  @Test
  fun `celsius unit lands in the query`() {
    val url = OpenMeteoApi.url(latitude = 52.52, longitude = 13.405, unit = TempUnit.Celsius)

    assertTrue(url.contains("temperature_unit=celsius"))
  }

  @Test
  fun `coordinates use dot decimals even under a comma-decimal locale`() {
    Locale.setDefault(Locale.GERMANY)

    val url = OpenMeteoApi.url(latitude = 52.52, longitude = 13.405, unit = TempUnit.Celsius)

    assertTrue(url.contains("latitude=52.5200"))
    assertTrue(url.contains("longitude=13.4050"))
    assertFalse(url.contains("52,5200"))
  }

  @Test
  fun `parses current conditions from fixture`() {
    val conditions = OpenMeteoApi.parse(OPEN_METEO_FIXTURE)

    assertEquals(72.1, conditions.temperature, 1e-9)
    assertEquals(0, conditions.weatherCode)
    assertTrue(conditions.isDay)
  }

  @Test
  fun `is_day zero parses as night`() {
    val conditions = OpenMeteoApi.parse(
      """{"current":{"temperature_2m":15.0,"weather_code":3,"is_day":0}}"""
    )

    assertFalse(conditions.isDay)
  }
}
