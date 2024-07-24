package com.app.eventsync.presentation.authentication_screens.splash_screen

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.app.eventsync.MainActivity
import com.app.eventsync.R
import com.app.eventsync.utils.AppUtils
import com.app.eventsync.utils.PreferenceManager
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SplashFragment : Fragment() {

    @Inject
    lateinit var preferenceManager: PreferenceManager


    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        if (preferenceManager.getThemeData()) {
            AppUtils.setDarkMode()
        } else {
            AppUtils.setLightMode()

        }

        GlobalScope.launch(Dispatchers.Main) {
            delay(1500)
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                requireContext().startActivity(Intent(requireActivity(), MainActivity::class.java))
                requireActivity().finish()
            } else {
                findNavController().navigate(R.id.action_splashFragment_to_loginFragment)
            }
        }

        return inflater.inflate(R.layout.fragment_splash, container, false)
    }


}