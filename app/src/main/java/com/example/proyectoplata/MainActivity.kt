package com.example.proyectoplata

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import com.google.firebase.messaging.FirebaseMessaging
import androidx.appcompat.app.ActionBarDrawerToggle
import com.google.android.material.navigation.NavigationView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import android.util.Log

// Importaciones de tus fragmentos
import com.example.proyectoplata.fragments.HomeFragment
import com.example.proyectoplata.fragments.TemperatureFragment // Mantenemos esta importación por si se usa en otro lugar, pero su case de navegación se eliminará
import com.example.proyectoplata.fragments.HumidityFragment
import com.example.proyectoplata.fragments.LightFragment
import com.example.proyectoplata.fragments.NPKFragment
import com.example.proyectoplata.fragments.GeminiFragment

// Importación del ViewModel (directamente en el paquete principal)
import com.example.proyectoplata.SharedSensorViewModel
import com.example.proyectoplata.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var toolbar: Toolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var auth: FirebaseAuth

    private lateinit var sharedSensorViewModel: SharedSensorViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        sharedSensorViewModel = ViewModelProvider(this).get(SharedSensorViewModel::class.java)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.openDrawer, R.string.closeDrawer
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener { menuItem ->
            var fragment: Fragment? = null
            var titleString: String? = null

            when (menuItem.itemId) {
                R.id.nav_home -> {
                    fragment = HomeFragment()
                    titleString = "Clima"
                }
                // ELIMINADO: R.id.nav_temperature -> { fragment = TemperatureFragment(); titleString = "Temperatura" }
                R.id.nav_humidity -> {
                    fragment = HumidityFragment()
                    titleString = "Humedad"
                }
                R.id.nav_light -> {
                    fragment = LightFragment()
                    titleString = "Luz Solar"
                }
                R.id.nav_npk -> {
                    fragment = NPKFragment()
                    titleString = "NPK"
                }
                R.id.nav_gemini_ai -> {
                    fragment = GeminiFragment()
                    titleString = "Inteligencia Artificial"
                }
                R.id.nav_logout -> {
                    auth.signOut()
                    Toast.makeText(this, "Sesión cerrada.", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                // Si la ID del elemento de menú no coincide con ninguna de las opciones anteriores
                // se mantendrá el fragmento actual o se puede definir un comportamiento por defecto
                else -> {
                    Log.w("MainActivity", "Item de menú no reconocido: ${menuItem.itemId}")
                    // Opcional: podrías mantener el fragmento actual o cargar un fragmento de error/por defecto
                    // fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                    // titleString = supportActionBar?.title?.toString()
                }
            }

            fragment?.let {
                replaceFragment(it)
                supportActionBar?.title = titleString
            }
            drawerLayout.closeDrawers()
            true
        }

        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
            supportActionBar?.title = "Clima"
            navView.setCheckedItem(R.id.nav_home)
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("MainActivity", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            Log.d("MainActivity", "FCM Token: $token")
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
