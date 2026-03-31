package com.example.primer.notification

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
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
class NotificationHelperTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `createChannel registers channel with correct id`() {
        NotificationHelper.createChannel(context)

        val nm = context.getSystemService(NotificationManager::class.java)
        assertEquals(1, nm.notificationChannels.size)
        assertEquals(NotificationHelper.CHANNEL_ID, nm.notificationChannels[0].id)
    }

    @Test
    fun `createChannel is idempotent`() {
        NotificationHelper.createChannel(context)
        NotificationHelper.createChannel(context)

        val nm = context.getSystemService(NotificationManager::class.java)
        assertEquals(1, nm.notificationChannels.size)
    }

    @Test
    fun `postNotification creates notification with correct title`() {
        NotificationHelper.createChannel(context)
        NotificationHelper.postNotification(context)

        val nm = context.getSystemService(NotificationManager::class.java)
        val notifications = shadowOf(nm).allNotifications
        assertEquals(1, notifications.size)
        assertEquals(
            "Time to prime your day",
            notifications[0].extras.getString(Notification.EXTRA_TITLE)
        )
    }
}
