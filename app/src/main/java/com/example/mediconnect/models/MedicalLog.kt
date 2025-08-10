package com.example.mediconnect.models

import android.os.Parcel
import android.os.Parcelable

data class MedicalLog(
    val patientName: String?,
    val appointmentDate: String?,
    val diagnosis: String?,
    val notes: String?,
    val status: String?,
    val doctorNotes: String?,
    val date: String?,
    val doctorName: String?,
    val doctorId: String?,
    val patientId: String?,
    val appointmentId: String?,
    val appointmentTime: String?,
    val appointmentDay: String?,
    val appointmentMonth: String?,
    val appointmentYear: String?,
    val appointmentHour: String?,
    val appointmentMinute: String?
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(patientName)
        parcel.writeString(appointmentDate)
        parcel.writeString(diagnosis)
        parcel.writeString(notes)
        parcel.writeString(status)
        parcel.writeString(doctorNotes)
        parcel.writeString(date)
        parcel.writeString(doctorName)
        parcel.writeString(doctorId)
        parcel.writeString(patientId)
        parcel.writeString(appointmentId)
        parcel.writeString(appointmentTime)
        parcel.writeString(appointmentDay)
        parcel.writeString(appointmentMonth)
        parcel.writeString(appointmentYear)
        parcel.writeString(appointmentHour)
        parcel.writeString(appointmentMinute)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<MedicalLog> {
        override fun createFromParcel(parcel: Parcel): MedicalLog {
            return MedicalLog(parcel)
        }

        override fun newArray(size: Int): Array<MedicalLog?> {
            return arrayOfNulls(size)
        }
    }
}
