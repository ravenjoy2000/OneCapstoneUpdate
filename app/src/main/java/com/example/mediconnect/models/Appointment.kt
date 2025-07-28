package com.example.mediconnect.models

import android.os.Parcel
import android.os.Parcelable
import java.util.Date

data class Appointment(
    val doctorName: String = "",
    val status: String = "",
    val date: String = "",
    val time: String = "",
    val mode: String = "",
    val location: String = "",
    val note: String = "",
    val reason: String = "",       // Reason for cancellation or reschedule
    val bookedAt: Date? = null,
    val previousDate: String = ""  // <-- New field added
) : Parcelable {

    constructor(parcel: Parcel) : this(
        doctorName = parcel.readString() ?: "",
        status = parcel.readString() ?: "",
        date = parcel.readString() ?: "",
        time = parcel.readString() ?: "",
        mode = parcel.readString() ?: "",
        location = parcel.readString() ?: "",
        note = parcel.readString() ?: "",
        reason = parcel.readString() ?: "",
        bookedAt = parcel.readSerializable() as? Date,
        previousDate = parcel.readString() ?: "" // <-- Read this
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(doctorName)
        parcel.writeString(status)
        parcel.writeString(date)
        parcel.writeString(time)
        parcel.writeString(mode)
        parcel.writeString(location)
        parcel.writeString(note)
        parcel.writeString(reason)
        parcel.writeSerializable(bookedAt)
        parcel.writeString(previousDate) // <-- Write this
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
