package com.example.housemouse.workers

import android.Manifest
import android.app.Notification // Ensure this is android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.housemouse.MainActivity
import com.example.housemouse.MyApplication
import com.example.housemouse.R
import com.example.housemouse.data.AppDatabase
import com.example.housemouse.data.Task // Ensure this is your domain Task model
import com.example.housemouse.data.toDomainModel
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class DailyNotificationWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "DailyTaskNotificationWorker"
        private var lastNotificationId = 100 // Base for unique notification IDs
        private const val MAX_NOTIFICATIONS_TO_SHOW = 5 // Limit for how many individual notifications
    }

    override suspend fun doWork(): Result {
        Log.d(WORK_NAME, "Worker started.")
        val taskDao = AppDatabase.getDatabase(appContext).taskDao()
        val today = LocalDate.now()

        // Permission Check (Important for Android 13+)
        if (ActivityCompat.checkSelfPermission(
            appContext,
            Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(WORK_NAME, "Notification permission not granted. Skipping notification.")
            return Result.failure() // Or Result.success() if you don't want it to retry soon
        }

        try {
            val dueTasksEntities = taskDao.getDueTasks(today.toEpochDay()).firstOrNull() ?: emptyList()
            val dueTasks = dueTasksEntities.map { it.toDomainModel() }
                .filter { task ->
                    // Only notify if not completed today
                    task.lastCompletedDate == null || !task.lastCompletedDate!!.isEqual(today)
                }

            Log.d(WORK_NAME, "Found ${dueTasks.size} due tasks to notify.")

            if (dueTasks.isEmpty()) {
                Log.d(WORK_NAME, "No due tasks to notify.")
                return Result.success()
            }

            val notificationManager = NotificationManagerCompat.from(appContext)

            // Create and post individual notifications up to the limit
            dueTasks.take(MAX_NOTIFICATIONS_TO_SHOW).forEachIndexed { index, task ->
                val notificationId = lastNotificationId + index + 1 // Unique ID for each task notification
                val notification = createTaskNotification(task, today, notificationId)
                notificationManager.notify(notificationId, notification)
                Log.d(WORK_NAME, "Posted notification for task: ${task.name} with ID: $notificationId")
            }

            // If there are more tasks than MAX_NOTIFICATIONS_TO_SHOW, you might log it
            // or decide on a different strategy later (like a single generic "more tasks due" notification).
            if (dueTasks.size > MAX_NOTIFICATIONS_TO_SHOW) {
                Log.d(WORK_NAME, "${dueTasks.size - MAX_NOTIFICATIONS_TO_SHOW} more tasks are due but not individually notified.")
                // Optionally, post one generic notification here if dueTasks.size > MAX_NOTIFICATIONS_TO_SHOW
                // val genericNotificationId = lastNotificationId + MAX_NOTIFICATIONS_TO_SHOW + 1
                // val genericNotification = createGenericDueTasksNotification(dueTasks.size)
                // notificationManager.notify(genericNotificationId, genericNotification)
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e(WORK_NAME, "Error in DailyNotificationWorker", e)
            return Result.retry()
        }
    }

    private fun createTaskNotification(task: Task, today: LocalDate, notificationId: Int): Notification {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // You could add extras to navigate to a specific task if you implement that
            // putExtra("taskId", task.id)
        }
        val pendingIntentFlags =
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent: PendingIntent = PendingIntent.getActivity(appContext, notificationId, intent, pendingIntentFlags)

        val dueStatusText = when {
            task.nextDueDate == null -> "Status unknown" // Should not happen for due tasks
            task.nextDueDate!!.isBefore(today) -> {
                val daysOverdue = ChronoUnit.DAYS.between(task.nextDueDate, today)
                "Overdue by $daysOverdue day(s)"
            }
            task.nextDueDate!!.isEqual(today) -> "Due today"
            else -> "Due soon" // Should be filtered by getDueTasks, but as a fallback
        }

        return NotificationCompat.Builder(appContext, MyApplication.TASK_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_icon) // <<< --- IMPORTANT: REPLACE with your actual icon
            .setContentTitle(task.name)
            .setContentText(dueStatusText)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Or PRIORITY_DEFAULT
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Dismisses the notification when clicked
            // No .setGroup() or summary notification logic needed in this simplified version
            .build()
    }

    // Optional: If you want a single notification when there are more tasks than MAX_NOTIFICATIONS_TO_SHOW
    // private fun createGenericDueTasksNotification(totalDueTasks: Int): Notification {
    //     val intent = Intent(appContext, MainActivity::class.java).apply {
    //         flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    //     }
    //     val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    //         PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    //     } else {
    //         PendingIntent.FLAG_UPDATE_CURRENT
    //     }
    //     val pendingIntent: PendingIntent = PendingIntent.getActivity(appContext, 0, intent, pendingIntentFlags) // Request code 0 for generic
    //
    //     return NotificationCompat.Builder(appContext, MyApplication.TASK_NOTIFICATION_CHANNEL_ID)
    //         .setSmallIcon(R.drawable.ic_notification_icon) // <<< --- IMPORTANT: REPLACE
    //         .setContentTitle("$totalDueTasks tasks require attention")
    //         .setContentText("Open HouseMouse to see all due tasks.")
    //         .setPriority(NotificationCompat.PRIORITY_DEFAULT)
    //         .setContentIntent(pendingIntent)
    //         .setAutoCancel(true)
    //         .build()
    // }
}