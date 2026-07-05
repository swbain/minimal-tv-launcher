package com.pavlovsfrog.minimaltvlauncher

/**
 * The ViewModel's seam onto wall-clock time — fake-able in tests via SAM conversion.
 * A fun interface rather than `() -> Long` so Dagger has a concrete type to bind.
 */
fun interface TimeSource {
  fun nowMillis(): Long
}
