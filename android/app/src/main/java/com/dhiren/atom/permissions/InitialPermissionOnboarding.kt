package com.dhiren.atom.permissions

import android.content.Context

enum class InitialPermissionStep {
    Notifications,
    Microphone,
    ExactAlarms,
    FullScreenAlarms,
}

fun initialPermissionPlan(
    notificationsGranted: Boolean,
    microphoneGranted: Boolean,
    exactAlarmAccessGranted: Boolean,
    fullScreenAlarmAccessGranted: Boolean,
): List<InitialPermissionStep> = listOfNotNull(
    InitialPermissionStep.Notifications.takeUnless { notificationsGranted },
    InitialPermissionStep.Microphone.takeUnless { microphoneGranted },
    InitialPermissionStep.ExactAlarms.takeUnless { exactAlarmAccessGranted },
    InitialPermissionStep.FullScreenAlarms.takeUnless { fullScreenAlarmAccessGranted },
)

class InitialPermissionOnboardingPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "atom_permission_onboarding",
        Context.MODE_PRIVATE,
    )

    var completed: Boolean
        get() = preferences.getInt(CompletedVersionKey, 0) >= CurrentVersion
        set(value) {
            preferences.edit().putInt(CompletedVersionKey, if (value) CurrentVersion else 0).apply()
        }

    private companion object {
        // Version 2 adds Android 14+ full-screen alarm access. Existing installs
        // receive this setup once after updating, without being prompted forever.
        const val CurrentVersion = 2
        const val CompletedVersionKey = "initial_permission_setup_completed_version"
    }
}
