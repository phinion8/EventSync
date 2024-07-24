package com.app.eventsync.presentation.user_data_screens.add_friend_screen

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
import com.app.eventsync.data.models.User
import com.app.eventsync.data.repositories.DatabaseState
import com.app.eventsync.databinding.FragmentAddFriendBinding
import com.app.eventsync.presentation.profile_page_screen.adapter.FriendListAdapter
import com.app.eventsync.presentation.profile_page_screen.request_list_screen.FriendItemListCallback
import com.app.eventsync.presentation.user_data_screens.viewModels.UserDataViewModel
import com.app.eventsync.utils.Constants
import com.app.eventsync.utils.LoadingDialog
import com.app.eventsync.utils.PreferenceManager
import com.app.eventsync.utils.Resource
import com.app.eventsync.utils.SuccessDialog
import com.app.eventsync.utils.showToast
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class AddFriendFragment : Fragment(), FriendItemListCallback {

    private lateinit var binding: FragmentAddFriendBinding
    private val viewModel: UserDataViewModel by viewModels()
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var adapter: FriendListAdapter

    @Inject
    lateinit var preferenceManager: PreferenceManager


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAddFriendBinding.inflate(layoutInflater, container, false)
        loadingDialog = LoadingDialog(requireContext())
        adapter = FriendListAdapter(this, isInBottomSheet = false)
        showNoFriendText(false)
        var requestId = ""

        if (savedInstanceState == null){
            viewModel.getUserList()
        }


        val usersList: ArrayList<User> = ArrayList()

        binding.usersList.adapter = adapter
        binding.usersList.layoutManager = LinearLayoutManager(requireContext())

        viewModel.removeFriendState.observe(viewLifecycleOwner){ value->
            when(value){
                is Resource.Loading->{
                    loadingDialog.show()
                }
                is Resource.Success->{

                    requireContext().showToast("Your friend has been removed successfully.")

                }
                is Resource.Error->{
                    requireContext().showToast("Something went wrong.")
                }
            }

        }

        viewModel.userListState.observe(viewLifecycleOwner) {value->
            when (value) {
                is DatabaseState.Loading -> {
                    loadingDialog.show()
                }

                is DatabaseState.Success -> {
                    usersList.addAll(value.data!!)
                    loadingDialog.dismiss()

                }

                is DatabaseState.Error -> {
                    showNoFriendText(true)
                    showProfileView(false)
                    loadingDialog.dismiss()
                }
            }
        }

        viewModel.findFriendState.observe(viewLifecycleOwner) { value ->

            when (value) {
                is DatabaseState.Loading -> {
                    loadingDialog.show()
                }

                is DatabaseState.Success -> {
                    showNoFriendText(false)


                        binding.usersList.visibility = View.GONE


                    requestId = value.data!!.id
                    showProfileView(true, value.data.name, value.data.profilePhoto, value.data.id)
                    loadingDialog.dismiss()

                }

                is DatabaseState.Error -> {
                    binding.usersList.visibility = View.GONE
                    showNoFriendText(true)
                    showProfileView(false)
                    loadingDialog.dismiss()
                }
            }

        }

        viewModel.sendFriendState.observe(viewLifecycleOwner) { value ->

            when (value) {
                is Resource.Loading -> {
                    loadingDialog.show()
                }

                is Resource.Success -> {
                    SuccessDialog(
                        requireContext(),
                        "Success",
                        "Request sent successfully."
                    ).showSuccessDialog()
                    loadingDialog.dismiss()


                }

                is Resource.Error -> {
                    loadingDialog.dismiss()
                }
            }

        }


        binding.etUniqueId.addTextChangedListener { searchQuery ->

            binding.usersList.visibility = View.VISIBLE
            val newUserList = usersList.filter { user ->
                user.name.lowercase(Locale.ROOT).contains(
                    searchQuery.toString()
                        .lowercase(Locale.ROOT)

                ) && user.id != preferenceManager.getUserId()
            }
            val friendList = mutableListOf<FriendItem>()
            newUserList.map {
                val friendItem = FriendItem(it.id, it.name, it.profilePhoto, it.friendRequests, it.friendList)
                friendList.add(friendItem)
            }

            if (searchQuery != null) {
                if (searchQuery.isEmpty()){
                    friendList.clear()
                    showNoFriendText(false)
                }
            }

            adapter.updateList(friendList)
            adapter.notifyDataSetChanged()
            if (searchQuery != null) {
                if (searchQuery.isNotEmpty()){
                    checkForNoData()
                }
            }
        }

        binding.submitBtn.setOnClickListener {
            viewModel.findFriend(binding.etUniqueId.text.toString())
        }

        binding.sendRequestBtn.setOnClickListener {
            viewModel.sendFriendRequest(requestId, preferenceManager.getUserId()!!)
        }

        binding.backBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        return binding.root
    }


    private fun checkForNoData(){
        if (adapter.isEmpty()){
            showNoFriendText(true)
        }else{
            showNoFriendText(false)
        }
    }


    private fun showProfileView(
        value: Boolean,
        name: String = "",
        profilePhoto: String = "",
        profileId: String = ""
    ) {
        binding.tvProfileName.isVisible = value
        binding.profileImage.isVisible = value
        binding.sendRequestBtn.isVisible = value
        binding.tvProfileId.isVisible = value
        if (value) {
            binding.tvProfileName.text = name
            binding.etUniqueId.setText(profileId)
            binding.tvProfileId.text = profileId
            Glide.with(requireContext())
                .load(profilePhoto)
                .error(Constants.DEFAULT_PROFILE_IMG)
                .into(binding.profileImage)
        }

    }

    private fun showNoFriendText(value: Boolean) {
        binding.tvNoUser.isVisible = value
    }

    override fun onAcceptClick(requestId: String) {

    }

    override fun onRemoveClick(requestId: String) {
       viewModel.removeFriend(preferenceManager.getUserId()!!, requestId)
    }

    override fun onAddedClick(friendItem: FriendItem) {
        viewModel.sendFriendRequest(friendItem.id, preferenceManager.getUserId()!!)
    }

    override fun onRemoveCollaborator(friendItem: FriendItem) {

    }

    override fun onFriendItemClick(userId: String) {
        val bundle = Bundle()
        bundle.putString(Constants.USER_ID, userId)
        requireActivity().findNavController(R.id.fragmentMainContainerView).navigate(R.id.action_addFriendFragment_to_userProfileFragment, bundle, null)
    }


}