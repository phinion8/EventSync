package com.app.eventsync.presentation.profile_page_screen

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import com.app.eventsync.R
import com.app.eventsync.data.models.User
import com.app.eventsync.data.repositories.DatabaseState
import com.app.eventsync.databinding.FragmentUserProfileScreenBinding
import com.app.eventsync.presentation.main_screens.chats_screen.chat_activity.ChatActivity
import com.app.eventsync.presentation.main_screens.home_screen.adapters.VPAdapter
import com.app.eventsync.presentation.profile_page_screen.viewModel.UserProfileViewModel
import com.app.eventsync.presentation.user_data_screens.enrolled_event_screen.EnrolledEventsFragment
import com.app.eventsync.presentation.user_data_screens.user_created_event_screen.UserCreatedEventFragment
import com.app.eventsync.utils.Constants
import com.app.eventsync.utils.ErrorDialog
import com.app.eventsync.utils.LoadingDialog
import com.app.eventsync.utils.PreferenceManager
import com.app.eventsync.utils.showToast
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class UserProfileFragment : Fragment() {

    private lateinit var binding: FragmentUserProfileScreenBinding

    @Inject
    lateinit var preferenceManager: PreferenceManager
    private lateinit var vpAdapter: VPAdapter
    private val viewModel: UserProfileViewModel by viewModels()
    private var isUser: Boolean = true
    private lateinit var loadingDialog: LoadingDialog


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentUserProfileScreenBinding.inflate(layoutInflater)
        val userId = arguments?.getString(Constants.USER_ID)
        loadingDialog = LoadingDialog(requireContext())

        isUser = preferenceManager.getUserId()!! == userId

        vpAdapter = VPAdapter(childFragmentManager)

        val bundle = Bundle()
        bundle.putBoolean(Constants.IS_PROFILE, true)
        bundle.putString(Constants.USER_ID, userId)


        val userCreatedEventFragment = UserCreatedEventFragment()
        userCreatedEventFragment.arguments = bundle

        val enrolledEventsFragment = EnrolledEventsFragment()
        enrolledEventsFragment.arguments = bundle

        vpAdapter.addFragment(userCreatedEventFragment, "Created")
        vpAdapter.addFragment(enrolledEventsFragment, "Enrolled")

        binding.tabLayout.setupWithViewPager(binding.profileViewPager)

        binding.profileViewPager.adapter = vpAdapter

        if (savedInstanceState == null) {
            if (userId != null) {
                viewModel.getUserDetails(userId)
            }
        }


        if (isUser) {
            binding.requestLayout.visibility = View.VISIBLE
            binding.requestText.visibility = View.VISIBLE
            binding.tvRequestCount.visibility = View.VISIBLE
            binding.addFriendBtn.visibility = View.GONE
            binding.chatBtn.visibility = View.GONE
            binding.friendsLayout.visibility = View.VISIBLE
        } else {

            binding.requestText.visibility = View.GONE
            binding.tvRequestCount.visibility = View.GONE
            binding.addFriendBtn.visibility = View.VISIBLE
            binding.chatBtn.visibility = View.VISIBLE
            binding.friendsLayout.visibility = View.GONE

        }

        viewModel.userDetailsState.observe(viewLifecycleOwner) { value ->
            when (value) {
                is DatabaseState.Loading -> {

                }

                is DatabaseState.Success -> {

                    updateProfileViews(value.data!!)

                }

                is DatabaseState.Error -> {

                }
            }
        }



        binding.addFriendBtn.setOnClickListener {
            if (binding.addFriendBtn.text == "Remove") {
                ErrorDialog(
                    requireContext(),
                    "Are you sure?",
                    "Your friend will be removed from your friend list. You won't be able to chat your friend.",
                    onPositiveButtonClick = {
                        loadingDialog.show()
                        FirebaseFirestore.getInstance().collection("users")
                            .document(preferenceManager.getUserId()!!)
                            .update("friendList", FieldValue.arrayRemove(userId))

                        if (userId != null) {
                            FirebaseFirestore.getInstance().collection("users")
                                .document(userId)
                                .update(
                                    "friendList",
                                    FieldValue.arrayRemove(preferenceManager.getUserId())
                                )
                                .addOnSuccessListener {
                                    loadingDialog.dismiss()
                                    requireContext().showToast("Removed successfully.")

                                }.addOnFailureListener {
                                    loadingDialog.dismiss()
                                    requireContext().showToast("Something went wrong!")
                                }
                        }
                    }).showErrorDialog()


            } else {
                if (userId != null) {
                    FirebaseFirestore.getInstance().collection("users")
                        .document(userId)
                        .update(
                            "friendRequests",
                            FieldValue.arrayUnion(preferenceManager.getUserId()!!)
                        )
                        .addOnSuccessListener {
                            binding.addFriendBtn.text = "Requested"
                        }
                }
            }

        }

        binding.backBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.friendsLayout.setOnClickListener {
            requireActivity().findNavController(R.id.fragmentMainContainerView)
                .navigate(R.id.action_userProfileFragment_to_friendListFragment)
        }

        binding.requestLayout.setOnClickListener {
            requireActivity().findNavController(R.id.fragmentMainContainerView)
                .navigate(R.id.action_userProfileFragment_to_requestListFragment)
        }

        binding.chatBtn.setOnClickListener {
            if (userId != null) {
                moveToChatScreen(userId)
            }
        }


        return binding.root
    }

    private fun updateProfileViews(user: User) {
        with(binding) {
            Glide.with(requireContext())
                .load(user.profilePhoto)
                .error(R.drawable.sample_profile_img)
                .into(profileImage)

            tvUserName.text = user.name
            tvUserEmail.text = user.email
            tvFollowersCount.text = user.friendList.size.toString()
            tvRequestCount.text = user.friendRequests.size.toString()

            if (user.friendRequests.contains(preferenceManager.getUserId()!!)) {
                binding.view.visibility = View.GONE
                binding.addFriendBtn.text = "Requested"
                binding.chatBtn.visibility = View.GONE
                binding.addFriendBtn.isEnabled = false
            } else if (user.friendList.contains(preferenceManager.getUserId())) {
                binding.view.visibility = View.VISIBLE
                binding.chatBtn.visibility = View.VISIBLE
                binding.addFriendBtn.text = "Remove"
                binding.addFriendBtn.isEnabled = true
            } else {
                binding.view.visibility = View.GONE
                binding.chatBtn.visibility = View.GONE
                binding.addFriendBtn.text = "+Add"
                binding.addFriendBtn.isEnabled = true
            }
        }
    }

    private fun moveToChatScreen(userId: String) {
        startActivity(
            Intent(requireContext(), ChatActivity::class.java)
                .putExtra(Constants.USER_ID, userId)
        )
    }

}