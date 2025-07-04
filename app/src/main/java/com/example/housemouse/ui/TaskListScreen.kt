package com.example.housemouse.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.housemouse.data.Task
import com.example.housemouse.workers.DailyNotificationWorker


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(viewModel: TaskViewModel) {
    val tasks by viewModel.allTasks.collectAsState()
    var showAddTaskDialog by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current.applicationContext

    val requestPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(context, "Notification permission granted!", Toast.LENGTH_SHORT).show()
                triggerDueTaskNotificationsWorker(context)
            } else {
                Toast.makeText(context, "Notifications will not be shown without permission.", Toast.LENGTH_LONG).show()
            }
        }

    fun checkPermissionAndTriggerNotifications() {
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED -> {
                triggerDueTaskNotificationsWorker(context)
            }
            // TODO: Consider adding shouldShowRequestPermissionRationale if you want to show a custom rationale dialog
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("HouseMouse Tasks") })
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(onClick = { showAddTaskDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Task")
                }
                Spacer(modifier = Modifier.height(16.dp))
                FloatingActionButton(onClick = {
                    checkPermissionAndTriggerNotifications()
                }) {
                    Icon(Icons.Filled.Notifications, contentDescription = "Re-Notify Due Tasks")
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (tasks.isEmpty() && !showAddTaskDialog) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No tasks yet! Add one using the + button.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tasks, key = { task -> task.id }) { task ->
                        TaskItem(
                            task = task,
                            onMarkComplete = { viewModel.markTaskComplete(task) },
                        )
                    }
                }
            }
        }

        if (showAddTaskDialog) {
            AddTaskDialog(
                onDismissRequest = { showAddTaskDialog = false },
                onConfirm = { name, minDays, maxDays ->
                    viewModel.addTask(name, minDays, maxDays)
                    showAddTaskDialog = false
                }
            )
        }
    }
}

@Composable
fun AddTaskDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (name: String, minDays: Int, maxDays: Int) -> Unit
) {
    var taskName by rememberSaveable { mutableStateOf("") }
    var minDaysString by rememberSaveable { mutableStateOf("") }
    var maxDaysString by rememberSaveable { mutableStateOf("") }
    var nameError by rememberSaveable { mutableStateOf<String?>(null) }
    var minDaysError by rememberSaveable { mutableStateOf<String?>(null) }
    var maxDaysError by rememberSaveable { mutableStateOf<String?>(null) }

    fun validateFields(): Boolean {
        nameError = if (taskName.isBlank()) "Name cannot be empty" else null
        minDaysError = if (minDaysString.isBlank()) "Min days cannot be empty" else try {
            if (minDaysString.toInt() <= 0) "Min days must be positive" else null
        } catch (e: NumberFormatException) {
            "Invalid number for min days"
        }
        maxDaysError = if (maxDaysString.isBlank()) "Max days cannot be empty" else try {
            val minD = minDaysString.toIntOrNull()
            val maxD = maxDaysString.toInt()
            if (maxD <= 0) "Max days must be positive"
            else if (minD != null && maxD < minD) "Max days must be >= min days"
            else null
        } catch (e: NumberFormatException) {
            "Invalid number for max days"
        }
        return nameError == null && minDaysError == null && maxDaysError == null
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Add New Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = taskName,
                    onValueChange = { taskName = it; nameError = null },
                    label = { Text("Task Name") },
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = { if (nameError != null) Text(nameError!!) }
                )
                OutlinedTextField(
                    value = minDaysString,
                    onValueChange = { minDaysString = it; minDaysError = null },
                    label = { Text("Min Recurrence Days") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = minDaysError != null,
                    supportingText = { if (minDaysError != null) Text(minDaysError!!) }
                )
                OutlinedTextField(
                    value = maxDaysString,
                    onValueChange = { maxDaysString = it; maxDaysError = null },
                    label = { Text("Max Recurrence Days") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = maxDaysError != null,
                    supportingText = { if (maxDaysError != null) Text(maxDaysError!!) }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (validateFields()) {
                        onConfirm(taskName, minDaysString.toInt(), maxDaysString.toInt())
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

private fun triggerDueTaskNotificationsWorker(context: Context) {
    val oneTimeWorkRequest = OneTimeWorkRequestBuilder<DailyNotificationWorker>()
        .build()
    WorkManager.getInstance(context).enqueueUniqueWork(
        "ManualDueTaskCheck", // Unique name for this manual trigger
        ExistingWorkPolicy.REPLACE, // Replace any existing pending manual check
        oneTimeWorkRequest
    )
    Toast.makeText(context, "Checking for due tasks to re-notify...", Toast.LENGTH_SHORT).show()
}