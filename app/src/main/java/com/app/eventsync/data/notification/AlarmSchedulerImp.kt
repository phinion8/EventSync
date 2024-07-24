package com.app.eventsync.data.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.app.eventsync.data.models.AlarmItem
import com.app.eventsync.utils.Constants
import kotlin.random.Random

class AlarmSchedulerImp(
    private val context: Context
) : AlarmScheduler {


    private val alarmManager = context.getSystemService(AlarmManager::class.java)


    override fun schedule(item: AlarmItem) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(Constants.NOTIFICATION_CHANNEL_ID, item.channelId)
            putExtra(Constants.NOTIFICATION_CHANNEL_NAME, item.channelName)
            putExtra(Constants.NOTIFICATION_CHANNEL_DESCRIPTION, item.channelDescription)
            putExtra(Constants.NOTIFICATION_TITLE, item.title)
            putExtra(Constants.NOTIFICATION_DESCRIPTION, item.message)
        }
        val random = Random(100000)
        val id1 = random.nextInt()
        val id2 = random.nextInt()
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            item.time.time - 1800000,
            PendingIntent.getBroadcast(
                context,
                id1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            item.time.time - 300000,
            PendingIntent.getBroadcast(
                context,
                id2,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
    }

    override fun cancel(item: AlarmItem) {
        alarmManager.cancel(
            PendingIntent.getBroadcast(
                context,
                item.hashCode(),
                Intent(),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
    }
}