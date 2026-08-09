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
                InitialPermissionStep.FullScreenAlarms,
            ),
            initialPermissionPlan(
                notificationsGranted = false,
                microphoneGranted = false,
                exactAlarmAccessGranted = false,
                fullScreenAlarmAccessGranted = false,
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
                fullScreenAlarmAccessGranted = true,
            ),
        )
        assertTrue(
            initialPermissionPlan(
                notificationsGranted = true,
                microphoneGranted = true,
                exactAlarmAccessGranted = true,
                fullScreenAlarmAccessGranted = true,
            ).isEmpty(),
        )
    }

    @Test
    fun `full-screen alarm access remains required when other access is ready`() {
        assertEquals(
            listOf(InitialPermissionStep.FullScreenAlarms),
            initialPermissionPlan(
                notificationsGranted = true,
                microphoneGranted = true,
                exactAlarmAccessGranted = true,
                fullScreenAlarmAccessGranted = false,
            ),
        )
    }
}
