package com.example.mediconnect.models

import android.os.Parcel
import android.os.Parcelable

// Data class para sa Booking na pwedeng i-pass sa intents dahil nag-implement ng Parcelable
data class Booking(
    val patientId: String = "",            // ID ng pasyente
    val patientName: String = "",          // Pangalan ng pasyente
    val doctorId: String = "",             // ID ng doktor
    val doctorName: String? = "",          // Pangalan ng doktor (optional)
    val date: String = "",                 // Petsa ng booking
    val timeSlot: String = "",             // Oras o slot ng appointment
    val status: String = "booked",         // Status ng booking (default: booked)
    val mode: String = "in_person",        // Mode ng appointment (default: in person)
    val timestamp: Long = System.currentTimeMillis(),  // Timestamp kung kailan na-book
    val reason: String = "",               // Dahilan ng appointment
    val doctorPhone: String = "",          // Numero ng doktor
    val doctorAddress: String = ""         // Address ng klinika/doktor
) : Parcelable {

    // Constructor para i-create ang object mula sa Parcel (pagbasa mula Intent o Bundle)
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",          // Basa ng patientId mula sa parcel
        parcel.readString() ?: "",          // Basa ng patientName mula sa parcel
        parcel.readString() ?: "",          // Basa ng doctorId mula sa parcel
        parcel.readString(),                // Basa ng doctorName mula sa parcel (nullable)
        parcel.readString() ?: "",          // Basa ng date mula sa parcel
        parcel.readString() ?: "",          // Basa ng timeSlot mula sa parcel
        parcel.readString() ?: "",          // Basa ng status mula sa parcel
        parcel.readString() ?: "",          // Basa ng mode mula sa parcel
        parcel.readLong(),                  // Basa ng timestamp mula sa parcel
        parcel.readString() ?: "",          // Basa ng reason mula sa parcel
        parcel.readString() ?: "",          // Basa ng doctorPhone mula sa parcel
        parcel.readString() ?: ""           // Basa ng doctorAddress mula sa parcel
    )

    // Isinusulat ang properties papunta sa Parcel para maipasa sa Intent o Bundle
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(patientId)       // Isinusulat ang patientId
        parcel.writeString(patientName)     // Isinusulat ang patientName
        parcel.writeString(doctorId)        // Isinusulat ang doctorId
        parcel.writeString(doctorName)      // Isinusulat ang doctorName
        parcel.writeString(date)             // Isinusulat ang date
        parcel.writeString(timeSlot)         // Isinusulat ang timeSlot
        parcel.writeString(status)           // Isinusulat ang status
        parcel.writeString(mode)             // Isinusulat ang mode
        parcel.writeLong(timestamp)          // Isinusulat ang timestamp
        parcel.writeString(reason)           // Isinusulat ang reason
        parcel.writeString(doctorPhone)      // Isinusulat ang doctorPhone
        parcel.writeString(doctorAddress)    // Isinusulat ang doctorAddress
    }

    override fun describeContents(): Int = 0  // Required override ng Parcelable (walang special content)

    // Companion object para sa Parcelable.Creator na gagawa ng Booking instances mula sa Parcel
    companion object CREATOR : Parcelable.Creator<Booking> {
        override fun createFromParcel(parcel: Parcel): Booking = Booking(parcel)  // Gumagawa ng Booking mula sa Parcel
        override fun newArray(size: Int): Array<Booking?> = arrayOfNulls(size)   // Gumagawa ng array ng Booking na nullable
    }
}
