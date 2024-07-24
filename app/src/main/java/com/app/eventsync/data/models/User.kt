package com.app.eventsync.data.models

import java.util.Calendar
import java.util.Date

data class User(
    val id: String = "",
    var name: String = "",
    val email: String = "",
    val profilePhoto: String = "",
    val enrolledEvents: ArrayList<String> = ArrayList(),
    val createdEvents: ArrayList<String> = ArrayList(),
    val friendList: ArrayList<String> = ArrayList(),
    val friendRequests: ArrayList<String> = ArrayList(),
    val fcmToken: String = "",
    val availability: Int = 0,
    val lastChatMessage: String = "",
    val lastSeen: Date = Calendar.getInstance().time
)
