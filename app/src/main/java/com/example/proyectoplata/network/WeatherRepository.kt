package com.example.proyectoplata.network

import com.example.proyectoplata.models.WeatherResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WeatherRepository {

    // Base URL for OpenWeatherMap API
    private val BASE_URL = "https://api.openweathermap.org/data/2.5/"

    // Lazy initialization of Retrofit and ApiService to ensure they are created only when needed
    private val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    /**
     * Fetches current weather data for a given city and country code.
     * @param fullQuery A string combining city name and country code (e.g., "London,uk").
     * @param apiKey Your OpenWeatherMap API key.
     * @return A WeatherResponse object if successful, null otherwise.
     */
    suspend fun fetchWeatherData(fullQuery: String, apiKey: String): WeatherResponse? {
        return withContext(Dispatchers.IO) {
            try {
                // The 'city' parameter in getCurrentWeather should contain "city,countryCode"
                val response = apiService.getCurrentWeather(fullQuery, apiKey, "metric", "es")
                if (response.isSuccessful) {
                    response.body()
                } else {
                    // Log the error message if the API call was not successful
                    println("Error fetching weather: ${response.code()} - ${response.message()}")
                    null
                }
            } catch (e: Exception) {
                // Log any exceptions that occur during the network call
                println("Exception during weather data fetch: ${e.message}")
                null
            }
        }
    }
}
