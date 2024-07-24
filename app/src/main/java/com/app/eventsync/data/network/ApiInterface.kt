package com.app.eventsync.data.network

import com.app.eventsync.presentation.main_screens.chats_screen.chat_activity.FcmNotificationPayload
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST

interface ApiInterface {
    @POST("v1/projects/eventsync-fd543/messages:send")
    fun sendMessage(
        @HeaderMap headers: Map<String, String>,
        @Body messageBody: FcmNotificationPayload
    ): Call<NotificationResponse>
}

data class NotificationResponse(
    val name: String
)