package com.pavlovsfrog.minimaltvlauncher.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.pavlovsfrog.minimaltvlauncher.db.LauncherDatabase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test

/** SQLDelight on an in-memory JDBC driver — no emulator, no Robolectric. */
class VisibilityRepositoryTest {

  private fun createRepository(): SqlDelightVisibilityRepository {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    LauncherDatabase.Schema.create(driver)
    return SqlDelightVisibilityRepository(LauncherDatabase(driver))
  }

  @Test
  fun `hide then show round-trips back to visible`() = runTest {
    val repository = createRepository()

    repository.setHidden("com.pavlovsfrog.cinema", hidden = true)
    assertEquals(setOf("com.pavlovsfrog.cinema"), repository.hiddenPackages().first())

    repository.setHidden("com.pavlovsfrog.cinema", hidden = false)
    assertEquals(emptySet<String>(), repository.hiddenPackages().first())
  }

  @Test
  fun `hide is idempotent`() = runTest {
    val repository = createRepository()

    repository.setHidden("com.pavlovsfrog.cinema", hidden = true)
    repository.setHidden("com.pavlovsfrog.cinema", hidden = true)

    assertEquals(setOf("com.pavlovsfrog.cinema"), repository.hiddenPackages().first())
  }

  @Test
  fun `prune drops only uninstalled packages`() = runTest {
    val repository = createRepository()
    repository.setHidden("com.pavlovsfrog.kept", hidden = true)
    repository.setHidden("com.pavlovsfrog.gone", hidden = true)

    repository.prune(installed = listOf("com.pavlovsfrog.kept", "com.pavlovsfrog.other"))

    assertEquals(setOf("com.pavlovsfrog.kept"), repository.hiddenPackages().first())
  }

  @Test
  fun `active collector sees every write`() = runBlocking {
    val repository = createRepository()
    val emissions = Channel<Set<String>>(Channel.UNLIMITED)
    val collector = launch { repository.hiddenPackages().collect { emissions.send(it) } }

    try {
      withTimeout(5_000) {
        assertEquals(emptySet<String>(), emissions.receive())

        repository.setHidden("com.pavlovsfrog.cinema", hidden = true)
        assertEquals(setOf("com.pavlovsfrog.cinema"), emissions.receive())

        repository.setHidden("com.pavlovsfrog.cinema", hidden = false)
        assertEquals(emptySet<String>(), emissions.receive())
      }
    } finally {
      collector.cancel()
    }
  }
}
