package com.pavlovsfrog.minimaltvlauncher

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ClockFormatterTest {

  private val zone = TimeZone.getTimeZone("America/Chicago")
  private lateinit var defaultZone: TimeZone

  @Before
  fun pinTimeZone() {
    defaultZone = TimeZone.getDefault()
    TimeZone.setDefault(zone)
  }

  @After
  fun restoreTimeZone() {
    TimeZone.setDefault(defaultZone)
  }

  private fun epochOf(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
    Calendar.getInstance(zone).run {
      clear()
      set(year, month - 1, day, hour, minute, 0)
      timeInMillis
    }

  @Test
  fun `afternoon time splits into time and am-pm`() {
    val clock = ClockFormatter.format(epochOf(2026, 7, 4, 21, 41), Locale.US)

    assertEquals("9:41", clock.time)
    assertEquals("PM", clock.amPm)
  }

  @Test
  fun `date reads EEE dot MMM d`() {
    val clock = ClockFormatter.format(epochOf(2026, 7, 4, 21, 41), Locale.US)

    assertEquals("Sat · Jul 4", clock.date)
  }

  @Test
  fun `noon renders as 12 PM`() {
    val clock = ClockFormatter.format(epochOf(2026, 7, 4, 12, 0), Locale.US)

    assertEquals("12:00", clock.time)
    assertEquals("PM", clock.amPm)
  }

  @Test
  fun `midnight renders as 12 AM`() {
    val clock = ClockFormatter.format(epochOf(2026, 7, 4, 0, 5), Locale.US)

    assertEquals("12:05", clock.time)
    assertEquals("AM", clock.amPm)
  }

  @Test
  fun `minutes zero-pad but hours do not`() {
    val clock = ClockFormatter.format(epochOf(2026, 7, 4, 9, 5), Locale.US)

    assertEquals("9:05", clock.time)
  }
}
