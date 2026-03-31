package com.example.primer.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.primer.data.SettingsRepository
import java.util.Calendar

internal fun computeNextAlarmMillis(nowMillis: Long, hour: Int, minute: Int): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = nowMillis
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    if (cal.timeInMillis <= nowMillis) {
        cal.add(Calendar.DAY_OF_YEAR, 1)
    }
    return cal.timeInMillis
}

interface IAlarmScheduler {
    fun scheduleNext()
}

class DefaultAlarmScheduler(private val context: Context) : IAlarmScheduler {
    override fun scheduleNext() = AlarmScheduler.scheduleNext(context)
}

object AlarmScheduler {
    private const val ACTION_ALARM_FIRE = "com.example.primer.ACTION_ALARM_FIRE"

    fun scheduleNext(context: Context) {
        val settings = SettingsRepository(context).loadSettings()
        val triggerMillis = computeNextAlarmMillis(
            System.currentTimeMillis(),
            settings.notificationHour,
            settings.notificationMinute
        )
        context.getSystemService(AlarmManager::class.java)
            .setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, buildPendingIntent(context))
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java)
            .cancel(buildPendingIntent(context))
    }

    private fun buildPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_FIRE
        }
        return PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
