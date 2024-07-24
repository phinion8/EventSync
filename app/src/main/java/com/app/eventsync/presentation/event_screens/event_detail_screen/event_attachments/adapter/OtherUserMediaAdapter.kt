package com.app.eventsync.presentation.event_screens.event_detail_screen.event_attachments.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.app.eventsync.data.models.EventImage
import com.app.eventsync.databinding.MediaItemBinding
import com.app.eventsync.utils.PreferenceManager
import com.bumptech.glide.Glide


class OtherUserMediaAdapter(
    private val context: Context,
    private val photoList: List<EventImage>,
    private val otherUserOnRemoveMediaCallback: OtherUserOnRemoveMediaCallback,
    private val creatorId: String
) : RecyclerView.Adapter<OtherUserMediaAdapter.OtherUserMediaViewHolder>() {

    class OtherUserMediaViewHolder(
        private val context: Context,
        private val binding: MediaItemBinding,
        private val otherUserOnRemoveMediaCallback: OtherUserOnRemoveMediaCallback
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            eventImage: EventImage,
            position: Int,
            creatorId: String
        ) {

            val preferenceManager = PreferenceManager(context)
            val userId = preferenceManager.getUserId()

            if (eventImage.userId == userId || creatorId == userId) {
                binding.removeBtn.visibility = View.VISIBLE
            } else {
                binding.removeBtn.visibility = View.GONE
            }

            binding.removeBtn.setOnClickListener {

                otherUserOnRemoveMediaCallback.onMediaRemoved(eventImage, position)

            }
            Glide.with(context)
                .load(eventImage.imageUrl)
                .into(binding.choosedMedia)


        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): OtherUserMediaViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = MediaItemBinding.inflate(inflater, parent, false)
        return OtherUserMediaViewHolder(context, binding, otherUserOnRemoveMediaCallback)
    }

    override fun onBindViewHolder(
        holder: OtherUserMediaAdapter.OtherUserMediaViewHolder,
        position: Int
    ) {
        holder.bind(photoList[position], position, creatorId)
    }

    override fun getItemCount(): Int {
        return photoList.size
    }
}

interface OtherUserOnRemoveMediaCallback{
    fun onMediaRemoved(eventImage: EventImage, position: Int)
}
