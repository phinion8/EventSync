package com.app.eventsync.presentation.main_screens.chats_screen.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.eventsync.data.models.User
import com.app.eventsync.data.repositories.DatabaseState
import com.app.eventsync.data.repositories.FirebaseDatabaseRepository
import com.app.eventsync.utils.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: FirebaseDatabaseRepository,
    private val preferenceManager: PreferenceManager
): ViewModel() {

    private val _chatFriendListState: MutableLiveData<DatabaseState<List<User>>> =
        MutableLiveData()
    val chatFriendListState: LiveData<DatabaseState<List<User>>> = _chatFriendListState

    init {
        getChatFriendList(preferenceManager.getUserId()!!)
    }

    fun getChatFriendList(userId: String){
        viewModelScope.launch {
            repository.getChatFriendList(userId).collect{
                _chatFriendListState.value = it
            }
        }
    }

}