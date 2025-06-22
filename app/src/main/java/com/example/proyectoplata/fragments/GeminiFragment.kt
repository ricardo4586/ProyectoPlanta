package com.example.proyectoplata.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
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
    // Inicializadas con valores que indican "no disponible" (-1.0 para Double, -1 para Int)
    private var currentHumedadAmbiental: Double = -1.0
    private var currentHumedadSuelo: Double = -1.0
    private var currentTemperaturaAmbiental: Double = -1.0
    private var currentIndiceUv: Double = -1.0
    private var currentVoltajeUva: Double = -1.0 // Para el sensor de luz UVA
    private var currentNitrogeno: Int = -1
    private var currentFosforo: Int = -1
    private var currentPotasio: Int = -1

    // Elementos de UI
    private lateinit var etUserQuery: EditText
    private lateinit var btnSendQuery: Button // Corregido: eliminado el 'var' duplicado
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
        // Esto es útil si el fragmento se crea una sola vez y luego se reanuda.
        attemptGeminiRecommendation()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d(TAG, "onDestroyView: GeminiFragment view destruida.")
    }

    /**
     * Configura los observadores para los LiveData de los sensores en el SharedSensorViewModel.
     * Cada vez que un dato de sensor cambia, se actualiza la variable local y se intenta
     * generar una nueva recomendación automática.
     */
    /**
     * Configura los observadores para los LiveData de los sensores en el SharedSensorViewModel.
     * Cada vez que un dato de sensor cambia, se actualiza la variable local y se intenta
     * generar una nueva recomendación automática.
     */
    private fun observeSensorData() {
        Log.d(TAG, "observeSensorData: Configurando observadores para datos de sensores.")
        sharedSensorViewModel.temperature.observe(viewLifecycleOwner) { temp ->
            if (currentTemperaturaAmbiental != temp) { // Evita actualizaciones innecesarias si el valor no cambia
                currentTemperaturaAmbiental = temp
                Log.d(TAG, "Observed: Temp Amb = $currentTemperaturaAmbiental. Attempting Gemini Rec.")
                attemptGeminiRecommendation()
            }
        }
        sharedSensorViewModel.humidity.observe(viewLifecycleOwner) { hum ->
            if (currentHumedadAmbiental != hum) {
                currentHumedadAmbiental = hum // ¡Aquí está la corrección!
                Log.d(TAG, "Observed: Hum Amb = $currentHumedadAmbiental. Attempting Gemini Rec.")
                attemptGeminiRecommendation()
            }
        }
        sharedSensorViewModel.humiditySuelo.observe(viewLifecycleOwner) { humSuelo ->
            if (currentHumedadSuelo != humSuelo) {
                currentHumedadSuelo = humSuelo
                Log.d(TAG, "Observed: Hum Suelo = $humSuelo. Attempting Gemini Rec.")
                attemptGeminiRecommendation()
            }
        }
        sharedSensorViewModel.uvIndex.observe(viewLifecycleOwner) { uv ->
            if (currentIndiceUv != uv) {
                currentIndiceUv = uv
                Log.d(TAG, "Observed: UV = $uv. Attempting Gemini Rec.")
                attemptGeminiRecommendation()
            }
        }
        sharedSensorViewModel.light.observe(viewLifecycleOwner) { lightVal ->
            if (currentVoltajeUva != lightVal) {
                currentVoltajeUva = lightVal
                Log.d(TAG, "Observed: Voltaje UVA = $lightVal. Attempting Gemini Rec.")
                attemptGeminiRecommendation()
            }
        }
        sharedSensorViewModel.nitrogeno.observe(viewLifecycleOwner) { nitro ->
            if (currentNitrogeno != nitro) {
                currentNitrogeno = nitro
                Log.d(TAG, "Observed: Nitrogeno = $nitro. Attempting Gemini Rec.")
                attemptGeminiRecommendation()
            }
        }
        sharedSensorViewModel.fosforo.observe(viewLifecycleOwner) { phos ->
            if (currentFosforo != phos) {
                currentFosforo = phos
                Log.d(TAG, "Observed: Fosforo = $phos. Attempting Gemini Rec.")
                attemptGeminiRecommendation()
            }
        }
        sharedSensorViewModel.potasio.observe(viewLifecycleOwner) { pot ->
            if (currentPotasio != pot) {
                currentPotasio = pot
                Log.d(TAG, "Observed: Potasio = $pot. Attempting Gemini Rec.")
                attemptGeminiRecommendation()
            }
        }
    }

    /**
     * Intenta generar una recomendación de Gemini automáticamente si todos los datos
     * de los sensores están disponibles (no son -1.0 o -1) y la clave API de Gemini está configurada.
     * Esta recomendación se basa EXCLUSIVAMENTE en los datos de los sensores.
     */
    private fun attemptGeminiRecommendation() {
        Log.d(TAG, "attemptGeminiRecommendation: Iniciando verificación de datos para recomendación automática.")
        Log.d(TAG, "Valores de sensores actuales: TempAmb=$currentTemperaturaAmbiental, HumAmb=$currentHumedadAmbiental, HumSuelo=$currentHumedadSuelo, UV=$currentIndiceUv, UVA=$currentVoltajeUva, NPK_N=$currentNitrogeno, NPK_P=$currentFosforo, NPK_K=$currentPotasio")

        // Verificar que todos los datos necesarios de los sensores estén disponibles
        val allSensorsAvailable =
            currentTemperaturaAmbiental != -1.0 &&
                    currentHumedadAmbiental != -1.0 &&
                    currentHumedadSuelo != -1.0 &&
                    currentIndiceUv != -1.0 &&
                    currentVoltajeUva != -1.0 &&
                    currentNitrogeno != -1 &&
                    currentFosforo != -1 &&
                    currentPotasio != -1

        if (::generativeModel.isInitialized && !geminiApiKey.isNullOrEmpty() && geminiApiKey != "YOUR_GEMINI_API_KEY_HERE" && allSensorsAvailable) {
            Log.d(TAG, "attemptGeminiRecommendation: ¡Todos los datos de sensores DISPONIBLES son válidos y Gemini está listo! Generando recomendación.")

            // Instrucción para la recomendación automática:
            // Es estricta para evitar que pida información adicional y mencione limitaciones.
            val autoRecommendationRequest = """
                Analiza los siguientes datos de los sensores y proporciona una recomendación de cuidado directa y concisa.
                Para cada sensor, resume su estado actual y luego ofrece una sugerencia de acción si es necesario.
                **NO** menciones que la información es parcial, que faltan datos, ni solicites información adicional (como tipo de cultivo, suelo, etapa de crecimiento, etc.).
                Tu respuesta debe ser puramente descriptiva del estado de los sensores y prescriptiva con base en ellos.
                Mantenlo profesional y fácil de entender.
                Ejemplo de formato:
                - Temperatura Ambiental: [Valor]. Condición: [Condición]. Sugerencia: [Sugerencia].
                - Humedad Ambiental: [Valor]. Condición: [Condición]. Sugerencia: [Sugerencia].
                - Humedad del Suelo: [Valor]. Condición: [Condición]. Sugerencia: [Sugerencia].
                - Índice UV: [Valor]. Condición: [Condición]. Sugerencia: [Sugerencia].
                - Voltaje UVA (Intensidad de Luz UV-A): [Valor]. Condición: [Condición]. Sugerencia: [Sugerencia].
                - Nitrógeno (N) en el suelo: [Valor]. Condición: [Condición]. Sugerencia: [Sugerencia].
                - Fósforo (P) en el suelo: [Valor]. Condición: [Condición]. Sugerencia: [Sugerencia].
                - Potasio (K) en el suelo: [Valor]. Condición: [Condición]. Sugerencia: [Sugerencia].
                Termina la respuesta con un resumen general.
            """.trimIndent()

            // Construye el prompt completo para Gemini usando createGeminiPrompt.
            val fullAutoPrompt = createGeminiPrompt(
                userQuestion = autoRecommendationRequest, // Le pasamos la solicitud de recomendación
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
            // Se envía el prompt a generateGeminiResponse para obtener la respuesta.
            generateGeminiResponse(
                prompt = fullAutoPrompt,
                targetTextView = tvGeminiRecommendation
            )
        } else if (!::generativeModel.isInitialized || geminiApiKey.isNullOrEmpty() || geminiApiKey == "YOUR_GEMINI_API_KEY_HERE") {
            if (isAdded) {
                val errorText = "ERROR: Clave API de Gemini no configurada o modelo no inicializado para recomendación automática."
                tvGeminiRecommendation.text = errorText
                Log.e(TAG, "Recomendación Gemini omitida: $errorText")
            }
        } else {
            // Este es el caso donde faltan algunos datos de los sensores.
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
                    "Esperando datos de los siguientes sensores para la recomendación automática: ${missingSensors.joinToString()}. "
                } else {
                    "" // No debería ocurrir si la condición if principal falló por -1, pero para seguridad.
                }
                tvGeminiRecommendation.text = "${missingText}Por favor, asegúrate de que todos los sensores estén enviando datos y la app los esté recibiendo."
            }
            Log.d(TAG, "Recomendación Gemini omitida: No todos los datos de los sensores están aún disponibles o válidos.")
        }
    }

    /**
     * Construye el prompt para la API de Gemini, combinando el rol de la IA,
     * los datos de los sensores y la pregunta/solicitud específica.
     * Este es el "cerebro" de la instrucción para Gemini.
     *
     * @param userQuestion La pregunta o solicitud específica del usuario o una instrucción de recomendación automática.
     * @param tempAmb Temperatura Ambiental.
     * @param humAmb Humedad Ambiental.
     * @param humSuelo Humedad del Suelo.
     * @param uvIndex Índice UV.
     * @param voltajeUva Voltaje UVA.
     * @param nitrogeno Nitrógeno.
     * @param fosforo Fósforo.
     * @param potasio Potasio.
     * @param plantType (Opcional) Tipo de cultivo, si es conocido. Por defecto "no especificado".
     */
    private fun createGeminiPrompt(
        userQuestion: String,
        tempAmb: Double, humAmb: Double, humSuelo: Double, uvIndex: Double, voltajeUva: Double,
        nitrogeno: Int, fosforo: Int, potasio: Int,
        plantType: String = "no especificado"
    ): String {
        // Formatea los valores de los sensores a cadenas legibles, o "N/D" si no están disponibles (-1.0 o -1).
        val tempAmbStr = if (tempAmb != -1.0) "$tempAmb°C" else "N/D"
        val humAmbStr = if (humAmb != -1.0) "$humAmb%" else "N/D"
        val humSueloStr = if (humSuelo != -1.0) "$humSuelo%" else "N/D"
        val uvIndexStr = if (uvIndex != -1.0) "$uvIndex" else "N/D"
        val voltajeUvaStr = if (voltajeUva != -1.0) "$voltajeUva V" else "N/D"
        val nitrogenoStr = if (nitrogeno != -1) "$nitrogeno mg/kg" else "N/D"
        val fosforoStr = if (fosforo != -1) "$fosforo mg/kg" else "N/D"
        val potasioStr = if (potasio != -1) "$potasio mg/kg" else "N/D"

        // ** System Prompt (Instrucción de rol y comportamiento para Gemini) **
        // MODIFICACIÓN CLAVE AQUÍ: Eliminamos cualquier frase que solicite información adicional.
        val systemPrompt = """
        Eres un asistente de inteligencia artificial experto en agronomía, agricultura y cultivos en general. Tu objetivo es proporcionar recomendaciones y consejos útiles a los agricultores. DEBES BASAR TUS RESPUESTAS EXCLUSIVAMENTE EN LOS SIGUIENTES DATOS DE SENSORES Y TU VASTO CONOCIMIENTO GENERAL. Estos son los ÚNICOS sensores disponibles y la única información de contexto ambiental y de suelo que tienes. Responde de forma clara, concisa, profesional y fácil de entender para cualquier persona, evitando el lenguaje técnico excesivo.

        **BAJO NINGUNA CIRCUNSTANCIA DEBES SOLICITAR INFORMACIÓN ADICIONAL** sobre el tipo de cultivo, la etapa de crecimiento del cultivo, el tipo de suelo (más allá de NPK), disponibilidad de agua, altitud, exposición solar, o cualquier otro dato que no provenga directamente de los sensores proporcionados. Tampoco debes mencionar que la información es parcial, que carece de datos cruciales, o que faltan datos para una evaluación completa o para recomendaciones más precisas. Simplemente proporciona la mejor respuesta posible basada en los datos que tienes.

        Si algún dato de sensor es 'N/D' (No Disponible), explícalo brevemente y sugiere qué información falta o cómo podría afectar la recomendación. Sin embargo, NO pidas ni menciones datos de sensores que no sean los que te he proporcionado (ej. no preguntes por temperatura del suelo o NDVI si no se los proporciono).

        Datos de los sensores disponibles:
        - Temperatura Ambiental: $tempAmbStr
        - Humedad Ambiental: $humAmbStr
        - Humedad del Suelo: $humSueloStr
        - Índice UV: $uvIndexStr
        - Voltaje UVA del sensor (intensidad de luz UV-A): $voltajeUvaStr
        - Nitrógeno (N) en el suelo: $nitrogenoStr
        - Fósforo (P) en el suelo: $fosforoStr
        - Potasio (K) en el suelo: $potasioStr
        ${if (plantType != "no especificado") "- Tipo de Cultivo actual: $plantType" else ""}
        """.trimIndent()

        // Combina el system prompt con la pregunta/solicitud específica del usuario.
        return "$systemPrompt\n\nSolicitud del usuario: $userQuestion".trimIndent()
    }

    /**
     * Envía la consulta del usuario a la API de Gemini y muestra la respuesta en tvUserQueryResponse.
     * Esta función construye un prompt basado en la pregunta del usuario y los datos actuales de los sensores.
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
        // Aquí se pasa la pregunta del usuario directamente como 'userQuestion'.
        val combinedPrompt = createGeminiPrompt(
            userQuestion = userQuery,
            tempAmb = currentTemperaturaAmbiental,
            humAmb = currentHumedadAmbiental,
            humSuelo = currentHumedadSuelo,
            uvIndex = currentIndiceUv,
            voltajeUva = currentVoltajeUva,
            nitrogeno = currentNitrogeno,
            fosforo = currentFosforo,
            potasio = currentPotasio,
            plantType = "no especificado" // Dejarlo como "no especificado" por defecto para preguntas generales del usuario
        )
        Log.d(TAG, "Prompt combinado para Gemini (usuario): $combinedPrompt")

        // Llama a la función genérica para obtener y mostrar la respuesta de Gemini.
        generateGeminiResponse(
            prompt = combinedPrompt,
            targetTextView = tvUserQueryResponse
        )
        binding.etUserQuery.text.clear() // Limpiar campo de texto del usuario después de enviar.
    }

    /**
     * Función genérica para interactuar con la API de Gemini y actualizar un TextView.
     * Esta función es ahora más simple, ya que el 'prompt' ya contiene toda la información
     * necesaria (datos de sensores, contexto, y pregunta/solicitud).
     *
     * @param prompt La cadena de texto COMPLETA que se enviará a Gemini.
     * @param targetTextView El TextView donde se mostrará la respuesta.
     */
    private fun generateGeminiResponse(
        prompt: String,
        targetTextView: TextView
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
                        if (!recommendation.isNullOrEmpty()) {
                            targetTextView.text = recommendation
                            Log.d(TAG, "TextView '${
                                when(targetTextView.id) {
                                    tvGeminiRecommendation.id -> "tvGeminiRecommendation"
                                    tvUserQueryResponse.id -> "tvUserQueryResponse"
                                    else -> "Unknown TextView" // Añade esta línea
                                }
                            }' actualizado con: '$recommendation'")
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
}