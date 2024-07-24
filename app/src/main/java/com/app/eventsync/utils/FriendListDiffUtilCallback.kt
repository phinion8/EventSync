package com.app.eventsync.utils

import androidx.recyclerview.widget.DiffUtil
import com.app.eventsync.data.models.FriendItem


class FriendListDiffUtilCallback(
        private val oldList: List<FriendItem>,
        private val newList: List<FriendItem>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            // Check if items have the same ID, assuming ID is unique
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            // Check if the content of items is the same
            return oldList[oldItemPosition] == newList[newItemPosition]
        }

        // Optionally, you can override getChangePayload if you need to pass additional info
        // about the change to the adapter
        override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
            // For example, you could return a payload containing specific changed fields
            // between the old and new items
            return super.getChangePayload(oldItemPosition, newItemPosition)
        }
    }

