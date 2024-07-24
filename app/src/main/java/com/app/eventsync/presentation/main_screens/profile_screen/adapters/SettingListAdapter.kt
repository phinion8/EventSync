package com.app.eventsync.presentation.main_screens.profile_screen.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.app.eventsync.data.models.SettingItem
import com.app.eventsync.databinding.ItemSettingsLayoutBinding
import com.app.eventsync.presentation.main_screens.profile_screen.SettingItemOnClickListener
import com.app.eventsync.presentation.main_screens.profile_screen.SettingItemSwitchChangeListener
import com.app.eventsync.presentation.main_screens.profile_screen.SettingType
import com.app.eventsync.utils.AppUtils
import com.app.eventsync.utils.PreferenceManager
import com.app.eventsync.utils.showToast

class SettingListAdapter(
    private val settingItemList: List<SettingItem>,
    private val settingItemOnClickListener: SettingItemOnClickListener,
    private val settingItemSwitchChangeListener: SettingItemSwitchChangeListener,
    private val preferenceManager: PreferenceManager
) : RecyclerView.Adapter<SettingListAdapter.SettingItemViewHolder>() {

    class SettingItemViewHolder(private val binding: ItemSettingsLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            settingItem: SettingItem,
            position: Int,
            isChecked: (value: Boolean) -> Unit,
            preferenceManager: PreferenceManager
        ) {
            binding.settingIcon.setImageResource(settingItem.icon)
            binding.tvSettingTitle.text = settingItem.title
            binding.tvSettingInfo.text = settingItem.info
            if (settingItem.showSwitch) {

                when (settingItem.id) {
                    SettingType.THEME -> {
                        if (AppUtils.isDarkMode(binding.root.context)){
                            binding.switch1.isChecked = preferenceManager.getThemeData()
                        }else{
                            binding.switch1.isChecked = preferenceManager.getThemeData()
                        }

                    }

                    SettingType.NOTIFICATION->{
                        if (preferenceManager.getNotificationAllowed()){
                            binding.switch1.isChecked = preferenceManager.getNotificationAllowed()
                        }
                        binding.switch1.isChecked = preferenceManager.getNotificationAllowed()
                    }

                    SettingType.SHOW_BACKGROUND_IMAGES->{
                        binding.switch1.isChecked = preferenceManager.getAllowBackgroundImage()
                    }

                    else -> {}
                }

                binding.switch1.visibility = View.VISIBLE
                binding.switch1.setOnCheckedChangeListener { _, isChecked ->

                    if (settingItem.id == SettingType.NOTIFICATION){
                        if (isChecked){
                            isChecked(isChecked)
                            binding.switch1.isChecked = preferenceManager.getNotificationAllowed()
                        }else{
                            binding.root.context.showToast("Notification permission is allowed, You can turn off by going into the app settings.")
                            binding.switch1.isChecked = true
                        }
                    }else{
                        isChecked(isChecked)
                    }


                }
            }


        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding: ItemSettingsLayoutBinding =
            ItemSettingsLayoutBinding.inflate(inflater, parent, false)
        return SettingItemViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return settingItemList.size
    }

    override fun onBindViewHolder(holder: SettingItemViewHolder, position: Int) {
        holder.bind(settingItemList[position], position, isChecked = {
            settingItemSwitchChangeListener.onSettingItemSwitchChecked(
                settingItemList[position].id,
                it
            )
        }, preferenceManager)
        holder.itemView.setOnClickListener {
            settingItemOnClickListener.onSettingItemClick(settingItemList[position].id)
        }
    }
}