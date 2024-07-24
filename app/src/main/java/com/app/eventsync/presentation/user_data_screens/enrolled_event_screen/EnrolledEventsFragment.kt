package com.app.eventsync.presentation.user_data_screens.enrolled_event_screen

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
import com.app.eventsync.databinding.FragmentEnrolledEventsBinding
import com.app.eventsync.presentation.main_screens.home_screen.adapters.EventsListAdapter
import com.app.eventsync.presentation.main_screens.home_screen.events_screens.EventItemListener
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
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject


@AndroidEntryPoint
class EnrolledEventsFragment : Fragment(), EventItemListener {


    private lateinit var binding: FragmentEnrolledEventsBinding
    private lateinit var adapter: EventsListAdapter
    private val viewModel: UserDataViewModel by viewModels()
    private lateinit var loadingDialog: LoadingDialog
    @Inject
    lateinit var preferenceManager: PreferenceManager
    private lateinit var alarmSchedulerImp: AlarmSchedulerImp
    private var isProfile: Boolean = false
    private var currentEnrolledEventList: List<Event> = ArrayList()
    private var userId: String = ""


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentEnrolledEventsBinding.inflate(layoutInflater)
        adapter = EventsListAdapter(requireContext(),this)
        loadingDialog = LoadingDialog(requireContext())
        alarmSchedulerImp = AlarmSchedulerImp(requireContext())
        isProfile = arguments?.getBoolean(Constants.IS_PROFILE) ?: false
        userId = arguments?.getString(Constants.USER_ID).toString()


        if (isProfile){
            binding.tvEnrolledEvents.visibility = View.GONE
            binding.backBtn.visibility = View.GONE
        }else{
            binding.tvEnrolledEvents.visibility = View.VISIBLE
            binding.backBtn.visibility = View.VISIBLE
        }


        viewModel.getUserEnrolledEventsList(userId)
        loadingDialog.show()


        viewModel.enrolledList.observe(viewLifecycleOwner) { value ->

            when (value) {
                is DatabaseState.Loading -> {

                }

                is DatabaseState.Success -> {
                    binding.tvEnrolledEvents.text = "Enrolled Events(${value.data!!.size})"
                    value.data.let { adapter.updateEvents(it!!) }
                    currentEnrolledEventList = value.data
                    loadingDialog.dismiss()
                    checkForNoData()
                }

                is DatabaseState.Error -> {
                    loadingDialog.dismiss()
                    requireContext().showToast(value.message!!)
                }

            }


        }


        binding.etSearch.addTextChangedListener {searchQuery->

            val searchList = currentEnrolledEventList.filter {event->
                event.title.contains(searchQuery.toString())
            }
            adapter.updateEvents(searchList)
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

                if (parentFragment is UserProfileFragment){
                    requireActivity().findNavController(R.id.fragmentMainContainerView)
                        .navigate(R.id.action_userProfileFragment_to_eventDetailsFragment, bundle, navOptions)
                }else{
                    requireActivity().findNavController(R.id.fragmentMainContainerView)
                        .navigate(R.id.action_enrolledEventsFragment_to_eventDetailsFragment3, bundle, navOptions)
                }

            }, onEnrollButtonClick = {


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

                            viewModel.getUserEnrolledEventsList(userId)


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

    }
}


