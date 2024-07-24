package com.app.eventsync.presentation.comment_screen.adapter


import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.app.eventsync.R
import com.app.eventsync.data.models.CommentItem
import com.app.eventsync.databinding.CommentItemLayoutBinding
import com.app.eventsync.utils.CommentListDIffUtilCallback
import com.app.eventsync.utils.PreferenceManager
import com.app.eventsync.utils.showToast
import com.google.firebase.firestore.FirebaseFirestore

class CommentsAdapter(
    private val commentItemListener: CommentItemListener
): RecyclerView.Adapter<CommentsAdapter.CommentItemViewHolder>() {

    private val commentList: ArrayList<CommentItem> = ArrayList()

    class CommentItemViewHolder(private val binding: CommentItemLayoutBinding): RecyclerView.ViewHolder(binding.root){
        fun bind(commentItem: CommentItem, commentItemListener: CommentItemListener){
            binding.tvUserComment.text = commentItem.comment
            binding.profileImage.load(commentItem.userProfilePhoto){
                placeholder(R.drawable.sample_profile_img)
                error(R.drawable.sample_profile_img)
            }
            binding.tvUserName.text = commentItem.userName
            val preferenceManager = PreferenceManager(binding.root.context)

            FirebaseFirestore.getInstance().collection("all_events")
                .document(commentItem.eventId)
                .get().addOnSuccessListener {
                    val creatorId = it.getString("creatorId")
                    if (creatorId == preferenceManager.getUserId()){
                        binding.moreBtn.visibility = View.VISIBLE
                    }
                }



            if (commentItem.userId == preferenceManager.getUserId()!!){
                binding.moreBtn.visibility = View.VISIBLE
            }else{
                binding.moreBtn.visibility = View.GONE
            }

            binding.moreBtn.setOnClickListener {
                showPopupMenu(it, commentItem.commentId, commentItem.eventId)
            }

            listOf(binding.profileImage, binding.tvUserName).forEach {
                it.setOnClickListener {
                    commentItemListener.onCommentClick(commentItem)
                }
            }

        }
        private fun showPopupMenu(view: View, commentId: String, eventId: String) {
            val popupMenu = PopupMenu(binding.root.context, view)
            popupMenu.menuInflater.inflate(R.menu.comment_drop_down_menu, popupMenu.menu)

            popupMenu.setOnMenuItemClickListener { item: MenuItem ->
                when (item.itemId) {
                    R.id.delete_comment -> {

                        FirebaseFirestore.getInstance().collection("all_events")
                            .document(eventId)
                            .collection("comments")
                            .document(commentId)
                            .delete().addOnSuccessListener {
                                binding.root.context.showToast("Successfully deleted")
                            }.addOnFailureListener {
                                binding.root.context.showToast("Something went wrong.")
                            }
                        true
                    }
                    // Add more cases as needed
                    else -> false
                }
            }

            popupMenu.show()

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = CommentItemLayoutBinding.inflate(inflater, parent, false)
        return CommentItemViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return commentList.size
    }

    override fun onBindViewHolder(holder: CommentItemViewHolder, position: Int) {
       holder.bind(commentList[position], commentItemListener)
    }

    fun isEmpty(): Boolean{
        return commentList.isEmpty()
    }

    fun updateList(newCommentList: List<CommentItem>){
        val diffCallback = CommentListDIffUtilCallback(commentList, newCommentList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        commentList.clear()
        commentList.addAll(newCommentList)
        diffResult.dispatchUpdatesTo(this)
    }
}

interface CommentItemListener{
    fun onCommentClick(commentItem: CommentItem)
}