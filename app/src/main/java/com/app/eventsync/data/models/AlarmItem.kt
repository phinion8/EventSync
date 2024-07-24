package com.app.eventsync.data.models

import java.util.Date

data class AlarmItem(
    val channelId: String,
    val channelName: String,
    val channelDescription: String,
    val time: Date,
    val title: String,
    val message: String,
)
