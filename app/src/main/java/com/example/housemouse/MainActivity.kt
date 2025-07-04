package com.example.housemouse // Your main package

import android.Manifest
import android.content.Context // Added for triggerDueTaskNotifications
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
// import androidx.activity.result.launch // Not directly used in this version, but fine to keep
import androidx.activity.viewModels // For by viewModels()
import androidx.compose.foundation.layout.Arrangement // Added for Button layout
import androidx.compose.foundation.layout.Column // Added for Button layout
import androidx.compose.foundation.layout.Spacer // Added for Button layout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height // Added for Button layout
import androidx.compose.foundation.layout.padding // Added for Scaffold padding
import androidx.compose.material3.Button // Added for the new Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold // Added to easily place content and button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text // Added for Button text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment // Added for Button layout
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp // Added for padding and spacer
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy // Added for OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder // Added for OneTimeWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.housemouse.data.AppDatabase // Import your AppDatabase
// import com.example.housemouse.ui.FakeTaskViewModel // Not used in this final version
import com.example.housemouse.ui.TaskListScreen
import com.example.housemouse.ui.TaskViewModel // Import your TaskViewModel
import com.example.housemouse.ui.theme.HouseMouseTheme // Your app's theme
import com.example.housemouse.workers.DailyNotificationWorker
import java.util.concurrent.TimeUnit


class MainActivity : ComponentActivity() {

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
        scheduleDailyNotificationWorker()

        setContent {
            HouseMouseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TaskListScreen(viewModel = taskViewModel)
                }
            }
        }
    }

    private fun scheduleDailyNotificationWorker() {
        val dailyWorkRequest = PeriodicWorkRequestBuilder<DailyNotificationWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        ).build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            DailyNotificationWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            dailyWorkRequest
        )
    }
}
