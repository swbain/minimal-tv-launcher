package com.pavlovsfrog.minimaltvlauncher.di

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.pavlovsfrog.minimaltvlauncher.data.OrderRepository
import com.pavlovsfrog.minimaltvlauncher.data.SqlDelightOrderRepository
import com.pavlovsfrog.minimaltvlauncher.data.SqlDelightVisibilityRepository
import com.pavlovsfrog.minimaltvlauncher.data.VisibilityRepository
import com.pavlovsfrog.minimaltvlauncher.db.LauncherDatabase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseModule {

  @Binds
  @Singleton
  abstract fun bindVisibilityRepository(impl: SqlDelightVisibilityRepository): VisibilityRepository

  @Binds
  @Singleton
  abstract fun bindOrderRepository(impl: SqlDelightOrderRepository): OrderRepository

  companion object {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LauncherDatabase =
      LauncherDatabase(AndroidSqliteDriver(LauncherDatabase.Schema, context, "launcher.db"))
  }
}
