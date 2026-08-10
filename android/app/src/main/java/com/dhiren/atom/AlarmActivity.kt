package com.dhiren.atom

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.dhiren.atom.notifications.AlarmContract
import com.dhiren.atom.notifications.AlarmRinger
import com.dhiren.atom.data.NotificationHistoryEventType
import java.time.Instant
import com.dhiren.atom.ui.AtomAlarmScreen
import com.dhiren.atom.ui.AtomTheme
import java.time.Duration
import kotlinx.coroutines.launch

class AlarmActivity : ComponentActivity() {
    private lateinit var ringer: AlarmRinger
    private var reminderId = 0L
    private var dismissReceiverRegistered = false
    private val dismissReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (
                intent.action == AlarmContract.ActionDismissAlarmUi &&
                intent.getLongExtra(AlarmContract.ExtraReminderId, 0L) == reminderId
            ) {
                if (::ringer.isInitialized) ringer.stop()
                finishAndRemoveTask()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        reminderId = intent.getLongExtra(AlarmContract.ExtraReminderId, 0L)
        val title = intent.getStringExtra(AlarmContract.ExtraReminderTitle).orEmpty().ifBlank { "Reminder" }
        if (reminderId <= 0L) {
            finish()
            return
        }
        ContextCompat.registerReceiver(
            this,
            dismissReceiver,
            IntentFilter(AlarmContract.ActionDismissAlarmUi),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        dismissReceiverRegistered = true
        ringer = AlarmRinger(this)
        ringer.start()
        val application = applicationContext as AtomApplication
        setContent {
            var busy = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
            BackHandler(enabled = true) { }
            AtomTheme(darkTheme = true) {
                AtomAlarmScreen(
                    title = title,
                    busy = busy.value,
                    onDone = {
                        busy.value = true
                        finishWithAction {
                            application.reminderRepository.complete(reminderId)
                            application.notificationHistoryRepository.record(
                                reminderId,
                                title,
                                NotificationHistoryEventType.Completed,
                                "Marked done",
                            )
                        }
                    },
                    onSnooze = {
                        busy.value = true
                        finishWithAction {
                            application.reminderRepository.snooze(
                                reminderId,
                                Duration.ofMinutes(AlarmContract.DefaultSnoozeMinutes),
                            )
                            application.notificationHistoryRepository.record(
                                reminderId,
                                title,
                                NotificationHistoryEventType.Snoozed,
                                "Snoozed for 10 minutes",
                                Instant.now().plusSeconds(AlarmContract.DefaultSnoozeMinutes * 60),
                            )
                        }
                    },
                    onRemindAgain = {
                        busy.value = true
                        finishWithAction {
                            application.reminderRepository.remindAgain(
                                reminderId,
                                Duration.ofMinutes(AlarmContract.DefaultRemindAgainMinutes),
                            )
                            application.notificationHistoryRepository.record(
                                reminderId,
                                title,
                                NotificationHistoryEventType.RemindedAgain,
                                "Asked Atom to remind again in 1 hour",
                                Instant.now().plusSeconds(AlarmContract.DefaultRemindAgainMinutes * 60),
                            )
                        }
                    },
                    onIgnore = {
                        busy.value = true
                        finishWithAction {
                            application.reminderRepository.ignore(reminderId)
                            application.notificationHistoryRepository.record(
                                reminderId,
                                title,
                                NotificationHistoryEventType.Ignored,
                                "Ignored",
                            )
                        }
                    },
                )
            }
        }
    }

    private fun finishWithAction(action: suspend () -> Unit) {
        ringer.stop()
        lifecycleScope.launch {
            runCatching { action() }
            getSystemService(NotificationManager::class.java).cancel(
                AlarmContract.notificationId(intent.getLongExtra(AlarmContract.ExtraReminderId, 0L)),
            )
            finishAndRemoveTask()
        }
    }

    override fun onDestroy() {
        if (dismissReceiverRegistered) {
            unregisterReceiver(dismissReceiver)
            dismissReceiverRegistered = false
        }
        if (::ringer.isInitialized) ringer.stop()
        super.onDestroy()
    }
}
