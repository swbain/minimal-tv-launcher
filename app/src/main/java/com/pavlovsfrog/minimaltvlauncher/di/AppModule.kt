package com.pavlovsfrog.minimaltvlauncher.di

import com.pavlovsfrog.minimaltvlauncher.AppsLoader
import com.pavlovsfrog.minimaltvlauncher.AppsRepository
import com.pavlovsfrog.minimaltvlauncher.TimeSource
import com.pavlovsfrog.minimaltvlauncher.weather.JsonFetcher
import com.pavlovsfrog.minimaltvlauncher.weather.OkHttpJsonFetcher
import com.pavlovsfrog.minimaltvlauncher.weather.WeatherProvider
import com.pavlovsfrog.minimaltvlauncher.weather.WeatherRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

  @Binds @Singleton abstract fun bindAppsLoader(impl: AppsRepository): AppsLoader

  @Binds @Singleton abstract fun bindJsonFetcher(impl: OkHttpJsonFetcher): JsonFetcher

  companion object {
    // Provided rather than @Binds-ed so WeatherRepository keeps its defaulted clock lambda.
    @Provides
    @Singleton
    fun provideWeatherProvider(fetcher: JsonFetcher): WeatherProvider = WeatherRepository(fetcher)

    @Provides fun provideTimeSource(): TimeSource = TimeSource(System::currentTimeMillis)
  }
}
