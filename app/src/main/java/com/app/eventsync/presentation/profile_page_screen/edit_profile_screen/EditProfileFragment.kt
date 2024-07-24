package com.app.eventsync.presentation.profile_page_screen.edit_profile_screen

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.viewModels
import com.app.eventsync.data.models.User
import com.app.eventsync.data.repositories.DatabaseState
import com.app.eventsync.databinding.FragmentEditProfileBinding
import com.app.eventsync.presentation.profile_page_screen.viewModel.UserProfileViewModel
import com.app.eventsync.utils.AppUtils
import com.app.eventsync.utils.Constants
import com.app.eventsync.utils.LoadingDialog
import com.app.eventsync.utils.PhotoChooserDialog
import com.app.eventsync.utils.PreferenceManager
import com.app.eventsync.utils.Resource
import com.app.eventsync.utils.showToast
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class EditProfileFragment : Fragment() {

    private lateinit var binding: FragmentEditProfileBinding
    private val viewModel: UserProfileViewModel by viewModels()
    @Inject
    lateinit var preferenceManager: PreferenceManager
    private lateinit var loadingDialog: LoadingDialog
    private var photoUri: Uri? = null
    private var profilePhotoUrl: String? = null
    private var currentProfilePhotoUrl: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentEditProfileBinding.inflate(layoutInflater, container, false)
        loadingDialog = LoadingDialog(requireContext())


        val takePhotoContract = registerForActivityResult(ActivityResultContracts.TakePicture()) {
            Glide.with(requireContext())
                .load(photoUri)
                .into(binding.profileImage)
        }

        if (savedInstanceState == null) {
            viewModel.getUserDetails(preferenceManager.getUserId()!!)
        }

        viewModel.userDetailsState.observe(viewLifecycleOwner) { value ->

            when (value) {
                is DatabaseState.Loading -> {
                    loadingDialog.show()
                }

                is DatabaseState.Success -> {

                    currentProfilePhotoUrl = value.data!!.profilePhoto
                    updateProfileViews(value.data)
                    loadingDialog.dismiss()


                }

                is DatabaseState.Error -> {
                    loadingDialog.dismiss()
                    requireContext().showToast(value.message!!)
                }
            }

        }


        viewModel.uploadImageState.observe(viewLifecycleOwner) { value ->

            when (value) {
                is DatabaseState.Loading -> {
                    loadingDialog.show()
                }

                is DatabaseState.Success -> {

                    val userId = preferenceManager.getUserId()!!
                    val name = binding.nameEt.text.toString()
                    val photoUrl = value.data
                    profilePhotoUrl = photoUrl
                    if (photoUrl != null) {
                        viewModel.updateProfileDetails(userId, name, photoUrl)
                    }

                    loadingDialog.dismiss()


                }

                is DatabaseState.Error -> {
                    loadingDialog.dismiss()
                    requireContext().showToast(value.message!!)
                }
            }

        }

        viewModel.updateProfileState.observe(viewLifecycleOwner){value->

            when (value) {
                is Resource.Loading -> {
                    loadingDialog.show()
                }

                is Resource.Success -> {

                    val name = binding.nameEt.text.toString()
                    if (profilePhotoUrl == null || photoUri == null){
                        preferenceManager.editUserInfoInSharePreferences(name, currentProfilePhotoUrl!!)
                    }else{
                        profilePhotoUrl?.let {
                            preferenceManager.editUserInfoInSharePreferences(name,
                                it
                            )
                    }

                    }
                    requireContext().showToast("Profile updated successfully.")
                    loadingDialog.dismiss()


                }

                is Resource.Error -> {
                    loadingDialog.dismiss()
                    requireContext().showToast(value.message!!)
                }
            }

        }

        binding.changeProfilePhotoBtn.setOnClickListener {

            if (AppUtils.checkCameraAndGalleryPermissions(requireContext())) {
                PhotoChooserDialog(
                    requireContext(),
                    cameraOnClick = {
                        photoUri = createImageUri()
                        takePhotoContract.launch(photoUri)
                    },
                    galleryOnClick = {
                        invokeAttachFileIntent(Constants.ATTACHMENT_CHOICE_CHOOSE_GALLERY)
                    }
                ).showPhotoPickerDialog()
            } else {
                AppUtils.requestCameraAndGalleryPermissions(
                    requireActivity(),
                    Constants.CAMERA_AND_GALLERY_PERMISSION_CODE
                )
            }


        }

        binding.updateBtn.setOnClickListener {

            if (photoUri != null){

                val userId = preferenceManager.getUserId()!!
                photoUri?.let { it1 -> viewModel.uploadProfilePhoto(userId, it1) }


            }else{
                //Changing only name
                val name = binding.nameEt.text.toString()
                viewModel.updateProfileDetails(preferenceManager.getUserId()!!, name, currentProfilePhotoUrl!!)
            }


        }

        binding.backBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.copyUserIdBtn.setOnClickListener {
            copyTextToClipboard("User ID", binding.tvUserId.text.toString())
        }

        binding.copyEmailBtn.setOnClickListener {
            copyTextToClipboard("Email", binding.tvUserEmail.text.toString())
        }

        return binding.root
    }

    private fun createImageUri(): Uri? {
        val image = File(requireContext().filesDir, "camera_photo.png")
        return FileProvider.getUriForFile(
            requireContext(),
            "com.app.eventsync.fileProvider",
            image
        )

    }

    private fun invokeAttachFileIntent(attachmentChoice: Int) {
        when (attachmentChoice) {
            Constants.ATTACHMENT_CHOICE_CHOOSE_GALLERY -> {
                val galleryIntent =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                galleryLauncher.launch(galleryIntent)
                return
            }

        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val selectedImageUri: Uri? = data?.data
            photoUri = selectedImageUri
            Glide.with(requireContext())
                .load(selectedImageUri)
                .into(binding.profileImage)
        }
    }

    private fun updateProfileViews(user: User) {
        with(binding) {
            nameEt.setText(user.name)
            tvUserId.text = user.id
            tvUserEmail.text = user.email
            Glide.with(requireContext())
                .load(user.profilePhoto)
                .error(Constants.DEFAULT_PROFILE_IMG)
                .into(profileImage)
        }
    }

    private fun copyTextToClipboard(label: String, text: String){
        val clipboard: ClipboardManager = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        requireContext().showToast("$label copied to clipboard")
    }


}