plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
  alias(libs.plugins.hilt.android)
  alias(libs.plugins.sqldelight)
}

sqldelight {
  databases {
    create("LauncherDatabase") {
      packageName.set("com.pavlovsfrog.minimaltvlauncher.db")
    }
  }
}

// SQLDelight 2.3.2 wires its generated sources into AGP 9's built-in Kotlin compilation, but
// KSP resolves its own source roots and misses them — without this, Hilt cannot resolve
// LauncherDatabase. The task.map provider carries the generate-task dependency along.
tasks.withType(com.google.devtools.ksp.gradle.KspAATask::class.java).configureEach {
  val variantName = name.removePrefix("ksp").removeSuffix("Kotlin")
  val generateName = "generate${variantName}LauncherDatabaseInterface"
  // Test variants (kspDebugUnitTestKotlin, …) have no generate task and don't need one:
  // their compilations resolve LauncherDatabase from the main classes output.
  if (generateName in tasks.names) {
    val generateTask =
      tasks.named(generateName, app.cash.sqldelight.gradle.SqlDelightTask::class.java)
    kspConfig.sourceRoots.from(generateTask.map { it.outputDirectory })
  }
}

android {
    namespace = "com.pavlovsfrog.minimaltvlauncher"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.pavlovsfrog.minimaltvlauncher"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = false
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }

    testOptions {
      // JVM unit tests build AppInfo fixtures, whose ComponentName is an android.jar stub.
      unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Dependency injection
  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)

  // Persistence (hidden-apps visibility store)
  implementation(libs.sqldelight.android.driver)
  implementation(libs.sqldelight.coroutines.extensions)
  testImplementation(libs.sqldelight.sqlite.driver)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  // Compose for TV (focus-aware, D-pad friendly Material components)
  implementation(libs.androidx.tv.material)

  // Networking + JSON (weather: GeoJS geolocation, Open-Meteo forecast)
  implementation(libs.okhttp)
  implementation(libs.kotlinx.serialization.json)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)
}
