package com.example.mediconnect.models

import android.os.Parcel
import android.os.Parcelable

data class Booking(
    val patientId: String = "",
    val patientName: String = "",
    val doctorName: String? = "", // <- Add this
    val date: String = "",           // Format: "2025-07-28"
    val timeSlot: String = "",       // Format: "09:00 AM"
    val status: String = "booked",   // Default value: "booked" or "cancelled"
    val mode: String = "in_person",  // Options: "in_person" or "teleconsult"
    val timestamp: Long = System.currentTimeMillis(), // Time the booking was created
    val reason: String = ""


) : Parcelable {

    // Constructor to recreate Booking from Parcel
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readLong(),
        parcel.readString() ?: ""
        )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(patientId)
        parcel.writeString(patientName)
        parcel.writeString(doctorName)
        parcel.writeString(date)
        parcel.writeString(timeSlot)
        parcel.writeString(status)
        parcel.writeString(mode)
        parcel.writeLong(timestamp)
        parcel.writeString(reason)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Booking> {
        override fun createFromParcel(parcel: Parcel): Booking {
            return Booking(parcel)
        }

        override fun newArray(size: Int): Array<Booking?> {
            return arrayOfNulls(size)
        }
    }
}
