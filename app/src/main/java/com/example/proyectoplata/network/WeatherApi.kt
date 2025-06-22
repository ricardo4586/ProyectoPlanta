package com.example.proyectoplata.network

import retrofit2.http.GET
import retrofit2.http.Query
import com.example.proyectoplata.models.WeatherResponse // <--- ¡Importación corregida aquí!

interface WeatherApiService {
    @GET("data/2.5/weather") // La ruta base de la API de OpenWeatherMap
    suspend fun getWeather(
        @Query("q") query: String, // Aquí pasamos "ciudad,codigo_iso"
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric" // Para obtener temperaturas en Celsius
    ): retrofit2.Response<WeatherResponse> // Cambiado a retrofit2.Response
}