package com.example.primer.notification

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = android.app.Application::class)
class AlarmSchedulerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    private fun setNotificationTime(hour: Int, minute: Int) {
        context.getSharedPreferences("primer_prefs", Context.MODE_PRIVATE)
            .edit()
            .putInt("notification_hour", hour)
            .putInt("notification_minute", minute)
            .apply()
    }

    @Test
    fun `scheduleNext creates exactly one alarm with correct hour and minute`() {
        setNotificationTime(10, 30)

        AlarmScheduler.scheduleNext(context)

        val shadow = shadowOf(context.getSystemService(AlarmManager::class.java))
        assertEquals(1, shadow.scheduledAlarms.size)

        val triggerCal = Calendar.getInstance().apply {
            timeInMillis = shadow.scheduledAlarms[0].triggerAtTime
        }
        assertEquals(10, triggerCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, triggerCal.get(Calendar.MINUTE))
    }

    @Test
    fun `cancel after scheduleNext leaves no pending alarms`() {
        setNotificationTime(10, 30)
        AlarmScheduler.scheduleNext(context)

        AlarmScheduler.cancel(context)

        val shadow = shadowOf(context.getSystemService(AlarmManager::class.java))
        assertEquals(0, shadow.scheduledAlarms.size)
    }

    @Test
    fun `scheduleNext called twice registers only one alarm`() {
        setNotificationTime(10, 30)

        AlarmScheduler.scheduleNext(context)
        AlarmScheduler.scheduleNext(context)

        val shadow = shadowOf(context.getSystemService(AlarmManager::class.java))
        assertEquals(1, shadow.scheduledAlarms.size)
    }
}
