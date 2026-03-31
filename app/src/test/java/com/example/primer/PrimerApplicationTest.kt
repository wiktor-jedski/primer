package com.example.primer

import android.app.NotificationManager
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = PrimerApplication::class)
class PrimerApplicationTest {

    @Test
    fun `onCreate registers primer_daily notification channel`() {
        Robolectric.buildApplication(PrimerApplication::class.java).create().get()

        val nm = RuntimeEnvironment.getApplication()
            .getSystemService(NotificationManager::class.java)
        val channel = nm.getNotificationChannel("primer_daily")
        assertNotNull(channel)
    }
}
