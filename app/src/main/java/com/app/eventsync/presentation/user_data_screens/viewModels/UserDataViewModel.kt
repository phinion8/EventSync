package com.app.eventsync.presentation.user_data_screens.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.eventsync.data.models.AlarmItem
import com.app.eventsync.data.models.Event
import com.app.eventsync.data.models.FriendItem
import com.app.eventsync.data.models.User
import com.app.eventsync.data.repositories.DatabaseState
import com.app.eventsync.data.repositories.FirebaseDatabaseRepository
import com.app.eventsync.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class UserDataViewModel @Inject constructor(
    private val repository: FirebaseDatabaseRepository
) : ViewModel() {
    private var _enrolledList: MutableLiveData<DatabaseState<List<Event>>> = MutableLiveData()
    val enrolledList: LiveData<DatabaseState<List<Event>>> = _enrolledList

    private var _userListState: MutableLiveData<DatabaseState<List<User>>> = MutableLiveData()
    val userListState: LiveData<DatabaseState<List<User>>> = _userListState

    private var _createdEventList: MutableLiveData<DatabaseState<List<Event>>> = MutableLiveData()
    val createdEventList: LiveData<DatabaseState<List<Event>>> = _createdEventList

    private val cancelEventState: MutableStateFlow<DatabaseState<AlarmItem>> =
        MutableStateFlow(DatabaseState.Loading())

    val deleteEventTriggerState: MutableLiveData<Boolean> = MutableLiveData(false)
    private val _findFriendState: MutableLiveData<DatabaseState<FriendItem>> = MutableLiveData()
    val findFriendState: LiveData<DatabaseState<FriendItem>> = _findFriendState

    private val _sendFriendRequestState: MutableLiveData<Resource> = MutableLiveData()
    val sendFriendState: LiveData<Resource> = _sendFriendRequestState

    private val _removeFriendState: MutableLiveData<Resource> = MutableLiveData()
    val removeFriendState: LiveData<Resource> = _removeFriendState

    fun getUserList(){
        viewModelScope.launch {
            repository.getUsersList().collect{
                _userListState.value = it
            }
        }
    }

    fun sendFriendRequest(requestId: String, userId: String){
        viewModelScope.launch {
            repository.sendFriendRequest(requestId, userId).collect{
                _sendFriendRequestState.value = it
            }
        }
    }



    fun removeFriend(userId: String, requestId: String){
        viewModelScope.launch {
            repository.removeFriend(userId, requestId).collect{
                _removeFriendState.value = it
            }
        }
    }




    fun findFriend(requestId: String) {

        viewModelScope.launch {
            repository.findFriend(requestId).collect {
                _findFriendState.value = it
            }
        }

    }


    fun getUserCreatedEventList(userId: String) {
        viewModelScope.launch {
            repository.getUserCreatedEventList(userId).collect {
                _createdEventList.value = it
            }
        }
    }

    fun getUserEnrolledEventsList(userId: String) {
        viewModelScope.launch {
            repository.getEnrolledEventsList(userId).collect {
                _enrolledList.value = it
            }
        }
    }

    fun cancelUserEvent(
        userId: String,
        eventId: String,
        eventTitle: String,
        eventTime: Date
    ): Flow<DatabaseState<AlarmItem>> {

        viewModelScope.launch {
            repository.cancelEvent(userId, eventId, eventTime, eventTitle).collect {
                cancelEventState.value = it
            }
        }

        return cancelEventState
    }

}