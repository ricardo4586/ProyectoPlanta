package com.example.proyectoplata.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import com.example.proyectoplata.models.WeatherResponse // Asegúrate de que esta ruta sea correcta

interface ApiService {
    @GET("weather")
    suspend fun getCurrentWeather(
        @Query("q") city: String,
        @Query("appid") apiKey: String, // La clave API
        @Query("units") units: String = "metric", // Para obtener temperatura en Celsius
        @Query("lang") lang: String = "es" // Para obtener descripciones en español
    ): Response<WeatherResponse>
}