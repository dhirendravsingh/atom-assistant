package com.dhiren.atom.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import java.time.Instant

interface ReminderAlarmScheduler {
    fun schedule(reminderId: Long, title: String, triggerAt: Instant)
    fun cancel(reminderId: Long)
}

object NoOpReminderAlarmScheduler : ReminderAlarmScheduler {
    override fun schedule(reminderId: Long, title: String, triggerAt: Instant) = Unit
    override fun cancel(reminderId: Long) = Unit
}

class AndroidReminderAlarmScheduler(context: Context) : ReminderAlarmScheduler {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    override fun schedule(reminderId: Long, title: String, triggerAt: Instant) {
        val operation = requireNotNull(
            pendingIntent(reminderId, title, triggerAt, PendingIntent.FLAG_UPDATE_CURRENT),
        )
        val triggerMillis = triggerAt.toEpochMilli()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, operation)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, operation)
        }
    }

    override fun cancel(reminderId: Long) {
        val operation = pendingIntent(
            reminderId = reminderId,
            title = "",
            triggerAt = null,
            extraFlags = PendingIntent.FLAG_NO_CREATE,
        ) ?: return
        alarmManager.cancel(operation)
        operation.cancel()
    }

    private fun pendingIntent(
        reminderId: Long,
        title: String,
        triggerAt: Instant?,
        extraFlags: Int,
    ): PendingIntent? {
        val intent = Intent(appContext, ReminderAlarmReceiver::class.java).apply {
            action = AlarmContract.ActionFire
            data = Uri.parse("atom://reminder/$reminderId/alarm")
            putExtra(AlarmContract.ExtraReminderId, reminderId)
            putExtra(AlarmContract.ExtraReminderTitle, title)
            triggerAt?.let { putExtra(AlarmContract.ExtraScheduledAtUtc, it.toString()) }
        }
        return PendingIntent.getBroadcast(
            appContext,
            AlarmContract.stableRequestCode(reminderId, 0),
            intent,
            PendingIntent.FLAG_IMMUTABLE or extraFlags,
        )
    }
}
