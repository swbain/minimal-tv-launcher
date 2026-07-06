package com.pavlovsfrog.minimaltvlauncher.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.pavlovsfrog.minimaltvlauncher.db.LauncherDatabase
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext

/** The ViewModel's seam onto "the user's saved home-grid order" — fake-able in tests. */
interface OrderRepository {
  /** Emits the saved package order (empty = no custom order yet), then again after every write. */
  fun order(): Flow<List<String>>

  /** Replaces the whole saved order with [packageNames], numbered 0..n by list position. */
  suspend fun setOrder(packageNames: List<String>)

  /** Drops rows for packages no longer installed, so the saved order stays honest. */
  suspend fun prune(installed: Collection<String>)
}

class SqlDelightOrderRepository @Inject constructor(
  database: LauncherDatabase,
) : OrderRepository {

  private val queries = database.appOrderQueries

  override fun order(): Flow<List<String>> =
    queries.selectAll()
      .asFlow()
      .mapToList(Dispatchers.IO)
      .distinctUntilChanged()

  override suspend fun setOrder(packageNames: List<String>) {
    withContext(Dispatchers.IO) {
      queries.transaction {
        queries.deleteAll()
        packageNames.forEachIndexed { index, packageName ->
          queries.upsert(packageName, index.toLong())
        }
      }
    }
  }

  override suspend fun prune(installed: Collection<String>) {
    withContext(Dispatchers.IO) { queries.prune(installed) }
  }
}
