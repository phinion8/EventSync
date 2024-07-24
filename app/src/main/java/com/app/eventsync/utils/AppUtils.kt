package com.app.eventsync.utils

import android.Manifest
import android.R
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import coil.load
import com.app.eventsync.data.models.Event
import com.app.eventsync.databinding.ErrorDialogLayoutBinding
import com.app.eventsync.databinding.EventPreviewLayoutBinding
import com.app.eventsync.databinding.LoadingLayoutBinding
import com.app.eventsync.databinding.MediaChooserDialogBinding
import com.app.eventsync.databinding.SuccessDialogLayoutBinding
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun Context.showToast(message: String) {
    Toast.makeText(
        this,
        message,
        Toast.LENGTH_SHORT
    ).show()

}

class LoadingDialog(context: Context) {

    private var loadingDialogBinding: LoadingLayoutBinding =
        LoadingLayoutBinding.inflate(LayoutInflater.from(context))
    var loadingDialog: AlertDialog

    init {
        loadingDialog = AlertDialog.Builder(context)
            .setView(loadingDialogBinding.root)
            .setCancelable(false)
            .create()
        loadingDialog.window?.setBackgroundDrawable(ColorDrawable(context.resources.getColor(R.color.transparent)))
    }

    fun show() {
        loadingDialog.show()
    }

    fun dismiss() {
        loadingDialog.dismiss()
    }


}

class SuccessDialog(context: Context, title: String, description: String) {

    private var binding = SuccessDialogLayoutBinding.inflate(LayoutInflater.from(context))
    private var successDialog: AlertDialog

    init {
        successDialog = AlertDialog.Builder(context)
            .setView(binding.root)
            .setCancelable(true)
            .create()
        successDialog.window?.setBackgroundDrawable(ColorDrawable(context.resources.getColor(R.color.transparent)))
        with(binding) {
            yesBtn.setOnClickListener {
                successDialog.dismiss()
            }
            successTitle.text = title
            successDescription.text = description
        }
    }

    fun showSuccessDialog() {
        successDialog.show()
    }

    fun dismissSuccessDialog() {
        successDialog.dismiss()
    }


}

class ErrorDialog(
    context: Context,
    title: String,
    description: String,
    onPositiveButtonClick: () -> Unit
) {

    private var binding = ErrorDialogLayoutBinding.inflate(LayoutInflater.from(context))
    private var errorDialog: AlertDialog

    init {
        errorDialog = AlertDialog.Builder(context)
            .setView(binding.root)
            .setCancelable(false)
            .create()
        errorDialog.window?.setBackgroundDrawable(ColorDrawable(context.resources.getColor(R.color.transparent)))
        with(binding) {
            yesBtn.setOnClickListener {
                onPositiveButtonClick()
                errorDialog.dismiss()
            }
            noButton.setOnClickListener {
                errorDialog.dismiss()
            }
            errorTitle.text = title
            errorDescription.text = description
        }
    }

    fun showErrorDialog() {
        errorDialog.show()
    }

    fun dismissDialog() {
        errorDialog.dismiss()
    }


}

class PhotoChooserDialog(context: Context, cameraOnClick: () -> Unit, galleryOnClick: () -> Unit) {

    private var photoPickerDialog: androidx.appcompat.app.AlertDialog
    private val photoChooserDialogBinding =
        MediaChooserDialogBinding.inflate(LayoutInflater.from(context))

    init {
        photoPickerDialog = androidx.appcompat.app.AlertDialog.Builder(context)
            .setView(photoChooserDialogBinding.root)
            .setCancelable(true)
            .create()
        photoChooserDialogBinding.cameraBtn.setOnClickListener {
            cameraOnClick()
            photoPickerDialog.dismiss()
        }

        photoChooserDialogBinding.galleryBtn.setOnClickListener {
            galleryOnClick()
            photoPickerDialog.dismiss()
        }
        photoPickerDialog.window!!.setBackgroundDrawable(ColorDrawable(context.resources.getColor(R.color.transparent)))
    }

    fun showPhotoPickerDialog() {
        photoPickerDialog.show()
    }

    fun dismissPhotoPickerDialog() {
        photoPickerDialog.dismiss()
    }
}

class EventPreviewDialog(
    context: Context,
    event: Event,
    onClick: (id: String) -> Unit,
    onEnrollButtonClick: () -> Unit,
    onCancelButtonClick: () -> Unit
) {
    private val preferenceManager = PreferenceManager(context)
    private var binding: EventPreviewLayoutBinding =
        EventPreviewLayoutBinding.inflate(LayoutInflater.from(context))
    private var eventPreviewDialog: AlertDialog

    init {
        eventPreviewDialog = AlertDialog.Builder(context)
            .setView(binding.root)
            .setCancelable(true)
            .create()
        eventPreviewDialog.window?.setBackgroundDrawable(ColorDrawable(context.resources.getColor(R.color.transparent)))

        FirebaseFirestore.getInstance().collection("users")
            .document(event.creatorId)
            .addSnapshotListener { value, error ->

                val photoUrl = value?.get("profilePhoto")

                binding.creatorImg.load(photoUrl) {
                    error(com.app.eventsync.R.drawable.sample_profile_img)
                    placeholder(com.app.eventsync.R.drawable.sample_profile_img)
                }

            }

        if (preferenceManager.getAllowBackgroundImage()) {
            binding.backgroundImg.visibility = View.VISIBLE


            if (event.imageList.isNotEmpty()) {
                binding.enrolledPersonImg1.load(event.imageList[0]) {
                    error(com.app.eventsync.R.drawable.sample_background_photo)
                    placeholder(com.app.eventsync.R.drawable.sample_background_photo)
                }
            }
        } else {
            binding.backgroundImg.visibility = View.GONE
        }

        if (event.participantsList.isNotEmpty()) {
            binding.noEnrollText.visibility = View.GONE
            when (event.participantsList.size) {
                1 -> {
                    FirebaseFirestore.getInstance().collection("users")
                        .document(event.participantsList[0])
                        .addSnapshotListener { value, error ->

                            val photoUrl = value?.get("profilePhoto")

                            binding.enrolledPersonImg1.load(photoUrl) {
                                error(com.app.eventsync.R.drawable.sample_profile_img)
                                placeholder(com.app.eventsync.R.drawable.sample_profile_img)
                            }

                        }
                    binding.tvEnrollmentCount.visibility = View.GONE
                    binding.enrolledPersonImg2.visibility = View.GONE
                    binding.enrolledPersonImg3.visibility = View.GONE
                }

                2 -> {
                    FirebaseFirestore.getInstance().collection("users")
                        .document(event.participantsList[0])
                        .addSnapshotListener { value, error ->

                            val photoUrl = value?.get("profilePhoto")

                            binding.enrolledPersonImg1.load(photoUrl) {
                                error(com.app.eventsync.R.drawable.sample_profile_img)
                                placeholder(com.app.eventsync.R.drawable.sample_profile_img)
                            }

                        }

                    FirebaseFirestore.getInstance().collection("users")
                        .document(event.participantsList[1])
                        .addSnapshotListener { value, error ->

                            val photoUrl = value?.get("profilePhoto")

                            binding.enrolledPersonImg2.load(photoUrl) {
                                error(com.app.eventsync.R.drawable.sample_profile_img)
                                placeholder(com.app.eventsync.R.drawable.sample_profile_img)
                            }

                        }
                    binding.tvEnrollmentCount.visibility = View.GONE
                    binding.enrolledPersonImg3.visibility = View.GONE
                }

                3 -> {
                    binding.tvEnrollmentCount.visibility = View.GONE
                    FirebaseFirestore.getInstance().collection("users")
                        .document(event.participantsList[0])
                        .addSnapshotListener { value, error ->

                            val photoUrl = value?.get("profilePhoto")

                            binding.enrolledPersonImg1.load(photoUrl) {
                                error(com.app.eventsync.R.drawable.sample_profile_img)
                                placeholder(com.app.eventsync.R.drawable.sample_profile_img)
                            }

                        }
                    FirebaseFirestore.getInstance().collection("users")
                        .document(event.participantsList[1])
                        .addSnapshotListener { value, error ->

                            val photoUrl = value?.get("profilePhoto")

                            binding.enrolledPersonImg2.load(photoUrl) {
                                error(com.app.eventsync.R.drawable.sample_profile_img)
                                placeholder(com.app.eventsync.R.drawable.sample_profile_img)
                            }

                        }
                    FirebaseFirestore.getInstance().collection("users")
                        .document(event.participantsList[2])
                        .addSnapshotListener { value, error ->

                            val photoUrl = value?.get("profilePhoto")

                            binding.enrolledPersonImg3.load(photoUrl) {
                                error(com.app.eventsync.R.drawable.sample_profile_img)
                                placeholder(com.app.eventsync.R.drawable.sample_profile_img)
                            }

                        }
                }

                else -> {
                    FirebaseFirestore.getInstance().collection("users")
                        .document(event.participantsList[0])
                        .addSnapshotListener { value, error ->

                            val photoUrl = value?.get("profilePhoto")

                            binding.enrolledPersonImg1.load(photoUrl) {
                                error(com.app.eventsync.R.drawable.sample_profile_img)
                                placeholder(com.app.eventsync.R.drawable.sample_profile_img)
                            }

                        }
                    FirebaseFirestore.getInstance().collection("users")
                        .document(event.participantsList[1])
                        .addSnapshotListener { value, error ->

                            val photoUrl = value?.get("profilePhoto")

                            binding.enrolledPersonImg2.load(photoUrl) {
                                error(com.app.eventsync.R.drawable.sample_profile_img)
                                placeholder(com.app.eventsync.R.drawable.sample_profile_img)
                            }

                        }
                    FirebaseFirestore.getInstance().collection("users")
                        .document(event.participantsList[2])
                        .addSnapshotListener { value, error ->

                            val photoUrl = value?.get("profilePhoto")

                            binding.enrolledPersonImg3.load(photoUrl) {
                                error(com.app.eventsync.R.drawable.sample_profile_img)
                                placeholder(com.app.eventsync.R.drawable.sample_profile_img)
                            }

                        }
                    binding.tvEnrollmentCount.visibility = View.VISIBLE
                    binding.tvEnrollmentCount.text =
                        "  +" + (event.participantsList.size - 3).toString()
                }
            }
        } else {
            binding.enrolledPersonImg1.visibility = View.GONE
            binding.enrolledPersonImg2.visibility = View.GONE
            binding.enrolledPersonImg3.visibility = View.GONE
            binding.tvEnrollmentCount.visibility = View.GONE
            binding.noEnrollText.visibility = View.VISIBLE
        }

        with(binding) {
            eventItemLayout.setOnClickListener {
                onClick(event.id)
                eventPreviewDialog.dismiss()
            }
            if (event.participantsList.isNotEmpty() && event.participantsList.contains(
                    preferenceManager.getUserId()
                )
            ) {
                tvIsEnrolled.text = "Enrolled"
                tvIsEnrolled.setTextColor(context.getColor(com.app.eventsync.R.color.green))
                previewEnrollBtn.text = "Cancel Event"
                previewEnrollBtn.background.setTint(context.getColor(com.app.eventsync.R.color.content_grey_color))
            } else {
                previewEnrollBtn.text = "Enroll Now"
                previewEnrollBtn.background.setTint(context.getColor(com.app.eventsync.R.color.button_background_color))
            }
            tvEventTitle.text = event.title
            tvLocation.text = event.location
            tvDescription.text = event.description
            tvEventDate.text = AppUtils.getFormattedDateWithTime(event.startTime!!)
            tvAvailableSeats.text = "Available Seats - " + event.availableSeats.toString()
            if (event.availableSeats <= 0) {
                tvAvailableSeats.text = "Full"
                tvAvailableSeats.setTextColor(context.getColor(R.color.holo_red_light))
            } else
                tvCreatorName.text = event.creatorName

            if (event.imageList.isNotEmpty()) {
                Glide.with(context)
                    .load(event.imageList)
                    .error(com.app.eventsync.R.drawable.sample_background_photo)
                    .into(backgroundImg)
            }

            previewEnrollBtn.setOnClickListener {
                if (event.participantsList.isNotEmpty() && event.participantsList.contains(
                        preferenceManager.getUserId()
                    )
                ) {

                    FirebaseFirestore.getInstance().collection("all_events")
                        .document(event.id)
                        .update("availableSeats", FieldValue.increment(1)).addOnSuccessListener {
                            onCancelButtonClick()
                        }


                } else {
                    if (event.availableSeats > 0) {
                        FirebaseFirestore.getInstance().collection("all_events")
                            .document(event.id)
                            .update("availableSeats", FieldValue.increment(-1))
                            .addOnSuccessListener {
                                onEnrollButtonClick()
                            }
                    } else {
                        binding.previewEnrollBtn.text = "Full"
                        binding.previewEnrollBtn.background.setTint(context.getColor(com.app.eventsync.R.color.content_grey_color))
                    }
                }

                dismiss()
            }

        }

    }


    fun show() {
        eventPreviewDialog.show()
    }

    fun dismiss() {
        eventPreviewDialog.dismiss()
    }
}

object AppUtils {

    fun hideKeyboard(activity: Activity) {
        val view = activity.currentFocus
        if (view != null) {
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    fun requestCameraAndGalleryPermissions(
        activity: Activity,
        requestCameraAndGalleryPermissionCode: Int
    ) {
        val permission = Manifest.permission.READ_EXTERNAL_STORAGE
        val cameraPermission = Manifest.permission.CAMERA
        val granted = PackageManager.PERMISSION_GRANTED

        if (ContextCompat.checkSelfPermission(activity, permission) != granted) {
            if (ContextCompat.checkSelfPermission(activity, cameraPermission) != granted) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(permission, cameraPermission),
                    requestCameraAndGalleryPermissionCode
                )
            }
            // Permission is not granted; request it
        } else {
            // Permission is already granted; you can access the gallery
            // Perform your gallery-related operations here
        }
    }

    fun checkCameraAndGalleryPermissions(context: Context): Boolean {
        val permission = Manifest.permission.READ_EXTERNAL_STORAGE
        val cameraPermission = Manifest.permission.CAMERA
        val granted = PackageManager.PERMISSION_GRANTED
        if (ContextCompat.checkSelfPermission(context, permission) != granted
            && ContextCompat.checkSelfPermission(context, cameraPermission) != granted
        ) {
            return false
        } else {
            return true
        }

    }

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri: Uri = Uri.fromParts("package", context.packageName, null)
        intent.data = uri
        context.startActivity(intent)
    }

    fun isNotificationPermissionGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                return true
            } else {
                return false
            }

        }
        return true
    }

    fun setDarkMode() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }

    fun isDarkMode(context: Context): Boolean {

        return when (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }
    }

    fun setLightMode() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }

    fun setDefaultThemeMode() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

    fun getTodayDate(): Date {
        val calendar = Calendar.getInstance()
        return calendar.time
    }

    fun getFormattedDate(date: Date): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(date)
    }

    fun getTomorrowDate(): Date {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        return calendar.time
    }

    fun getFormattedDateWithTime(date: Date): String {
        val sdf = SimpleDateFormat("dd MMM yyyy hh:mma", Locale.getDefault())
        val formattedDate = sdf.format(date)
        return formattedDate.replace("am", "AM").replace("pm", "PM")
    }


    fun getStartOfDay(): Date {
        val calendar = Calendar.getInstance()
        calendar[Calendar.HOUR_OF_DAY] = 0
        calendar[Calendar.MINUTE] = 0
        calendar[Calendar.SECOND] = 0
        calendar[Calendar.MILLISECOND] = 0
        return calendar.time
    }

    // Helper method to get the end of the day in milliseconds
    fun getEndOfDay(): Date {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.time
    }

    fun getStartOfTomorrow(): Date {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    fun getEndOfTomorrow(): Date {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.time
    }

    fun getNavOptionsAnimation() {

    }
}