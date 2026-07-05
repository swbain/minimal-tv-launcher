package com.pavlovsfrog.minimaltvlauncher.weather

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

/** The single seam between the weather layer and the network; trivially fake-able in tests. */
fun interface JsonFetcher {
  /** Returns the response body for a GET of [url], throwing on any transport or HTTP error. */
  suspend fun fetch(url: String): String
}

/** Shared parser configuration for all weather DTOs. */
internal val WeatherJson = Json { ignoreUnknownKeys = true }

/** Production [JsonFetcher] backed by one shared [OkHttpClient]. */
class OkHttpJsonFetcher : JsonFetcher {

  private val client =
    OkHttpClient.Builder()
      .connectTimeout(10, TimeUnit.SECONDS)
      .readTimeout(10, TimeUnit.SECONDS)
      .build()

  override suspend fun fetch(url: String): String = withContext(Dispatchers.IO) {
    client.newCall(Request.Builder().url(url).build()).execute().use { response ->
      if (!response.isSuccessful) throw IOException("HTTP ${response.code} for $url")
      response.body.string()
    }
  }
}
