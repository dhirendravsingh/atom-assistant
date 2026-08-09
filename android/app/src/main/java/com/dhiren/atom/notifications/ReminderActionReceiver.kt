package com.dhiren.atom.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dhiren.atom.AtomApplication
import java.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(AlarmContract.ExtraReminderId, 0L)
        if (reminderId <= 0L) return
        val application = context.applicationContext as AtomApplication
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    AlarmContract.ActionDone -> application.reminderRepository.complete(reminderId)
                    AlarmContract.ActionSnooze -> application.reminderRepository.snooze(
                        reminderId,
                        Duration.ofMinutes(AlarmContract.DefaultSnoozeMinutes),
                    )
                    AlarmContract.ActionRemindAgain -> application.reminderRepository.remindAgain(
                        reminderId,
                        Duration.ofMinutes(AlarmContract.DefaultRemindAgainMinutes),
                    )
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
