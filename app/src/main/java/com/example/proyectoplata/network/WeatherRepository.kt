package com.example.proyectoplata.network

import android.util.Log

class WeatherRepository {

    // La firma de esta función debe coincidir con la llamada en HomeFragment (query, apiKey)
    suspend fun fetchWeatherData(query: String, apiKey: String): WeatherResponse? {
        try {
            val response = RetrofitInstance.api.getWeather(query, apiKey)
            if (response.isSuccessful) {
                return response.body()
            } else {
                Log.e("WeatherAPI", "Error: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("WeatherAPI", "Exception: ${e.message}")
        }
        return null
    }
}