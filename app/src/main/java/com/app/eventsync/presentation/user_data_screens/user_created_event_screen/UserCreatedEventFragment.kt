package com.app.eventsync.presentation.user_data_screens.user_created_event_screen

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.eventsync.R
import com.app.eventsync.data.models.Event
import com.app.eventsync.data.notification.AlarmSchedulerImp
import com.app.eventsync.data.repositories.DatabaseState
import com.app.eventsync.databinding.FragmentUserCreatedEventBinding
import com.app.eventsync.presentation.main_screens.home_screen.adapters.EventsListAdapter
import com.app.eventsync.presentation.main_screens.home_screen.events_screens.EventItemListener
import com.app.eventsync.presentation.main_screens.home_screen.viewModel.HomeViewModel
import com.app.eventsync.presentation.profile_page_screen.UserProfileFragment
import com.app.eventsync.presentation.user_data_screens.viewModels.UserDataViewModel
import com.app.eventsync.utils.Constants
import com.app.eventsync.utils.EventPreviewDialog
import com.app.eventsync.utils.LoadingDialog
import com.app.eventsync.utils.PreferenceManager
import com.app.eventsync.utils.SuccessDialog
import com.app.eventsync.utils.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
class UserCreatedEventFragment : Fragment(), EventItemListener {

    private lateinit var binding: FragmentUserCreatedEventBinding
    private lateinit var adapter: EventsListAdapter
    private lateinit var loadingDialog: LoadingDialog
    private val viewModel: UserDataViewModel by viewModels()
    private var isProfile: Boolean = false
    private var userId: String = ""
    private var searchEventList: List<Event> = ArrayList()
    private val currentEnrolledList: ArrayList<Event> = ArrayList()
    private var isEnrollEventStateObserved: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val homeViewModel: HomeViewModel by viewModels()
    private lateinit var alarmSchedulerImp: AlarmSchedulerImp

    @Inject
    lateinit var preferenceManager: PreferenceManager


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentUserCreatedEventBinding.inflate(layoutInflater)
        loadingDialog = LoadingDialog(requireContext())
        isProfile = arguments?.getBoolean(Constants.IS_PROFILE) ?: false
        userId = arguments?.getString(Constants.USER_ID).toString()

        alarmSchedulerImp = AlarmSchedulerImp(requireContext())

        if (isProfile) {
            binding.tvEnrolledEvents.visibility = View.GONE
            binding.backBtn.visibility = View.GONE
        } else {
            binding.tvEnrolledEvents.visibility = View.VISIBLE
            binding.backBtn.visibility = View.VISIBLE
        }
        adapter = EventsListAdapter(requireContext(),this, showEditButton = !isProfile)

        if (savedInstanceState == null) {
            viewModel.getUserCreatedEventList(userId)
        }

        viewModel.createdEventList.observe(viewLifecycleOwner) { value ->

            when (value) {
                is DatabaseState.Loading -> {
                    loadingDialog.show()
                }

                is DatabaseState.Success -> {
                    binding.tvEnrolledEvents.text = "Created Events(${value.data!!.size})"
                    value.data.let { adapter.updateEvents(it!!) }
                    currentEnrolledList.addAll(value.data)
                    loadingDialog.dismiss()
                    checkForNoData()
                }

                is DatabaseState.Error -> {
                    loadingDialog.dismiss()
                    requireContext().showToast(value.message!!)
                }

            }


        }

        binding.etSearch.addTextChangedListener { searchText ->
            searchEventList = currentEnrolledList.filter { event ->
                event.title.contains(searchText.toString())
            }
            adapter.updateEvents(searchEventList)

            if (adapter.isEventListEmpty()) {
                binding.noEventAnim.visibility = View.VISIBLE
                binding.etNoEvent.visibility = View.VISIBLE
            } else {
                binding.noEventAnim.visibility = View.GONE
                binding.etNoEvent.visibility = View.GONE
            }
        }



        binding.backBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.rvEvent.adapter = adapter
        binding.rvEvent.layoutManager = LinearLayoutManager(requireContext())
        return binding.root
    }

    private fun checkForNoData() {
        if (adapter.isEventListEmpty()) {
            showNoEventDataLayout(true)
        } else {
            showNoEventDataLayout(false)
        }
    }

    private fun showNoEventDataLayout(value: Boolean) {
        binding.noEventAnim.isVisible = value
        binding.etNoEvent.isVisible = value
    }

    override fun onEventEnrollButtonClick(eventId: String, eventTitle: String, eventTime: Date) {

    }

    override fun onEventItemClick(eventItem: Event) {
        val eventPreviewDialog =
            EventPreviewDialog(requireContext(), eventItem, onClick = { eventId ->
                val navOptions = NavOptions.Builder()
                    .setEnterAnim(R.anim.fade_in)  // the enter animation
                    .setExitAnim(R.anim.fade_out)    // the exit animation
                    .setPopEnterAnim(R.anim.fade_in)  // the pop enter animation
                    .setPopExitAnim(R.anim.fade_out) // the pop exit animation
                    .build()


                val bundle = Bundle()
                bundle.putString(Constants.EVENT_ID, eventId)

                if (parentFragment is UserProfileFragment) {

                    requireActivity().findNavController(R.id.fragmentMainContainerView)
                        .navigate(
                            R.id.action_userProfileFragment_to_eventDetailsFragment,
                            bundle,
                            navOptions
                        )

                } else {
                    requireActivity().findNavController(R.id.fragmentMainContainerView)
                        .navigate(
                            R.id.action_userCreatedEventFragment_to_eventDetailsFragment2,
                            bundle,
                            navOptions
                        )
                }


            }, onEnrollButtonClick = {


                CoroutineScope(Dispatchers.Main).launch {
                    isEnrollEventStateObserved.value = false
                    homeViewModel.enrollUserToEvent(
                        preferenceManager.getUserId()!!,
                        eventItem.id,
                        eventItem.title,
                        eventItem.startTime!!
                    ).collect { value ->
                        when (value) {
                            is DatabaseState.Loading -> {
                                loadingDialog.show()
                            }

                            is DatabaseState.Success -> {
                                if (!isEnrollEventStateObserved.value) {
                                    isEnrollEventStateObserved.value = true
                                    loadingDialog.dismiss()
                                    alarmSchedulerImp.schedule(value.data!!)
                                    SuccessDialog(
                                        requireContext(),
                                        "Successfully Enrolled..",
                                        "You can check your enrolled event by going to enrolled events section."
                                    )
                                        .showSuccessDialog()
                                    viewModel.getUserCreatedEventList(userId)
                                }

                            }

                            is DatabaseState.Error -> {
                                if (!isEnrollEventStateObserved.value) {
                                    isEnrollEventStateObserved.value = true
                                    loadingDialog.dismiss()
                                }

                            }
                        }
                    }
                }


            }, onCancelButtonClick = {

                CoroutineScope(Dispatchers.Main).launch {
                    viewModel.cancelUserEvent(
                        preferenceManager.getUserId()!!,
                        eventItem.id,
                        eventItem.title,
                        eventItem.startTime!!
                    ).collect { value ->
                        when (value) {
                            is DatabaseState.Loading -> {
                                loadingDialog.show()
                            }

                            is DatabaseState.Success -> {

                                loadingDialog.dismiss()
                                requireContext().showToast("Success")
                                alarmSchedulerImp.cancel(value.data!!)
                                SuccessDialog(
                                    requireContext(),
                                    "Event Cancelled...",
                                    "Event has been deleted from your event list"
                                )
                                    .showSuccessDialog()

                                viewModel.getUserCreatedEventList(userId)


                            }

                            is DatabaseState.Error -> {

                                loadingDialog.dismiss()


                            }
                        }
                    }
                }

            })

        eventPreviewDialog.show()
    }

    override fun onEventEditButtonClick(eventId: String) {

        val navOptions = NavOptions.Builder()
            .setEnterAnim(R.anim.fade_in)  // the enter animation
            .setExitAnim(R.anim.fade_out)    // the exit animation
            .setPopEnterAnim(R.anim.fade_in)  // the pop enter animation
            .setPopExitAnim(R.anim.fade_out) // the pop exit animation
            .build()


        val bundle = Bundle()
        bundle.putString(Constants.EVENT_ID, eventId)
        bundle.putBoolean(Constants.IS_EDIT_MODE, true)

        requireActivity().findNavController(R.id.fragmentMainContainerView).navigate(
            R.id.action_userCreatedEventFragment_to_createEventFragment,
            bundle,
            navOptions
        )


    }


}