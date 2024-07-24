package com.app.eventsync.utils

import android.text.TextUtils
import java.util.Calendar
import java.util.Date

object ValidationUtil {

    fun validateEmail(email: String): Boolean{
        return !TextUtils.isEmpty(email) && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun validatePassword(password: String): Boolean{
        return password.length >= 6
    }

    fun validateName(name: String): Boolean {
        // Check if the name is not empty
        if (name.isEmpty()) {
            return false
        }

        // Check if the name contains only letters and spaces
        val regex = Regex("^[a-zA-Z ]+\$")
        return regex.matches(name)
    }

    fun validateEventEditText(value: String, length: Int): Boolean{
        if (value.length >= length){
            return true
        }else{
            return false
        }
    }

    fun isEndDateGreaterThanStartDateAfterOneHour(startDate: Date, endDate: Date): Boolean {
        val calendar = Calendar.getInstance()
        calendar.time = startDate
        calendar.add(Calendar.HOUR_OF_DAY, 1)
        val startDatePlusOneHour = calendar.time

        // Check if endDate is after startDate + 1 hour
        return endDate.after(startDatePlusOneHour)
    }

    fun validateAvailableSeats(seats: Int): Boolean{
        return seats > 0
    }

}