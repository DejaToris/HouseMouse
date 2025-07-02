package com.example.housemouse.ui // Or com.example.housemouse.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.housemouse.data.Task
import com.example.housemouse.data.TaskDao
import com.example.housemouse.data.toDomainModel
import com.example.housemouse.data.toEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.random.Random // Make sure this is imported

open class TaskViewModel(private val taskDao: TaskDao) : ViewModel() {

    open val allTasks: StateFlow<List<Task>> = taskDao.getAllTasks()
        .map { entityList -> entityList.map { it.toDomainModel() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    open fun addTask(name: String, minDays: Int, maxDays: Int) {
        if (name.isBlank() || minDays <= 0 || maxDays <= 0 || minDays > maxDays) {
            // Handle invalid input, maybe expose an error state to the UI
            // For now, just return or log
            println("Invalid task input: Name='$name', MinDays='$minDays', MaxDays='$maxDays'")
            return
        }
        viewModelScope.launch {
            // For a new task, lastCompletedDate is null.
            // nextDueDate could be set to today or also null until first completion,
            // depending on how you want to handle brand new tasks.
            // Let's set nextDueDate to today for simplicity, so it appears immediately.
            val today = LocalDate.now()
            val initialNextDueDate = today // Or calculate based on a default window if preferred

            val newTask = Task(
                name = name,
                minRecurrenceDays = minDays,
                maxRecurrenceDays = maxDays,
                lastCompletedDate = null,
                nextDueDate = initialNextDueDate // Task is due "now" or based on its window from "now"
            )
            taskDao.insertTask(newTask.toEntity())
        }
    }

    // --- ADD THIS METHOD ---
    open fun markTaskComplete(task: Task) {
        viewModelScope.launch {
            val today = LocalDate.now()
            // Calculate the next due date based on a random day within the recurrence window
            val daysUntilNextDue = Random.nextInt(task.minRecurrenceDays, task.maxRecurrenceDays + 1)
            val nextDueDate = today.plusDays(daysUntilNextDue.toLong())

            val updatedTask = task.copy(
                lastCompletedDate = today,
                nextDueDate = nextDueDate
            )
            taskDao.updateTask(updatedTask.toEntity())
            // Here you might also want to:
            // - Cancel any existing notifications for this specific task.
            // - Potentially schedule a new notification if your strategy involves individual task notifications
            //   rather than just a daily global check (though daily check is simpler to start).
        }
    }
    // --- END OF ADDED METHOD ---

    open fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskDao.deleteTask(task.toEntity())
        }
    }

    // You might want a method to update a task if you implement an edit feature
    open fun updateTaskDetails(task: Task) { // Example, if you add editing
        viewModelScope.launch {
            // Ensure nextDueDate is recalculated if recurrence changed, or handle appropriately
            taskDao.updateTask(task.toEntity())
        }
    }
}