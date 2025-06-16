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
import androidx.annotation.NonNull
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Observer
import com.example.proyectoplata.BuildConfig
import com.example.proyectoplata.databinding.FragmentHomeBinding
import com.example.proyectoplata.SharedSensorViewModel
import com.example.proyectoplata.models.NPKData
import com.example.proyectoplata.models.SensorValueData
import com.example.proyectoplata.network.ApiService
import com.example.proyectoplata.network.WeatherRepository
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.ai.client.generativeai.GenerativeModel // Mantenemos esta importación porque GeminiFragment la usa
import com.google.ai.client.generativeai.type.content // Mantenemos esta importación
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class HomeFragment : Fragment() {

    private val TAG = "HomeFragment"
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // Firebase
    private lateinit var firebaseDatabase: FirebaseDatabase
    // ELIMINADO: private lateinit var temperaturaAmbientalRef: DatabaseReference
    private lateinit var temperaturaSueloRef: DatabaseReference // Aún la mantendremos si tienes sensor de suelo
    private lateinit var humedadAmbientalRef: DatabaseReference
    private lateinit var humedadSueloRef: DatabaseReference
    private lateinit var indiceUvRef: DatabaseReference
    private lateinit var voltajeUvaRef: DatabaseReference
    private lateinit var npkRef: DatabaseReference

    // ViewModel
    private lateinit var sharedSensorViewModel: SharedSensorViewModel

    // UI elements (solo para mostrar datos de sensores y clima, no de Gemini)
    // ELIMINADO: private lateinit var tvTemperaturaActual: TextView
    private lateinit var tvHumedadActual: TextView
    private lateinit var tvHumedadSueloActual: TextView
    private lateinit var tvIndiceUvActual: TextView
    private lateinit var tvVoltajeUvaActual: TextView
    private lateinit var tvNitrogenoActual: TextView
    private lateinit var tvFosforoActual: TextView
    private lateinit var tvPotasioActual: TextView

    // Clima
    private lateinit var ciudadNombreEditText: EditText
    private lateinit var codigoIsoEditText: EditText
    private lateinit var obtenerButton: Button
    private lateinit var temperaturaActualClimaTextView: TextView
    private lateinit var temperaturaMinimaClimaTextView: TextView
    private lateinit var temperaturaMaximaClimaTextView: TextView

    // Claves API
    private val openWeatherApiKey = BuildConfig.OPEN_WEATHER_API_KEY
    // Mantenemos la clave de Gemini, aunque la lógica principal ahora esté en GeminiFragment
    private val geminiApiKey = BuildConfig.GEMINI_API_KEY
    private lateinit var generativeModel: GenerativeModel // Se mantiene por si se necesita para GeminiFragment

    // Variables para almacenar los últimos valores de los sensores (para el prompt de Gemini)
    private var currentHumedadAmbiental: Double = -1.0
    private var currentHumedadSuelo: Double = -1.0
    private var currentTemperaturaAmbiental: Double = -1.0 // Se mantiene pero ya no se actualiza desde Firebase aquí
    private var currentTemperaturaSuelo: Double = -1.0
    private var currentIndiceUv: Double = -1.0
    private var currentVoltajeUva: Double = -1.0
    private var currentNitrogeno: Int = -1
    private var currentFosforo: Int = -1
    private var currentPotasio: Int = -1

    // Elementos de UI de Gemini (ELIMINADOS de este layout y fragment, pero se mantienen como variables Dummy por ahora)
    // Esto es solo para evitar errores de compilación si otras partes del código los esperan.
    // Lo ideal es que estas no existan aquí si la UI de Gemini se movió por completo.
    private lateinit var etUserQuery: EditText
    private lateinit var btnSendQuery: Button
    private lateinit var tvGeminiRecommendation: TextView
    private lateinit var tvUserQueryResponse: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedSensorViewModel = ViewModelProvider(requireActivity()).get(SharedSensorViewModel::class.java)
        Log.d(TAG, "onCreate: SharedSensorViewModel inicializado.")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val view = binding.root
        Log.d(TAG, "onCreateView: HomeFragment layout inflado.")

        // Inicializar TextViews y otros elementos de UI
        // ELIMINADO: tvTemperaturaActual = binding.tvTemperaturaActual
        tvHumedadActual = binding.tvHumedadActual
        tvHumedadSueloActual = binding.tvHumedadSueloActual
        tvIndiceUvActual = binding.tvIndiceUvActual
        tvVoltajeUvaActual = binding.tvVoltajeUvaActual
        tvNitrogenoActual = binding.tvNitrogenoActual
        tvFosforoActual = binding.tvFosforoActual
        tvPotasioActual = binding.tvPotasioActual
        Log.d(TAG, "onCreateView: TextViews de sensores (excepto Temp Amb) inicializados.")


        // Inicializar elementos de UI del clima
        ciudadNombreEditText = binding.ciudadNombre
        codigoIsoEditText = binding.codigoIso
        obtenerButton = binding.obtenerButton
        temperaturaActualClimaTextView = binding.temperaturaActual
        temperaturaMinimaClimaTextView = binding.temperaturaMinima
        temperaturaMaximaClimaTextView = binding.temperaturaMaxima
        Log.d(TAG, "onCreateView: Elementos de UI del clima inicializados.")

        // Inicializar elementos de UI de Gemini - DUMMY porque ya no están en este layout
        // Esto es para evitar errores de referencia si otros lugares aún esperan que existan
        // aunque lo ideal es que estos sean eliminados si la UI de Gemini se movió completamente.
        etUserQuery = EditText(requireContext()) // Dummy
        btnSendQuery = Button(requireContext()) // Dummy
        tvGeminiRecommendation = TextView(requireContext()) // Dummy
        tvUserQueryResponse = TextView(requireContext()) // Dummy

        // Inicialización de Gemini Model (solo para evitar errores de inicialización si se llama a attemptGeminiRecommendation)
        // La lógica principal de Gemini debe estar en GeminiFragment.kt
        if (geminiApiKey.isNullOrEmpty() || geminiApiKey == "YOUR_GEMINI_API_KEY_HERE") {
            Log.e(TAG, "ERROR: La clave API de Gemini no se encontró o no está configurada correctamente en BuildConfig.")
            // No actualizamos la UI aquí, ya que la UI de Gemini no está en este fragmento.
        } else {
            generativeModel = GenerativeModel(modelName = "gemini-1.5-flash", apiKey = geminiApiKey)
            Log.d(TAG, "Gemini Model inicializado en HomeFragment (solo para evitar NullPointerException si attemptGeminiRecommendation es llamada por error).")
        }


        // Validación de OpenWeatherMap API Key
        if (openWeatherApiKey.isNullOrEmpty()) {
            Log.e(TAG, "ERROR: La clave API de OpenWeatherMap no se encontró o no está configurada en BuildConfig.")
            Toast.makeText(requireContext(), "Advertencia: Clave API de OpenWeatherMap no configurada.", Toast.LENGTH_LONG).show()
        }

        // Inicializar referencias de Firebase
        firebaseDatabase = FirebaseDatabase.getInstance()
        // ELIMINADO: temperaturaAmbientalRef = firebaseDatabase.getReference("sensores/temperatura_ambiental")
        temperaturaSueloRef = firebaseDatabase.getReference("sensores/temperatura_suelo")
        humedadAmbientalRef = firebaseDatabase.getReference("sensores/humedad_ambiental") // Corregido a _ambiental
        humedadSueloRef = firebaseDatabase.getReference("sensores/humedad_suelo")
        indiceUvRef = firebaseDatabase.getReference("sensores/indice_uv")
        voltajeUvaRef = firebaseDatabase.getReference("sensores/voltaje_uva")
        npkRef = firebaseDatabase.getReference("sensores/npk")
        Log.d(TAG, "onCreateView: Referencias de Firebase inicializadas (sin Temp Amb).")

        // Click Listeners
        obtenerButton.setOnClickListener {
            val ciudadNombre = ciudadNombreEditText.text.toString().trim()
            val codigoIso = codigoIsoEditText.text.toString().trim()

            if (openWeatherApiKey.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Error: Clave API de OpenWeatherMap no configurada.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (ciudadNombre.isNotEmpty() && codigoIso.isNotEmpty()) {
                fetchWeatherData(fullQuery = "$ciudadNombre,$codigoIso")
            } else {
                Toast.makeText(requireContext(), "Por favor, ingrese la ciudad y el código ISO", Toast.LENGTH_SHORT).show()
            }
        }
        Log.d(TAG, "onCreateView: Click Listener de obtener clima configurado.")

        // No es necesario configurar el click listener para btnSendQuery aquí, ya que su UI fue movida.
        // Se mantiene la llamada a observeViewModel y readSensorData, pero sin Temp Ambiental.
        observeViewModel()
        readSensorData()
        Log.d(TAG, "onCreateView: Observación de ViewModel y lectura de datos de sensor iniciadas (sin Temp Amb).")

        return view
    }

    private fun fetchWeatherData(fullQuery: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val weather = WeatherRepository().fetchWeatherData(fullQuery, openWeatherApiKey)

                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        if (weather != null) {
                            binding.temperaturaActual.text = "Actual: ${weather.main.temp}°C"
                            binding.temperaturaMinima.text = "Mínima: ${weather.main.temp_min}°C"
                            binding.temperaturaMaxima.text = "Máxima: ${weather.main.temp_max}°C"
                            Log.d(TAG, "fetchWeatherData: Clima obtenido y UI actualizada: Temp=${weather.main.temp}")
                        } else {
                            Toast.makeText(requireContext(), "Error al obtener el clima. Verifique ciudad/código o conexión.", Toast.LENGTH_SHORT).show()
                            binding.temperaturaActual.text = "Actual: N/A"
                            binding.temperaturaMinima.text = "Mínima: N/A"
                            binding.temperaturaMaxima.text = "Mínima: N/A"
                            Log.w(TAG, "fetchWeatherData: No se pudo obtener datos de clima.")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching weather data: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Error de red al obtener el clima: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        binding.temperaturaActual.text = "Actual: N/A"
                        binding.temperaturaMinima.text = "Mínima: N/A"
                        binding.temperaturaMaxima.text = "Mínima: N/A"
                    }
                }
            }
        }
    }

    /**
     * Configura los listeners de Firebase para leer los datos de los sensores
     * y actualizar el SharedSensorViewModel.
     * La lógica de 'attemptGeminiRecommendation()' solo se mantiene para la estructura,
     * la llamada real se controlará en GeminiFragment.kt.
     */
    private fun readSensorData() {
        Log.d(TAG, "readSensorData: Iniciando listeners de Firebase para sensores (sin Temp Amb).")

        // ELIMINADO: Listener para Temperatura Ambiental

        // Listener para Temperatura del Suelo
        temperaturaSueloRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(@NonNull snapshot: DataSnapshot) {
                Log.d(TAG, "TS: onDataChange - Snapshot recibido. Existe: ${snapshot.exists()}, Hijos: ${snapshot.childrenCount}")
                var latestTemperatureSuelo: Double = -1.0
                val lastChild = snapshot.children.lastOrNull()
                lastChild?.let {
                    val sensorValueData = it.getValue(SensorValueData::class.java)
                    sensorValueData?.let { data ->
                        latestTemperatureSuelo = data.valor
                        sharedSensorViewModel.updateTemperatureSuelo(latestTemperatureSuelo)
                        currentTemperaturaSuelo = latestTemperatureSuelo // Actualiza para el prompt de Gemini
                        Log.d(TAG, "TS: Temperatura del Suelo publicada en ViewModel: $latestTemperatureSuelo °C")
                    } ?: Log.w(TAG, "TS: SensorValueData es nulo para el último hijo: ${it.key}. Contenido: ${it.value}.")
                } ?: Log.d(TAG, "TS: No hay hijos en el snapshot o snapshot vacío.")

                if (!snapshot.exists() || lastChild == null || latestTemperatureSuelo == -1.0) {
                    Log.d(TAG, "TS: Datos de temperatura del suelo no válidos o no encontrados. Valor actual en ViewModel: ${sharedSensorViewModel.temperatureSuelo.value}")
                }
            }
            override fun onCancelled(@NonNull error: DatabaseError) {
                Log.e(TAG, "TS: Error al leer temperatura del suelo: ${error.message}", error.toException())
                if (isAdded) Toast.makeText(requireContext(), "Error de Firebase Temperatura Suelo: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        // Listener para Humedad Ambiental
        humedadAmbientalRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(@NonNull snapshot: DataSnapshot) {
                Log.d(TAG, "HA: onDataChange - Snapshot recibido. Existe: ${snapshot.exists()}, Hijos: ${snapshot.childrenCount}")
                var latestHumidity: Double = -1.0
                val lastChild = snapshot.children.lastOrNull()
                lastChild?.let {
                    val sensorValueData = it.getValue(SensorValueData::class.java)
                    sensorValueData?.let { data ->
                        latestHumidity = data.valor
                        sharedSensorViewModel.updateHumidity(latestHumidity)
                        currentHumedadAmbiental = latestHumidity // Actualiza para el prompt de Gemini
                        Log.d(TAG, "HA: Humedad Ambiental publicada en ViewModel: $latestHumidity %")
                    } ?: Log.w(TAG, "HA: SensorValueData es nulo para el último hijo: ${it.key}. Contenido: ${it.value}.")
                } ?: Log.d(TAG, "HA: No hay hijos en el snapshot o snapshot vacío.")

                if (!snapshot.exists() || lastChild == null || latestHumidity == -1.0) {
                    Log.d(TAG, "HA: Datos de humedad ambiental no válidos o no encontrados. Valor actual en ViewModel: ${sharedSensorViewModel.humidity.value}")
                }
            }
            override fun onCancelled(@NonNull error: DatabaseError) {
                Log.e(TAG, "HA: Error al leer humedad ambiental: ${error.message}", error.toException())
                if (isAdded) Toast.makeText(requireContext(), "Error de Firebase Humedad: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        // Listener para Humedad del Suelo
        humedadSueloRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(@NonNull snapshot: DataSnapshot) {
                Log.d(TAG, "HS: onDataChange - Snapshot recibido. Existe: ${snapshot.exists()}, Hijos: ${snapshot.childrenCount}")
                var latestHumiditySuelo: Double = -1.0
                val lastChild = snapshot.children.lastOrNull()
                lastChild?.let {
                    val sensorValueData = it.getValue(SensorValueData::class.java)
                    sensorValueData?.let { data ->
                        latestHumiditySuelo = data.valor
                        sharedSensorViewModel.updateHumiditySuelo(latestHumiditySuelo)
                        currentHumedadSuelo = latestHumiditySuelo // Actualiza para el prompt de Gemini
                        Log.d(TAG, "HS: Humedad del Suelo publicada en ViewModel: $latestHumiditySuelo %")
                    } ?: Log.w(TAG, "HS: SensorValueData es nulo para el último hijo: ${it.key}. Contenido: ${it.value}.")
                } ?: Log.d(TAG, "HS: No hay hijos en el snapshot o snapshot vacío.")

                if (!snapshot.exists() || lastChild == null || latestHumiditySuelo == -1.0) {
                    Log.d(TAG, "HS: Datos de humedad del suelo no válidos o no encontrados. Valor actual en ViewModel: ${sharedSensorViewModel.humiditySuelo.value}")
                }
            }
            override fun onCancelled(@NonNull error: DatabaseError) {
                Log.e(TAG, "HS: Error al leer humedad del suelo: ${error.message}", error.toException())
                if (isAdded) Toast.makeText(requireContext(), "Error de Firebase Humedad Suelo: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        // Listener para Indice UV
        indiceUvRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(@NonNull snapshot: DataSnapshot) {
                Log.d(TAG, "UV: onDataChange - Snapshot recibido. Existe: ${snapshot.exists()}, Hijos: ${snapshot.childrenCount}")
                var latestUvIndex: Double = -1.0
                val lastChild = snapshot.children.lastOrNull()
                lastChild?.let {
                    val sensorValueData = it.getValue(SensorValueData::class.java)
                    sensorValueData?.let { data ->
                        latestUvIndex = data.valor
                        sharedSensorViewModel.updateUvIndex(latestUvIndex)
                        currentIndiceUv = latestUvIndex // Actualiza para el prompt de Gemini
                        Log.d(TAG, "UV: Índice UV publicado en ViewModel: $latestUvIndex")
                    } ?: Log.w(TAG, "UV: SensorValueData es nulo para el último hijo: ${it.key}. Contenido: ${it.value}.")
                } ?: Log.d(TAG, "UV: No hay hijos en el snapshot o snapshot vacío.")

                if (!snapshot.exists() || lastChild == null || latestUvIndex == -1.0) {
                    Log.d(TAG, "UV: Datos de índice UV no válidos o no encontrados. Valor actual en ViewModel: ${sharedSensorViewModel.uvIndex.value}")
                }
            }
            override fun onCancelled(@NonNull error: DatabaseError) {
                Log.e(TAG, "UV: Error al leer índice UV: ${error.message}", error.toException())
                if (isAdded) Toast.makeText(requireContext(), "Error de Firebase Índice UV: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        // Listener para Voltaje UVA
        voltajeUvaRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(@NonNull snapshot: DataSnapshot) {
                Log.d(TAG, "UVA: onDataChange - Snapshot recibido. Existe: ${snapshot.exists()}, Hijos: ${snapshot.childrenCount}")
                var latestVoltajeUva: Double = -1.0
                val lastChild = snapshot.children.lastOrNull()
                lastChild?.let {
                    val sensorValueData = it.getValue(SensorValueData::class.java)
                    sensorValueData?.let { data ->
                        latestVoltajeUva = data.valor
                        sharedSensorViewModel.updateLight(latestVoltajeUva)
                        currentVoltajeUva = latestVoltajeUva // Actualiza para el prompt de Gemini
                        Log.d(TAG, "UVA: Voltaje UVA publicado en ViewModel: $latestVoltajeUva V")
                    } ?: Log.w(TAG, "UVA: SensorValueData es nulo para el último hijo: ${it.key}. Contenido: ${it.value}.")
                } ?: Log.d(TAG, "UVA: No hay hijos en el snapshot o snapshot vacío.")

                if (!snapshot.exists() || lastChild == null || latestVoltajeUva == -1.0) {
                    Log.d(TAG, "UVA: Datos de voltaje UVA no válidos o no encontrados. Valor actual en ViewModel: ${sharedSensorViewModel.light.value}")
                }
            }
            override fun onCancelled(@NonNull error: DatabaseError) {
                Log.e(TAG, "UVA: Error al leer voltaje UVA: ${error.message}", error.toException())
                if (isAdded) Toast.makeText(requireContext(), "Error de Firebase Voltaje UVA: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        // Listener para NPK
        npkRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(@NonNull snapshot: DataSnapshot) {
                Log.d(TAG, "NPK: onDataChange - Snapshot recibido. Existe: ${snapshot.exists()}, Hijos: ${snapshot.childrenCount}")
                var latestNitrogeno: Int = -1
                var latestFosforo: Int = -1
                var latestPotasio: Int = -1
                val lastChild = snapshot.children.lastOrNull()
                lastChild?.let {
                    val npkData = it.getValue(NPKData::class.java)
                    npkData?.let { data ->
                        latestNitrogeno = data.nitrogeno
                        latestFosforo = data.fosforo
                        latestPotasio = data.potasio
                        Log.d(TAG, "NPK: NPK leído: N=$latestNitrogeno, P=$latestFosforo, K=$latestPotasio")
                    } ?: Log.w(TAG, "NPK: NPKData es nulo para el último hijo: ${it.key}. Contenido: ${it.value}.")
                } ?: Log.d(TAG, "NPK: No hay hijos en el snapshot o snapshot vacío.")

                // Actualizar siempre el ViewModel, incluso si los datos son -1
                sharedSensorViewModel.updateNitrogeno(latestNitrogeno)
                sharedSensorViewModel.updateFosforo(latestFosforo)
                sharedSensorViewModel.updatePotasio(latestPotasio)
                currentNitrogeno = latestNitrogeno // Actualiza para el prompt de Gemini
                currentFosforo = latestFosforo     // Actualiza para el prompt de Gemini
                currentPotasio = latestPotasio     // Actualiza para el prompt de Gemini
                Log.d(TAG, "NPK: NPK publicado en ViewModel: N=${latestNitrogeno}, P=${latestFosforo}, K=${latestPotasio}")

                if (!snapshot.exists() || lastChild == null || latestNitrogeno == -1 || latestFosforo == -1 || latestPotasio == -1) {
                    Log.d(TAG, "NPK: Datos NPK no válidos o no encontrados. Valores actuales en ViewModel: N=${sharedSensorViewModel.nitrogeno.value}, P=${sharedSensorViewModel.fosforo.value}, K=${sharedSensorViewModel.potasio.value}")
                }
            }
            override fun onCancelled(@NonNull error: DatabaseError) {
                Log.e(TAG, "NPK: Error al leer datos NPK: ${error.message}", error.toException())
                if (isAdded) Toast.makeText(requireContext(), "Error de Firebase NPK: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /**
     * Observa los LiveData del SharedSensorViewModel para actualizar la UI de HomeFragment.
     */
    private fun observeViewModel() {
        Log.d(TAG, "observeViewModel: Iniciando observación de LiveData para UI de HomeFragment.")
        // ELIMINADO: sharedSensorViewModel.temperature.observe(viewLifecycleOwner, Observer { temp ->
        // ELIMINADO:    tvTemperaturaActual.text = "Temperatura Ambiental: $temp °C"
        // ELIMINADO:    Log.d(TAG, "UI Update: tvTemperaturaActual = $temp")
        // ELIMINADO: })

        sharedSensorViewModel.humidity.observe(viewLifecycleOwner, Observer { hum ->
            tvHumedadActual.text = "Humedad Ambiental: $hum%"
            Log.d(TAG, "UI Update: tvHumedadActual = $hum")
        })

        sharedSensorViewModel.humiditySuelo.observe(viewLifecycleOwner, Observer { humSuelo ->
            tvHumedadSueloActual.text = "Humedad del Suelo: $humSuelo%"
            Log.d(TAG, "UI Update: tvHumedadSueloActual = $humSuelo")
        })

        sharedSensorViewModel.uvIndex.observe(viewLifecycleOwner, Observer { uv ->
            tvIndiceUvActual.text = "Índice UV: $uv"
            Log.d(TAG, "UI Update: tvIndiceUvActual = $uv")
        })

        sharedSensorViewModel.light.observe(viewLifecycleOwner, Observer { lightVal ->
            tvVoltajeUvaActual.text = "Voltaje UVA: $lightVal V"
            Log.d(TAG, "UI Update: tvVoltajeUvaActual = $lightVal")
        })

        sharedSensorViewModel.nitrogeno.observe(viewLifecycleOwner, Observer { nitro ->
            tvNitrogenoActual.text = "Nitrógeno (N): $nitro"
            Log.d(TAG, "UI Update: tvNitrogenoActual = $nitro")
        })

        sharedSensorViewModel.fosforo.observe(viewLifecycleOwner, Observer { phos ->
            tvFosforoActual.text = "Fósforo (P): $phos"
            Log.d(TAG, "UI Update: tvFosforoActual = $phos")
        })

        sharedSensorViewModel.potasio.observe(viewLifecycleOwner, Observer { pot ->
            tvPotasioActual.text = "Potasio (K): $pot"
            Log.d(TAG, "UI Update: tvPotasioActual = $pot")
        })
    }

    // Estas funciones attemptGeminiRecommendation, convertDateToTimestamp, createGeminiPrompt y sendUserQueryToGemini
    // NO DEBERÍAN ESTAR EN HOMEFRAGMENT.KT ya que la lógica de Gemini se movió a GeminiFragment.kt.
    // Las mantendremos aquí como DUMMYs temporales y vacías para evitar errores de compilación
    // si alguna otra parte del código aún las llama por error. La implementación real está en GeminiFragment.kt.

    private fun attemptGeminiRecommendation() {
        // No hacer nada aquí, la lógica real está en GeminiFragment
        Log.d(TAG, "attemptGeminiRecommendation: Ignorado en HomeFragment. La lógica real está en GeminiFragment.")
    }

    private fun convertDateToTimestamp(dateString: String): Long {
        // No hacer nada aquí
        return -1L
    }

    private fun createGeminiPrompt(
        userQuestion: String,
        tempAmb: Float, humAmb: Float, tempSuelo: Float, uvIndex: Float, voltajeUva: Float,
        nitrogeno: Int, fosforo: Int, potasio: Int
    ): String {
        // No hacer nada aquí
        return ""
    }

    private fun sendUserQueryToGemini(userQuery: String) {
        // No hacer nada aquí, la lógica real está en GeminiFragment
        Log.d(TAG, "sendUserQueryToGemini: Ignorado en HomeFragment. La lógica real está en GeminiFragment.")
    }

    private fun generateGeminiResponse(prompt: String, targetTextView: TextView) {
        // No hacer nada aquí
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d(TAG, "onDestroyView: HomeFragment view destruida.")
    }
}
