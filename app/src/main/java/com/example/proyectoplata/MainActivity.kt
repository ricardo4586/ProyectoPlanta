package com.example.proyectoplata

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.proyectoplata.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging

// Importaciones de tus fragmentos
import com.example.proyectoplata.fragments.HomeFragment
import com.example.proyectoplata.fragments.TemperatureFragment // Este es para Temperatura Ambiental
import com.example.proyectoplata.fragments.HumidityFragment
import com.example.proyectoplata.fragments.HumiditySoilFragment
import com.example.proyectoplata.fragments.UvIndexFragment
import com.example.proyectoplata.fragments.LightFragment
import com.example.proyectoplata.fragments.NitrogenFragment
import com.example.proyectoplata.fragments.PhosphorusFragment
import com.example.proyectoplata.fragments.PotassiumFragment
import com.example.proyectoplata.fragments.GeminiFragment
// La línea para TemperatureSoilFragment ha sido eliminada o comentada.
// Ejemplo: // import com.example.proyectoplata.fragments.TemperatureSoilFragment


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var sharedSensorViewModel: SharedSensorViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Redirige al usuario a la pantalla de inicio de sesión si no está autenticado
        if (auth.currentUser == null) {
            navigateToLogin()
            return
        }

        // Inicializa el ViewModel compartido para la comunicación de datos entre fragmentos
        sharedSensorViewModel = ViewModelProvider(this)[SharedSensorViewModel::class.java]

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
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            var fragment: Fragment? = null
            var titleString: String? = null

            when (menuItem.itemId) {
                R.id.nav_home -> {
                    fragment = HomeFragment()
                    titleString = "Clima y Sensores"
                }
                // Usa el nuevo ID del menú: nav_temperature_ambient
                R.id.nav_temperature_ambient -> { // Coincide con el nuevo ID en menu_sensores.xml
                    fragment = TemperatureFragment() // Este es el fragmento que ya hemos corregido para Temperatura Ambiental
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
                }
                else -> {
                    Log.w("MainActivity", "Elemento de menú no reconocido: ${menuItem.itemId}")
                }
            }

            fragment?.let {
                replaceFragment(it)
                supportActionBar?.title = titleString
            }
            binding.drawerLayout.closeDrawers()
            true
        }

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
                Log.w("MainActivity", "Fallo al obtener el token de registro de FCM", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            Log.d("MainActivity", "FCM Token: $token")
        }
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