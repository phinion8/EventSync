package com.app.eventsync.presentation.main_screens.profile_screen

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.eventsync.NotificationPermissionListener
import com.app.eventsync.R
import com.app.eventsync.data.models.SettingItem
import com.app.eventsync.databinding.FragmentProfileBinding
import com.app.eventsync.presentation.authentication_screens.RegistrationActivity
import com.app.eventsync.presentation.main_screens.profile_screen.adapters.SettingListAdapter
import com.app.eventsync.utils.AppUtils
import com.app.eventsync.utils.Constants
import com.app.eventsync.utils.ErrorDialog
import com.app.eventsync.utils.PreferenceManager
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.identity.Identity
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : Fragment(), SettingItemOnClickListener, SettingItemSwitchChangeListener {

    private lateinit var binding: FragmentProfileBinding
    private lateinit var accountSettingAdapter: SettingListAdapter
    private lateinit var appSettingsAdapter: SettingListAdapter
    @Inject
    lateinit var preferenceManager: PreferenceManager
    private var notificationPermissionListener: NotificationPermissionListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProfileBinding.inflate(layoutInflater)

        Glide.with(requireContext())
            .load(preferenceManager.getUserProfilePic())
            .error(Constants.DEFAULT_PROFILE_IMG)
            .into(binding.profileImage)

        binding.tvProfileEmail.text = preferenceManager.getUserEmail()
        binding.tvProfileName.text = preferenceManager.getUserName()


        accountSettingAdapter = SettingListAdapter(
            listOf(
                SettingItem(
                    SettingType.EDIT_PROFILE,
                    "Edit Profile",
                    R.drawable.ic_edit,
                    "Edit Your Profile"
                ),
                SettingItem(
                    SettingType.CREATED_EVENT,
                    "Your Created Events",
                    R.drawable.ic_event,
                    "Checkout your created events."
                ),
                SettingItem(
                    SettingType.ENROLLED_EVENT,
                    "Your Enrolled Events",
                    R.drawable.ic_enrolled_event,
                    "Checkout your enrolled events."
                ),
                SettingItem(
                    SettingType.LOG_OUT,
                    "Log Out",
                    R.drawable.ic_log_out,
                    "Log out from your account."
                ),

                ),
            this,
            this,
            preferenceManager
        )

        appSettingsAdapter = SettingListAdapter(
            listOf(
                SettingItem(
                    SettingType.NOTIFICATION,
                    "Notifications",
                    R.drawable.ic_notification,
                    "Turn on mobile notification to receive updated.",
                    showSwitch = true
                ),
                SettingItem(
                    SettingType.THEME,
                    "Dark Mode",
                    R.drawable.ic_theme,
                    "Turn on dark mode",
                    showSwitch = true
                ),
                SettingItem(
                    SettingType.SHOW_BACKGROUND_IMAGES,
                    "Show Images",
                    R.drawable.ic_gallery,
                    "Show background images in the event list",
                    showSwitch = true
                )
            ),
            this,
            this,
            preferenceManager
        )

        binding.editProfileImg.setOnClickListener {
            requireActivity().findNavController(R.id.fragmentMainContainerView)
                .navigate(R.id.action_mainFragment_to_editProfileFragment)
        }

        binding.rvSettings.adapter = accountSettingAdapter
        binding.rvSettings.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAppSettings.adapter = appSettingsAdapter
        binding.rvAppSettings.layoutManager = LinearLayoutManager(requireContext())





        return binding.root
    }

    override fun onSettingItemClick(id: SettingType) {
        when (id) {
            SettingType.EDIT_PROFILE -> {

                requireActivity().findNavController(R.id.fragmentMainContainerView)
                    .navigate(R.id.action_mainFragment_to_editProfileFragment)

            }

            SettingType.CREATED_EVENT -> {

                val bundle = Bundle()
                bundle.putString(Constants.USER_ID, preferenceManager.getUserId())
                bundle.putBoolean(Constants.IS_PROFILE, false)

                requireActivity().findNavController(R.id.fragmentMainContainerView)
                    .navigate(R.id.action_mainFragment_to_userCreatedEventFragment, bundle)
            }

            SettingType.ENROLLED_EVENT -> {

                val bundle = Bundle()
                bundle.putString(Constants.USER_ID, preferenceManager.getUserId())
                bundle.putBoolean(Constants.IS_PROFILE, false)

                requireActivity().findNavController(R.id.fragmentMainContainerView)
                    .navigate(R.id.action_mainFragment_to_enrolledEventsFragment, bundle)


            }

            SettingType.LOG_OUT -> {
                ErrorDialog(
                    requireContext(),
                    title = "Are you sure you want to logout?",
                    description = "You won't be able to receive updates about upcoming event but you can log in back anytime.",
                    onPositiveButtonClick = {
                        if (preferenceManager.isGoogleAccount()) {
                            val oneTapClient = Identity.getSignInClient(requireContext())
                            oneTapClient.signOut()
                        }
                        val user = FirebaseAuth.getInstance()
                        user.signOut()
                        preferenceManager.clearSharedPreferences()
                        moveToRegistrationActivity()
                    }).showErrorDialog()

            }

            else -> {

            }
        }
    }

    private fun moveToRegistrationActivity() {
        startActivity(Intent(requireActivity(), RegistrationActivity::class.java))
        requireActivity().finish()
    }

    override fun onSettingItemSwitchChecked(settingType: SettingType, isChecked: Boolean) {

        when (settingType) {
            SettingType.NOTIFICATION -> {
                if (isChecked) {

                    notificationPermissionListener?.onNotificationPermissionListenerFromProfileFragment()

                }
            }

            SettingType.THEME -> {
                if (isChecked) {
                    AppUtils.setDarkMode()
                    preferenceManager.setThemeData(true)
                } else {
                    AppUtils.setLightMode()
                    preferenceManager.setThemeData(false)
                }
            }

            SettingType.SHOW_BACKGROUND_IMAGES -> {
                if (isChecked) {
                    preferenceManager.setAllowBackgroundImage(true)
                } else {
                    preferenceManager.setAllowBackgroundImage(false)
                }
            }

            else -> {}
        }

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is NotificationPermissionListener) {
            notificationPermissionListener = context
        }
    }


}

interface SettingItemOnClickListener {

    fun onSettingItemClick(id: SettingType)

}

interface SettingItemSwitchChangeListener {
    fun onSettingItemSwitchChecked(settingType: SettingType, isChecked: Boolean)
}

enum class SettingType {
    EDIT_PROFILE,
    CREATED_EVENT,
    ENROLLED_EVENT,
    NOTIFICATION,
    THEME,
    LOG_OUT,
    SHOW_BACKGROUND_IMAGES
}