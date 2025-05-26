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
import com.google.firebase.auth.FirebaseAuth // Importa FirebaseAuth

// Importaciones de tus fragmentos (asegúrate de que estas rutas sean correctas)
import com.example.proyectoplata.fragments.HomeFragment
import com.example.proyectoplata.fragments.TemperatureFragment
import com.example.proyectoplata.fragments.HumidityFragment
import com.example.proyectoplata.fragments.LightFragment
import com.example.proyectoplata.fragments.NPKFragment


class MainActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var auth: FirebaseAuth // Declaración de FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance() // Inicializa FirebaseAuth

        // **VERIFICAR SI EL USUARIO ESTÁ LOGEADO AL INICIAR MainActivity**
        if (auth.currentUser == null) {
            // Si no hay usuario logeado, redirige a LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() // Finaliza MainActivity para que el usuario no pueda volver atrás sin logearse
            return // Sale del onCreate para evitar inicializar el resto de la UI si no hay sesión
        }

        // Si el usuario está logeado, procede con la inicialización normal de MainActivity
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.openDrawer, R.string.closeDrawer
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    replaceFragment(HomeFragment())
                    supportActionBar?.title = "Clima"
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
                R.id.nav_logout -> { // Opción para cerrar sesión
                    auth.signOut() // Cierra la sesión de Firebase
                    Toast.makeText(this, "Sesión cerrada.", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish() // Finaliza MainActivity
                }
            }
            drawerLayout.closeDrawers()
            true
        }

        // Carga el fragmento inicial si es la primera vez que se crea la actividad
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
            supportActionBar?.title = "Clima"
            navView.setCheckedItem(R.id.nav_home)
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

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}