package com.example.proyectoplata.network

data class WeatherResponse(
    val main: Main,
    val weather: List<Weather>
)

data class Main(
    val temp: Double,
    val temp_min: Double,  // Agregar temp_min
    val temp_max: Double,  // Agregar temp_max
    val humidity: Int
)

data class Weather(
    val description: String
)
