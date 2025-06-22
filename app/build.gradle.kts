// build.gradle.kts (Module :app)

// No necesitas la importación de Properties si no vas a cargar un archivo específico
// import java.util.Properties // Esta línea se puede comentar o eliminar

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services") // Para Firebase
}

android {
    namespace = "com.example.proyectoplata"
    compileSdk = 35 // Asegúrate de que esta SDK esté instalada

    defaultConfig {
        applicationId = "com.example.proyectoplata"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // *** CONFIGURACIÓN PARA OCULTAR CLAVES API EN buildConfigField ***
        // Estas propiedades se leerán directamente de gradle.properties (en la raíz del proyecto)
        // Asegúrate que en gradle.properties tengas las claves sin comillas:
        // WEATHER_API_KEY=tu_clave_de_open_weather
        // GEMINI_API_KEY=tu_clave_de_gemini

        val openWeatherApiKey: String = (project.findProperty("WEATHER_API_KEY") as? String) ?: "YOUR_OPEN_WEATHER_API_KEY_HERE"
        val geminiApiKey: String = (project.findProperty("GEMINI_API_KEY") as? String) ?: "YOUR_GEMINI_API_KEY_HERE"

        // Se añaden comillas escapadas aquí para que la clave sea un String válido en BuildConfig.java
        buildConfigField("String", "OPEN_WEATHER_API_KEY", "\"$openWeatherApiKey\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
        // *** FIN CONFIGURACIÓN CLAVES API ***
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
        buildConfig = true // <--- ¡MANTÉN ESTA LÍNEA! Es crucial para buildConfigField.
    }

    // Para evitar conflictos de recursos duplicados con MPAndroidChart (si los hay)
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Librerías de AndroidX y Kotlin
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.fragment:fragment-ktx:1.8.1")
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.0")
    // Si necesitas lifecycle-runtime-ktx explícitamente:
    // implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")

    // Dependencias fundamentales de UI
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0") // Eliminada la duplicidad
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")

    // Librería de Gemini
    implementation("com.google.ai.client.generativeai:generativeai:0.8.0")

    // Librerías de Jetpack Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // Librerías de Gráficos (MPAndroidChart)
    // Asegúrate de que tienes jitpack.io en tus repositories en settings.gradle.kts (project level)
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0") // Eliminada la duplicidad

    // Librerías de Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-database-ktx")

    // Kotlin Coroutines para Firebase y otras tareas asíncronas
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.0")

    // Librerías de Retrofit y OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Librerías de Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    // Librerías de Debug para Compose
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}