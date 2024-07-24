package com.app.eventsync.presentation.authentication_screens.login_screen

import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.app.eventsync.MainActivity
import com.app.eventsync.R
import com.app.eventsync.data.models.User
import com.app.eventsync.databinding.FragmentLoginBinding
import com.app.eventsync.presentation.authentication_screens.signup_screen.viewmodels.AuthenticationState
import com.app.eventsync.presentation.authentication_screens.signup_screen.viewmodels.AuthenticationViewModel
import com.app.eventsync.presentation.authentication_screens.signup_screen.viewmodels.SignInWithGoogleState
import com.app.eventsync.utils.AppUtils
import com.app.eventsync.utils.Constants
import com.app.eventsync.utils.LoadingDialog
import com.app.eventsync.utils.PreferenceManager
import com.app.eventsync.utils.Resource
import com.app.eventsync.utils.ValidationUtil
import com.app.eventsync.utils.showToast
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private lateinit var binding: FragmentLoginBinding
    private val viewModel: AuthenticationViewModel by viewModels()
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var navOptions: NavOptions
    @Inject
    lateinit var preferenceManager: PreferenceManager
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLoginBinding.inflate(layoutInflater)
        auth = FirebaseAuth.getInstance()

        loadingDialog = LoadingDialog(requireContext())

        val oneTapClient = Identity.getSignInClient(requireContext())

        navOptions = NavOptions.Builder()
            .setEnterAnim(R.anim.fade_in)  // specify the enter animation
            .setExitAnim(R.anim.fade_out)         // specify the exit animation
            .setPopEnterAnim(R.anim.fade_in)      // specify the pop enter animation
            .setPopExitAnim(R.anim.fade_out) // specify the pop exit animation
            .build()

        viewModel.authenticationState.observe(viewLifecycleOwner) { value ->

            when (value) {
                is AuthenticationState.Loading -> {
                    loadingDialog.show()
                }

                is AuthenticationState.Success -> {
                    FirebaseFirestore.getInstance().collection("users")
                        .document(auth.currentUser!!.uid)
                        .get().addOnSuccessListener { userObj ->

                            val user: User? = userObj?.toObject(User::class.java)
                            if (user != null) {
                                preferenceManager.saveUserInfoToSharedPreferences(
                                    user.id,
                                    user.name,
                                    user.email,
                                    user.profilePhoto,
                                    false
                                )
                            }

                            requireContext().showToast("Successfully logged in...")
                            loadingDialog.dismiss()
                            moveToMainActivity()

                        }
                }

                is AuthenticationState.Error -> {
                    requireContext().showToast(value.message.toString())
                    loadingDialog.dismiss()
                }
            }

        }

        viewModel.databaseState.observe(viewLifecycleOwner) { value ->

            when (value) {
                is Resource.Loading -> {
                    loadingDialog.show()
                }

                is Resource.Success -> {
                    preferenceManager.setIsUserFirstTime(false)
                    requireContext().showToast("Google sign up success.")
                    loadingDialog.dismiss()
                    moveToMainActivity()
                }

                is Resource.Error -> {
                    requireContext().showToast(value.message.toString())
                    loadingDialog.dismiss()
                }
            }

        }

        binding.loginBtn.setOnClickListener {
            AppUtils.hideKeyboard(requireActivity())
            val email = binding.emailEt.text.toString()
            val password = binding.passEt.text.toString()
            if (ValidationUtil.validateEmail(email)) {
                if (ValidationUtil.validatePassword(password)) {

                    viewModel.signUserWithUserWithEmailAndPassword(
                        email, password
                    )

                } else {
                    requireContext().showToast("Password must contain at least 6 characters.")
                }
            } else {
                requireContext().showToast("Please enter a correct email address.")
            }

        }

        val activityResultLauncher: ActivityResultLauncher<IntentSenderRequest> =
            registerForActivityResult(
                ActivityResultContracts.StartIntentSenderForResult()
            ) { result ->

                try {
                    val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
                    val idToken = credential.googleIdToken

                    if (idToken != null) {
                        val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                        FirebaseAuth.getInstance().signInWithCredential(authCredential)
                            .addOnCompleteListener { task ->

                                val isNewUser = task.result.additionalUserInfo?.isNewUser
                                val userId = task.result.user!!.uid
                                val userName = task.result.user!!.displayName
                                val userEmail = task.result.user!!.email
                                val userPhoto = task.result.user!!.photoUrl.toString()
                                if (isNewUser == true) {
                                    preferenceManager.saveUserInfoToSharedPreferences(
                                        userId,
                                        userName,
                                        userEmail,
                                        userPhoto,
                                        true
                                    )
                                    viewModel.saveUserToDatabase(
                                        userId,
                                        userName!!,
                                        userEmail!!,
                                        userPhoto
                                    )
                                } else {
                                    FirebaseFirestore.getInstance().collection("users")
                                        .document(userId)
                                        .get().addOnCompleteListener {
                                            if (it.isSuccessful) {
                                                val photoUrl =
                                                    it.result.getString("profilePhoto").toString()
                                                preferenceManager.saveUserInfoToSharedPreferences(
                                                    userId,
                                                    userName,
                                                    userEmail,
                                                    photoUrl,
                                                    true
                                                )
                                                moveToMainActivity()
                                                requireContext().showToast("Google sign in success...")
                                            } else {
                                                preferenceManager.saveUserInfoToSharedPreferences(
                                                    userId,
                                                    userName,
                                                    userEmail,
                                                    Constants.DEFAULT_PROFILE_IMG,
                                                    true
                                                )
                                                moveToMainActivity()
                                                requireContext().showToast("Google sign in success...")
                                            }


                                        }

                                }

                            }.addOnFailureListener {
                                loadingDialog.dismiss()
                                it.localizedMessage?.let { it1 -> requireContext().showToast(it1) }
                            }
                    }
                } catch (e: ApiException) {
                    loadingDialog.dismiss()
                    requireContext().showToast("Google sign up cancelled...")
                }


            }

        viewModel.signInWithGoogleState.observe(viewLifecycleOwner) { value ->

            when (value) {
                is SignInWithGoogleState.Loading -> {
                    loadingDialog.show()
                }

                is SignInWithGoogleState.Success -> {
                    try {
                        val intentSenderRequest =
                            value.data?.pendingIntent?.intentSender?.let {
                                IntentSenderRequest.Builder(
                                    it
                                ).build()
                            }
                        activityResultLauncher.launch(intentSenderRequest)
                    } catch (e: IntentSender.SendIntentException) {
                        requireContext().showToast(e.localizedMessage)

                    }
                }

                is SignInWithGoogleState.Error -> {
                    loadingDialog.dismiss()
                    requireContext().showToast(value.message.toString())
                }
            }

        }

        binding.registerWithGoogleBtn.setOnClickListener {
            viewModel.signUpWithGoogle(requireContext())
        }


        binding.forgetPassBtn.setOnClickListener {
            findNavController().navigate(
                R.id.action_loginFragment_to_forgetPasswordFragment,
                null,
                navOptions
            )
        }

        binding.moveToRegisterBtn.setOnClickListener {
            findNavController().navigate(
                R.id.action_loginFragment_to_signUpFragment,
                null,
                navOptions
            )
        }


        return binding.root
    }

    private fun moveToMainActivity() {
        startActivity(Intent(requireContext(), MainActivity::class.java))
        requireActivity().finish()
    }

}