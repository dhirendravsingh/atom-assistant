package com.dhiren.atom.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dhiren.atom.AtomApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmContract.ActionFire) return
        val reminderId = intent.getLongExtra(AlarmContract.ExtraReminderId, 0L)
        if (reminderId <= 0L) return
        val title = intent.getStringExtra(AlarmContract.ExtraReminderTitle).orEmpty().ifBlank { "Reminder" }
        val application = context.applicationContext as AtomApplication
        application.notificationCenter.showReminder(reminderId, title)

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                application.reminderRepository.advanceRecurringAfterDelivery(reminderId)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
