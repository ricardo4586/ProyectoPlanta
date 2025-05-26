package com.example.proyectoplata

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectoplata.network.WeatherRepository
import com.example.proyectoplata.firebase.MyFirebaseMessagingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var codigoIsoEditText: EditText
    private lateinit var ciudadNombreEditText: EditText
    private lateinit var obtenerButton: Button

    private lateinit var temperaturaActualTextView: TextView
    private lateinit var temperaturaMinimaTextView: TextView
    private lateinit var temperaturaMaximaTextView: TextView

    private val apiKey = "aa8782089df8fb9de8b95f66b22f29f9" // Usar tu propia API Key

    // Instanciamos el repositorio
    private val weatherRepository = WeatherRepository()

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
                fetchWeatherData(ciudadNombre)
            } else {
                Toast.makeText(this, "Por favor, ingrese el código y la ciudad", Toast.LENGTH_SHORT).show()
            }
        }

        // Obtener el token FCM y mostrarlo
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                // Si no se puede obtener el token
                return@addOnCompleteListener
            }

            // Obtener el token
            val token = task.result
            println("FCM Token: $token")  // Puedes enviar este token a tu servidor si lo necesitas
        }
    }

    private fun fetchWeatherData(cityName: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val weather = weatherRepository.fetchWeatherData(cityName, apiKey)

            withContext(Dispatchers.Main) {
                if (weather != null) {
                    temperaturaActualTextView.text = "Actual: ${weather.main.temp}°C"
                    temperaturaMinimaTextView.text = "Mínima: ${weather.main.temp_min}°C"
                    temperaturaMaximaTextView.text = "Máxima: ${weather.main.temp_max}°C"
                } else {
                    Toast.makeText(this@MainActivity, "Error al obtener el clima", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

