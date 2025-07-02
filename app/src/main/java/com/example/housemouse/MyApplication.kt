package com.example.housemouse

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class MyApplication : Application() {

    companion object {
        const val TASK_NOTIFICATION_CHANNEL_ID = "task_due_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name) // Add this string resource
            val descriptionText = getString(R.string.notification_channel_description) // Add this
            val importance = NotificationManager.IMPORTANCE_HIGH // Or IMPORTANCE_DEFAULT
            val channel =
                NotificationChannel(TASK_NOTIFICATION_CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                    // You can also set other channel properties like lights, vibration, etc.
                }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}