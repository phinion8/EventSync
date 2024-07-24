package com.app.eventsync.presentation.event_screens.event_detail_screen.add_friend_in_event_screen

import android.os.Bundle
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
import com.app.eventsync.databinding.CollaboratorSelectionBottomSheetLayoutBinding
import com.app.eventsync.presentation.event_screens.event_detail_screen.add_friend_in_event_screen.adapter.CollaboratorListAdapter
import com.app.eventsync.presentation.event_screens.event_detail_screen.create_event_detail.CreateEventDetailFragment
import com.app.eventsync.presentation.profile_page_screen.adapter.FriendListAdapter
import com.app.eventsync.presentation.profile_page_screen.request_list_screen.FriendItemListCallback
import com.app.eventsync.presentation.profile_page_screen.viewModel.UserProfileViewModel
import com.app.eventsync.utils.Constants
import com.app.eventsync.utils.LoadingDialog
import com.app.eventsync.utils.PreferenceManager
import com.app.eventsync.utils.Resource
import com.app.eventsync.utils.showToast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FriendListBottomSheetFragment : BottomSheetDialogFragment(), FriendItemListCallback,
    CollaboratorItemListener {
    private lateinit var binding: CollaboratorSelectionBottomSheetLayoutBinding
    private lateinit var adapter: FriendListAdapter
    private lateinit var collaboratorListAdapter: CollaboratorListAdapter
    private val viewModel: UserProfileViewModel by viewModels(
        ownerProducer = { requireParentFragment() }
    )
    private var collaboratorList: List<FriendItem> = ArrayList()
    private lateinit var loadingDialog: LoadingDialog
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            CollaboratorSelectionBottomSheetLayoutBinding.inflate(layoutInflater, container, false)

        loadingDialog = LoadingDialog(requireContext())

        val isShowCollaborators = arguments?.getBoolean(Constants.IS_COLLABORATOR_LIST)
        val eventId = arguments?.getString(Constants.EVENT_ID)

        if (isShowCollaborators == true) {
            if (eventId != null) {
                binding.submitListBtn.visibility = View.GONE
                binding.tvTitle.text = "Collaborators List"
                binding.rvCollaborators.visibility = View.GONE
                adapter = FriendListAdapter(this, false, false, true)
                viewModel.getCollaboratorList(eventId)
                viewModel.enrolledListState.observe(viewLifecycleOwner) { value ->
                    when (value) {
                        is DatabaseState.Loading -> {
                            loadingDialog.show()

                        }

                        is DatabaseState.Success -> {

                            adapter.updateList(value.data!!)
                            collaboratorList = value.data
                            checkForNoData()
                            loadingDialog.dismiss()

                        }

                        is DatabaseState.Error -> {
                            loadingDialog.dismiss()
                        }
                    }

                }
            }
        } else {

            binding.submitListBtn.visibility = View.VISIBLE
            binding.tvTitle.text = "Select Collaborators"
            binding.rvCollaborators.visibility = View.VISIBLE

            viewModel.collaboratorIdList.clear()

            adapter = FriendListAdapter(this, isInBottomSheet = true)
            collaboratorListAdapter = CollaboratorListAdapter(this)


            if (savedInstanceState == null) {
                viewModel.getFriendList(PreferenceManager(requireContext()).getUserId()!!)
            }


            viewModel.friendListState.observe(viewLifecycleOwner) { value ->
                when (value) {
                    is DatabaseState.Loading -> {
                        loadingDialog.show()

                    }

                    is DatabaseState.Success -> {

                        adapter.updateList(value.data!!)
                        collaboratorList = value.data
                        checkForNoData()
                        loadingDialog.dismiss()

                    }

                    is DatabaseState.Error -> {
                        loadingDialog.dismiss()
                    }
                }
            }

            viewModel.removeFriendState.observe(viewLifecycleOwner) { value ->
                when (value) {
                    is Resource.Loading -> {
                        loadingDialog.show()
                    }

                    is Resource.Success -> {
                        requireContext().showToast("Friend removed successfully...")
                        viewModel.getFriendList(PreferenceManager(requireContext()).getUserId()!!)
                        loadingDialog.dismiss()
                    }

                    is Resource.Error -> {
                        loadingDialog.dismiss()
                    }
                }
            }

            binding.submitListBtn.setOnClickListener {
                viewModel.collaboratorList.postValue(collaboratorListAdapter.returnList())
                dismissAllowingStateLoss()
            }


            binding.rvCollaborators.adapter = collaboratorListAdapter
            binding.rvCollaborators.layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)


        }

        binding.etSearch.addTextChangedListener {searchQuery->

            val searchList = collaboratorList.filter {friendItem ->

                friendItem.name.contains(searchQuery.toString())

            }

            adapter.updateList(searchList)
            checkForNoData()

        }


        binding.rvFriend.adapter = adapter
        binding.rvFriend.layoutManager = LinearLayoutManager(requireContext())
        return binding.root
    }

    private fun checkForNoData() {
        if (adapter.isEmpty()) {
            showNoDataLayout(true)
        } else {
            showNoDataLayout(false)
        }
    }


    private fun showNoDataLayout(value: Boolean) {
        binding.noFriendFoundAnim.isVisible = value
        binding.noDataText.isVisible = value
    }

    override fun onAcceptClick(requestId: String) {

    }

    override fun onRemoveClick(requestId: String) {

        viewModel.removeFriend(PreferenceManager(requireContext()).getUserId()!!, requestId)

    }

    override fun onAddedClick(friendItem: FriendItem) {

        binding.submitListBtn.visibility = View.VISIBLE
        viewModel.setCollaboratorListAddedState()
        viewModel.collaboratorIdList.add(friendItem.id)
        collaboratorListAdapter.addItem(friendItem)
        collaboratorListAdapter.notifyDataSetChanged()

    }

    override fun onRemoveCollaborator(friendItem: FriendItem) {

        if (collaboratorListAdapter.isEmpty()) {
            binding.submitListBtn.visibility = View.GONE
        }
        viewModel.collaboratorIdList.remove(friendItem.id)
        collaboratorListAdapter.removeItem(friendItem)
        collaboratorListAdapter.notifyDataSetChanged()

    }

    override fun onFriendItemClick(userId: String) {
        if (parentFragment is CreateEventDetailFragment) {

        } else {
            val bundle = Bundle()
            bundle.putString(Constants.USER_ID, userId)
            requireActivity().findNavController(R.id.fragmentMainContainerView)
                .navigate(R.id.action_eventDetailsFragment_to_userProfileFragment, bundle, null)

        }

    }

    override fun onRemoveClick(friendItem: FriendItem) {
        if (collaboratorListAdapter.isEmpty()) {
            binding.submitListBtn.visibility = View.GONE
        }
        collaboratorListAdapter.removeItem(friendItem)
        collaboratorListAdapter.notifyDataSetChanged()
    }


}

interface CollaboratorItemListener {
    fun onRemoveClick(friendItem: FriendItem)
}

