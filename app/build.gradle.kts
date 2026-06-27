import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  id("com.google.gms.google-services")
}

val localProperties = Properties().apply {
  val localPropertiesFile = rootProject.file("local.properties")
  if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use(::load)
  }
}

fun configValue(key: String, fallback: String = ""): String {
  return providers.gradleProperty(key).orNull
    ?: localProperties.getProperty(key)
    ?: System.getenv(key)
    ?: fallback
}

fun String.asBuildConfigString(): String =
  "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
  namespace = "com.civilagesurvey.app"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.civilagesurvey.app"
    minSdk = 24
    targetSdk = 35
    versionCode = 4
    versionName = "1.4"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    buildConfigField("String", "ADMIN_EMAIL", configValue("ADMIN_EMAIL", "mdc18042007@gmail.com").asBuildConfigString())
    buildConfigField("String", "DEFAULT_SURVEYOR_ID", configValue("DEFAULT_SURVEYOR_ID", "CAS-ADMIN-001").asBuildConfigString())
    buildConfigField("String", "DEFAULT_FORM_ID", configValue("DEFAULT_FORM_ID").asBuildConfigString())
    buildConfigField("String", "FORMS_WEB_APP_URL", configValue("FORMS_WEB_APP_URL").asBuildConfigString())
    buildConfigField("String", "FORMS_SHARED_SECRET", configValue("FORMS_SHARED_SECRET").asBuildConfigString())
    buildConfigField("String", "GEMINI_API_KEY", configValue("GEMINI_API_KEY").asBuildConfigString())
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      val keystoreFile = file(keystorePath)
      if (keystoreFile.exists() && !System.getenv("STORE_PASSWORD").isNullOrBlank() && !System.getenv("KEY_PASSWORD").isNullOrBlank()) {
        storeFile = keystoreFile
        storePassword = System.getenv("STORE_PASSWORD")
        keyAlias = "upload"
        keyPassword = System.getenv("KEY_PASSWORD")
      } else {
        storeFile = file("${rootDir}/debug.keystore")
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      val debugKeystore = file("${rootDir}/debug.keystore")
      if (debugKeystore.exists()) {
    // signingConfig = signingConfigs.getByName("debugConfig")
      }
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  kotlinOptions {
    jvmTarget = "11"
    freeCompilerArgs = freeCompilerArgs + listOf("-Xskip-metadata-version-check")
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.analytics)
  implementation(libs.firebase.auth)
  implementation(libs.firebase.firestore)
  implementation(libs.firebase.storage)
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services.auth)
  implementation(libs.google.id)
  implementation(libs.play.services.auth)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.play.services)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  testImplementation(libs.androidx.compose.ui.test.manifest)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
