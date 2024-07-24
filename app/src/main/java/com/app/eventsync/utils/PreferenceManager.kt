package com.app.eventsync.utils

import android.content.Context
import android.content.SharedPreferences
import javax.inject.Inject

class PreferenceManager @Inject constructor(private val context: Context) {
    private var sharedPreferences: SharedPreferences =
        context.getSharedPreferences(USER_PREFERENCES, Context.MODE_PRIVATE)

    companion object {
        const val USER_PREFERENCES = "user_preferences"
        const val USER_ID = "user_id"
        const val DISPLAY_NAME = "display_name"
        const val EMAIL = "email"
        const val PHOTO_URL = "photo_url"
        const val THEME_MODE = "theme_mode"
        const val ALLOW_BACKGROUND_IMAGE = "background_image"
        const val IS_USER_FIRST_TIME = "is_user_first_time"
        const val NOTIFICATION_ALLOWED = "notification_allowed"
        const val IS_GOOGLE_ACCOUNT = "is_google_account"
        const val SIGN_OUT_FROM_GOOGLE = "sign_out_from_google"
    }

    fun saveUserInfoToSharedPreferences(userId: String, displayName: String?, email: String?, photoUrl: String?, isGoogleAccount: Boolean) {
        val editor = sharedPreferences.edit()

        editor.putString(USER_ID, userId)
        editor.putString(DISPLAY_NAME, displayName)
        editor.putString(EMAIL, email)
        editor.putString(PHOTO_URL, photoUrl)
        editor.putBoolean(IS_GOOGLE_ACCOUNT, isGoogleAccount)
        editor.apply()
    }

    fun editUserInfoInSharePreferences(displayName: String, photoUrl: String){
        val editor = sharedPreferences.edit()
        editor.putString(DISPLAY_NAME, displayName)
        editor.putString(PHOTO_URL, photoUrl)
        editor.apply()
    }

    fun getUserId(): String?{
        return sharedPreferences.getString(USER_ID, null)
    }

    fun getUserName(): String?{
        return sharedPreferences.getString(DISPLAY_NAME, null)
    }

    fun getUserEmail(): String?{
        return sharedPreferences.getString(EMAIL, null)
    }

    fun getUserProfilePic(): String?{
        return sharedPreferences.getString(PHOTO_URL, null)
    }

    fun isGoogleAccount(): Boolean{
        return sharedPreferences.getBoolean(IS_GOOGLE_ACCOUNT, false)
    }

    fun setThemeData(isDarkMode: Boolean){
        val editor = sharedPreferences.edit()
        editor.putBoolean(THEME_MODE, isDarkMode)
        editor.apply()
    }

    private val defaultThemeDataValue = AppUtils.isDarkMode(context)

    fun getThemeData(): Boolean{
        return sharedPreferences.getBoolean(THEME_MODE, defaultThemeDataValue)
    }

    fun setAllowBackgroundImage(value: Boolean){
        val editor = sharedPreferences.edit()
        editor.putBoolean(ALLOW_BACKGROUND_IMAGE, value)
        editor.apply()
    }


    fun getAllowBackgroundImage(): Boolean{
        return sharedPreferences.getBoolean(ALLOW_BACKGROUND_IMAGE, true)
    }

    fun setNotificationAllowed(value: Boolean){
        val editor = sharedPreferences.edit()
        editor.putBoolean(NOTIFICATION_ALLOWED, value)
        editor.apply()
    }

    fun getNotificationAllowed(): Boolean{
        return sharedPreferences.getBoolean(NOTIFICATION_ALLOWED, false)
    }

    fun setIsUserFirstTime(value: Boolean){
        val editor = sharedPreferences.edit()
        editor.putBoolean(IS_USER_FIRST_TIME, false)
        editor.apply()
    }

    fun setFCMToken(value: String){
        sharedPreferences.edit().putString(Constants.KEY_FCM_TOKEN, value).apply()
    }

    fun getFCMToken(): String{
        return sharedPreferences.getString(Constants.KEY_FCM_TOKEN, "").toString()
    }


    fun clearSharedPreferences(){
        context.getSharedPreferences(USER_PREFERENCES, 0).edit().clear().apply()
    }

    fun signOutFromGoogle(){

        sharedPreferences.edit().putBoolean(SIGN_OUT_FROM_GOOGLE, true).apply()

    }

    fun isUserSignOutFromGoogle(): Boolean{
        return sharedPreferences.getBoolean(SIGN_OUT_FROM_GOOGLE, false)
    }

}