package com.taskmanager.app.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.taskmanager.app.R
import com.taskmanager.app.data.model.Task
import com.taskmanager.app.data.model.TaskStatus
import com.taskmanager.app.databinding.ActivityMainBinding
import com.taskmanager.app.notifications.AlarmScheduler
import com.taskmanager.app.ui.addtask.AddEditTaskActivity
import com.taskmanager.app.viewmodel.TaskViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: TaskViewModel by viewModels()
    private lateinit var taskAdapter: TaskAdapter

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(this, getString(R.string.notification_permission_needed), Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        requestNotificationPermission()
        setupRecyclerView()
        setupFab()
        setupFilterChips()
        observeViewModel()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            onTaskClick = { task -> openEditTask(task) },
            onTaskComplete = { task -> viewModel.completeTask(task.id) },
            onTaskDelete = { task -> confirmDelete(task) }
        )

        binding.recyclerViewTasks.apply {
            adapter = taskAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }

        // Swipe to delete
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val task = taskAdapter.getTaskAt(viewHolder.adapterPosition)
                viewModel.deleteTask(task)
                Snackbar.make(binding.root, getString(R.string.task_deleted), Snackbar.LENGTH_LONG)
                    .setAction(getString(R.string.undo)) {
                        viewModel.insertTask(task)
                    }.show()
            }
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.recyclerViewTasks)
    }

    private fun setupFab() {
        binding.fabAddTask.setOnClickListener {
            startActivity(Intent(this, AddEditTaskActivity::class.java))
        }
    }

    private fun setupFilterChips() {
        binding.chipAll.setOnClickListener { viewModel.setFilter(null) }
        binding.chipPending.setOnClickListener { viewModel.setFilter(TaskStatus.PENDING) }
        binding.chipInProgress.setOnClickListener { viewModel.setFilter(TaskStatus.IN_PROGRESS) }
        binding.chipCompleted.setOnClickListener { viewModel.setFilter(TaskStatus.COMPLETED) }
    }

    private fun observeViewModel() {
        viewModel.filteredTasks.observe(this) { tasks ->
            taskAdapter.submitList(tasks)
            binding.textEmptyState.visibility =
                if (tasks.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }

        viewModel.pendingTaskCount.observe(this) { count ->
            supportActionBar?.subtitle = if (count > 0)
                resources.getQuantityString(R.plurals.pending_tasks, count, count)
            else
                getString(R.string.all_tasks_done)
        }

        viewModel.operationResult.observe(this) { result ->
            when (result) {
                is TaskViewModel.OperationResult.Success ->
                    Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                is TaskViewModel.OperationResult.Error ->
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openEditTask(task: Task) {
        val intent = Intent(this, AddEditTaskActivity::class.java).apply {
            putExtra(AddEditTaskActivity.EXTRA_TASK_ID, task.id)
        }
        startActivity(intent)
    }

    private fun confirmDelete(task: Task) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.delete_task))
            .setMessage(getString(R.string.delete_task_confirm, task.title))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                viewModel.deleteTask(task)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_clear_completed -> {
                confirmClearCompleted()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun confirmClearCompleted() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.clear_completed))
            .setMessage(getString(R.string.clear_completed_confirm))
            .setPositiveButton(getString(R.string.clear)) { _, _ ->
                viewModel.filteredTasks.value
                    ?.filter { it.status == TaskStatus.COMPLETED }
                    ?.forEach { viewModel.deleteTask(it) }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
}
