package com.app.eventsync.presentation.main_screens.home_screen.events_screens

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.eventsync.R
import com.app.eventsync.data.models.Event
import com.app.eventsync.data.notification.AlarmSchedulerImp
import com.app.eventsync.data.repositories.DatabaseState
import com.app.eventsync.databinding.FragmentUpcomingEventBinding
import com.app.eventsync.presentation.main_screens.home_screen.adapters.EventsListAdapter
import com.app.eventsync.presentation.main_screens.home_screen.viewModel.HomeViewModel
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
class UpcomingEventFragment : Fragment(), EventItemListener {

    private lateinit var binding: FragmentUpcomingEventBinding
    private lateinit var adapter: EventsListAdapter
    @Inject
    lateinit var preferenceManager: PreferenceManager
    private lateinit var alarmSchedulerImp: AlarmSchedulerImp
    private lateinit var loadingDialog: LoadingDialog
    private var isEnrollEventStateObserved: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val viewModel: HomeViewModel by viewModels(
        ownerProducer = {
            requireParentFragment()
        }
    )


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        alarmSchedulerImp = AlarmSchedulerImp(requireContext())
        loadingDialog = LoadingDialog(requireContext())
        binding = FragmentUpcomingEventBinding.inflate(layoutInflater)
        adapter = EventsListAdapter(requireContext(),this)




        viewModel.getUpcomingDataList().observe(viewLifecycleOwner) { value ->

            when (value) {
                is DatabaseState.Loading -> {

                }

                is DatabaseState.Success -> {
                    viewModel.setUpcomingEventListSize(value.data!!.size)
                    value.data.let { adapter.updateEvents(it) }
                    checkForNoData()
                }

                is DatabaseState.Error -> {

                }
            }


        }


        binding.rvEvent.adapter = adapter
        binding.rvEvent.layoutManager = LinearLayoutManager(requireContext())


        binding.rvEvent.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0) {

                    viewModel.setIsScrollUp(true)
                    Log.i("SCROLLING", "DOWN");

                } else if (dy < 0) {
                    viewModel.setIsScrollUp(false)
                    Log.i("SCROLLING", "UP");
                }
            }

        })

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

                requireActivity().findNavController(R.id.fragmentMainContainerView)
                    .navigate(R.id.action_mainFragment_to_eventDetailsFragment, bundle, navOptions)


            }, onEnrollButtonClick = {

                CoroutineScope(Dispatchers.Main).launch {
                    isEnrollEventStateObserved.value = false
                    viewModel.enrollUserToEvent(
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
                                if (!isEnrollEventStateObserved.value) {
                                    isEnrollEventStateObserved.value = true
                                    requireContext().showToast("Success")
                                    alarmSchedulerImp.schedule(value.data!!)
                                    SuccessDialog(
                                        requireContext(),
                                        "Successfully Enrolled..",
                                        "You can check your enrolled event by going to enrolled events section."
                                    )
                                        .showSuccessDialog()
                                }

                            }

                            is DatabaseState.Error -> {
                                loadingDialog.dismiss()
                                if (!isEnrollEventStateObserved.value) {
                                    isEnrollEventStateObserved.value = true
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
        TODO("Not yet implemented")
    }


}