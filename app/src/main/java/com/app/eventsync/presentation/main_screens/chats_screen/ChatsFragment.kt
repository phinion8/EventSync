package com.app.eventsync.presentation.main_screens.chats_screen

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.eventsync.data.models.User
import com.app.eventsync.presentation.main_screens.chats_screen.chat_activity.ChatActivity
import com.app.eventsync.data.repositories.DatabaseState
import com.app.eventsync.databinding.FragmentChatsBinding
import com.app.eventsync.presentation.main_screens.chats_screen.adapters.ChatListAdapter
import com.app.eventsync.presentation.main_screens.chats_screen.viewModel.ChatViewModel
import com.app.eventsync.utils.Constants
import com.app.eventsync.utils.LoadingDialog
import com.app.eventsync.utils.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class ChatsFragment : Fragment(), ChatItemListener {

    private lateinit var binding: FragmentChatsBinding
    private lateinit var adapter: ChatListAdapter
    private val viewModel: ChatViewModel by viewModels()
    @Inject
    lateinit var preferenceManager: PreferenceManager
    private lateinit var loadingDialog: LoadingDialog
    private var currentChatList: List<User> = ArrayList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentChatsBinding.inflate(layoutInflater)
        loadingDialog = LoadingDialog(requireContext())

        viewModel.chatFriendListState.observe(viewLifecycleOwner){value->

            when(value){
                is DatabaseState.Loading->{
                    loadingDialog.show()

                }
                is DatabaseState.Success->{

                    loadingDialog.dismiss()
                    adapter.updateList(value.data!!)
                    currentChatList = value.data
                    checkForNoData()

                }
                is DatabaseState.Error->{

                    loadingDialog.dismiss()
                }
            }

        }
        adapter = ChatListAdapter(this)

        binding.etSearch.addTextChangedListener {searchQuery->

            val searchList = currentChatList.filter {user ->

                user.name.contains(searchQuery.toString())

            }

            adapter.updateList(searchList)
            checkForNoData()

        }

        binding.rvChats.adapter = adapter
        binding.rvChats.layoutManager = LinearLayoutManager(requireContext())

        return binding.root
    }

    override fun onChatItemClick(userId: String, profilePic: String) {

        startActivity(Intent(requireContext(), ChatActivity::class.java).apply {
            putExtra(Constants.USER_ID, userId)
            putExtra(Constants.RECEIVER_USER_PROFILE_PIC, profilePic)
        })

    }

    private fun checkForNoData() {
        if (adapter.isEmpty()) {
            showNoEventDataLayout(true)
        } else {
            showNoEventDataLayout(false)
        }
    }

    private fun showNoEventDataLayout(value: Boolean) {
        binding.noEventAnim.isVisible = value
        binding.etNoEvent.isVisible = value
    }


}

interface ChatItemListener{
    fun onChatItemClick(userId: String, profilePic: String)
}