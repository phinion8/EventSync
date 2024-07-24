package com.app.eventsync.data.models

import java.util.Date

data class ChatMessage(
    var senderId: String = "",
    var receiverId: String = "",
    var message: String = "",
    var dateTime: Date = Date()
)
