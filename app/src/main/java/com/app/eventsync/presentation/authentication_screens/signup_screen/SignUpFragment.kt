package com.app.eventsync.presentation.authentication_screens.signup_screen

import android.R
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
import com.app.eventsync.databinding.FragmentSignUpBinding
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
class SignUpFragment : Fragment() {

    private lateinit var binding: FragmentSignUpBinding
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var auth: FirebaseAuth
    @Inject
    lateinit var preferenceManager: PreferenceManager


    private val viewModel: AuthenticationViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentSignUpBinding.inflate(inflater)
        loadingDialog = LoadingDialog(requireContext())
        auth = FirebaseAuth.getInstance()
        val oneTapClient = Identity.getSignInClient(requireContext())


        viewModel.authenticationState.observe(
            viewLifecycleOwner
        ) { value ->
            when (value) {
                is AuthenticationState.Loading -> {
                    loadingDialog.show()
                }

                is AuthenticationState.Success -> {
                    preferenceManager.saveUserInfoToSharedPreferences(
                        auth.currentUser!!.uid,
                        binding.nameEt.text.toString(),
                        binding.emailEt.text.toString(),
                        Constants.DEFAULT_PROFILE_IMG,
                        false
                    )
                    viewModel.saveUserToDatabase(
                        auth.currentUser!!.uid,
                        binding.nameEt.text.toString(),
                        binding.emailEt.text.toString(),
                        ""
                    )
                }

                is AuthenticationState.Error -> {
                    enableRegisterButton(true)
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
                    requireContext().showToast("Account created successfully...")
                    loadingDialog.dismiss()
                    requireActivity().startActivity(
                        Intent(
                            requireContext(),
                            MainActivity::class.java
                        )
                    )
                    requireActivity().finish()
                }

                is Resource.Error -> {
                    enableRegisterButton(true)
                    requireContext().showToast(value.message.toString())
                    loadingDialog.dismiss()
                }
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

                        auth.signInWithCredential(authCredential).addOnCompleteListener { task ->
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

        binding.registerButton.setOnClickListener {
            AppUtils.hideKeyboard(requireActivity())
            val name = binding.nameEt.text.toString()
            val email = binding.emailEt.text.toString()
            val password = binding.passEt.text.toString()

            if (ValidationUtil.validateName(name)) {

                if (ValidationUtil.validateEmail(email)) {

                    if (ValidationUtil.validatePassword(password)) {

                        if (binding.termsCheckbox.isChecked) {
                            enableRegisterButton(false)
                            viewModel.registerUserWithEmailAndPassword(
                                name, email, password
                            )
                        } else {
                            requireContext().showToast("Please accept our terms and conditions to continue.")
                        }
                    } else {
                        requireContext().showToast("Password must contain at least 6 characters.")
                    }
                } else {
                    requireContext().showToast("Please enter a valid email.")
                }

            } else {
                requireContext().showToast("Please enter a valid name")
            }

        }


        binding.moveToLoginBtn.setOnClickListener {
            val navOptions = NavOptions.Builder()
                .setEnterAnim(com.app.eventsync.R.anim.fade_in)  // the enter animation
                .setExitAnim(com.app.eventsync.R.anim.fade_out)    // the exit animation
                .setPopEnterAnim(com.app.eventsync.R.anim.fade_in)  // the pop enter animation
                .setPopExitAnim(com.app.eventsync.R.anim.fade_out) // the pop exit animation
                .build()

            findNavController().navigate(
                com.app.eventsync.R.id.action_signUpFragment_to_loginFragment,
                null,
                navOptions
            )
        }



        binding.signUpWithGoogleBtn.setOnClickListener {
            viewModel.signUpWithGoogle(requireContext())
        }

        binding.backBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }


        return binding.root


    }

    private fun enableRegisterButton(enabled: Boolean) {
        binding.registerButton.isEnabled = enabled
    }

    private fun moveToMainActivity() {
        startActivity(Intent(requireContext(), MainActivity::class.java))
        requireActivity().finish()
    }


}