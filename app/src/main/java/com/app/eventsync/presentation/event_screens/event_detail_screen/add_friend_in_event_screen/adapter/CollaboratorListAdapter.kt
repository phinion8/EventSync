package com.app.eventsync.presentation.event_screens.event_detail_screen.add_friend_in_event_screen.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.app.eventsync.data.models.FriendItem
import com.app.eventsync.databinding.ItemCollaboratorLayoutBinding
import com.app.eventsync.presentation.event_screens.event_detail_screen.add_friend_in_event_screen.CollaboratorItemListener
import com.app.eventsync.utils.Constants
import com.app.eventsync.utils.FriendListDiffUtilCallback
import com.bumptech.glide.Glide

class CollaboratorListAdapter(private val collaboratorItemListener: CollaboratorItemListener) :
    RecyclerView.Adapter<CollaboratorListAdapter.CollaboratorItemViewHolder>() {

    private val collaboratorList: ArrayList<FriendItem> = ArrayList()

    class CollaboratorItemViewHolder(private val binding: ItemCollaboratorLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(friendItem: FriendItem, collaboratorItemListener: CollaboratorItemListener) {
            val context = binding.root.context
            Glide.with(context)
                .load(friendItem.profilePhoto)
                .error(Constants.DEFAULT_PROFILE_IMG)
                .into(binding.profileImage)

            binding.userName.text = friendItem.name

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollaboratorItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemCollaboratorLayoutBinding.inflate(inflater, parent, false)
        return CollaboratorItemViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return collaboratorList.size
    }

    override fun onBindViewHolder(holder: CollaboratorItemViewHolder, position: Int) {
        holder.bind(collaboratorList[position], collaboratorItemListener)
    }

    fun isEmpty(): Boolean {
        return collaboratorList.isEmpty()
    }

    fun returnList(): ArrayList<FriendItem>{
        return collaboratorList
    }

    fun addItem(friendItem: FriendItem){
        collaboratorList.add(friendItem)
    }

    fun removeItem(friendItem: FriendItem){
        collaboratorList.remove(friendItem)
    }

    fun updateList(newCollaboratorList: List<FriendItem>) {
        val diffCallback = FriendListDiffUtilCallback(collaboratorList, newCollaboratorList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        collaboratorList.clear()
        collaboratorList.addAll(newCollaboratorList)
        diffResult.dispatchUpdatesTo(this)
    }

}