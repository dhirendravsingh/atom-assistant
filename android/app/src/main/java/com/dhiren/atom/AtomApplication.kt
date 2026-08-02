package com.dhiren.atom

import android.app.Application
import com.dhiren.atom.data.ReminderRepository
import com.dhiren.atom.data.local.AtomDatabase
import com.dhiren.atom.notifications.AndroidReminderAlarmScheduler
import com.dhiren.atom.notifications.AtomNotificationCenter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AtomApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AtomDatabase by lazy {
        AtomDatabase.getInstance(this)
    }

    val alarmScheduler by lazy {
        AndroidReminderAlarmScheduler(this)
    }

    val notificationCenter by lazy {
        AtomNotificationCenter(this)
    }

    val reminderRepository: ReminderRepository by lazy {
        ReminderRepository(
            reminderDao = database.reminderDao(),
            alarmScheduler = alarmScheduler,
        )
    }

    override fun onCreate() {
        super.onCreate()
        notificationCenter.createChannels()
        applicationScope.launch {
            reminderRepository.reconcileAlarms()
        }
    }
}
