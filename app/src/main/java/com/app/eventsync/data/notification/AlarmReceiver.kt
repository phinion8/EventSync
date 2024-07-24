package com.app.eventsync.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.app.eventsync.utils.Constants

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {

        val channelId = intent?.getStringExtra(Constants.NOTIFICATION_CHANNEL_ID)
        val channelDescription = intent?.getStringExtra(Constants.NOTIFICATION_CHANNEL_DESCRIPTION)
        val channelName = intent?.getStringExtra(Constants.NOTIFICATION_CHANNEL_NAME)
        val notificationTitle = intent?.getStringExtra(Constants.NOTIFICATION_TITLE)
        val notificationDescription = intent?.getStringExtra(Constants.NOTIFICATION_DESCRIPTION)

        val notificationManager = NotificationManager(
            context!!,
            channelId!!,
            channelName!!,
            channelDescription!!,
            notificationTitle!!,
            notificationDescription!!
        )
        notificationManager.notifyUser()

    }
}