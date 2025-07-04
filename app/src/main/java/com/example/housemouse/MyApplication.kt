package com.example.housemouse

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class MyApplication : Application() {

    companion object {
        const val TASK_NOTIFICATION_CHANNEL_ID = "task_due_channel"
        const val MANUAL_TRIGGER_CHANNEL_ID = "manual_reminder_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val reminderChannelName = getString(R.string.notification_channel_name) // Add this string resource
        val reminderChannelDescription = getString(R.string.notification_channel_description) // Add this
        val reminderChannelImportance = NotificationManager.IMPORTANCE_HIGH // Or IMPORTANCE_DEFAULT
        val reminderChannel =
            NotificationChannel(TASK_NOTIFICATION_CHANNEL_ID, reminderChannelName, reminderChannelImportance).apply {
                description = reminderChannelDescription
                // You can also set other channel properties like lights, vibration, etc.
            }

        // Register the channel with the system
        val notificationManager: NotificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(reminderChannel)
    }
}