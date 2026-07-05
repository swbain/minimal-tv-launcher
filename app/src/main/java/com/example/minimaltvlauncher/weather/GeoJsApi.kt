package com.example.minimaltvlauncher.weather

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Where the TV appears to be, resolved from its public IP. */
data class GeoLocation(
  val latitude: Double,
  val longitude: Double,
  val city: String?,
  val countryCode: String?,
)

/** GeoJS IP geolocation — keyless, HTTPS. */
object GeoJsApi {

  const val URL = "https://get.geojs.io/v1/ip/geo.json"

  // GeoJS quirk: latitude/longitude arrive as JSON strings, not numbers.
  @Serializable
  internal data class GeoJsResponse(
    val latitude: String? = null,
    val longitude: String? = null,
    val city: String? = null,
    @SerialName("country_code") val countryCode: String? = null,
  )

  /** Parses a GeoJS response, throwing if the coordinates are missing or unparseable. */
  fun parse(json: String): GeoLocation {
    val dto = WeatherJson.decodeFromString<GeoJsResponse>(json)
    val latitude = dto.latitude?.toDoubleOrNull()
    val longitude = dto.longitude?.toDoubleOrNull()
    require(latitude != null && longitude != null) {
      "GeoJS returned unparseable coordinates: latitude=${dto.latitude} longitude=${dto.longitude}"
    }
    return GeoLocation(latitude, longitude, dto.city, dto.countryCode)
  }
}
