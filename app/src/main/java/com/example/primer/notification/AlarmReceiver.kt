package com.example.primer.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "com.example.primer.ACTION_ALARM_FIRE" -> {
                NotificationHelper.postNotification(context)
                AlarmScheduler.scheduleNext(context)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                AlarmScheduler.scheduleNext(context)
            }
        }
    }
}
