package com.example.proyectoplata

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectoplata.databinding.ActivityLoginBinding // Importar View Binding
import com.google.firebase.auth.FirebaseAuth

import com.google.firebase.auth.FirebaseAuthException // Para manejar excepciones específicas
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await // Para usar .await() en tareas de Firebase

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityLoginBinding // Declaración del binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater) // Inicialización del binding
        setContentView(binding.root) // Usar el root del binding

        auth = FirebaseAuth.getInstance()

        binding.btnLogin.setOnClickListener {
            val email = binding.etLoginEmail.text.toString().trim() // .trim() para eliminar espacios
            val password = binding.etLoginPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, complete todos los campos.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Usar Coroutines para la llamada a Firebase Auth
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    auth.signInWithEmailAndPassword(email, password).await() // Usar .await()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@LoginActivity, "Inicio de sesión exitoso.", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish() // Finaliza LoginActivity
                    }
                } catch (e: FirebaseAuthException) {
                    withContext(Dispatchers.Main) {
                        // Manejo de errores específicos de Firebase Auth
                        val errorMessage = when (e.errorCode) {
                            "ERROR_INVALID_EMAIL" -> "El formato del correo electrónico es inválido."
                            "ERROR_WRONG_PASSWORD" -> "La contraseña es incorrecta."
                            "ERROR_USER_NOT_FOUND" -> "No hay ningún usuario con ese correo electrónico."
                            else -> "Error de autenticación: ${e.message}"
                        }
                        Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@LoginActivity, "Error inesperado: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        binding.tvRegisterLink.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}