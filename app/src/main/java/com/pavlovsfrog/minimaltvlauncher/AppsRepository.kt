package com.pavlovsfrog.minimaltvlauncher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The ViewModel's seam onto "whatever can produce the launchable apps" — fake-able in tests. */
fun interface AppsLoader {
  suspend fun loadApps(): List<AppInfo>
}

/** Loads the list of launchable apps installed on the device. */
class AppsRepository(context: Context) : AppsLoader {

  private val appContext = context.applicationContext
  private val packageManager: PackageManager = appContext.packageManager
  private val ownPackage = appContext.packageName

  /**
   * Returns every launchable app, de-duplicated by package and sorted alphabetically.
   *
   * TV apps expose a `LEANBACK_LAUNCHER` activity; we also fall back to the standard mobile
   * `LAUNCHER` category so that sideloaded phone apps remain reachable. This runs off the main
   * thread because loading each app's banner/icon touches disk.
   */
  override suspend fun loadApps(): List<AppInfo> = withContext(Dispatchers.IO) {
    val resolved = LinkedHashMap<String, ResolveInfo>()

    // Prefer the TV (leanback) launcher entry for each package.
    for (info in queryLauncherActivities(Intent.CATEGORY_LEANBACK_LAUNCHER)) {
      resolved.putIfAbsent(info.activityInfo.packageName, info)
    }
    // Then add any package that only has a regular launcher entry.
    for (info in queryLauncherActivities(Intent.CATEGORY_LAUNCHER)) {
      resolved.putIfAbsent(info.activityInfo.packageName, info)
    }

    resolved.values
      .asSequence()
      .filter { it.activityInfo.packageName != ownPackage }
      .map { it.toAppInfo() }
      .sortedBy { it.label.lowercase() }
      .toList()
  }

  private fun queryLauncherActivities(category: String): List<ResolveInfo> {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
    return packageManager.queryIntentActivities(intent, 0)
  }

  private fun ResolveInfo.toAppInfo(): AppInfo {
    val activityInfo = activityInfo
    // loadBanner() returns the wide TV banner when the app provides one; icon is the fallback.
    val bannerDrawable = activityInfo.loadBanner(packageManager)
    val hasBanner = bannerDrawable != null
    val drawable = bannerDrawable ?: loadIcon(packageManager)

    return AppInfo(
      label = loadLabel(packageManager).toString(),
      packageName = activityInfo.packageName,
      componentName = ComponentName(activityInfo.packageName, activityInfo.name),
      image = drawable?.let {
        // Some drawables (e.g. solid colors) report no intrinsic size; give them one.
        val width = it.intrinsicWidth.takeIf { w -> w > 0 } ?: if (hasBanner) 320 else 108
        val height = it.intrinsicHeight.takeIf { h -> h > 0 } ?: if (hasBanner) 180 else 108
        it.toBitmap(width, height)
      },
      hasBanner = hasBanner,
    )
  }
}
