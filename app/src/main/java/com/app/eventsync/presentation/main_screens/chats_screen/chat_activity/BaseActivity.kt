package com.app.eventsync.presentation.main_screens.chats_screen.chat_activity

import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import com.app.eventsync.utils.Constants
import com.app.eventsync.utils.PreferenceManager
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
open class BaseActivity: AppCompatActivity() {

    @Inject
    lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        preferenceManager.getUserId()
            ?.let { FirebaseFirestore.getInstance().collection("users").document(it).update(Constants.KEY_AVAILABILITY, 1) }
    }

    override fun onStart() {
        super.onStart()
        preferenceManager.getUserId()
            ?.let { FirebaseFirestore.getInstance().collection("users").document(it).update(Constants.KEY_AVAILABILITY, 1) }
    }

    override fun onPause() {
        super.onPause()
        preferenceManager.getUserId()
            ?.let { FirebaseFirestore.getInstance().collection("users").document(it).update(Constants.KEY_AVAILABILITY, 0) }
    }

    override fun onResume() {
        super.onResume()
        preferenceManager.getUserId()
            ?.let { FirebaseFirestore.getInstance().collection("users").document(it).update(Constants.KEY_AVAILABILITY, 1) }
    }

    override fun onStop() {
        super.onStop()
        preferenceManager.getUserId()
            ?.let { FirebaseFirestore.getInstance().collection("users").document(it).update(Constants.KEY_AVAILABILITY, 0) }
    }

}