package com.dhiren.atom.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dhiren.atom.AlarmActivity
import com.dhiren.atom.R

class AtomNotificationCenter(context: Context) {
    private val appContext = context.applicationContext
    private val systemManager = appContext.getSystemService(NotificationManager::class.java)

    fun createChannels() {
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val channel = NotificationChannel(
            AlarmContract.ReminderChannelId,
            AlarmContract.ReminderChannelName,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Time-sensitive Atom reminders"
            enableVibration(true)
            vibrationPattern = longArrayOf(0L, 700L, 350L, 700L)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            setBypassDnd(false)
            setSound(
                alarmSound,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
        }
        systemManager.createNotificationChannel(channel)
    }

    fun showReminder(reminderId: Long, title: String): Boolean {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        createChannels()
        val fullScreenIntent = PendingIntent.getActivity(
            appContext,
            AlarmContract.stableRequestCode(reminderId, 0),
            Intent(appContext, AlarmActivity::class.java).apply {
                action = AlarmContract.ActionFire
                data = Uri.parse("atom://reminder/$reminderId/ring")
                putExtra(AlarmContract.ExtraReminderId, reminderId)
                putExtra(AlarmContract.ExtraReminderTitle, title)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(appContext, AlarmContract.ReminderChannelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Atom reminder")
            .setContentText(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(title))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(fullScreenIntent)
            .addAction(0, "Done", actionPendingIntent(reminderId, title, AlarmContract.ActionDone, 1))
            .addAction(0, "Snooze 10 min", actionPendingIntent(reminderId, title, AlarmContract.ActionSnooze, 2))
            .addAction(0, "Remind in 1 hour", actionPendingIntent(reminderId, title, AlarmContract.ActionRemindAgain, 3))
        if (AlarmPreferences(appContext).alarmModeEnabled) {
            builder.setFullScreenIntent(fullScreenIntent, true)
        }
        val notification = builder.build()
        NotificationManagerCompat.from(appContext).notify(AlarmContract.notificationId(reminderId), notification)
        return true
    }

    fun dismiss(reminderId: Long) {
        systemManager.cancel(AlarmContract.notificationId(reminderId))
    }

    private fun actionPendingIntent(
        reminderId: Long,
        title: String,
        actionName: String,
        actionOffset: Int,
    ): PendingIntent = PendingIntent.getBroadcast(
        appContext,
        AlarmContract.stableRequestCode(reminderId, actionOffset),
        Intent(appContext, ReminderActionReceiver::class.java).apply {
            action = actionName
            data = Uri.parse("atom://reminder/$reminderId/action/$actionOffset")
            putExtra(AlarmContract.ExtraReminderId, reminderId)
            putExtra(AlarmContract.ExtraReminderTitle, title)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
