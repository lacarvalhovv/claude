package com.taskmanager.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.taskmanager.app.data.model.Task
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SnoozeReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SNOOZE = "com.taskmanager.SNOOZE_ACTION"
        const val ACTION_DISMISS = "com.taskmanager.DISMISS_ACTION"
        const val SNOOZE_MINUTES = 10
    }

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(AlarmReceiver.EXTRA_TASK_ID, -1L)
        if (taskId == -1L) return

        // Dismiss the notification
        NotificationManagerCompat.from(context).cancel(taskId.toInt())

        when (intent.action) {
            ACTION_SNOOZE -> {
                val taskTitle = intent.getStringExtra(AlarmReceiver.EXTRA_TASK_TITLE) ?: ""
                val taskDescription = intent.getStringExtra(AlarmReceiver.EXTRA_TASK_DESCRIPTION) ?: ""
                val snoozeTime = System.currentTimeMillis() + (SNOOZE_MINUTES * 60 * 1000L)
                val snoozeTask = Task(
                    id = taskId,
                    title = taskTitle,
                    description = taskDescription,
                    alertTime = snoozeTime,
                    isAlertEnabled = true,
                    alertSound = "default"
                )
                alarmScheduler.scheduleAlarm(snoozeTask)
            }
            ACTION_DISMISS -> {
                alarmScheduler.cancelAlarm(taskId)
            }
        }
    }
}
