package com.example.proyectoplata.network

import com.example.proyectoplata.models.WeatherResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response // Importar retrofit2.Response

class WeatherRepository {

    private val BASE_URL = "https://api.openweathermap.org/data/2.5/"

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
     * @return A retrofit2.Response<WeatherResponse> object.
     */
    suspend fun fetchWeatherData(fullQuery: String, apiKey: String): Response<WeatherResponse> { // <-- ¡Ahora devuelve Response<WeatherResponse>!
        return withContext(Dispatchers.IO) {
            try {
                apiService.getCurrentWeather(fullQuery, apiKey, "metric", "es") // <-- Simplemente devuelve la respuesta de la API
            } catch (e: Exception) {
                // En caso de excepción de red (no 200-OK), puedes devolver un Response de error
                // Esto es un ejemplo. Podrías crear tu propio Response de error o propagar la excepción.
                // Para simplificar, si hay una excepción, puedes construir un Response fallido.
                // Sin embargo, lo más común es simplemente dejar que la excepción sea capturada
                // en la capa superior (Fragment/ViewModel) si necesitas un manejo específico.
                // Para que el tipo coincida, debemos devolver un Response.
                // Una forma simple es devolver un Response que indique fallo.
                // Esto es solo un placeholder, un manejo más robusto podría ser lanzar la excepción.

                // Aquí, para mantener el tipo de retorno, estamos creando una respuesta fallida.
                // Esto puede ser simplificado si el catch en el fragmento maneja las excepciones directamente.
                Response.error(500, okhttp3.ResponseBody.create(null, "Error de red: ${e.message}"))
            }
        }
    }
}