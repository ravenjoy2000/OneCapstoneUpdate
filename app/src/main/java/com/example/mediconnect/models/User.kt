package com.example.mediconnect.models

import android.os.Parcel
import android.os.Parcelable

// Data class para sa User na maaaring ipasa gamit Parcelable
data class User(
    val id: String = "",                 // Unique ID ng user
    val name: String = "",               // Buong pangalan ng user
    val username: String = "",           // Username ng user
    val email: String = "",              // Email address ng user
    val goverment_or_phealtID: String = "", // PhilHealth o Government ID URL
    val role: String = "",               // Role ng user (hal. patient, doctor)
    val phone: String  = "",             // Numero ng cellphone
    val image: String = "",              // URL ng profile picture
    val fcmToken: String = "",           // Firebase Cloud Messaging token para sa notifications
    var selected: Boolean = false        // Ginagamit para sa pagpili sa list views
) : Parcelable {

    // Constructor para mabuo ang object mula sa Parcel (Parcelable implementation)
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",       // Basa ng id
        parcel.readString() ?: "",       // Basa ng name
        parcel.readString() ?: "",       // Basa ng username
        parcel.readString() ?: "",       // Basa ng email
        parcel.readString() ?: "",       // Basa ng goverment_or_phealtID
        parcel.readString() ?: "",       // Basa ng role
        parcel.readString() ?: "",       // Basa ng phone
        parcel.readString() ?: "",       // Basa ng image
        parcel.readString() ?: "",       // Basa ng fcmToken
        parcel.readByte() != 0.toByte()  // Basa ng selected (boolean)
    )

    // Isinusulat ang mga fields papunta sa Parcel para maipasa sa Intent o Bundle
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)            // Isinusulat ang id
        parcel.writeString(name)          // Isinusulat ang name
        parcel.writeString(username)      // Isinusulat ang username
        parcel.writeString(email)         // Isinusulat ang email
        parcel.writeString(goverment_or_phealtID)  // Isinusulat ang PhilHealth/Gov ID URL
        parcel.writeString(role)          // Isinusulat ang role ng user
        parcel.writeString(phone)         // Isinusulat ang phone number
        parcel.writeString(image)         // Isinusulat ang image URL
        parcel.writeString(fcmToken)      // Isinusulat ang FCM token
        parcel.writeByte(if (selected) 1 else 0)  // Isinusulat ang selected bilang byte
    }

    // Required override para sa Parcelable, walang special contents kaya 0 lang ang ibinabalik
    override fun describeContents(): Int = 0

    // Companion object para sa Parcelable.Creator na gumagawa ng User mula sa Parcel
    companion object CREATOR : Parcelable.Creator<User> {
        override fun createFromParcel(parcel: Parcel): User = User(parcel)  // Gumagawa ng User object mula Parcel
        override fun newArray(size: Int): Array<User?> = arrayOfNulls(size) // Gumagawa ng array ng User na nullable
    }
}
