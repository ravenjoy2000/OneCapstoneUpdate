// Ito ang package kung saan naka-save ang model class na ito
package com.example.mediconnect.models

// Mga import na kailangan para sa Parcelable functionality
import android.os.Parcel
import android.os.Parcelable
import com.google.android.gms.common.internal.safeparcel.SafeParcelWriter.writeString

// Gumagawa tayo ng data class na tinatawag na User, na maaaring i-serialize gamit ang Parcelable
data class User (
    val id: String = "",          // Unique ID ng user
    val name: String = "",        // Buong pangalan ng user
    val username: String = "",    // Username ng user
    val email: String = "",       // Email address ng user
    val goverment_or_phealtID : String = "",
    val image: String = "",       // URL o path ng profile picture ng user
    val mobile: Long = 0,         // Cellphone number ng user
    val fcmToken: String = "",    // Token para sa Firebase Cloud Messaging (push notifications)
    var selected: Boolean = false // Ginagamit para malaman kung ang user ay napili sa listahan



): Parcelable { // Ipinapahiwatig na ang User object ay Parcelable (maipapasa sa ibang Activity/Fragment)

    // Secondary constructor na ginagamit para i-recreate ang User object mula sa Parcel
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,           // Binabasa ang id mula sa parcel
        parcel.readString()!!,           // Binabasa ang name mula sa parcel
        parcel.readString()!!,
        parcel.readString()!!,           // Binabasa ang email mula sa parcel
        parcel.readString()!!,           // Binabasa ang image URL mula sa parcel
        parcel.readString()!!,
        parcel.readLong(),               // Binabasa ang mobile number mula sa parcel
        parcel.readString()!!,           // Binabasa ang FCM token mula sa parcel
        parcel.readByte() != 0.toByte()  // Binabasa ang selected value (true kung hindi 0)
    )

    // Required ng Parcelable interface; 0 lang kasi walang special objects na dine-describe
    override fun describeContents(): Int = 0

    // Isinusulat ang data ng User object papunta sa Parcel
    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(id)         // Isinusulat ang id sa parcel
        writeString(name)       // Isinusulat ang name sa parcel
        writeString(username)
        writeString(email)      // Isinusulat ang email sa parcel
        writeString(goverment_or_phealtID)
        writeString(image)      // Isinusulat ang image URL sa parcel
        writeLong(mobile)       // Isinusulat ang mobile number sa parcel
        writeString(fcmToken)   // Isinusulat ang FCM token sa parcel
        writeByte(if (selected) 1 else 0) // Isinusulat ang selected bilang 1 (true) o 0 (false)
    }

    // Companion object na kailangan para sa Parcelable implementation
    companion object CREATOR : Parcelable.Creator<User> {
        // Ginagamit para i-recreate ang User object mula sa Parcel
        override fun createFromParcel(parcel: Parcel): User {
            return User(parcel) // Tumatawag sa constructor na may Parcel parameter
        }

        // Gumagawa ng array ng nullable User objects
        override fun newArray(size: Int): Array<User?> {
            return arrayOfNulls(size) // Bumabalik ng empty array ng User objects
        }
    }

} // Wakas ng User data class
