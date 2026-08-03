package com.dhiren.atom

import android.app.Application
import com.dhiren.atom.data.ReminderRepository
import com.dhiren.atom.data.local.AtomDatabase
import com.dhiren.atom.notifications.AndroidReminderAlarmScheduler
import com.dhiren.atom.notifications.AlarmReconciliationReason
import com.dhiren.atom.notifications.AtomNotificationCenter
import com.dhiren.atom.notifications.DeviceReliabilityManager
import com.dhiren.atom.notifications.DeviceReliabilityMonitor
import com.dhiren.atom.notifications.ReliabilityPreferences
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

    private val reliabilityPreferences by lazy {
        ReliabilityPreferences(this)
    }

    private val deviceReliabilityMonitor by lazy {
        DeviceReliabilityMonitor(this, reliabilityPreferences)
    }

    val reminderRepository: ReminderRepository by lazy {
        ReminderRepository(
            reminderDao = database.reminderDao(),
            alarmScheduler = alarmScheduler,
        )
    }

    val deviceReliabilityManager by lazy {
        DeviceReliabilityManager(
            reminderRepository = reminderRepository,
            notificationCenter = notificationCenter,
            preferences = reliabilityPreferences,
            monitor = deviceReliabilityMonitor,
        )
    }

    override fun onCreate() {
        super.onCreate()
        notificationCenter.createChannels()
        applicationScope.launch {
            deviceReliabilityManager.reconcile(AlarmReconciliationReason.AppStart)
        }
    }
}
