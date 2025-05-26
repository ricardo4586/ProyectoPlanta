package com.example.proyectoplata.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.proyectoplata.BuildConfig // Importar BuildConfig para la clave API
import com.example.proyectoplata.databinding.FragmentHomeBinding // Importar View Binding
import com.example.proyectoplata.network.WeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!! // Getter para acceder al binding de forma segura

    private val weatherRepository = WeatherRepository()
    private val apiKey = BuildConfig.OPEN_WEATHER_API_KEY // Acceder a la clave API de forma segura

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.obtenerButton.setOnClickListener {
            val ciudadNombre = binding.ciudadNombre.text.toString().trim()
            val codigoIso = binding.codigoIso.text.toString().trim()

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
                    binding.temperaturaActual.text = "Actual: ${weather.main.temp}°C"
                    binding.temperaturaMinima.text = "Mínima: ${weather.main.temp_min}°C"
                    binding.temperaturaMaxima.text = "Máxima: ${weather.main.temp_max}°C"
                } else {
                    Toast.makeText(requireContext(), "Error al obtener el clima. Verifique ciudad/código o conexión.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Liberar la referencia al binding para evitar memory leaks
    }
}