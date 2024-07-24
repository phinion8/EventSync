package com.app.eventsync.presentation.comment_screen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.eventsync.R
import com.app.eventsync.data.models.CommentItem
import com.app.eventsync.data.repositories.DatabaseState
import com.app.eventsync.databinding.FragmentCommentsBottomSheetBinding
import com.app.eventsync.presentation.comment_screen.adapter.CommentItemListener
import com.app.eventsync.presentation.comment_screen.adapter.CommentsAdapter
import com.app.eventsync.presentation.comment_screen.viewModel.CommentsViewModel
import com.app.eventsync.utils.Constants
import com.app.eventsync.utils.LoadingDialog
import com.app.eventsync.utils.PreferenceManager
import com.app.eventsync.utils.showToast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CommentsBottomSheetFragment : BottomSheetDialogFragment(), CommentItemListener {

    private lateinit var binding: FragmentCommentsBottomSheetBinding
    private val viewModel: CommentsViewModel by viewModels()
    private lateinit var adapter: CommentsAdapter
    private lateinit var loadingDialog: LoadingDialog

    @Inject
    lateinit var preferenceManager: PreferenceManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCommentsBottomSheetBinding.inflate(layoutInflater, container, false)
        adapter = CommentsAdapter(this)

        loadingDialog = LoadingDialog(requireContext())
        val eventId = arguments?.getString(Constants.EVENT_ID)

        if (savedInstanceState == null) {
            if (eventId != null) {
                viewModel.getCommentList(eventId)
            }
        }

        viewModel.commentListState.observe(viewLifecycleOwner) { value ->

            when (value) {
                is DatabaseState.Loading -> {
                    loadingDialog.show()
                }

                is DatabaseState.Success -> {
                    adapter.updateList(value.data!!)
                    loadingDialog.dismiss()
                    checkForNoData()
                }

                is DatabaseState.Error -> {
                    loadingDialog.dismiss()
                    requireContext().showToast(value.message!!)
                }
            }

        }


        binding.sendCommentBtn.setOnClickListener {
            val comment = binding.etSendComment.text.toString()
            val commentItem = CommentItem(
                eventId!!,
                preferenceManager.getUserId()!!,
                preferenceManager.getUserName()!!,
                preferenceManager.getUserProfilePic()!!,
                comment
            )
            binding.etSendComment.setText(null)
            viewModel.postComment(commentItem)
        }

        binding.rvComments.adapter = adapter
        binding.rvComments.layoutManager = LinearLayoutManager(requireContext())
        return binding.root
    }

    private fun checkForNoData() {
        if (adapter.isEmpty()) {
            showNoDataLayout(true)
        } else {
            showNoDataLayout(false)
        }
    }

    private fun showNoDataLayout(value: Boolean) {
        binding.noCommentFoundAnim.isVisible = value
        binding.noDataText.isVisible = value
    }

    override fun onCommentClick(commentItem: CommentItem) {
        val bundle = Bundle()
        bundle.putString(Constants.USER_ID, commentItem.userId)
        requireActivity().findNavController(R.id.fragmentMainContainerView)
            .navigate(R.id.action_eventDetailsFragment_to_userProfileFragment, bundle, null)
    }


}