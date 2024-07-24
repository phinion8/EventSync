package com.app.eventsync.presentation.event_screens.event_detail_screen.create_event_detail

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.app.eventsync.data.models.Event
import com.app.eventsync.data.models.EventImage
import com.app.eventsync.data.repositories.DatabaseState
import com.app.eventsync.databinding.FragmentCreateEventDetailBinding
import com.app.eventsync.presentation.event_screens.create_event_screen.viewModel.CreateEventViewModel
import com.app.eventsync.presentation.event_screens.event_detail_screen.add_friend_in_event_screen.FriendListBottomSheetFragment
import com.app.eventsync.presentation.profile_page_screen.viewModel.UserProfileViewModel
import com.app.eventsync.utils.AppUtils
import com.app.eventsync.utils.Constants
import com.app.eventsync.utils.LoadingDialog
import com.app.eventsync.utils.PreferenceManager
import com.app.eventsync.utils.Resource
import com.app.eventsync.utils.SuccessDialog
import com.app.eventsync.utils.ValidationUtil
import com.app.eventsync.utils.showToast
import com.bumptech.glide.Glide
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class CreateEventDetailFragment : Fragment() {

    private lateinit var binding: FragmentCreateEventDetailBinding
    private var isRepeatedEvent: Boolean = false
    private var isCommentAllowed: Boolean = false
    private var startDateTime: Date? = null
    private var endDateTime: Date? = null
    private lateinit var storage: FirebaseStorage
    private lateinit var loadingDialog: LoadingDialog
    private val viewModel: CreateEventViewModel by viewModels(
        ownerProducer = { requireParentFragment() }
    )

    @Inject
    lateinit var preferenceManager: PreferenceManager

    private val userProfileViewModel: UserProfileViewModel by viewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCreateEventDetailBinding.inflate(layoutInflater)

        storage = FirebaseStorage.getInstance()
        loadingDialog = LoadingDialog(requireContext())


        Glide.with(requireContext())
            .load(Constants.DEFAULT_PROFILE_IMG)
            .into(binding.enrolledPersonImg1)
        Glide.with(requireContext())
            .load(Constants.DEFAULT_PROFILE_IMG)
            .into(binding.enrolledPersonImg2)
        Glide.with(requireContext())
            .load(Constants.DEFAULT_PROFILE_IMG)
            .into(binding.enrolledPersonImg3)


        CoroutineScope(Dispatchers.Main).launch {

            viewModel.isEditMode.collect { it ->
                if (it) {
                    viewModel.triggerUpdateButton.observe(viewLifecycleOwner) {
                        if (it) {
                            viewModel.updateEvent(
                                createEventFromUserData().copy(
                                    id = viewModel.eventId.value,
                                    title = viewModel.eventTitle.value!!,
                                    startTime = startDateTime,
                                )
                            )
                        }
                    }
                    viewModel.editEventState.observe(viewLifecycleOwner) { value ->
                        when (value) {
                            is Resource.Loading -> {
                                loadingDialog.show()
                            }

                            is Resource.Success -> {
                                SuccessDialog(
                                    requireContext(),
                                    "Successfully updated...",
                                    "Your event has been updated successfully."
                                ).showSuccessDialog()
                                loadingDialog.dismiss()
                            }

                            is Resource.Error -> {
                                loadingDialog.dismiss()
                                requireContext().showToast(value.message.toString())
                            }

                        }

                    }

                    viewModel.eventId.collect { eventId ->
                        viewModel.eventDetailsState.observe(viewLifecycleOwner) { value ->
                            when (value) {
                                is DatabaseState.Loading -> {
                                    loadingDialog.dismiss()
                                }

                                is DatabaseState.Success -> {
                                    viewModel.setEventTitle(value.data!!.title)
                                    updateEventDetailsViews(value.data)
                                    loadingDialog.dismiss()
                                }

                                is DatabaseState.Error -> {
                                    loadingDialog.dismiss()
                                    requireContext().showToast(value.message.toString())
                                }

                            }

                        }
                    }

                } else {
                    viewModel.triggerSaveEvent.observe(viewLifecycleOwner) {

                        if (it) {
                            val creatorId = preferenceManager.getUserId()
                            val creatorName = preferenceManager.getUserName()
                            val description = binding.etDescription.text.toString()
                            val requirements = binding.etRequirements.text.toString()
                            val paymentRequirements = binding.etPaymentRequirement.text.toString()
                            val location = binding.etLocation.text.toString()
                            val organizationType = binding.etOraganizaton.text.toString()
                            var limitCount = 100
                            if (binding.limitSeatsSwitch.isChecked) {
                                limitCount = binding.etLimitSeats.text.toString().toInt()
                            }
                            if (ValidationUtil.validateEventEditText(
                                    viewModel.eventTitle.value!!,
                                    10
                                )
                            ) {
                                if (ValidationUtil.validateEventEditText(description, 40)) {

                                    if (ValidationUtil.validateEventEditText(location, 10)) {

                                        if (binding.repeatEventSwitch.isChecked) {

                                            if (startDateTime != null) {
                                                if (viewModel.mediaUriList.isNotEmpty()) {
                                                    loadingDialog.show()
                                                    val storageRef =
                                                        storage.reference.child("userPhotos")
                                                    val mediaUriList = viewModel.mediaUriList
                                                    val totalImages = mediaUriList.size
                                                    var uploadedImages =
                                                        0 // Counter for uploaded images
                                                    val downloadUriList: ArrayList<String> =
                                                        ArrayList()

                                                    val eventImageList: ArrayList<EventImage> = ArrayList()

                                                    for (images in mediaUriList) {
                                                        val fileName = generateUniqueFileName("png")
                                                        val imageRef =
                                                            storageRef.child("${preferenceManager.getUserId()}/$fileName")
                                                        val uploadTask =
                                                            imageRef.putFile(Uri.parse(images))

                                                        uploadTask.addOnCompleteListener { task ->
                                                            if (task.isSuccessful) {
                                                                // Get the download URL for the uploaded image
                                                                imageRef.downloadUrl.addOnSuccessListener { uri ->
                                                                    val eventImage = EventImage(userId = preferenceManager.getUserId()!!, imageUrl = uri.toString())
                                                                    eventImageList.add(eventImage)

                                                                    downloadUriList.add(uri.toString())
                                                                    uploadedImages++

                                                                    // Check if all images have been uploaded
                                                                    if (uploadedImages == totalImages) {
                                                                        // All images are uploaded, now add the question to the database
                                                                        loadingDialog.dismiss()

                                                                        val event = creatorId?.let {
                                                                            Event(
                                                                                id = "",
                                                                                title = viewModel.eventTitle.value!!,
                                                                                description = description,
                                                                                creatorId = creatorId,
                                                                                creatorName = creatorName!!,
                                                                                startTime = startDateTime!!,
                                                                                endTime = null,
                                                                                location = location,
                                                                                requirements = requirements,
                                                                                paymentRequirements = paymentRequirements,
                                                                                organizationType = organizationType,
                                                                                availableSeats = limitCount.toLong(),
                                                                                commentsAllowed = isCommentAllowed,
                                                                                repeated = true,
                                                                                participantsList = userProfileViewModel.collaboratorIdList,
                                                                                imageList = downloadUriList,
                                                                                privateMode = binding.privateModeSwitch.isChecked
                                                                            )

                                                                        }
                                                                        if (event != null) {
                                                                            viewModel.saveEvent(
                                                                                event
                                                                            )
                                                                        }


                                                                    }
                                                                }
                                                            }

                                                        }

                                                    }
                                                } else {


                                                    val event = creatorId?.let {
                                                        Event(
                                                            id = "",
                                                            title = viewModel.eventTitle.value!!,
                                                            description = description,
                                                            creatorId = creatorId,
                                                            creatorName = creatorName!!,
                                                            startTime = startDateTime!!,
                                                            endTime = null,
                                                            location = location,
                                                            requirements = requirements,
                                                            paymentRequirements = paymentRequirements,
                                                            organizationType = organizationType,
                                                            availableSeats = limitCount.toLong(),
                                                            commentsAllowed = isCommentAllowed,
                                                            repeated = true,
                                                            participantsList = userProfileViewModel.collaboratorIdList,
                                                            privateMode = binding.privateModeSwitch.isChecked
                                                        )

                                                    }
                                                    if (event != null) {
                                                        viewModel.saveEvent(event)
                                                    }


                                                }

                                            } else {
                                                loadingDialog.dismiss()
                                                requireContext().showToast("Please enter start date.")
                                            }

                                        } else {

                                            if (startDateTime != null && endDateTime != null) {
                                                if (ValidationUtil.isEndDateGreaterThanStartDateAfterOneHour(
                                                        startDateTime!!,
                                                        endDateTime!!
                                                    )
                                                ) {
                                                    if (viewModel.mediaUriList.isNotEmpty()) {
                                                        loadingDialog.show()
                                                        val storageRef =
                                                            storage.reference.child("userPhotos")
                                                        val mediaUriList = viewModel.mediaUriList
                                                        val totalImages = mediaUriList.size
                                                        var uploadedImages =
                                                            0 // Counter for uploaded images
                                                        val downloadUriList: ArrayList<String> =
                                                            ArrayList()

                                                        for (images in mediaUriList) {
                                                            val fileName =
                                                                generateUniqueFileName("png")
                                                            val imageRef =
                                                                storageRef.child("${preferenceManager.getUserId()}/$fileName")
                                                            val uploadTask =
                                                                imageRef.putFile(Uri.parse(images))

                                                            uploadTask.addOnCompleteListener { task ->
                                                                if (task.isSuccessful) {
                                                                    // Get the download URL for the uploaded image
                                                                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                                                                        downloadUriList.add(uri.toString())
                                                                        uploadedImages++

                                                                        // Check if all images have been uploaded
                                                                        if (uploadedImages == totalImages) {
                                                                            loadingDialog.dismiss()
                                                                            // All images are uploaded, now add the question to the database

                                                                            val event =
                                                                                creatorId?.let {
                                                                                    Event(
                                                                                        id = "",
                                                                                        title = viewModel.eventTitle.value!!,
                                                                                        description = description,
                                                                                        creatorId = creatorId,
                                                                                        creatorName = creatorName!!,
                                                                                        startTime = startDateTime!!,
                                                                                        endTime = endDateTime,
                                                                                        location = location,
                                                                                        requirements = requirements,
                                                                                        paymentRequirements = paymentRequirements,
                                                                                        organizationType = organizationType,
                                                                                        availableSeats = limitCount.toLong(),
                                                                                        commentsAllowed = isCommentAllowed,
                                                                                        repeated = false,
                                                                                        participantsList = userProfileViewModel.collaboratorIdList,
                                                                                        imageList = downloadUriList,
                                                                                        privateMode = binding.privateModeSwitch.isChecked
                                                                                    )

                                                                                }
                                                                            if (event != null) {
                                                                                viewModel.saveEvent(
                                                                                    event
                                                                                )
                                                                            }


                                                                        }
                                                                    }
                                                                }

                                                            }

                                                        }
                                                    } else {
                                                        val event = creatorId?.let {
                                                            Event(
                                                                id = "",
                                                                title = viewModel.eventTitle.value!!,
                                                                description = description,
                                                                creatorId = creatorId,
                                                                creatorName = creatorName!!,
                                                                startTime = startDateTime!!,
                                                                endTime = endDateTime,
                                                                location = location,
                                                                requirements = requirements,
                                                                paymentRequirements = paymentRequirements,
                                                                organizationType = organizationType,
                                                                availableSeats = limitCount.toLong(),
                                                                commentsAllowed = isCommentAllowed,
                                                                repeated = false,
                                                                participantsList = userProfileViewModel.collaboratorIdList,
                                                                privateMode = binding.privateModeSwitch.isChecked
                                                            )

                                                        }
                                                        if (event != null) {
                                                            viewModel.saveEvent(event)
                                                        }
                                                    }


                                                } else {
                                                    requireContext().showToast("End time should be at least one hour greater than the start time")
                                                }
                                            } else {
                                                requireContext().showToast("Please enter start date and end date.")
                                            }


                                        }
                                    } else {
                                        requireContext().showToast("Location should be greater than 10 characters.")
                                    }
                                } else {
                                    requireContext().showToast("Event description should be greater than 40 characters.")
                                }
                            } else {
                                requireContext().showToast("Event title should be greater than 10 characters.")
                            }
                        }
                    }
                }
            }
        }



        CoroutineScope(Dispatchers.Main).launch {
            userProfileViewModel.collaboratorList.observe(viewLifecycleOwner) { collaboratorList ->
                if (collaboratorList.isNotEmpty()) {

                    binding.tvNoOneSelected.visibility = View.GONE
                    binding.selectCollaboratorBtn.text = "Remove All"
                    Glide.with(requireContext())
                        .load(collaboratorList[0].profilePhoto)
                        .error(Constants.DEFAULT_PROFILE_IMG)
                        .into(binding.enrolledPersonImg1)
                    binding.tvEnrollmentCount.visibility = View.GONE

                } else {
                    binding.selectCollaboratorBtn.text = "Select"
                    binding.tvNoOneSelected.visibility = View.VISIBLE
                    Glide.with(requireContext())
                        .load(Constants.DEFAULT_PROFILE_IMG)
                        .into(binding.enrolledPersonImg1)
                }

            }
        }

        binding.selectCollaboratorBtn.setOnClickListener {
            if (binding.selectCollaboratorBtn.text == "Remove All") {
                userProfileViewModel.collaboratorList.value?.clear()
                userProfileViewModel.collaboratorList.postValue(ArrayList())
                Log.d("CREATEEVENTDETAIL", userProfileViewModel.collaboratorList.value.toString())

            } else {
                showCollaboratorSelectionBottomSheet()
            }

        }
        binding.repeatEventSwitch.setOnCheckedChangeListener { _, isChecked ->

            isRepeatedEvent = isChecked

        }

        binding.allowCommentsSwitch.setOnCheckedChangeListener { _, isChecked ->

            isCommentAllowed = isChecked

        }

        binding.limitSeatsSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                limitEditTextVisibility(true)
            } else {
                limitEditTextVisibility(false)
            }
        }

        binding.repeatEventSwitch.setOnCheckedChangeListener { _, isChecked ->

            if (isChecked) {
                endDateTimeLayoutVisibility(false)
            } else {
                endDateTimeLayoutVisibility(true)
            }

        }

        binding.etStartDateTime.setOnClickListener {

            showDateTimePicker(true)

        }

        binding.etEndDateTime.setOnClickListener {
            showDateTimePicker(false)
        }

        binding

        return binding.root
    }


    private fun limitEditTextVisibility(visibility: Boolean) {
        binding.etLimitSeats.isVisible = visibility
    }

    private fun endDateTimeLayoutVisibility(visibility: Boolean) {
        binding.endDateTimeText.isVisible = visibility
        binding.etEndDateTime.isVisible = visibility
    }

    private fun showDateTimePicker(isStartDate: Boolean) {
        val currentTime = MaterialDatePicker.todayInUtcMilliseconds()

        val materialDatePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Date")
            .setSelection(currentTime)
            .build()

        materialDatePicker.addOnPositiveButtonClickListener { selectedDate ->
            if (selectedDate >= currentTime) {
                val calendar = Calendar.getInstance().apply {
                    timeInMillis = selectedDate
                }
                showTimePicker(calendar, isStartDate)
            } else {
                // Handle the case where the selected date is in the past
                // You may show a message or take other actions
                Toast.makeText(
                    requireContext(),
                    "Please select a date and time in the future",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        materialDatePicker.show(childFragmentManager, "tag")
    }


    private fun showTimePicker(calendar: Calendar, isStartDate: Boolean) {
        val currentTime = Calendar.getInstance()
        val hour = currentTime.get(Calendar.HOUR_OF_DAY)
        val minute = currentTime.get(Calendar.MINUTE)

        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(hour)
            .setMinute(minute)
            .build()

        timePicker.addOnPositiveButtonClickListener {
            calendar.set(Calendar.HOUR_OF_DAY, timePicker.hour)
            calendar.set(Calendar.MINUTE, timePicker.minute)

            if (isStartDate) {
                startDateTime = calendar.time
                binding.etStartDateTime.text = AppUtils.getFormattedDateWithTime(calendar.time)
            } else {
                endDateTime = calendar.time
                binding.etEndDateTime.text = AppUtils.getFormattedDateWithTime(calendar.time)
            }
        }

        timePicker.show(childFragmentManager, "tag")
    }

    private fun updateEventDetailsViews(event: Event) {
        binding.etDescription.setText(event.description)
        startDateTime = event.startTime
        binding.etStartDateTime.text = AppUtils.getFormattedDateWithTime(event.startTime!!)
        endDateTime = event.endTime ?: Date()
        binding.etEndDateTime.text = AppUtils.getFormattedDateWithTime(event.endTime ?: Date())
        binding.repeatEventSwitch.isChecked = event.repeated
        binding.etRequirements.setText(event.requirements)
        binding.etLocation.setText(event.location)
        binding.etOraganizaton.setText(event.organizationType)
        binding.etLimitSeats.setText(event.availableSeats.toString())
        binding.limitSeatsSwitch.isChecked = true
        binding.allowCommentsSwitch.isChecked = event.commentsAllowed
        binding.etPaymentRequirement.setText(event.paymentRequirements)
        if (event.privateMode != null){
            binding.privateModeSwitch.isChecked = event.privateMode
        }

    }

    private fun createEventFromUserData(): Event {
        if (binding.repeatEventSwitch.isChecked) {

            return Event(
                title = viewModel.eventTitle.value!!,
                description = binding.etDescription.text.toString(),
                startTime = startDateTime,
                repeated = binding.repeatEventSwitch.isChecked,
                requirements = binding.etRequirements.text.toString(),
                location = binding.etLocation.text.toString(),
                paymentRequirements = binding.etPaymentRequirement.text.toString(),
                organizationType = binding.etOraganizaton.text.toString(),
                commentsAllowed = binding.allowCommentsSwitch.isChecked,
                availableSeats = binding.etLimitSeats.text.toString().toLong(),
                privateMode = binding.privateModeSwitch.isChecked,
                imageList = viewModel.mediaUriList
            )

        } else {

            return Event(
                title = viewModel.eventTitle.value!!,
                description = binding.etDescription.text.toString(),
                endTime = endDateTime ?: Date(),
                startTime = startDateTime,
                repeated = binding.repeatEventSwitch.isChecked,
                requirements = binding.etRequirements.text.toString(),
                location = binding.etLocation.text.toString(),
                paymentRequirements = binding.etPaymentRequirement.text.toString(),
                organizationType = binding.etOraganizaton.text.toString(),
                commentsAllowed = binding.allowCommentsSwitch.isChecked,
                availableSeats = binding.etLimitSeats.text.toString().toLong(),
                privateMode = binding.privateModeSwitch.isChecked,
                imageList = viewModel.mediaUriList
            )

        }

    }

    private fun showCollaboratorSelectionBottomSheet() {
        val bottomSheetFragment = FriendListBottomSheetFragment()
        bottomSheetFragment.show(childFragmentManager, bottomSheetFragment.tag)

    }

    fun generateUniqueFileName(fileExtension: String): String {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val randomString = UUID.randomUUID().toString().replace("-", "").substring(0, 6)
        return "file_$timeStamp$randomString.$fileExtension"
    }


}