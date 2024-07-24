package com.app.eventsync.presentation.event_screens.create_event_screen.viewModel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.eventsync.data.models.Event
import com.app.eventsync.data.models.EventImage
import com.app.eventsync.data.repositories.DatabaseState
import com.app.eventsync.data.repositories.FirebaseDatabaseRepository
import com.app.eventsync.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateEventViewModel @Inject constructor(
    private val databaseRepository: FirebaseDatabaseRepository
) : ViewModel() {

    private val _eventSavedState: MutableLiveData<Resource> = MutableLiveData()
    val eventSavedState: LiveData<Resource> = _eventSavedState
    var isEditMode: MutableStateFlow<Boolean> = MutableStateFlow(false)
    var eventId: MutableStateFlow<String> = MutableStateFlow("")
    private val _triggerSaveEvent: MutableLiveData<Boolean> = MutableLiveData(false)
    val triggerSaveEvent: LiveData<Boolean> = _triggerSaveEvent
    private val _eventDetailsState: MutableLiveData<DatabaseState<Event>> = MutableLiveData()
    val eventDetailsState : LiveData<DatabaseState<Event>> = _eventDetailsState

    private val _otherUsersImageState: MutableLiveData<DatabaseState<List<EventImage>>> = MutableLiveData()
    val otherUsersImageState: LiveData<DatabaseState<List<EventImage>>> = _otherUsersImageState

    private val _editEventState: MutableLiveData<Resource> = MutableLiveData()
    val editEventState: LiveData<Resource> = _editEventState

    private val _deleteEventState: MutableLiveData<Resource> = MutableLiveData()
    val deleteEventState: LiveData<Resource> = _deleteEventState

    private val _triggerDeleteButton: MutableLiveData<Boolean> = MutableLiveData()
    val triggerDeleteButton: LiveData<Boolean> = _triggerDeleteButton

    private val _triggerUpdateButton: MutableLiveData<Boolean> = MutableLiveData()
    val triggerUpdateButton: LiveData<Boolean> = _triggerUpdateButton

    private val _eventTitle: MutableLiveData<String> = MutableLiveData()
    val eventTitle: LiveData<String> = _eventTitle

    val mediaUriList: ArrayList<String> = ArrayList()

    private val _updateEventImageState: MutableLiveData<DatabaseState<String>> = MutableLiveData()
    val updateEventImageState: LiveData<DatabaseState<String>> = _updateEventImageState

    private val _otherUserRemoveImageState: MutableLiveData<Resource> = MutableLiveData()
    val otherUserRemoveImageState: LiveData<Resource> = _otherUserRemoveImageState

    private val _removeImageFromEventState: MutableLiveData<Resource> = MutableLiveData()
    val removeImageFromEventState: LiveData<Resource> = _removeImageFromEventState

    fun removeImageFromEvent(imageUrl: String, eventId: String){
        viewModelScope.launch {
            databaseRepository.removeImageFromEvent(imageUrl, eventId).collect{
                _removeImageFromEventState.postValue(it)
            }
        }
    }

    fun removeOtherUserImage(eventImage: EventImage){
        viewModelScope.launch {
            databaseRepository.removeOtherUserImage(eventImage).collect{
                _otherUserRemoveImageState.postValue(it)
            }
        }
    }


    fun getOtherUserImageList(eventId: String){
        viewModelScope.launch {
            databaseRepository.getOtherUsersImageList(eventId).collect{
                _otherUsersImageState.value = it
            }
        }
    }


    fun updateEventImageList(imageUri: Uri, eventId: String){
        viewModelScope.launch {
            databaseRepository.updateImageInEvent(imageUri, eventId).collect{
                _updateEventImageState.value = it
            }
        }
    }

    fun saveEvent(event: Event){
        viewModelScope.launch {
            databaseRepository.saveEventToTheDatabase(event).collect{
                _eventSavedState.value = it
            }
        }
    }

    fun setEventTitle(value: String){
        _eventTitle.value = value
    }

    fun onUpdateButtonClick(value: String){
        setEventTitle(value)
        _triggerUpdateButton.value = true
    }

    fun onDeleteButtonClick(){
        _triggerDeleteButton.value = true
    }

    fun createEvent(eventTitle: String){
        setEventTitle(eventTitle)
        _triggerSaveEvent.value = true
    }

    fun getEventDetails(eventId: String){
        viewModelScope.launch{
            databaseRepository.getEventDetails(eventId).collect{
                _eventDetailsState.value = it
            }
        }
    }

    fun updateEvent(event: Event){
        viewModelScope.launch {
            databaseRepository.updateEvent(event).collect{
                _editEventState.value= it
            }
        }
    }

    fun deleteEvent(eventId: String){
        viewModelScope.launch {
            databaseRepository.deleteEvent(eventId).collect{
                _deleteEventState.value = it
            }
        }
    }



}