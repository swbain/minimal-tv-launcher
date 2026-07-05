package com.pavlovsfrog.minimaltvlauncher.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.pavlovsfrog.minimaltvlauncher.db.LauncherDatabase
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** The ViewModel's seam onto "which packages are hidden from home" — fake-able in tests. */
interface VisibilityRepository {
  /** Emits the current hidden set, then again after every write. */
  fun hiddenPackages(): Flow<Set<String>>

  suspend fun setHidden(packageName: String, hidden: Boolean)

  /** Drops rows for packages no longer installed, so uninstall → reinstall lands visible. */
  suspend fun prune(installed: Collection<String>)
}

class SqlDelightVisibilityRepository @Inject constructor(
  database: LauncherDatabase,
) : VisibilityRepository {

  private val queries = database.hiddenAppQueries

  override fun hiddenPackages(): Flow<Set<String>> =
    queries.selectAll()
      .asFlow()
      .mapToList(Dispatchers.IO)
      .map { it.toSet() }
      .distinctUntilChanged()

  override suspend fun setHidden(packageName: String, hidden: Boolean) {
    withContext(Dispatchers.IO) {
      if (hidden) queries.hide(packageName) else queries.show(packageName)
    }
  }

  override suspend fun prune(installed: Collection<String>) {
    withContext(Dispatchers.IO) { queries.prune(installed) }
  }
}
