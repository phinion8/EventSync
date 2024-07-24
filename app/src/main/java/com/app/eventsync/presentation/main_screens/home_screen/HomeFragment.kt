package com.app.eventsync.presentation.main_screens.home_screen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.viewpager.widget.ViewPager
import com.app.eventsync.R
import com.app.eventsync.databinding.FragmentHomeBinding
import com.app.eventsync.presentation.main_screens.home_screen.adapters.VPAdapter
import com.app.eventsync.presentation.main_screens.home_screen.events_screens.LaterEventFragment
import com.app.eventsync.presentation.main_screens.home_screen.events_screens.TodayEventFragment
import com.app.eventsync.presentation.main_screens.home_screen.events_screens.UpcomingEventFragment
import com.app.eventsync.presentation.main_screens.home_screen.viewModel.HomeViewModel
import com.app.eventsync.presentation.search_screen.SearchType
import com.app.eventsync.utils.AppUtils
import com.app.eventsync.utils.Constants
import com.app.eventsync.utils.PreferenceManager
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment(), ViewPager.OnPageChangeListener {
    private lateinit var binding: FragmentHomeBinding
    private val viewModel: HomeViewModel by viewModels()
    @Inject
    lateinit var preferenceManager: PreferenceManager
    private var todayListSize: MutableStateFlow<Int> = MutableStateFlow(0)
    private var upcomingListSize: MutableStateFlow<Int> = MutableStateFlow(0)
    private var laterEventSize: MutableStateFlow<Int> = MutableStateFlow(0)


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(layoutInflater)
        binding.tabLayout.setupWithViewPager(binding.viewPager)

        setObservables()
        getToken()

        val vPAdapter = VPAdapter(childFragmentManager)

        vPAdapter.addFragment(TodayEventFragment(), "Today")
        vPAdapter.addFragment(UpcomingEventFragment(), "Upcoming")
        vPAdapter.addFragment(LaterEventFragment(), "Later")

        binding.viewPager.adapter = vPAdapter
        binding.viewPager.addOnPageChangeListener(this)

        Glide.with(requireContext())
            .load(preferenceManager.getUserProfilePic())
            .error(R.drawable.sample_profile_img)
            .into(binding.profileImage)

        binding.tvUserName.text = preferenceManager.getUserName()
        binding.tvGreet.text = getGreetText()
        binding.tvDate.text = AppUtils.getFormattedDate(AppUtils.getTodayDate())
        updateTodayEventText()

        val navOptions = NavOptions.Builder()
            .setEnterAnim(R.anim.fade_in)  // the enter animation
            .setExitAnim(R.anim.fade_out)    // the exit animation
            .setPopEnterAnim(R.anim.fade_in)  // the pop enter animation
            .setPopExitAnim(R.anim.fade_out) // the pop exit animation
            .build()

        binding.addFriendImg.setOnClickListener {
            requireActivity().findNavController(R.id.fragmentMainContainerView)
                .navigate(R.id.action_mainFragment_to_addFriendFragment, null, navOptions)
        }

        binding.createEventBtn.setOnClickListener {

            requireActivity().findNavController(R.id.fragmentMainContainerView)
                .navigate(R.id.action_mainFragment_to_createEventFragment, null, navOptions)
        }

        binding.profileImage.setOnClickListener {
            val bundle = Bundle()
            bundle.putString(Constants.USER_ID, preferenceManager.getUserId())
            requireActivity().findNavController(R.id.fragmentMainContainerView).navigate(
                R.id.action_mainFragment_to_userProfileFragment, bundle, navOptions
            )
        }

        binding.notificationImageView.setOnClickListener {
            requireActivity().findNavController(R.id.fragmentMainContainerView).navigate(
                R.id.action_mainFragment_to_notificationFragment, null, navOptions
            )
        }

        binding.tvSearch.setOnClickListener {
            val bundle = Bundle()
            bundle.putString(Constants.SEARCH_TYPE, SearchType.ALL_EVENTS.name)
            requireActivity().findNavController(R.id.fragmentMainContainerView).navigate(
                R.id.action_mainFragment_to_searchFragment, bundle, navOptions
            )
        }



        return binding.root
    }


    private fun setObservables() {


        viewModel.todayEventListSize.observe(viewLifecycleOwner) {
            todayListSize.value = it
        }

        viewModel.upcomingEventListSize.observe(viewLifecycleOwner) {
            upcomingListSize.value = it
        }

        viewModel.laterEventListSize.observe(viewLifecycleOwner) {
            laterEventSize.value = it
        }



        viewModel.isScrollUp.observe(viewLifecycleOwner) {
            if (it) {
                binding.createEventBtn.visibility = View.GONE
            } else {
                binding.createEventBtn.visibility = View.VISIBLE
            }
        }

    }

    private fun getGreetText(): String {
        val calendar = Calendar.getInstance()
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
        if (hourOfDay in 5..11) {
            return "Good Morning \uD83D\uDC4B"
        } else if (hourOfDay in 12..18) {
            return "Good Afternoon"
        } else if (hourOfDay in 19..23) {
            return "Good Evening \uD83D\uDC4B"
        } else {
            return "Good Evening \uD83D\uDC4B"
        }
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

    }

    override fun onPageSelected(position: Int) {
        when (position) {
            0 -> {
                updateTodayEventText()
                binding.tvDate.text = AppUtils.getFormattedDate(AppUtils.getTodayDate())
            }

            1 -> {
                updateUpcomingEventText()
                binding.tvDate.text = AppUtils.getFormattedDate(AppUtils.getTomorrowDate())
            }

            2 -> {
                updateLaterEventText()
                binding.tvDate.text =
                    "Events After " + AppUtils.getFormattedDate(AppUtils.getTomorrowDate())
            }
        }
    }

    override fun onPageScrollStateChanged(state: Int) {

    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun updateTodayEventText() {
        GlobalScope.launch(Dispatchers.Main) {

            todayListSize.collect {
                binding.tvTodayEvent.text = "Today Events(${it})"
            }
        }

    }

    private fun updateUpcomingEventText() {
        GlobalScope.launch(Dispatchers.Main) {

            upcomingListSize.collect {
                binding.tvTodayEvent.text = "Upcoming Events($it)"
            }
        }

    }

    private fun updateLaterEventText() {
        GlobalScope.launch(Dispatchers.Main) {

            laterEventSize.collect {
                binding.tvTodayEvent.text = "Later Events($it)"
            }
        }

    }

    override fun onResume() {
        super.onResume()
        requireActivity().window.navigationBarColor = resources.getColor(R.color.secondaryColor)
    }

    private fun getToken(){
        FirebaseMessaging.getInstance().token.addOnSuccessListener(this::updateToken)
    }

    private fun updateToken(token: String){

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(preferenceManager.getUserId()!!)
            .update("fcmToken", token)

        preferenceManager.setFCMToken(token)
    }


}


