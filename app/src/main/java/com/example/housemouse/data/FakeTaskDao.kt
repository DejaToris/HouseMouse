package com.example.housemouse.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate

class FakeTaskDao : TaskDao {
    private val tasks = mutableListOf<TaskEntity>()

    init {
        // Add some sample data
        val today = LocalDate.now()
        tasks.addAll(listOf(
            TaskEntity(
                id = "1",
                name = "Vacuum Living Room (Preview)",
                minRecurrenceDays = 3,
                maxRecurrenceDays = 5,
                lastCompletedDate = today.minusDays(4),
                nextDueDate = today
            ),
            TaskEntity(
                id = "2",
                name = "Load Dishwasher (Preview)",
                minRecurrenceDays = 1,
                maxRecurrenceDays = 1,
                lastCompletedDate = today.minusDays(2),
                nextDueDate = today.minusDays(1) // Overdue
            ),
            TaskEntity(
                id = "3",
                name = "Clean Bathrooms (Preview)",
                minRecurrenceDays = 7,
                maxRecurrenceDays = 10,
                lastCompletedDate = today.minusDays(1),
                nextDueDate = today.plusDays(6) // Due in future
            ),
            TaskEntity(
                id = "4",
                name = "Take out Trash (Preview)",
                minRecurrenceDays = 2,
                maxRecurrenceDays = 3,
                lastCompletedDate = null, // Never completed
                nextDueDate = today.plusDays(1)
            )
        ))
    }

    override suspend fun insertTask(task: TaskEntity) {
        tasks.removeAll { it.id == task.id } // Replace if exists
        tasks.add(task)
    }

    override suspend fun updateTask(task: TaskEntity) {
        val index = tasks.indexOfFirst { it.id == task.id }
        if (index != -1) {
            tasks[index] = task
        }
    }

    override suspend fun deleteTask(task: TaskEntity) {
        tasks.removeAll { it.id == task.id }
    }

    override fun getAllTasks(): Flow<List<TaskEntity>> = flowOf(tasks.toList().sortedBy { it.nextDueDate })

    override fun getDueTasks(currentDateEpochDay: Long): Flow<List<TaskEntity>> {
        val currentDate = LocalDate.ofEpochDay(currentDateEpochDay)
        return flowOf(tasks.filter { it.nextDueDate != null && !it.nextDueDate!!.isAfter(currentDate) })
    }

    override suspend fun getTaskById(taskId: String): TaskEntity? {
        return tasks.find { it.id == taskId }
    }
}