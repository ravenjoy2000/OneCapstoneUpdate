package com.example.mediconnect.models

import android.os.Parcel
import android.os.Parcelable

data class User(
    val id: String = "",                 // Unique ID ng user
    val name: String = "",               // Buong pangalan ng user
    val username: String = "",           // Username ng user
    val email: String = "",              // Email address ng user
    val goverment_or_phealtID: String = "", // PhilHealth or Government ID URL
    val role: String = "",               // Role ng user (e.g., patient, doctor)
    val phone: String  = "",                // Cellphone number
    val image: String = "",              // Profile picture URL
    val fcmToken: String = "",           // Firebase Cloud Messaging token
    var selected: Boolean = false        // Ginagamit para sa selection sa list views
) : Parcelable {

    // Constructor to recreate object from Parcel
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",       // id
        parcel.readString() ?: "",       // name
        parcel.readString() ?: "",       // username
        parcel.readString() ?: "",       // email
        parcel.readString() ?: "",       // goverment_or_phealtID
        parcel.readString() ?: "",       // role ✅ now included
        parcel.readString() ?: "",       // phone
        parcel.readString() ?: "",       // image
        parcel.readString() ?: "",       // fcmToken
        parcel.readByte() != 0.toByte()  // selected
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeString(username)
        parcel.writeString(email)
        parcel.writeString(goverment_or_phealtID)
        parcel.writeString(role)              // ✅ role included
        parcel.writeString(phone)
        parcel.writeString(image)
        parcel.writeString(fcmToken)
        parcel.writeByte(if (selected) 1 else 0)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<User> {
        override fun createFromParcel(parcel: Parcel): User = User(parcel)
        override fun newArray(size: Int): Array<User?> = arrayOfNulls(size)
    }
}
