package com.example.housemouse.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.housemouse.data.Task // Keep this if TaskItem is in the same file


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(viewModel: TaskViewModel) {
    val tasks by viewModel.allTasks.collectAsState()
    var showAddTaskDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("HouseMouse Tasks") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddTaskDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Task")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (tasks.isEmpty() && !showAddTaskDialog) { // Avoid showing "No tasks" when dialog is up
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
                            // Add onDelete if you implement it in TaskItem and ViewModel
                            // onDelete = { viewModel.deleteTask(task) }
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