package com.app.eventsync.utils

import com.app.eventsync.BuildConfig

object Constants {
    const val NOTIFICATION_PERMISSION_REQUEST_CODE = 101
    const val NOTIFICATION_CHANNEL_ID = "notification_channel"
    const val NOTIFICATION_CHANNEL_1_ID = "channel_1_id"
    const val NOTIFICATION_CHANNEL_NAME = "Event's Notifications"
    const val NOTIFICATION_CHANNEL_DESCRIPTION = "Notification of app events"
    const val NOTIFICATION_TITLE = "notification_title"
    const val NOTIFICATION_DESCRIPTION = "notification_description"
    const val EVENT_ID = "event_id"
    const val IS_EDIT_MODE = "is_edit_mode"
    const val IS_PROFILE = "is_profile"
    const val DEFAULT_PROFILE_IMG = BuildConfig.DEFAULT_PROFILE_URL
    const val USER_ID = "user_id"
    const val IS_IN_BOTTOM_SHEET = "is_in_bottom_sheet"
    const val CAMERA_AND_GALLERY_PERMISSION_CODE = 100
    const val ATTACHMENT_CHOICE_CHOOSE_GALLERY = 101
    const val KEY_COLLECTION_CHAT = "chat"
    const val KEY_SENDER_ID = "senderId"
    const val KEY_RECEIVER_ID = "receiverId"
    const val KEY_MESSAGE = "message"
    const val KEY_TIMESTAMP = "timestamp"
    const val KEY_AVAILABILITY = "availability"
    const val REMOTE_MSG_AUTHORIZATION = "Authorization"
    const val REMOTE_MSG_CONTENT_TYPE = "Content-Type"
    const val REMOTE_MSG_DATA = "data"
    const val REMOTE_MSG_REGISTRATION_IDS = "to"
    const val KEY_FCM_TOKEN = "fcmToken"
    const val IS_USER = "is_user"
    const val SEARCH_TYPE = "search_type"
    const val IS_COLLABORATOR_LIST = "is_collaborator_list"
    const val RECEIVER_USER_PROFILE_PIC = "receiver_user_profile_pic"

    fun getRemoteHeaders(): HashMap<String, String> {
        val remoteMsgHeaders: HashMap<String, String> = HashMap()
        remoteMsgHeaders.put("Content-Type", "application/json")
        remoteMsgHeaders.put(
            "Authorization",
            BuildConfig.FCM_TOKEN)
        return remoteMsgHeaders
    }
}