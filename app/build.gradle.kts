import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "com.example.moneychangersdatacollectionsystem"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.moneychangersdatacollectionsystem"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String", "SUPABASE_URL",
            "\"${localProperties.getProperty("SUPABASE_URL") ?: ""}\""
        )
        buildConfigField(
            "String", "SUPABASE_KEY",
            "\"${localProperties.getProperty("SUPABASE_KEY") ?: ""}\""
        )
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        // Stop failing the build on lint warnings, but keep errors as blockers
        abortOnError = true
        checkReleaseBuilds = true
    }
}

ksp {
    arg("hilt.enableCorrectErrorTypes", "true")
    // Export Room schema for migrations
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {

    // ── AndroidX Core ──────────────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // ── Compose ────────────────────────────────────────────────────────────
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // ── Browser ────────────────────────────────────────────────────────────
    implementation(libs.androidx.browser) {
        version {
            strictly("1.8.0")
        }
    }

    // ── Hilt ───────────────────────────────────────────────────────────────
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // ── Room + SQLCipher ───────────────────────────────────────────────────
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    ksp(libs.androidx.room.compiler)
    implementation(libs.sqlcipher)

    // ── Paging ─────────────────────────────────────────────────────────────
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    // ── Supabase ───────────────────────────────────────────────────────────
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.storage)

    // ── Ktor ───────────────────────────────────────────────────────────────
    implementation(libs.ktor.android)
    implementation(libs.ktor.core)
    implementation(libs.ktor.content.negotiation)
    implementation(libs.ktor.serialization)

    // ── WorkManager ────────────────────────────────────────────────────────
    implementation(libs.androidx.work.runtime.ktx)

    // ── Coil ───────────────────────────────────────────────────────────────
    implementation(libs.coil.compose)

    // ── Coroutines ─────────────────────────────────────────────────────────
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // ── Serialization ──────────────────────────────────────────────────────
    implementation(libs.kotlinx.serialization.json)

    // ── Location ───────────────────────────────────────────────────────────
    implementation(libs.play.services.location)

    // ── Testing ────────────────────────────────────────────────────────────
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
