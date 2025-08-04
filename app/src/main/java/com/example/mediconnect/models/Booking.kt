package com.example.mediconnect.models

import android.os.Parcel
import android.os.Parcelable

data class Booking(
    val patientId: String = "",
    val patientName: String = "",
    val doctorId: String = "",
    val doctorName: String? = "",
    val date: String = "",
    val timeSlot: String = "",
    val status: String = "booked",
    val mode: String = "in_person",
    val timestamp: Long = System.currentTimeMillis(),
    val reason: String = "",
    val doctorPhone: String = "",
    val doctorAddress: String = ""
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readLong(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(patientId)
        parcel.writeString(patientName)
        parcel.writeString(doctorId)
        parcel.writeString(doctorName)
        parcel.writeString(date)
        parcel.writeString(timeSlot)
        parcel.writeString(status)
        parcel.writeString(mode)
        parcel.writeLong(timestamp)
        parcel.writeString(reason)
        parcel.writeString(doctorPhone)
        parcel.writeString(doctorAddress)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Booking> {
        override fun createFromParcel(parcel: Parcel): Booking = Booking(parcel)
        override fun newArray(size: Int): Array<Booking?> = arrayOfNulls(size)
    }
}
