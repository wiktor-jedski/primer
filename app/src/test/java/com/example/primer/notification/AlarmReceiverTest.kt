package com.example.primer.notification

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = android.app.Application::class)
class AlarmReceiverTest {

    private lateinit var context: Context
    private lateinit var receiver: AlarmReceiver

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        receiver = AlarmReceiver()
        NotificationHelper.createChannel(context)
    }

    @Test
    fun `ACTION_ALARM_FIRE posts notification and schedules next alarm`() {
        receiver.onReceive(context, Intent("com.example.primer.ACTION_ALARM_FIRE"))

        val nm = context.getSystemService(NotificationManager::class.java)
        assertEquals(1, shadowOf(nm).allNotifications.size)

        val am = context.getSystemService(AlarmManager::class.java)
        assertEquals(1, shadowOf(am).scheduledAlarms.size)
    }

    @Test
    fun `BOOT_COMPLETED schedules alarm but posts no notification`() {
        receiver.onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))

        val nm = context.getSystemService(NotificationManager::class.java)
        assertEquals(0, shadowOf(nm).allNotifications.size)

        val am = context.getSystemService(AlarmManager::class.java)
        assertEquals(1, shadowOf(am).scheduledAlarms.size)
    }
}
