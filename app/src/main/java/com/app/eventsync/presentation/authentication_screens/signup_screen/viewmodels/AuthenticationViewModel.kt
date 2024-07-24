package com.app.eventsync.presentation.authentication_screens.signup_screen.viewmodels

import android.content.Context
import android.content.IntentSender
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.eventsync.BuildConfig
import com.app.eventsync.data.models.User
import com.app.eventsync.data.repositories.FirebaseDatabaseRepository
import com.app.eventsync.data.repositories.AuthenticationRepository
import com.app.eventsync.utils.Resource
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.BeginSignInResult
import com.google.android.gms.auth.api.identity.Identity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthenticationViewModel @Inject constructor(
    private val registrationRepository: AuthenticationRepository,
    private val databaseRepository: FirebaseDatabaseRepository
) : ViewModel() {

    private val _authenticationState: MutableLiveData<AuthenticationState> = MutableLiveData()
    val authenticationState: LiveData<AuthenticationState> = _authenticationState

    private val _databaseState: MutableLiveData<Resource> = MutableLiveData()
    val databaseState: LiveData<Resource> = _databaseState

    private val _forgetPassState: MutableLiveData<AuthenticationState> = MutableLiveData()
    val forgetPassState: LiveData<AuthenticationState> = _forgetPassState

    private val _signInWithGoogleState: MutableLiveData<SignInWithGoogleState<BeginSignInResult>> =
        MutableLiveData()
    val signInWithGoogleState: LiveData<SignInWithGoogleState<BeginSignInResult>> =
        _signInWithGoogleState


    fun registerUserWithEmailAndPassword(name: String, email: String, password: String) {
        viewModelScope.launch {
            registrationRepository.signUpUserWithEmailAndPassword(name, email, password).collect {
                _authenticationState.value = it
            }

        }
    }

    fun signUserWithUserWithEmailAndPassword(email: String, password: String) {
        viewModelScope.launch {
            registrationRepository.loginUserWithEmailAndPassword(email, password).collect {
                _authenticationState.value = it
            }
        }
    }

    fun saveUserToDatabase(userId: String, name: String, email: String, profilePhoto: String = "") {
        viewModelScope.launch {
            databaseRepository.saveUserToTheDatabase(User(userId, name, email, profilePhoto)).collect {
                _databaseState.value = it
            }
        }
    }

    fun forgetPasswordUsingEmail(email: String) {
        viewModelScope.launch {
            registrationRepository.forgetPasswordUsingEmail(email).collect {
                _forgetPassState.value = it
            }
        }
    }

    fun signUpWithGoogle(
        context: Context
    ) {

        _signInWithGoogleState.value = SignInWithGoogleState.Loading()
        val oneTapClient = Identity.getSignInClient(context)
        val signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(BuildConfig.WEB_CLIENT_ID)
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .build()


        oneTapClient.beginSignIn(signInRequest).addOnSuccessListener { task ->

            try {
                _signInWithGoogleState.value = SignInWithGoogleState.Success(task, "Success")
            } catch (e: IntentSender.SendIntentException) {

                _signInWithGoogleState.value =
                    SignInWithGoogleState.Error("Something went wrong...")

            }

        }.addOnFailureListener {
            _signInWithGoogleState.value = SignInWithGoogleState.Error(it.localizedMessage)
        }
    }


}

sealed class AuthenticationState(val message: String? = null) {
    class Success(message: String) : AuthenticationState(message)
    class Loading() : AuthenticationState()
    class Error(errorMessage: String) : AuthenticationState(errorMessage)
}

sealed class SignInWithGoogleState<T>(val data: T? = null, val message: String? = null) {
    class Loading<T>() : SignInWithGoogleState<T>()
    class Success<T>(data: T?, message: String?) : SignInWithGoogleState<T>(data, message)
    class Error<T>(message: String?) : SignInWithGoogleState<T>(null, message)
}