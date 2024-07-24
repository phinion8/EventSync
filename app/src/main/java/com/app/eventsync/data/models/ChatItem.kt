package com.app.eventsync.data.models

data class ChatItem(
    val id: String = "",
    val name: String = "",
    val lastMessage: String = "",
    val profileImage: String = "",
    val lastChatTime: Long = System.currentTimeMillis()
)
