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
    // Librerías de AndroidX y Kotlin (actualizadas)
    implementation(libs.androidx.core.ktx) // Asumo que libs.androidx.core.ktx apunta a una versión reciente como 1.13.1
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose) // Si estás mezclando Views con Compose, esto está bien.

    // Dependencias fundamentales de UI (ACTUALIZADAS y COMPATIBLES)
    // androidx.appcompat es la base para la mayoría de las funcionalidades de compatibilidad
    implementation("androidx.appcompat:appcompat:1.7.0") // ¡ACTUALIZADO!
    // Material Design components - Necesario para NavigationView, etc.
    implementation("com.google.android.material:material:1.12.0") // ¡ACTUALIZADO!
    // Para el DrawerLayout
    implementation("androidx.drawerlayout:drawerlayout:1.2.0") // ¡ACTUALIZADO!

    // Librerías de Jetpack Compose (mantengo las que ya tienes y el BOM)
    implementation(platform(libs.androidx.compose.bom)) // Asegúrate de que tu compose.bom sea reciente, ej. 2024.06.00
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3) // Si usas Material Design 3 en Compose

    // Librerías de Gráficos
    // Si NO usas Jetpack Compose para los gráficos, ELIMINA la siguiente línea:
    // implementation("com.github.tehras:charts:0.2.4-alpha")
    // Mantén MPAndroidChart si tus gráficos son con vistas tradicionales
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0") // Versión estable popular.

    implementation("androidx.appcompat:appcompat:1.3.1")
    // Recuerda añadir jitpack.io en settings.gradle si no lo tienes.

    // Librerías de Firebase (ACTUALIZADAS)
    // El BOM (Bill Of Materials) gestiona las versiones de Firebase por ti
    implementation(platform("com.google.firebase:firebase-bom:33.14.0")) // Versión estable y reciente del BOM
    // Firebase Cloud Messaging. El BOM define su versión, no necesitas especificarla aquí.
    implementation("com.google.firebase:firebase-messaging")

    // Librerías de Retrofit y OkHttp (ACTUALIZADAS)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // OkHttp actualizado para compatibilidad y mejoras
    implementation("com.squareup.okhttp3:okhttp:4.12.0") // ¡ACTUALIZADO!
    // Opcional: si necesitas ver los logs de las peticiones de red
    // implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Librerías de Testing (mantengo las que ya tienes)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // Librerías de Debug (mantengo las que ya tienes)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}