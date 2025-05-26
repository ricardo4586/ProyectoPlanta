package com.example.proyectoplata.firebase

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    // Este método se llama cuando se recibe un mensaje push
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Verifica si el mensaje contiene una notificación
        remoteMessage.notification?.let {
            // Muestra la notificación
            showNotification(it.body)
        }
    }

    // Método para mostrar la notificación en el dispositivo
    private fun showNotification(messageBody: String?) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Para dispositivos con Android Oreo (API 26) o superior, necesitamos configurar un canal de notificación
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "default_channel"
            val channelName = "Default Notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance)
            notificationManager.createNotificationChannel(channel)
        }

        // Construye la notificación
        val notification = NotificationCompat.Builder(this, "default_channel")
            .setContentTitle("Notificación de FCM")
            .setContentText(messageBody) // El mensaje que recibimos
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true) // La notificación se cancelará al hacer click
            .build()

        // Muestra la notificación
        notificationManager.notify(0, notification)
    }
}
