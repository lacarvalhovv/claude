package com.taskmanager.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.taskmanager.app.R
import com.taskmanager.app.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_ALARM = "com.taskmanager.ALARM_ACTION"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_TASK_DESCRIPTION = "extra_task_description"
        const val EXTRA_ALERT_SOUND = "extra_alert_sound"
        const val EXTRA_IS_REPEATING = "extra_is_repeating"
        const val EXTRA_REPEAT_INTERVAL = "extra_repeat_interval"

        const val CHANNEL_ID = "task_alerts_channel"
        const val CHANNEL_NAME = "Alertas de Tarefas"
        const val CHANNEL_DESCRIPTION = "Notificações de alertas para tarefas agendadas"
    }

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_ALARM -> handleAlarm(context, intent)
            Intent.ACTION_BOOT_COMPLETED -> handleBootCompleted(context)
        }
    }

    private fun handleAlarm(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Tarefa"
        val taskDescription = intent.getStringExtra(EXTRA_TASK_DESCRIPTION) ?: ""
        val alertSound = intent.getStringExtra(EXTRA_ALERT_SOUND) ?: "default"
        val isRepeating = intent.getBooleanExtra(EXTRA_IS_REPEATING, false)
        val repeatInterval = intent.getIntExtra(EXTRA_REPEAT_INTERVAL, 0)

        if (taskId == -1L) return

        createNotificationChannel(context)
        playAlertSound(context, alertSound)
        vibrateDevice(context)
        showNotification(context, taskId, taskTitle, taskDescription)

        // Schedule next repetition if needed
        if (isRepeating && repeatInterval > 0) {
            val nextTime = System.currentTimeMillis() + (repeatInterval * 60 * 1000L)
            val task = com.taskmanager.app.data.model.Task(
                id = taskId,
                title = taskTitle,
                description = taskDescription,
                alertTime = nextTime,
                isAlertEnabled = true,
                alertSound = alertSound,
                isRepeating = true,
                repeatIntervalMinutes = repeatInterval
            )
            alarmScheduler.scheduleAlarm(task)
        }
    }

    private fun handleBootCompleted(context: Context) {
        // Re-schedule alarms after reboot - handled by WorkManager in the Application class
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                enableLights(true)
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun playAlertSound(context: Context, soundKey: String) {
        try {
            val soundUri: Uri = when (soundKey) {
                "beep" -> Uri.parse("android.resource://${context.packageName}/${R.raw.sound_beep}")
                "chime" -> Uri.parse("android.resource://${context.packageName}/${R.raw.sound_chime}")
                "bell" -> Uri.parse("android.resource://${context.packageName}/${R.raw.sound_bell}")
                "urgent" -> Uri.parse("android.resource://${context.packageName}/${R.raw.sound_urgent}")
                else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }

            val ringtone = RingtoneManager.getRingtone(context, soundUri)
                ?: RingtoneManager.getRingtone(
                    context,
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                )
            ringtone?.play()
        } catch (e: Exception) {
            // Fallback to default notification sound
            try {
                val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(context, defaultUri)
                ringtone?.play()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun vibrateDevice(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500), -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500), -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 500, 200, 500), -1)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showNotification(context: Context, taskId: Long, title: String, description: String) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context, taskId.toInt(), openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, SnoozeReceiver::class.java).apply {
            action = SnoozeReceiver.ACTION_SNOOZE
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, title)
            putExtra(EXTRA_TASK_DESCRIPTION, description)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context, (taskId * 10).toInt(), snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, SnoozeReceiver::class.java).apply {
            action = SnoozeReceiver.ACTION_DISMISS
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context, (taskId * 10 + 1).toInt(), dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("⏰ $title")
            .setContentText(description.ifBlank { context.getString(R.string.task_reminder) })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(R.drawable.ic_snooze, context.getString(R.string.snooze_10min), snoozePendingIntent)
            .addAction(R.drawable.ic_dismiss, context.getString(R.string.dismiss), dismissPendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(taskId.toInt(), notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
