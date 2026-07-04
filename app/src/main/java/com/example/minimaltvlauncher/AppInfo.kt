package com.example.minimaltvlauncher

import android.content.ComponentName
import android.graphics.Bitmap

/**
 * A single launchable app shown on the home screen.
 *
 * @param label the user-visible name of the app.
 * @param packageName the app's package, used for de-duplication.
 * @param componentName the concrete launcher activity to start.
 * @param image the app's banner (16:9) or, if it has none, its square icon.
 * @param hasBanner true when [image] is a wide TV banner, false when it is a square icon.
 */
data class AppInfo(
  val label: String,
  val packageName: String,
  val componentName: ComponentName,
  val image: Bitmap?,
  val hasBanner: Boolean,
)
