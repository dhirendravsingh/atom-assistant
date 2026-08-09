package com.dhiren.atom.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AlarmContractTest {
    @Test
    fun `request codes are stable for the same reminder and action`() {
        assertEquals(
            AlarmContract.stableRequestCode(4_294_967_300L, 2),
            AlarmContract.stableRequestCode(4_294_967_300L, 2),
        )
    }

    @Test
    fun `alarm actions use distinct pending intent request codes`() {
        val id = 42L

        assertNotEquals(AlarmContract.stableRequestCode(id, 1), AlarmContract.stableRequestCode(id, 2))
        assertNotEquals(AlarmContract.stableRequestCode(id, 2), AlarmContract.stableRequestCode(id, 3))
    }
}
