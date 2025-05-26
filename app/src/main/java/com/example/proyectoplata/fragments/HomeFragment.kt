package com.example.proyectoplata.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.proyectoplata.R
import com.example.proyectoplata.network.WeatherRepository // Asegúrate de que esta ruta sea correcta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private lateinit var codigoIsoEditText: EditText
    private lateinit var ciudadNombreEditText: EditText
    private lateinit var obtenerButton: Button
    private lateinit var temperaturaActualTextView: TextView
    private lateinit var temperaturaMinimaTextView: TextView
    private lateinit var temperaturaMaximaTextView: TextView

    // Instanciamos el repositorio y la API Key aquí, ya que el botón "Obtener" está en este fragmento
    private val weatherRepository = WeatherRepository()
    private val apiKey = "aa8782089df8fb9de8b95f66b22f29f9" // Usar tu propia API Key

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Inicialización de vistas que están DENTRO de fragment_home.xml
        codigoIsoEditText = view.findViewById(R.id.codigo_iso)
        ciudadNombreEditText = view.findViewById(R.id.ciudad_nombre)
        obtenerButton = view.findViewById(R.id.obtener_button)
        temperaturaActualTextView = view.findViewById(R.id.temperatura_actual)
        temperaturaMinimaTextView = view.findViewById(R.id.temperatura_minima)
        temperaturaMaximaTextView = view.findViewById(R.id.temperatura_maxima)

        // Configuración del click listener para el botón "Obtener"
        obtenerButton.setOnClickListener {
            val ciudadNombre = ciudadNombreEditText.text.toString()
            val codigoIso = codigoIsoEditText.text.toString()

            if (ciudadNombre.isNotEmpty() && codigoIso.isNotEmpty()) {
                val fullQuery = "$ciudadNombre,$codigoIso"
                fetchWeatherData(fullQuery)
            } else {
                Toast.makeText(requireContext(), "Por favor, ingrese la ciudad y el código ISO", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun fetchWeatherData(fullQuery: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val weather = weatherRepository.fetchWeatherData(fullQuery, apiKey)

            withContext(Dispatchers.Main) {
                if (weather != null) {
                    temperaturaActualTextView.text = "Actual: ${weather.main.temp}°C"
                    temperaturaMinimaTextView.text = "Mínima: ${weather.main.temp_min}°C"
                    temperaturaMaximaTextView.text = "Máxima: ${weather.main.temp_max}°C"
                } else {
                    Toast.makeText(requireContext(), "Error al obtener el clima", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}