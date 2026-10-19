import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// La clé de signature dédiée à Mindcare Mobile : hors du dépôt, sur le PC de Jordan (C:\Users\death\.android), avec une
// sauvegarde chiffrée sur son Google Drive. Sans elle, plus aucune mise à jour ne peut s'installer sur les téléphones.
val cle = Properties().apply {
    val f = File(System.getProperty("user.home"), ".android/mindcare-mobile.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "io.github.jordanjacquot.mindcare"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.jordanjacquot.mindcare"
        minSdk = 26          // Android 8
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("mindcare") {
            if (cle.isNotEmpty()) {
                storeFile = file(cle.getProperty("storeFile"))
                storePassword = cle.getProperty("storePassword")
                keyAlias = cle.getProperty("keyAlias")
                keyPassword = cle.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (cle.isNotEmpty()) signingConfig = signingConfigs.getByName("mindcare")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.junit)
}
