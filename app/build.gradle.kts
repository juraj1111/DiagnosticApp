import java.util.Properties

val localPropsFile = rootProject.file("local.properties")
val localProps = Properties()

if (localPropsFile.exists()) {
    localPropsFile.inputStream().use { localProps.load(it) }
}

val username = localProps.getProperty("USERNAME") ?: "default_user"
val password = localProps.getProperty("PASSWORD") ?: "default_pass"
val url = localProps.getProperty("URL") ?: "https://example.com"

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    kotlin("plugin.serialization") version "2.0.20" // use the same version as your Kotlin
}

//val username = localProps["USERNAME"]?.toString() ?: "default_user"
//val username = System.getenv("USERNAME") ?: "default_user"
//val password = System.getenv("PASSWORD") ?: "default_pass"
//val url = System.getenv("URL") ?: "https://example.com"

android {
    namespace = "com.example.diagnosticapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.diagnosticapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "USERNAME", "\"$username\"")
        buildConfigField("String", "PASSWORD", "\"$password\"")
        buildConfigField("String", "URL", "\"$url\"")
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }


    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }


}

dependencies {

    implementation("com.github.thegrizzlylabs:sardine-android:0.7")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.runtime.saved.instance.state)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

