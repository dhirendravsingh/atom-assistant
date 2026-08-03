package com.dhiren.atom.notifications

object AlarmContract {
    const val ReminderChannelId = "atom_reminders_v1"
    const val ReminderChannelName = "Reminder alarms"

    const val ActionFire = "com.dhiren.atom.action.FIRE_REMINDER"
    const val ActionDone = "com.dhiren.atom.action.DONE_REMINDER"
    const val ActionSnooze = "com.dhiren.atom.action.SNOOZE_REMINDER"
    const val ActionRemindAgain = "com.dhiren.atom.action.REMIND_AGAIN"
    const val ActionDismissAlarmUi = "com.dhiren.atom.action.DISMISS_ALARM_UI"
    const val ActionOpenMissed = "com.dhiren.atom.action.OPEN_MISSED_REMINDER"
    const val ActionExactAlarmPermissionStateChanged =
        "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"

    const val ExtraReminderId = "extra_reminder_id"
    const val ExtraReminderTitle = "extra_reminder_title"

    const val DefaultSnoozeMinutes = 10L
    const val DefaultRemindAgainMinutes = 60L

    fun notificationId(reminderId: Long): Int = stableRequestCode(reminderId, 0)

    fun stableRequestCode(reminderId: Long, actionOffset: Int): Int {
        val folded = (reminderId xor (reminderId ushr 32)).toInt() and 0x0FFFFFFF
        return folded or (actionOffset shl 28)
    }
}
