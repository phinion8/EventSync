package com.app.eventsync.presentation.search_screen

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.eventsync.R
import com.app.eventsync.data.models.Event
import com.app.eventsync.data.notification.AlarmSchedulerImp
import com.app.eventsync.data.repositories.DatabaseState
import com.app.eventsync.databinding.FragmentSearchBinding
import com.app.eventsync.presentation.main_screens.home_screen.adapters.EventsListAdapter
import com.app.eventsync.presentation.main_screens.home_screen.events_screens.EventItemListener
import com.app.eventsync.presentation.main_screens.home_screen.viewModel.HomeViewModel
import com.app.eventsync.utils.AppUtils
import com.app.eventsync.utils.Constants
import com.app.eventsync.utils.EventPreviewDialog
import com.app.eventsync.utils.LoadingDialog
import com.app.eventsync.utils.PreferenceManager
import com.app.eventsync.utils.SuccessDialog
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
class SearchFragment : Fragment(), EventItemListener {

    private lateinit var binding: FragmentSearchBinding
    private lateinit var eventsListAdapter: EventsListAdapter
    private val eventList: ArrayList<Event> = ArrayList()
    private lateinit var loadingDialog: LoadingDialog
    private val homeViewModel: HomeViewModel by viewModels()
    private lateinit var alarmSchedulerImp: AlarmSchedulerImp
    private var isEnrollEventStateObserved: MutableStateFlow<Boolean> = MutableStateFlow(false)

    @Inject
    lateinit var preferenceManager: PreferenceManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSearchBinding.inflate(layoutInflater, container, false)
        loadingDialog = LoadingDialog(requireContext())
        alarmSchedulerImp = AlarmSchedulerImp(requireContext())

        val searchType = arguments?.getString(Constants.SEARCH_TYPE)

        eventsListAdapter = EventsListAdapter(requireContext(),this, false)

        when(searchType){
            SearchType.ALL_EVENTS.name->{

                setToolBarTitle("Search Events")

            }
            SearchType.CREATED_EVENTS.name->{

                setToolBarTitle("Search Created Events")

            }
            SearchType.ENROLLED_EVENTS.name->{



            }
            SearchType.FRIEND_LIST.name->{

            }
            SearchType.REQUEST_LIST.name->{

            }
        }


        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                AppUtils.hideKeyboard(requireActivity())
                binding.noDataAnim.visibility = View.GONE
                binding.noDataText.visibility = View.GONE
                performSearch(binding.etSearch.text.toString())
                true
            } else {
                false
            }
        }


        binding.rvSearch.adapter = eventsListAdapter
        binding.rvSearch.layoutManager = LinearLayoutManager(requireContext())

        binding.backBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        return binding.root
    }


    private fun performSearch(searchQuery: String) {
        loadingDialog.show()
        FirebaseFirestore.getInstance().collection("all_events")
            .whereGreaterThanOrEqualTo("title", searchQuery)
            .whereLessThanOrEqualTo("title", searchQuery + "\uf7ff")
            .get().addOnSuccessListener { querySnapshot ->

                eventList.clear()
                loadingDialog.dismiss()
                for (snapshot in querySnapshot.documents) {
                    val event: Event? = snapshot.toObject(Event::class.java)
                    if (event != null) {
                        eventList.add(event)
                    }
                }

                eventsListAdapter.updateEvents(eventList)
                if (eventsListAdapter.isEventListEmpty()) {
                    binding.noDataAnim.visibility = View.VISIBLE
                    binding.noDataText.visibility = View.VISIBLE
                } else {
                    binding.noDataAnim.visibility = View.GONE
                    binding.noDataText.visibility = View.GONE
                }

            }

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
                    .navigate(
                        R.id.action_searchFragment_to_eventDetailsFragment,
                        bundle,
                        navOptions
                    )


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
                    homeViewModel.cancelUserEvent(
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

    }

    private fun setToolBarTitle(title: String){
        binding.tvSearchTitle.text = title
    }
}

enum class SearchType{
    ALL_EVENTS,
    CREATED_EVENTS,
    ENROLLED_EVENTS,
    FRIEND_LIST,
    REQUEST_LIST,
}