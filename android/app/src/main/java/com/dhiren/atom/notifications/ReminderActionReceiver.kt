package com.dhiren.atom.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dhiren.atom.AtomApplication
import com.dhiren.atom.data.NotificationHistoryEventType
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(AlarmContract.ExtraReminderId, 0L)
        if (reminderId <= 0L) return
        val title = intent.getStringExtra(AlarmContract.ExtraReminderTitle).orEmpty().ifBlank { "Reminder" }
        val application = context.applicationContext as AtomApplication
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val now = Instant.now()
                when (intent.action) {
                    AlarmContract.ActionDone -> {
                        application.reminderRepository.complete(reminderId)
                        application.notificationHistoryRepository.record(
                            reminderId,
                            title,
                            NotificationHistoryEventType.Completed,
                            "Marked done",
                        )
                    }
                    AlarmContract.ActionSnooze -> {
                        application.reminderRepository.snooze(
                            reminderId,
                            Duration.ofMinutes(AlarmContract.DefaultSnoozeMinutes),
                        )
                        application.notificationHistoryRepository.record(
                            reminderId,
                            title,
                            NotificationHistoryEventType.Snoozed,
                            "Snoozed for 10 minutes",
                            now.plusSeconds(AlarmContract.DefaultSnoozeMinutes * 60),
                        )
                    }
                    AlarmContract.ActionRemindAgain -> {
                        application.reminderRepository.remindAgain(
                            reminderId,
                            Duration.ofMinutes(AlarmContract.DefaultRemindAgainMinutes),
                        )
                        application.notificationHistoryRepository.record(
                            reminderId,
                            title,
                            NotificationHistoryEventType.RemindedAgain,
                            "Asked Atom to remind again in 1 hour",
                            now.plusSeconds(AlarmContract.DefaultRemindAgainMinutes * 60),
                        )
                    }
                    AlarmContract.ActionIgnore -> {
                        application.reminderRepository.ignore(reminderId)
                        application.notificationHistoryRepository.record(
                            reminderId,
                            title,
                            NotificationHistoryEventType.Ignored,
                            "Ignored",
                        )
                    }
                    else -> return@launch
                }
                application.notificationCenter.dismiss(reminderId)
                context.sendBroadcast(
                    Intent(AlarmContract.ActionDismissAlarmUi)
                        .setPackage(context.packageName)
                        .putExtra(AlarmContract.ExtraReminderId, reminderId),
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
