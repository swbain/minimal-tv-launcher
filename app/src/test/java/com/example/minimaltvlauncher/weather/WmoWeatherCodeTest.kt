package com.example.minimaltvlauncher.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WmoWeatherCodeTest {

  @Test
  fun `clear codes are day-night aware`() {
    assertEquals("Sunny", WmoWeatherCode.label(0, isDay = true))
    assertEquals("Clear", WmoWeatherCode.label(0, isDay = false))
    assertEquals("Mostly Sunny", WmoWeatherCode.label(1, isDay = true))
    assertEquals("Mostly Clear", WmoWeatherCode.label(1, isDay = false))
  }

  @Test
  fun `cloud cover labels ignore day-night`() {
    assertEquals("Partly Cloudy", WmoWeatherCode.label(2, isDay = true))
    assertEquals("Partly Cloudy", WmoWeatherCode.label(2, isDay = false))
    assertEquals("Overcast", WmoWeatherCode.label(3, isDay = true))
  }

  @Test
  fun `every mapped code group resolves to its label`() {
    val expected = mapOf(
      listOf(45, 48) to "Foggy",
      listOf(51, 53, 55, 56, 57) to "Drizzle",
      listOf(61, 63, 65, 66, 67) to "Rain",
      listOf(71, 73, 75, 77) to "Snow",
      listOf(80, 81, 82) to "Showers",
      listOf(85, 86) to "Snow Showers",
      listOf(95, 96, 99) to "Thunderstorm",
    )
    for ((codes, label) in expected) {
      for (code in codes) {
        assertEquals("code $code", label, WmoWeatherCode.label(code, isDay = true))
        assertEquals("code $code", label, WmoWeatherCode.label(code, isDay = false))
      }
    }
  }

  @Test
  fun `unknown codes return null`() {
    assertNull(WmoWeatherCode.label(4, isDay = true))
    assertNull(WmoWeatherCode.label(-1, isDay = true))
    assertNull(WmoWeatherCode.label(100, isDay = false))
  }
}
