package com.app.eventsync.presentation.profile_page_screen.viewModel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.eventsync.data.models.FriendItem
import com.app.eventsync.data.models.User
import com.app.eventsync.data.repositories.DatabaseState
import com.app.eventsync.data.repositories.FirebaseDatabaseRepository
import com.app.eventsync.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserProfileViewModel @Inject constructor(private val repository: FirebaseDatabaseRepository) :
    ViewModel() {

    private val _requestListState: MutableLiveData<DatabaseState<List<FriendItem>>> =
        MutableLiveData()
    val requestListState: LiveData<DatabaseState<List<FriendItem>>> = _requestListState

    private val _friendListState: MutableLiveData<DatabaseState<List<FriendItem>>> =
        MutableLiveData()
    val friendListState: LiveData<DatabaseState<List<FriendItem>>> = _friendListState

    private val _acceptFriendState: MutableLiveData<Resource> = MutableLiveData()
    val acceptFriendState: LiveData<Resource> = _acceptFriendState

    private val _removeFriendState: MutableLiveData<Resource> = MutableLiveData()
    val removeFriendState: LiveData<Resource> = _removeFriendState

    private val _userDetailsState: MutableLiveData<DatabaseState<User>> = MutableLiveData()
    val userDetailsState: LiveData<DatabaseState<User>> = _userDetailsState

    private val _updateProfileState: MutableLiveData<Resource> = MutableLiveData()
    val updateProfileState: LiveData<Resource> = _updateProfileState

    private val _uploadImageState: MutableLiveData<DatabaseState<String>> = MutableLiveData()
    val uploadImageState: LiveData<DatabaseState<String>> = _uploadImageState

    val collaboratorIdList: ArrayList<String> = ArrayList()

    val collaboratorList: MutableLiveData<ArrayList<FriendItem>> = MutableLiveData()


    private val _collaboratorListAddedState: MutableLiveData<Boolean> = MutableLiveData()
    val collaboratorListAddedState: LiveData<Boolean> = _collaboratorListAddedState

    private val _enrolledListState: MutableLiveData<DatabaseState<List<FriendItem>>> = MutableLiveData()
    val enrolledListState: LiveData<DatabaseState<List<FriendItem>>> = _enrolledListState

    fun setCollaboratorListAddedState(){
        _collaboratorListAddedState.value = true
    }

    fun addItemToCollaborator(friendItem: FriendItem){
        collaboratorList.value?.add(friendItem)
    }



    fun uploadProfilePhoto(userId: String, imageUri: Uri){
        viewModelScope.launch {
            repository.uploadProfilePhoto(imageUri, userId).collect{
                _uploadImageState.value = it
            }
        }
    }

    fun updateProfileDetails(userId: String, name: String, profilePhoto: String){

        viewModelScope.launch {
            repository.updateProfileInfo(userId, name, profilePhoto).collect{
                _updateProfileState.value = it
            }
        }

    }

    fun getUserDetails(userId: String){

        viewModelScope.launch {
            repository.getUserDetails(userId).collect{
                _userDetailsState.value = it
            }
        }

    }

    fun removeFriend(userId: String, friendId: String) {
        viewModelScope.launch {
            repository.removeFriend(userId, friendId)
                .collect {
                    _removeFriendState.value = it
                }
        }
    }

    fun acceptFriendRequest(userId: String, requestId: String) {
        viewModelScope.launch {
            repository.acceptFriendRequest(userId, requestId)
                .collect {
                    _acceptFriendState.value = it
                }
        }
    }

    fun getFriendList(userId: String) {
        viewModelScope.launch {
            repository.getFriendList(userId).collect {
                _friendListState.value = it
            }
        }
    }


    fun getRequestList(userId: String) {
        viewModelScope.launch {
            repository.getRequestList(userId).collect {
                _requestListState.value = it
            }
        }
    }

    fun getCollaboratorList(eventId: String){
        viewModelScope.launch {
            repository.getCollaboratorList(eventId).collect{
                _enrolledListState.value = it
            }
        }
    }


}