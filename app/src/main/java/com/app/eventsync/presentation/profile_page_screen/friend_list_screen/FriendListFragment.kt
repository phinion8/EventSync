package com.app.eventsync.presentation.profile_page_screen.friend_list_screen

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.eventsync.R
import com.app.eventsync.data.models.FriendItem
import com.app.eventsync.data.repositories.DatabaseState
import com.app.eventsync.databinding.FragmentFriendListBinding
import com.app.eventsync.presentation.profile_page_screen.adapter.FriendListAdapter
import com.app.eventsync.presentation.profile_page_screen.request_list_screen.FriendItemListCallback
import com.app.eventsync.presentation.profile_page_screen.viewModel.UserProfileViewModel
import com.app.eventsync.utils.Constants
import com.app.eventsync.utils.LoadingDialog
import com.app.eventsync.utils.PreferenceManager
import com.app.eventsync.utils.Resource
import com.app.eventsync.utils.showToast
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FriendListFragment : Fragment(), FriendItemListCallback {

    private lateinit var binding: FragmentFriendListBinding
    private lateinit var adapter: FriendListAdapter
    private val viewModel: UserProfileViewModel by viewModels()
    private lateinit var loadingDialog: LoadingDialog
    private var isInBottomSheet: Boolean = false
    private var currentFriendList: List<FriendItem> = ArrayList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFriendListBinding.inflate(inflater, container, false)
        loadingDialog = LoadingDialog(requireContext())


        isInBottomSheet = arguments?.getBoolean(Constants.IS_IN_BOTTOM_SHEET, false) ?: false


        adapter = FriendListAdapter(this, isInBottomSheet = false)

        viewModel.getFriendList(PreferenceManager(requireContext()).getUserId()!!)

        viewModel.friendListState.observe(viewLifecycleOwner){value->
            when(value){
                is DatabaseState.Loading->{
                    loadingDialog.show()

                }
                is DatabaseState.Success->{

                    adapter.updateList(value.data!!)
                    currentFriendList = value.data
                    checkForNoData()
                    loadingDialog.dismiss()

                }
                is DatabaseState.Error->{
                    loadingDialog.dismiss()
                }
            }
        }

        viewModel.removeFriendState.observe(viewLifecycleOwner){value->
            when(value){
                is Resource.Loading->{
                    loadingDialog.show()
                }
                is Resource.Success->{
                    requireContext().showToast("Friend removed successfully...")
                    viewModel.getFriendList(PreferenceManager(requireContext()).getUserId()!!)
                    loadingDialog.dismiss()
                }
                is Resource.Error->{
                    loadingDialog.dismiss()
                }
            }
        }

        binding.etSearch.addTextChangedListener {searchQuery->

            val searchList = currentFriendList.filter {friendItem ->

                friendItem.name.contains(searchQuery.toString())

            }

            adapter.updateList(searchList)
            checkForNoData()

        }


        binding.backBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.rvFriend.adapter = adapter
        binding.rvFriend.layoutManager = LinearLayoutManager(requireContext())
        return binding.root
    }

    private fun checkForNoData(){
        if (adapter.isEmpty()){
            showNoDataLayout(true)
        }else{
            showNoDataLayout(false)
        }
    }


    private fun showNoDataLayout(value: Boolean){
        binding.noFriendFoundAnim.isVisible = value
        binding.noDataText.isVisible= value
    }

    override fun onAcceptClick(requestId: String) {

    }

    override fun onRemoveClick(requestId: String) {

        viewModel.removeFriend(PreferenceManager(requireContext()).getUserId()!!, requestId)

    }

    override fun onAddedClick(friendItem: FriendItem) {

    }

    override fun onRemoveCollaborator(friendItem: FriendItem) {

    }

    override fun onFriendItemClick(userId: String) {
        val bundle = Bundle()
        bundle.putString(Constants.USER_ID, userId)
        requireActivity().findNavController(R.id.fragmentMainContainerView).navigate(R.id.action_friendListFragment_to_userProfileFragment, bundle, null)

    }

}