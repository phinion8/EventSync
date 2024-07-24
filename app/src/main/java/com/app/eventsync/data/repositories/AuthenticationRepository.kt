package com.app.eventsync.data.repositories

import com.app.eventsync.presentation.authentication_screens.signup_screen.viewmodels.AuthenticationState
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.scopes.ActivityScoped
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

@ActivityScoped
class AuthenticationRepository @Inject constructor(

) {

    fun signUpUserWithEmailAndPassword(name: String, email: String, password: String): Flow<AuthenticationState> = callbackFlow {
        trySend(AuthenticationState.Loading()).isSuccess
        val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    trySend(AuthenticationState.Success("Account created successfully...")).isSuccess
                } else {
                    trySend(AuthenticationState.Error("User creation failed: ${task.exception?.message}")).isSuccess
                }

            }

        awaitClose {
            channel.close()
        }
    }

    fun loginUserWithEmailAndPassword(email: String, password: String) :Flow<AuthenticationState> = callbackFlow{
        trySend(AuthenticationState.Loading())
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password).addOnCompleteListener {task->

            if (task.isSuccessful) {
                trySend(AuthenticationState.Success("Login Success...")).isSuccess
            } else {
                trySend(AuthenticationState.Error("${task.exception?.message}")).isSuccess
            }

        }

        awaitClose {
            channel.close()
        }
    }

    fun forgetPasswordUsingEmail(email: String): Flow<AuthenticationState> = callbackFlow{

        trySend(AuthenticationState.Loading())

        FirebaseAuth.getInstance().sendPasswordResetEmail(email).addOnCompleteListener {task->

            if (task.isSuccessful){
                trySend(AuthenticationState.Success("Forget password sent successfully..."))
            }else{
                task.exception?.localizedMessage?.let { AuthenticationState.Error(it) }
                    ?.let { trySend(it) }
            }

        }

        awaitClose{
            channel.close()
        }

    }



}