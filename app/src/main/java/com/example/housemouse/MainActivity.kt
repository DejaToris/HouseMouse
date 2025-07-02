package com.example.housemouse // Your main package

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.activity.viewModels // For by viewModels()
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.housemouse.data.AppDatabase // Import your AppDatabase
import com.example.housemouse.ui.FakeTaskViewModel
import com.example.housemouse.ui.TaskListScreen
import com.example.housemouse.ui.TaskViewModel // Import your TaskViewModel
import com.example.housemouse.ui.theme.HouseMouseTheme // Your app's theme
import com.example.housemouse.workers.DailyNotificationWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    // ViewModel Factory to provide TaskDao to TaskViewModel
    private val taskViewModel: TaskViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
                    val taskDao = AppDatabase.getDatabase(applicationContext).taskDao()
                    @Suppress("UNCHECKED_CAST")
                    return TaskViewModel(taskDao) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        askNotificationPermission()
        setContent {
            HouseMouseTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Pass the ViewModel to your main Composable screen
                    HouseMouseApp(taskViewModel = taskViewModel)
                }
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. You can now schedule work or post notifications.
                scheduleDailyNotificationWorker() // We'll create this function
            } else {
                // Explain to the user that the feature is unavailable because
                // the feature requires a permission that the user has denied.
                // At the same time, respect the user's decision. Don't link to
                // system settings in an effort to convince the user to change
                // their decision.
                // You could show a Snackbar or a dialog here.
                Toast.makeText(this, "Notification permission denied. Task reminders will not be shown.", Toast.LENGTH_LONG).show()
            }
        }

    private fun askNotificationPermission() {
        // TIRAMISU is Android 13
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission is already granted
                scheduleDailyNotificationWorker()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                // Show an educational UI to the user explaining why the permission is needed.
                // For this example, we'll just request directly.
                // In a real app, show a dialog first.
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            else -> {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun scheduleDailyNotificationWorker() {
        // Create a periodic work request that runs roughly once a day.
        // The exact timing will be optimized by WorkManager based on system conditions.
        val dailyWorkRequest = PeriodicWorkRequestBuilder<DailyNotificationWorker>(
            repeatInterval = 1, // Repeat every 1 unit
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            // You can add constraints here if needed, e.g.,
            // .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            DailyNotificationWorker.WORK_NAME, // A unique name for this work
            ExistingPeriodicWorkPolicy.KEEP, // KEEP: If pending work with the same name exists, do nothing.
            // REPLACE: If it exists, cancel and replace it.
            dailyWorkRequest
        )
        // You can log or Toast here to confirm scheduling if needed for debugging
        // Toast.makeText(this, "Daily task check scheduled!", Toast.LENGTH_SHORT).show()
    }
}

// This will be your main Composable function that orchestrates the UI
@Composable
fun HouseMouseApp(taskViewModel: TaskViewModel) {
    // For now, let's just display a placeholder.
    // We'll build out the TaskListScreen and AddTaskScreen later.
    // Text("Welcome to HouseMouse! Tasks will be shown here.")

    // You'll replace the above Text with your actual UI,
    // likely starting with a screen that lists tasks.
    // For example:
    TaskListScreen(viewModel = taskViewModel)
}

//@Preview(showBackground = true, name = "App Preview")
//@Composable
//fun DefaultPreview() {
//    HouseMouseTheme {
//        // Use the FakeTaskViewModel for the preview
//        // HouseMouseApp is your main entry point for the UI that takes the ViewModel
//        HouseMouseApp(taskViewModel = FakeTaskViewModel())
//    }
//}
