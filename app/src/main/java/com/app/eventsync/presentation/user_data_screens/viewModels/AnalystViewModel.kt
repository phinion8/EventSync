package com.app.eventsync.presentation.user_data_screens.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.eventsync.data.models.Event
import com.app.eventsync.data.repositories.DatabaseState
import com.app.eventsync.data.repositories.FirebaseDatabaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnalystViewModel @Inject constructor(
    private val repository: FirebaseDatabaseRepository
): ViewModel() {

    private val _eventListState: MutableLiveData<DatabaseState<List<Event>>> = MutableLiveData()
    val eventListState: LiveData<DatabaseState<List<Event>>> = _eventListState

    fun getEventListFromDate(year: Int, month: Int, day: Int){
        viewModelScope.launch {
            repository.getEventListFromDate(year, month, day).collect{
                _eventListState.value = it
            }
        }
    }

}