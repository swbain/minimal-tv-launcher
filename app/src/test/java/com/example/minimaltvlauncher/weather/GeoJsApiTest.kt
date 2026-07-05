package com.example.minimaltvlauncher.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

/** Fixture captured from the live GeoJS endpoint during planning (2026-07-04). */
internal const val GEOJS_FIXTURE = """
  {"city":"Nashville","country_code":"US","latitude":"36.1671","longitude":"-86.7861",
   "timezone":"America/Chicago","organization_name":"Google Fiber Inc."}
"""

class GeoJsApiTest {

  @Test
  fun `parses live fixture with string coordinates`() {
    val location = GeoJsApi.parse(GEOJS_FIXTURE)

    assertEquals(36.1671, location.latitude, 1e-9)
    assertEquals(-86.7861, location.longitude, 1e-9)
    assertEquals("Nashville", location.city)
    assertEquals("US", location.countryCode)
  }

  @Test
  fun `missing optional fields parse as null`() {
    val location = GeoJsApi.parse("""{"latitude":"1.5","longitude":"-2.25"}""")

    assertEquals(1.5, location.latitude, 1e-9)
    assertEquals(-2.25, location.longitude, 1e-9)
    assertNull(location.city)
    assertNull(location.countryCode)
  }

  @Test
  fun `unparseable coordinates throw`() {
    assertThrows(IllegalArgumentException::class.java) {
      GeoJsApi.parse("""{"latitude":"not-a-number","longitude":"-86.7861"}""")
    }
  }

  @Test
  fun `missing coordinates throw`() {
    assertThrows(IllegalArgumentException::class.java) {
      GeoJsApi.parse("""{"city":"Nowhere"}""")
    }
  }
}
