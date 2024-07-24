package com.app.eventsync.data.models

import java.util.Date

data class Event(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val participantsList: ArrayList<String> = ArrayList(),
    val imageList: ArrayList<String> = ArrayList(),
    val videoList: ArrayList<String> = ArrayList(),
    val startTime: Date? = null,
    val repeated: Boolean = true,
    val endTime : Date? = null,
    val location: String = "",
    val requirements: String = "",
    val organizationType: String = "",
    val paymentRequirements: String = "",
    val availableSeats: Long = 100,
    val commentsAllowed: Boolean = true,
    val rating: String = "",
    val privateMode: Boolean = true
)