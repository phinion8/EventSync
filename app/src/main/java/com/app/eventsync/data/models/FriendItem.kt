package com.app.eventsync.data.models

data class FriendItem(
    val id: String = "",
    val name: String = "",
    val profilePhoto: String = "",
    val requestList: ArrayList<String> = ArrayList(),
    val friendList: ArrayList<String> = ArrayList()
)

enum class FriendState{
    FRIEND_STATE,
    REQUEST_STATE,
    ADD_STATE
}