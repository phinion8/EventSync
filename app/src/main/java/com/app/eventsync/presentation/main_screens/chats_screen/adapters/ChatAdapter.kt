package com.app.eventsync.presentation.main_screens.chats_screen.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.app.eventsync.data.models.ChatMessage
import com.app.eventsync.databinding.ItemContainerReceivedMessageBinding
import com.app.eventsync.databinding.ItemContainerSendMessageBinding
import com.app.eventsync.utils.AppUtils
import com.app.eventsync.utils.Constants
import com.bumptech.glide.Glide

class ChatAdapter(
    private val chatMessages: List<ChatMessage>,
    private val senderId: String,
    private val receiverProfilePic: String
): RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    companion object{
        const val VIEW_TYPE_SENT = 1
        const val VIEW_TYPE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        if (chatMessages.get(position).senderId.equals(senderId)){
            return VIEW_TYPE_SENT
        }else{
            return VIEW_TYPE_RECEIVED
        }
    }

    class SentMessageViewHolder(private val binding: ItemContainerSendMessageBinding): RecyclerView.ViewHolder(binding.root){
        fun bind(chatMessage: ChatMessage){
            binding.tvSentMessage.text = chatMessage.message
            binding.tvDateTime.text = AppUtils.getFormattedDateWithTime(chatMessage.dateTime)
        }
    }

    class ReceivedMessageViewHolder(private val binding: ItemContainerReceivedMessageBinding): RecyclerView.ViewHolder(binding.root){
        fun bind(chatMessage: ChatMessage, receiverProfilePic: String){
            Glide.with(binding.root.context)
                .load(receiverProfilePic)
                .error(Constants.DEFAULT_PROFILE_IMG)
                .into(binding.receivedMessageImg)
            binding.tvMessage.text = chatMessage.message
            binding.receivedMessageTime.text = AppUtils.getFormattedDateWithTime(chatMessage.dateTime)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == VIEW_TYPE_SENT){
            return SentMessageViewHolder(ItemContainerSendMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }else{
            return ReceivedMessageViewHolder(ItemContainerReceivedMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
    }

    override fun getItemCount(): Int {
        return chatMessages.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder){
            is SentMessageViewHolder->{
                holder.bind(chatMessages[position])
            }
            is ReceivedMessageViewHolder->{
                holder.bind(chatMessages[position], receiverProfilePic)
            }
        }
    }
}