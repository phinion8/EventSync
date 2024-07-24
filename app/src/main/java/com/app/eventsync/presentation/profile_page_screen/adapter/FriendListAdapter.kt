package com.app.eventsync.presentation.profile_page_screen.adapter

import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.app.eventsync.R
import com.app.eventsync.data.models.FriendItem
import com.app.eventsync.databinding.FriendItemLayoutBinding
import com.app.eventsync.presentation.profile_page_screen.request_list_screen.FriendItemListCallback
import com.app.eventsync.utils.FriendListDiffUtilCallback
import com.app.eventsync.utils.PreferenceManager

class FriendListAdapter(
    private val friendItemListCallback: FriendItemListCallback,
    private val isRequest: Boolean = false,
    private val isInBottomSheet: Boolean = false,
    private val isCollaboratorList: Boolean = false,
) : RecyclerView.Adapter<FriendListAdapter.FriendItemViewHolder>() {

    private val friendItemList: ArrayList<FriendItem> = ArrayList()

    class FriendItemViewHolder(private val binding: FriendItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            friendItem: FriendItem,
            friendItemListCallback: FriendItemListCallback,
            isRequest: Boolean = false,
            isInBottomSheet: Boolean,
            isCollaboratorList: Boolean
        ) {
            val context = binding.root.context
            val friendName = friendItem.name
            binding.tvFriendListItemText.text = friendName

            val preferenceManager = PreferenceManager(context)
            val userId = preferenceManager.getUserId()

            binding.profileImage.load(friendItem.profilePhoto){
                error(R.drawable.sample_profile_img)
                placeholder(R.drawable.sample_profile_img)
            }


            listOf(binding.profileImage, binding.tvFriendListItemText).forEach {
                it.setOnClickListener {
                    friendItemListCallback.onFriendItemClick(friendItem.id)
                }
            }





            if (isRequest) {

                val message = friendName + " is Requesting to become your friend."
                val spannableString = SpannableString(message)
                spannableString.setSpan(
                    StyleSpan(Typeface.BOLD),
                    message.indexOf(friendName),
                    friendName.length,
                    SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                binding.tvFriendListItemText.setText(spannableString, TextView.BufferType.SPANNABLE)



                binding.friendListItemBtn.setOnClickListener {
                    friendItemListCallback.onAcceptClick(friendItem.id)
                }

            } else if (isInBottomSheet) {
                binding.friendListItemBtn.visibility = View.GONE
                binding.checkBox.visibility = View.VISIBLE
                binding.friendListItemBtn.background.setTint(context.getColor(R.color.button_background_color))
                binding.checkBox.setOnCheckedChangeListener { _, isChecked ->

                    if (isChecked) {
                        friendItemListCallback.onAddedClick(friendItem)
                    } else {
                        friendItemListCallback.onRemoveCollaborator(friendItem)
                    }

                }

            } else if (isCollaboratorList) {

                binding.friendListItemBtn.visibility = View.GONE
                binding.checkBox.visibility = View.GONE

            } else {
                if (userId != friendItem.id){
                    binding.friendListItemBtn.visibility = View.VISIBLE
                    if (friendItem.requestList.contains(userId)){
                        binding.friendListItemBtn.text = "Requested"
                        binding.friendListItemBtn.isEnabled = false
                        binding.friendListItemBtn.background.setTint(context.getColor(R.color.content_grey_color))
                    }else if(friendItem.friendList.contains(userId)){
                        binding.friendListItemBtn.text = "Remove"
                        binding.friendListItemBtn.setOnClickListener {
                            friendItemListCallback.onRemoveClick(friendItem.id)
                            binding.friendListItemBtn.text = "Add"
                        }
                        binding.friendListItemBtn.background.setTint(context.getColor(R.color.content_grey_color))
                    }else{
                        binding.friendListItemBtn.text = "Add"
                        binding.friendListItemBtn.setOnClickListener {
                            friendItemListCallback.onAddedClick(friendItem)
                            binding.friendListItemBtn.text = "Requested"
                        }
                        binding.friendListItemBtn.background.setTint(context.getColor(R.color.button_background_color))
                    }
                }else{
                    binding.friendListItemBtn.visibility = View.GONE
                }
                binding.tvFriendListItemText.text = friendName
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendItemViewHolder {

        val inflater = LayoutInflater.from(parent.context)
        val binding = FriendItemLayoutBinding.inflate(inflater, parent, false)
        return FriendItemViewHolder(binding)

    }

    override fun getItemCount(): Int {
        return friendItemList.size
    }

    override fun onBindViewHolder(holder: FriendItemViewHolder, position: Int) {

        holder.bind(friendItemList[position], friendItemListCallback, isRequest, isInBottomSheet, isCollaboratorList)

    }

    fun isEmpty(): Boolean {
        return friendItemList.isEmpty()
    }

    fun updateList(newFriendList: List<FriendItem>) {
        val diffCallback = FriendListDiffUtilCallback(friendItemList, newFriendList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        friendItemList.clear()
        friendItemList.addAll(newFriendList)
        diffResult.dispatchUpdatesTo(this)
    }

}