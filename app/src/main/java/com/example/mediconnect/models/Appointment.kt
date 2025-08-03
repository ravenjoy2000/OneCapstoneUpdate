package com.example.mediconnect.models

import android.os.Parcel
import android.os.Parcelable
import java.util.Date

data class Appointment(
    val appointmentId: String = "",         // Optional: If you store doc ID separately
    val patientId: String = "",
    val patientName: String = "",
    val doctorId: String = "",
    val doctorName: String = "",
    val status: String = "",
    val date: String = "",
    val time: String = "",
    val mode: String = "",
    val location: String = "",
    val note: String = "",
    val reason: String = "",
    val bookedAt: Date? = null,
    val previousDate: String = ""
) : Parcelable {

    constructor(parcel: Parcel) : this(
        appointmentId = parcel.readString() ?: "",
        patientId = parcel.readString() ?: "",
        patientName = parcel.readString() ?: "",
        doctorId = parcel.readString() ?: "",
        doctorName = parcel.readString() ?: "",
        status = parcel.readString() ?: "",
        date = parcel.readString() ?: "",
        time = parcel.readString() ?: "",
        mode = parcel.readString() ?: "",
        location = parcel.readString() ?: "",
        note = parcel.readString() ?: "",
        reason = parcel.readString() ?: "",
        bookedAt = parcel.readLong().let { if (it != -1L) Date(it) else null },
        previousDate = parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(appointmentId)
        parcel.writeString(patientId)
        parcel.writeString(patientName)
        parcel.writeString(doctorId)
        parcel.writeString(doctorName)
        parcel.writeString(status)
        parcel.writeString(date)
        parcel.writeString(time)
        parcel.writeString(mode)
        parcel.writeString(location)
        parcel.writeString(note)
        parcel.writeString(reason)
        parcel.writeLong(bookedAt?.time ?: -1L)
        parcel.writeString(previousDate)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Appointment> {
        override fun createFromParcel(parcel: Parcel): Appointment {
            return Appointment(parcel)
        }

        override fun newArray(size: Int): Array<Appointment?> {
            return arrayOfNulls(size)
        }
    }
}
