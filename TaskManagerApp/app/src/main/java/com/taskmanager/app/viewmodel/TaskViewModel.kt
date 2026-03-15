package com.taskmanager.app.viewmodel

import androidx.lifecycle.*
import com.taskmanager.app.data.model.Task
import com.taskmanager.app.data.model.TaskStatus
import com.taskmanager.app.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val repository: TaskRepository
) : ViewModel() {

    val allTasks: LiveData<List<Task>> = repository.allTasks.asLiveData()
    val pendingTaskCount: LiveData<Int> = repository.pendingTaskCount.asLiveData()

    private val _currentFilter = MutableLiveData<TaskStatus?>(null)
    val currentFilter: LiveData<TaskStatus?> = _currentFilter

    val filteredTasks: LiveData<List<Task>> = _currentFilter.switchMap { filter ->
        if (filter == null) {
            repository.allTasks.asLiveData()
        } else {
            repository.getTasksByStatus(filter).asLiveData()
        }
    }

    private val _operationResult = MutableLiveData<OperationResult>()
    val operationResult: LiveData<OperationResult> = _operationResult

    fun setFilter(status: TaskStatus?) {
        _currentFilter.value = status
    }

    fun insertTask(task: Task, onTaskInserted: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val id = repository.insertTask(task)
            _operationResult.postValue(OperationResult.Success("Tarefa criada com sucesso"))
            onTaskInserted?.invoke(id)
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task.copy(updatedAt = System.currentTimeMillis()))
            _operationResult.postValue(OperationResult.Success("Tarefa atualizada"))
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
            _operationResult.postValue(OperationResult.Success("Tarefa eliminada"))
        }
    }

    fun completeTask(taskId: Long) {
        viewModelScope.launch {
            repository.completeTask(taskId)
            _operationResult.postValue(OperationResult.Success("Tarefa concluída!"))
        }
    }

    fun getTaskById(id: Long, onResult: (Task?) -> Unit) {
        viewModelScope.launch {
            onResult(repository.getTaskById(id))
        }
    }

    sealed class OperationResult {
        data class Success(val message: String) : OperationResult()
        data class Error(val message: String) : OperationResult()
    }
}
