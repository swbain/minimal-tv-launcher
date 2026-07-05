package com.example.minimaltvlauncher.weather

/** WMO weather-interpretation codes (as used by Open-Meteo) → display labels. */
object WmoWeatherCode {

  /** Returns a short condition label, or null for unknown codes (show temperature only). */
  fun label(code: Int, isDay: Boolean): String? = when (code) {
    0 -> if (isDay) "Sunny" else "Clear"
    1 -> if (isDay) "Mostly Sunny" else "Mostly Clear"
    2 -> "Partly Cloudy"
    3 -> "Overcast"
    45, 48 -> "Foggy"
    51, 53, 55, 56, 57 -> "Drizzle"
    61, 63, 65, 66, 67 -> "Rain"
    71, 73, 75, 77 -> "Snow"
    80, 81, 82 -> "Showers"
    85, 86 -> "Snow Showers"
    95, 96, 99 -> "Thunderstorm"
    else -> null
  }
}
