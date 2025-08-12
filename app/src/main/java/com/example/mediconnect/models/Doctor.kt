package com.example.mediconnect.models

import android.os.Parcel
import android.os.Parcelable

// Data class para sa Doctor na pwede i-pass sa intents dahil nag-implement ng Parcelable
data class Doctor(
    val profileImage: String = "",     // URL o path ng profile image ng doktor
    val name: String = "",             // Pangalan ng doktor
    val specialty: String = "",        // Espesyalisasyon ng doktor
    val phoneNumber: String = "",      // Numero ng telepono ng doktor
    val email: String = "",            // Email address ng doktor
    val address: String = "",          // Address o klinika ng doktor
    val website: String = "",          // Website ng doktor o klinika
    val bio: String = "",              // Maikling bio o impormasyon tungkol sa doktor
    val role : String = "doctor"       // Role, default na "doctor"
) : Parcelable {

    // Constructor na nagbabasa mula sa Parcel (para sa Parcelable implementation)
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",    // Basa ng profileImage mula sa parcel
        parcel.readString() ?: "",    // Basa ng name mula sa parcel
        parcel.readString() ?: "",    // Basa ng specialty mula sa parcel
        parcel.readString() ?: "",    // Basa ng phoneNumber mula sa parcel
        parcel.readString() ?: "",    // Basa ng email mula sa parcel
        parcel.readString() ?: "",    // Basa ng address mula sa parcel
        parcel.readString() ?: "",    // Basa ng website mula sa parcel
        parcel.readString() ?: "",    // Basa ng bio mula sa parcel
        parcel.readString() ?: ""     // Basa ng role mula sa parcel
    )

    // Isinusulat ang mga properties papunta sa Parcel (para maipasa sa Intent/Bundle)
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(profileImage)  // Isinusulat ang profileImage
        parcel.writeString(name)          // Isinusulat ang name
        parcel.writeString(specialty)     // Isinusulat ang specialty
        parcel.writeString(phoneNumber)   // Isinusulat ang phoneNumber
        parcel.writeString(email)          // Isinusulat ang email
        parcel.writeString(address)        // Isinusulat ang address
        parcel.writeString(website)        // Isinusulat ang website
        parcel.writeString(bio)            // Isinusulat ang bio
        parcel.writeString(role)           // Isinusulat ang role
    }

    override fun describeContents(): Int = 0   // Required override ng Parcelable (walang special content)

    // Companion object para sa Parcelable.Creator na gagawa ng Doctor instances mula sa Parcel
    companion object CREATOR : Parcelable.Creator<Doctor> {
        override fun createFromParcel(parcel: Parcel): Doctor {
            return Doctor(parcel)          // Gumagawa ng Doctor mula sa Parcel
        }

        override fun newArray(size: Int): Array<Doctor?> {
            return arrayOfNulls(size)     // Gumagawa ng array ng Doctor na nullable
        }
    }
}
