package com.example.housemouse.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.housemouse.data.FakeTaskDao // Import your FakeTaskDao
import com.example.housemouse.data.Task
import com.example.housemouse.data.toDomainModel
import com.example.housemouse.data.toEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.random.Random

/**
 * A fake ViewModel for use in Composable Previews.
 * It uses FakeTaskDao to provide sample data.
 */
class FakeTaskViewModel : TaskViewModel(FakeTaskDao()) { // You can also make it extend your actual TaskViewModel if it's simple enough
    // or create a common interface. For previews, direct is often fine.

    private val fakeTaskDao = FakeTaskDao() // Instantiate the fake DAO

    // Expose tasks from the FakeTaskDao
    override val allTasks: StateFlow<List<Task>> = fakeTaskDao.getAllTasks()
        .map { entityList -> entityList.map { it.toDomainModel() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    override fun addTask(name: String, minDays: Int, maxDays: Int) {
        // For previews, you might just log or add to the FakeDao's list directly
        // This won't trigger UI updates unless FakeTaskDao's Flow emits new values,
        // which it does with flowOf(tasks.toList()) on each call.
        // For more interactive previews, FakeTaskDao would need to use a MutableStateFlow itself.
        println("Preview: Add task called - Name: $name")
        viewModelScope.launch { // To match real ViewModel structure
            val today = LocalDate.now()
            val newTask = Task(
                name = name,
                minRecurrenceDays = minDays,
                maxRecurrenceDays = maxDays,
                lastCompletedDate = null,
                nextDueDate = today
            )
            fakeTaskDao.insertTask(newTask.toEntity())
        }
    }

    override fun markTaskComplete(task: Task) {
        println("Preview: Mark task complete called - Task ID: ${task.id}")
        viewModelScope.launch {
            val today = LocalDate.now()
            val daysUntilNextDue = Random.nextInt(task.minRecurrenceDays, task.maxRecurrenceDays + 1)
            val nextDueDate = today.plusDays(daysUntilNextDue.toLong())
            val updatedTask = task.copy(
                lastCompletedDate = today,
                nextDueDate = nextDueDate
            )
            fakeTaskDao.updateTask(updatedTask.toEntity())
            // To make the preview update, FakeTaskDao's getAllTasks would ideally return a
            // MutableStateFlow that gets updated here.
            // For simplicity, current FakeTaskDao re-emits the whole list on each call to getAllTasks().
        }
    }

    override fun deleteTask(task: Task) {
        println("Preview: Delete task called - Task ID: ${task.id}")
        viewModelScope.launch {
            fakeTaskDao.deleteTask(task.toEntity())
        }
    }

    override fun updateTaskDetails(task: Task) {
        println("Preview: Update task details called - Task ID: ${task.id}")
        viewModelScope.launch {
            fakeTaskDao.updateTask(task.toEntity())
        }
    }
}