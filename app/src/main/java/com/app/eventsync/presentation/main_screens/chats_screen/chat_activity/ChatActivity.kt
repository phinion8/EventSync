package com.app.eventsync.presentation.main_screens.chats_screen.chat_activity

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.eventsync.R
import com.app.eventsync.data.models.ChatMessage
import com.app.eventsync.data.models.User
import com.app.eventsync.data.network.ApiInterface
import com.app.eventsync.data.network.NotificationResponse
import com.app.eventsync.data.repositories.DatabaseState
import com.app.eventsync.databinding.ActivityChatBinding
import com.app.eventsync.presentation.main_screens.chats_screen.adapters.ChatAdapter
import com.app.eventsync.presentation.profile_page_screen.viewModel.UserProfileViewModel
import com.app.eventsync.utils.Constants
import com.app.eventsync.utils.LoadingDialog
import com.app.eventsync.utils.showToast
import com.bumptech.glide.Glide
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.gson.annotations.SerializedName
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Callback
import java.io.InputStream
import java.util.Calendar
import java.util.Date
import javax.inject.Inject


@AndroidEntryPoint
class ChatActivity : BaseActivity() {
    private lateinit var binding: ActivityChatBinding
    private val userProfileViewModel: UserProfileViewModel by viewModels()
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var receiverUser: User
    private lateinit var chatMessages: ArrayList<ChatMessage>
    private var availabilty: Boolean = false
    private lateinit var database: FirebaseFirestore
    private lateinit var chatAdapter: ChatAdapter
    private var lastChatMessage = ""

    @Inject
    lateinit var apiInterface: ApiInterface

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loadingDialog = LoadingDialog(this)
        database = FirebaseFirestore.getInstance()
        chatMessages = ArrayList()

        val receiverUserId = intent.getStringExtra(Constants.USER_ID)

        val receiverUserProfilePic = intent.getStringExtra(Constants.RECEIVER_USER_PROFILE_PIC) ?: Constants.DEFAULT_PROFILE_IMG

        if (receiverUserId != null) {
            userProfileViewModel.getUserDetails(receiverUserId)
            database.collection("users")
                .document(receiverUserId)
                .addSnapshotListener { value, error ->


                    when (value?.getLong(Constants.KEY_AVAILABILITY)?.toInt()) {
                        1 -> {

                            binding.tvStatus.text = "Online"
                            binding.tvStatus.setTextColor(getColor(R.color.green))
                            availabilty = true

                        }

                        0 -> {
                            availabilty = false
                            binding.tvStatus.text = "Offline"
                            binding.tvStatus.setTextColor(getColor(R.color.content_grey_color))
                        }

                        null -> {
                            binding.tvStatus.text = "Offline"
                            binding.tvStatus.setTextColor(getColor(R.color.content_grey_color))
                        }
                    }


                }
        }

        chatAdapter = ChatAdapter(chatMessages, preferenceManager.getUserId()!!, receiverUserProfilePic)

        if (receiverUserId != null) {
            listenMessages(receiverUserId)
        }

        userProfileViewModel.userDetailsState.observe(this) { value ->

            when (value) {
                is DatabaseState.Loading -> {
                    loadingDialog.show()
                }

                is DatabaseState.Success -> {
                    Glide.with(this)
                        .load(value.data?.profilePhoto)
                        .error(Constants.DEFAULT_PROFILE_IMG)
                        .into(binding.profileImage)

                    binding.chatUserName.text = value.data?.name
                    receiverUser = value.data!!
                    loadingDialog.dismiss()


                }

                is DatabaseState.Error -> {
                    loadingDialog.dismiss()
                    showToast(value.message!!)
                }
            }

        }

        binding.rvChat.adapter = chatAdapter

        binding.rvChat.layoutManager = LinearLayoutManager(this)

        binding.sendMessageBtn.setOnClickListener {
            if (receiverUserId != null) {
                sendMessage(receiverUserId)
            }

        }

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

    }

    private fun listenMessages(receiverId: String) {
        database.collection(Constants.KEY_COLLECTION_CHAT)
            .whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getUserId()!!)
            .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverId)
            .addSnapshotListener(eventListener)
        database.collection(Constants.KEY_COLLECTION_CHAT)
            .whereEqualTo(Constants.KEY_SENDER_ID, receiverId)
            .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getUserId()!!)
            .addSnapshotListener(eventListener)
    }

    private val eventListener: EventListener<QuerySnapshot> =
        EventListener<QuerySnapshot> { value, error ->
            if (error != null) {
                showToast("Something went wrong.")
                return@EventListener
            }

            if (value != null) {

                val count = chatMessages.size
                for (documentChanges in value.documentChanges) {

                    if (documentChanges.type == DocumentChange.Type.ADDED) {
                        val chatMessage = ChatMessage()
                        chatMessage.senderId =
                            documentChanges.document.getString(Constants.KEY_SENDER_ID).toString()
                        chatMessage.receiverId =
                            documentChanges.document.getString(Constants.KEY_RECEIVER_ID).toString()
                        chatMessage.message =
                            documentChanges.document.getString(Constants.KEY_MESSAGE).toString()
                        chatMessage.dateTime =
                            documentChanges.document.getDate(Constants.KEY_TIMESTAMP)!!
                        chatMessages.add(chatMessage)

                    }
                }

                chatMessages.sortBy { it.dateTime }

                if (count == 0) {
                    chatAdapter.notifyDataSetChanged()
                } else {
                    Log.d("CHATADAPTER", chatAdapter.itemCount.toString())
                    chatAdapter.notifyItemRangeInserted(chatMessages.size, chatMessages.size)
                    binding.rvChat.smoothScrollToPosition(chatMessages.size - 1)
                }
            }

        }

    private fun sendMessage(receiverId: String) {
        val message: HashMap<String, Any> = HashMap<String, Any>()
        message.put(Constants.KEY_SENDER_ID, preferenceManager.getUserId()!!)
        message.put(Constants.KEY_RECEIVER_ID, receiverId)
        message.put(Constants.KEY_MESSAGE, binding.etSendMessage.text.toString())
        message.put(Constants.KEY_TIMESTAMP, Date())
        lastChatMessage = binding.etSendMessage.text.toString()
        database.collection(Constants.KEY_COLLECTION_CHAT)
            .add(message)
        updateLastSeen()
        if (!availabilty) {
            try {

                val fcmNotification = FcmNotification("hi", "hello")

                val messagePayload = FcmMessage(receiverUser.fcmToken, fcmNotification)

                val fcmNotificationPayload = FcmNotificationPayload(messagePayload)

                val messageObj = JSONObject()
                val notificationBodyObj = JSONObject()

                notificationBodyObj.put("body", binding.etSendMessage.text.toString())
                notificationBodyObj.put("title", preferenceManager.getUserName())

                messageObj.put("token", receiverUser.fcmToken)
                messageObj.put("notification", notificationBodyObj)

                val finalBody = JSONObject()
                finalBody.put("message", messageObj)


                Log.d("NOTIFICATIONISSUE", finalBody.toString())


                val accessToken = getAccessToken()

                fun getRemoteHeaders(): HashMap<String, String> {
                    val remoteMsgHeaders: HashMap<String, String> = HashMap()
                    remoteMsgHeaders.put("Content-Type", "application/json")
                    remoteMsgHeaders.put(
                        "Authorization",
                        "Bearer $accessToken"
                    )
                    return remoteMsgHeaders

                }


                Log.d("NOTIFICATIONISSUE", "accessToken: $accessToken")

                sendNotification(getRemoteHeaders(), fcmNotificationPayload)


            } catch (e: Exception) {

            }
        }
        binding.etSendMessage.setText(null)
    }

    private fun getAccessToken(): String? {

        try {
            val assetManager = this.assets
            val serviceAccountStream: InputStream = assetManager.open("service-account.json")
            val googleCredentials: GoogleCredentials =
                GoogleCredentials.fromStream(serviceAccountStream).createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))
            googleCredentials.refresh()
            return googleCredentials.accessToken.tokenValue
        } catch (e: Exception) {
            Log.d("NOTIFICATIONISSUE", "Error getting access token: ${e.localizedMessage}")
            return null
        }
    }

    private fun sendNotification(headers: Map<String, String>,fcmNotificationPayload: FcmNotificationPayload) {
        // Assuming apiInterface is an instance of your Retrofit service interface

        apiInterface.sendMessage(headers, fcmNotificationPayload)
            .enqueue(object : Callback<NotificationResponse> {
                override fun onResponse(
                    call: retrofit2.Call<NotificationResponse>,
                    response: retrofit2.Response<NotificationResponse>
                ) {

                    if (response.isSuccessful) {
                        try {
//                            val responseJson = response.body()?.let { JSONObject(it) }
//                            val results = responseJson?.getJSONArray("results")
//                            if (responseJson?.getInt("failure") == 1) {
//                                val error = results?.get(0) as JSONObject
//                                showToast(error.getString("error"))
//                                return
//                            }

                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                        showToast("Notification sent successfully")
                    } else {
                        showToast("Error: ${response.code()}")
                    }

                }

                override fun onFailure(call: retrofit2.Call<NotificationResponse>, t: Throwable) {
                    Log.d("NOTIFICATIONISSUE", t.message.toString())
                    showToast(t.message!!)
                }

            })


    }

    private fun updateLastSeen(){
        val calendar = Calendar.getInstance()
        FirebaseFirestore.getInstance().collection("users").document(preferenceManager.getUserId()!!)
            .update("lastSeen", calendar.time, "lastChatMessage", lastChatMessage)
    }

    override fun onPause() {
        super.onPause()
        updateLastSeen()
    }

    override fun onStop() {
        super.onStop()
        updateLastSeen()
    }


}

data class FcmNotificationPayload(
    @SerializedName("message") val message: FcmMessage
)

data class FcmMessage(
    @SerializedName("token") val token: String,
    @SerializedName("notification") val notification: FcmNotification
)

data class FcmNotification(
    @SerializedName("body") val body: String,
    @SerializedName("title") val title: String
)




