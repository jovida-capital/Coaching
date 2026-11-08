import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "fr.jovida.decisionboard"
    compileSdk = 35

    defaultConfig {
        applicationId = "fr.jovida.decisionboard"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        // URL HTTPS du backend et jeton : à renseigner dans local.properties (jamais dans git)
        // BACKEND_URL=https://ton-serveur.example   APP_TOKEN=...
        val props = Properties().apply {
            val f = rootProject.file("local.properties")
            if (f.exists()) f.inputStream().use { load(it) }
        }
        buildConfigField("String", "BACKEND_URL", "\"${props.getProperty("BACKEND_URL", "https://example.invalid")}\"")
        buildConfigField("String", "APP_TOKEN", "\"${props.getProperty("APP_TOKEN", "")}\"")
    }

    // Clé de signature commune aux 3 apps (nécessaire pour la permission "signature").
    // Fournie par l'environnement (GitHub Actions) ; sinon clé debug locale par défaut.
    signingConfigs {
        System.getenv("KEYSTORE_PATH")?.let { path ->
            create("shared") {
                storeFile = file(path)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        getByName("debug") {
            signingConfigs.findByName("shared")?.let { signingConfig = it }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
