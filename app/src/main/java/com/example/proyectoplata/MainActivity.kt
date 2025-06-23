package com.example.proyectoplata

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.proyectoplata.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging

import com.github.mikephil.charting.data.Entry

import java.text.SimpleDateFormat
import java.util.*

import com.example.proyectoplata.SharedSensorViewModel

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
import com.example.proyectoplata.fragments.CropRecommendationFragment
import com.example.proyectoplata.fragments.HistoryFragment // <-- ¡IMPORTACIÓN NECESARIA PARA HistoryFragment!


class MainActivity : AppCompatActivity(), com.google.android.material.navigation.NavigationView.OnNavigationItemSelectedListener {

    private val TAG = "MainActivity"
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var sharedSensorViewModel: SharedSensorViewModel
    private lateinit var firebaseDatabase: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firebaseDatabase = FirebaseDatabase.getInstance()

        if (auth.currentUser == null) {
            navigateToLogin()
            return
        }

        sharedSensorViewModel = ViewModelProvider(this)[SharedSensorViewModel::class.java]

        setupTemperatureAmbientalFirebaseListener()


        setSupportActionBar(binding.toolbar)

        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.openDrawer,
            R.string.closeDrawer
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.navView.setNavigationItemSelectedListener(this)

        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
            supportActionBar?.title = "Clima y Sensores"
            binding.navView.setCheckedItem(R.id.nav_home)
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fallo al obtener el token de registro de FCM", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            Log.d(TAG, "FCM Token: $token")

            saveFCMTokenToDatabase(token)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        var fragment: Fragment? = null
        var titleString: String? = null

        when (item.itemId) {
            R.id.nav_home -> {
                fragment = HomeFragment()
                titleString = "Clima y Sensores"
            }
            R.id.nav_crop_recommendation -> {
                fragment = CropRecommendationFragment()
                titleString = "Recomendación de Cultivos"
            }
            R.id.nav_history -> { // <-- ¡NUEVA CATEGORÍA AÑADIDA AQUÍ!
                fragment = HistoryFragment()
                titleString = "Historial de Datos"
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
                binding.drawerLayout.closeDrawers()
                return true
            }
            else -> {
                Log.w(TAG, "Elemento de menú no reconocido: ${item.itemId}")
            }
        }

        fragment?.let {
            replaceFragment(it)
            supportActionBar?.title = titleString
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }


    private fun setupTemperatureAmbientalFirebaseListener() {
        val ref = firebaseDatabase.getReference("mediciones/temperatura_ambiental")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val entries = ArrayList<Entry>()
                for (childSnapshot in snapshot.children) {
                    val key = childSnapshot.key
                    val value = childSnapshot.getValue(Float::class.java)

                    if (key != null && value != null) {
                        try {
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
                            val date = dateFormat.parse(key)
                            if (date != null) {
                                entries.add(Entry(date.time.toFloat(), value))
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error al parsear fecha para temperatura ambiental: ${e.message}")
                        }
                    }
                }
                entries.sortBy { it.x }
                sharedSensorViewModel.updateTemperatureEntries(entries)
                Log.d(TAG, "Datos de temperatura ambiental de Firebase procesados: ${entries.size} entradas.")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error al leer datos de temperatura ambiental de Firebase: ${error.message}")
            }
        })
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, fragment) // Asegúrate de que R.id.content_frame sea el ID correcto de tu FrameLayout/FragmentContainerView
            .commit()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun saveFCMTokenToDatabase(token: String?) {
        val userId = auth.currentUser?.uid
        if (userId != null && token != null) {
            val database = FirebaseDatabase.getInstance()
            database.getReference("users").child(userId).child("fcmToken").setValue(token)
                .addOnSuccessListener {
                    Log.d(TAG, "FCM token saved to database for user: $userId")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to save FCM token to database for user: $userId", e)
                }
        } else if (token != null) {
            Log.d(TAG, "No user logged in. FCM token obtained: $token. Cannot save to specific user.")
        } else {
            Log.d(TAG, "FCM token is null, cannot save.")
        }
    }
}