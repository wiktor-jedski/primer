package com.example.primer.notification

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class NextAlarmTimeTest {

    private fun calAt(hour: Int, minute: Int, offsetDays: Int = 0): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (offsetDays != 0) add(Calendar.DAY_OF_YEAR, offsetDays)
        }.timeInMillis

    @Test
    fun `configured time 2 hours from now fires today`() {
        val now = calAt(10, 30)
        val result = computeNextAlarmMillis(now, 12, 30)
        assertEquals(calAt(12, 30), result)
    }

    @Test
    fun `configured time 1 minute ago fires tomorrow`() {
        val now = calAt(10, 30)
        val result = computeNextAlarmMillis(now, 10, 29)
        assertEquals(calAt(10, 29, offsetDays = 1), result)
    }

    @Test
    fun `configured time exactly now fires tomorrow`() {
        val now = calAt(10, 30)
        val result = computeNextAlarmMillis(now, 10, 30)
        assertEquals(calAt(10, 30, offsetDays = 1), result)
    }

    @Test
    fun `midnight edge - configured 00 00 current 23 59 fires tomorrow`() {
        val now = calAt(23, 59)
        val result = computeNextAlarmMillis(now, 0, 0)
        assertEquals(calAt(0, 0, offsetDays = 1), result)
    }

    @Test
    fun `configured 23 59 current 00 00 fires today`() {
        val now = calAt(0, 0)
        val result = computeNextAlarmMillis(now, 23, 59)
        assertEquals(calAt(23, 59), result)
    }
}
