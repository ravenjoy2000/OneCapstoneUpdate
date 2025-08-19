package com.example.mediconnect.patient

import android.os.Parcelable
import com.google.android.material.datepicker.CalendarConstraints
import kotlinx.parcelize.Parcelize
import java.util.Calendar
import java.util.TimeZone

@Parcelize
class FutureDateValidator : CalendarConstraints.DateValidator, Parcelable {
    override fun isValid(date: Long): Boolean {
        // Compare against today's midnight in UTC to align with MaterialDatePicker
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return date >= utc.timeInMillis
    }
}
