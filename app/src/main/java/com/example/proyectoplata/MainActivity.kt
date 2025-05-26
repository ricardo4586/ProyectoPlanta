package com.example.proyectoplata

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectoplata.network.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    // Los elementos de la interfaz
    private lateinit var codigoIsoEditText: EditText
    private lateinit var ciudadNombreEditText: EditText
    private lateinit var obtenerButton: Button

    private lateinit var temperaturaActualTextView: TextView
    private lateinit var temperaturaMinimaTextView: TextView
    private lateinit var temperaturaMaximaTextView: TextView

    private val apiKey = "aa8782089df8fb9de8b95f66b22f29f9" // Usa tu propia API Key

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicialización de vistas
        codigoIsoEditText = findViewById(R.id.codigo_iso)
        ciudadNombreEditText = findViewById(R.id.ciudad_nombre)
        obtenerButton = findViewById(R.id.obtener_button)

        temperaturaActualTextView = findViewById(R.id.temperatura_actual)
        temperaturaMinimaTextView = findViewById(R.id.temperatura_minima)
        temperaturaMaximaTextView = findViewById(R.id.temperatura_maxima)

        // Acción al presionar el botón de obtener
        obtenerButton.setOnClickListener {
            val codigoIso = codigoIsoEditText.text.toString()
            val ciudadNombre = ciudadNombreEditText.text.toString()

            if (ciudadNombre.isNotEmpty() && codigoIso.isNotEmpty()) {
                fetchWeatherData(codigoIso, ciudadNombre)
            } else {
                Toast.makeText(this, "Por favor, ingrese el código y la ciudad", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchWeatherData(codigoIso: String, ciudadNombre: String) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                // Realiza la solicitud a la API
                val response = RetrofitInstance.api.getWeather(ciudadNombre, apiKey)
                if (response.isSuccessful) {
                    val weather = response.body()

                    // Verifica la respuesta con los logs
                    Log.d("WeatherAPI", "Response: ${response.body()}")
                    if (weather != null) {
                        // Actualiza la UI con los datos obtenidos
                        withContext(Dispatchers.Main) {
                            temperaturaActualTextView.text = "Actual: ${weather.main.temp}°C"
                            temperaturaMinimaTextView.text = "Mínima: ${weather.main.temp_min}°C"
                            temperaturaMaximaTextView.text = "Máxima: ${weather.main.temp_max}°C"
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Error al obtener el clima", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Ocurrió un error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
