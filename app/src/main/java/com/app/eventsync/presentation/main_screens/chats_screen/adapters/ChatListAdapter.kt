package com.app.eventsync.presentation.main_screens.chats_screen.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.app.eventsync.R
import com.app.eventsync.data.models.User
import com.app.eventsync.databinding.ItemChatLayoutBinding
import com.app.eventsync.presentation.main_screens.chats_screen.ChatItemListener
import com.app.eventsync.utils.AppUtils
import com.app.eventsync.utils.ChatListDiffUtilCallback
import com.app.eventsync.utils.Constants
import com.bumptech.glide.Glide

class ChatListAdapter(
    private val chatItemListener: ChatItemListener
): RecyclerView.Adapter<ChatListAdapter.ChatItemViewHolder>() {

    private val userList: ArrayList<User> = ArrayList()

    class ChatItemViewHolder(private val binding: ItemChatLayoutBinding): RecyclerView.ViewHolder(binding.root){
        fun bind(user: User){
            binding.tvChatName.text = user.name
            Glide.with(binding.root.context)
                .load(user.profilePhoto)
                .error(Constants.DEFAULT_PROFILE_IMG)
                .into(binding.recentChatProfileImg)

            binding.tvLastChatTime.text = AppUtils.getFormattedDate(user.lastSeen)
            if (user.availability == 1){
                binding.tvLastMessage.text = user.lastChatMessage
                binding.tvLastChatTime.setTextColor(binding.root.context.getColor(R.color.green))
                binding.tvLastChatTime.text = "Online"
            }else{
                if (user.lastChatMessage.isEmpty() || user.lastChatMessage.isBlank()){
                    binding.tvLastMessage.text = "Start Chatting Now!"
                }else{
                    binding.tvLastMessage.text = user.lastChatMessage
                }
                binding.tvLastMessage.setTextColor(binding.root.context.getColor(R.color.content_grey_color))
                binding.tvLastChatTime.text = AppUtils.getFormattedDate(user.lastSeen)
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding: ItemChatLayoutBinding = ItemChatLayoutBinding.inflate(inflater, parent, false)
        return ChatItemViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    override fun onBindViewHolder(holder: ChatItemViewHolder, position: Int) {
        holder.bind(userList[position])
        holder.itemView.setOnClickListener {
            chatItemListener.onChatItemClick(userList[position].id, userList[position].profilePhoto)
        }
    }

    fun isEmpty(): Boolean{
        return userList.isEmpty()
    }

    fun updateList(newUserList: List<User>){
        val diffCallback = ChatListDiffUtilCallback(userList, newUserList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        userList.clear()
        userList.addAll(newUserList)
        diffResult.dispatchUpdatesTo(this)
    }


}