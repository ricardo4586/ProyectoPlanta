package com.example.proyectoplata.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import com.example.proyectoplata.models.WeatherResponse // <--- ¡CAMBIA ESTA LÍNEA!

interface ApiService {
    @GET("weather")
    suspend fun getCurrentWeather(
        @Query("q") city: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "es"
    ): Response<WeatherResponse>
}