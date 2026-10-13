plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Version is bumped automatically on every push: CI passes its run number
// (GITHUB_RUN_NUMBER) so versionCode increases monotonically and versionName
// becomes <major.minor>.<build>. Local builds fall back to build 0.
val baseVersion = "0.1"
val buildNumber = (System.getenv("GITHUB_RUN_NUMBER") ?: "0").toInt()

android {
    namespace = "com.newsstream"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.newsstream"
        minSdk = 24
        targetSdk = 34
        versionCode = maxOf(buildNumber, 1)
        versionName = "$baseVersion.$buildNumber"
    }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
