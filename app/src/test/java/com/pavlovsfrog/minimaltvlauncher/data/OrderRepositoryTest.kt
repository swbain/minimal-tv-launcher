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
class OrderRepositoryTest {

  private fun createRepository(): SqlDelightOrderRepository {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    LauncherDatabase.Schema.create(driver)
    return SqlDelightOrderRepository(LauncherDatabase(driver))
  }

  @Test
  fun `setOrder round-trips the exact sequence`() = runTest {
    val repository = createRepository()
    val order = listOf("com.b", "com.a", "com.c")

    repository.setOrder(order)

    assertEquals(order, repository.order().first())
  }

  @Test
  fun `setOrder replaces the previous order wholesale`() = runTest {
    val repository = createRepository()
    repository.setOrder(listOf("com.a", "com.b", "com.c"))

    repository.setOrder(listOf("com.c", "com.a"))

    assertEquals(listOf("com.c", "com.a"), repository.order().first())
  }

  @Test
  fun `empty order is the default`() = runTest {
    val repository = createRepository()

    assertEquals(emptyList<String>(), repository.order().first())
  }

  @Test
  fun `prune drops only uninstalled packages, preserving order`() = runTest {
    val repository = createRepository()
    repository.setOrder(listOf("com.a", "com.gone", "com.b"))

    repository.prune(installed = listOf("com.a", "com.b"))

    assertEquals(listOf("com.a", "com.b"), repository.order().first())
  }

  @Test
  fun `active collector sees every write`() = runBlocking {
    val repository = createRepository()
    val emissions = Channel<List<String>>(Channel.UNLIMITED)
    val collector = launch { repository.order().collect { emissions.send(it) } }

    try {
      withTimeout(5_000) {
        assertEquals(emptyList<String>(), emissions.receive())

        repository.setOrder(listOf("com.a", "com.b"))
        assertEquals(listOf("com.a", "com.b"), emissions.receive())

        repository.setOrder(listOf("com.b"))
        assertEquals(listOf("com.b"), emissions.receive())
      }
    } finally {
      collector.cancel()
    }
  }
}
