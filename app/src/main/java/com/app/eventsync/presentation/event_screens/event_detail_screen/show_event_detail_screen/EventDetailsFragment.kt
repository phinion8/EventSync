package com.app.eventsync.presentation.event_screens.event_detail_screen.show_event_detail_screen

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
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.eventsync.R
import com.app.eventsync.data.models.EventImage
import com.app.eventsync.data.notification.AlarmSchedulerImp
import com.app.eventsync.data.repositories.DatabaseState
import com.app.eventsync.databinding.FragmentEventDetailsBinding
import com.app.eventsync.presentation.comment_screen.CommentsBottomSheetFragment
import com.app.eventsync.presentation.event_screens.create_event_screen.viewModel.CreateEventViewModel
import com.app.eventsync.presentation.event_screens.event_detail_screen.add_friend_in_event_screen.FriendListBottomSheetFragment
import com.app.eventsync.presentation.event_screens.event_detail_screen.event_attachments.adapter.ChooseMediaAdapter
import com.app.eventsync.presentation.event_screens.event_detail_screen.event_attachments.adapter.OtherUserMediaAdapter
import com.app.eventsync.presentation.event_screens.event_detail_screen.event_attachments.adapter.OtherUserOnRemoveMediaCallback
import com.app.eventsync.presentation.event_screens.event_detail_screen.event_attachments.adapter.RemoveImageCallback
import com.app.eventsync.presentation.main_screens.home_screen.viewModel.HomeViewModel
import com.app.eventsync.utils.AppUtils
import com.app.eventsync.utils.Constants
import com.app.eventsync.utils.Constants.EVENT_ID
import com.app.eventsync.utils.ErrorDialog
import com.app.eventsync.utils.LoadingDialog
import com.app.eventsync.utils.PhotoChooserDialog
import com.app.eventsync.utils.PreferenceManager
import com.app.eventsync.utils.Resource
import com.app.eventsync.utils.SuccessDialog
import com.app.eventsync.utils.showToast
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class EventDetailsFragment : Fragment(), RemoveImageCallback, OtherUserOnRemoveMediaCallback {


    private lateinit var binding: FragmentEventDetailsBinding
    private val viewModel: CreateEventViewModel by viewModels()
    private lateinit var loadingDialog: LoadingDialog
    private val homeViewModel: HomeViewModel by viewModels()
    private var isEnrollEventStateObserved: MutableStateFlow<Boolean> = MutableStateFlow(false)

    @Inject
    lateinit var preferenceManager: PreferenceManager

    private var isCommentsAllowed: Boolean = false

    private val requestCode = 1000

    private lateinit var imageUri: Uri

    private var eventId: String = ""

    private var creatorId: String = ""

    private lateinit var mediaAdapter: ChooseMediaAdapter

    private lateinit var otherUserMediaAdapter: OtherUserMediaAdapter

    private var currentPhotoList: ArrayList<String> = ArrayList()

    private var otherUserPhotoList: ArrayList<EventImage> = ArrayList()

    private var currentPhotoListSize: Int = 0

    private lateinit var alarmSchedulerImp: AlarmSchedulerImp

    private val contract = registerForActivityResult(ActivityResultContracts.TakePicture()) {
        ErrorDialog(
            requireContext(),
            "Are you sure?",
            "Are you sure you want to upload the image.",
            onPositiveButtonClick = {
                viewModel.updateEventImageList(imageUri, eventId)
            }).showErrorDialog()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentEventDetailsBinding.inflate(layoutInflater)
        requireActivity().window.navigationBarColor =
            resources.getColor(R.color.background_grey_color)
        loadingDialog = LoadingDialog(requireContext())
        alarmSchedulerImp = AlarmSchedulerImp(requireContext())
        mediaAdapter = ChooseMediaAdapter(requireContext(), currentPhotoList, this, false)
        otherUserMediaAdapter =
            OtherUserMediaAdapter(requireContext(), otherUserPhotoList, this, creatorId)
        binding.rvOtherMedia.adapter = otherUserMediaAdapter
        binding.rvOtherMedia.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

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
        }

        viewModel.otherUsersImageState.observe(viewLifecycleOwner) { value ->
            when (value) {
                is DatabaseState.Loading -> {
                    loadingDialog.show()
                }

                is DatabaseState.Success -> {
                    otherUserPhotoList.clear()
                    otherUserPhotoList.addAll(value.data!!)
                    otherUserMediaAdapter.notifyDataSetChanged()
                    loadingDialog.dismiss()
                }

                is DatabaseState.Error -> {
                    loadingDialog.dismiss()
                    requireContext().showToast("Something went wrong.")
                }
            }
        }

        viewModel.otherUserRemoveImageState.observe(viewLifecycleOwner) { value ->

            when (value) {
                is Resource.Loading -> {
                    loadingDialog.show()
                    requireContext().showToast("Removing image...")
                }

                is Resource.Success -> {

                    loadingDialog.dismiss()
                    requireContext().showToast("Deleted Successfully.")
                }

                is Resource.Error -> {
                    loadingDialog.dismiss()
                    requireContext().showToast("Something went wrong.")
                }
            }

        }

        viewModel.updateEventImageState.observe(viewLifecycleOwner) { value ->
            when (value) {
                is DatabaseState.Loading -> {
                    requireContext().showToast("Uploading Image...")
                    loadingDialog.show()
                }

                is DatabaseState.Success -> {

                    loadingDialog.dismiss()
                    requireContext().showToast("Upload success.")

                }

                is DatabaseState.Error -> {
                    loadingDialog.dismiss()
                    requireContext().showToast(value.message.toString())
                }
            }
        }

        binding.addImgBtn.setOnClickListener {


            PhotoChooserDialog(requireContext(), cameraOnClick = {

                imageUri = createImageUri()!!
                contract.launch(imageUri)

            }, galleryOnClick = {

                invokeAttachFileIntent(Constants.ATTACHMENT_CHOICE_CHOOSE_GALLERY)

            }).showPhotoPickerDialog()


        }


        eventId = arguments?.getString(EVENT_ID).toString()

        if (savedInstanceState == null) {
            viewModel.getOtherUserImageList(eventId)
            viewModel.getEventDetails(eventId)
        }

        viewModel.eventDetailsState.observe(viewLifecycleOwner) { value ->

            when (value) {
                is DatabaseState.Loading -> {
                    loadingDialog.show()
                }

                is DatabaseState.Success -> {

                    val eventItem = value.data

                    if (eventItem != null) {
                        creatorId = eventItem.creatorId
                        binding.tvTitle.text = eventItem.title
                        binding.tvDate.text =
                            "${AppUtils.getFormattedDateWithTime(eventItem.startTime!!)}"
                        binding.tvDescription.text = eventItem.description
                        binding.tvLocation.text = eventItem.location
                        if (eventItem.paymentRequirements.isEmpty()) {
                            binding.tvPaymentRequirement.text = "Not mentioned"
                        } else {
                            binding.tvPaymentRequirement.text = eventItem.paymentRequirements
                        }
                        if (eventItem.organizationType.isEmpty()) {
                            binding.tvOrganizationType.text = "Not mentioned"
                        } else {
                            binding.tvOrganizationType.text = eventItem.organizationType
                        }
                        if (eventItem.requirements.isEmpty()) {
                            binding.tvRequirements.text = "Not mentioned"
                        } else {
                            binding.tvRequirements.text = eventItem.requirements
                        }

                        isCommentsAllowed = eventItem.commentsAllowed

                        if (eventItem.privateMode) {
                            if (eventItem.creatorId == preferenceManager.getUserId()) {
                                binding.showAllUserBtn.visibility = View.VISIBLE
                            } else {
                                binding.showAllUserBtn.visibility = View.GONE
                            }
                        } else {
                            binding.showAllCommentBtn.visibility = View.VISIBLE
                        }

                        if (eventItem.commentsAllowed) {
                            binding.showAllCommentBtn.visibility = View.VISIBLE
                        } else {
                            binding.showAllCommentBtn.visibility = View.GONE
                            binding.tvCommentTitle.text = "Comments are not allowed."
                        }
                        currentPhotoList.addAll(eventItem.imageList)
                        eventId = eventItem.id
                        creatorId = creatorId
                        currentPhotoListSize = eventItem.imageList.size
                        mediaAdapter.notifyDataSetChanged()
                        checkForNoMedia()
                        if (eventItem.participantsList.isNotEmpty()) {
                            when (eventItem.participantsList.size) {
                                1 -> {
                                    FirebaseFirestore.getInstance().collection("users")
                                        .document(eventItem.participantsList[0])
                                        .addSnapshotListener { value, error ->

                                            val photoUrl = value?.get("profilePhoto")

                                            Glide.with(requireContext())
                                                .load(photoUrl)
                                                .error(Constants.DEFAULT_PROFILE_IMG)
                                                .into(binding.enrolledPersonImg1)

                                        }
                                    binding.tvEnrollmentCount.visibility = View.GONE
                                    binding.enrolledPersonImg2.visibility = View.GONE
                                    binding.enrolledPersonImg3.visibility = View.GONE
                                }

                                2 -> {
                                    FirebaseFirestore.getInstance().collection("users")
                                        .document(eventItem.participantsList[0])
                                        .addSnapshotListener { value, error ->

                                            val photoUrl = value?.get("profilePhoto")

                                            Glide.with(requireContext())
                                                .load(photoUrl)
                                                .error(Constants.DEFAULT_PROFILE_IMG)
                                                .into(binding.enrolledPersonImg1)

                                        }

                                    FirebaseFirestore.getInstance().collection("users")
                                        .document(eventItem.participantsList[1])
                                        .addSnapshotListener { value, error ->

                                            val photoUrl = value?.get("profilePhoto")

                                            Glide.with(requireContext())
                                                .load(photoUrl)
                                                .error(Constants.DEFAULT_PROFILE_IMG)
                                                .into(binding.enrolledPersonImg2)

                                        }
                                    binding.tvEnrollmentCount.visibility = View.GONE
                                    binding.enrolledPersonImg3.visibility = View.GONE
                                }

                                3 -> {
                                    binding.tvEnrollmentCount.visibility = View.GONE
                                    FirebaseFirestore.getInstance().collection("users")
                                        .document(eventItem.participantsList[0])
                                        .addSnapshotListener { value, error ->

                                            val photoUrl = value?.get("profilePhoto")

                                            Glide.with(requireContext())
                                                .load(photoUrl)
                                                .error(Constants.DEFAULT_PROFILE_IMG)
                                                .into(binding.enrolledPersonImg1)

                                        }
                                    FirebaseFirestore.getInstance().collection("users")
                                        .document(eventItem.participantsList[1])
                                        .addSnapshotListener { value, error ->

                                            val photoUrl = value?.get("profilePhoto")

                                            Glide.with(requireContext())
                                                .load(photoUrl)
                                                .error(Constants.DEFAULT_PROFILE_IMG)
                                                .into(binding.enrolledPersonImg2)

                                        }
                                    FirebaseFirestore.getInstance().collection("users")
                                        .document(eventItem.participantsList[2])
                                        .addSnapshotListener { value, error ->

                                            val photoUrl = value?.get("profilePhoto")

                                            Glide.with(requireContext())
                                                .load(photoUrl)
                                                .error(Constants.DEFAULT_PROFILE_IMG)
                                                .into(binding.enrolledPersonImg3)

                                        }
                                }

                                else -> {
                                    FirebaseFirestore.getInstance().collection("users")
                                        .document(eventItem.participantsList[0])
                                        .addSnapshotListener { value, error ->

                                            val photoUrl = value?.get("profilePhoto")

                                            Glide.with(requireContext())
                                                .load(photoUrl)
                                                .error(Constants.DEFAULT_PROFILE_IMG)
                                                .into(binding.enrolledPersonImg1)

                                        }
                                    FirebaseFirestore.getInstance().collection("users")
                                        .document(eventItem.participantsList[1])
                                        .addSnapshotListener { value, error ->

                                            val photoUrl = value?.get("profilePhoto")

                                            Glide.with(requireContext())
                                                .load(photoUrl)
                                                .error(Constants.DEFAULT_PROFILE_IMG)
                                                .into(binding.enrolledPersonImg2)

                                        }
                                    FirebaseFirestore.getInstance().collection("users")
                                        .document(eventItem.participantsList[2])
                                        .addSnapshotListener { value, error ->

                                            val photoUrl = value?.get("profilePhoto")

                                            Glide.with(requireContext())
                                                .load(photoUrl)
                                                .error(Constants.DEFAULT_PROFILE_IMG)
                                                .into(binding.enrolledPersonImg3)

                                        }
                                    binding.tvEnrollmentCount.visibility = View.VISIBLE
                                    binding.tvEnrollmentCount.text =
                                        "  +" + (eventItem.participantsList.size - 3).toString()
                                }
                            }
                        }
                        binding.tvAvialableSeats.text = eventItem.availableSeats.toString()

                        if (eventItem.participantsList.isNotEmpty() && eventItem.participantsList.contains(
                                preferenceManager.getUserId()
                            )
                        ) {
                            binding.enrollNowBtn.text = "Cancel Event"
                            binding.enrollNowBtn.background.setTint(requireContext().getColor(com.app.eventsync.R.color.content_grey_color))
                            binding.enrollNowBtn.setOnClickListener {
                                CoroutineScope(Dispatchers.Main).launch {
                                    isEnrollEventStateObserved.value = false
                                    homeViewModel.cancelUserEvent(
                                        preferenceManager.getUserId()!!,
                                        eventItem.id,
                                        eventItem.title,
                                        eventItem.startTime!!
                                    ).collect { value ->
                                        when (value) {
                                            is DatabaseState.Loading -> {
                                                loadingDialog.show()
                                            }

                                            is DatabaseState.Success -> {
                                                if (!isEnrollEventStateObserved.value) {
                                                    isEnrollEventStateObserved.value = true
                                                    loadingDialog.dismiss()
                                                    requireContext().showToast("Success")
                                                    alarmSchedulerImp.cancel(value.data!!)
                                                    SuccessDialog(
                                                        requireContext(),
                                                        "Event Cancelled...",
                                                        "Event has been deleted from your event list"
                                                    )
                                                        .showSuccessDialog()
                                                }
                                                FirebaseFirestore.getInstance()
                                                    .collection("all_events")
                                                    .document(eventItem.id)
                                                    .update(
                                                        "availableSeats",
                                                        FieldValue.increment(1)
                                                    ).addOnSuccessListener {
                                                    }

                                            }

                                            is DatabaseState.Error -> {
                                                if (!isEnrollEventStateObserved.value) {
                                                    isEnrollEventStateObserved.value = true
                                                    loadingDialog.dismiss()
                                                }

                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            binding.enrollNowBtn.text = "Enroll Now"
                            binding.enrollNowBtn.background.setTint(requireContext().getColor(com.app.eventsync.R.color.button_background_color))
                            binding.enrollNowBtn.setOnClickListener {
                                CoroutineScope(Dispatchers.Main).launch {
                                    isEnrollEventStateObserved.value = false
                                    homeViewModel.enrollUserToEvent(
                                        preferenceManager.getUserId()!!,
                                        eventItem.id,
                                        eventItem.title,
                                        eventItem.startTime!!
                                    ).collect { value ->
                                        when (value) {
                                            is DatabaseState.Loading -> {
                                                loadingDialog.show()
                                            }

                                            is DatabaseState.Success -> {
                                                if (!isEnrollEventStateObserved.value) {
                                                    isEnrollEventStateObserved.value = true
                                                    loadingDialog.dismiss()
                                                    requireContext().showToast("Success")
                                                    alarmSchedulerImp.schedule(value.data!!)
                                                    SuccessDialog(
                                                        requireContext(),
                                                        "Successfully Enrolled..",
                                                        "You can check your enrolled event by going to enrolled events section."
                                                    )
                                                        .showSuccessDialog()

                                                    FirebaseFirestore.getInstance()
                                                        .collection("all_events")
                                                        .document(eventItem.id)
                                                        .update(
                                                            "availableSeats",
                                                            FieldValue.increment(-1)
                                                        ).addOnSuccessListener {

                                                        }
                                                }

                                            }

                                            is DatabaseState.Error -> {
                                                if (!isEnrollEventStateObserved.value) {
                                                    isEnrollEventStateObserved.value = true
                                                    loadingDialog.dismiss()
                                                }

                                            }
                                        }
                                    }
                                }
                            }
                        }


                    }



                    loadingDialog.dismiss()
                }

                is DatabaseState.Error -> {
                    loadingDialog.dismiss()
                }
            }

        }

        binding.showAllCommentBtn.setOnClickListener {
            val commentsBottomSheet = CommentsBottomSheetFragment()
            val bundle = Bundle()
            bundle.putString(Constants.EVENT_ID, eventId)
            commentsBottomSheet.arguments = bundle
            commentsBottomSheet.show(childFragmentManager, commentsBottomSheet.tag)
        }

        binding.showAllUserBtn.setOnClickListener {
            val bundle = Bundle()
            bundle.putBoolean(Constants.IS_COLLABORATOR_LIST, true)
            bundle.putString(Constants.EVENT_ID, eventId)
            val collaboratorBottomSheet = FriendListBottomSheetFragment()
            collaboratorBottomSheet.arguments = bundle
            collaboratorBottomSheet.show(childFragmentManager, collaboratorBottomSheet.tag)
        }

        binding.backBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.recyclerView.adapter = mediaAdapter
        binding.recyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)



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

            ErrorDialog(
                requireContext(),
                "Are you sure?",
                "Are you sure you want to upload the image.",
                onPositiveButtonClick = {
                    if (selectedImageUri != null) {
                        viewModel.updateEventImageList(selectedImageUri, eventId)
                    }
                }).showErrorDialog()

        }
    }

    override fun removeImageFromList(position: Int) {

    }

    private fun checkForNoMedia() {
        if (mediaAdapter.itemCount == 0) {
            showNoMediaLayout(true)
        } else {
            showNoMediaLayout(false)
        }
    }

    private fun showNoMediaLayout(value: Boolean) {
        binding.noMediaAnim.isVisible = value
        binding.noMediaFoundText.isVisible = value
    }

    override fun onMediaRemoved(eventImage: EventImage, position: Int) {
        ErrorDialog(
            requireContext(),
            "Are you sure?",
            "Are you sure you want to delete the image you can't revert back!"
        ) {
            viewModel.removeOtherUserImage(eventImage)
        }.showErrorDialog()

    }

}