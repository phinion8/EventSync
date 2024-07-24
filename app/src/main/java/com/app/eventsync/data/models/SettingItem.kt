package com.app.eventsync.data.models

import androidx.annotation.DrawableRes
import com.app.eventsync.presentation.main_screens.profile_screen.SettingType

data class SettingItem(
    val id: SettingType = SettingType.EDIT_PROFILE,
    val title: String = "",
    @DrawableRes
    val icon: Int = -1,
    val info: String = "",
    val showSwitch: Boolean = false
)
