package com.example.proyectoplata.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope // Importa lifecycleScope
import com.example.proyectoplata.BuildConfig
import com.example.proyectoplata.databinding.FragmentCropRecommendationBinding
import com.example.proyectoplata.network.ApiService // Tu ApiService existente
import com.example.proyectoplata.models.WeatherResponse // ¡Importación corregida y confirmada que es de 'models'!
import com.example.proyectoplata.network.WeatherRepository // <-- Importa WeatherRepository
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Normalizer
import java.util.Locale


class CropRecommendationFragment : Fragment() {

    private val TAG = "CropRecFragment"
    private var _binding: FragmentCropRecommendationBinding? = null
    private val binding get() = _binding!!

    private lateinit var spinnerMonth: Spinner
    private lateinit var etCityName: EditText
    private lateinit var etCountryName: EditText
    private lateinit var btnGetRecommendation: Button
    private lateinit var tvRecommendationResult: TextView

    private var selectedMonth: String = ""

    // Gemini
    private val geminiApiKey = BuildConfig.GEMINI_API_KEY
    private lateinit var generativeModel: GenerativeModel

    // OpenWeatherMap API Key
    private val openWeatherApiKey = BuildConfig.OPEN_WEATHER_API_KEY

    // Instancia de WeatherRepository (se inicializa una vez de forma perezosa)
    private val weatherRepository: WeatherRepository by lazy { WeatherRepository() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCropRecommendationBinding.inflate(inflater, container, false)
        val view = binding.root
        Log.d(TAG, "onCreateView: CropRecommendationFragment layout inflado.")

        // Inicializar los elementos de UI
        spinnerMonth = binding.spinnerMonth
        etCityName = binding.tilCityName.editText!!
        etCountryName = binding.tilCountryName.editText!!
        btnGetRecommendation = binding.btnGetRecommendation
        tvRecommendationResult = binding.tvRecommendationResult
        Log.d(TAG, "onCreateView: Elementos de UI inicializados.")

        // Inicializar el modelo Gemini
        if (geminiApiKey.isNullOrEmpty() || geminiApiKey == "YOUR_GEMINI_API_KEY_HERE") {
            Log.e(TAG, "ERROR: La clave API de Gemini no se encontró o no está configurada correctamente.")
            tvRecommendationResult.text = "Error: Clave API de Gemini no configurada."
            btnGetRecommendation.isEnabled = false
        } else {
            generativeModel = GenerativeModel(modelName = "gemini-1.5-flash", apiKey = geminiApiKey)
            Log.d(TAG, "Gemini Model inicializado en CropRecommendationFragment.")
            btnGetRecommendation.isEnabled = true
        }

        // Configurar listener para el Spinner de Mes
        spinnerMonth.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedMonth = parent?.getItemAtPosition(position).toString()
                Log.d(TAG, "Mes seleccionado: $selectedMonth")
            }
            override fun onNothingSelected(parent: AdapterView<*>?) { /* No hacer nada */ }
        }

        // Click listener para el botón de recomendación
        btnGetRecommendation.setOnClickListener {
            val cityName = etCityName.text.toString().trim()
            val countryName = etCountryName.text.toString().trim()

            if (selectedMonth.isNotEmpty() && cityName.isNotEmpty() && countryName.isNotEmpty()) {
                if (openWeatherApiKey.isNullOrEmpty() || openWeatherApiKey == "YOUR_OPEN_WEATHER_API_KEY_HERE") {
                    Toast.makeText(requireContext(), "Error: Clave API de OpenWeatherMap no configurada.", Toast.LENGTH_LONG).show()
                } else if (!::generativeModel.isInitialized) {
                    Toast.makeText(requireContext(), "Error: Modelo Gemini no inicializado.", Toast.LENGTH_LONG).show()
                } else {
                    getWeatherAndRecommendation(selectedMonth, cityName, countryName)
                }
            } else {
                Toast.makeText(requireContext(), "Por favor, selecciona un mes e ingresa la ciudad y el país.", Toast.LENGTH_SHORT).show()
            }
        }

        // Configura el ArrayAdapter para el spinner (Asegúrate de tener @array/months_array en strings.xml)
        ArrayAdapter.createFromResource(
            requireContext(),
            com.example.proyectoplata.R.array.months_array, // Asegúrate de que este ID sea correcto
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerMonth.adapter = adapter
        }

        return view
    }

    /**
     * Función principal para obtener datos climáticos (actuales) y luego la recomendación de Gemini.
     */
    private fun getWeatherAndRecommendation(month: String, cityName: String, countryName: String) {
        tvRecommendationResult.text = "Obteniendo datos climáticos para $cityName, $countryName en $month..."
        Log.d(TAG, "Iniciando proceso de recomendación para $cityName, $countryName en $month.")

        // Inicia una corrutina en el Dispatcher de IO para operaciones de red/disco
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Paso 1: Obtener el código ISO del país
                val isoCode = getIsoCodeForCountry(countryName)
                if (isoCode == null) {
                    withContext(Dispatchers.Main) { // Vuelve al hilo principal para mostrar Toast y actualizar UI
                        Toast.makeText(requireContext(), "País no reconocido. Por favor, intenta con otro nombre.", Toast.LENGTH_SHORT).show()
                        tvRecommendationResult.text = "Error: País no reconocido."
                    }
                    return@launch // Salir de la coroutine si el país no es reconocido
                }

                val weatherQuery = "$cityName,$isoCode"
                Log.d(TAG, "Consultando OpenWeatherMap para: $weatherQuery")

                // Obtener datos climáticos actuales usando el repositorio
                // Ahora se captura la respuesta completa de Retrofit
                val weatherResponse: retrofit2.Response<WeatherResponse> = weatherRepository.fetchWeatherData(weatherQuery, openWeatherApiKey)

                val weatherInfo: String
                val weatherData: WeatherResponse? // Declaramos weatherData como nullable

                // Procesar la respuesta de la API
                if (weatherResponse.isSuccessful) {
                    weatherData = weatherResponse.body() // Obtenemos el cuerpo de la respuesta
                    if (weatherData != null) {
                        weatherInfo = "En la ciudad de ${weatherData.name} ($countryName) actualmente hay: " +
                                "Temperatura: ${weatherData.main.temp}°C, " +
                                "Humedad: ${weatherData.main.humidity}%, " +
                                "Sensación térmica: ${weatherData.main.feels_like}°C, " +
                                "Presión: ${weatherData.main.pressure} hPa. " +
                                "Condiciones: ${weatherData.weather.firstOrNull()?.description ?: "N/D"}."
                        Log.d(TAG, "Datos climáticos actuales obtenidos: $weatherInfo")
                    } else {
                        // La llamada fue exitosa, pero el cuerpo de la respuesta fue nulo/vacío
                        weatherInfo = "No se pudieron parsear los datos climáticos para $cityName, $countryName. La recomendación se basará en el conocimiento general."
                        Log.w(TAG, "Cuerpo de respuesta de OpenWeatherMap vacío o nulo para $weatherQuery (pero la llamada fue exitosa).")
                    }
                } else {
                    // La llamada a la API no fue exitosa (ej. error 404, 500, sin conexión)
                    weatherData = null // No hay datos válidos en caso de fallo
                    weatherInfo = "No se pudieron obtener datos climáticos actuales para $cityName, $countryName (Código de error: ${weatherResponse.code()}). La recomendación se basará en el conocimiento general."
                    Log.w(TAG, "Fallo al obtener datos climáticos de OpenWeatherMap para $weatherQuery. Código: ${weatherResponse.code()}, Mensaje: ${weatherResponse.message()}")
                }

                // Paso 2: Crear el prompt para Gemini con la información climática (sea real o por defecto).
                val geminiPrompt = createCropRecommendationPrompt(month, cityName, countryName, weatherInfo)
                Log.d(TAG, "Enviando prompt a Gemini: $geminiPrompt")

                // Paso 3: Obtener la recomendación de Gemini.
                val response = generativeModel.generateContent(
                    content { text(geminiPrompt) }
                )

                withContext(Dispatchers.Main) { // Vuelve al hilo principal para actualizar la UI
                    val recommendation = response.text
                    if (!recommendation.isNullOrEmpty()) {
                        tvRecommendationResult.text = recommendation
                        Log.d(TAG, "Recomendación de Gemini recibida: $recommendation")
                    } else {
                        tvRecommendationResult.text = "Gemini no pudo generar una recomendación válida. Intenta de nuevo."
                        Log.w(TAG, "Respuesta vacía de Gemini para $cityName, $countryName en $month.")
                    }
                }

            } catch (e: Exception) {
                // Captura cualquier excepción que ocurra durante el proceso
                Log.e(TAG, "Error al obtener recomendación de cultivos: ${e.message}", e)
                withContext(Dispatchers.Main) { // Vuelve al hilo principal para mostrar el error al usuario
                    tvRecommendationResult.text = "Error al obtener recomendación: ${e.message}\nVerifica tu conexión a internet o la configuración de API Keys."
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show() // También muestra un toast
                }
            }
        }
    }

    /**
     * Función helper para obtener el código ISO de 2 letras de un país dado su nombre.
     * Reutilizada de HomeFragment.
     */
    private fun getIsoCodeForCountry(countryName: String): String? {
        val normalizedCountryName = Normalizer.normalize(countryName, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .lowercase(Locale.ROOT)

        for (isoCountryCode in Locale.getISOCountries()) {
            val locale = Locale("", isoCountryCode)
            val name = locale.getDisplayCountry(Locale.getDefault())
            val normalizedName = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
                .lowercase(Locale.ROOT)

            if (normalizedCountryName == normalizedName) {
                return isoCountryCode
            }
        }
        return null
    }

    /**
     * Construye el prompt específico para que Gemini recomiende cultivos basado en mes, ciudad y país.
     * Añade la información climática obtenida de la API.
     */
    private fun createCropRecommendationPrompt(month: String, cityName: String, countryName: String, weatherData: String): String {
        return """
        Eres un asistente experto en agronomía. Tu objetivo es recomendar cultivos adecuados para plantar en una ubicación específica (ciudad, país) durante un mes determinado, basándote en la información climática proporcionada y tu conocimiento general. La información climática se refiere a las condiciones actuales de la ciudad ingresada, y debe usarse como una referencia general para el mes.

        Información de Contexto:
        - Mes: $month
        - Ubicación: $cityName, $countryName
        - Información Climática de referencia para $cityName: $weatherData

        Con base en esta información, por favor, enumera y describe brevemente 3 a 5 cultivos que serían óptimos para sembrar o que tengan su mejor rendimiento si se inician en este mes y ubicación. Para cada cultivo, menciona brevemente por qué es adecuado (ej. "tolerante a bajas temperaturas", "requiere mucha humedad", "aprovecha la temporada de lluvias"). Si las condiciones descritas son particularmente adversas, indícalo y explica cómo podrían afectar los cultivos. Si necesitas más información de contexto para dar una mejor recomendación (ej. tipo de suelo específico de la granja, disponibilidad de agua, altitud específica), por favor, menciona qué tipo de información adicional te sería útil. Responde de forma clara, concisa y fácil de entender.
        """.trimIndent()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d(TAG, "onDestroyView: CropRecommendationFragment view destruida.")
    }
}