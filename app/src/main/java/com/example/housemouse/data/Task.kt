package com.example.housemouse.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.time.LocalDate
import java.util.UUID

// --- Domain Model ---
data class Task(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val minRecurrenceDays: Int,
    val maxRecurrenceDays: Int,
    var lastCompletedDate: LocalDate?,
    var nextDueDate: LocalDate?
)

// --- Room Entity ---
@Entity(tableName = "tasks")
@TypeConverters(Converters::class) // For LocalDate to Long conversion
data class TaskEntity(
    @PrimaryKey val id: String,
    val name: String,
    val minRecurrenceDays: Int,
    val maxRecurrenceDays: Int,
    var lastCompletedDate: LocalDate?,
    var nextDueDate: LocalDate?
)

// --- Mappers ---
fun Task.toEntity(): TaskEntity {
    return TaskEntity(
        id = this.id,
        name = this.name,
        minRecurrenceDays = this.minRecurrenceDays,
        maxRecurrenceDays = this.maxRecurrenceDays,
        lastCompletedDate = this.lastCompletedDate,
        nextDueDate = this.nextDueDate
    )
}

fun TaskEntity.toDomainModel(): Task {
    return Task(
        id = this.id,
        name = this.name,
        minRecurrenceDays = this.minRecurrenceDays,
        maxRecurrenceDays = this.maxRecurrenceDays,
        lastCompletedDate = this.lastCompletedDate,
        nextDueDate = this.nextDueDate
    )
}

// --- Room Type Converters for LocalDate ---
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDate? {
        return value?.let { LocalDate.ofEpochDay(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDate?): Long? {
        return date?.toEpochDay()
    }
}