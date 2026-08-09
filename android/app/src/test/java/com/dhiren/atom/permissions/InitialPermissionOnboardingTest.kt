package com.dhiren.atom.permissions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InitialPermissionOnboardingTest {
    @Test
    fun `missing permissions are requested in a calm predictable order`() {
        assertEquals(
            listOf(
                InitialPermissionStep.Notifications,
                InitialPermissionStep.Microphone,
                InitialPermissionStep.ExactAlarms,
            ),
            initialPermissionPlan(
                notificationsGranted = false,
                microphoneGranted = false,
                exactAlarmAccessGranted = false,
            ),
        )
    }

    @Test
    fun `granted permissions are not requested again`() {
        assertEquals(
            listOf(InitialPermissionStep.ExactAlarms),
            initialPermissionPlan(
                notificationsGranted = true,
                microphoneGranted = true,
                exactAlarmAccessGranted = false,
            ),
        )
        assertTrue(
            initialPermissionPlan(
                notificationsGranted = true,
                microphoneGranted = true,
                exactAlarmAccessGranted = true,
            ).isEmpty(),
        )
    }
}
