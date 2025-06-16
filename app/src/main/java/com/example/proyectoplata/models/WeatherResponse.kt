package com.example.proyectoplata.models

data class WeatherResponse(
    val main: Main,
    val weather: List<Weather>,
    val name: String // Nombre de la ciudad
)

data class Main(
    val temp: Double,
    val feels_like: Double,
    val temp_min: Double,
    val temp_max: Double,
    val pressure: Int,
    val humidity: Int
)

data class Weather(
    val id: Int,
    val main: String, // Ej: "Clouds"
    val description: String, // Ej: "few clouds"
    val icon: String
)