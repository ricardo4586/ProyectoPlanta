import java.util.Properties // Importación necesaria para 'Properties'

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.proyectoplata"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.proyectoplata"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // *** CONFIGURACIÓN PARA OCULTAR LA CLAVE API ***
        val properties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            properties.load(localPropertiesFile.inputStream())
        }
        buildConfigField("String", "OPEN_WEATHER_API_KEY", properties.getProperty("WEATHER_API_KEY") ?: "\"aa8782089df8fb9de8b95f66b22f29f9\"")
        // *** FIN CONFIGURACIÓN CLAVE API ***
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
        viewBinding = true
        buildConfig = true // <--- ¡AÑADE ESTA LÍNEA! Esto habilitará la generación de BuildConfig con campos personalizados.
    }
}

dependencies {
    // Librerías de AndroidX y Kotlin
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation("androidx.fragment:fragment-ktx:1.8.0")

    // Dependencias fundamentales de UI
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")


    // Librerías de Jetpack Compose (mantengo las que ya tienes y el BOM)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Librerías de Gráficos (MPAndroidChart)
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Librerías de Firebase (El BOM gestiona las versiones)
    implementation(platform("com.google.firebase:firebase-bom:33.14.0"))
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-auth-ktx")

    // Kotlin Coroutines para Firebase (para usar .await() en tareas de Firebase)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")


    // Librerías de Retrofit y OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Librerías de Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // Librerías de Debug
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}