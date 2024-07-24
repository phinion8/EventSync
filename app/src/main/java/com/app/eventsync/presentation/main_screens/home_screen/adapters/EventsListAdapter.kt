package com.app.eventsync.presentation.main_screens.home_screen.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.app.eventsync.R
import com.app.eventsync.data.models.Event
import com.app.eventsync.databinding.ItemEventLayoutBinding
import com.app.eventsync.presentation.main_screens.home_screen.events_screens.EventItemListener
import com.app.eventsync.utils.AppUtils
import com.app.eventsync.utils.EventListDiffCallback
import com.app.eventsync.utils.PreferenceManager
import com.google.firebase.firestore.FirebaseFirestore

class EventsListAdapter(
    private val context: Context,
    private val eventItemListener: EventItemListener,
    private val showEditButton: Boolean = false
) : RecyclerView.Adapter<EventsListAdapter.EventItemViewHolder>() {

    private var eventList: ArrayList<Event> = ArrayList()

    class EventItemViewHolder(private val binding: ItemEventLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(

            context: Context,
            event: Event,
            eventItemListener: EventItemListener,
            showEditButton: Boolean = false
        ) {

            val preferenceManager = PreferenceManager(context)

            if (showEditButton) {
                binding.editImg.visibility = View.VISIBLE
            } else {
                binding.editImg.visibility = View.GONE
            }

            binding.editImg.setOnClickListener {
                if (showEditButton) {
                    eventItemListener.onEventEditButtonClick(eventId = event.id)
                }
            }

            if (event.participantsList.isNotEmpty()) {
                binding.noEnrollmentText.visibility = View.GONE
                when (event.participantsList.size) {
                    1 -> {
                        binding.enrolledPersonImg1.visibility = View.VISIBLE
                        FirebaseFirestore.getInstance().collection("users")
                            .document(event.participantsList[0])
                            .addSnapshotListener { value, error ->

                                val photoUrl = value?.get("profilePhoto")

                                binding.enrolledPersonImg1.load(photoUrl){
                                    error(R.drawable.sample_profile_img)
                                    placeholder(R.drawable.sample_profile_img)
                                }

                            }
                        binding.tvEnrollmentCount.visibility = View.GONE
                        binding.enrolledPersonImg2.visibility = View.GONE
                        binding.enrolledPersonImg3.visibility = View.GONE
                    }

                    2 -> {
                        binding.enrolledPersonImg1.visibility = View.VISIBLE
                        binding.enrolledPersonImg2.visibility = View.VISIBLE
                        FirebaseFirestore.getInstance().collection("users")
                            .document(event.participantsList[0])
                            .addSnapshotListener { value, error ->

                                val photoUrl = value?.get("profilePhoto")

                                binding.enrolledPersonImg1.load(photoUrl){
                                    error(R.drawable.sample_profile_img)
                                    placeholder(R.drawable.sample_profile_img)

                                }

                            }

                        FirebaseFirestore.getInstance().collection("users")
                            .document(event.participantsList[1])
                            .addSnapshotListener { value, error ->

                                val photoUrl = value?.get("profilePhoto")

                                binding.enrolledPersonImg2.load(photoUrl){
                                    error(R.drawable.sample_profile_img)
                                    placeholder(R.drawable.sample_profile_img)
                                }

                            }
                        binding.tvEnrollmentCount.visibility = View.GONE
                        binding.enrolledPersonImg3.visibility = View.GONE
                    }

                    3 -> {
                        binding.enrolledPersonImg1.visibility = View.VISIBLE
                        binding.enrolledPersonImg2.visibility = View.VISIBLE
                        binding.enrolledPersonImg3.visibility = View.VISIBLE
                        binding.tvEnrollmentCount.visibility = View.GONE
                        FirebaseFirestore.getInstance().collection("users")
                            .document(event.participantsList[0])
                            .addSnapshotListener { value, error ->

                                val photoUrl = value?.get("profilePhoto")

                                binding.enrolledPersonImg1.load(photoUrl){
                                    error(R.drawable.sample_profile_img)
                                    placeholder(R.drawable.sample_profile_img)
                                }

                            }
                        FirebaseFirestore.getInstance().collection("users")
                            .document(event.participantsList[1])
                            .addSnapshotListener { value, error ->

                                val photoUrl = value?.get("profilePhoto")

                                binding.enrolledPersonImg2.load(photoUrl){
                                    error(R.drawable.sample_profile_img)
                                    placeholder(R.drawable.sample_profile_img)
                                }

                            }
                        FirebaseFirestore.getInstance().collection("users")
                            .document(event.participantsList[2])
                            .addSnapshotListener { value, error ->

                                val photoUrl = value?.get("profilePhoto")


                                binding.enrolledPersonImg3.load(photoUrl){
                                    error(R.drawable.sample_profile_img)
                                    placeholder(R.drawable.sample_profile_img)
                                }


                            }

                    }

                    else -> {
                        binding.enrolledPersonImg1.visibility = View.VISIBLE
                        binding.enrolledPersonImg2.visibility = View.VISIBLE
                        binding.enrolledPersonImg3.visibility = View.VISIBLE
                        FirebaseFirestore.getInstance().collection("users")
                            .document(event.participantsList[0])
                            .addSnapshotListener { value, error ->

                                val photoUrl = value?.get("profilePhoto")

                                binding.enrolledPersonImg1.load(photoUrl){
                                    error(R.drawable.sample_profile_img)
                                    placeholder(R.drawable.sample_profile_img)

                                }

                            }
                        FirebaseFirestore.getInstance().collection("users")
                            .document(event.participantsList[1])
                            .addSnapshotListener { value, error ->

                                val photoUrl = value?.get("profilePhoto")

                                binding.enrolledPersonImg2.load(photoUrl){
                                    error(R.drawable.sample_profile_img)
                                    placeholder(R.drawable.sample_profile_img)
                                }

                            }
                        FirebaseFirestore.getInstance().collection("users")
                            .document(event.participantsList[2])
                            .addSnapshotListener { value, error ->

                                val photoUrl = value?.get("profilePhoto")

                                binding.enrolledPersonImg3.load(photoUrl){
                                    error(R.drawable.sample_profile_img)
                                    placeholder(R.drawable.sample_profile_img)
                                }

                            }
                        binding.tvEnrollmentCount.visibility = View.VISIBLE
                        binding.tvEnrollmentCount.text =
                            " +" +  (event.participantsList.size - 3).toString()
                    }
                }
            }else{
                binding.tvEnrollmentCount.visibility = View.GONE
                binding.enrolledPersonImg1.visibility = View.GONE
                binding.enrolledPersonImg2.visibility = View.GONE
                binding.enrolledPersonImg3.visibility = View.GONE
                binding.tvEnrollmentCount.visibility = View.GONE
                binding.noEnrollmentText.visibility = View.VISIBLE
            }

            if (preferenceManager.getAllowBackgroundImage()) {
                if (event.imageList.isNotEmpty()){

                    binding.backgroundImg.load(event.imageList[0]){
                        placeholder(R.drawable.sample_background_photo)
                        error(R.drawable.sample_background_photo)
                    }

                }else{
                    binding.backgroundImg.load(R.drawable.sample_background_photo)
                }

            } else {
                binding.backgroundImg.visibility = View.GONE
            }

            binding.tvEventTitle.text = event.title
            binding.tvEventDate.text = AppUtils.getFormattedDateWithTime(event.startTime!!)
            binding.tvLocation.text = event.location
            binding.enrollBtn.setOnClickListener {
                eventItemListener.onEventEnrollButtonClick(event.id, event.title, event.startTime)
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding: ItemEventLayoutBinding =
            ItemEventLayoutBinding.inflate(inflater, parent, false)
        return EventItemViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return eventList.size
    }

    override fun onBindViewHolder(holder: EventItemViewHolder, position: Int) {
        val eventItem = eventList[position]
        holder.bind(context, eventItem, eventItemListener, showEditButton)
        holder.itemView.setOnClickListener {
            eventItemListener.onEventItemClick(eventItem)
        }
    }

    fun isEventListEmpty(): Boolean {
        return eventList.isEmpty()
    }

    fun updateEvents(newEventList: List<Event>) {
        val diffCallback = EventListDiffCallback(eventList, newEventList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        eventList.clear()
        eventList.addAll(newEventList)
        diffResult.dispatchUpdatesTo(this)
    }


}