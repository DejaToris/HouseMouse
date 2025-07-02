package com.example.housemouse.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.housemouse.data.Task
import com.example.housemouse.ui.theme.HouseMouseTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun TaskItem(
    task: Task,
    onMarkComplete: () -> Unit,
    modifier: Modifier = Modifier // Allow passing modifiers from the caller
) {
    val today = LocalDate.now()
    val dueDate = task.nextDueDate
    val isOverdue = dueDate != null && dueDate.isBefore(today)
    val isDueToday = dueDate != null && dueDate.isEqual(today)

    val dueStatusText = when {
        dueDate == null -> "Not scheduled" // Should ideally not happen for active tasks
        isOverdue -> {
            val daysOverdue = ChronoUnit.DAYS.between(dueDate, today)
            "Overdue by $daysOverdue day(s)"
        }

        isDueToday -> "Due today"
        else -> {
            val daysUntilDue = ChronoUnit.DAYS.between(today, dueDate)
            "Due in $daysUntilDue day(s) (by ${dueDate.format(DateTimeFormatter.ofPattern("MMM d"))})"
        }
    }

    val statusColor = when {
        isOverdue -> Color.Red
        isDueToday -> Color(0xFFFFA500) // Orange
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dueStatusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor,
                    fontStyle = if (isOverdue || isDueToday) FontStyle.Italic else FontStyle.Normal
                )
                task.lastCompletedDate?.let {
                    Text(
                        text = "Last done: ${it.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = onMarkComplete,
                enabled = task.lastCompletedDate != today // Disable if already completed today
            ) {
                Text("Done")
            }
        }
    }
}

// You might want a preview for TaskItem as well
@Composable
@Preview(showBackground = true)
fun PreviewTaskItemDue() {
    val task = Task(
        id = "1",
        name = "Vacuum Floors",
        minRecurrenceDays = 3,
        maxRecurrenceDays = 5,
        lastCompletedDate = LocalDate.now().minusDays(4),
        nextDueDate = LocalDate.now()
    )
    HouseMouseTheme { // Assuming you have a theme
        TaskItem(task = task, onMarkComplete = {})
    }
}

@Composable
@Preview(showBackground = true)
fun PreviewTaskItemOverdue() {
    val task = Task(
        id = "2",
        name = "Load Dishwasher",
        minRecurrenceDays = 1,
        maxRecurrenceDays = 1,
        lastCompletedDate = LocalDate.now().minusDays(2),
        nextDueDate = LocalDate.now().minusDays(1)
    )
    HouseMouseTheme {
        TaskItem(task = task, onMarkComplete = {})
    }
}

@Composable
@Preview(showBackground = true)
fun PreviewTaskItemFuture() {
    val task = Task(
        id = "3",
        name = "Clean Bathrooms",
        minRecurrenceDays = 7,
        maxRecurrenceDays = 10,
        lastCompletedDate = LocalDate.now().minusDays(1),
        nextDueDate = LocalDate.now().plusDays(6)
    )
    HouseMouseTheme {
        TaskItem(task = task, onMarkComplete = {})
    }
}