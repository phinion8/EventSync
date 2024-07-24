package com.app.eventsync.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.app.eventsync.MainActivity
import com.app.eventsync.R

class NotificationManager(
    private val context: Context,
    private val channelId: String,
    private val channelName: String,
    private val channelDescription: String,
    private val notificationTitle: String,
    private val notificationDescription: String,
) {


    private val intent = Intent(context, MainActivity::class.java)

    private val pendingIntent: PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )


    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private var builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.app_icon)
        .setContentTitle(notificationTitle)
        .setContentText(notificationDescription)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(
            pendingIntent
        )

    init {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = channelName
            val descriptionText = channelDescription
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system.
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun notifyUser() {
        notificationManager.notify(1234, builder.build())
    }


}