package com.example.mediconnect.models

import android.os.Parcel
import android.os.Parcelable

// Data class para sa MedicalLog na pwedeng i-pass gamit Parcelable
data class MedicalLog(
    val patientName: String?,          // Pangalan ng pasyente (nullable)
    val appointmentDate: String?,      // Petsa ng appointment (nullable)
    val diagnosis: String?,            // Diagnosis ng pasyente (nullable)
    val notes: String?,                // Notes tungkol sa pasyente o appointment (nullable)
    val status: String?,               // Status ng appointment (nullable)
    val doctorNotes: String?,          // Notes ng doktor (nullable)
    val date: String?,                 // Petsa ng record (nullable)
    val doctorName: String?,           // Pangalan ng doktor (nullable)
    val doctorId: String?,             // ID ng doktor (nullable)
    val patientId: String?,            // ID ng pasyente (nullable)
    val appointmentId: String?,        // ID ng appointment (nullable)
    val appointmentTime: String?,      // Oras ng appointment (nullable)
    val appointmentDay: String?,       // Araw ng appointment (nullable)
    val appointmentMonth: String?,     // Buwan ng appointment (nullable)
    val appointmentYear: String?,      // Taon ng appointment (nullable)
    val appointmentHour: String?,      // Oras (hour) ng appointment (nullable)
    val appointmentMinute: String?     // Oras (minute) ng appointment (nullable)
) : Parcelable {

    // Constructor para i-create ang object mula sa Parcel (Parcelable implementation)
    constructor(parcel: Parcel) : this(
        parcel.readString(),   // Basa ng patientName
        parcel.readString(),   // Basa ng appointmentDate
        parcel.readString(),   // Basa ng diagnosis
        parcel.readString(),   // Basa ng notes
        parcel.readString(),   // Basa ng status
        parcel.readString(),   // Basa ng doctorNotes
        parcel.readString(),   // Basa ng date
        parcel.readString(),   // Basa ng doctorName
        parcel.readString(),   // Basa ng doctorId
        parcel.readString(),   // Basa ng patientId
        parcel.readString(),   // Basa ng appointmentId
        parcel.readString(),   // Basa ng appointmentTime
        parcel.readString(),   // Basa ng appointmentDay
        parcel.readString(),   // Basa ng appointmentMonth
        parcel.readString(),   // Basa ng appointmentYear
        parcel.readString(),   // Basa ng appointmentHour
        parcel.readString()    // Basa ng appointmentMinute
    )

    // Isinusulat ang mga fields papunta sa Parcel para maipasa sa Intent/Bundle
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(patientName)        // Isinusulat ang patientName
        parcel.writeString(appointmentDate)    // Isinusulat ang appointmentDate
        parcel.writeString(diagnosis)          // Isinusulat ang diagnosis
        parcel.writeString(notes)              // Isinusulat ang notes
        parcel.writeString(status)             // Isinusulat ang status
        parcel.writeString(doctorNotes)        // Isinusulat ang doctorNotes
        parcel.writeString(date)               // Isinusulat ang date
        parcel.writeString(doctorName)         // Isinusulat ang doctorName
        parcel.writeString(doctorId)           // Isinusulat ang doctorId
        parcel.writeString(patientId)          // Isinusulat ang patientId
        parcel.writeString(appointmentId)      // Isinusulat ang appointmentId
        parcel.writeString(appointmentTime)    // Isinusulat ang appointmentTime
        parcel.writeString(appointmentDay)     // Isinusulat ang appointmentDay
        parcel.writeString(appointmentMonth)   // Isinusulat ang appointmentMonth
        parcel.writeString(appointmentYear)    // Isinusulat ang appointmentYear
        parcel.writeString(appointmentHour)    // Isinusulat ang appointmentHour
        parcel.writeString(appointmentMinute)  // Isinusulat ang appointmentMinute
    }

    // Required override para sa Parcelable, wala itong special contents kaya 0 lang
    override fun describeContents(): Int = 0

    // Companion object para sa Parcelable.Creator na gumagawa ng MedicalLog mula sa Parcel
    companion object CREATOR : Parcelable.Creator<MedicalLog> {
        override fun createFromParcel(parcel: Parcel): MedicalLog = MedicalLog(parcel) // Gumagawa ng MedicalLog mula Parcel
        override fun newArray(size: Int): Array<MedicalLog?> = arrayOfNulls(size)     // Gumagawa ng array ng MedicalLog nullable
    }
}
