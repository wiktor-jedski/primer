package com.example.primer

import android.app.Application
import com.example.primer.notification.NotificationHelper

class PrimerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
    }
}
