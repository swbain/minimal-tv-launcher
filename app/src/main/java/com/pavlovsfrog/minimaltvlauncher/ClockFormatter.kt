package com.pavlovsfrog.minimaltvlauncher

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pure epoch-millis → [ClockUiState] formatter. SimpleDateFormat because minSdk 24 predates
 * java.time (API 26) and desugaring for one clock string isn't worth the build complexity.
 * Formatters are built per call so time-zone changes take effect on the next tick.
 */
object ClockFormatter {

  fun format(epochMillis: Long, locale: Locale = Locale.getDefault()): ClockUiState {
    val date = Date(epochMillis)
    return ClockUiState(
      time = SimpleDateFormat("h:mm", locale).format(date),
      amPm = SimpleDateFormat("a", locale).format(date),
      date = SimpleDateFormat("EEE · MMM d", locale).format(date),
    )
  }
}
