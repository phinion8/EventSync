package com.app.eventsync.presentation.event_screens.create_event_screen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.app.eventsync.databinding.FragmentCreateEventFragementBinding
import com.app.eventsync.presentation.event_screens.create_event_screen.viewModel.CreateEventViewModel
import com.app.eventsync.presentation.event_screens.event_detail_screen.create_event_detail.CreateEventDetailFragment
import com.app.eventsync.presentation.event_screens.event_detail_screen.event_attachments.CreateEventAttachmentsFragment
import com.app.eventsync.presentation.main_screens.home_screen.adapters.VPAdapter
import com.app.eventsync.utils.Constants
import com.app.eventsync.utils.ErrorDialog
import com.app.eventsync.utils.LoadingDialog
import com.app.eventsync.utils.Resource
import com.app.eventsync.utils.showToast
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateEventFragment : Fragment() {

    private val viewModel: CreateEventViewModel by viewModels()
    private lateinit var binding: FragmentCreateEventFragementBinding
    private lateinit var vpAdapter: VPAdapter
    private lateinit var loadingDialog: LoadingDialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCreateEventFragementBinding.inflate(layoutInflater)
        vpAdapter = VPAdapter(childFragmentManager)
        binding.tabLayout.setupWithViewPager(binding.createEventViewPager)
        vpAdapter.addFragment(CreateEventDetailFragment(), "Event Details")
        vpAdapter.addFragment(CreateEventAttachmentsFragment(), "Attachments")

        binding.createEventViewPager.adapter = vpAdapter

        loadingDialog = LoadingDialog(requireContext())

        val isEditMode = arguments?.getBoolean(Constants.IS_EDIT_MODE)
        val eventId = arguments?.getString(Constants.EVENT_ID)

        if (isEditMode != null) {
            viewModel.isEditMode.value = isEditMode
            viewModel.eventId.value = eventId!!
        }

        if (isEditMode == true) {
            binding.screenTitle.text = "Edit Event"
            binding.deleteEventImg.visibility = View.VISIBLE
            binding.editSaveImg.visibility = View.VISIBLE
            binding.saveBtn.visibility = View.GONE
            if (eventId != null) {
                viewModel.getEventDetails(eventId)
                viewModel.eventTitle.observe(viewLifecycleOwner) {
                    binding.tvEventTitle.setText(it)
                }

                viewModel.deleteEventState.observe(viewLifecycleOwner){value->
                    when (value) {
                        is Resource.Loading -> {
                            loadingDialog.show()
                        }

                        is Resource.Success -> {
                            requireContext().showToast(value.message.toString())
                            requireActivity().supportFragmentManager.popBackStack()
                            loadingDialog.dismiss()
                        }

                        is Resource.Error -> {
                            loadingDialog.dismiss()
                            requireContext().showToast(value.message.toString())
                        }

                    }
                }


            }
        } else {
            binding.deleteEventImg.visibility = View.GONE
            binding.editSaveImg.visibility = View.GONE
            binding.saveBtn.visibility = View.VISIBLE
            viewModel.eventSavedState.observe(viewLifecycleOwner) { value ->

                when (value) {
                    is Resource.Loading -> {
                        loadingDialog.show()
                    }

                    is Resource.Success -> {
                        requireContext().showToast(value.message.toString())
                        requireActivity().supportFragmentManager.popBackStack()
                        loadingDialog.dismiss()
                    }

                    is Resource.Error -> {
                        loadingDialog.dismiss()
                        requireContext().showToast(value.message.toString())
                    }

                }

            }

        }




        binding.deleteEventImg.setOnClickListener {

            ErrorDialog(
                requireContext(),
                "Delete '${binding.tvEventTitle.text.toString()}'",
                "Are you sure you want to delete this event? you won't be able to revert back",
                onPositiveButtonClick = {

                    viewModel.deleteEvent(eventId!!)


                }).showErrorDialog()



        }

        binding.editSaveImg.setOnClickListener {

            val eventTitle = binding.tvEventTitle.text.toString()

            viewModel.onUpdateButtonClick(eventTitle)

        }


        binding.saveBtn.setOnClickListener {


            val eventTitle = binding.tvEventTitle.text.toString()

            viewModel.createEvent(eventTitle)


        }


        binding.backBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }




        return binding.root
    }

}