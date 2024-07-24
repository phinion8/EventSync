package com.app.eventsync

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.app.eventsync.databinding.ActivityMainBinding
import com.app.eventsync.utils.AppUtils
import com.app.eventsync.utils.Constants
import com.app.eventsync.utils.ErrorDialog
import com.app.eventsync.utils.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), NotificationPermissionListener {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        if (AppUtils.isNotificationPermissionGranted(this)) {
            PreferenceManager(this).setNotificationAllowed(true)
        }else{
            PreferenceManager(this).setNotificationAllowed(false)
            requestNotificationPermission()
        }

    }

    private fun requestNotificationPermission() {
        requestPermissions(
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            Constants.NOTIFICATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onBackPressed() {
        super.onBackPressed()
        moveTaskToBack(true)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                PreferenceManager(this).setNotificationAllowed(true)
            } else {
                ErrorDialog(
                    this,
                    title = "Notification permissions is required.",
                    description = "You won't be able to receive updates about upcoming event turn it on from settings?",
                    onPositiveButtonClick = {
                        AppUtils.openAppSettings(this)
                    }).showErrorDialog()
            }
        }
    }

    override fun onNotificationPermissionListenerFromProfileFragment() {
        requestNotificationPermission()
    }

}

interface NotificationPermissionListener {
    fun onNotificationPermissionListenerFromProfileFragment()
}