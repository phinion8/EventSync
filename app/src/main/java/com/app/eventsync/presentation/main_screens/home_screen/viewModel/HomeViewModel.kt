package com.app.eventsync.presentation.main_screens.home_screen.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.eventsync.data.models.AlarmItem
import com.app.eventsync.data.models.Event
import com.app.eventsync.data.repositories.DatabaseState
import com.app.eventsync.data.repositories.EventListType
import com.app.eventsync.data.repositories.FirebaseDatabaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val databaseRepository: FirebaseDatabaseRepository
) : ViewModel() {

    private val _isScrollUp: MutableLiveData<Boolean> = MutableLiveData(false)
    val isScrollUp: LiveData<Boolean> = _isScrollUp
    private val _todayEventListSize: MutableLiveData<Int> = MutableLiveData()
    val todayEventListSize: LiveData<Int> = _todayEventListSize
    private val _upcomingEventListSize: MutableLiveData<Int> = MutableLiveData()
    val upcomingEventListSize: LiveData<Int> = _upcomingEventListSize
    private val _laterEventListSize: MutableLiveData<Int> = MutableLiveData()
    val laterEventListSize: LiveData<Int> = _laterEventListSize
    private val todayDataList = databaseRepository.getEventListFromDatabase()
    private val upcomingDataList =
        databaseRepository.getEventListFromDatabase(EventListType.UPCOMING)
    private val laterDataList = databaseRepository.getEventListFromDatabase(EventListType.LATER)

    private val enrollEventState: MutableStateFlow<DatabaseState<AlarmItem>> =
        MutableStateFlow(DatabaseState.Loading())

    private val cancelEventState: MutableStateFlow<DatabaseState<AlarmItem>> = MutableStateFlow(DatabaseState.Loading())



    fun setIsScrollUp(value: Boolean) {
        _isScrollUp.value = value

    }

    fun setTodayEventListSize(value: Int) {
        _todayEventListSize.value = value
    }

    fun setUpcomingEventListSize(value: Int) {
        _upcomingEventListSize.value = value
    }

    fun setLaterEventListSize(value: Int) {
        _laterEventListSize.value = value
    }


    fun getTodayDataList(): LiveData<DatabaseState<List<Event>>> {
        return todayDataList
    }

    fun getUpcomingDataList(): LiveData<DatabaseState<List<Event>>> {
        return upcomingDataList
    }

    fun getLaterDataList(): LiveData<DatabaseState<List<Event>>> {
        return laterDataList
    }

    fun enrollUserToEvent(
        userId: String,
        eventId: String,
        eventTitle: String,
        eventTime: Date
    ): Flow<DatabaseState<AlarmItem>> {

        viewModelScope.launch {
            databaseRepository.saveEventToUserDatabase(userId, eventId, eventTime, eventTitle)
                .collect {
                    enrollEventState.value = it
                }
        }

        return enrollEventState

    }

    fun cancelUserEvent(
        userId: String,
        eventId: String,
        eventTitle: String,
        eventTime: Date
    ): Flow<DatabaseState<AlarmItem>>{

        viewModelScope.launch {
            databaseRepository.cancelEvent(userId, eventId, eventTime, eventTitle).collect{
                cancelEventState.value = it
            }
        }

        return cancelEventState
    }
}

