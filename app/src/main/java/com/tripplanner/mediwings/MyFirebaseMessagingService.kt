package com.tripplanner.mediwings

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCM_Service"
        private const val CHANNEL_ID = "mediwings_chat_channel"
        private const val CHANNEL_NAME = "Chat Notifications"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        
        // Save token to Firebase Database for current user
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            FirebaseDatabase.getInstance().reference
                .child("users")
                .child(userId)
                .child("fcmToken")
                .setValue(token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Log.d(TAG, "Message received from: ${remoteMessage.from}")
        
        // Check if message contains a notification payload
        remoteMessage.notification?.let {
            val title = it.title ?: "New Message"
            val body = it.body ?: ""
            showNotification(title, body, remoteMessage.data)
        }
        
        // Check if message contains a data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            val title = remoteMessage.data["title"] ?: "New Message"
            val body = remoteMessage.data["body"] ?: ""
            showNotification(title, body, remoteMessage.data)
        }
    }

    private fun showNotification(title: String, message: String, data: Map<String, String>) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for chat messages"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        // Create intent to open chat activity
        val intent = Intent(this, ChatActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            // Add any extra data from the notification
            data.forEach { (key, value) ->
                putExtra(key, value)
            }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build notification
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.mediwings_logo)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
        
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
