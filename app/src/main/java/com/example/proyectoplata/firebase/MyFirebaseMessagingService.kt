package com.example.proyectoplata

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager // Importación necesaria para RingtoneManager
import android.os.Build
import android.util.Log // Importación necesaria para Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
// Si necesitas acceder a la base de datos o autenticación desde aquí en el futuro, puedes descomentar estas líneas:
// import com.google.firebase.database.FirebaseDatabase
// import com.google.firebase.auth.FirebaseAuth

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "MyFirebaseMsgService" // Declaración de TAG para logs

    // Se llama cuando se recibe un mensaje de FCM
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Comprobar si el mensaje contiene una carga útil de datos (data payload)
        // Puedes enviar datos adicionales desde tu backend junto con la notificación
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")

            // Aquí puedes procesar tus datos de sensor si los enviaste en el 'data' payload, ej.
            val sensorType = remoteMessage.data["sensorType"] // ej. "humedad"
            val currentValue = remoteMessage.data["currentValue"] // ej. "25"
            val threshold = remoteMessage.data["threshold"] // ej. "30"

            // Puedes construir un mensaje de notificación más específico basado en estos datos
            val title = remoteMessage.notification?.title ?: "Alerta de Cultivo"
            val body = remoteMessage.notification?.body ?: "Hay una actualización de tus sensores."

            // Si envías título y cuerpo en 'data' payload en lugar de 'notification' payload:
            // val customTitle = remoteMessage.data["title"]
            // val customBody = remoteMessage.data["body"]
            // sendNotification(customTitle ?: "Notificación", customBody ?: "Nuevo mensaje")

            sendNotification(title, body)

        } else {
            // Si el mensaje no tiene 'data' payload, solo comprueba 'notification' payload
            remoteMessage.notification?.let {
                Log.d(TAG, "Message Notification Body: ${it.body}")
                sendNotification(it.title ?: "Notificación", it.body ?: "Nuevo mensaje")
            }
        }
    }

    // Se llama cuando el token de registro de la instancia cambia
    // Esto es importante para actualizar el token en tu base de datos si cambia
    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")

        // Aquí deberías enviar este nuevo token a tu servidor/base de datos
        // para asegurarte de que siempre tengas el token más reciente del dispositivo
        // Puedes reutilizar la función saveFCMTokenToDatabase de MainActivity aquí
        // (tendrías que pasar una referencia a la base de datos y la autenticación)
        // O una función similar que envíe el token a tu backend
        /*
        val database = FirebaseDatabase.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            database.getReference("users").child(userId).child("fcmToken").setValue(token)
                .addOnSuccessListener {
                    Log.d(TAG, "FCM token updated in database for user: $userId")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to update FCM token in database for user: $userId", e)
                }
        } else {
            Log.d(TAG, "User not logged in, cannot update FCM token for specific user.")
        }
        */
    }

    // Muestra la notificación en la barra de estado
    private fun sendNotification(notificationTitle: String, notificationBody: String) {
        // Al hacer clic en la notificación, se abre MainActivity
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0 /* Request code */, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE // FLAG_IMMUTABLE es requerido para Android S (API 31) en adelante
        )

        // Define el ID del canal de notificación (para Android 8.0 Oreo y superior)
        val channelId = "sensor_alert_channel"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // Construye la notificación
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_ic_notification) // Asegúrate de que este drawable existe
            .setContentTitle(notificationTitle)
            .setContentText(notificationBody)
            .setAutoCancel(true) // La notificación se cierra al hacer clic
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setColor(resources.getColor(R.color.notification_color, theme)) // Usa el color que definimos en colors.xml
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Asegura que sea una notificación de alta prioridad

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear el canal de notificación para Android 8.0 Oreo y superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alertas de Sensores", // Nombre del canal visible para el usuario
                NotificationManager.IMPORTANCE_HIGH // Nivel de importancia (alto = sonido y pop-up)
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Muestra la notificación (ID de notificación 0 o un ID único si tienes varias notificaciones activas)
        notificationManager.notify(0 /* ID de notificación */, notificationBuilder.build())
    }
}