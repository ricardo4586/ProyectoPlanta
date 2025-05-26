package com.example.proyectoplata.network

import android.util.Log
import com.example.proyectoplata.network.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class WeatherRepository {

    suspend fun fetchWeatherData(cityName: String, apiKey: String): WeatherResponse? {
        try {
            val response = RetrofitInstance.api.getWeather(cityName, apiKey)
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
