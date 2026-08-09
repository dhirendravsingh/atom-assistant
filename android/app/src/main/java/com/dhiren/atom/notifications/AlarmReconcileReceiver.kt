package com.dhiren.atom.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dhiren.atom.AtomApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AlarmReconcileReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reason = when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> AlarmReconciliationReason.Boot
            Intent.ACTION_MY_PACKAGE_REPLACED -> AlarmReconciliationReason.AppUpdate
            Intent.ACTION_TIME_CHANGED -> AlarmReconciliationReason.ClockChanged
            Intent.ACTION_TIMEZONE_CHANGED -> AlarmReconciliationReason.TimezoneChanged
            Intent.ACTION_LOCALE_CHANGED -> AlarmReconciliationReason.LocaleChanged
            AlarmContract.ActionExactAlarmPermissionStateChanged ->
                AlarmReconciliationReason.ExactAlarmPermissionChanged
            else -> return
        }
        val application = context.applicationContext as AtomApplication
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                application.deviceReliabilityManager.reconcile(reason)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
