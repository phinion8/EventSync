package com.app.eventsync.presentation.authentication_screens.forget_password_screen

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.app.eventsync.databinding.FragmentForgetPasswordScreenBinding
import com.app.eventsync.presentation.authentication_screens.signup_screen.viewmodels.AuthenticationState
import com.app.eventsync.presentation.authentication_screens.signup_screen.viewmodels.AuthenticationViewModel
import com.app.eventsync.utils.LoadingDialog
import com.app.eventsync.utils.ValidationUtil
import com.app.eventsync.utils.showToast
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForgetPasswordFragment : Fragment() {

    private lateinit var binding: FragmentForgetPasswordScreenBinding

    private val viewModel: AuthenticationViewModel by viewModels()

    private lateinit var loadingDialog: LoadingDialog


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentForgetPasswordScreenBinding.inflate(layoutInflater)

        loadingDialog = LoadingDialog(requireContext())


        viewModel.forgetPassState.observe(viewLifecycleOwner){value->

            when(value){
                is AuthenticationState.Loading->{
                    loadingDialog.show()
                }
                is AuthenticationState.Success->{

                    loadingDialog.dismiss()
                    requireContext().showToast(value.message.toString())

                }
                is AuthenticationState.Error->{

                    loadingDialog.dismiss()
                    requireContext().showToast(value.message.toString())

                }
            }

        }


        binding.submitBtn.setOnClickListener {
            val email = binding.emailEt.text.toString().trim()
            val isValidEmail = ValidationUtil.validateEmail(email)
            if (isValidEmail){
                viewModel.forgetPasswordUsingEmail(email)
            }else{
                requireContext().showToast("Please enter a valid email address...")
            }
        }



        return binding.root
    }

}