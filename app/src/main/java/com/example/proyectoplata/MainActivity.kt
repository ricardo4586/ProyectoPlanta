package com.example.proyectoplata

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import com.google.firebase.messaging.FirebaseMessaging
import androidx.appcompat.app.ActionBarDrawerToggle
import com.google.android.material.navigation.NavigationView
import androidx.fragment.app.Fragment // Importar la clase base Fragment

// Importaciones de tus fragmentos (asegúrate de que estas rutas sean correctas)
import com.example.proyectoplata.fragments.HomeFragment // ¡Importamos HomeFragment!
import com.example.proyectoplata.fragments.TemperatureFragment
import com.example.proyectoplata.fragments.HumidityFragment
import com.example.proyectoplata.fragments.LightFragment
import com.example.proyectoplata.fragments.NPKFragment


class MainActivity : AppCompatActivity() {

    // Ya NO se necesitan estas declaraciones aquí. Ahora están en HomeFragment o en los fragmentos de gráficos.
    // private lateinit var codigoIsoEditText: EditText
    // private lateinit var ciudadNombreEditText: EditText
    // private lateinit var obtenerButton: Button
    // private lateinit var temperaturaActualTextView: TextView
    // private lateinit var temperaturaMinimaTextView: TextView
    // private lateinit var temperaturaMaximaTextView: TextView
    // private lateinit var lineChartTemperature: LineChart
    // private lateinit var lineChartHumidity: LineChart
    // private lateinit var lineChartLight: LineChart
    // private lateinit var lineChartNPK: LineChart
    // private val apiKey = "aa8782089df8fb9de8b95f66b22f29f9"
    // private val weatherRepository = WeatherRepository()

    private lateinit var toolbar: Toolbar
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicialización de Toolbar
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Inicialización del DrawerLayout y NavigationView
        drawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)

        // Agregar ActionBarDrawerToggle (para el icono de hamburguesa)
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.openDrawer, R.string.closeDrawer
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Configuración del NavigationView: maneja los clics en los ítems del menú
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> { // Ítem para la pestaña de Clima
                    replaceFragment(HomeFragment())
                    supportActionBar?.title = "Clima" // Título en la Toolbar
                }
                R.id.nav_temperature -> {
                    replaceFragment(TemperatureFragment())
                    supportActionBar?.title = "Temperatura"
                }
                R.id.nav_humidity -> {
                    replaceFragment(HumidityFragment())
                    supportActionBar?.title = "Humedad"
                }
                R.id.nav_light -> {
                    replaceFragment(LightFragment())
                    supportActionBar?.title = "Luz"
                }
                R.id.nav_npk -> {
                    replaceFragment(NPKFragment())
                    supportActionBar?.title = "NPK"
                }
            }
            drawerLayout.closeDrawers() // Cierra el Drawer después de la selección
            true
        }

        // Establecer el fragmento por defecto al iniciar la actividad
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment()) // ¡Carga el HomeFragment como pantalla inicial!
            supportActionBar?.title = "Clima" // Título inicial de la Toolbar
            navView.setCheckedItem(R.id.nav_home) // Marca el ítem "Clima" en el Drawer
        }

        // Obtener el token FCM y mostrarlo (puede quedarse aquí o moverse según sea necesario)
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                return@addOnCompleteListener
            }
            val token = task.result
            println("FCM Token: $token")
        }
    }

    // Función para reemplazar fragmentos (es crucial y debe quedarse aquí)
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment) // 'fragment_container' es el ID del FrameLayout en activity_main.xml
            .commit()
    }
}