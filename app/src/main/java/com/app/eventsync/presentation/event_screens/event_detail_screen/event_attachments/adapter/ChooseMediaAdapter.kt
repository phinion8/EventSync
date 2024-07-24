package com.app.eventsync.presentation.event_screens.event_detail_screen.event_attachments.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.app.eventsync.databinding.MediaItemBinding
import com.bumptech.glide.Glide


class ChooseMediaAdapter(
    private val context: Context,
    private val photoList: List<String>,
    private val removeImageCallback: RemoveImageCallback,
    private val showRemoveIcon: Boolean = true
) : RecyclerView.Adapter<ChooseMediaAdapter.ChooseMediaViewHolder>() {

    class ChooseMediaViewHolder(
        private val context: Context,
        private val binding: MediaItemBinding,
        private val removeImageCallback: RemoveImageCallback
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            data: String,
            position: Int,
            showRemoveIcon: Boolean
        ) {


            if (!showRemoveIcon) {
                binding.removeBtn.visibility = View.GONE
            } else {
                binding.removeBtn.visibility = View.VISIBLE
            }

            binding.removeBtn.setOnClickListener {

                removeImageCallback.removeImageFromList(position)

            }
            Glide.with(context)
                .load(data)
                .into(binding.choosedMedia)


        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ChooseMediaAdapter.ChooseMediaViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = MediaItemBinding.inflate(inflater, parent, false)
        return ChooseMediaAdapter.ChooseMediaViewHolder(context, binding, removeImageCallback)
    }

    override fun onBindViewHolder(holder: ChooseMediaAdapter.ChooseMediaViewHolder, position: Int) {

        holder.bind(photoList[position], position, showRemoveIcon)


    }

    override fun getItemCount(): Int {
        return photoList.size
    }
}

interface RemoveImageCallback {
    fun removeImageFromList(position: Int)
}