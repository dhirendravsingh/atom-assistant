package com.dhiren.atom.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.time.Instant

data class DeviceReliabilitySnapshot(
    val notificationPermissionGranted: Boolean,
    val exactAlarmAccessGranted: Boolean,
    val fullScreenAccessGranted: Boolean,
    val batteryOptimizationExempt: Boolean,
    val lastSuccessfulReconciliation: Instant?,
    val lastReconciliationReason: AlarmReconciliationReason?,
    val lastScheduledAlarmCount: Int,
    val lastMissedReminderCount: Int,
) {
    fun missingAlarmRequirements(alarmModeEnabled: Boolean): List<String> = listOfNotNull(
        if (!notificationPermissionGranted) "notification permission" else null,
        if (!exactAlarmAccessGranted) "exact alarm access" else null,
        if (alarmModeEnabled && !fullScreenAccessGranted) "full-screen access" else null,
    )
}

class DeviceReliabilityMonitor(
    context: Context,
    private val preferences: ReliabilityPreferences,
) {
    private val appContext = context.applicationContext

    fun snapshot(): DeviceReliabilitySnapshot {
        val alarmManager = appContext.getSystemService(AlarmManager::class.java)
        val notificationManager = appContext.getSystemService(NotificationManager::class.java)
        val powerManager = appContext.getSystemService(PowerManager::class.java)
        val runtimeNotificationPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val reminderChannelEnabled = Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            notificationManager.getNotificationChannel(AlarmContract.ReminderChannelId)?.importance !=
            NotificationManager.IMPORTANCE_NONE
        return DeviceReliabilitySnapshot(
            notificationPermissionGranted = runtimeNotificationPermission &&
                NotificationManagerCompat.from(appContext).areNotificationsEnabled() &&
                reminderChannelEnabled,
            exactAlarmAccessGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                alarmManager.canScheduleExactAlarms(),
            fullScreenAccessGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
                notificationManager.canUseFullScreenIntent(),
            batteryOptimizationExempt = powerManager.isIgnoringBatteryOptimizations(appContext.packageName),
            lastSuccessfulReconciliation = preferences.lastSuccessfulReconciliation,
            lastReconciliationReason = preferences.lastReconciliationReason,
            lastScheduledAlarmCount = preferences.lastScheduledAlarmCount,
            lastMissedReminderCount = preferences.lastMissedReminderCount,
        )
    }
}
