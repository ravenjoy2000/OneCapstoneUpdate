package com.example.mediconnect.models

import android.os.Parcel
import android.os.Parcelable

data class Doctor(
    val profileImage: String = "",
    val name: String = "",
    val specialty: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val address: String = "",
    val website: String = "",
    val bio: String = "",
    val role : String = "doctor"
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: ""

    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(profileImage)
        parcel.writeString(name)
        parcel.writeString(specialty)
        parcel.writeString(phoneNumber)
        parcel.writeString(email)
        parcel.writeString(address)
        parcel.writeString(website)
        parcel.writeString(bio)
        parcel.writeString(role)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Doctor> {
        override fun createFromParcel(parcel: Parcel): Doctor {
            return Doctor(parcel)
        }

        override fun newArray(size: Int): Array<Doctor?> {
            return arrayOfNulls(size)
        }
    }
}
