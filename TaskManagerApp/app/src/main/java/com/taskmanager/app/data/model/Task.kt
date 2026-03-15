package com.taskmanager.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TaskPriority { LOW, MEDIUM, HIGH }
enum class TaskStatus { PENDING, IN_PROGRESS, COMPLETED }

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val status: TaskStatus = TaskStatus.PENDING,
    val alertTime: Long? = null,        // timestamp in millis for alarm
    val isAlertEnabled: Boolean = false,
    val alertSound: String = "default", // "default", "beep", "chime", "bell"
    val isRepeating: Boolean = false,
    val repeatIntervalMinutes: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
