package com.app.eventsync.presentation.comment_screen.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.eventsync.data.models.CommentItem
import com.app.eventsync.data.repositories.DatabaseState
import com.app.eventsync.data.repositories.FirebaseDatabaseRepository
import com.app.eventsync.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommentsViewModel @Inject constructor(private val repository: FirebaseDatabaseRepository) :
    ViewModel() {

    private val _commentListState: MutableLiveData<DatabaseState<List<CommentItem>>> = MutableLiveData()
    val commentListState: LiveData<DatabaseState<List<CommentItem>>> = _commentListState

    private val _postCommentState: MutableLiveData<Resource> = MutableLiveData()
    val postCommentState: LiveData<Resource> = _postCommentState

    fun postComment(commentItem: CommentItem){
        viewModelScope.launch {
            repository.postCommentOnEvent(commentItem).collect{
                _postCommentState.value = it
            }
        }
    }

    fun getCommentList(eventId: String){
        viewModelScope.launch {
            repository.getCommentList(eventId).collect{
                _commentListState.value = it
            }
        }
    }


}