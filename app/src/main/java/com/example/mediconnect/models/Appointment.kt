package com.example.mediconnect.models

import android.os.Parcel
import android.os.Parcelable
import java.util.Date

// Data class na nagrerepresenta ng Appointment, na pwede maipasa sa intents gamit Parcelable
data class Appointment(
    val appointmentId: String = "",         // ID ng appointment (optional kung separate ang doc ID)
    val patientId: String = "",             // ID ng pasyente
    val patientName: String = "",           // Pangalan ng pasyente
    val doctorId: String = "",              // ID ng doktor
    val doctorName: String = "",            // Pangalan ng doktor
    val status: String = "",                // Status ng appointment (e.g., booked, cancelled)
    val date: String = "",                  // Petsa ng appointment (format depende sa implementation)
    val time: String = "",                  // Oras ng appointment
    val mode: String = "",                  // Mode ng appointment (e.g., in-person, teleconsult)
    val location: String = "",              // Lugar ng appointment
    val note: String = "",                  // Additional notes para sa appointment
    val reason: String = "",                // Reason o dahilan ng appointment
    val bookedAt: Date? = null,             // Date kung kailan na-book ang appointment
    val previousDate: String = "",           // Dating petsa ng appointment kung na-reschedule
    val dateTime: Long = 0L,  // <-- Make sure this exists
    val cancellationReason: String? = null // ✅ add this


) : Parcelable {                           // Ginagamit para maipasa ang object sa intents o bundles

    // Constructor para i-unparcel ang data mula sa Parcel
    constructor(parcel: Parcel) : this(
        appointmentId = parcel.readString() ?: "",      // Basahin appointmentId mula sa parcel
        patientId = parcel.readString() ?: "",          // Basahin patientId
        patientName = parcel.readString() ?: "",        // Basahin patientName
        doctorId = parcel.readString() ?: "",           // Basahin doctorId
        doctorName = parcel.readString() ?: "",         // Basahin doctorName
        status = parcel.readString() ?: "",             // Basahin status
        date = parcel.readString() ?: "",               // Basahin date
        time = parcel.readString() ?: "",               // Basahin time
        mode = parcel.readString() ?: "",               // Basahin mode
        location = parcel.readString() ?: "",           // Basahin location
        note = parcel.readString() ?: "",               // Basahin note
        reason = parcel.readString() ?: "",             // Basahin reason
        // Basahin bookedAt bilang Long timestamp, -1L kung wala, kaya convert sa Date o null
        bookedAt = parcel.readLong().let { if (it != -1L) Date(it) else null },
        previousDate = parcel.readString() ?: "" ,       // Basahin previousDate
        dateTime = parcel.readLong()  ,                  // Basahin dateTime
        cancellationReason = parcel.readString()        // Basahin cancellationReason

    )

    // Isusulat ang mga properties sa parcel para maipasa sa ibang activity/fragment
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(appointmentId)     // Isulat appointmentId
        parcel.writeString(patientId)         // Isulat patientId
        parcel.writeString(patientName)       // Isulat patientName
        parcel.writeString(doctorId)          // Isulat doctorId
        parcel.writeString(doctorName)        // Isulat doctorName
        parcel.writeString(status)            // Isulat status
        parcel.writeString(date)              // Isulat date
        parcel.writeString(time)              // Isulat time
        parcel.writeString(mode)              // Isulat mode
        parcel.writeString(location)          // Isulat location
        parcel.writeString(note)              // Isulat note
        parcel.writeString(reason)            // Isulat reason
        parcel.writeLong(bookedAt?.time ?: -1L)  // Isulat bookedAt bilang Long timestamp o -1L kung null
        parcel.writeString(previousDate)     // Isulat previousDate
        parcel.writeLong(dateTime)            // Isulat dateTime
        parcel.writeString(cancellationReason) // Isulat cancellationReason
    }

    override fun describeContents(): Int = 0  // Default implementation ng describeContents para sa Parcelable

    companion object CREATOR : Parcelable.Creator<Appointment> {
        // Ginagawa ang Appointment object mula sa Parcel
        override fun createFromParcel(parcel: Parcel): Appointment {
            return Appointment(parcel)
        }

        // Gumagawa ng array ng Appointment objects
        override fun newArray(size: Int): Array<Appointment?> {
            return arrayOfNulls(size)
        }
    }
}
