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
        if (
            intent.action !in setOf(
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED,
                Intent.ACTION_LOCALE_CHANGED,
            )
        ) return
        val application = context.applicationContext as AtomApplication
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                application.reminderRepository.reconcileAlarms()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
