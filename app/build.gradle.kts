import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.time.Duration
import java.util.Base64

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

// Materialize app/google-services.json from CI environment variables or Base64 project config
// when AI Studio's GitHub export omits app/google-services.json.
val googleServicesJsonFile = project.file("google-services.json")
val defaultGoogleServicesB64 =
  "ewogICJwcm9qZWN0X2luZm8iOiB7CiAgICAicHJvamVjdF9udW1iZXIiOiAiNjI5NTg2NzQ5NTQ3IiwKICAgICJwcm9qZWN0X2lkIjogImdlbi1sYW5nLWNsaWVudC0wMDQ4MDU5OTQ0IiwKICAgICJzdG9yYWdlX2J1Y2tldCI6ICJnZW4tbGFuZy1jbGllbnQtMDA0ODA1OTk0NC5maXJlYmFzZXN0b3JhZ2UuYXBwIgogIH0sCiAgImNsaWVudCI6IFsKICAgIHsKICAgICAgImNsaWVudF9pbmZvIjogewogICAgICAgICJtb2JpbGVzZGtfYXBwX2lkIjogIjE6NjI5NTg2NzQ5NTQ3OmFuZHJvaWQ6NzAwMGQ0MjdmOGVmOTUyYjI5MjkyZSIsCiAgICAgICAgImFuZHJvaWRfY2xpZW50X2luZm8iOiB7CiAgICAgICAgICAicGFja2FnZV9uYW1lIjogImNvbS5haXN0dWRpby5xYXdtaWtvcm1vLmtoZWRtdCIKICAgICAgICB9CiAgICAgIH0sCiAgICAgICJvYXV0aF9jbGllbnQiOiBbCiAgICAgICAgewogICAgICAgICAgImNsaWVudF9pZCI6ICI2Mjk1ODY3NDk1NDctZjAydmVsMHZub3B2cmhwanNlcjRwbzN1aWsyNnIwcjEuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLAogICAgICAgICAgImNsaWVudF90eXBlIjogMSwKICAgICAgICAgICJhbmRyb2lkX2luZm8iOiB7CiAgICAgICAgICAgICJwYWNrYWdlX25hbWUiOiAiY29tLmFpc3R1ZGlvLnFhd21pa29ybW8ua2hlZG10IiwKICAgICAgICAgICAgImNlcnRpZmljYXRlX2hhc2giOiAiYmFhNzk3YTBhNmYwMmI3MTExYmFkYTQ3OTVhNDUzMWJiZDRjZTEyMyIKICAgICAgICAgIH0KICAgICAgICB9LAogICAgICAgIHsKICAgICAgICAgICJjbGllbnRfaWQiOiAiNjI5NTg2NzQ5NTQ3LWlhNmNkOTUwZDgxbmlvdGV1YW1ncmxrdG9mM3Brc3FxLmFwcHMuZ29vZ2xldXNlcmNvbnRlbnQuY29tIiwKICAgICAgICAgICJjbGllbnRfdHlwZSI6IDEsCiAgICAgICAgICAiYW5kcm9pZF9pbmZvIjogewogICAgICAgICAgICAicGFja2FnZV9uYW1lIjogImNvbS5haXN0dWRpby5xYXdtaWtvcm1vLmtoZWRtdCIsCiAgICAgICAgICAgICJjZXJ0aWZpY2F0ZV9oYXNoIjogIjUwYTk5YTA5MGRjMTgxYTQxOGJiNDNiYjRkMWE1NmYwNTkxZmJjNDYiCiAgICAgICAgICB9CiAgICAgICAgfSwKICAgICAgICB7CiAgICAgICAgICAiY2xpZW50X2lkIjogIjYyOTU4Njc0OTU0Ny11YmdvOTl1OGM3Mm42M3QxcHVqdDdkbG9mNTFtcW41ZS5hcHBzLmdvb2dsZXVzZXJjb250ZW50LmNvbSIsCiAgICAgICAgICAiY2xpZW50X3R5cGUiOiAzCiAgICAgICAgfQogICAgICBdLAogICAgICAiYXBpX2tleSI6IFsKICAgICAgICB7CiAgICAgICAgICAiY3VycmVudF9rZXkiOiAiQUl6YVN5QTc0MWQzbkZQamE1R2lwZ1E0b0EwU1duclN5c2ZBVlRrIgogICAgICAgIH0KICAgICAgXSwKICAgICAgInNlcnZpY2VzIjogewogICAgICAgICJhcHBpbnZpdGVfc2VydmljZSI6IHsKICAgICAgICAgICJvdGhlcl9wbGF0Zm9ybV9vYXV0aF9jbGllbnQiOiBbCiAgICAgICAgICAgIHsKICAgICAgICAgICAgICAiY2xpZW50X2lkIjogIjYyOTU4Njc0OTU0Ny11YmdvOTl1OGM3Mm42M3QxcHVqdDdkbG9mNTFtcW41ZS5hcHBzLmdvb2dsZXVzZXJjb250ZW50LmNvbSIsCiAgICAgICAgICAgICAgImNsaWVudF90eXBlIjogMwogICAgICAgICAgICB9CiAgICAgICAgICBdCiAgICAgICAgfQogICAgICB9CiAgICB9LAogICAgewogICAgICAiY2xpZW50X2luZm8iOiB7CiAgICAgICAgIm1vYmlsZXNka19hcHBfaWQiOiAiMTo2Mjk1ODY3NDk1NDc6YW5kcm9pZDphNDE0NDRmODg0YTc2ZmRlMjkyOTJlIiwKICAgICAgICAiYW5kcm9pZF9jbGllbnRfaW5mbyI6IHsKICAgICAgICAgICJwYWNrYWdlX25hbWUiOiAiY29tLmV4YW1wbGUiCiAgICAgICAgfQogICAgICB9LAogICAgICAib2F1dGhfY2xpZW50IjogWwogICAgICAgIHsKICAgICAgICAgICJjbGllbnRfaWQiOiAiNjI5NTg2NzQ5NTQ3LTQybWY4NnZqcmZpdDl1cW41cDBwdjMxODNsOHVrbmgwLmFwcHMuZ29vZ2xldXNlcmNvbnRlbnQuY29tIiwKICAgICAgICAgICJjbGllbnRfdHlwZSI6IDEsCiAgICAgICAgICAiYW5kcm9pZF9pbmZvIjogewogICAgICAgICAgICAicGFja2FnZV9uYW1lIjogImNvbS5leGFtcGxlIiwKICAgICAgICAgICAgImNlcnRpZmljYXRlX2hhc2giOiAiNWFkY2I5ZDg5NDhhOGJmNzY0ZTVlOTc1MjdiYjA4MmZkMjVlOTRiZSIKICAgICAgICAgIH0KICAgICAgICB9LAogICAgICAgIHsKICAgICAgICAgICJjbGllbnRfaWQiOiAiNjI5NTg2NzQ5NTQ3LXViZ285OXU4YzcybjYzdDFwdWp0N2Rsb2Y1MW1xbjVlLmFwcHMuZ29vZ2xldXNlcmNvbnRlbnQuY29tIiwKICAgICAgICAgICJjbGllbnRfdHlwZSI6IDMKICAgICAgICB9CiAgICAgIF0sCiAgICAgICJhcGlfa2V5IjogWwogICAgICAgIHsKICAgICAgICAgICJjdXJyZW50X2tleSI6ICJBSXphU3lBNzQxZDNuRlBqYTVHaXBnUTRvQTBTV25yU3lzZkFWVGsiCiAgICAgICAgfQogICAgICBdLAogICAgICAic2VydmljZXMiOiB7CiAgICAgICAgImFwcGludml0ZV9zZXJ2aWNlIjogewogICAgICAgICAgIm90aGVyX3BsYXRmb3JtX29hdXRoX2NsaWVudCI6IFsKICAgICAgICAgICAgewogICAgICAgICAgICAgICJjbGllbnRfaWQiOiAiNjI5NTg2NzQ5NTQ3LXViZ285OXU4YzcybjYzdDFwdWp0N2Rsb2Y1MW1xbjVlLmFwcHMuZ29vZ2xldXNlcmNvbnRlbnQuY29tIiwKICAgICAgICAgICAgICAiY2xpZW50X3R5cGUiOiAzCiAgICAgICAgICAgIH0KICAgICAgICAgIF0KICAgICAgICB9CiAgICAgIH0KICAgIH0KICBdLAogICJjb25maWd1cmF0aW9uX3ZlcnNpb24iOiAiMSIKfQ=="

fun isValidGoogleServicesContent(text: String): Boolean {
  return text.contains("\"com.aistudio.qawmikormo.khedmt\"") &&
    text.contains("\"gen-lang-client-0048059944\"") &&
    text.contains("\"baa797a0a6f02b7111bada4795a4531bbd4ce123\"") &&
    Regex("\"client_type\"\\s*:\\s*3").containsMatchIn(text)
}

val hasValidExistingGoogleServices = googleServicesJsonFile.exists() &&
  runCatching { isValidGoogleServicesContent(googleServicesJsonFile.readText()) }.getOrDefault(false)

if (!hasValidExistingGoogleServices) {
  val envGoogleServices = System.getenv("GOOGLE_SERVICES_JSON")?.trim()
    ?: System.getenv("GOOGLE_SERVICES_JSON_BASE64")?.trim()
  val candidateFromEnv = if (!envGoogleServices.isNullOrEmpty()) {
    if (envGoogleServices.startsWith("{")) {
      envGoogleServices
    } else {
      runCatching {
        String(Base64.getDecoder().decode(envGoogleServices.replace("\\s".toRegex(), "")), Charsets.UTF_8)
      }.getOrNull() ?: ""
    }
  } else {
    ""
  }

  val resolvedJson = if (isValidGoogleServicesContent(candidateFromEnv)) {
    candidateFromEnv
  } else {
    String(Base64.getDecoder().decode(defaultGoogleServicesB64), Charsets.UTF_8)
  }
  googleServicesJsonFile.writeText(resolvedJson)
}

// Verify whether app/google-services.json will cause Google Services Gradle Plugin to generate R.string.default_web_client_id.
val hasGoogleServicesWebClientId: Boolean = googleServicesJsonFile.exists() && runCatching {
  isValidGoogleServicesContent(googleServicesJsonFile.readText())
}.getOrDefault(false)

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.qawmikormo.khedmt"
    minSdk = 24
    targetSdk = 36
    versionCode = (project.findProperty("versionCode")?.toString()?.toIntOrNull()) ?: 1
    versionName = (project.findProperty("versionName")?.toString()?.takeIf { it.isNotBlank() }) ?: "1.0"

    if (!hasGoogleServicesWebClientId) {
      val defaultClientId = String(
        Base64.getDecoder().decode("NjI5NTg2NzQ5NTQ3LXViZ285OXU4YzcybjYzdDFwdWp0N2Rsb2Y1MW1xbjVlLmFwcHMuZ29vZ2xldXNlcmNvbnRlbnQuY29t"),
        Charsets.UTF_8
      )
      val fallbackWebClientId = System.getenv("DEFAULT_WEB_CLIENT_ID")?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: defaultClientId
      resValue("string", "default_web_client_id", fallbackWebClientId)
    }

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  // Do not remove or modify these signingConfigs. They are necessary for building, installing,
  // and updating Android apps in AI Studio.
  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
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
      // Do not remove or modify this signingConfig assignment. It is necessary for Android apps in
      // AI Studio.
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      // Do not remove or modify this signingConfig assignment. It is necessary for Android apps in
      // AI Studio.
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
    resValues = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

tasks.withType<Test> {
  failFast = true
  timeout.set(Duration.ofMinutes(2))
  testLogging {
    events("passed", "failed", "skipped")
  }
}

// Synchronize secrets injected via AI Studio Secrets panel or CI environment variables into the gitignored .env file
// using Gradle providers so Configuration Cache tracks both environment variables and .env file presence on disk.
val envExampleFile = rootProject.file(".env.example")
val dotEnvFile = rootProject.file(".env")
val existingDotEnvContent = providers.fileContents(rootProject.layout.projectDirectory.file(".env")).asText.orNull
if (envExampleFile.exists()) {
  val resolvedMap = linkedMapOf<String, String>()
  val defaultMap = linkedMapOf<String, String>()

  envExampleFile.readLines().forEach { line ->
    val trimmed = line.trim()
    if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
      val k = trimmed.substringBefore("=").trim()
      val v = trimmed.substringAfter("=").trim()
      defaultMap[k] = v
      resolvedMap[k] = v
    }
  }

  if (!existingDotEnvContent.isNullOrEmpty()) {
    existingDotEnvContent.lineSequence().forEach { line ->
      val trimmed = line.trim()
      if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
        val k = trimmed.substringBefore("=").trim()
        val v = trimmed.substringAfter("=").trim()
        if (v.isNotEmpty() && v != defaultMap[k]) {
          resolvedMap[k] = v
        }
      }
    }
  }

  defaultMap.keys.forEach { key ->
    val fromProvider = providers.environmentVariable(key).orNull?.trim()
    val sysVal = if (!fromProvider.isNullOrEmpty()) fromProvider else System.getenv(key)?.trim()
    if (!sysVal.isNullOrEmpty() && sysVal != defaultMap[key]) {
      resolvedMap[key] = sysVal
    }
  }

  val desiredContent = resolvedMap.entries.joinToString("\n") { (k, v) -> "$k=$v" } + "\n"
  if (existingDotEnvContent == null || existingDotEnvContent != desiredContent) {
    dotEnvFile.writeText(desiredContent)
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
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
  // implementation(libs.androidx.navigation.compose)
  // implementation(libs.androidx.room.ktx)
  // implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  // implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  // Uncomment to use Firestore:
  implementation(libs.firebase.firestore)
  implementation(libs.firebase.storage)
  implementation(libs.firebase.messaging)

  // Uncomment ALL FOUR of the following dependencies together to use Firebase Auth and Google
  // Sign-In via Credential Manager:
  implementation(libs.firebase.auth)
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services)
  implementation(libs.googleid)
  implementation(libs.play.services.auth)
  // implementation(libs.firebase.appcheck.recaptcha)
  // implementation(libs.firebase.appcheck.debug)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  // implementation(libs.logging.interceptor)
  // implementation(libs.moshi.kotlin)
  // implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  // implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  // "ksp"(libs.androidx.room.compiler)
  // "ksp"(libs.moshi.kotlin.codegen)
}
