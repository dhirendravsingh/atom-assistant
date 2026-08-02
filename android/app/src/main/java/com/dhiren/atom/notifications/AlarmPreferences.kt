package com.dhiren.atom.notifications

import android.content.Context

class AlarmPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("atom_alarm_preferences", Context.MODE_PRIVATE)

    var alarmModeEnabled: Boolean
        get() = preferences.getBoolean(AlarmModeKey, true)
        set(value) {
            preferences.edit().putBoolean(AlarmModeKey, value).apply()
        }

    private companion object {
        const val AlarmModeKey = "alarm_mode_enabled"
    }
}
