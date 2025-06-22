package com.example.proyectoplata

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem // Necesario para onNavigationItemSelected
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat // Necesario para closeDrawers
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.proyectoplata.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

// Importaciones de Firebase para la base de datos y mensajes
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging

// Importación para MPAndroidChart
import com.github.mikephil.charting.data.Entry

// Importaciones para formato de fecha/hora
import java.text.SimpleDateFormat
import java.util.*

// *** IMPORTACIÓN CRUCIAL PARA SharedSensorViewModel ***
import com.example.proyectoplata.SharedSensorViewModel

// Importaciones de tus fragmentos (asegúrate de que estas rutas sean correctas)
import com.example.proyectoplata.fragments.HomeFragment
import com.example.proyectoplata.fragments.TemperatureFragment
import com.example.proyectoplata.fragments.HumidityFragment
import com.example.proyectoplata.fragments.HumiditySoilFragment
import com.example.proyectoplata.fragments.UvIndexFragment
import com.example.proyectoplata.fragments.LightFragment
import com.example.proyectoplata.fragments.NitrogenFragment
import com.example.proyectoplata.fragments.PhosphorusFragment
import com.example.proyectoplata.fragments.PotassiumFragment
import com.example.proyectoplata.fragments.GeminiFragment
import com.example.proyectoplata.fragments.CropRecommendationFragment // Importación añadida para CropRecommendationFragment


class MainActivity : AppCompatActivity(), com.google.android.material.navigation.NavigationView.OnNavigationItemSelectedListener { // Implementar la interfaz

    private val TAG = "MainActivity" // Para los logs
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var sharedSensorViewModel: SharedSensorViewModel
    private lateinit var firebaseDatabase: FirebaseDatabase // Declaración de la instancia de FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firebaseDatabase = FirebaseDatabase.getInstance() // Inicialización de FirebaseDatabase

        // Redirige al usuario a la pantalla de inicio de sesión si no está autenticado
        if (auth.currentUser == null) {
            navigateToLogin()
            return
        }

        // Inicializa el ViewModel compartido para la comunicación de datos entre fragmentos
        sharedSensorViewModel = ViewModelProvider(this)[SharedSensorViewModel::class.java]

        // Llamada a la función para leer datos de temperatura ambiental de Firebase
        setupTemperatureAmbientalFirebaseListener()


        // Configura la barra de herramientas como la barra de acción de la actividad
        setSupportActionBar(binding.toolbar)

        // Configura el botón de alternancia del cajón de navegación (icono de hamburguesa)
        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.openDrawer,
            R.string.closeDrawer
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Configura el listener para los elementos seleccionados en el menú de navegación
        binding.navView.setNavigationItemSelectedListener(this) // Establece 'this' como listener

        // Carga el HomeFragment al inicio de la actividad si no hay estado de instancia guardado
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
            supportActionBar?.title = "Clima y Sensores"
            // Selecciona visualmente el elemento "Home" al inicio
            binding.navView.setCheckedItem(R.id.nav_home)
        }

        // Obtiene y registra el token de FCM (para notificaciones push)
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fallo al obtener el token de registro de FCM", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            Log.d(TAG, "FCM Token: $token")
        }
    }

    // Implementación del método de la interfaz OnNavigationItemSelectedListener
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        var fragment: Fragment? = null
        var titleString: String? = null

        when (item.itemId) {
            R.id.nav_home -> {
                fragment = HomeFragment()
                titleString = "Clima y Sensores"
            }
            R.id.nav_crop_recommendation -> { // <-- ¡Lógica para el Fragmento de Recomendación de Cultivos!
                fragment = CropRecommendationFragment()
                titleString = "Recomendación de Cultivos"
            }
            R.id.nav_temperature_ambient -> {
                fragment = TemperatureFragment()
                titleString = "Gráfico Temperatura Ambiental"
            }
            R.id.nav_humidity -> {
                fragment = HumidityFragment()
                titleString = "Gráfico Humedad Ambiental"
            }
            R.id.nav_humidity_soil -> {
                fragment = HumiditySoilFragment()
                titleString = "Gráfico Humedad del Suelo"
            }
            R.id.nav_uv_index -> {
                fragment = UvIndexFragment()
                titleString = "Gráfico Índice UV"
            }
            R.id.nav_voltage_uva -> {
                fragment = LightFragment()
                titleString = "Gráfico Voltaje UVA"
            }
            R.id.nav_nitrogen -> {
                fragment = NitrogenFragment()
                titleString = "Gráfico Nitrógeno (N)"
            }
            R.id.nav_phosphorus -> {
                fragment = PhosphorusFragment()
                titleString = "Gráfico Fósforo (P)"
            }
            R.id.nav_potassium -> {
                fragment = PotassiumFragment()
                titleString = "Gráfico Potasio (K)"
            }
            R.id.nav_gemini_ai -> {
                fragment = GeminiFragment()
                titleString = "Inteligencia Artificial"
            }
            R.id.nav_logout -> {
                auth.signOut()
                Toast.makeText(this, "Sesión cerrada.", Toast.LENGTH_SHORT).show()
                navigateToLogin()
                binding.drawerLayout.closeDrawers() // Cerrar el cajón después de logout
                return true // Retornar true ya que el evento fue manejado
            }
            else -> {
                Log.w(TAG, "Elemento de menú no reconocido: ${item.itemId}")
            }
        }

        fragment?.let {
            replaceFragment(it)
            supportActionBar?.title = titleString
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START) // Cerrar el cajón
        return true // El evento ha sido manejado
    }


    // Función para leer datos de temperatura ambiental de Firebase
    private fun setupTemperatureAmbientalFirebaseListener() {
        val ref = firebaseDatabase.getReference("mediciones/temperatura_ambiental")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val entries = ArrayList<Entry>()
                for (childSnapshot in snapshot.children) {
                    val key = childSnapshot.key // Por ejemplo: "YYYY-MM-DD_HH-mm-ss"
                    val value = childSnapshot.getValue(Float::class.java)

                    if (key != null && value != null) {
                        try {
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
                            val date = dateFormat.parse(key)
                            if (date != null) {
                                entries.add(Entry(date.time.toFloat(), value)) // x: timestamp, y: value
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error al parsear fecha para temperatura ambiental: ${e.message}")
                        }
                    }
                }
                entries.sortBy { it.x } // Asegurarse de que los datos estén ordenados por tiempo
                sharedSensorViewModel.updateTemperatureEntries(entries) // Envía los datos a tu ViewModel
                Log.d(TAG, "Datos de temperatura ambiental de Firebase procesados: ${entries.size} entradas.")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error al leer datos de temperatura ambiental de Firebase: ${error.message}")
            }
        })
    }

    // Función auxiliar para reemplazar el fragmento actual en el contenedor principal
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, fragment)
            .commit()
    }

    // Función auxiliar para navegar a LoginActivity y finalizar la actividad actual
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}