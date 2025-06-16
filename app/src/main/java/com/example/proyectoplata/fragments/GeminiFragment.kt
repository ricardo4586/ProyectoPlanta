package com.example.proyectoplata.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.widget.Button
import androidx.annotation.NonNull
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.proyectoplata.BuildConfig
import com.example.proyectoplata.SharedSensorViewModel
import com.example.proyectoplata.databinding.FragmentGeminiBinding
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GeminiFragment : Fragment() {

    private val TAG = "GeminiFragment"
    private var _binding: FragmentGeminiBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedSensorViewModel: SharedSensorViewModel

    private val geminiApiKey = BuildConfig.GEMINI_API_KEY
    private lateinit var generativeModel: GenerativeModel

    private var currentHumedadAmbiental: Double = -1.0
    private var currentHumedadSuelo: Double = -1.0
    private var currentIndiceUv: Double = -1.0
    private var currentVoltajeUva: Double = -1.0
    private var currentNitrogeno: Int = -1
    private var currentFosforo: Int = -1
    private var currentPotasio: Int = -1

    private lateinit var etUserQuery: EditText
    private lateinit var btnSendQuery: Button
    private lateinit var tvGeminiRecommendation: TextView
    private lateinit var tvUserQueryResponse: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedSensorViewModel = ViewModelProvider(requireActivity()).get(SharedSensorViewModel::class.java)
        Log.d(TAG, "onCreate: SharedSensorViewModel inicializado en GeminiFragment.")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGeminiBinding.inflate(inflater, container, false)
        val view = binding.root
        Log.d(TAG, "onCreateView: GeminiFragment layout inflado.")

        etUserQuery = binding.etUserQuery
        btnSendQuery = binding.btnSendQuery
        tvGeminiRecommendation = binding.tvGeminiRecommendation
        tvUserQueryResponse = binding.tvUserQueryResponse
        Log.d(TAG, "onCreateView: Elementos de UI de Gemini inicializados.")

        if (geminiApiKey.isNullOrEmpty() || geminiApiKey == "YOUR_GEMINI_API_KEY_HERE") {
            Log.e(TAG, "ERROR: La clave API de Gemini no se encontró o no está configurada correctamente en BuildConfig.")
            val errorText = "ERROR: Clave API de Gemini no configurada. Por favor, revisa tu local.properties y build.gradle."
            tvGeminiRecommendation.text = errorText
            tvUserQueryResponse.text = errorText
        } else {
            generativeModel = GenerativeModel(modelName = "gemini-1.5-flash", apiKey = geminiApiKey)
            Log.d(TAG, "Gemini Model inicializado con API Key y modelo gemini-1.5-flash.")
        }

        binding.btnSendQuery.setOnClickListener {
            val userQuery = binding.etUserQuery.text.toString().trim()
            if (userQuery.isNotEmpty()) {
                sendUserQueryToGemini(userQuery)
            } else {
                Toast.makeText(requireContext(), "Por favor, escribe tu pregunta.", Toast.LENGTH_SHORT).show()
            }
        }
        Log.d(TAG, "onCreateView: Click Listener de enviar consulta configurado.")

        observeSensorData()
        Log.d(TAG, "onCreateView: Observación de datos de sensor iniciada en GeminiFragment.")

        return view
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: GeminiFragment está visible.")
        attemptGeminiRecommendation()
    }

    private fun observeSensorData() {
        Log.d(TAG, "observeSensorData: Configurando observadores para datos de sensores.")
        sharedSensorViewModel.humidity.observe(viewLifecycleOwner) { hum ->
            if (currentHumedadAmbiental != hum) {
                currentHumedadAmbiental = hum
                Log.d(TAG, "Observed: Hum Amb = $currentHumedadAmbiental. Attempting Gemini Rec.")
                attemptGeminiRecommendation()
            }
        }
        sharedSensorViewModel.humiditySuelo.observe(viewLifecycleOwner) { humSuelo ->
            if (currentHumedadSuelo != humSuelo) {
                currentHumedadSuelo = humSuelo
                Log.d(TAG, "Observed: Hum Suelo = $currentHumedadSuelo. Attempting Gemini Rec.")
                attemptGeminiRecommendation()
            }
        }
        sharedSensorViewModel.uvIndex.observe(viewLifecycleOwner) { uv ->
            if (currentIndiceUv != uv) {
                currentIndiceUv = uv
                Log.d(TAG, "Observed: UV = $currentIndiceUv. Attempting Gemini Rec.")
                attemptGeminiRecommendation()
            }
        }
        sharedSensorViewModel.light.observe(viewLifecycleOwner) { lightVal ->
            if (currentVoltajeUva != lightVal) {
                currentVoltajeUva = lightVal
                Log.d(TAG, "Observed: Voltaje UVA = $currentVoltajeUva. Attempting Gemini Rec.")
                attemptGeminiRecommendation()
            }
        }
        sharedSensorViewModel.nitrogeno.observe(viewLifecycleOwner) { nitro ->
            if (currentNitrogeno != nitro) {
                currentNitrogeno = nitro
                Log.d(TAG, "Observed: Nitrogeno = $currentNitrogeno. Attempting Gemini Rec.")
                attemptGeminiRecommendation()
            }
        }
        sharedSensorViewModel.fosforo.observe(viewLifecycleOwner) { phos ->
            if (currentFosforo != phos) {
                currentFosforo = phos
                Log.d(TAG, "Observed: Fosforo = $currentFosforo. Attempting Gemini Rec.")
                attemptGeminiRecommendation()
            }
        }
        sharedSensorViewModel.potasio.observe(viewLifecycleOwner) { pot ->
            if (currentPotasio != pot) {
                currentPotasio = pot
                Log.d(TAG, "Observed: Potasio = $currentPotasio. Attempting Gemini Rec.")
                attemptGeminiRecommendation()
            }
        }
    }

    /**
     * Intenta generar una recomendación de Gemini automáticamente si los datos disponibles
     * de los sensores son válidos y la clave API de Gemini está configurada.
     */
    private fun attemptGeminiRecommendation() {
        Log.d(TAG, "attemptGeminiRecommendation: Iniciando verificación de datos para recomendación automática.")
        // Mostrar solo los valores de los sensores que realmente se esperan y están siendo observados.
        Log.d(TAG, "Valores de sensores actuales en GeminiFragment: HumAmb=$currentHumedadAmbiental, HumSuelo=$currentHumedadSuelo, UV=$currentIndiceUv, UVA=$currentVoltajeUva, NPK_N=$currentNitrogeno, NPK_P=$currentFosforo, NPK_K=$currentPotasio")


        if (::generativeModel.isInitialized &&
            currentHumedadAmbiental != -1.0 &&
            currentHumedadSuelo != -1.0 &&
            currentIndiceUv != -1.0 &&
            currentVoltajeUva != -1.0 &&
            currentNitrogeno != -1 &&
            currentFosforo != -1 &&
            currentPotasio != -1 &&
            !geminiApiKey.isNullOrEmpty() && geminiApiKey != "YOUR_GEMINI_API_KEY_HERE") {

            Log.d(TAG, "attemptGeminiRecommendation: ¡Todos los datos de sensores DISPONIBLES son válidos y Gemini está listo! Generando recomendación.")
            val autoPrompt = createGeminiPrompt(
                // Instrucción modificada: Más concisa y fácil de entender
                "Genera una recomendación para el cultivo de papas. Analiza los datos de los sensores (humedad ambiental, humedad del suelo, índice UV, voltaje UVA, nitrógeno, fósforo, potasio) y, con tu conocimiento general, da consejos sobre riego, luz y nutrientes. Usa palabras claras y precisas, evitando lenguaje técnico. Si algún dato es 'N/D', explica brevemente su impacto y qué hacer.",
                currentHumedadAmbiental.toFloat(),
                currentHumedadSuelo.toFloat(),
                currentIndiceUv.toFloat(),
                currentVoltajeUva.toFloat(),
                currentNitrogeno,
                currentFosforo,
                currentPotasio
            )
            Log.d(TAG, "Prompt automático para Gemini: $autoPrompt")
            generateGeminiResponse(autoPrompt, tvGeminiRecommendation)
        } else if (!::generativeModel.isInitialized || geminiApiKey.isNullOrEmpty() || geminiApiKey == "YOUR_GEMINI_API_KEY_HERE") {
            if (isAdded) {
                val errorText = "ERROR: Clave API de Gemini no configurada o modelo no inicializado para recomendación automática."
                tvGeminiRecommendation.text = errorText
                Log.e(TAG, "Recomendación Gemini omitida: $errorText")
            }
        } else {
            if (isAdded) {
                tvGeminiRecommendation.text = "Esperando datos completos de sensores disponibles para recomendación automática..."
            }
            Log.d(TAG, "Recomendación Gemini omitida: No todos los datos de los sensores DISPONIBLES están aún.")
        }
    }

    /**
     * Construye el prompt para la API de Gemini, combinando los datos de los sensores
     * con la pregunta del usuario.
     */
    private fun createGeminiPrompt(
        userQuestion: String,
        humAmb: Float, humSuelo: Float, uvIndex: Float, voltajeUva: Float,
        nitrogeno: Int, fosforo: Int, potasio: Int
    ): String {
        // Formatea los valores de los sensores a cadenas legibles, o "N/D" si no están disponibles.
        val humAmbStr = if (humAmb != -1f) "$humAmb%" else "N/D"
        val humSueloStr = if (humSuelo != -1f) "$humSuelo%" else "N/D"
        val uvIndexStr = if (uvIndex != -1f) "$uvIndex" else "N/D"
        val voltajeUvaStr = if (voltajeUva != -1f) "$voltajeUva V" else "N/D"
        val nitrogenoStr = if (nitrogeno != -1) "$nitrogeno mg/kg" else "N/D"
        val fosforoStr = if (fosforo != -1) "$fosforo mg/kg" else "N/D"
        val potasioStr = if (potasio != -1) "$potasio mg/kg" else "N/D"

        // Construye el prompt completo para Gemini.
        return """
        Aquí están los últimos datos de los sensores de una planta:
        - Humedad Ambiental: $humAmbStr
        - Humedad del Suelo: $humSueloStr
        - Índice UV: $uvIndexStr
        - Voltaje UVA del sensor: $voltajeUvaStr
        - Nitrógeno (N): $nitrogenoStr
        - Fósforo (P): $fosforoStr
        - Potasio (K): $potasioStr

        Pregunta del usuario: $userQuestion
        """.trimIndent()
    }

    /**
     * Envía la consulta del usuario a la API de Gemini y muestra la respuesta.
     */
    private fun sendUserQueryToGemini(userQuery: String) {
        // Verifica si el modelo está inicializado y la clave API es válida.
        if (!::generativeModel.isInitialized || geminiApiKey.isNullOrEmpty() || geminiApiKey == "YOUR_GEMINI_API_KEY_HERE") {
            if (isAdded) {
                tvUserQueryResponse.text = "ERROR: Modelo Gemini no inicializado o clave API no configurada. Por favor, configura tu clave API."
            }
            return
        }

        // Muestra un mensaje de "Consultando..." mientras se espera la respuesta.
        if (isAdded) {
            tvUserQueryResponse.text = "Consultando a Gemini..."
            Log.d(TAG, "sendUserQueryToGemini: Estableciendo texto 'Consultando a Gemini...' en tvUserQueryResponse")
        }

        // Modificación: Agregamos contexto al userQuery para que Gemini se explaye sobre cultivo de papas y sea claro/preciso.
        val contextualUserQuery = "Considerando que el cultivo es de papas y usando tu conocimiento general junto con los datos de los sensores, por favor, responde a mi pregunta de forma clara, precisa y fácil de entender para cualquier persona, evitando lenguaje técnico: $userQuery"

        // Construye el prompt combinado con la pregunta del usuario y los datos de los sensores.
        val combinedPrompt = createGeminiPrompt(
            contextualUserQuery, // Usamos la pregunta contextualizada
            currentHumedadAmbiental.toFloat(),
            currentHumedadSuelo.toFloat(),
            currentIndiceUv.toFloat(),
            currentVoltajeUva.toFloat(),
            currentNitrogeno,
            currentFosforo,
            currentPotasio
        )
        Log.d(TAG, "Prompt combinado para Gemini (usuario): $combinedPrompt")

        // Llama a la función genérica para obtener y mostrar la respuesta de Gemini.
        generateGeminiResponse(combinedPrompt, tvUserQueryResponse)
        binding.etUserQuery.text.clear() // Limpiar campo de texto del usuario después de enviar.
    }

    /**
     * Función genérica para interactuar con la API de Gemini y actualizar un TextView.
     * @param prompt La cadena de texto que se enviará a Gemini.
     * @param targetTextView El TextView donde se mostrará la respuesta.
     */
    private fun generateGeminiResponse(prompt: String, targetTextView: TextView) {
        // Verifica si el modelo está inicializado.
        if (!::generativeModel.isInitialized) {
            Log.e(TAG, "generateGeminiResponse: Gemini model not initialized.")
            lifecycleScope.launch(Dispatchers.Main) {
                if (isAdded) {
                    targetTextView.text = "Error: Modelo de IA no disponible. Revisa tu clave API."
                }
            }
            return
        }

        // Lanza una corrutina en el hilo de IO para realizar la llamada a la red.
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Realiza la llamada a la API de Gemini.
                val response = generativeModel.generateContent(
                    content {
                        text(prompt)
                    }
                )
                val recommendation = response.text // Obtiene el texto de la respuesta.
                Log.d(TAG, "Respuesta de Gemini recibida: '$recommendation'")

                // Vuelve al hilo principal (UI) para actualizar el TextView.
                withContext(Dispatchers.Main) {
                    if (isAdded) { // Asegura que el fragmento sigue adjunto a la actividad.
                        // Actualiza el TextView con la respuesta completa.
                        if (!recommendation.isNullOrEmpty()) {
                            targetTextView.text = recommendation
                            Log.d(TAG, "TextView ${
                                // Log para identificar qué TextView se actualizó.
                                when(targetTextView.id) {
                                    tvGeminiRecommendation.id -> "tvGeminiRecommendation"
                                    tvUserQueryResponse.id -> "tvUserQueryResponse"
                                    else -> "Unknown TextView"
                                }
                            } actualizado con: '$recommendation'")
                        } else {
                            // Si Gemini no devuelve una respuesta válida.
                            targetTextView.text = "Gemini no pudo generar una respuesta válida."
                            Log.w(TAG, "Gemini generó una respuesta nula o vacía para el prompt: '$prompt'")
                        }
                    }
                }
            } catch (e: Exception) {
                // Manejo de errores en caso de fallo de la API.
                Log.e(TAG, "Error llamando a Gemini API: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        targetTextView.text = "Error al obtener respuesta de Gemini: ${e.message}"
                        Log.e(TAG, "generateGeminiResponse: Estableciendo error de API en TextView: ${e.message}")
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d(TAG, "onDestroyView: GeminiFragment view destruida.")
    }
}
