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
import com.example.proyectoplata.BuildConfig // Para la API Key de OpenWeatherMap
import com.example.proyectoplata.databinding.FragmentHomeBinding // Para ViewBinding
import com.example.proyectoplata.SharedSensorViewModel // ViewModel compartido
import com.example.proyectoplata.models.NPKData // Modelo de datos para NPK
// import com.example.proyectoplata.network.ApiService // Ya no necesitas importar ApiService directamente aquí, WeatherRepository lo maneja
import com.example.proyectoplata.models.WeatherResponse // Modelo de respuesta de clima
import com.example.proyectoplata.network.WeatherRepository // <-- ¡IMPORTACIÓN AÑADIDA!
import com.github.mikephil.charting.data.Entry // Para gráficos (si los implementas con MPAndroidChart)
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers // Para manejo de coroutines
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Normalizer // Importar para Normalizer
import java.text.SimpleDateFormat // Importar para SimpleDateFormat
import java.util.* // Para Locale, Calendar y Date


/**
 * Fragmento principal que muestra los datos actuales de los sensores,
 * permite consultar el clima y actúa como un punto central para la observación
 * de datos de sensores a través de un ViewModel compartido.
 */
class HomeFragment : Fragment() {

    private val TAG = "HomeFragment"
    // _binding es una referencia nullable al objeto ViewBinding
    // Se inicializa en onCreateView y se limpia en onDestroyView para evitar memory leaks.
    private var _binding: FragmentHomeBinding? = null
    // binding es una propiedad que proporciona acceso no-nullable al ViewBinding,
    // solo se usa cuando _binding no es nulo (entre onCreateView y onDestroyView).
    private val binding get() = _binding!!

    // Firebase Realtime Database
    private lateinit var firebaseDatabase: FirebaseDatabase
    // Referencias a la base de datos para cada tipo de sensor.
    // Asume que la estructura de la base de datos es "mediciones/{tipo_sensor}/{timestamp}: valor".
    private lateinit var temperaturaAmbientalRef: DatabaseReference
    private lateinit var temperaturaSueloRef: DatabaseReference
    private lateinit var humedadAmbientalRef: DatabaseReference
    private lateinit var humedadSueloRef: DatabaseReference
    private lateinit var indiceUvRef: DatabaseReference
    private lateinit var voltajeUvaRef: DatabaseReference
    private lateinit var npkRef: DatabaseReference

    // ViewModel compartido para manejar el estado de los datos de los sensores.
    private lateinit var sharedSensorViewModel: SharedSensorViewModel

    // Elementos de la UI para mostrar los datos actuales de los sensores.
    private lateinit var tvTemperaturaActual: TextView
    private lateinit var tvHumedadActual: TextView
    private lateinit var tvHumedadSueloActual: TextView
    private lateinit var tvIndiceUvActual: TextView
    private lateinit var tvVoltajeUvaActual: TextView
    private lateinit var tvNitrogenoActual: TextView
    private lateinit var tvFosforoActual: TextView
    private lateinit var tvPotasioActual: TextView

    // Elementos de la UI para la funcionalidad de clima.
    private lateinit var ciudadNombreEditText: EditText
    private lateinit var paisNombreEditText: EditText
    private lateinit var obtenerButton: Button
    private lateinit var temperaturaActualClimaTextView: TextView
    private lateinit var temperaturaMinimaClimaTextView: TextView
    private lateinit var temperaturaMaximaClimaTextView: TextView

    // Clave API para OpenWeatherMap (obtenida de BuildConfig).
    private val openWeatherApiKey = BuildConfig.OPEN_WEATHER_API_KEY

    // Instancia perezosa (lazy) de WeatherRepository
    private val weatherRepository: WeatherRepository by lazy { WeatherRepository() }

    /**
     * Se llama cuando el fragmento es creado por primera vez.
     * Aquí se inicializa el ViewModel compartido.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedSensorViewModel = ViewModelProvider(requireActivity()).get(SharedSensorViewModel::class.java)
        Log.d(TAG, "onCreate: SharedSensorViewModel inicializado.")
    }

    /**
     * Se llama para que el fragmento instancie su diseño de interfaz de usuario.
     * Aquí se infla el layout, se inicializan los elementos de la UI
     * y se configuran los listeners.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflar el layout utilizando ViewBinding
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val view = binding.root
        Log.d(TAG, "onCreateView: HomeFragment layout inflado.")

        // Inicializar los TextViews de sensores usando ViewBinding.
        tvTemperaturaActual = binding.tvTemperature
        tvHumedadActual = binding.tvHumidity
        tvHumedadSueloActual = binding.tvHumiditySoil
        tvIndiceUvActual = binding.tvUvIndex
        tvVoltajeUvaActual = binding.tvUvaVoltage
        tvNitrogenoActual = binding.tvNitrogen
        tvFosforoActual = binding.tvPhosphorus
        tvPotasioActual = binding.tvPotassium
        Log.d(TAG, "onCreateView: TextViews de sensores inicializados.")

        // Inicializar elementos de UI relacionados con el clima.
        ciudadNombreEditText = binding.tilCityName.editText!!
        paisNombreEditText = binding.tilCountryName.editText!!
        obtenerButton = binding.btnGetClimate
        temperaturaActualClimaTextView = binding.tvClimateActualValue
        temperaturaMinimaClimaTextView = binding.tvClimateMinimaValue
        temperaturaMaximaClimaTextView = binding.tvClimateMaximaValue
        Log.d(TAG, "onCreateView: Elementos de UI del clima inicializados.")

        // Advertencia si la clave API de OpenWeatherMap no está configurada.
        if (openWeatherApiKey.isNullOrEmpty() || openWeatherApiKey == "YOUR_OPEN_WEATHER_API_KEY_HERE") {
            Log.e(TAG, "ERROR: La clave API de OpenWeatherMap no se encontró o no está configurada en BuildConfig.")
            Toast.makeText(requireContext(), "Advertencia: Clave API de OpenWeatherMap no configurada.", Toast.LENGTH_LONG).show()
        }

        // Inicializar Firebase Realtime Database y sus referencias.
        firebaseDatabase = FirebaseDatabase.getInstance()
        temperaturaAmbientalRef = firebaseDatabase.getReference("mediciones/temperatura_ambiental")
        temperaturaSueloRef = firebaseDatabase.getReference("mediciones/temperatura_suelo")
        humedadAmbientalRef = firebaseDatabase.getReference("mediciones/humedad_ambiental")
        humedadSueloRef = firebaseDatabase.getReference("mediciones/humedad_suelo")
        indiceUvRef = firebaseDatabase.getReference("mediciones/indice_uv")
        voltajeUvaRef = firebaseDatabase.getReference("mediciones/voltaje_uva")
        npkRef = firebaseDatabase.getReference("mediciones/npk")
        Log.d(TAG, "onCreateView: Referencias de Firebase Realtime Database inicializadas a 'mediciones/'.")

        // Configurar el click listener para el botón de obtener clima.
        obtenerButton.setOnClickListener {
            val ciudadNombre = ciudadNombreEditText.text.toString().trim()
            val paisNombre = paisNombreEditText.text.toString().trim()

            if (openWeatherApiKey.isNullOrEmpty() || openWeatherApiKey == "YOUR_OPEN_WEATHER_API_KEY_HERE") {
                Toast.makeText(requireContext(), "Error: Clave API de OpenWeatherMap no configurada.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (ciudadNombre.isNotEmpty() && paisNombre.isNotEmpty()) {
                // Intenta obtener el código ISO del país para la API de clima.
                val isoCode = getIsoCodeForCountry(paisNombre)
                if (isoCode != null) {
                    fetchWeatherData(fullQuery = "$ciudadNombre,$isoCode")
                } else {
                    Toast.makeText(requireContext(), "País no reconocido. Por favor, intente con otro nombre.", Toast.LENGTH_SHORT).show()
                }

            } else {
                Toast.makeText(requireContext(), "Por favor, ingrese la ciudad y el país", Toast.LENGTH_SHORT).show()
            }
        }
        Log.d(TAG, "onCreateView: Click Listener de obtener clima configurado.")

        // Iniciar la observación del ViewModel y la lectura de datos de sensores.
        observeViewModel()
        readSensorData()
        Log.d(TAG, "onCreateView: Observación de ViewModel y lectura de datos de sensor iniciadas.")

        return view
    }

    /**
     * Función helper para obtener el código ISO de 2 letras de un país dado su nombre.
     * Esta función ha sido mejorada para reconocer cualquier país del mundo
     * utilizando la clase `Locale` y normalizando la entrada para manejar tildes.
     */
    private fun getIsoCodeForCountry(countryName: String): String? {
        // Normaliza la cadena de entrada: quita tildes y convierte a minúsculas.
        // Ejemplo: "Perú" -> "Peru" -> "peru"
        val normalizedCountryName = Normalizer.normalize(countryName, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .lowercase(Locale.ROOT)

        // Itera a través de todos los códigos ISO de países disponibles.
        // Para cada código, obtiene el nombre del país y lo normaliza para la comparación.
        for (isoCountryCode in Locale.getISOCountries()) {
            val locale = Locale("", isoCountryCode)
            val name = locale.getDisplayCountry(Locale.getDefault()) // Obtiene el nombre del país en el idioma por defecto del dispositivo
            val normalizedName = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
                .lowercase(Locale.ROOT)

            if (normalizedCountryName == normalizedName) {
                return isoCountryCode
            }
        }
        return null // Si no se encuentra ninguna coincidencia.
    }

    /**
     * Función que obtiene los datos climáticos de la API de OpenWeatherMap
     * utilizando el WeatherRepository y actualiza la UI.
     */
    private fun fetchWeatherData(fullQuery: String) {
        // Lanza una coroutine en el ámbito del ciclo de vida del fragmento en el hilo de IO.
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Llama al repositorio para obtener los datos del clima.
                // AHORA ESPERAMOS un retrofit2.Response<WeatherResponse>
                val weatherResponse = weatherRepository.fetchWeatherData(fullQuery, openWeatherApiKey)

                // Vuelve al hilo principal para actualizar la UI.
                withContext(Dispatchers.Main) {
                    // Solo actualiza la UI si el fragmento todavía está activo y adjunto a una actividad.
                    if (isAdded) {
                        if (weatherResponse.isSuccessful) {
                            val weather = weatherResponse.body() // Obtenemos el cuerpo de la respuesta
                            if (weather != null) {
                                // Actualiza los TextViews del clima con los datos obtenidos.
                                binding.tvClimateActualValue.text = "%.1f°C".format(weather.main.temp)
                                binding.tvClimateMinimaValue.text = "%.1f°C".format(weather.main.temp_min)
                                binding.tvClimateMaximaValue.text = "%.1f°C".format(weather.main.temp_max)
                                Log.d(TAG, "fetchWeatherData: Clima obtenido y UI actualizada: Actual=${weather.main.temp}, Minima=${weather.main.temp_min}, Maxima=${weather.main.temp_max}")
                            } else {
                                // La llamada fue exitosa, pero el cuerpo de la respuesta fue nulo
                                Toast.makeText(requireContext(), "Error al parsear datos de clima. Intente de nuevo.", Toast.LENGTH_SHORT).show()
                                binding.tvClimateActualValue.text = "N/A"
                                binding.tvClimateMinimaValue.text = "N/A"
                                binding.tvClimateMaximaValue.text = "N/A"
                                Log.w(TAG, "fetchWeatherData: Cuerpo de respuesta de clima nulo.")
                            }
                        } else {
                            // La llamada a la API no fue exitosa (código de error HTTP)
                            Toast.makeText(requireContext(), "Error al obtener el clima: ${weatherResponse.code()} - ${weatherResponse.message()}", Toast.LENGTH_SHORT).show()
                            binding.tvClimateActualValue.text = "N/A"
                            binding.tvClimateMinimaValue.text = "N/A"
                            binding.tvClimateMaximaValue.text = "N/A"
                            Log.w(TAG, "fetchWeatherData: Fallo en la llamada API de clima. Código: ${weatherResponse.code()}, Mensaje: ${weatherResponse.message()}")
                        }
                    }
                }
            } catch (e: Exception) {
                // Manejo de errores de red o excepciones durante la llamada a la API.
                Log.e(TAG, "Error fetching weather data: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Error de red al obtener el clima: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        binding.tvClimateActualValue.text = "N/A"
                        binding.tvClimateMinimaValue.text = "N/A"
                        binding.tvClimateMaximaValue.text = "N/A"
                    }
                }
            }
        }
    }

    /**
     * Configura los listeners de Firebase Realtime Database para leer los datos de los sensores
     * en tiempo real. Cada vez que los datos cambian en la base de datos,
     * este método actualiza el SharedSensorViewModel, que a su vez actualiza la UI.
     */
    private fun readSensorData() {
        Log.d(TAG, "readSensorData: Iniciando listeners de Firebase Realtime Database para sensores.")

        // Definir el SimpleDateFormat una vez para el formato de tus claves en Firebase
        // Ejemplo: "2025-06-18_13-29-16"
        val firebaseKeyDateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ROOT)
        firebaseKeyDateFormat.timeZone = TimeZone.getTimeZone("UTC") // Asegura que la zona horaria sea consistente si tus timestamps son UTC

        // Helper para convertir la clave de cadena a Long timestamp
        fun parseKeyToTimestamp(key: String?): Long? {
            return key?.let {
                try {
                    firebaseKeyDateFormat.parse(it)?.time // .time devuelve el timestamp en milisegundos
                } catch (e: Exception) {
                    Log.e(TAG, "Error al parsear la clave de Firebase '$it': ${e.message}")
                    null
                }
            }
        }


        // Listener para Temperatura Ambiental
        temperaturaAmbientalRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(@NonNull snapshot: DataSnapshot) {
                Log.d(TAG, "TA: onDataChange - Snapshot recibido. Existe: ${snapshot.exists()}, Hijos: ${snapshot.childrenCount}")
                var latestTemperature: Double = -1.0 // Valor por defecto si no se encuentran datos
                val temperatureEntries = ArrayList<Entry>() // Para datos históricos, útiles para gráficos

                // Itera sobre todos los hijos (que se esperan sean claves de fecha/hora)
                for (child in snapshot.children) {
                    // PARSEAMOS LA CLAVE DE CADENA A LONG TIMESTAMP
                    val timestampKey = parseKeyToTimestamp(child.key)
                    val value = child.getValue(Double::class.java) // Lee el valor como Double

                    if (timestampKey != null && value != null) {
                        // Añade para el gráfico (X = timestamp, Y = valor)
                        temperatureEntries.add(Entry(timestampKey.toFloat(), value.toFloat()))
                        latestTemperature = value // El último valor en la iteración es el más reciente
                    }
                }
                // Ordena las entradas por el timestamp (eje X) para asegurar la visualización correcta en gráficos.
                temperatureEntries.sortBy { it.x }

                sharedSensorViewModel.updateTemperature(latestTemperature) // Actualiza el valor actual
                sharedSensorViewModel.updateTemperatureEntries(temperatureEntries) // Actualiza los datos para el gráfico
                Log.d(TAG, "TA: Temperatura Ambiental publicada en ViewModel: $latestTemperature °C. Entries para gráfico: ${temperatureEntries.size}")

                if (!snapshot.exists() || temperatureEntries.isEmpty()) {
                    Log.d(TAG, "TA: Datos de temperatura ambiental no válidos o no encontrados. Mostrando valor N/D en UI.")
                }
            }
            override fun onCancelled(@NonNull error: DatabaseError) {
                Log.e(TAG, "TA: Error al leer temperatura ambiental: ${error.message}", error.toException())
                if (isAdded) Toast.makeText(requireContext(), "Error Firebase Temperatura Ambiental: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        // Listener para Temperatura del Suelo (estructura similar al anterior)
        temperaturaSueloRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(@NonNull snapshot: DataSnapshot) {
                Log.d(TAG, "TS: onDataChange - Snapshot recibido. Existe: ${snapshot.exists()}, Hijos: ${snapshot.childrenCount}")
                var latestTemperatureSuelo: Double = -1.0
                val temperatureSueloEntries = ArrayList<Entry>()

                for (child in snapshot.children) {
                    // PARSEAMOS LA CLAVE DE CADENA A LONG TIMESTAMP
                    val timestampKey = parseKeyToTimestamp(child.key)
                    val value = child.getValue(Double::class.java)

                    if (timestampKey != null && value != null) {
                        temperatureSueloEntries.add(Entry(timestampKey.toFloat(), value.toFloat()))
                        latestTemperatureSuelo = value
                    }
                }
                temperatureSueloEntries.sortBy { it.x }
                sharedSensorViewModel.updateTemperatureSuelo(latestTemperatureSuelo)
                sharedSensorViewModel.updateTemperatureSueloEntries(temperatureSueloEntries)
                Log.d(TAG, "TS: Temperatura del Suelo publicada en ViewModel: $latestTemperatureSuelo °C. Entries para gráfico: ${temperatureSueloEntries.size}")

                if (!snapshot.exists() || temperatureSueloEntries.isEmpty()) {
                    Log.d(TAG, "TS: Datos de temperatura del suelo no válidos o no encontrados. Mostrando valor N/D en UI.")
                }
            }
            override fun onCancelled(@NonNull error: DatabaseError) {
                Log.e(TAG, "TS: Error al leer temperatura del suelo: ${error.message}", error.toException())
                if (isAdded) Toast.makeText(requireContext(), "Error Firebase Temperatura Suelo: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        // Listener para Humedad Ambiental
        humedadAmbientalRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(@NonNull snapshot: DataSnapshot) {
                Log.d(TAG, "HA: onDataChange - Snapshot recibido. Existe: ${snapshot.exists()}, Hijos: ${snapshot.childrenCount}")
                var latestHumidity: Double = -1.0
                val humidityEntries = ArrayList<Entry>()

                for (child in snapshot.children) {
                    // PARSEAMOS LA CLAVE DE CADENA A LONG TIMESTAMP
                    val timestampKey = parseKeyToTimestamp(child.key)
                    val value = child.getValue(Double::class.java)

                    if (timestampKey != null && value != null) {
                        humidityEntries.add(Entry(timestampKey.toFloat(), value.toFloat()))
                        latestHumidity = value
                    }
                }
                humidityEntries.sortBy { it.x }
                sharedSensorViewModel.updateHumidity(latestHumidity)
                sharedSensorViewModel.updateHumidityEntries(humidityEntries)
                Log.d(TAG, "HA: Humedad Ambiental publicada en ViewModel: $latestHumidity %. Entries para gráfico: ${humidityEntries.size}")

                if (!snapshot.exists() || humidityEntries.isEmpty()) {
                    Log.d(TAG, "HA: Datos de humedad ambiental no válidos o no encontrados. Mostrando valor N/D en UI.")
                }
            }
            override fun onCancelled(@NonNull error: DatabaseError) {
                Log.e(TAG, "HA: Error al leer humedad ambiental: ${error.message}", error.toException())
                if (isAdded) Toast.makeText(requireContext(), "Error Firebase Humedad Ambiental: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        // Listener para Humedad del Suelo
        humedadSueloRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(@NonNull snapshot: DataSnapshot) {
                Log.d(TAG, "HS: onDataChange - Snapshot recibido. Existe: ${snapshot.exists()}, Hijos: ${snapshot.childrenCount}")
                var latestHumiditySuelo: Double = -1.0
                val humiditySueloEntries = ArrayList<Entry>()

                for (child in snapshot.children) {
                    // PARSEAMOS LA CLAVE DE CADENA A LONG TIMESTAMP
                    val timestampKey = parseKeyToTimestamp(child.key)
                    val value = child.getValue(Double::class.java)

                    if (timestampKey != null && value != null) {
                        humiditySueloEntries.add(Entry(timestampKey.toFloat(), value.toFloat()))
                        latestHumiditySuelo = value
                    }
                }
                humiditySueloEntries.sortBy { it.x }
                sharedSensorViewModel.updateHumiditySuelo(latestHumiditySuelo)
                sharedSensorViewModel.updateHumiditySueloEntries(humiditySueloEntries)
                Log.d(TAG, "HS: Humedad del Suelo publicada en ViewModel: $latestHumiditySuelo %. Entries para gráfico: ${humiditySueloEntries.size}")

                if (!snapshot.exists() || humiditySueloEntries.isEmpty()) {
                    Log.d(TAG, "HS: Datos de humedad del suelo no válidos o no encontrados. Mostrando valor N/D en UI.")
                }
            }
            override fun onCancelled(@NonNull error: DatabaseError) {
                Log.e(TAG, "HS: Error al leer humedad del suelo: ${error.message}", error.toException())
                if (isAdded) Toast.makeText(requireContext(), "Error Firebase Humedad Suelo: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        // Listener para Indice UV
        indiceUvRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(@NonNull snapshot: DataSnapshot) {
                Log.d(TAG, "UV: onDataChange - Snapshot recibido. Existe: ${snapshot.exists()}, Hijos: ${snapshot.childrenCount}")
                var latestUvIndex: Double = -1.0
                val uvIndexEntries = ArrayList<Entry>()

                for (child in snapshot.children) {
                    // PARSEAMOS LA CLAVE DE CADENA A LONG TIMESTAMP
                    val timestampKey = parseKeyToTimestamp(child.key)
                    val value = child.getValue(Double::class.java)

                    if (timestampKey != null && value != null) {
                        uvIndexEntries.add(Entry(timestampKey.toFloat(), value.toFloat()))
                        latestUvIndex = value
                    }
                }
                uvIndexEntries.sortBy { it.x }
                sharedSensorViewModel.updateUvIndex(latestUvIndex)
                sharedSensorViewModel.updateUvIndexEntries(uvIndexEntries)
                Log.d(TAG, "UV: Índice UV publicado en ViewModel: $latestUvIndex. Entries para gráfico: ${uvIndexEntries.size}")

                if (!snapshot.exists() || uvIndexEntries.isEmpty()) {
                    Log.d(TAG, "UV: Datos de índice UV no válidos o no encontrados. Mostrando valor N/D en UI.")
                }
            }
            override fun onCancelled(@NonNull error: DatabaseError) {
                Log.e(TAG, "UV: Error al leer índice UV: ${error.message}", error.toException())
                if (isAdded) Toast.makeText(requireContext(), "Error Firebase Índice UV: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        // Listener para Voltaje UVA (Luz)
        voltajeUvaRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(@NonNull snapshot: DataSnapshot) {
                Log.d(TAG, "UVA: onDataChange - Snapshot recibido. Existe: ${snapshot.exists()}, Hijos: ${snapshot.childrenCount}")
                var latestVoltajeUva: Double = -1.0
                val lightEntries = ArrayList<Entry>()

                for (child in snapshot.children) {
                    // PARSEAMOS LA CLAVE DE CADENA A LONG TIMESTAMP
                    val timestampKey = parseKeyToTimestamp(child.key)
                    val value = child.getValue(Double::class.java)

                    if (timestampKey != null && value != null) {
                        lightEntries.add(Entry(timestampKey.toFloat(), value.toFloat()))
                        latestVoltajeUva = value
                    }
                }
                lightEntries.sortBy { it.x }
                sharedSensorViewModel.updateLight(latestVoltajeUva)
                sharedSensorViewModel.updateLightEntries(lightEntries)
                Log.d(TAG, "UVA: Voltaje UVA publicado en ViewModel: $latestVoltajeUva V. Entries para gráfico: ${lightEntries.size}")

                if (!snapshot.exists() || lightEntries.isEmpty()) {
                    Log.d(TAG, "UVA: Datos de voltaje UVA no válidos o no encontrados. Mostrando valor N/D en UI.")
                }
            }
            override fun onCancelled(@NonNull error: DatabaseError) {
                Log.e(TAG, "UVA: Error al leer voltaje UVA: ${error.message}", error.toException())
                if (isAdded) Toast.makeText(requireContext(), "Error Firebase Voltaje UVA: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        // Listener para NPK (Nitrógeno, Fósforo, Potasio)
        npkRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(@NonNull snapshot: DataSnapshot) {
                Log.d(TAG, "NPK: onDataChange - Snapshot recibido. Existe: ${snapshot.exists()}, Hijos: ${snapshot.childrenCount}")
                var latestNitrogeno: Int = -1
                var latestFosforo: Int = -1
                var latestPotasio: Int = -1
                val nitrogenoEntries = ArrayList<Entry>()
                val fosforoEntries = ArrayList<Entry>()
                val potasioEntries = ArrayList<Entry>()

                // Itera sobre todos los hijos para obtener los valores NPK y construir los gráficos.
                for (child in snapshot.children) {
                    // PARSEAMOS LA CLAVE DE CADENA A LONG TIMESTAMP
                    val timestampKey = parseKeyToTimestamp(child.key)
                    // Se espera que cada hijo sea un objeto NPKData.
                    val npkData = child.getValue(NPKData::class.java)

                    if (timestampKey != null && npkData != null) {
                        // Añade entradas para cada componente NPK.
                        nitrogenoEntries.add(Entry(timestampKey.toFloat(), npkData.nitrogeno.toFloat()))
                        fosforoEntries.add(Entry(timestampKey.toFloat(), npkData.fosforo.toFloat()))
                        potasioEntries.add(Entry(timestampKey.toFloat(), npkData.potasio.toFloat()))

                        // El último valor en la iteración es el más reciente.
                        latestNitrogeno = npkData.nitrogeno
                        latestFosforo = npkData.fosforo
                        latestPotasio = npkData.potasio
                    }
                }
                // Ordena las entradas para asegurar la visualización correcta en gráficos.
                nitrogenoEntries.sortBy { it.x }
                fosforoEntries.sortBy { it.x }
                potasioEntries.sortBy { it.x }

                // Actualiza el ViewModel con los últimos valores NPK y sus entradas para gráficos.
                sharedSensorViewModel.updateNitrogeno(latestNitrogeno)
                sharedSensorViewModel.updateFosforo(latestFosforo)
                sharedSensorViewModel.updatePotasio(latestPotasio)
                sharedSensorViewModel.updateNitrogenoEntries(nitrogenoEntries)
                sharedSensorViewModel.updateFosforoEntries(fosforoEntries)
                sharedSensorViewModel.updatePotasioEntries(potasioEntries)

                Log.d(TAG, "NPK: NPK publicado en ViewModel: N=${latestNitrogeno}, P=${latestFosforo}, K=${latestPotasio}. Entries para gráfico: N=${nitrogenoEntries.size}, P=${fosforoEntries.size}, K=${potasioEntries.size}")

                if (!snapshot.exists() || nitrogenoEntries.isEmpty() || fosforoEntries.isEmpty() || potasioEntries.isEmpty()) {
                    Log.d(TAG, "NPK: Datos NPK no válidos o no encontrados. Mostrando valores N/D en UI.")
                }
            }
            override fun onCancelled(@NonNull error: DatabaseError) {
                Log.e(TAG, "NPK: Error al leer datos NPK: ${error.message}", error.toException())
                if (isAdded) Toast.makeText(requireContext(), "Error Firebase NPK: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /**
     * Observa los LiveData del SharedSensorViewModel.
     * Cada vez que un dato en el ViewModel cambia, este método actualiza
     * el TextView correspondiente en la UI del HomeFragment.
     */
    private fun observeViewModel() {
        Log.d(TAG, "observeViewModel: Iniciando observación de LiveData para UI de HomeFragment.")

        // Observador para la temperatura ambiental.
        sharedSensorViewModel.temperature.observe(viewLifecycleOwner, Observer { temp ->
            // Formatea el valor a un decimal.
            tvTemperaturaActual.text = "Temperatura Ambiental: %.1f °C".format(temp)
            Log.d(TAG, "UI Update: tvTemperaturaActual = $temp")
        })

        // Observador para la humedad ambiental.
        sharedSensorViewModel.humidity.observe(viewLifecycleOwner, Observer { hum ->
            tvHumedadActual.text = "Humedad Ambiental: %.1f %%".format(hum)
            Log.d(TAG, "UI Update: tvHumedadActual = $hum")
        })

        // Observador para la humedad del suelo.
        sharedSensorViewModel.humiditySuelo.observe(viewLifecycleOwner, Observer { humSuelo ->
            tvHumedadSueloActual.text = "Humedad del Suelo: %.1f %%".format(humSuelo)
            Log.d(TAG, "UI Update: tvHumedadSueloActual = $humSuelo")
        })

        // Observador para el índice UV.
        sharedSensorViewModel.uvIndex.observe(viewLifecycleOwner, Observer { uv ->
            tvIndiceUvActual.text = "Índice UV: %.1f".format(uv)
            Log.d(TAG, "UI Update: tvIndiceUvActual = $uv")
        })

        // Observador para el voltaje UVA (luz).
        sharedSensorViewModel.light.observe(viewLifecycleOwner, Observer { lightVal ->
            tvVoltajeUvaActual.text = "Voltaje UVA: %.1f V".format(lightVal)
            Log.d(TAG, "UI Update: tvVoltajeUvaActual = $lightVal")
        })

        // Observador para el nitrógeno (N).
        sharedSensorViewModel.nitrogeno.observe(viewLifecycleOwner, Observer { nitro ->
            tvNitrogenoActual.text = "Nitrógeno (N): $nitro"
            Log.d(TAG, "UI Update: tvNitrogenoActual = $nitro")
        })

        // Observador para el fósforo (P).
        sharedSensorViewModel.fosforo.observe(viewLifecycleOwner, Observer { fosfo ->
            tvFosforoActual.text = "Fósforo (P): $fosfo"
            Log.d(TAG, "UI Update: tvFosforoActual = $fosfo")
        })

        // Observador para el potasio (K).
        sharedSensorViewModel.potasio.observe(viewLifecycleOwner, Observer { potasio ->
            tvPotasioActual.text = "Potasio (K): $potasio"
            Log.d(TAG, "UI Update: tvPotasioActual = $potasio")
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d(TAG, "onDestroyView: HomeFragment view destruida.")
    }
}