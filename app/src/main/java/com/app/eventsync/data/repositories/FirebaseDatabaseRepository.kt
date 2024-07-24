package com.app.eventsync.data.repositories

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.app.eventsync.data.models.AlarmItem
import com.app.eventsync.data.models.CommentItem
import com.app.eventsync.data.models.Event
import com.app.eventsync.data.models.EventImage
import com.app.eventsync.data.models.FriendItem
import com.app.eventsync.data.models.User
import com.app.eventsync.utils.AppUtils.getEndOfDay
import com.app.eventsync.utils.AppUtils.getEndOfTomorrow
import com.app.eventsync.utils.AppUtils.getStartOfDay
import com.app.eventsync.utils.AppUtils.getStartOfTomorrow
import com.app.eventsync.utils.Constants
import com.app.eventsync.utils.PreferenceManager
import com.app.eventsync.utils.Resource
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.storage
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

enum class EventListType {
    TODAY, UPCOMING, LATER
}

sealed class DatabaseState<T>(val data: T? = null, val message: String? = null) {
    class Loading<T>() : DatabaseState<T>()
    class Success<T>(data: T?, message: String?) : DatabaseState<T>(data, message)
    class Error<T>(message: String?) : DatabaseState<T>(null, message)
}

class FirebaseDatabaseRepository @Inject constructor(
    private val context: Context
) {

    fun removeImageFromEvent(imageUrl: String, eventId: String): Flow<Resource> = callbackFlow {
        trySend(Resource.Loading())
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.getReferenceFromUrl(imageUrl)
        storageRef.delete()
            .addOnSuccessListener {
                FirebaseFirestore.getInstance().collection("all_events")
                    .document(eventId)
                    .update("imageList", FieldValue.arrayRemove(imageUrl))
                    .addOnSuccessListener {
                        trySend(Resource.Success("Deleted successfully."))
                    }.addOnFailureListener {
                        trySend(Resource.Error("Something went wrong."))
                    }
            }.addOnFailureListener {
                trySend(Resource.Error("Something went wrong."))
            }

        awaitClose{
            cancel()
        }
    }

    fun removeOtherUserImage(eventImage: EventImage): Flow<Resource> = callbackFlow {
        trySend(Resource.Loading())
        val storage = FirebaseStorage.getInstance()
        val storageRef: StorageReference = storage.getReferenceFromUrl(eventImage.imageUrl)
        storageRef.delete()
            .addOnSuccessListener {
                FirebaseFirestore.getInstance().collection("all_events")
                    .document(eventImage.eventId)
                    .collection("otherUsersImageList")
                    .document(eventImage.id)
                    .delete()
                    .addOnSuccessListener {
                        trySend(Resource.Success("Deleted Successfully."))

                    }.addOnFailureListener {
                        trySend(Resource.Error("Something went wrong."))
                    }
            }
            .addOnFailureListener {
                trySend(Resource.Success("Something went wrong!"))
            }

        awaitClose{
            cancel()
        }

    }

    fun getOtherUsersImageList(eventId: String): Flow<DatabaseState<List<EventImage>>> =
        callbackFlow {
            val otherUserImageList = mutableListOf<EventImage>()
            trySend(DatabaseState.Loading())
            FirebaseFirestore.getInstance().collection("all_events")
                .document(eventId)
                .collection("otherUsersImageList")
                .addSnapshotListener { value, error ->

                    if (error != null) {
                        trySend(DatabaseState.Error("Something went wrong."))
                    }

                    if (value != null) {
                        otherUserImageList.clear()
                        for (snapshot in value.documents) {
                            val eventImage = snapshot.toObject(EventImage::class.java)
                            if (eventImage != null) {
                                otherUserImageList.add(eventImage)
                            }
                        }
                        trySend(DatabaseState.Success(otherUserImageList, "Success"))
                    }
                }

            awaitClose {
                cancel()
            }

        }

    fun updateImageInEvent(imageUri: Uri, eventId: String): Flow<DatabaseState<String>> =
        callbackFlow {
            trySend(DatabaseState.Loading())
            val preferenceManager = PreferenceManager(context)
            val userId = preferenceManager.getUserId()
            val storageRef = FirebaseStorage.getInstance().reference
            val imagePath = storageRef.child("users/${userId}/events/${eventId}")

            imagePath.putFile(imageUri).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    imagePath.downloadUrl.addOnSuccessListener { imageUri ->
                        val eventImage = EventImage(eventId, userId!!, imageUri.toString())
                        FirebaseFirestore.getInstance().collection("all_events")
                            .document(eventImage.eventId)
                            .collection("otherUsersImageList")
                            .add(eventImage)
                            .addOnSuccessListener {document->

                                document.update("id", document.id).addOnSuccessListener {
                                    trySend(DatabaseState.Success(eventImage.imageUrl, "Success"))
                                }


                            }.addOnFailureListener {
                                trySend(DatabaseState.Error("Something went wrong."))
                            }
                    }
                } else {
                    trySend(DatabaseState.Error("Something went wrong."))
                }
            }.addOnFailureListener {
                trySend(DatabaseState.Error("Something went wrong."))
            }



            awaitClose {
                cancel()
            }

        }

    fun getRequestIdList(): Flow<DatabaseState<List<String>>> = callbackFlow {
        var requestIdList = mutableListOf<String>()
        val preferenceManager = PreferenceManager(context)
        trySend(DatabaseState.Loading())

        FirebaseFirestore.getInstance().collection("users")
            .document(preferenceManager.getUserId()!!)
            .addSnapshotListener { value, error ->

                if (error != null) {
                    trySend(DatabaseState.Error("Something went wrong."))
                }

                requestIdList = value?.get("friendRequests") as MutableList<String>

                trySend(DatabaseState.Success(requestIdList, "Success"))

            }
        awaitClose {
            cancel()
        }

    }

    fun getFriendIdList(): Flow<DatabaseState<List<String>>> = callbackFlow {
        var friendIdList = mutableListOf<String>()
        val preferenceManager = PreferenceManager(context)
        trySend(DatabaseState.Loading())

        FirebaseFirestore.getInstance().collection("users")
            .document(preferenceManager.getUserId()!!)
            .addSnapshotListener { value, error ->

                if (error != null) {
                    trySend(DatabaseState.Error("Something went wrong."))
                }

                friendIdList = value?.get("friendList") as MutableList<String>

                trySend(DatabaseState.Success(friendIdList, "Success"))

            }
        awaitClose {
            cancel()
        }

    }

    fun getCommentList(eventId: String): Flow<DatabaseState<List<CommentItem>>> = callbackFlow {
        trySend(DatabaseState.Loading())
        val commentList = mutableListOf<CommentItem>()

        FirebaseFirestore.getInstance().collection("all_events")
            .document(eventId)
            .collection("comments")
            .orderBy("timeStamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    trySend(DatabaseState.Error("Something went wrong."))
                    return@addSnapshotListener
                }

                if (value != null) {
                    commentList.clear()
                    for (snapshot in value.documents) {
                        val commentItem = snapshot.toObject(CommentItem::class.java)
                        if (commentItem != null) {
                            commentList.add(commentItem)
                        }
                    }
                    trySend(DatabaseState.Success(commentList, "Success"))

                }
            }

        awaitClose {
            cancel()
        }
    }

    fun postCommentOnEvent(commentItem: CommentItem): Flow<Resource> = callbackFlow {
        trySend(Resource.Loading()).isSuccess
        val database = FirebaseFirestore.getInstance()
        database.collection("all_events")
            .document(commentItem.eventId)
            .collection("comments")
            .add(commentItem).addOnSuccessListener { document ->
                document.update("commentId", document.id).addOnCompleteListener { task ->

                    if (task.isSuccessful) {
                        trySend(Resource.Success("Success")).isSuccess
                    }

                }


            }.addOnFailureListener {
                trySend(Resource.Error("Something went wrong")).isSuccess
            }
        awaitClose {
            cancel()
        }
    }

    fun uploadProfilePhoto(imageUri: Uri, userId: String): Flow<DatabaseState<String>> =
        callbackFlow {

            trySend(DatabaseState.Loading())

            var storageRef = Firebase.storage.reference
            val userProfilePhotoRef = storageRef.child("users/${userId}/profilePhoto")
            val uploadTask = userProfilePhotoRef.putFile(imageUri)
            uploadTask.addOnCompleteListener {
                if (it.isSuccessful) {
                    userProfilePhotoRef.downloadUrl.addOnSuccessListener { uri ->

                        trySend(DatabaseState.Success(uri.toString(), "Upload Success"))

                    }

                } else {
                    trySend(DatabaseState.Error("Upload Failed"))
                }
            }

            awaitClose {
                cancel()
            }

        }

    fun updateProfileInfo(userId: String, name: String, profilePhoto: String): Flow<Resource> =
        callbackFlow {

            trySend(Resource.Loading())

            FirebaseFirestore.getInstance().collection("users")
                .document(userId)
                .update(
                    "name", name,
                    "profilePhoto", profilePhoto
                ).addOnSuccessListener {

                    trySend(Resource.Success("Success"))

                }.addOnFailureListener {
                    trySend(Resource.Error("Something went wrong."))
                }
            awaitClose {
                cancel()
            }
        }

    fun getUserDetails(userId: String): Flow<DatabaseState<User>> = callbackFlow {
        trySend(DatabaseState.Loading())
        FirebaseFirestore.getInstance().collection("users")
            .document(userId)
            .addSnapshotListener { value, error ->

                if (error != null) {
                    trySend(DatabaseState.Error("Something went wrong."))
                }

                val user = value?.toObject(User::class.java)
                trySend(DatabaseState.Success(user, "User data fetched successfully."))

            }
        awaitClose {
            cancel()
        }
    }

    fun sendFriendRequest(requestId: String, userId: String): Flow<Resource> = callbackFlow {
        trySend(Resource.Loading())
        FirebaseFirestore.getInstance().collection("users")
            .document(requestId)
            .update("friendRequests", FieldValue.arrayUnion(userId))
            .addOnSuccessListener {
                trySend(Resource.Success("Success"))
            }.addOnFailureListener {
                trySend(Resource.Error("Something went wrong..."))
            }
        awaitClose {
            cancel()
        }
    }

    fun findFriend(friendId: String): Flow<DatabaseState<FriendItem>> = callbackFlow {
        trySend(DatabaseState.Loading())

        FirebaseFirestore.getInstance().collection("users")
            .document(friendId)
            .addSnapshotListener { value, error ->

                if (error != null) {
                    trySend(DatabaseState.Error("Something went wrong..."))
                }

                if (value != null) {
                    val name = value.getString("name")
                    val profilePhoto = value.getString("profilePhoto")

                    if (name != null && profilePhoto != null) {
                        val friendItem = FriendItem(friendId, name!!, profilePhoto!!)
                        trySend(DatabaseState.Success(friendItem, "Success"))
                    } else {
                        trySend(DatabaseState.Error("No user found."))
                    }

                } else {
                    trySend(DatabaseState.Error("No user found."))
                }

            }
        awaitClose {
            cancel()
        }
    }

    fun removeFriend(userId: String, friendId: String): Flow<Resource> = callbackFlow {
        trySend(Resource.Loading())

        val database = FirebaseFirestore.getInstance()
        database.collection("users")
            .document(userId)
            .update(
                "friendList", FieldValue.arrayRemove(friendId)
            )
            .addOnSuccessListener {

                database.collection("users")
                    .document(friendId)
                    .update("friendList", FieldValue.arrayRemove(userId))
                    .addOnSuccessListener {
                        trySend(Resource.Success("Success"))
                    }

            }.addOnFailureListener {
                trySend(Resource.Error("Something went wrong..."))
            }
        awaitClose {
            cancel()
        }
    }


    fun acceptFriendRequest(userId: String, requestId: String): Flow<Resource> = callbackFlow {
        trySend(Resource.Loading())

        val database = FirebaseFirestore.getInstance()
        database.collection("users")
            .document(userId)
            .update(
                "friendRequests", FieldValue.arrayRemove(requestId),
                "friendList", FieldValue.arrayUnion(requestId)
            )
            .addOnCompleteListener {
                if (it.isSuccessful) {

                    database.collection("users")
                        .document(requestId)
                        .update("friendList", FieldValue.arrayUnion(userId))
                        .addOnSuccessListener {
                            trySend(Resource.Success("Success"))
                        }

                } else {
                    trySend(Resource.Error("Something went wrong..."))
                }

            }.addOnFailureListener {
                trySend(Resource.Error("Something went wrong..."))
            }

        awaitClose {
            cancel()
        }
    }

    fun getChatFriendList(userId: String): Flow<DatabaseState<List<User>>> = callbackFlow {
        try {
            send(DatabaseState.Loading())

            val database = FirebaseFirestore.getInstance()
            val friendItemList = mutableListOf<User>()
            val friendIdList = database.collection("users").document(userId).get().await()
                .get("friendList") as? List<String> ?: emptyList()

            Log.d("REQUESTLIST", "Request IDs: $friendIdList")

            friendIdList.map { id ->
                val documentSnapshot =
                    database.collection("users").document(id.trim()).get().await()

                if (documentSnapshot.exists()) {
                    val user = documentSnapshot.toObject(User::class.java)
                    if (user != null) {
                        friendItemList.add(user)
                    } else {
                        send(DatabaseState.Error("User not found..."))
                    }
                } else {
                    Log.e("REQUESTLIST", "Document does not exist for ID: $id")
                }
            }

            send(DatabaseState.Success(friendItemList, "Success"))

        } catch (e: Exception) {
            Log.e("REQUESTLIST", "Error fetching friend requests", e)
            send(DatabaseState.Error(e.message ?: "Unknown error"))
        } finally {
            close()
        }
    }

    fun getCollaboratorList(eventId: String): Flow<DatabaseState<List<FriendItem>>> = callbackFlow {
        try {
            send(DatabaseState.Loading())

            val database = FirebaseFirestore.getInstance()
            val friendItemList = mutableListOf<FriendItem>()
            val collaboratorList = database.collection("all_events").document(eventId).get().await()
                .get("participantsList") as? List<String> ?: emptyList()


            collaboratorList.map { id ->
                val documentSnapshot =
                    database.collection("users").document(id.trim()).get().await()

                if (documentSnapshot.exists()) {
                    val id = documentSnapshot.getString("id") ?: ""
                    val name = documentSnapshot.getString("name") ?: ""
                    val profilePhoto =
                        documentSnapshot.getString("profilePhoto") ?: Constants.DEFAULT_PROFILE_IMG
                    val friendItem = FriendItem(id, name, profilePhoto)
                    Log.d("REQUESTLIST", "FriendItem: $friendItem")
                    friendItemList.add(friendItem)
                } else {
                    Log.e("REQUESTLIST", "Document does not exist for ID: $id")
                }
            }

            send(DatabaseState.Success(friendItemList, "Success"))

        } catch (e: Exception) {
            Log.e("REQUESTLIST", "Error fetching friend requests", e)
            send(DatabaseState.Error(e.message ?: "Unknown error"))
        } finally {
            close()
        }
    }


    fun getUsersList(): Flow<DatabaseState<List<User>>> = callbackFlow {
        val userList = mutableListOf<User>()
        trySend(DatabaseState.Loading())
        val database = FirebaseFirestore.getInstance()
        database.collection("users")
            .addSnapshotListener { value, error ->
                if (error != null) {
                    trySend(DatabaseState.Error("Something went wrong"))
                }

                if (value != null) {
                    userList.clear()
                    for (snapshot in value.documents) {
                        val user = snapshot.toObject(User::class.java)
                        if (user != null) {
                            userList.add(user)
                        }
                    }
                }
                trySend(DatabaseState.Success(userList, "Success"))

            }
        awaitClose {
            cancel()
        }
    }

    fun getFriendList(userId: String): Flow<DatabaseState<List<FriendItem>>> = callbackFlow {
        try {
            send(DatabaseState.Loading())

            val database = FirebaseFirestore.getInstance()
            val friendItemList = mutableListOf<FriendItem>()
            val friendIdList = database.collection("users").document(userId).get().await()
                .get("friendList") as? List<String> ?: emptyList()

            Log.d("REQUESTLIST", "Request IDs: $friendIdList")

            friendIdList.map { id ->
                val documentSnapshot =
                    database.collection("users").document(id.trim()).get().await()

                if (documentSnapshot.exists()) {
                    val id = documentSnapshot.getString("id") ?: ""
                    val name = documentSnapshot.getString("name") ?: ""
                    val profilePhoto =
                        documentSnapshot.getString("profilePhoto") ?: Constants.DEFAULT_PROFILE_IMG
                    val friendList = documentSnapshot.get("friendList") as ArrayList<String>
                    val requestList = documentSnapshot.get("friendRequests") as ArrayList<String>
                    val friendItem = FriendItem(id, name, profilePhoto, requestList, friendList)
                    Log.d("REQUESTLIST", "FriendItem: $friendItem")
                    friendItemList.add(friendItem)
                } else {
                    Log.e("REQUESTLIST", "Document does not exist for ID: $id")
                }
            }

            send(DatabaseState.Success(friendItemList, "Success"))

        } catch (e: Exception) {
            Log.e("REQUESTLIST", "Error fetching friend requests", e)
            send(DatabaseState.Error(e.message ?: "Unknown error"))
        } finally {
            close()
        }
    }

    suspend fun getRequestList(userId: String): Flow<DatabaseState<List<FriendItem>>> =
        callbackFlow {
            try {
                send(DatabaseState.Loading())

                val database = FirebaseFirestore.getInstance()
                val friendItemList = mutableListOf<FriendItem>()
                val requestIdList = database.collection("users").document(userId).get().await()
                    .get("friendRequests") as? List<String> ?: emptyList()

                Log.d("REQUESTLIST", "Request IDs: $requestIdList")

                requestIdList.map { requestId ->
                    val documentSnapshot =
                        database.collection("users").document(requestId.trim()).get().await()

                    if (documentSnapshot.exists()) {
                        val id = documentSnapshot.getString("id") ?: ""
                        val name = documentSnapshot.getString("name") ?: ""
                        val profilePhoto = documentSnapshot.getString("profilePhoto")
                            ?: Constants.DEFAULT_PROFILE_IMG
                        val friendItem = FriendItem(id, name, profilePhoto)
                        Log.d("REQUESTLIST", "FriendItem: $friendItem")
                        friendItemList.add(friendItem)
                    } else {
                        Log.e("REQUESTLIST", "Document does not exist for ID: $requestId")
                    }
                }

                send(DatabaseState.Success(friendItemList, "Success"))

            } catch (e: Exception) {
                Log.e("REQUESTLIST", "Error fetching friend requests", e)
                send(DatabaseState.Error(e.message ?: "Unknown error"))
            } finally {
                close()
            }
        }


    fun deleteEvent(eventId: String): Flow<Resource> = callbackFlow {
        trySend(Resource.Loading())
        val database = FirebaseFirestore.getInstance()
        database.collection("all_events")
            .document(eventId)
            .delete().addOnSuccessListener {
                database.collection("users")
                    .document(PreferenceManager(context).getUserId()!!)
                    .update("createdEvents", FieldValue.arrayRemove(eventId))
                    .addOnSuccessListener {

                        trySend(Resource.Success("Event Deleted Successfully."))

                    }
            }.addOnFailureListener {
                trySend(Resource.Error("Something went wrong."))
            }
        awaitClose {
            cancel()
        }
    }

    fun updateEvent(event: Event): Flow<Resource> = callbackFlow {

        trySend(Resource.Loading())

        FirebaseFirestore.getInstance().collection("all_events")
            .document(event.id)
            .update(
                "title", event.title,
                "description", event.description,
                "startTime", event.startTime,
                "endTime", event.endTime,
                "repeated", event.repeated,
                "requirements", event.requirements,
                "location", event.location,
                "organizationType", event.organizationType,
                "availableSeats", event.availableSeats,
                "commentsAllowed", event.commentsAllowed,
                "paymentRequirements", event.paymentRequirements,
                "privateMode", event.privateMode,
                "imageList", event.imageList,
                "participantsList", event.participantsList
            ).addOnSuccessListener {

                trySend(Resource.Success("Successfully updated...")).isSuccess
            }.addOnFailureListener {
                trySend(Resource.Error("Something went wrong..."))
            }

        awaitClose {
            cancel()
        }

    }

    fun getEventDetails(eventId: String): Flow<DatabaseState<Event>> = callbackFlow {

        trySend(DatabaseState.Loading())

        FirebaseFirestore.getInstance().collection("all_events")
            .document(eventId)
            .addSnapshotListener { value, error ->

                if (error != null) {
                    trySend(DatabaseState.Error("Something went wrong."))
                    return@addSnapshotListener
                }

                val eventItem = value?.toObject(Event::class.java)

                eventItem?.let {
                    trySend(DatabaseState.Success(it, "Success"))
                }

            }

        awaitClose {
            cancel()
        }


    }

    fun getUserCreatedEventList(userId: String): Flow<DatabaseState<List<Event>>> = callbackFlow {
        trySend(DatabaseState.Loading())

        val database = FirebaseFirestore.getInstance()
        val eventList = mutableListOf<Event>()

        try {
            val userDocumentRef = database.collection("users").document(userId).get().await()
            val user = userDocumentRef.toObject(User::class.java)

            if (user != null) {
                eventList.clear()

                user.createdEvents.map { eventId ->
                    val eventSnapshot =
                        database.collection("all_events").document(eventId).get().await()
                    val eventItem = eventSnapshot.toObject(Event::class.java)
                    if (eventItem != null) {
                        eventList.add(eventItem)
                    }
                }

                trySend(DatabaseState.Success(eventList, "Success"))

            } else {
                trySend(DatabaseState.Error("User not found"))
            }
        } catch (e: Exception) {
            trySend(DatabaseState.Error("Something went wrong."))
        }
        awaitClose {
            cancel()
        }

    }

    suspend fun getEnrolledEventsList(userId: String): Flow<DatabaseState<List<Event>>> =
        callbackFlow {

            trySend(DatabaseState.Loading())

            val database = FirebaseFirestore.getInstance()
            val eventList = mutableListOf<Event>()

            try {
                val userDocumentRef = database.collection("users").document(userId).get().await()
                val user = userDocumentRef.toObject(User::class.java)

                if (user != null) {
                    eventList.clear()

                    user.enrolledEvents.map { eventId ->
                        val eventSnapshot =
                            database.collection("all_events").document(eventId).get().await()
                        val eventItem = eventSnapshot.toObject(Event::class.java)
                        if (eventItem != null) {
                            eventList.add(eventItem)
                        }
                    }

                    trySend(DatabaseState.Success(eventList, "Success"))

                } else {
                    trySend(DatabaseState.Error("User not found"))
                }
            } catch (e: Exception) {
                trySend(DatabaseState.Error("Something went wrong."))
            }

            awaitClose {
                cancel()
            }
        }

    fun cancelEvent(
        userId: String,
        eventId: String,
        eventTime: Date,
        eventInfo: String
    ): Flow<DatabaseState<AlarmItem>> = callbackFlow {


        trySend(DatabaseState.Loading())
        val alarmItem =
            AlarmItem(
                Constants.NOTIFICATION_CHANNEL_ID,
                Constants.NOTIFICATION_CHANNEL_NAME,
                Constants.NOTIFICATION_CHANNEL_DESCRIPTION,
                eventTime,
                "Its time for your event",
                eventInfo

            )
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .update("enrolledEvents", FieldValue.arrayRemove(eventId))
            .addOnSuccessListener { task ->
                FirebaseFirestore.getInstance().collection("all_events")
                    .document(eventId)
                    .update(
                        "participantsList",
                        FieldValue.arrayRemove(userId)
                    )
                    .addOnSuccessListener {

                        trySend(
                            DatabaseState.Success(
                                alarmItem,
                                "Added to database successfully..."
                            )
                        ).isSuccess

                    }
            }.addOnFailureListener {
                trySend(DatabaseState.Error(it.localizedMessage.toString())).isSuccess
            }.addOnCanceledListener {
                trySend(DatabaseState.Error("Process cancelled...")).isSuccess
            }
        awaitClose {
            channel.close()
        }


    }


    fun saveEventToUserDatabase(
        userId: String,
        eventId: String,
        eventTime: Date,
        eventInfo: String
    ): Flow<DatabaseState<AlarmItem>> = callbackFlow {
        trySend(DatabaseState.Loading())
        val alarmItem =
            AlarmItem(
                Constants.NOTIFICATION_CHANNEL_ID,
                Constants.NOTIFICATION_CHANNEL_NAME,
                Constants.NOTIFICATION_CHANNEL_DESCRIPTION,
                eventTime,
                "Its time for your event",
                eventInfo

            )
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .update("enrolledEvents", FieldValue.arrayUnion(eventId))
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    FirebaseFirestore.getInstance().collection("all_events")
                        .document(eventId)
                        .update(
                            "participantsList",
                            FieldValue.arrayUnion(userId)
                        )
                        .addOnCompleteListener {

                            trySend(
                                DatabaseState.Success(
                                    alarmItem,
                                    "Added to database successfully..."
                                )
                            ).isSuccess

                        }
                } else {
                    trySend(DatabaseState.Error(task.exception?.localizedMessage.toString())).isSuccess
                }

            }.addOnFailureListener {
                trySend(DatabaseState.Error(it.localizedMessage.toString())).isSuccess
            }.addOnCanceledListener {
                trySend(DatabaseState.Error("Process cancelled...")).isSuccess
            }
        awaitClose {
            channel.close()
        }
    }

    fun saveUserToTheDatabase(user: User): Flow<Resource> = callbackFlow {
        trySend(Resource.Loading())
        FirebaseFirestore.getInstance().collection("users").document(user.id).set(user)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    trySend(Resource.Success("Added to database successfully..."))
                } else {
                    trySend(Resource.Error(task.exception?.localizedMessage.toString()))
                }

            }.addOnFailureListener {
                trySend(Resource.Error(it.localizedMessage.toString()))
            }.addOnCanceledListener {
                trySend(Resource.Error("Process cancelled..."))
            }
        awaitClose {
            channel.close()
        }
    }

    fun saveEventToTheDatabase(event: Event): Flow<Resource> = callbackFlow {
        trySend(Resource.Loading()).isSuccess
        val database = FirebaseFirestore.getInstance()
        database.collection("all_events").add(event)
            .addOnSuccessListener { document ->
                document.update("id", document.id).addOnCompleteListener { task ->
                    if (task.isSuccessful) {

                        database.collection("users")
                            .document(PreferenceManager(context).getUserId()!!)
                            .update("createdEvents", FieldValue.arrayUnion(document.id))
                            .addOnCompleteListener { task2 ->

                                if (task2.isSuccessful) {
                                    Log.d("SAVEEVENT", "success")
                                    trySend(Resource.Success("Event Added Successfully.")).isSuccess
                                } else {
                                    trySend(Resource.Error("Something went wrong.")).isSuccess
                                }
                            }

                    }

                }
            }.addOnFailureListener {
                trySend(Resource.Error("Something went wrong.")).isSuccess
            }
        awaitClose {
            channel.close()
        }
    }

    fun getEventListFromDate(
        year: Int,
        month: Int,
        day: Int
    ): Flow<DatabaseState<List<Event>>> = callbackFlow {
        val eventList = mutableListOf<Event>()
        trySend(DatabaseState.Loading())
        val start = Calendar.getInstance()
        start.set(Calendar.YEAR, year)
        start.set(Calendar.MONTH, month)
        start.set(Calendar.DAY_OF_MONTH, day)
        start[Calendar.HOUR_OF_DAY] = 0
        start[Calendar.MINUTE] = 0
        start[Calendar.SECOND] = 0
        start[Calendar.MILLISECOND] = 0

        val end = Calendar.getInstance()
        end.set(Calendar.YEAR, year)
        end.set(Calendar.MONTH, month)
        end.set(Calendar.DAY_OF_MONTH, day)
        end[Calendar.HOUR_OF_DAY] = 23
        end[Calendar.MINUTE] = 59
        end[Calendar.SECOND] = 59
        end[Calendar.MILLISECOND] = 999

        Log.d("EVENTDATA", "start${start.time.toString()}")

        Log.d("EVENTDATA", "end${end.time.toString()}")



        FirebaseFirestore.getInstance().collection("all_events")
            .orderBy("startTime")
            .where(Filter.greaterThan("startTime", start.time))
            .where(Filter.lessThan("startTime", end.time))
            .addSnapshotListener { value, error ->

                if (error != null) {
                    trySend(DatabaseState.Error("Something went wrong."))
                }

                if (value != null) {
                    for (snapshot in value.documents) {
                        val eventItem = snapshot.toObject(Event::class.java)
                        eventList.add(eventItem!!)
                    }
                    Log.d("EVENTSDATA", eventList.toString())
                    trySend(DatabaseState.Success(eventList, "Success"))
                }

            }
        awaitClose {
            cancel()
        }
    }

    fun getEventListFromDatabase(eventListType: EventListType = EventListType.TODAY): LiveData<DatabaseState<List<Event>>> {
        val dataList = MutableLiveData<DatabaseState<List<Event>>>()
        dataList.postValue(DatabaseState.Loading())
        when (eventListType) {
            EventListType.TODAY -> {

                val startOfDay = getStartOfDay()
                val endOfDay = getEndOfDay()

                Log.d("EVENTDATA", "start${startOfDay.toString()}")

                Log.d("EVENTDATA", "end${endOfDay.toString()}")

                FirebaseFirestore.getInstance().collection("all_events").orderBy("startTime")
                    .where(Filter.greaterThan("startTime", startOfDay))
                    .where(Filter.lessThan("startTime", endOfDay))
                    .addSnapshotListener { value, error ->

                        val eventList = mutableListOf<Event>()

                        if (error != null) {
                            dataList.postValue(DatabaseState.Error("Something went wrong."))
                            return@addSnapshotListener
                        }

                        if (value != null) {
                            for (snapshot in value.documents) {
                                val eventItem = snapshot.toObject(Event::class.java)
                                eventList.add(eventItem!!)
                            }
                            dataList.postValue(DatabaseState.Success(eventList, "Success"))
                        }


                    }

            }

            EventListType.UPCOMING -> {

                val startOfTomorrow = getStartOfTomorrow()
                val endOfTomorrow = getEndOfTomorrow()
                FirebaseFirestore.getInstance().collection("all_events").orderBy("startTime")
                    .where(Filter.greaterThan("startTime", startOfTomorrow))
                    .where(Filter.lessThan("startTime", endOfTomorrow))
                    .addSnapshotListener { value, error ->

                        val eventList = mutableListOf<Event>()

                        if (error != null) {
                            dataList.postValue(DatabaseState.Error("Something went wrong."))
                            return@addSnapshotListener
                        }

                        if (value != null) {
                            for (snapshot in value.documents) {
                                val eventItem = snapshot.toObject(Event::class.java)
                                eventList.add(eventItem!!)
                            }
                            dataList.postValue(DatabaseState.Success(eventList, "Success"))
                        }


                    }
            }

            EventListType.LATER -> {
                val endOfTomorrow = getEndOfTomorrow()
                FirebaseFirestore.getInstance().collection("all_events").orderBy("startTime")
                    .where(Filter.greaterThan("startTime", endOfTomorrow))
                    .addSnapshotListener { value, error ->

                        val eventList = mutableListOf<Event>()

                        if (error != null) {
                            dataList.postValue(DatabaseState.Error("Something went wrong."))
                            return@addSnapshotListener
                        }

                        if (value != null) {
                            for (snapshot in value.documents) {
                                val eventItem = snapshot.toObject(Event::class.java)
                                eventList.add(eventItem!!)
                            }
                            dataList.postValue(DatabaseState.Success(eventList, "Success"))
                        }
                    }

            }
        }
        return dataList

    }


}
