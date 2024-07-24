package com.app.eventsync.presentation.authentication_screens

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.app.eventsync.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegistrationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)
    }
}