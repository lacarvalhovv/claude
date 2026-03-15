package com.taskmanager.app.ui.main

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.taskmanager.app.R
import com.taskmanager.app.data.model.Task
import com.taskmanager.app.data.model.TaskPriority
import com.taskmanager.app.data.model.TaskStatus
import com.taskmanager.app.databinding.ItemTaskBinding
import java.text.SimpleDateFormat
import java.util.*

class TaskAdapter(
    private val onTaskClick: (Task) -> Unit,
    private val onTaskComplete: (Task) -> Unit,
    private val onTaskDelete: (Task) -> Unit
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun getTaskAt(position: Int): Task = getItem(position)

    inner class TaskViewHolder(private val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(task: Task) {
            binding.apply {
                textTaskTitle.text = task.title
                textTaskDescription.text = task.description
                textTaskDescription.visibility = if (task.description.isBlank()) View.GONE else View.VISIBLE

                // Priority indicator
                val priorityColor = when (task.priority) {
                    TaskPriority.HIGH -> ContextCompat.getColor(root.context, R.color.priority_high)
                    TaskPriority.MEDIUM -> ContextCompat.getColor(root.context, R.color.priority_medium)
                    TaskPriority.LOW -> ContextCompat.getColor(root.context, R.color.priority_low)
                }
                viewPriorityIndicator.setBackgroundColor(priorityColor)

                // Priority badge text
                textPriority.text = when (task.priority) {
                    TaskPriority.HIGH -> root.context.getString(R.string.priority_high)
                    TaskPriority.MEDIUM -> root.context.getString(R.string.priority_medium)
                    TaskPriority.LOW -> root.context.getString(R.string.priority_low)
                }
                textPriority.setTextColor(priorityColor)

                // Status
                val isCompleted = task.status == TaskStatus.COMPLETED
                checkboxComplete.isChecked = isCompleted

                if (isCompleted) {
                    textTaskTitle.paintFlags = textTaskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    textTaskTitle.alpha = 0.5f
                    textTaskDescription.alpha = 0.5f
                } else {
                    textTaskTitle.paintFlags = textTaskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    textTaskTitle.alpha = 1f
                    textTaskDescription.alpha = 1f
                }

                // Alert icon
                imageAlertIcon.visibility = if (task.isAlertEnabled && task.alertTime != null) View.VISIBLE else View.GONE

                // Alert time
                if (task.isAlertEnabled && task.alertTime != null) {
                    val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                    textAlertTime.text = sdf.format(Date(task.alertTime))
                    textAlertTime.visibility = View.VISIBLE
                } else {
                    textAlertTime.visibility = View.GONE
                }

                // Status chip
                chipStatus.text = when (task.status) {
                    TaskStatus.PENDING -> root.context.getString(R.string.status_pending)
                    TaskStatus.IN_PROGRESS -> root.context.getString(R.string.status_in_progress)
                    TaskStatus.COMPLETED -> root.context.getString(R.string.status_completed)
                }

                // Click listeners
                root.setOnClickListener { onTaskClick(task) }
                checkboxComplete.setOnClickListener {
                    if (task.status != TaskStatus.COMPLETED) {
                        onTaskComplete(task)
                    }
                }
                buttonDelete.setOnClickListener { onTaskDelete(task) }
            }
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Task, newItem: Task) = oldItem == newItem
    }
}
