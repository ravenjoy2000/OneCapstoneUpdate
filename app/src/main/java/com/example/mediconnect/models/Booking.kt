package com.example.mediconnect.models

import android.os.Parcel
import android.os.Parcelable

// Data class para sa Booking na pwedeng i-pass sa intents dahil nag-implement ng Parcelable
data class Booking(
    val appointmentId: String = "",        // ID ng appointment
    val patientId: String = "",            // ID ng pasyente
    val patientName: String = "",          // Pangalan ng pasyente
    val patientEmail: String = "",         // 📧 Email ng pasyente
    val doctorId: String = "",             // ID ng doktor
    val doctorName: String? = "",          // Pangalan ng doktor (optional)
    val doctorEmail: String = "",          // 📧 Email ng doktor
    val date: String = "",                 // Petsa ng booking
    val timeSlot: String = "",             // Oras o slot ng appointment
    val status: String = "booked",         // Status ng booking (default: booked)
    val mode: String = "in_person",        // Mode ng appointment (default: in person)
    val timestamp: Long = System.currentTimeMillis(),  // Timestamp kung kailan na-book
    val reason: String = "",               // Dahilan ng appointment o service name
    val doctorPhone: String = "",          // Numero ng doktor
    val doctorAddress: String = "",        // Address ng klinika/doktor
    val servicePrice: Int = 0              // 💰 Presyo ng service (bago)
) : Parcelable {

    // Constructor para i-create ang object mula sa Parcel (pagbasa mula Intent o Bundle)
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",          // appointmentId
        parcel.readString() ?: "",          // patientId
        parcel.readString() ?: "",          // patientName
        parcel.readString() ?: "",          // patientEmail
        parcel.readString() ?: "",          // doctorId
        parcel.readString(),                // doctorName (nullable)
        parcel.readString() ?: "",          // doctorEmail
        parcel.readString() ?: "",          // date
        parcel.readString() ?: "",          // timeSlot
        parcel.readString() ?: "",          // status
        parcel.readString() ?: "",          // mode
        parcel.readLong(),                  // timestamp
        parcel.readString() ?: "",          // reason
        parcel.readString() ?: "",          // doctorPhone
        parcel.readString() ?: "",          // doctorAddress
        parcel.readInt()                    // servicePrice (bago)
    )

    // Isinusulat ang properties papunta sa Parcel para maipasa sa Intent o Bundle
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(appointmentId)   // appointmentId
        parcel.writeString(patientId)       // patientId
        parcel.writeString(patientName)     // patientName
        parcel.writeString(patientEmail)    // patientEmail
        parcel.writeString(doctorId)        // doctorId
        parcel.writeString(doctorName)      // doctorName
        parcel.writeString(doctorEmail)     // doctorEmail
        parcel.writeString(date)            // date
        parcel.writeString(timeSlot)        // timeSlot
        parcel.writeString(status)          // status
        parcel.writeString(mode)            // mode
        parcel.writeLong(timestamp)         // timestamp
        parcel.writeString(reason)          // reason
        parcel.writeString(doctorPhone)     // doctorPhone
        parcel.writeString(doctorAddress)   // doctorAddress
        parcel.writeInt(servicePrice)       // 💰 servicePrice (bago)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Booking> {
        override fun createFromParcel(parcel: Parcel): Booking = Booking(parcel)
        override fun newArray(size: Int): Array<Booking?> = arrayOfNulls(size)
    }
}
