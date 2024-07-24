package com.app.eventsync.presentation.event_screens.event_detail_screen.event_attachments

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.eventsync.data.repositories.DatabaseState
import com.app.eventsync.databinding.FragmentCreateEventAttachmentsBinding
import com.app.eventsync.presentation.event_screens.create_event_screen.viewModel.CreateEventViewModel
import com.app.eventsync.presentation.event_screens.event_detail_screen.event_attachments.adapter.ChooseMediaAdapter
import com.app.eventsync.presentation.event_screens.event_detail_screen.event_attachments.adapter.RemoveImageCallback
import com.app.eventsync.utils.Constants
import com.app.eventsync.utils.LoadingDialog
import com.app.eventsync.utils.PhotoChooserDialog
import com.app.eventsync.utils.Resource
import com.app.eventsync.utils.showToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class CreateEventAttachmentsFragment : Fragment(), RemoveImageCallback {

    private lateinit var binding: FragmentCreateEventAttachmentsBinding
    private lateinit var adapter: ChooseMediaAdapter
    private val viewModel: CreateEventViewModel by viewModels(
        ownerProducer = {requireParentFragment()}
    )
    private lateinit var imageUri: Uri
    private val contract = registerForActivityResult(ActivityResultContracts.TakePicture()) {
        viewModel.mediaUriList.add(imageUri.toString())
        adapter.notifyDataSetChanged()
        if (adapter.itemCount > 0) {
            binding.noMediaAnim.visibility = View.GONE
            binding.noMediaText.visibility = View.GONE
        }
    }
    private lateinit var loadingDialog: LoadingDialog
    val requestCode = 1000


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCreateEventAttachmentsBinding.inflate(layoutInflater, container, false)
        adapter = ChooseMediaAdapter(requireContext(), viewModel.mediaUriList, this)
        loadingDialog = LoadingDialog(requireContext())

        CoroutineScope(Dispatchers.Main).launch {
            viewModel.isEditMode.collect{
                if (it){
                    viewModel.eventDetailsState.observe(viewLifecycleOwner) { value ->
                        when (value) {
                            is DatabaseState.Loading -> {
                            }

                            is DatabaseState.Success -> {
                                viewModel.mediaUriList.addAll(value.data!!.imageList)
                                adapter.notifyDataSetChanged()
                                if (adapter.itemCount <= 0) {
                                    binding.noMediaAnim.visibility = View.VISIBLE
                                    binding.noMediaText.visibility = View.VISIBLE
                                }else{
                                    binding.noMediaAnim.visibility = View.GONE
                                    binding.noMediaText.visibility = View.GONE
                                }

                            }

                            is DatabaseState.Error -> {
                                requireContext().showToast(value.message.toString())
                            }

                        }

                    }
                }
            }
        }

        binding.addMediaBtn.setOnClickListener {
            PhotoChooserDialog(requireContext(), cameraOnClick = {

                imageUri = createImageUri()!!
                contract.launch(imageUri)

            }, galleryOnClick = {

                invokeAttachFileIntent(Constants.ATTACHMENT_CHOICE_CHOOSE_GALLERY)

            }).showPhotoPickerDialog()
        }


        val permission = Manifest.permission.READ_EXTERNAL_STORAGE
        val cameraPermission = Manifest.permission.CAMERA
        val granted = PackageManager.PERMISSION_GRANTED

        if (ContextCompat.checkSelfPermission(requireContext(), permission) != granted) {
            if (ContextCompat.checkSelfPermission(requireContext(), cameraPermission) != granted) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(permission, cameraPermission),
                    requestCode
                )
            }
            // Permission is not granted; request it
        } else {

        }

        viewModel.removeImageFromEventState.observe(viewLifecycleOwner){value->

            when(value){
                is Resource.Loading->{
                    loadingDialog.show()
                    requireContext().showToast("Deleting image...")
                }
                is Resource.Success->{

                    loadingDialog.dismiss()
                    requireContext().showToast("Deleted successfully.")

                }
                is Resource.Error->{
                    loadingDialog.dismiss()
                    requireContext().showToast("Something went wrong.")
                }
            }

        }


        binding.rvMedia.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvMedia.adapter = adapter

        return binding.root
    }

    override fun removeImageFromList(position: Int) {
        lifecycleScope.launch {
            viewModel.isEditMode.collect{
                if (it){
                    viewModel.removeImageFromEvent(viewModel.mediaUriList[position], viewModel.eventId.value)
                    viewModel.mediaUriList.removeAt(position)
                    adapter.notifyDataSetChanged()
                    if (adapter.itemCount <= 0) {
                        binding.noMediaAnim.visibility = View.VISIBLE
                        binding.noMediaText.visibility = View.VISIBLE
                    }
                }else{
                    viewModel.mediaUriList.removeAt(position)
                    adapter.notifyDataSetChanged()
                    if (adapter.itemCount <= 0) {
                        binding.noMediaAnim.visibility = View.VISIBLE
                        binding.noMediaText.visibility = View.VISIBLE
                    }
                }
            }
        }

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
            viewModel.mediaUriList.add(selectedImageUri.toString())
            adapter.notifyDataSetChanged()
            if (adapter.itemCount > 0) {
                binding.noMediaText.visibility = View.GONE
                binding.noMediaAnim.visibility = View.GONE
            }
        }
    }

}