package com.app.eventsync.data.notification

import com.app.eventsync.data.models.AlarmItem

interface AlarmScheduler {
    fun schedule(item: AlarmItem)
    fun cancel(item: AlarmItem)
}