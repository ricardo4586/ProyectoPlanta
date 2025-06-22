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
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.proyectoplata.BuildConfig // Asegúrate de que tu clave API de Gemini esté en BuildConfig
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

    // Se asegura de que la clave API se lea correctamente.
    private val geminiApiKey = BuildConfig.GEMINI_API_KEY
    private lateinit var generativeModel: GenerativeModel

    // Variables para almacenar los últimos datos de los sensores
    // Inicializadas con valores que indican "no disponible"
    private var currentHumedadAmbiental: Double = -1.0
    private var currentHumedadSuelo: Double = -1.0
    private var currentTemperaturaAmbiental: Double = -1.0
    private var currentIndiceUv: Double = -1.0
    private var currentVoltajeUva: Double = -1.0
    private var currentNitrogeno: Int = -1
    private var currentFosforo: Int = -1
    private var currentPotasio: Int = -1

    // Elementos de UI
    private lateinit var etUserQuery: EditText
    private lateinit var btnSendQuery: Button
    private lateinit var tvGeminiRecommendation: TextView // Para la recomendación automática de sensores
    private lateinit var tvUserQueryResponse: TextView   // Para la respuesta a la pregunta directa del usuario

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

        // Inicialización de los elementos de UI a través del binding
        etUserQuery = binding.etUserQuery
        btnSendQuery = binding.btnSendQuery
        tvGeminiRecommendation = binding.tvGeminiRecommendation
        tvUserQueryResponse = binding.tvUserQueryResponse
        Log.d(TAG, "onCreateView: Elementos de UI de Gemini inicializados.")

        // Verificación de la clave API de Gemini y inicialización del modelo
        if (geminiApiKey.isNullOrEmpty() || geminiApiKey == "YOUR_GEMINI_API_KEY_HERE") {
            Log.e(TAG, "ERROR: La clave API de Gemini no se encontró o no está configurada correctamente en BuildConfig.")
            val errorText = "ERROR: Clave API de Gemini no configurada. Por favor, revisa tu local.properties y build.gradle."
            tvGeminiRecommendation.text = errorText
            tvUserQueryResponse.text = errorText
        } else {
            generativeModel = GenerativeModel(modelName = "gemini-1.5-flash", apiKey = geminiApiKey)
            Log.d(TAG, "Gemini Model inicializado con API Key y modelo gemini-1.5-flash.")
        }

        // Configuración del click listener para el botón de enviar pregunta
        binding.btnSendQuery.setOnClickListener {
            val userQuery = binding.etUserQuery.text.toString().trim()
            if (userQuery.isNotEmpty()) {
                sendUserQueryToGemini(userQuery)
            } else {
                Toast.makeText(requireContext(), "Por favor, escribe tu pregunta.", Toast.LENGTH_SHORT).show()
            }
        }
        Log.d(TAG, "onCreateView: Click Listener de enviar consulta configurado.")

        // Iniciar la observación de los datos de los sensores
        observeSensorData()
        Log.d(TAG, "onCreateView: Observación de datos de sensor iniciada en GeminiFragment.")

        return view
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: GeminiFragment está visible.")
        // Intenta generar una recomendación automática cuando el fragmento se hace visible o se reanuda.
        attemptGeminiRecommendation()
    }

    private fun observeSensorData() {
        Log.d(TAG, "observeSensorData: Configurando observadores para datos de sensores.")
        // Observa cada LiveData del ViewModel y actualiza las variables locales.
        // Después de cada actualización, se intenta generar una nueva recomendación de Gemini.
        sharedSensorViewModel.temperature.observe(viewLifecycleOwner) { temp ->
            if (currentTemperaturaAmbiental != temp) {
                currentTemperaturaAmbiental = temp
                Log.d(TAG, "Observed: Temp Amb = $currentTemperaturaAmbiental. Attempting Gemini Rec.")
                attemptGeminiRecommendation()
            }
        }
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
        // Mostrar todos los valores de los sensores que realmente se esperan y están siendo observados.
        Log.d(TAG, "Valores de sensores actuales en GeminiFragment: TempAmb=$currentTemperaturaAmbiental, HumAmb=$currentHumedadAmbiental, HumSuelo=$currentHumedadSuelo, UV=$currentIndiceUv, UVA=$currentVoltajeUva, NPK_N=$currentNitrogeno, NPK_P=$currentFosforo, NPK_K=$currentPotasio")

        // ** LOGS DETALLADOS PARA DEPURACIÓN **
        Log.d(TAG, "Condiciones de intento de recomendación:")
        Log.d(TAG, "  generativeModel.isInitialized: ${::generativeModel.isInitialized}")
        Log.d(TAG, "  TempAmb != -1.0: ${currentTemperaturaAmbiental != -1.0}")
        Log.d(TAG, "  HumAmb != -1.0: ${currentHumedadAmbiental != -1.0}")
        Log.d(TAG, "  HumSuelo != -1.0: ${currentHumedadSuelo != -1.0}")
        Log.d(TAG, "  IndiceUv != -1.0: ${currentIndiceUv != -1.0}")
        Log.d(TAG, "  VoltajeUva != -1.0: ${currentVoltajeUva != -1.0}")
        Log.d(TAG, "  Nitrogeno != -1: ${currentNitrogeno != -1}")
        Log.d(TAG, "  Fosforo != -1: ${currentFosforo != -1}")
        Log.d(TAG, "  Potasio != -1: ${currentPotasio != -1}")
        Log.d(TAG, "  API Key valid: ${!geminiApiKey.isNullOrEmpty() && geminiApiKey != "YOUR_GEMINI_API_KEY_HERE"}")
        // ************************************

        if (::generativeModel.isInitialized &&
            currentTemperaturaAmbiental != -1.0 &&
            currentHumedadAmbiental != -1.0 &&
            currentHumedadSuelo != -1.0 &&
            currentIndiceUv != -1.0 &&
            currentVoltajeUva != -1.0 &&
            currentNitrogeno != -1 &&
            currentFosforo != -1 &&
            currentPotasio != -1 &&
            !geminiApiKey.isNullOrEmpty() && geminiApiKey != "YOUR_GEMINI_API_KEY_HERE") {

            Log.d(TAG, "attemptGeminiRecommendation: ¡Todos los datos de sensores DISPONIBLES son válidos y Gemini está listo! Generando recomendación.")

            // PROMPT AUTOMÁTICO GENERALISTA: No asume un cultivo específico.
            val autoPromptContent = "Eres un asistente experto en agronomía. Analiza exclusivamente los datos de los sensores proporcionados, que son los únicos disponibles para este cultivo. Basándote en estos datos, ofrece una evaluación del estado actual de la tierra/planta y sugerencias de acciones generales para mantener o mejorar las condiciones. Si un dato de sensor es 'N/D', explícalo brevemente y sugiere qué información falta o cómo podría afectar la recomendación. Responde de forma clara, concisa, profesional y fácil de entender, evitando el lenguaje técnico excesivo. No asumas un cultivo específico."

            // Construye el prompt completo para Gemini usando createGeminiPrompt
            val fullAutoPrompt = createGeminiPrompt(
                userQuestion = autoPromptContent, // La instrucción es la "pregunta" para este caso
                tempAmb = currentTemperaturaAmbiental,
                humAmb = currentHumedadAmbiental,
                humSuelo = currentHumedadSuelo,
                uvIndex = currentIndiceUv,
                voltajeUva = currentVoltajeUva,
                nitrogeno = currentNitrogeno,
                fosforo = currentFosforo,
                potasio = currentPotasio,
                plantType = "no especificado" // Para la recomendación automática, no especificamos tipo de planta inicialmente
            )

            Log.d(TAG, "Prompt automático para Gemini: $fullAutoPrompt")
            // Se envía el prompt general y los datos de los sensores a generateGeminiResponse
            generateGeminiResponse(
                prompt = fullAutoPrompt, // <--- CORRECCIÓN APLICADA AQUÍ: Se pasa el resultado de createGeminiPrompt al parámetro 'prompt'
                targetTextView = tvGeminiRecommendation,
                tempAmb = currentTemperaturaAmbiental, // Se pasan los datos de los sensores
                humAmb = currentHumedadAmbiental,
                humSuelo = currentHumedadSuelo,
                uvIndex = currentIndiceUv,
                voltajeUva = currentVoltajeUva,
                nitrogeno = currentNitrogeno,
                fosforo = currentFosforo,
                potasio = currentPotasio,
                isAutomaticRecommendation = true
            )
        } else if (!::generativeModel.isInitialized || geminiApiKey.isNullOrEmpty() || geminiApiKey == "YOUR_GEMINI_API_KEY_HERE") {
            if (isAdded) {
                val errorText = "ERROR: Clave API de Gemini no configurada o modelo no inicializado para recomendación automática."
                tvGeminiRecommendation.text = errorText
                Log.e(TAG, "Recomendación Gemini omitida: $errorText")
            }
        } else {
            // Este es el caso donde faltan algunos datos. El mensaje aquí es importante.
            if (isAdded) {
                val missingSensors = mutableListOf<String>()
                if (currentTemperaturaAmbiental == -1.0) missingSensors.add("Temperatura Ambiental")
                if (currentHumedadAmbiental == -1.0) missingSensors.add("Humedad Ambiental")
                if (currentHumedadSuelo == -1.0) missingSensors.add("Humedad del Suelo")
                if (currentIndiceUv == -1.0) missingSensors.add("Índice UV")
                if (currentVoltajeUva == -1.0) missingSensors.add("Voltaje UVA")
                if (currentNitrogeno == -1) missingSensors.add("Nitrógeno (N)")
                if (currentFosforo == -1) missingSensors.add("Fósforo (P)")
                if (currentPotasio == -1) missingSensors.add("Potasio (K)")

                val missingText = if (missingSensors.isNotEmpty()) {
                    "Esperando datos de los siguientes sensores: ${missingSensors.joinToString()}. "
                } else {
                    "" // No debería ocurrir si la condición if principal falló por -1, pero para seguridad.
                }
                tvGeminiRecommendation.text = "${missingText}Por favor, asegúrate de que todos los sensores estén enviando datos y la app los esté recibiendo para la recomendación automática."
            }
            Log.d(TAG, "Recomendación Gemini omitida: No todos los datos de los sensores DISPONIBLES están aún.")
        }
    }

    /**
     * Construye el prompt para la API de Gemini, combinando el rol de la IA,
     * los datos de los sensores y la pregunta/solicitud específica.
     * @param userQuestion La pregunta o solicitud del usuario.
     * @param tempAmb Temperatura Ambiental.
     * @param humAmb Humedad Ambiental.
     * @param humSuelo Humedad del Suelo.
     * @param uvIndex Índice UV.
     * @param voltajeUva Voltaje UVA.
     * @param nitrogeno Nitrógeno.
     * @param fosforo Fósforo.
     * @param potasio Potasio.
     * @param plantType (Opcional) Tipo de cultivo, si es conocido.
     */
    private fun createGeminiPrompt(
        userQuestion: String,
        tempAmb: Double, humAmb: Double, humSuelo: Double, uvIndex: Double, voltajeUva: Double,
        nitrogeno: Int, fosforo: Int, potasio: Int,
        plantType: String = "no especificado" // Por defecto, no especificado.
    ): String {
        // Formatea los valores de los sensores a cadenas legibles, o "N/D" si no están disponibles.
        val tempAmbStr = if (tempAmb != -1.0) "$tempAmb°C" else "N/D"
        val humAmbStr = if (humAmb != -1.0) "$humAmb%" else "N/D"
        val humSueloStr = if (humSuelo != -1.0) "$humSuelo%" else "N/D"
        val uvIndexStr = if (uvIndex != -1.0) "$uvIndex" else "N/D"
        val voltajeUvaStr = if (voltajeUva != -1.0) "$voltajeUva V" else "N/D"
        val nitrogenoStr = if (nitrogeno != -1) "$nitrogeno mg/kg" else "N/D"
        val fosforoStr = if (fosforo != -1) "$fosforo mg/kg" else "N/D"
        val potasioStr = if (potasio != -1) "$potasio mg/kg" else "N/D"

        // ** Sistema de rol para la IA (System Prompt) - CLAVE para la flexibilidad.**
        // Se le dice a Gemini que actúe como un experto general y use SOLO los sensores proporcionados.
        // Si necesita el tipo de cultivo para una pregunta, se le indica que lo solicite.
        val systemPrompt = """
        Eres un asistente de inteligencia artificial experto en agronomía, agricultura y cultivos en general. Tu objetivo es proporcionar recomendaciones y consejos útiles a los agricultores. DEBES BASAR TUS RESPUESTAS EXCLUSIVAMENTE EN LOS SIGUIENTES DATOS DE SENSORES Y TU VASTO CONOCIMIENTO GENERAL. Estos son los ÚNICOS sensores disponibles y la única información de contexto ambiental y de suelo que tienes. Responde de forma clara, concisa, profesional y fácil de entender para cualquier persona, evitando el lenguaje técnico excesivo.

        Si algún dato de sensor es 'N/D' (No Disponible), explícalo brevemente y sugiere qué información falta o cómo podría afectar la recomendación. Sin embargo, NO pidas ni menciones datos de sensores que no sean los que te he proporcionado (ej. no preguntes por temperatura del suelo o NDVI si no se los proporciono).

        Si la pregunta del usuario es sobre un cultivo específico y el tipo de cultivo actual no ha sido especificado (es decir, 'no especificado'), puedes preguntar al usuario por el tipo de cultivo para ofrecer una respuesta más precisa. No asumas un tipo de cultivo si no se te ha dado.

        Datos de los sensores disponibles:
        - Temperatura Ambiental: $tempAmbStr
        - Humedad Ambiental: $humAmbStr
        - Humedad del Suelo: $humSueloStr
        - Índice UV: $uvIndexStr
        - Voltaje UVA del sensor (indicador de la intensidad de luz UV-A): $voltajeUvaStr
        - Nitrógeno (N) en el suelo: $nitrogenoStr
        - Fósforo (P) en el suelo: $fosforoStr
        - Potasio (K) en el suelo: $potasioStr
        ${if (plantType != "no especificado") "- Tipo de Cultivo actual: $plantType" else ""}
        """.trimIndent()

        // Combina el system prompt con la pregunta/solicitud específica del usuario.
        return "$systemPrompt\n\nPregunta/Solicitud del usuario: $userQuestion".trimIndent()
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

        // Construye el prompt combinado con la pregunta del usuario y los datos de los sensores.
        // Aquí NO se añade ningún contexto de cultivo automáticamente a la pregunta del usuario.
        // La IA usará los datos de los sensores y el system prompt para responder de forma general,
        // o preguntará por el tipo de cultivo si lo considera necesario.
        val combinedPrompt = createGeminiPrompt(
            userQuestion = userQuery, // La pregunta del usuario es la solicitud específica
            tempAmb = currentTemperaturaAmbiental,
            humAmb = currentHumedadAmbiental,
            humSuelo = currentHumedadSuelo,
            uvIndex = currentIndiceUv,
            voltajeUva = currentVoltajeUva,
            nitrogeno = currentNitrogeno,
            fosforo = currentFosforo,
            potasio = currentPotasio,
            plantType = "no especificado" // Dejarlo como "no especificado" por defecto para preguntas del usuario
        )
        Log.d(TAG, "Prompt combinado para Gemini (usuario): $combinedPrompt")

        // Llama a la función genérica para obtener y mostrar la respuesta de Gemini.
        generateGeminiResponse(
            prompt = combinedPrompt, // Se pasa el resultado de createGeminiPrompt al parámetro 'prompt'
            targetTextView = tvUserQueryResponse,
            tempAmb = currentTemperaturaAmbiental, // Se pasan los datos de los sensores
            humAmb = currentHumedadAmbiental,
            humSuelo = currentHumedadSuelo,
            uvIndex = currentIndiceUv,
            voltajeUva = currentVoltajeUva,
            nitrogeno = currentNitrogeno,
            fosforo = currentFosforo,
            potasio = currentPotasio,
            plantType = "no especificado", // Mantener para la firma, aunque el prompt ya lo maneja
            isAutomaticRecommendation = false
        )
        binding.etUserQuery.text.clear() // Limpiar campo de texto del usuario después de enviar.
    }

    /**
     * Función genérica para interactuar con la API de Gemini y actualizar un TextView.
     * @param prompt La cadena de texto que se enviará a Gemini.
     * @param targetTextView El TextView donde se mostrará la respuesta.
     * @param plantType (Opcional) Tipo de cultivo, si es conocido.
     * @param isAutomaticRecommendation (Nuevo) Para diferenciar el comportamiento si es necesario.
     *
     * Nota: Los parámetros de sensores son redundantes aquí si se usan en createGeminiPrompt,
     * pero los mantengo para consistencia en la firma si se necesitara una lógica diferente
     * dentro de esta función más adelante. Sin embargo, el 'prompt' ya los incluye.
     */
    private fun generateGeminiResponse(
        prompt: String,
        targetTextView: TextView,
        tempAmb: Double, humAmb: Double, humSuelo: Double, uvIndex: Double, voltajeUva: Double,
        nitrogeno: Int, fosforo: Int, potasio: Int,
        plantType: String = "no especificado",
        isAutomaticRecommendation: Boolean = false
    ) {
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

        // Muestra un mensaje de "Consultando..." al iniciar la llamada a la API.
        lifecycleScope.launch(Dispatchers.Main) {
            if (isAdded) {
                targetTextView.text = "Consultando a Gemini..."
            }
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
                            targetTextView.text = "Gemini no pudo generar una respuesta válida. Intenta de nuevo."
                            Log.w(TAG, "Gemini generó una respuesta nula o vacía para el prompt: '$prompt'")
                        }
                    }
                }
            } catch (e: Exception) {
                // Manejo de errores en caso de fallo de la API.
                Log.e(TAG, "Error llamando a Gemini API: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        targetTextView.text = "Error al obtener respuesta de Gemini: ${e.message}\nPor favor, verifica tu conexión a internet o intenta de nuevo más tarde."
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