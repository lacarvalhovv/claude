package com.taskmanager.app.ui.addtask

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.taskmanager.app.R
import com.taskmanager.app.data.model.Task
import com.taskmanager.app.data.model.TaskPriority
import com.taskmanager.app.data.model.TaskStatus
import com.taskmanager.app.databinding.ActivityAddEditTaskBinding
import com.taskmanager.app.notifications.AlarmScheduler
import com.taskmanager.app.viewmodel.TaskViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class AddEditTaskActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
    }

    private lateinit var binding: ActivityAddEditTaskBinding
    private val viewModel: TaskViewModel by viewModels()

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    private var existingTask: Task? = null
    private var selectedAlertTime: Calendar? = null
    private var selectedAlertSound: String = "default"

    private val soundOptions = arrayOf("Padrão", "Bip", "Chime", "Sino", "Urgente")
    private val soundKeys = arrayOf("default", "beep", "chime", "bell", "urgent")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        if (taskId != -1L) {
            loadTask(taskId)
            supportActionBar?.title = getString(R.string.edit_task)
        } else {
            supportActionBar?.title = getString(R.string.add_task)
        }

        setupUI()
    }

    private fun loadTask(taskId: Long) {
        viewModel.getTaskById(taskId) { task ->
            task?.let {
                existingTask = it
                populateFields(it)
            }
        }
    }

    private fun populateFields(task: Task) {
        binding.apply {
            editTextTitle.setText(task.title)
            editTextDescription.setText(task.description)

            when (task.priority) {
                TaskPriority.LOW -> radioLow.isChecked = true
                TaskPriority.MEDIUM -> radioMedium.isChecked = true
                TaskPriority.HIGH -> radioHigh.isChecked = true
            }

            when (task.status) {
                TaskStatus.PENDING -> radioStatusPending.isChecked = true
                TaskStatus.IN_PROGRESS -> radioStatusInProgress.isChecked = true
                TaskStatus.COMPLETED -> radioStatusCompleted.isChecked = true
            }

            switchAlert.isChecked = task.isAlertEnabled
            layoutAlertOptions.visibility = if (task.isAlertEnabled) View.VISIBLE else View.GONE
            switchRepeating.isChecked = task.isRepeating

            if (task.alertTime != null) {
                selectedAlertTime = Calendar.getInstance().apply { timeInMillis = task.alertTime }
                updateAlertTimeDisplay()
            }

            selectedAlertSound = task.alertSound
            val soundIndex = soundKeys.indexOf(task.alertSound).coerceAtLeast(0)
            textSelectedSound.text = soundOptions[soundIndex]

            if (task.isRepeating) {
                editRepeatMinutes.setText(task.repeatIntervalMinutes.toString())
            }
        }
    }

    private fun setupUI() {
        binding.apply {
            // Alert toggle
            switchAlert.setOnCheckedChangeListener { _, isChecked ->
                layoutAlertOptions.visibility = if (isChecked) View.VISIBLE else View.GONE
            }

            // Pick date/time for alert
            buttonPickDateTime.setOnClickListener { pickDateTime() }

            // Sound selection
            buttonSelectSound.setOnClickListener { showSoundPicker() }

            // Save button
            buttonSave.setOnClickListener { saveTask() }
        }
    }

    private fun pickDateTime() {
        val calendar = selectedAlertTime ?: Calendar.getInstance()

        DatePickerDialog(
            this,
            { _, year, month, day ->
                TimePickerDialog(
                    this,
                    { _, hour, minute ->
                        selectedAlertTime = Calendar.getInstance().apply {
                            set(year, month, day, hour, minute, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        updateAlertTimeDisplay()
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis()
        }.show()
    }

    private fun updateAlertTimeDisplay() {
        selectedAlertTime?.let {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            binding.textSelectedDateTime.text = sdf.format(it.time)
        }
    }

    private fun showSoundPicker() {
        val currentIndex = soundKeys.indexOf(selectedAlertSound).coerceAtLeast(0)
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.select_sound))
            .setSingleChoiceItems(soundOptions, currentIndex) { dialog, which ->
                selectedAlertSound = soundKeys[which]
                binding.textSelectedSound.text = soundOptions[which]
                dialog.dismiss()
            }
            .show()
    }

    private fun saveTask() {
        val title = binding.editTextTitle.text.toString().trim()
        if (title.isEmpty()) {
            binding.editTextTitle.error = getString(R.string.title_required)
            return
        }

        val description = binding.editTextDescription.text.toString().trim()

        val priority = when {
            binding.radioHigh.isChecked -> TaskPriority.HIGH
            binding.radioLow.isChecked -> TaskPriority.LOW
            else -> TaskPriority.MEDIUM
        }

        val status = when {
            binding.radioStatusInProgress.isChecked -> TaskStatus.IN_PROGRESS
            binding.radioStatusCompleted.isChecked -> TaskStatus.COMPLETED
            else -> TaskStatus.PENDING
        }

        val isAlertEnabled = binding.switchAlert.isChecked
        val alertTime = if (isAlertEnabled) selectedAlertTime?.timeInMillis else null

        if (isAlertEnabled && alertTime == null) {
            Toast.makeText(this, getString(R.string.select_alert_time), Toast.LENGTH_SHORT).show()
            return
        }

        if (isAlertEnabled && alertTime != null && alertTime <= System.currentTimeMillis()) {
            Toast.makeText(this, getString(R.string.alert_time_past), Toast.LENGTH_SHORT).show()
            return
        }

        val isRepeating = binding.switchRepeating.isChecked
        val repeatMinutes = if (isRepeating) {
            binding.editRepeatMinutes.text.toString().toIntOrNull() ?: 0
        } else 0

        val task = existingTask?.copy(
            title = title,
            description = description,
            priority = priority,
            status = status,
            alertTime = alertTime,
            isAlertEnabled = isAlertEnabled,
            alertSound = selectedAlertSound,
            isRepeating = isRepeating,
            repeatIntervalMinutes = repeatMinutes,
            updatedAt = System.currentTimeMillis()
        ) ?: Task(
            title = title,
            description = description,
            priority = priority,
            status = status,
            alertTime = alertTime,
            isAlertEnabled = isAlertEnabled,
            alertSound = selectedAlertSound,
            isRepeating = isRepeating,
            repeatIntervalMinutes = repeatMinutes
        )

        if (existingTask != null) {
            // Cancel old alarm if it existed
            existingTask?.let { old ->
                if (old.isAlertEnabled) alarmScheduler.cancelAlarm(old.id)
            }
            viewModel.updateTask(task)
            scheduleAlarmIfNeeded(task.copy(id = existingTask!!.id))
        } else {
            viewModel.insertTask(task) { newId ->
                scheduleAlarmIfNeeded(task.copy(id = newId))
            }
        }

        finish()
    }

    private fun scheduleAlarmIfNeeded(task: Task) {
        if (task.isAlertEnabled && task.alertTime != null && task.alertTime > System.currentTimeMillis()) {
            alarmScheduler.scheduleAlarm(task)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
