package com.dhiren.atom.permissions

import android.content.Context

enum class InitialPermissionStep {
    Notifications,
    Microphone,
    ExactAlarms,
}

fun initialPermissionPlan(
    notificationsGranted: Boolean,
    microphoneGranted: Boolean,
    exactAlarmAccessGranted: Boolean,
): List<InitialPermissionStep> = listOfNotNull(
    InitialPermissionStep.Notifications.takeUnless { notificationsGranted },
    InitialPermissionStep.Microphone.takeUnless { microphoneGranted },
    InitialPermissionStep.ExactAlarms.takeUnless { exactAlarmAccessGranted },
)

class InitialPermissionOnboardingPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "atom_permission_onboarding",
        Context.MODE_PRIVATE,
    )

    var completed: Boolean
        get() = preferences.getBoolean(CompletedKey, false)
        set(value) {
            preferences.edit().putBoolean(CompletedKey, value).apply()
        }

    private companion object {
        const val CompletedKey = "initial_permission_setup_completed"
    }
}
