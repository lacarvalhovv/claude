package com.taskmanager.app.data.repository

import com.taskmanager.app.data.dao.TaskDao
import com.taskmanager.app.data.model.Task
import com.taskmanager.app.data.model.TaskStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(private val taskDao: TaskDao) {

    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()
    val pendingTaskCount: Flow<Int> = taskDao.getPendingTaskCount()

    fun getTasksByStatus(status: TaskStatus): Flow<List<Task>> =
        taskDao.getTasksByStatus(status)

    suspend fun getTaskById(id: Long): Task? = taskDao.getTaskById(id)

    suspend fun insertTask(task: Task): Long = taskDao.insertTask(task)

    suspend fun updateTask(task: Task) = taskDao.updateTask(task)

    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)

    suspend fun deleteTaskById(id: Long) = taskDao.deleteTaskById(id)

    suspend fun completeTask(id: Long) {
        val now = System.currentTimeMillis()
        taskDao.updateTaskStatus(id, TaskStatus.COMPLETED, now, now)
    }

    suspend fun getPendingAlerts(): List<Task> =
        taskDao.getPendingAlerts(System.currentTimeMillis())
}
