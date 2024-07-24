package com.app.eventsync.presentation.profile_page_screen.request_list_screen

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.eventsync.R
import com.app.eventsync.data.models.FriendItem
import com.app.eventsync.data.repositories.DatabaseState
import com.app.eventsync.databinding.FragmentRequestListBinding
import com.app.eventsync.presentation.profile_page_screen.adapter.FriendListAdapter
import com.app.eventsync.presentation.profile_page_screen.viewModel.UserProfileViewModel
import com.app.eventsync.utils.Constants
import com.app.eventsync.utils.LoadingDialog
import com.app.eventsync.utils.PreferenceManager
import com.app.eventsync.utils.Resource
import com.app.eventsync.utils.showToast
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RequestListFragment : Fragment(), FriendItemListCallback {

    private lateinit var binding: FragmentRequestListBinding
    private lateinit var adapter: FriendListAdapter
    private val viewModel: UserProfileViewModel by viewModels()
    private lateinit var loadingDialog: LoadingDialog
    private var currentRequestList: List<FriendItem> = ArrayList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentRequestListBinding.inflate(layoutInflater)
        loadingDialog = LoadingDialog(requireContext())
        adapter = FriendListAdapter(this, isRequest = true)

        viewModel.getRequestList(PreferenceManager(requireContext()).getUserId()!!)

        viewModel.requestListState.observe(viewLifecycleOwner){value->

            when(value){
                is DatabaseState.Loading->{

                    loadingDialog.show()

                }
                is DatabaseState.Success->{
                    adapter.updateList(value.data!!)
                    currentRequestList = value.data
                    checkForNoData()
                    loadingDialog.dismiss()

                }
                is DatabaseState.Error->{

                    loadingDialog.dismiss()

                }
            }

        }

        viewModel.acceptFriendState.observe(viewLifecycleOwner){value->
            when(value){
                is Resource.Loading->{
                    loadingDialog.show()
                }
                is Resource.Success->{
                    requireContext().showToast("Friend request accepted...")
                    viewModel.getRequestList(PreferenceManager(requireContext()).getUserId()!!)
                    loadingDialog.dismiss()
                }
                is Resource.Error->{
                    loadingDialog.dismiss()
                }
            }
        }

        binding.etSearch.addTextChangedListener {searchQuery->

            val searchList = currentRequestList.filter {friendItem ->

                friendItem.name.contains(searchQuery.toString())

            }

            adapter.updateList(searchList)
            checkForNoData()

        }

        binding.backBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.rvRequests.adapter = adapter
        binding.rvRequests.layoutManager = LinearLayoutManager(requireContext())
        return binding.root
    }

    override fun onAcceptClick(requestId: String) {

        viewModel.acceptFriendRequest(PreferenceManager(requireContext()).getUserId()!!, requestId)


    }

    override fun onRemoveClick(requestId: String) {

    }

    override fun onAddedClick(friendItem: FriendItem) {

    }

    override fun onRemoveCollaborator(friendItem: FriendItem) {

    }

    override fun onFriendItemClick(userId: String) {

        val bundle = Bundle()
        bundle.putString(Constants.USER_ID, userId)
        requireActivity().findNavController(R.id.fragmentMainContainerView).navigate(R.id.action_requestListFragment_to_userProfileFragment, bundle, null)


    }


    private fun checkForNoData(){
        if (adapter.isEmpty()){
            showNoDataLayout(true)
        }else{
            showNoDataLayout(false)
        }
    }


    private fun showNoDataLayout(value: Boolean){
       binding.noFriendFoundAnim.isVisible = value
       binding.noDataText.isVisible= value
    }

}

interface FriendItemListCallback{
    fun onAcceptClick(requestId: String)
    fun onRemoveClick(requestId: String)
    fun onAddedClick(friendItem: FriendItem)
    fun onRemoveCollaborator(friendItem: FriendItem)
    fun onFriendItemClick(userId: String)
}