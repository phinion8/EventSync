package com.app.eventsync.data.models

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class CommentItem(
    val eventId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userProfilePhoto: String = "",
    val comment: String = "",
    @ServerTimestamp
    val timeStamp: Date? = null,
    val commentId: String = ""
)
