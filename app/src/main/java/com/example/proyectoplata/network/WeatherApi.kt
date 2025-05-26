package com.example.proyectoplata.network // Asegúrate de usar el nombre correcto de tu paquete

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("weather")
    suspend fun getWeather(
        @Query("q") city: String, // Ciudad (puedes cambiarla por cualquier ciudad)
        @Query("appid") appid: String, // API Key de OpenWeatherMap
        @Query("units") units: String = "metric" // Unidades (Celsius)
    ): Response<WeatherResponse>
}
