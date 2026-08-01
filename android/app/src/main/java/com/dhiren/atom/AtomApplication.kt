package com.dhiren.atom

import android.app.Application
import com.dhiren.atom.data.ReminderRepository
import com.dhiren.atom.data.local.AtomDatabase

class AtomApplication : Application() {
    val database: AtomDatabase by lazy {
        AtomDatabase.getInstance(this)
    }

    val reminderRepository: ReminderRepository by lazy {
        ReminderRepository(database.reminderDao())
    }
}
