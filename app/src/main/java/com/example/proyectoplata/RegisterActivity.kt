package com.example.proyectoplata

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectoplata.databinding.ActivityRegisterBinding // Importar View Binding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityRegisterBinding // Declaración del binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater) // Inicialización del binding
        setContentView(binding.root) // Usar el root del binding

        auth = FirebaseAuth.getInstance()

        binding.btnRegister.setOnClickListener {
            val email = binding.etRegisterEmail.text.toString().trim()
            val password = binding.etRegisterPassword.text.toString().trim()
            val confirmPassword = binding.etRegisterConfirmPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Por favor, complete todos los campos.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Las contraseñas no coinciden.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Crear usuario con email y contraseña en Firebase
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    auth.createUserWithEmailAndPassword(email, password).await() // Usar .await()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@RegisterActivity, "Registro exitoso. ¡Bienvenido!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish() // Finaliza RegisterActivity
                    }
                } catch (e: FirebaseAuthException) {
                    withContext(Dispatchers.Main) {
                        // Manejo de errores específicos de Firebase Auth
                        val errorMessage = when (e.errorCode) {
                            "ERROR_INVALID_EMAIL" -> "El formato del correo electrónico es inválido."
                            "ERROR_EMAIL_ALREADY_IN_USE" -> "Este correo electrónico ya está registrado."
                            "ERROR_WEAK_PASSWORD" -> "La contraseña es demasiado débil."
                            else -> "Error en el registro: ${e.message}"
                        }
                        Toast.makeText(this@RegisterActivity, errorMessage, Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@RegisterActivity, "Error inesperado: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        binding.tvLoginLink.setOnClickListener {
            finish() // Cierra RegisterActivity para volver a LoginActivity
        }
    }
}