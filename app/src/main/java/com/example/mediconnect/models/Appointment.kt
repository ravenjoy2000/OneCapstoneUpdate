package com.example.mediconnect.models

import android.os.Parcel
import android.os.Parcelable

data class Appointment(
    val doctorName: String = "",
    val status: String = "",
    val date: String = "",
    val time: String = "",
    val mode: String = "",
    val location: String = "",
    val note: String = ""
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(doctorName)
        parcel.writeString(status)
        parcel.writeString(date)
        parcel.writeString(time)
        parcel.writeString(mode)
        parcel.writeString(location)
        parcel.writeString(note)
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
